package com.devoxx.genie.service.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.model.ap.ApAuthMode;
import com.devoxx.genie.model.ap.ApProject;
import com.devoxx.genie.model.ap.ApRunEvent;
import com.devoxx.genie.model.ap.ApRunHandle;
import com.devoxx.genie.model.ap.ApSession;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Thin wrapper around the Docker Agentic Platform (`ap`) binary.
 *
 * <p>Synchronous list methods are intended to be invoked from a pooled thread
 * (never EDT). {@link #startRun} spawns the process itself on a pooled thread
 * and streams events back through the supplied callbacks.</p>
 *
 * <p>The CLI is executed with whichever authentication env vars are configured
 * in {@link DevoxxGenieStateService} — either {@code AP_AGENTIC_PLATFORM_AUTH_MODE=docker-desktop}
 * (no extra creds, relies on Docker Desktop login) or
 * {@code AP_AGENTIC_PLATFORM_ACCESS_TOKEN}/{@code AP_AGENTIC_PLATFORM_REFRESH_TOKEN}.</p>
 */
@Slf4j
@Service(Service.Level.APP)
public final class ApCliService {

    static final String ENV_AUTH_MODE = "AP_AGENTIC_PLATFORM_AUTH_MODE";
    static final String ENV_ACCESS_TOKEN = "AP_AGENTIC_PLATFORM_ACCESS_TOKEN";
    static final String ENV_REFRESH_TOKEN = "AP_AGENTIC_PLATFORM_REFRESH_TOKEN";

    private static final long LIST_TIMEOUT_SECONDS = 30;
    private static final long VERSION_TIMEOUT_SECONDS = 10;
    private static final int LIST_DEFAULT_LIMIT = 50;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ApCliService getInstance() {
        return ApplicationManager.getApplication().getService(ApCliService.class);
    }

    public @NotNull List<ApAgent> listAgents(int limit) throws ApCliException {
        String json = runJsonCommand(List.of("agent", "ls", "--json", "--limit", String.valueOf(limit)),
                null, LIST_TIMEOUT_SECONDS);
        List<ApAgent> agents = new ArrayList<>(parseList(json, new TypeReference<>() {}));
        agents.sort(Comparator.comparing(ApAgent::name,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return agents;
    }

    public @NotNull List<ApProject> listProjects(int limit) throws ApCliException {
        String json = runJsonCommand(List.of("project", "ls", "--json", "--limit", String.valueOf(limit)),
                null, LIST_TIMEOUT_SECONDS);
        return parseList(json, new TypeReference<>() {});
    }

    public @NotNull List<ApSession> listSessions(@Nullable String agentFilter,
                                                  @Nullable String projectFilter,
                                                  int limit) throws ApCliException {
        List<String> args = new ArrayList<>(List.of("session", "ls", "--json", "--limit", String.valueOf(limit)));
        if (agentFilter != null && !agentFilter.isBlank()) {
            args.add("--agent");
            args.add(agentFilter);
        }
        if (projectFilter != null && !projectFilter.isBlank()) {
            args.add("--project");
            args.add(projectFilter);
        }
        String json = runJsonCommand(args, null, LIST_TIMEOUT_SECONDS);
        return parseList(json, new TypeReference<>() {});
    }

    /**
     * Smoke-tests the binary path and the configured authentication mode.
     * Returns a structured result instead of throwing so settings UIs can display either branch.
     */
    public @NotNull TestResult testConnection() {
        String binaryPath = DevoxxGenieStateService.getInstance().getApCliPath();
        log.info("ap testConnection: starting (binaryPath={}, authMode={})",
                binaryPath, DevoxxGenieStateService.getInstance().getApAuthMode());
        if (binaryPath == null || binaryPath.isBlank()) {
            return TestResult.failure("No ap CLI binary path configured.");
        }
        if (!Files.isExecutable(Path.of(binaryPath))) {
            return TestResult.failure("Binary not found or not executable: " + binaryPath);
        }
        try {
            log.info("ap testConnection: calling `ap version`");
            String versionOut = runJsonCommand(List.of("version"), null, VERSION_TIMEOUT_SECONDS);
            log.info("ap testConnection: `ap version` returned ({} chars)", versionOut.length());
        } catch (ApCliException e) {
            log.warn("ap testConnection: `ap version` failed: {}", e.getMessage());
            return TestResult.failure("`ap version` failed: " + e.getMessage());
        }
        try {
            log.info("ap testConnection: calling `ap agent ls`");
            List<ApAgent> agents = listAgents(1);
            log.info("ap testConnection: success, {} agent(s)", agents.size());
            return TestResult.success("Connection OK — " + agents.size() + " agent(s) reachable.");
        } catch (ApCliException e) {
            log.warn("ap testConnection: `ap agent ls` failed: {}", e.getMessage());
            return TestResult.failure("`ap agent ls` failed: " + e.getMessage());
        }
    }

    /**
     * Starts {@code ap run} asynchronously on a pooled thread and streams parsed events
     * to the supplied consumers. Each callback is dispatched on the EDT via {@code invokeLater}
     * so listeners can update Swing components directly.
     *
     * @param prompt        the user prompt
     * @param agentName     agent name or ID (required)
     * @param projectName   project name or ID (required)
     * @param workingDir    project root passed via {@code --working-dir}; may be {@code null}
     * @param onHandle      called once with the initial run metadata (sessionId, openUrl, …)
     * @param onEvent       called for every parsed stream event
     * @param onComplete    called once when the process exits, with the exit code
     */
    public @NotNull ApRunHandleRef startRun(@NotNull String prompt,
                                             @NotNull String agentName,
                                             @NotNull String projectName,
                                             @Nullable String workingDir,
                                             @NotNull Consumer<ApRunHandle> onHandle,
                                             @NotNull Consumer<ApRunEvent> onEvent,
                                             @NotNull Consumer<RunCompletion> onComplete) {
        ApRunHandleRef ref = new ApRunHandleRef();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<String> args = new ArrayList<>();
            args.add("run");
            args.add(prompt);
            args.add("--agent");
            args.add(agentName);
            args.add("--project");
            args.add(projectName);
            args.add("--json");

            List<String> command = buildCommand(args, workingDir);
            log.info("ap run starting: agent={} project={} workingDir={}", agentName, projectName, workingDir);

            Process process;
            try {
                process = buildProcess(command).start();
            } catch (IOException e) {
                log.warn("Failed to start ap run: {}", e.getMessage());
                dispatch(() -> onComplete.accept(RunCompletion.failure("Failed to start ap: " + e.getMessage())));
                return;
            }
            ref.process = process;

            List<String> stderrLines = new ArrayList<>();
            Thread stderrThread = new Thread(() -> drainStderr(process, stderrLines), "ap-run-stderr");
            stderrThread.setDaemon(true);
            stderrThread.start();

            boolean handleEmitted = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node;
                    try {
                        node = MAPPER.readTree(line);
                    } catch (IOException parseEx) {
                        log.debug("Non-JSON line from ap run, skipping: {}", line);
                        continue;
                    }
                    if (!handleEmitted && node.has("session_id")) {
                        ApRunHandle handle = new ApRunHandle(
                                node.path("session_id").asText(null),
                                node.path("agent_id").asText(null),
                                node.path("agent_name").asText(null),
                                node.path("project_id").asText(null),
                                node.path("open_url").asText(null));
                        handleEmitted = true;
                        dispatch(() -> onHandle.accept(handle));
                        continue;
                    }
                    ApRunEvent event = parseEvent(node, line);
                    dispatch(() -> onEvent.accept(event));
                }
            } catch (IOException e) {
                log.warn("ap run stdout reader error: {}", e.getMessage());
            }

            int exitCode;
            try {
                exitCode = process.waitFor();
                stderrThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exitCode = -1;
            }
            String stderr = String.join("\n", stderrLines).trim();
            int finalExit = exitCode;
            dispatch(() -> onComplete.accept(
                    finalExit == 0
                            ? RunCompletion.success()
                            : RunCompletion.failure("ap run exited " + finalExit
                                    + (stderr.isEmpty() ? "" : ": " + stderr))));
        });

        return ref;
    }

    private @NotNull String runJsonCommand(@NotNull List<String> apArgs,
                                            @Nullable String workingDir,
                                            long timeoutSeconds) throws ApCliException {
        List<String> command = buildCommand(apArgs, workingDir);
        log.info("ap exec: {} (timeout={}s)", command, timeoutSeconds);
        ProcessBuilder pb = buildProcess(command);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ApCliException("Failed to spawn ap: " + e.getMessage(), e);
        }

        // Drain both pipes concurrently to avoid pipe-buffer deadlock: if stderr fills
        // its 64KB buffer while we're still reading stdout, the child blocks on its
        // stderr write and we wait forever on readAllBytes().
        StringBuilder stdoutSink = new StringBuilder();
        StringBuilder stderrSink = new StringBuilder();
        Thread outThread = new Thread(() -> drain(process.getInputStream(), stdoutSink), "ap-cli-stdout");
        Thread errThread = new Thread(() -> drain(process.getErrorStream(), stderrSink), "ap-cli-stderr");
        outThread.setDaemon(true);
        errThread.setDaemon(true);
        outThread.start();
        errThread.start();

        boolean exited;
        try {
            exited = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ApCliException("ap command interrupted: " + e.getMessage(), e);
        }
        if (!exited) {
            process.destroyForcibly();
            log.warn("ap exec timed out: {}", command);
            throw new ApCliException("ap command timed out after " + timeoutSeconds + "s");
        }
        try {
            outThread.join(2000);
            errThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exit = process.exitValue();
        log.info("ap exec exited: code={} stdout={}c stderr={}c", exit, stdoutSink.length(), stderrSink.length());
        if (exit != 0) {
            String detail = stderrSink.length() == 0 ? stdoutSink.toString() : stderrSink.toString();
            throw new ApCliException("ap exited " + exit + ": " + detail.trim());
        }
        return stdoutSink.toString();
    }

    private static void drain(@NotNull java.io.InputStream in, @NotNull StringBuilder sink) {
        try (java.io.Reader r = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) {
                sink.append(buf, 0, n);
            }
        } catch (IOException ignored) {
            // Stream closed when the process exits — expected.
        }
    }

    /** Builds the full command-line: [binary, --working-dir, dir?, …apArgs]. */
    @NotNull List<String> buildCommand(@NotNull List<String> apArgs, @Nullable String workingDir) {
        String binaryPath = DevoxxGenieStateService.getInstance().getApCliPath();
        if (binaryPath == null || binaryPath.isBlank()) {
            throw new IllegalStateException("ap CLI binary path is not configured in DevoxxGenie settings.");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(binaryPath);
        if (workingDir != null && !workingDir.isBlank()) {
            cmd.add("--working-dir");
            cmd.add(workingDir);
        }
        cmd.addAll(apArgs);
        return cmd;
    }

    private @NotNull ProcessBuilder buildProcess(@NotNull List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        applyAuthEnv(pb.environment());
        return pb;
    }

    /**
     * Applies the env vars dictated by the configured {@link ApAuthMode}.
     * In {@link ApAuthMode#CACHED_LOGIN} the plugin sets nothing — the CLI does its own
     * detection of cached TUI tokens or Docker Desktop, whichever it finds first.
     */
    void applyAuthEnv(@NotNull Map<String, String> env) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        ApAuthMode mode = ApAuthMode.fromName(state.getApAuthMode());

        if (mode.getEnvValue() != null) {
            env.put(ENV_AUTH_MODE, mode.getEnvValue());
        }
        if (mode == ApAuthMode.MANUAL_TOKENS) {
            String access = state.getApAccessToken();
            String refresh = state.getApRefreshToken();
            if (access != null && !access.isBlank()) env.put(ENV_ACCESS_TOKEN, access);
            if (refresh != null && !refresh.isBlank()) env.put(ENV_REFRESH_TOKEN, refresh);
        }
    }

    @NotNull <T> List<T> parseList(@NotNull String json,
                                    @NotNull TypeReference<List<T>> typeRef) throws ApCliException {
        if (json.isBlank()) return Collections.emptyList();
        try {
            List<T> result = MAPPER.readValue(json, typeRef);
            return result != null ? result : Collections.emptyList();
        } catch (IOException e) {
            throw new ApCliException("Failed to parse ap JSON: " + e.getMessage(), e);
        }
    }

    @NotNull ApRunEvent parseEvent(@NotNull JsonNode node, @NotNull String rawJson) {
        String type = node.path("type").asText("");
        if ("agent_output".equals(type)) {
            return new ApRunEvent.AgentOutput(
                    node.path("agent").asText(null),
                    node.path("content").asText(""),
                    node.path("reasoning").asBoolean(false));
        }
        if ("stream_started".equals(type)) {
            return new ApRunEvent.StreamStarted(node.path("agent").asText(null));
        }
        if ("stream_stopped".equals(type)) {
            return new ApRunEvent.StreamStopped();
        }
        return new ApRunEvent.Other(type, rawJson);
    }

    private void drainStderr(@NotNull Process process, @NotNull List<String> sink) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.add(line);
            }
        } catch (IOException e) {
            log.debug("ap run stderr reader closed: {}", e.getMessage());
        }
    }

    private void dispatch(@NotNull Runnable r) {
        // ModalityState.any() so streaming-event callbacks still flow if the user happens
        // to have a modal dialog (e.g. Settings) open while a run is in progress.
        ApplicationManager.getApplication().invokeLater(r, ModalityState.any());
    }

    /** Result of {@link #testConnection()}. */
    public record TestResult(boolean ok, String message) {
        public static @NotNull TestResult success(@NotNull String message) {
            return new TestResult(true, message);
        }
        public static @NotNull TestResult failure(@NotNull String message) {
            return new TestResult(false, message);
        }
    }

    /** Terminal payload delivered to the {@code onComplete} consumer of {@link #startRun}. */
    public record RunCompletion(boolean ok, @Nullable String error) {
        public static @NotNull RunCompletion success() { return new RunCompletion(true, null); }
        public static @NotNull RunCompletion failure(@NotNull String error) { return new RunCompletion(false, error); }
    }

    /**
     * Caller-side handle for an active {@code ap run}.
     * {@link #cancel()} destroys the underlying process if it is still running;
     * the {@code onComplete} callback will fire afterwards with a non-zero exit.
     */
    public static final class ApRunHandleRef {
        volatile Process process;

        public synchronized void cancel() {
            Process p = process;
            if (p != null && p.isAlive()) {
                p.destroy();
            }
        }

        public boolean isAlive() {
            Process p = process;
            return p != null && p.isAlive();
        }
    }
}
