package com.devoxx.genie.service.agent.codemode;

import com.devoxx.genie.service.agent.AgentLoopTracker;
import io.roastedroot.quickjs4j.core.Builtins;
import io.roastedroot.quickjs4j.core.Engine;
import io.roastedroot.quickjs4j.core.GuestException;
import io.roastedroot.quickjs4j.core.HostFunction;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import run.endive.runtime.ByteArrayMemory;
import run.endive.runtime.WasmInterruptedException;
import run.endive.wasm.types.MemoryLimits;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
final class CodeModeScriptRun {

    static final int MAX_OUTPUT_LENGTH = 20_000;

    static final int MAX_MEMORY_PAGES = 2048;

    private static final String HOST_MODULE = "__host";

    private static final String GUEST_ERROR_MARKER = "`Err` value: ";

    private static final long WATCHDOG_INTERVAL_MILLIS = 50;

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        var thread = new Thread(task, "DevoxxGenie-code-mode-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private static final String PRELUDE = """
            const __output = [];
            const __render = (value) => typeof value === "string" ? value : JSON.stringify(value, null, 2);
            const __describe = (error) => error && error.stack ? String(error) + "\\n" + error.stack : String(error);
            globalThis.text = (value) => { __output.push(value === undefined ? "undefined" : __render(value)); };
            const __print = (...values) => { __output.push(values.map(__render).join(" ")); };
            globalThis.console = { log: __print, info: __print, warn: __print, error: __print, debug: __print };
            globalThis.ALL_TOOLS = %s;
            globalThis.tools = new Proxy({}, {
              get: (target, name) => {
                if (typeof name !== "string" || name === "then") {
                  return undefined;
                }
                return async (args) => {
                  const raw = __host.call(name, JSON.stringify(args === undefined ? {} : args));
                  const trimmed = raw.trimStart();
                  if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    return raw;
                  }
                  try {
                    return JSON.parse(raw);
                  } catch (notJson) {
                    return raw;
                  }
                };
              }
            });
            """;

    private static final String SCRIPT = """
            try {
              const __result = await (async () => {
            %s
              })();
              if (__result !== undefined) {
                text(__result);
              }
            } catch (error) {
              text("Error: the script threw " + __describe(error));
            }
            __host.finish(__output.join("\\n"));
            """;

    private final String code;

    private final String toolCatalogJson;

    private final NestedToolCaller nestedToolCaller;

    private final Duration scriptTimeBudget;

    private final int maxToolCalls;

    private int toolCallCount;

    private final AtomicReference<StopReason> stopReason = new AtomicReference<>();

    private final AtomicReference<String> output = new AtomicReference<>();

    private final Object lock = new Object();

    private Thread scriptThread;

    private boolean scriptRunning;

    private boolean interruptedByStop;

    private long scriptTimeUsedNanos;

    private long scriptClockStartedAtNanos;

    CodeModeScriptRun(@NotNull String code,
                      @NotNull String toolCatalogJson,
                      @NotNull NestedToolCaller nestedToolCaller,
                      @NotNull Duration scriptTimeBudget,
                      int maxToolCalls) {
        this.code = Objects.requireNonNull(code, "The script code can not be null");
        this.toolCatalogJson = Objects.requireNonNull(toolCatalogJson, "The tool catalog can not be null");
        this.nestedToolCaller = Objects.requireNonNull(nestedToolCaller, "The nested tool caller can not be null");
        this.scriptTimeBudget = Objects.requireNonNull(scriptTimeBudget, "The script time budget can not be null");
        this.maxToolCalls = maxToolCalls;
    }

    /**
     * Builds one engine and runs a no-op script, so the WebAssembly runtime is loaded
     * before the first real {@code run_script} call.
     *
     * @throws IllegalStateException when the runtime could not run the script ({@link #run()} reports
     *                               failures as an "Error:" result instead of throwing)
     */
    static void warmUp() {
        String result = new CodeModeScriptRun("", "[]", (toolName, argumentsJson) -> "", Duration.ofSeconds(30), 1).run();
        if (result.startsWith("Error:")) {
            throw new IllegalStateException("Code mode warm-up failed: " + result);
        }
    }

    @NotNull String run() {
        if (this.stopReason.get() != null) {
            return stoppedMessage();
        }
        try (Engine engine = newEngine()) {
            var compiled = engine.compile(PRELUDE.formatted(this.toolCatalogJson) + SCRIPT.formatted(this.code));
            return execute(engine, compiled);
        } catch (IllegalArgumentException e) {
            return "Error: the script could not run: " + guestErrorOf(e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Code mode script failed inside the JavaScript runtime", e);
            return this.stopReason.get() != null
                    ? stoppedMessage()
                    : "Error: the JavaScript runtime failed: " + e.getMessage();
        }
    }

    void cancel() {
        stop(StopReason.CANCELLED);
    }

    private Engine newEngine() {
        var host = Builtins.builder(HOST_MODULE)
                .add(new HostFunction("call", List.of(String.class, String.class), String.class,
                        arguments -> callTool((String) arguments.get(0), (String) arguments.get(1))))
                .add(new HostFunction("finish", List.of(String.class), Void.class, arguments -> {
                    this.output.set(Objects.requireNonNullElse((String) arguments.get(0), ""));
                    return null;
                }))
                .build();
        return Engine.builder()
                .addBuiltins(host)
                .withMemoryFactory(CodeModeScriptRun::cappedMemory)
                .build();
    }

    private static ByteArrayMemory cappedMemory(MemoryLimits limits) {
        var maximumPages = Math.max(limits.initialPages(), Math.min(MAX_MEMORY_PAGES, limits.maximumPages()));
        return new ByteArrayMemory(new MemoryLimits(limits.initialPages(), maximumPages));
    }

    private String execute(Engine engine, int compiled) {
        synchronized (this.lock) {
            this.scriptThread = Thread.currentThread();
        }
        resumeScriptClock();
        var watchdog = WATCHDOG.scheduleAtFixedRate(this::stopWhenOverBudget,
                WATCHDOG_INTERVAL_MILLIS, WATCHDOG_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        try {
            engine.exec(compiled);
        } catch (WasmInterruptedException e) {
            return this.stopReason.get() != null ? stoppedMessage() : "Error: the script was interrupted.";
        } catch (GuestException e) {
            return this.stopReason.get() != null
                    ? stoppedMessage()
                    : "Error: the script failed: " + guestErrorOf(e.getMessage());
        } finally {
            watchdog.cancel(false);
            synchronized (this.lock) {
                this.scriptRunning = false;
                this.scriptThread = null;
                if (this.interruptedByStop) {
                    Thread.interrupted();
                }
            }
        }
        if (this.stopReason.get() != null) {
            return stoppedMessage();
        }
        var result = this.output.get();
        return result == null
                ? "Error: the script never finished: it awaited a promise that never settles."
                : capped(result);
    }

    private String callTool(@Nullable String toolName, @Nullable String argumentsJson) {
        pauseScriptClock();
        try {
            if (this.stopReason.get() != null) {
                return stoppedMessage();
            }
            if (++this.toolCallCount > this.maxToolCalls) {
                return "Error: this script reached its limit of " + this.maxToolCalls
                        + " tool calls. Return what you have, or continue in another run_script call.";
            }
            var result = this.nestedToolCaller.call(String.valueOf(toolName),
                    Objects.requireNonNullElse(argumentsJson, "{}"));
            return Objects.requireNonNullElse(result, "");
        } catch (RuntimeException e) {
            log.debug("Nested tool '{}' failed inside a code mode script", toolName, e);
            return "Error: " + e.getMessage();
        } finally {
            resumeScriptClock();
        }
    }

    private void resumeScriptClock() {
        synchronized (this.lock) {
            this.scriptClockStartedAtNanos = System.nanoTime();
            this.scriptRunning = true;
            if (this.stopReason.get() != null) {
                interruptScript();
            }
        }
    }

    private void pauseScriptClock() {
        synchronized (this.lock) {
            this.scriptRunning = false;
            this.scriptTimeUsedNanos += System.nanoTime() - this.scriptClockStartedAtNanos;
        }
    }

    private void stopWhenOverBudget() {
        synchronized (this.lock) {
            if (!this.scriptRunning) {
                return;
            }
            var used = this.scriptTimeUsedNanos + System.nanoTime() - this.scriptClockStartedAtNanos;
            if (used > this.scriptTimeBudget.toNanos()) {
                stop(StopReason.TIMED_OUT);
            }
        }
    }

    private void stop(StopReason reason) {
        if (!this.stopReason.compareAndSet(null, reason)) {
            return;
        }
        synchronized (this.lock) {
            interruptScript();
        }
    }

    private void interruptScript() {
        if (this.scriptRunning && this.scriptThread != null && !this.interruptedByStop) {
            this.interruptedByStop = true;
            this.scriptThread.interrupt();
        }
    }

    private String stoppedMessage() {
        if (this.stopReason.get() == StopReason.CANCELLED) {
            return AgentLoopTracker.CANCELLED_MESSAGE;
        }
        return "Error: the script was stopped after " + this.scriptTimeBudget.toSeconds()
                + " seconds of JavaScript run time. Do less work per script, or split it into several scripts.";
    }

    private static String guestErrorOf(@Nullable String message) {
        if (message == null) {
            return "unknown error";
        }
        var start = message.lastIndexOf(GUEST_ERROR_MARKER);
        if (start < 0) {
            return message.lines().findFirst().orElse(message);
        }
        return message.substring(start + GUEST_ERROR_MARKER.length()).lines().findFirst().orElse("").strip();
    }

    private static String capped(String output) {
        if (output.isBlank()) {
            return "The script finished without output. Pass the values you need to text(value) or return them.";
        }
        if (output.length() <= MAX_OUTPUT_LENGTH) {
            return output;
        }
        return output.substring(0, MAX_OUTPUT_LENGTH)
                + "\n... (output truncated at " + MAX_OUTPUT_LENGTH + " characters; print less)";
    }

    private enum StopReason {
        CANCELLED,
        TIMED_OUT
    }
}
