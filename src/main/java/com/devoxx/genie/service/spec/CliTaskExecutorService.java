package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.CliToolConfig;
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

/**
 * Project-scoped service that runs a CLI tool as an external process.
 * Streams stdout/stderr to the IntelliJ Run tool window console
 * and notifies SpecTaskRunnerService when the process exits.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class CliTaskExecutorService implements Disposable {

    private final Project project;
    private volatile Process activeProcess;

    public CliTaskExecutorService(@NotNull Project project) {
        this.project = project;
    }

    public static CliTaskExecutorService getInstance(@NotNull Project project) {
        return project.getService(CliTaskExecutorService.class);
    }

    /**
     * Execute a CLI tool with the given prompt.
     * Runs the process on a pooled thread, streams output to console,
     * and calls notifyPromptExecutionCompleted() on process exit.
     *
     * @param cliTool the CLI tool configuration
     * @param prompt  the full prompt string to pass to the tool
     * @param taskId  task ID for console header
     * @param taskTitle task title for console header
     */
    public void execute(@NotNull CliToolConfig cliTool,
                        @NotNull String prompt,
                        @NotNull String taskId,
                        @NotNull String taskTitle) {
        CliConsoleManager consoleManager = CliConsoleManager.getInstance(project);
        consoleManager.printTaskHeader(taskId, taskTitle, cliTool.getName());
        consoleManager.activateToolWindow();

        List<String> command = new ArrayList<>(cliTool.buildCommand());

        // If MCP config flag is set, generate the Backlog MCP config and append to command
        String mcpConfigFlag = cliTool.getMcpConfigFlag();
        if (mcpConfigFlag != null && !mcpConfigFlag.isEmpty()) {
            CliToolConfig.CliType cliType = cliTool.getType() != null ? cliTool.getType() : CliToolConfig.CliType.CUSTOM;
            String mcpConfigPath = generateMcpConfig(cliType.getMcpJsonKey());
            if (mcpConfigPath != null) {
                command.add(mcpConfigFlag);
                String pathArg = cliType.isUseAtPrefix() ? "@" + mcpConfigPath : mcpConfigPath;
                command.add(pathArg);
                log.info("CLI MCP config: type={}, flag={}, path={}", cliType, mcpConfigFlag, pathArg);
            }
        }

        // Log command details (prompt piped via stdin)
        log.info("CLI execute: task={}, tool={}, command={}, promptLength={}",
                taskId, cliTool.getName(), command, prompt.length());

        String basePath = project.getBasePath();
        log.info("CLI working directory: {}", basePath);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
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
                activeProcess = pb.start();
                log.info("CLI process started successfully (pid={})", activeProcess.pid());

                // Pipe the prompt to the process's stdin
                try (var writer = new java.io.OutputStreamWriter(
                        activeProcess.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(prompt);
                    writer.flush();
                }
                log.info("CLI prompt written to stdin ({} chars)", prompt.length());

                // Collect stderr lines for error reporting
                List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

                // Stream stdout and stderr concurrently
                Thread stdoutThread = createStreamReader(
                        activeProcess, true, consoleManager, taskId, null);
                Thread stderrThread = createStreamReader(
                        activeProcess, false, consoleManager, taskId, stderrLines);

                stdoutThread.start();
                stderrThread.start();

                log.info("CLI waiting for process to finish (task={})", taskId);
                int exitCode = activeProcess.waitFor();
                long elapsed = System.currentTimeMillis() - startTime;
                stdoutThread.join(5000);
                stderrThread.join(5000);

                activeProcess = null;

                log.info("CLI process exited: task={}, exitCode={}, elapsed={}ms", taskId, exitCode, elapsed);

                String exitMsg = "\n=== Process exited with code " + exitCode + " (after " + elapsed + "ms) ===\n";
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (exitCode == 0) {
                        consoleManager.printSystem(exitMsg);
                        log.info("CLI notifying prompt execution completed for task {}", taskId);
                        SpecTaskRunnerService.getInstance(project).notifyPromptExecutionCompleted();
                    } else {
                        consoleManager.printError(exitMsg);
                        String errorOutput = String.join("\n", stderrLines).trim();
                        log.warn("CLI task {} failed with exit code {}: {}", taskId, exitCode,
                                errorOutput.length() > 300 ? errorOutput.substring(0, 300) + "..." : errorOutput);
                        SpecTaskRunnerService.getInstance(project)
                                .notifyCliTaskFailed(exitCode, errorOutput);
                    }
                });

            } catch (IOException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                activeProcess = null;
                log.error("CLI failed to start process: task={}, elapsed={}ms, error={}",
                        taskId, elapsed, e.getMessage(), e);
                ApplicationManager.getApplication().invokeLater(() -> {
                    consoleManager.printError("Failed to start process: " + e.getMessage());
                    SpecTaskRunnerService.getInstance(project)
                            .notifyCliTaskFailed(-1, e.getMessage());
                });
            } catch (InterruptedException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                activeProcess = null;
                Thread.currentThread().interrupt();
                log.info("CLI process interrupted: task={}, elapsed={}ms", taskId, elapsed);
                ApplicationManager.getApplication().invokeLater(() -> {
                    consoleManager.printSystem("Process interrupted");
                    SpecTaskRunnerService.getInstance(project).notifyPromptExecutionCompleted();
                });
            }
        });

        log.info("CLI execute() returned (process running async) for task {}", taskId);
    }

    /**
     * Cancel the currently running process.
     */
    public void cancelCurrentProcess() {
        Process process = activeProcess;
        if (process != null && process.isAlive()) {
            log.info("Destroying CLI process (pid={})", process.pid());
            process.destroyForcibly();
            activeProcess = null;
            ApplicationManager.getApplication().invokeLater(() ->
                    CliConsoleManager.getInstance(project).printSystem("\n=== Process cancelled ===\n"));
        }
    }

    public boolean isRunning() {
        Process process = activeProcess;
        return process != null && process.isAlive();
    }

    /**
     * Generate a temporary MCP config JSON file for the Backlog MCP server.
     * The file is written to the system temp directory and deleted on JVM exit.
     *
     * @param mcpJsonKey the top-level JSON key (e.g., "mcpServers" or "servers")
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
                                               @Nullable List<String> lineCollector) {
        String streamName = isStdout ? "stdout" : "stderr";
        Thread thread = new Thread(() -> {
            log.debug("CLI {}-reader started for task {}", streamName, taskId);
            int lineCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    isStdout ? process.getInputStream() : process.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    final String text = line;
                    if (lineCollector != null) {
                        lineCollector.add(text);
                    }
                    // Log first 5 lines and every 50th line to IDE log for debugging
                    if (lineCount <= 5 || lineCount % 50 == 0) {
                        log.info("CLI {} [{}] line {}: {}", streamName, taskId, lineCount,
                                text.length() > 200 ? text.substring(0, 200) + "..." : text);
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (isStdout) {
                            consoleManager.printOutput(text);
                        } else {
                            consoleManager.printError(text);
                        }
                    });
                }
            } catch (IOException e) {
                log.debug("CLI {}-reader ended for task {} ({}): {}", streamName, taskId,
                        lineCount > 0 ? lineCount + " lines read" : "no output", e.getMessage());
            }
            log.info("CLI {}-reader finished for task {}: {} lines total", streamName, taskId, lineCount);
        }, "cli-" + streamName + "-reader-" + taskId);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void dispose() {
        cancelCurrentProcess();
    }
}
