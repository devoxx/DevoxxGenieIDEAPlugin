package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RunCommandToolExecutor implements ToolExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_OUTPUT_LENGTH = 10000;
    private static final long OUTPUT_DRAIN_GRACE_MILLIS = 1_000;
    private static final File NULL_DEVICE = new File(SystemInfo.isWindows ? "NUL" : "/dev/null");

    private final Project project;
    private final int timeoutSeconds;
    private final int maxOutputLength;
    private final ProcessStarter processStarter;
    private final ExecutorService executor;
    private final String shellEnvFile;
    private final String shell;

    public RunCommandToolExecutor(@NotNull Project project) {
        this(project, DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_OUTPUT_LENGTH);
    }

    RunCommandToolExecutor(@NotNull Project project, int timeoutSeconds, int maxOutputLength) {
        this(project, timeoutSeconds, maxOutputLength, null);
    }

    RunCommandToolExecutor(@NotNull Project project, int timeoutSeconds, int maxOutputLength, ProcessStarter processStarter) {
        this(project, timeoutSeconds, maxOutputLength, processStarter, readShellEnvFileSetting(), readShellSetting());
    }

    RunCommandToolExecutor(@NotNull Project project,
                           int timeoutSeconds,
                           int maxOutputLength,
                           @Nullable ProcessStarter processStarter,
                           @Nullable String shellEnvFile,
                           @Nullable String shell) {
        this.project = project;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputLength = maxOutputLength;
        this.processStarter = processStarter != null ? processStarter : this::createProcess;
        this.shellEnvFile = shellEnvFile != null ? shellEnvFile : "";
        this.executor = outputReaderExecutor();
        this.shell = shell != null ? shell : "";
    }

    private static ExecutorService outputReaderExecutor() {
        try {
            return ThreadPoolManager.getInstance().getPromptExecutionPool();
        } catch (Exception e) {
            return ForkJoinPool.commonPool();
        }
    }

    private static String readShellEnvFileSetting() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            return state != null && state.getAgentShellEnvFile() != null ? state.getAgentShellEnvFile() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String readShellSetting() {
        try {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            return state != null && state.getAgentShell() != null ? state.getAgentShell() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String command = validateAndGetCommand(request);
            String workingDir = ToolArgumentParser.getString(request.arguments(), "working_dir");

            String effectiveCommand = applyShellEnvFile(command);

            Process process = processStarter.start(effectiveCommand, workingDir);

            StringBuffer outputSoFar = new StringBuffer();
            Future<String> output = executor.submit(() -> readProcessOutput(process, outputSoFar));

            boolean finished = exitedWithinTimeout(process);
            if (!finished) {
                destroyWithDescendants(process);
                return formatTimeoutError(awaitOutput(output, outputSoFar));
            }

            int exitCode = process.exitValue();
            return formatResult(exitCode, awaitOutput(output, outputSoFar));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Command execution was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing command", e);
            return "Error: Failed to execute command - " + e.getMessage();
        }
    }

    private boolean exitedWithinTimeout(Process process) throws InterruptedException {
        try {
            return process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            destroyWithDescendants(process);
            throw e;
        }
    }

    private static void destroyWithDescendants(Process process) {
        try {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (UnsupportedOperationException e) {
            log.debug("Process does not expose its descendants", e);
        }
        process.destroyForcibly();
    }

    private static String awaitOutput(Future<String> output, StringBuffer outputSoFar)
            throws InterruptedException, ExecutionException {
        try {
            return output.get(OUTPUT_DRAIN_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return outputSoFar.toString();
        }
    }

    private String validateAndGetCommand(ToolExecutionRequest request) {
        String command = ToolArgumentParser.getString(request.arguments(), "command");
        if (command == null || command.isBlank()) {
            return "Error: 'command' parameter is required.";
        }
        return command;
    }

    private String applyShellEnvFile(String command) {
        if (shellEnvFile == null || shellEnvFile.isBlank() || SystemInfo.isWindows) {
            return command;
        }
        String expanded = expandTilde(shellEnvFile.trim());
        // Use POSIX '.' (dot) instead of 'source' for portability with /bin/sh (dash on Debian/Ubuntu).
        // Quote the path and escape embedded double quotes so paths with spaces work.
        return ". \"" + expanded.replace("\"", "\\\"") + "\" && " + command;
    }

    private String expandTilde(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        if (path.equals("~")) {
            return System.getProperty("user.home");
        }
        return path;
    }

    private Process createProcess(String command, String workingDir) throws IOException {
        ProcessBuilder processBuilder = createProcessBuilder(command);

        File dir = determineWorkingDirectory(workingDir);
        processBuilder.directory(dir);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectInput(ProcessBuilder.Redirect.from(NULL_DEVICE));

        return processBuilder.start();
    }

    private ProcessBuilder createProcessBuilder(String command) {
        if (SystemInfo.isWindows) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        }
        String shellPath = resolveShellPath();
        return new ProcessBuilder(shellPath, "-c", command);
    }

    String resolveShellPath() {
        if (shell == null || shell.isBlank()) {
            return "/bin/bash";
        }
        String trimmed = shell.trim();
        // If a path is provided, use it verbatim. Otherwise pass the bare name to
        // ProcessBuilder and let the OS PATH resolve it (so /usr/bin/fish, /usr/bin/ksh,
        // and homebrew shells work without manual prefixing).
        return trimmed;
    }

    private File determineWorkingDirectory(String workingDir) {
        if (workingDir != null && !workingDir.isBlank()) {
            return new File(project.getBasePath(), workingDir);
        }
        return new File(Objects.requireNonNull(project.getBasePath()));
    }

    private String readProcessOutput(Process process, StringBuffer output) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < maxOutputLength) {
                    output.append(line).append("\n");
                }
            }
        }
        return output.toString();
    }

    private String formatTimeoutError(String output) {
        return "Error: Command timed out after " + timeoutSeconds + " seconds.\nPartial output:\n" + truncate(output);
    }

    private String formatResult(int exitCode, String output) {
        String result = truncate(output);
        if (exitCode == 0) {
            return result.isEmpty() ? "(command completed successfully with no output)" : result;
        } else {
            // Prefix with "Error:" so AgentLoopTracker.isErrorResult() classifies a non-zero
            // exit as TOOL_ERROR (red icon) instead of a green "success" check in the Activity
            // panel. The exit code is kept in the message for the LLM's benefit.
            return "Error: Command exited with code " + exitCode + "\n" + result;
        }
    }

    private String truncate(String text) {
        if (text.length() > maxOutputLength) {
            return text.substring(0, maxOutputLength) + "\n... (output truncated)";
        }
        return text;
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(String command, String workingDir) throws IOException;
    }
}
