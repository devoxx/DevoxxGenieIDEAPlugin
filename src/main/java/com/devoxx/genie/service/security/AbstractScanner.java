package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import com.intellij.util.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Template method for security scanners.
 * Subclasses implement buildCommand() and parseOutput().
 */
@Slf4j
public abstract class AbstractScanner {

    /**
     * Run the scanner against the given source path.
     *
     * @param binaryPath resolved scanner binary
     * @param sourcePath project root path to scan
     * @return list of findings
     */
    public List<SecurityFinding> scan(@NotNull String binaryPath, @NotNull String sourcePath)
            throws SecurityScanException {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(getType().getId() + "-report", ".json");
            List<String> command = buildCommand(binaryPath, sourcePath, tempFile.toString());

            log.info("Running {}: {}", getType().getDisplayName(), String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            pb.environment().putAll(EnvironmentUtil.getEnvironmentMap());

            Process process = pb.start();

            // Read stdout and stderr concurrently
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdout), getType().getId() + "-stdout");
            Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderr), getType().getId() + "-stderr");
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new SecurityScanException(getType().getDisplayName() +
                        " timed out after " + getTimeoutSeconds() + "s");
            }

            stdoutThread.join(5000);
            stderrThread.join(5000);

            int exitCode = process.exitValue();
            log.info("{} exited with code {}", getType().getDisplayName(), exitCode);

            if (!isAcceptableExitCode(exitCode)) {
                String errMsg = stderr.toString().trim();
                if (errMsg.isEmpty()) errMsg = stdout.toString().trim();
                throw new SecurityScanException(getType().getDisplayName() +
                        " failed (exit code " + exitCode + "): " + truncate(errMsg, 500));
            }

            return parseOutput(stdout.toString(), tempFile.toString());
        } catch (SecurityScanException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityScanException(getType().getDisplayName() + " error: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                    // cleanup best-effort
                }
            }
        }
    }

    protected abstract ScannerType getType();

    protected abstract List<String> buildCommand(@NotNull String binaryPath,
                                                  @NotNull String sourcePath,
                                                  @NotNull String tempFile);

    protected abstract List<SecurityFinding> parseOutput(@NotNull String stdout,
                                                          @NotNull String tempFile)
            throws SecurityScanException;

    protected abstract int getTimeoutSeconds();

    /**
     * Whether the given exit code indicates success (or at least parseable output).
     * Subclasses can override for scanners that use non-zero codes for "findings found".
     */
    protected boolean isAcceptableExitCode(int exitCode) {
        return exitCode == 0;
    }

    private void readStream(java.io.InputStream stream, StringBuilder sb) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            log.debug("Error reading process stream", e);
        }
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
