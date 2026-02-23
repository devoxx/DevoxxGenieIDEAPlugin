package com.devoxx.genie.service.cli;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.service.cli.command.CliCommand;
import com.devoxx.genie.service.spec.SpecTaskRunnerService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-scoped service that runs CLI tools as external processes.
 * Supports multiple concurrent processes tracked by task ID.
 * Delegates command construction and prompt delivery to the tool's
 * {@link CliCommand} implementation (Command pattern).
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class CliTaskExecutorService implements Disposable {

    private final Project project;

    /** Active CLI tasks indexed by task ID. Supports concurrent execution. */
    private final ConcurrentHashMap<String, ActiveCliTask> activeTasks = new ConcurrentHashMap<>();

    /**
     * Bundles the process, command, and completion-kill flag for a single CLI task.
     */
    private static class ActiveCliTask {
        final Process process;
        final CliCommand command;
        volatile boolean taskCompletedKill = false;

        ActiveCliTask(@NotNull Process process, @NotNull CliCommand command) {
            this.process = process;
            this.command = command;
        }
    }

    public CliTaskExecutorService(@NotNull Project project) {
        this.project = project;
    }

    public static CliTaskExecutorService getInstance(@NotNull Project project) {
        return project.getService(CliTaskExecutorService.class);
    }

    /**
     * Execute a CLI tool with the given prompt.
     * Runs the process on a pooled thread, streams output to console,
     * and calls notifyPromptExecutionCompleted() or notifyCliTaskFailed() on process exit.
     * Multiple tasks can execute concurrently; each is tracked by taskId.
     */
    public void execute(@NotNull CliToolConfig cliTool,
                        @NotNull String prompt,
                        @NotNull String taskId,
                        @NotNull String taskTitle) {
        CliConsoleManager consoleManager = CliConsoleManager.getInstance(project);
        consoleManager.printTaskHeader(taskId, taskTitle, cliTool.getName());
        consoleManager.activateToolWindow();

        CliToolConfig.CliType cliType = resolveCliType(cliTool);
        CliCommand cliCommand = cliType.createCommand();

        // Generate MCP config and let the command build the full process command
        String mcpConfigPath = generateMcpConfig(cliCommand.mcpJsonKey());
        List<String> command = cliCommand.buildProcessCommand(cliTool, prompt, mcpConfigPath);

        log.info("CLI execute: task={}, tool={}, type={}, command={}, promptLength={}",
                taskId, cliTool.getName(), cliType, command, prompt.length());

        final boolean parseClaudeStreamJson = cliType == CliToolConfig.CliType.CLAUDE
                && cliTool.getExtraArgs() != null
                && cliTool.getExtraArgs().stream()
                        .anyMatch(a -> a.contains(ClaudeStreamJsonParser.STREAM_JSON_FLAG));
        if (parseClaudeStreamJson) {
            log.info("CLI task {}: Claude stream-json mode enabled — events will be forwarded to Activity Logs", taskId);
        }

        String basePath = project.getBasePath();
        log.info("CLI working directory: {}", basePath);

        ApplicationManager.getApplication().executeOnPooledThread(
                () -> runCliTaskAsync(cliCommand, cliTool, command, taskId, prompt, basePath,
                        parseClaudeStreamJson, consoleManager));

        log.info("CLI execute() returned (process running async) for task {}", taskId);
    }

    /**
     * Resolves the CLI type for the given tool configuration.
     * Auto-detects the type from the tool name when the stored type is CUSTOM (backwards compat).
     */
    CliToolConfig.CliType resolveCliType(@NotNull CliToolConfig cliTool) {
        CliToolConfig.CliType cliType = cliTool.getType() != null ? cliTool.getType() : CliToolConfig.CliType.CUSTOM;
        if (cliType != CliToolConfig.CliType.CUSTOM) {
            return cliType;
        }
        for (CliToolConfig.CliType t : CliToolConfig.CliType.values()) {
            if (t != CliToolConfig.CliType.CUSTOM &&
                    t.getDisplayName().equalsIgnoreCase(cliTool.getName())) {
                return t;
            }
        }
        return cliType;
    }

    /**
     * Executes the CLI process on the current (pooled) thread.
     * Handles process lifecycle: start, stream I/O, wait for exit, and notify completion.
     */
    private void runCliTaskAsync(@NotNull CliCommand cliCommand,
                                 @NotNull CliToolConfig cliTool,
                                 @NotNull List<String> command,
                                 @NotNull String taskId,
                                 @NotNull String prompt,
                                 @Nullable String basePath,
                                 boolean parseClaudeStreamJson,
                                 @NotNull CliConsoleManager consoleManager) {
        log.info("CLI pooled thread started for task {}", taskId);
        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            if (basePath != null) {
                pb.directory(new java.io.File(basePath));
            }

            // Inherit the user's shell environment (PATH, tokens, etc.)
            // IntelliJ launched from Dock doesn't inherit ~/.zshrc env vars
            pb.environment().putAll(com.intellij.util.EnvironmentUtil.getEnvironmentMap());

            // Overlay with tool-specific env var overrides
            if (cliTool.getEnvVars() != null && !cliTool.getEnvVars().isEmpty()) {
                pb.environment().putAll(cliTool.getEnvVars());
                log.info("CLI env vars overrides: {}", cliTool.getEnvVars().keySet());
            }

            log.info("CLI starting process...");
            Process process = pb.start();
            log.info("CLI process started successfully (pid={})", process.pid());

            // Register the active task
            ActiveCliTask activeTask = new ActiveCliTask(process, cliCommand);
            activeTasks.put(taskId, activeTask);

            // Delegate prompt delivery to the command
            cliCommand.writePrompt(process, prompt);
            log.info("CLI prompt delivered via {} ({} chars)",
                    cliCommand.getClass().getSimpleName(), prompt.length());

            // Collect stderr lines for error reporting
            List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

            // Stream stdout and stderr concurrently
            Thread stdoutThread = createStreamReader(
                    process, true, consoleManager, taskId, null, parseClaudeStreamJson);
            Thread stderrThread = createStreamReader(
                    process, false, consoleManager, taskId, stderrLines, false);

            stdoutThread.start();
            stderrThread.start();

            log.info("CLI waiting for process to finish (task={})", taskId);
            int exitCode = process.waitFor();
            long elapsed = System.currentTimeMillis() - startTime;
            stdoutThread.join(5000);
            stderrThread.join(5000);

            boolean completedKill = activeTask.taskCompletedKill;
            activeTasks.remove(taskId);

            log.info("CLI process exited: task={}, exitCode={}, completedKill={}, elapsed={}ms",
                    taskId, exitCode, completedKill, elapsed);

            String exitMsg = "\n=== Process exited with code " + exitCode + " (after " + elapsed + "ms) ===\n";
            ApplicationManager.getApplication().invokeLater(() -> {
                if (exitCode == 0 || completedKill) {
                    consoleManager.printSystem(exitMsg);
                    log.info("CLI notifying prompt execution completed for task {}", taskId);
                    SpecTaskRunnerService.getInstance(project).notifyPromptExecutionCompleted(taskId);
                } else {
                    consoleManager.printError(exitMsg);
                    String errorOutput = String.join("\n", stderrLines).trim();
                    log.warn("CLI task {} failed with exit code {}: {}", taskId, exitCode,
                            errorOutput.length() > 300 ? errorOutput.substring(0, 300) + "..." : errorOutput);
                    SpecTaskRunnerService.getInstance(project)
                            .notifyCliTaskFailed(exitCode, errorOutput, taskId);
                }
            });

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            activeTasks.remove(taskId);
            log.error("CLI failed to start process: task={}, elapsed={}ms, error={}",
                    taskId, elapsed, e.getMessage(), e);
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleManager.printError("Failed to start process: " + e.getMessage());
                SpecTaskRunnerService.getInstance(project)
                        .notifyCliTaskFailed(-1, e.getMessage(), taskId);
            });
        } catch (InterruptedException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            activeTasks.remove(taskId);
            Thread.currentThread().interrupt();
            log.info("CLI process interrupted: task={}, elapsed={}ms", taskId, elapsed);
            ApplicationManager.getApplication().invokeLater(() -> {
                consoleManager.printSystem("[" + taskId + "] Process interrupted");
                SpecTaskRunnerService.getInstance(project).notifyPromptExecutionCompleted(taskId);
            });
        }
    }

    /**
     * Cancel the process for a specific task (user-initiated or parallel cancellation).
     */
    public void cancelTask(@NotNull String taskId) {
        ActiveCliTask task = activeTasks.remove(taskId);
        if (task != null && task.process.isAlive()) {
            log.info("Destroying CLI process for task {} (pid={})", taskId, task.process.pid());
            task.process.destroyForcibly();
            ApplicationManager.getApplication().invokeLater(() ->
                    CliConsoleManager.getInstance(project).printSystem(
                            "\n=== [" + taskId + "] Process cancelled ===\n"));
        }
    }

    /**
     * Cancel the currently running process (legacy single-task API).
     * When multiple tasks are running, cancels all of them.
     */
    public void cancelCurrentProcess() {
        cancelAllProcesses();
    }

    /**
     * Cancel all active CLI processes.
     */
    public void cancelAllProcesses() {
        List<String> taskIds = new ArrayList<>(activeTasks.keySet());
        for (String taskId : taskIds) {
            cancelTask(taskId);
        }
    }

    /**
     * Called when a specific backlog task is marked Done while the process is still running.
     * Delegates to the active {@link CliCommand#onTaskCompleted(Process)} —
     * only commands that don't self-exit (e.g., Codex) will kill the process.
     */
    public void notifyTaskDone(@NotNull String taskId) {
        ActiveCliTask task = activeTasks.get(taskId);
        if (task != null && task.process.isAlive()) {
            if (task.command.onTaskCompleted(task.process)) {
                task.taskCompletedKill = true;
                log.info("CLI process killed by {} — task {} completed",
                        task.command.getClass().getSimpleName(), taskId);
            }
        }
    }

    /**
     * Legacy single-task notifyTaskDone(). Notifies all active tasks.
     */
    public void notifyTaskDone() {
        for (String taskId : new ArrayList<>(activeTasks.keySet())) {
            notifyTaskDone(taskId);
        }
    }

    /**
     * Returns true if any CLI process is currently running.
     */
    public boolean isRunning() {
        return activeTasks.values().stream().anyMatch(t -> t.process.isAlive());
    }

    /**
     * Returns true if the process for a specific task is currently running.
     */
    public boolean isRunning(@NotNull String taskId) {
        ActiveCliTask task = activeTasks.get(taskId);
        return task != null && task.process.isAlive();
    }

    /**
     * Returns the number of currently active CLI processes.
     */
    public int getActiveCount() {
        return (int) activeTasks.values().stream().filter(t -> t.process.isAlive()).count();
    }

    /**
     * Generate a temporary MCP config JSON file for the Backlog MCP server.
     * The file is written to the system temp directory and deleted on JVM exit.
     */
    private @Nullable String generateMcpConfig(@NotNull String mcpJsonKey) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("backlog-mcp-", ".json");
            tempFile.deleteOnExit();

            String config = "{\n" +
                    "  \"" + mcpJsonKey + "\": {\n" +
                    "    \"backlog\": {\n" +
                    "      \"command\": \"backlog\",\n" +
                    "      \"args\": [\"mcp\", \"start\"]\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";

            java.nio.file.Files.writeString(tempFile.toPath(), config, StandardCharsets.UTF_8);
            log.info("CLI generated MCP config (key={}) at {}", mcpJsonKey, tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            log.error("Failed to generate MCP config: {}", e.getMessage(), e);
            return null;
        }
    }

    private @NotNull Thread createStreamReader(@NotNull Process process,
                                               boolean isStdout,
                                               @NotNull CliConsoleManager consoleManager,
                                               @NotNull String taskId,
                                               @Nullable List<String> lineCollector,
                                               boolean parseClaudeStreamJson) {
        String streamName = isStdout ? "stdout" : "stderr";
        Thread thread = new Thread(
                () -> runStreamReader(process, isStdout, streamName, consoleManager, taskId, lineCollector, parseClaudeStreamJson),
                "cli-" + streamName + "-reader-" + taskId);
        thread.setDaemon(true);
        return thread;
    }

    private void runStreamReader(@NotNull Process process, boolean isStdout, @NotNull String streamName,
                                  @NotNull CliConsoleManager consoleManager, @NotNull String taskId,
                                  @Nullable List<String> lineCollector, boolean parseClaudeStreamJson) {
        log.debug("CLI {}-reader started for task {}", streamName, taskId);
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                isStdout ? process.getInputStream() : process.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                processStreamLine(line, lineCount, isStdout, streamName, taskId, lineCollector, parseClaudeStreamJson, consoleManager);
            }
        } catch (IOException e) {
            String summary = lineCount > 0 ? lineCount + " lines read" : "no output";
            log.debug("CLI {}-reader ended for task {} ({}): {}", streamName, taskId, summary, e.getMessage());
        }
        log.info("CLI {}-reader finished for task {}: {} lines total", streamName, taskId, lineCount);
    }

    private void processStreamLine(@NotNull String line, int lineCount, boolean isStdout,
                                    @NotNull String streamName, @NotNull String taskId,
                                    @Nullable List<String> lineCollector, boolean parseClaudeStreamJson,
                                    @NotNull CliConsoleManager consoleManager) {
        if (lineCollector != null) {
            lineCollector.add(line);
        }
        // Log first 5 lines and every 50th line to IDE log for debugging
        if (lineCount <= 5 || lineCount % 50 == 0) {
            log.info("CLI {} [{}] line {}: {}", streamName, taskId, lineCount,
                    line.length() > 200 ? line.substring(0, 200) + "..." : line);
        }
        // Parse Claude stream-json events and forward to Activity Logs panel
        if (parseClaudeStreamJson && line.startsWith("{")) {
            publishClaudeStreamJsonEvents(line);
        }
        // Prefix with task ID for parallel execution traceability
        final boolean isParallel = activeTasks.size() > 1;
        ApplicationManager.getApplication().invokeLater(() -> {
            String displayText = isParallel ? "[" + taskId + "] " + line : line;
            if (isStdout) {
                consoleManager.printOutput(displayText);
            } else {
                consoleManager.printError(displayText);
            }
        });
    }

    /**
     * Parse a Claude CLI stream-json line and publish each resulting {@link AgentMessage}
     * to the application message bus so that {@code AgentMcpLogPanel} can display it.
     * Called from a background (pooled) thread; message bus delivery is thread-safe.
     */
    private void publishClaudeStreamJsonEvents(@NotNull String jsonLine) {
        ClaudeStreamJsonParser.parse(jsonLine, project.getLocationHash()).ifPresent(messages -> {
            ActivityLoggingMessage publisher = ApplicationManager.getApplication()
                    .getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG);
            for (AgentMessage msg : messages) {
                try {
                    publisher.onActivityMessage(ActivityMessage.fromAgent(msg));
                } catch (Exception e) {
                    log.debug("Failed to publish Claude stream-json event: {}", e.getMessage());
                }
            }
        });
    }

    @Override
    public void dispose() {
        cancelAllProcesses();
    }
}
