package com.devoxx.genie.service.agent.codemode;

import com.caoccao.javet.exceptions.BaseJavetScriptingException;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.exceptions.JavetTerminatedException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.IJavetDirectCallable;
import com.caoccao.javet.interop.callback.JavetCallbackContext;
import com.caoccao.javet.interop.callback.JavetCallbackType;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.IV8ValuePromise;
import com.caoccao.javet.values.reference.V8ValuePromise;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
final class CodeModeScriptRun {

    static final int MAX_OUTPUT_LENGTH = 20_000;

    private static final String TOOL_CALLBACK_NAME = "__callTool";

    private static final String RESOURCE_NAME = "run_script.js";

    private static final long WATCHDOG_INTERVAL_MILLIS = 50;

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(task -> {
        var thread = new Thread(task, "DevoxxGenie-code-mode-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private static final String PRELUDE = """
            const __output = [];
            const __render = (value) => typeof value === "string" ? value : JSON.stringify(value, null, 2);
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
                  const raw = __callTool(name, JSON.stringify(args === undefined ? {} : args));
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
            (async () => {
              try {
                const __result = await (async () => {
            %s
                })();
                if (__result !== undefined) {
                  text(__result);
                }
              } catch (error) {
                text("Error: the script threw " + (error && error.stack ? error.stack : String(error)));
              }
              return __output.join("\\n");
            })()
            """;

    private final String code;

    private final String toolCatalogJson;

    private final NestedToolCaller nestedToolCaller;

    private final Duration scriptTimeBudget;

    private final AtomicLong scriptTimeUsedNanos = new AtomicLong();

    private final AtomicReference<StopReason> stopReason = new AtomicReference<>();

    private volatile boolean scriptClockRunning;

    private volatile long scriptClockStartedAtNanos;

    private volatile V8Runtime runtime;

    CodeModeScriptRun(@NotNull String code,
                      @NotNull String toolCatalogJson,
                      @NotNull NestedToolCaller nestedToolCaller,
                      @NotNull Duration scriptTimeBudget) {
        this.code = Objects.requireNonNull(code, "The script code can not be null");
        this.toolCatalogJson = Objects.requireNonNull(toolCatalogJson, "The tool catalog can not be null");
        this.nestedToolCaller = Objects.requireNonNull(nestedToolCaller, "The nested tool caller can not be null");
        this.scriptTimeBudget = Objects.requireNonNull(scriptTimeBudget, "The script time budget can not be null");
    }

    static boolean isRuntimeAvailable() {
        return RuntimeAvailability.AVAILABLE;
    }

    private static boolean loadRuntime() {
        try {
            var host = V8Host.getV8Instance();
            if (!host.isLibraryLoaded()) {
                log.warn("Code mode unavailable: the V8 library did not load", host.getLastException());
                return false;
            }
            return true;
        } catch (Exception | LinkageError e) {
            log.warn("Code mode unavailable: the V8 runtime could not be initialised", e);
            return false;
        }
    }

    @NotNull String run() {
        if (this.stopReason.get() != null) {
            return stoppedMessage();
        }
        try (V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime()) {
            this.runtime = v8Runtime;
            bindToolCallback(v8Runtime);
            startScriptClock();
            var watchdog = WATCHDOG.scheduleAtFixedRate(this::stopWhenOverBudget,
                    WATCHDOG_INTERVAL_MILLIS, WATCHDOG_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            try {
                return capped(outputOf(v8Runtime));
            } finally {
                watchdog.cancel(false);
                this.runtime = null;
            }
        } catch (JavetTerminatedException e) {
            return stoppedMessage();
        } catch (BaseJavetScriptingException e) {
            return this.stopReason.get() != null
                    ? stoppedMessage()
                    : "Error: the script could not run: " + e.getScriptingError().getDetailedMessage();
        } catch (JavetException e) {
            log.warn("Code mode script failed inside the V8 runtime", e);
            return this.stopReason.get() != null
                    ? stoppedMessage()
                    : "Error: the JavaScript runtime failed: " + e.getMessage();
        }
    }

    void cancel() {
        stop(StopReason.CANCELLED);
    }

    private String outputOf(V8Runtime v8Runtime) throws JavetException {
        var source = PRELUDE.formatted(this.toolCatalogJson) + SCRIPT.formatted(this.code);
        try (V8ValuePromise promise = v8Runtime.getExecutor(source).setResourceName(RESOURCE_NAME).execute()) {
            v8Runtime.await();
            if (this.stopReason.get() != null) {
                return stoppedMessage();
            }
            if (promise.getState() != IV8ValuePromise.STATE_FULFILLED) {
                return "Error: the script never finished: it awaited a promise that never settles.";
            }
            try (V8Value output = promise.getResult()) {
                return output.toString();
            }
        }
    }

    private void bindToolCallback(V8Runtime v8Runtime) throws JavetException {
        var callback = new JavetCallbackContext(
                TOOL_CALLBACK_NAME,
                JavetCallbackType.DirectCallNoThisAndResult,
                (IJavetDirectCallable.NoThisAndResult<Exception>) arguments ->
                        v8Runtime.createV8ValueString(arguments.length < 2
                                ? "Error: " + TOOL_CALLBACK_NAME + " expects a tool name and JSON arguments."
                                : callTool(arguments[0].toString(), arguments[1].toString())));
        v8Runtime.getGlobalObject().bindFunction(callback);
    }

    private String callTool(String toolName, String argumentsJson) {
        pauseScriptClock();
        try {
            return this.nestedToolCaller.call(toolName, argumentsJson);
        } catch (RuntimeException e) {
            log.debug("Nested tool '{}' failed inside a code mode script", toolName, e);
            return "Error: " + e.getMessage();
        } finally {
            startScriptClock();
        }
    }

    private void startScriptClock() {
        this.scriptClockStartedAtNanos = System.nanoTime();
        this.scriptClockRunning = true;
    }

    private void pauseScriptClock() {
        this.scriptClockRunning = false;
        this.scriptTimeUsedNanos.addAndGet(System.nanoTime() - this.scriptClockStartedAtNanos);
    }

    private void stopWhenOverBudget() {
        if (!this.scriptClockRunning) {
            return;
        }
        var used = this.scriptTimeUsedNanos.get() + System.nanoTime() - this.scriptClockStartedAtNanos;
        if (used > this.scriptTimeBudget.toNanos()) {
            stop(StopReason.TIMED_OUT);
        }
    }

    private void stop(StopReason reason) {
        if (!this.stopReason.compareAndSet(null, reason)) {
            return;
        }
        var activeRuntime = this.runtime;
        if (activeRuntime != null) {
            activeRuntime.terminateExecution();
        }
    }

    private String stoppedMessage() {
        if (this.stopReason.get() == StopReason.CANCELLED) {
            return AgentLoopTracker.CANCELLED_MESSAGE;
        }
        return "Error: the script was stopped after " + this.scriptTimeBudget.toSeconds()
                + " seconds of JavaScript run time. Do less work per script, or split it into several scripts.";
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

    private static final class RuntimeAvailability {

        private static final boolean AVAILABLE = loadRuntime();
    }
}
