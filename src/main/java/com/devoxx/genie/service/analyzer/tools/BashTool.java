package com.devoxx.genie.service.analyzer.tools;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Implementation of DevoxxGenie Code's tools using Langchain4j @Tool annotation
 */
@Setter
public class BashTool {

    private static final Logger LOG = LoggerFactory.getLogger(BashTool.class);

    private Path currentWorkingDir = Paths.get(System.getProperty("user.dir"));

    /**
     * BashTool implementation - executes bash commands
     */
    public Map<String, Object> bash(String command, Integer timeout) {
        LOG.info("Executing bash tool with command: {}, timeout {}", command, timeout);
        Map<String, Object> result = new HashMap<>();

        if (timeout == null) {
            timeout = 30 * 60 * 1000; // 30 minutes default
        } else {
            timeout = Math.min(timeout, 10 * 60 * 1000); // Max 10 minutes
        }

        try {
            // Create process builder
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Set the command
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("bash", "-c", command);
            }

            // Set working directory
            processBuilder.directory(currentWorkingDir.toFile());

            // Start process
            Process process = processBuilder.start();

            // Read output and error streams
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            Thread outputThread = new Thread(() -> {
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    while (scanner.hasNextLine()) {
                        output.append(scanner.nextLine()).append("\n");
                    }
                }
            });

            Thread errorThread = new Thread(() -> {
                try (Scanner scanner = new Scanner(process.getErrorStream())) {
                    while (scanner.hasNextLine()) {
                        error.append(scanner.nextLine()).append("\n");
                    }
                }
            });

            outputThread.start();
            errorThread.start();

            // Wait for process to complete with timeout
            boolean completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroyForcibly();
                result.put("stdout", output.toString());
                result.put("stderr", error.toString() + "\nCommand execution timed out");
                result.put("code", -1);
                result.put("interrupted", true);
                return result;
            }

            // Wait for output threads to complete
            outputThread.join();
            errorThread.join();

            // Update working directory if command was successful and contained cd
            if (process.exitValue() == 0 && command.contains("cd ")) {
                try {
                    // Run pwd to get current directory
                    ProcessBuilder pwdBuilder = new ProcessBuilder();
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        pwdBuilder.command("cmd.exe", "/c", "cd");
                    } else {
                        pwdBuilder.command("bash", "-c", "pwd");
                    }
                    pwdBuilder.directory(currentWorkingDir.toFile());

                    Process pwdProcess = pwdBuilder.start();
                    try (Scanner scanner = new Scanner(pwdProcess.getInputStream())) {
                        if (scanner.hasNextLine()) {
                            String newDir = scanner.nextLine().trim();
                            currentWorkingDir = Paths.get(newDir);
                        }
                    }
                    pwdProcess.waitFor();
                } catch (Exception e) {
                    // Ignore errors in updating directory
                }
            }

            result.put("stdout", output.toString());
            result.put("stderr", error.toString());
            result.put("code", process.exitValue());
            result.put("interrupted", false);
            return result;
        } catch (Exception e) {
            result.put("stdout", "");
            result.put("stderr", "Failed to execute command: " + e.getMessage());
            result.put("code", -1);
            result.put("interrupted", false);
            return result;
        }
    }
}