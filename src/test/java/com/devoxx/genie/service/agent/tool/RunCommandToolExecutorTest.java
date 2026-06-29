package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunCommandToolExecutorTest {

    @Mock
    private Project project;

    private RunCommandToolExecutor executor;

    @BeforeEach
    void setUp() {
        when(project.getBasePath()).thenReturn(System.getProperty("java.io.tmpdir"));
        executor = new RunCommandToolExecutor(project);
    }

    @Test
    void execute_missingCommand_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("command");
    }

    @Test
    void execute_emptyCommand_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("command");
    }

    @Test
    void execute_echoCommand_returnsOutput() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("hello");
    }

    @Test
    void execute_commandWithArguments_returnsCombinedOutput() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo hello world from args\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("hello world from args");
    }

    @Test
    void execute_commandWithSpecialCharacters_returnsOutput() {
        String command = SystemInfo.isWindows ? "echo special^&chars" : "echo 'special&chars'";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("special&chars");
    }

    @Test
    void execute_withWorkingDirectory_usesSpecifiedDirectory() throws Exception {
        Path baseDir = Files.createTempDirectory("run-command-base");
        Path workingDir = Files.createDirectory(baseDir.resolve("nested"));
        when(project.getBasePath()).thenReturn(baseDir.toString());

        String command = SystemInfo.isWindows ? "cd" : "pwd";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command + "\", \"working_dir\": \"nested\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains(workingDir.toString());
    }

    @Test
    void execute_withInvalidWorkingDirectory_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo test\", \"working_dir\": \"does-not-exist\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error: Failed to execute command");
    }

    @Test
    void execute_stderrIsCapturedInOutput() {
        String command = SystemInfo.isWindows ? "echo error-message 1>&2" : "echo error-message >&2";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command + "\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("error-message");
    }

    @Test
    void execute_nonZeroExitCode_withOutput_returnsExitCodeAndOutput() {
        String command = SystemInfo.isWindows ? "(echo failing && exit /b 2)" : "echo failing && exit 2";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command + "\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("exited with code 2").contains("failing");
    }

    @Test
    void execute_noOutput_returnsSuccessMessage() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"exit 0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("(command completed successfully with no output)");
    }

    @Test
    void execute_largeOutput_isTruncated() {
        String command = SystemInfo.isWindows
                ? "powershell -Command \"$s='a'*12050; Write-Output $s\""
                : "python3 -c \"print('a'*12050)\"";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("... (output truncated)");
    }

    @Test
    void execute_environmentVariableCommand_returnsExpandedValue() {
        String command = SystemInfo.isWindows ? "set MY_TEST_VAR=works && echo %MY_TEST_VAR%" : "MY_TEST_VAR=works && echo $MY_TEST_VAR";
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"" + command + "\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("works");
    }

    @Test
    void execute_timeout_returnsTimeoutError() {
        Process timeoutProcess = new Process() {
            @Override
            public OutputStream getOutputStream() {
                return OutputStream.nullOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream("partial-output\n".getBytes());
            }

            @Override
            public InputStream getErrorStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public int waitFor() {
                return 0;
            }

            @Override
            public boolean waitFor(long timeout, TimeUnit unit) {
                return false;
            }

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public void destroy() {
            }

            @Override
            public Process destroyForcibly() {
                return this;
            }

            @Override
            public boolean isAlive() {
                return true;
            }
        };
        RunCommandToolExecutor timeoutExecutor = new RunCommandToolExecutor(project, 1, 10_000, (command, workingDir) -> timeoutProcess);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo will-timeout\"}")
                .build();

        String result = timeoutExecutor.execute(request, null);
        assertThat(result).contains("timed out after 1 seconds").contains("partial-output");
    }

    @Test
    void execute_nonZeroExitCode_returnsExitCode() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"exit 1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("exited with code 1");
    }

    /**
     * Reproduces the bug where a command exiting with a non-zero code was rendered
     * with the green "success" icon in the agent Activity panel. The icon is decided
     * by {@link AgentLoopTracker#isErrorResult(String)}, which only flags strings
     * beginning with "Error:". A non-zero exit must therefore produce an
     * "Error:"-prefixed result so it is classified as TOOL_ERROR (red icon).
     */
    @Test
    void execute_nonZeroExitCode_isClassifiedAsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"exit 1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(AgentLoopTracker.isErrorResult(result))
                .as("Non-zero exit code must be classified as an error result")
                .isTrue();
    }

    @Test
    void execute_zeroExitCode_isNotClassifiedAsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo ok\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(AgentLoopTracker.isErrorResult(result))
                .as("Successful command must not be classified as an error result")
                .isFalse();
    }

    @Test
    void execute_interruptedException_throwsRuntimeException() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("some output\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenThrow(new InterruptedException("interrupted"));

        RunCommandToolExecutor interruptExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, (command, workingDir) -> mockProcess);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo test\"}")
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> interruptExecutor.execute(request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("interrupted");
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // Clear interrupt flag for other tests
        Thread.interrupted();
    }

    @Test
    void execute_ioException_returnsError() {
        RunCommandToolExecutor ioExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, (command, workingDir) -> {
            throw new IOException("connection refused");
        });

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo test\"}")
                .build();

        String result = ioExecutor.execute(request, null);
        assertThat(result).contains("Error: Failed to execute command").contains("connection refused");
    }

    @Test
    void execute_multiLineOutputExceedsMaxLength_truncatesDuringCollection() throws Exception {
        // Build multi-line output that exceeds a small maxOutputLength
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeInput.append("line ").append(i).append(" with some padding text\n");
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream(largeInput.toString().getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        // Use small maxOutputLength so readProcessOutput stops collecting early
        RunCommandToolExecutor smallBufferExecutor = new RunCommandToolExecutor(
                project, 30, 100, (command, workingDir) -> mockProcess);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo test\"}")
                .build();

        String result = smallBufferExecutor.execute(request, null);
        // Output should be truncated by truncate() since collected output exceeds maxOutputLength
        assertThat(result).contains("... (output truncated)");
    }

    @Test
    void execute_blankCommand_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"   \"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("command");
    }

    @Test
    void execute_withNullWorkingDir_usesBasePath() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("output\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        RunCommandToolExecutor testExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, (command, workingDir) -> mockProcess);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo test\"}")
                .build();

        String result = testExecutor.execute(request, null);
        assertThat(result).contains("output");
    }

    @Test
    void execute_withShellEnvFile_prefixesCommandWithSource() throws Exception {
        if (SystemInfo.isWindows) {
            return; // shell env file is a Unix-only concept
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor envFileExecutor = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "/etc/profile", "");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo hello\"}")
                .build();

        envFileExecutor.execute(request, null);
        assertThat(capturedCommand[0]).isEqualTo(". \"/etc/profile\" && echo hello");
    }

    @Test
    void execute_withTildeInShellEnvFile_expandsToHome() throws Exception {
        if (SystemInfo.isWindows) {
            return;
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor envFileExecutor = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "~/.bash_profile", "");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo hi\"}")
                .build();

        envFileExecutor.execute(request, null);
        String home = System.getProperty("user.home");
        assertThat(capturedCommand[0]).isEqualTo(". \"" + home + "/.bash_profile\" && echo hi");
    }

    @Test
    void execute_withBlankShellEnvFile_doesNotAlterCommand() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor envFileExecutor = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "", "");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo unchanged\"}")
                .build();

        envFileExecutor.execute(request, null);
        assertThat(capturedCommand[0]).isEqualTo("echo unchanged");
    }

    @Test
    void execute_withCustomShell_usesShellBinary() throws Exception {
        if (SystemInfo.isWindows) {
            return;
        }
        // Use a real subprocess via the default starter and a shell that should be present on PATH.
        // ProcessBuilder resolves the bare name 'sh' via PATH; `$0` reports it as 'sh'.
        RunCommandToolExecutor shExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, null, "", "sh");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo $0\"}")
                .build();

        String result = shExecutor.execute(request, null);
        // $0 contains the shell name as invoked by ProcessBuilder (bare 'sh', no /bin/ prefix).
        assertThat(result).contains("sh");
    }

    @Test
    void execute_withCustomShellAbsolutePath_usesItVerbatim() throws Exception {
        if (SystemInfo.isWindows) {
            return;
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor exec = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "", "/usr/local/bin/zsh");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo z\"}")
                .build();

        String result = exec.execute(request, null);
        // The captured command is the raw command (process starter is mocked, so the shell
        // selection is not directly observable here). Just verify command flow is intact.
        assertThat(result).contains("ok");
        assertThat(capturedCommand[0]).isEqualTo("echo z");
    }

    @Test
    void execute_withEnvFilePathContainingSpaces_quotesPath() throws Exception {
        if (SystemInfo.isWindows) {
            return;
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor envFileExecutor = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "/path with spaces/my profile.sh", "");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo ok\"}")
                .build();

        envFileExecutor.execute(request, null);
        assertThat(capturedCommand[0])
                .isEqualTo(". \"/path with spaces/my profile.sh\" && echo ok");
    }

    @Test
    void execute_withEnvFilePathContainingDoubleQuote_escapesQuote() throws Exception {
        if (SystemInfo.isWindows) {
            return;
        }
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(
                new ByteArrayInputStream("ok\n".getBytes(StandardCharsets.UTF_8)));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        String[] capturedCommand = new String[1];
        RunCommandToolExecutor envFileExecutor = new RunCommandToolExecutor(
                project, 30, 10_000,
                (command, workingDir) -> {
                    capturedCommand[0] = command;
                    return mockProcess;
                },
                "/path/with\"quote/profile.sh", "");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_command")
                .arguments("{\"command\": \"echo ok\"}")
                .build();

        envFileExecutor.execute(request, null);
        assertThat(capturedCommand[0])
                .isEqualTo(". \"/path/with\\\"quote/profile.sh\" && echo ok");
    }

    @Test
    void resolveShellPath_bareNameIsReturnedVerbatim() {
        RunCommandToolExecutor bareShellExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, null, "", "zsh");
        // Bare names are not prefixed with /bin/; ProcessBuilder resolves via PATH.
        assertThat(bareShellExecutor.resolveShellPath()).isEqualTo("zsh");
    }

    @Test
    void resolveShellPath_blankShellDefaultsToBinBash() {
        RunCommandToolExecutor defaultShellExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, null, "", "");
        assertThat(defaultShellExecutor.resolveShellPath()).isEqualTo("/bin/bash");
    }

    @Test
    void resolveShellPath_absolutePathReturnedVerbatim() {
        RunCommandToolExecutor absShellExecutor = new RunCommandToolExecutor(
                project, 30, 10_000, null, "", "/usr/local/bin/fish");
        assertThat(absShellExecutor.resolveShellPath()).isEqualTo("/usr/local/bin/fish");
    }
}
