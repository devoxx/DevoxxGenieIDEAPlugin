package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.agent.TestResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static com.devoxx.genie.model.Constant.TEST_EXECUTION_DEFAULT_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunTestsToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private RunTestsToolExecutor executor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");
        when(stateService.getTestExecutionTimeoutSeconds()).thenReturn(30);
        when(stateService.getTestExecutionEnabled()).thenReturn(true);

        when(project.getBasePath()).thenReturn(tempDir.toString());
        executor = new RunTestsToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        stateServiceMock.close();
    }

    // --- execute() tests ---

    @Test
    void execute_noBuildSystem_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Could not determine test command");
    }

    @Test
    void execute_withCustomCommand_runsCommand() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo custom_test_output");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).containsAnyOf("PASSED", "custom_test_output");
    }

    @Test
    void execute_withCustomCommandAndTarget_replacesPlaceholder() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("printf '%s' '{target}'");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{\"test_target\": \"MyTest\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("PASSED");
        assertThat(result).doesNotContain("{target}");
    }

    @Test
    void execute_withWorkingDir_usesSpecifiedDirectory() {
        // Create a subdirectory
        File subDir = new File(tempDir.toFile(), "submodule");
        subDir.mkdirs();

        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo test_in_subdir");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{\"working_dir\": \"submodule\"}")
                .build();

        String result = executor.execute(request, null);
        // Should not error — the working dir exists
        assertThat(result).doesNotContain("Error");
    }

    @Test
    void execute_processThrowsIOException_returnsError() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo test");

        RunTestsToolExecutor failingExecutor = new RunTestsToolExecutor(project) {
            @Override
            Process createProcess(String command, File workingDir) throws IOException {
                throw new IOException("Process start failed");
            }
        };

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = failingExecutor.execute(request, null);
        assertThat(result).contains("Error").contains("Failed to execute tests").contains("Process start failed");
    }

    @Test
    void execute_processTimesOut_returnsTimeoutResult() throws Exception {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo test");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("partial output\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false);

        RunTestsToolExecutor timeoutExecutor = new RunTestsToolExecutor(project) {
            @Override
            Process createProcess(String command, File workingDir) {
                return mockProcess;
            }
        };

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = timeoutExecutor.execute(request, null);
        assertThat(result).contains("timed out").contains("30 seconds");
        assertThat(result).contains("partial output");
        verify(mockProcess).destroyForcibly();
    }

    @Test
    void execute_processCompletesSuccessfully_returnsFormattedResult() throws Exception {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo test");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("all tests passed\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        RunTestsToolExecutor successExecutor = new RunTestsToolExecutor(project) {
            @Override
            Process createProcess(String command, File workingDir) {
                return mockProcess;
            }
        };

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = successExecutor.execute(request, null);
        assertThat(result).contains("PASSED");
    }

    @Test
    void execute_processFailsWithNonZeroExit_includesOutput() throws Exception {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo test");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("FAILURE: some test failed\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1);

        RunTestsToolExecutor failExecutor = new RunTestsToolExecutor(project) {
            @Override
            Process createProcess(String command, File workingDir) {
                return mockProcess;
            }
        };

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = failExecutor.execute(request, null);
        // Non-zero exit → FAILED status → output is included
        assertThat(result).contains("Output:");
        assertThat(result).contains("some test failed");
    }

    // --- resolveTestCommand() tests ---

    @Test
    void resolveTestCommand_customCommand_returnsIt() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("./run-tests.sh");

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).isEqualTo("./run-tests.sh");
    }

    @Test
    void resolveTestCommand_customCommandWithTarget_replacesPlaceholder() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("./test.sh {target}");

        String result = executor.resolveTestCommand(tempDir.toString(), "MyTest");
        assertThat(result).isEqualTo("./test.sh MyTest");
    }

    @Test
    void resolveTestCommand_customCommandNoTarget_removesPlaceholder() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("./test.sh {target}");

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).isEqualTo("./test.sh ");
    }

    @Test
    void resolveTestCommand_customCommandBlankTarget_removesPlaceholder() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("./test.sh {target}");

        String result = executor.resolveTestCommand(tempDir.toString(), "   ");
        assertThat(result).isEqualTo("./test.sh ");
    }

    @Test
    void resolveTestCommand_noBuildSystem_returnsNull() {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).isNull();
    }

    @Test
    void resolveTestCommand_gradleProject_returnsGradleCommand() throws IOException {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");
        new File(tempDir.toFile(), "build.gradle").createNewFile();
        // Create executable gradlew
        File gradlew = new File(tempDir.toFile(), "gradlew");
        gradlew.createNewFile();
        gradlew.setExecutable(true);

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).contains("gradlew").contains("test");
    }

    @Test
    void resolveTestCommand_gradleWithNonExecutableWrapper_usesBash() throws IOException {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");
        new File(tempDir.toFile(), "build.gradle").createNewFile();
        File gradlew = new File(tempDir.toFile(), "gradlew");
        gradlew.createNewFile();
        gradlew.setExecutable(false);

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).startsWith("bash ");
    }

    @Test
    void resolveTestCommand_mavenProject_returnsMavenCommand() throws IOException {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");
        new File(tempDir.toFile(), "pom.xml").createNewFile();

        String result = executor.resolveTestCommand(tempDir.toString(), null);
        assertThat(result).contains("mvn").contains("test");
    }

    // --- determineWorkingDirectory() tests ---

    @Test
    void determineWorkingDirectory_null_returnsBasePath() {
        File result = executor.determineWorkingDirectory(null);
        assertThat(result.getAbsolutePath()).isEqualTo(new File(tempDir.toString()).getAbsolutePath());
    }

    @Test
    void determineWorkingDirectory_blank_returnsBasePath() {
        File result = executor.determineWorkingDirectory("   ");
        assertThat(result.getAbsolutePath()).isEqualTo(new File(tempDir.toString()).getAbsolutePath());
    }

    @Test
    void determineWorkingDirectory_subDir_returnsSubPath() {
        File result = executor.determineWorkingDirectory("submodule");
        assertThat(result.getAbsolutePath()).endsWith("submodule");
        assertThat(result.getParentFile().getAbsolutePath())
                .isEqualTo(new File(tempDir.toString()).getAbsolutePath());
    }

    // --- getTimeout() tests ---

    @Test
    void getTimeout_configuredValue_returnsIt() {
        when(stateService.getTestExecutionTimeoutSeconds()).thenReturn(60);

        int result = executor.getTimeout();
        assertThat(result).isEqualTo(60);
    }

    @Test
    void getTimeout_nullValue_returnsDefault() {
        when(stateService.getTestExecutionTimeoutSeconds()).thenReturn(null);

        int result = executor.getTimeout();
        assertThat(result).isEqualTo(TEST_EXECUTION_DEFAULT_TIMEOUT);
    }

    // --- formatResult() tests ---

    @Test
    void formatResult_passed_returnsOnlySummary() {
        TestResult result = TestResult.builder()
                .status(TestResult.Status.PASSED)
                .exitCode(0)
                .summary("Test Result: PASSED (10 tests)")
                .rawOutput("")
                .failedTestNames(List.of())
                .build();

        String formatted = executor.formatResult(result, "full output here");
        assertThat(formatted).isEqualTo("Test Result: PASSED (10 tests)");
        assertThat(formatted).doesNotContain("Output:");
    }

    @Test
    void formatResult_failed_includesOutput() {
        TestResult result = TestResult.builder()
                .status(TestResult.Status.FAILED)
                .exitCode(1)
                .summary("Test Result: FAILED (8 passed, 2 failed)")
                .rawOutput("")
                .failedTestNames(List.of("testA", "testB"))
                .build();

        String formatted = executor.formatResult(result, "failure details here");
        assertThat(formatted).contains("Test Result: FAILED");
        assertThat(formatted).contains("Output:");
        assertThat(formatted).contains("failure details here");
    }

    @Test
    void formatResult_error_includesOutput() {
        TestResult result = TestResult.builder()
                .status(TestResult.Status.ERROR)
                .exitCode(2)
                .summary("Test Result: ERROR")
                .rawOutput("")
                .failedTestNames(List.of())
                .build();

        String formatted = executor.formatResult(result, "error output");
        assertThat(formatted).contains("Output:");
        assertThat(formatted).contains("error output");
    }

    @Test
    void formatResult_timeout_includesOutput() {
        TestResult result = TestResult.builder()
                .status(TestResult.Status.TIMEOUT)
                .exitCode(-1)
                .summary("Test Result: TIMEOUT")
                .rawOutput("")
                .failedTestNames(List.of())
                .build();

        String formatted = executor.formatResult(result, "partial output");
        assertThat(formatted).contains("Output:");
        assertThat(formatted).contains("partial output");
    }

    // --- formatTimeoutResult() tests ---

    @Test
    void formatTimeoutResult_includesTimeoutMessage() {
        String result = executor.formatTimeoutResult("partial test output", 120);
        assertThat(result).contains("timed out").contains("120 seconds");
        assertThat(result).contains("Partial output:");
        assertThat(result).contains("partial test output");
    }

    @Test
    void formatTimeoutResult_withNullOutput_handlesGracefully() {
        String result = executor.formatTimeoutResult(null, 30);
        assertThat(result).contains("timed out").contains("30 seconds");
    }

    // --- truncate() tests ---

    @Test
    void truncate_shortText_returnsUnchanged() {
        String text = "short text";
        assertThat(RunTestsToolExecutor.truncate(text)).isEqualTo(text);
    }

    @Test
    void truncate_null_returnsNull() {
        assertThat(RunTestsToolExecutor.truncate(null)).isNull();
    }

    @Test
    void truncate_exactlyMaxLength_returnsUnchanged() {
        String text = "x".repeat(RunTestsToolExecutor.MAX_OUTPUT_LENGTH);
        assertThat(RunTestsToolExecutor.truncate(text)).isEqualTo(text);
    }

    @Test
    void truncate_exceedsMaxLength_truncatesWithMessage() {
        String text = "x".repeat(RunTestsToolExecutor.MAX_OUTPUT_LENGTH + 100);
        String result = RunTestsToolExecutor.truncate(text);
        assertThat(result).hasSize(RunTestsToolExecutor.MAX_OUTPUT_LENGTH + "\n... (output truncated)".length());
        assertThat(result).endsWith("... (output truncated)");
    }

    // --- readProcessOutput() tests ---

    @Test
    void readProcessOutput_readsAllLines() throws IOException {
        Process mockProcess = mock(Process.class);
        String input = "line1\nline2\nline3\n";
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        String result = executor.readProcessOutput(mockProcess);
        assertThat(result).contains("line1").contains("line2").contains("line3");
    }

    @Test
    void readProcessOutput_emptyOutput_returnsEmpty() throws IOException {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        String result = executor.readProcessOutput(mockProcess);
        assertThat(result).isEmpty();
    }

    // --- MAX_OUTPUT_LENGTH constant ---

    @Test
    void maxOutputLength_is50000() {
        assertThat(RunTestsToolExecutor.MAX_OUTPUT_LENGTH).isEqualTo(50_000);
    }
}
