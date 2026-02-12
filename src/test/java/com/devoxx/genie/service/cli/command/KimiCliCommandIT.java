package com.devoxx.genie.service.cli.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies Kimi CLI can be launched via ProcessBuilder
 * with the --prompt flag. Requires Kimi installed at /Users/stephan/.local/bin/kimi.
 */
class KimiCliCommandIT {

    private static final String KIMI_PATH = "/Users/stephan/.local/bin/kimi";

    static boolean kimiInstalled() {
        return new java.io.File(KIMI_PATH).canExecute();
    }

    @Test
    @EnabledIf("kimiInstalled")
    void testBuildProcessCommand() {
        KimiCliCommand command = new KimiCliCommand();
        CliToolConfig config = CliToolConfig.builder()
                .type(CliToolConfig.CliType.KIMI)
                .executablePath(KIMI_PATH)
                .extraArgs(List.of("--yolo"))
                .mcpConfigFlag("--mcp-config-file")
                .build();

        List<String> cmd = command.buildProcessCommand(config, "say hello", "/tmp/mcp.json");

        assertThat(cmd).containsExactly(
                KIMI_PATH,
                "--yolo",
                "--mcp-config-file", "/tmp/mcp.json",
                "--prompt", "say hello"
        );
    }

    @Test
    @EnabledIf("kimiInstalled")
    void testKimiProcessStartsWithPromptFlag() throws Exception {
        KimiCliCommand command = new KimiCliCommand();
        CliToolConfig config = CliToolConfig.builder()
                .type(CliToolConfig.CliType.KIMI)
                .executablePath(KIMI_PATH)
                .extraArgs(List.of("--yolo"))
                .mcpConfigFlag("")
                .build();

        List<String> cmd = command.buildProcessCommand(config, "Respond with only: OK", null);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        pb.environment().putAll(System.getenv());

        Process process = pb.start();

        // Don't close stdin — Kimi crashes with BrokenPipeError if stdin is closed
        command.writePrompt(process, "Respond with only: OK");

        // Read first few lines of stdout
        StringBuilder stdout = new StringBuilder();
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 20) {
                    stdout.append(line).append("\n");
                    count++;
                }
            } catch (Exception ignored) {}
        });

        StringBuilder stderr = new StringBuilder();
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 20) {
                    stderr.append(line).append("\n");
                    count++;
                }
            } catch (Exception ignored) {}
        });

        stdoutReader.setDaemon(true);
        stderrReader.setDaemon(true);
        stdoutReader.start();
        stderrReader.start();

        boolean exited = process.waitFor(60, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
        }

        stdoutReader.join(3000);
        stderrReader.join(3000);

        System.out.println("=== STDOUT ===");
        System.out.println(stdout);
        System.out.println("=== STDERR ===");
        System.out.println(stderr);
        System.out.println("=== EXIT CODE: " + (exited ? process.exitValue() : "TIMEOUT") + " ===");

        // The process should not crash with BrokenPipeError
        assertThat(stderr.toString()).doesNotContain("BrokenPipeError");
    }
}
