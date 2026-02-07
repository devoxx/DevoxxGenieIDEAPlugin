package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RunCommandToolExecutor implements ToolExecutor {

    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_LENGTH = 10000;

    private final Project project;

    public RunCommandToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String command = validateAndGetCommand(request);
            String workingDir = ToolArgumentParser.getString(request.arguments(), "working_dir");

            Process process = createProcess(command, workingDir);

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return formatTimeoutError(output);
            }

            int exitCode = process.exitValue();
            return formatResult(exitCode, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Command execution was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing command", e);
            return "Error: Failed to execute command - " + e.getMessage();
        }
    }

    private String validateAndGetCommand(ToolExecutionRequest request) {
        String command = ToolArgumentParser.getString(request.arguments(), "command");
        if (command == null || command.isBlank()) {
            return "Error: 'command' parameter is required.";
        }
        return command;
    }

    private Process createProcess(String command, String workingDir) throws IOException {
        ProcessBuilder processBuilder = createProcessBuilder(command);

        File dir = determineWorkingDirectory(workingDir);
        processBuilder.directory(dir);
        processBuilder.redirectErrorStream(true);

        return processBuilder.start();
    }

    private ProcessBuilder createProcessBuilder(String command) {
        if (SystemInfo.isWindows) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            return new ProcessBuilder("/bin/bash", "-c", command);
        }
    }

    private File determineWorkingDirectory(String workingDir) {
        if (workingDir != null && !workingDir.isBlank()) {
            return new File(project.getBasePath(), workingDir);
        }
        return new File(Objects.requireNonNull(project.getBasePath()));
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < MAX_OUTPUT_LENGTH) {
                    output.append(line).append("\n");
                }
            }
        }
        return output.toString();
    }

    private String formatTimeoutError(String output) {
        return "Error: Command timed out after " + TIMEOUT_SECONDS + " seconds.\nPartial output:\n" + truncate(output);
    }

    private String formatResult(int exitCode, String output) {
        String result = truncate(output);
        if (exitCode == 0) {
            return result.isEmpty() ? "(command completed successfully with no output)" : result;
        } else {
            return "Exit code: " + exitCode + "\n" + result;
        }
    }

    private String truncate(String text) {
        if (text.length() > MAX_OUTPUT_LENGTH) {
            return text.substring(0, MAX_OUTPUT_LENGTH) + "\n... (output truncated)";
        }
        return text;
    }
}