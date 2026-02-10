package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.agent.TestResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.devoxx.genie.model.Constant.TEST_EXECUTION_DEFAULT_TIMEOUT;

@Slf4j
public class RunTestsToolExecutor implements ToolExecutor {

    private static final int MAX_OUTPUT_LENGTH = 50_000;

    private final Project project;

    public RunTestsToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String testTarget = ToolArgumentParser.getString(request.arguments(), "test_target");
            String workingDir = ToolArgumentParser.getString(request.arguments(), "working_dir");

            File dir = determineWorkingDirectory(workingDir);
            String command = resolveTestCommand(dir.getAbsolutePath(), testTarget);

            if (command == null || command.isBlank()) {
                return "Error: Could not determine test command. No recognized build system found " +
                        "and no custom test command configured in Settings → Agent → Test Execution.";
            }

            int timeoutSeconds = getTimeout();
            log.info("Running tests: {} (timeout: {}s, dir: {})", command, timeoutSeconds, dir);

            Process process = createProcess(command, dir);
            String output = readProcessOutput(process);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return formatTimeoutResult(output, timeoutSeconds);
            }

            int exitCode = process.exitValue();
            BuildSystemDetector.BuildSystem buildSystem =
                    BuildSystemDetector.detect(dir.getAbsolutePath());
            TestResult result = TestResultParser.parse(output, buildSystem, exitCode);

            return formatResult(result, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test execution was interrupted", e);
        } catch (Exception e) {
            log.error("Error executing tests", e);
            return "Error: Failed to execute tests - " + e.getMessage();
        }
    }

    private String resolveTestCommand(@NotNull String basePath, String testTarget) {
        // Custom command takes priority
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String customCommand = state.getTestExecutionCustomCommand();
        if (customCommand != null && !customCommand.isBlank()) {
            if (testTarget != null && !testTarget.isBlank()) {
                return customCommand.replace("{target}", testTarget);
            }
            return customCommand.replace("{target}", "");
        }

        // Auto-detect build system
        BuildSystemDetector.BuildSystem buildSystem = BuildSystemDetector.detect(basePath);
        if (buildSystem == BuildSystemDetector.BuildSystem.UNKNOWN) {
            return null;
        }

        List<String> commandParts = BuildSystemDetector.getTestCommand(
                buildSystem, testTarget, SystemInfo.isWindows);

        if (commandParts.isEmpty()) {
            return null;
        }

        // Handle Gradle wrapper permissions on Unix
        if (buildSystem == BuildSystemDetector.BuildSystem.GRADLE && !SystemInfo.isWindows) {
            File gradlew = new File(basePath, "gradlew");
            if (gradlew.exists() && !gradlew.canExecute()) {
                // Use bash to run non-executable gradlew
                return "bash " + BuildSystemDetector.buildCommandString(commandParts)
                        .replace("./gradlew", "gradlew");
            }
        }

        return BuildSystemDetector.buildCommandString(commandParts);
    }

    private Process createProcess(String command, File workingDir) throws IOException {
        ProcessBuilder processBuilder;
        if (SystemInfo.isWindows) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
        }
        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
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

    private int getTimeout() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        Integer timeout = state.getTestExecutionTimeoutSeconds();
        return timeout != null ? timeout : TEST_EXECUTION_DEFAULT_TIMEOUT;
    }

    private String formatTimeoutResult(String output, int timeoutSeconds) {
        TestResult result = TestResult.builder()
                .status(TestResult.Status.TIMEOUT)
                .exitCode(-1)
                .summary("Test execution timed out after " + timeoutSeconds + " seconds.")
                .rawOutput(truncate(output))
                .failedTestNames(List.of())
                .build();

        return result.getSummary() + "\n\nPartial output:\n" + truncate(output);
    }

    private @NotNull String formatResult(@NotNull TestResult result, @NotNull String fullOutput) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSummary());

        // For failures, include relevant output to help the LLM diagnose
        if (result.getStatus() != TestResult.Status.PASSED) {
            sb.append("\nOutput:\n");
            sb.append(truncate(fullOutput));
        }

        return sb.toString();
    }

    private String truncate(String text) {
        if (text != null && text.length() > MAX_OUTPUT_LENGTH) {
            return text.substring(0, MAX_OUTPUT_LENGTH) + "\n... (output truncated)";
        }
        return text;
    }
}
