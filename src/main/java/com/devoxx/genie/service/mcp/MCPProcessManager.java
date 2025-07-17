package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * An application service for managing the lifecycle of MCP server processes (start, stop, restart)
 * and handling their console output.
 */
@Service
public final class MCPProcessManager {
    private static final Logger LOG = Logger.getInstance(MCPProcessManager.class);

    // Map to hold references to currently running MCPServer instances by their name.
    private final Map<String, MCPServer> runningServers = new ConcurrentHashMap<>();

    // Executor service for background tasks like process monitoring and output reading.
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // List of listeners to notify the UI or other components about process status changes.
    private final List<BiConsumer<String, Boolean>> statusListeners = new ArrayList<>();

    /**
     * Adds a listener that will be notified when an MCP server's running status changes.
     * @param listener A BiConsumer accepting (serverName, isRunning)
     */
    public void addProcessStatusListener(BiConsumer<String, Boolean> listener) {
        statusListeners.add(listener);
    }

    /**
     * Notifies all registered listeners about a status change for a specific server.
     * @param serverName The name of the server whose status changed.
     * @param isRunning The new running status (true if running, false if stopped).
     */
    private void notifyStatusChange(String serverName, boolean isRunning) {
        // Execute on EDT to avoid UI threading issues, though listeners might be non-UI.
        ApplicationManager.getApplication().invokeLater(() ->
                statusListeners.forEach(listener -> listener.accept(serverName, isRunning))
        );
    }

    /**
     * Checks if an MCP server with the given name is currently running.
     * @param serverName The name of the MCP server.
     * @return True if the server is running, false otherwise.
     */
    public boolean isRunning(@NotNull String serverName) {
        MCPServer server = runningServers.get(serverName);
        return server != null && server.getCurrentProcess() != null && server.getCurrentProcess().isAlive();
    }

    /**
     * Starts the process for a given MCP server.
     *
     * @param server The {@link MCPServer} configuration to start.
     * @throws IOException If an I/O error occurs during process creation.
     */
    public void startProcess(@NotNull MCPServer server) throws IOException {
        if (isRunning(server.getName())) {
            LOG.warn("Attempted to start an already running server: " + server.getName());
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(server.getCommand()); // Main executable (e.g., "java")
        if (server.getArgs() != null) {
            command.addAll(server.getArgs()); // Arguments (e.g., "-jar", "server.jar")
        }

        ProcessBuilder pb = new ProcessBuilder(command);

        // Determine the working directory for the process.
        // Priority: server.workingDirectory > server.installationPath.getParent() > default
        if (server.getWorkingDirectory() != null && !server.getWorkingDirectory().isEmpty()) {
            Path workingDirPath = Paths.get(server.getWorkingDirectory());
            if (Files.exists(workingDirPath) && Files.isDirectory(workingDirPath)) {
                pb.directory(workingDirPath.toFile());
            } else {
                LOG.warn("Configured working directory not found or not a directory: " + server.getWorkingDirectory() + " for server " + server.getName() + ". Using default.");
            }
        } else if (server.getInstallationPath() != null) {
            // For installed JARs, set the working directory to the folder containing the JAR.
            Path parentDir = server.getInstallationPath().getParent();
            if (parentDir != null && Files.exists(parentDir) && Files.isDirectory(parentDir)) {
                pb.directory(parentDir.toFile());
            }
        }

        // Add environment variables if configured
        if (server.getEnv() != null && !server.getEnv().isEmpty()) {
            pb.environment().putAll(server.getEnv());
        }

        // Redirect process output (stdout and stderr) to a dedicated log file.
        // This is crucial for installed servers to have persistent logs.
        Path logFilePath = null;
        if (pb.directory() != null) { // Use the effective working directory for logs
            logFilePath = pb.directory().toPath().resolve("console.log");
            // Delete existing log file to start fresh for each run
            Files.deleteIfExists(logFilePath);
            pb.redirectOutput(logFilePath.toFile());
            pb.redirectError(logFilePath.toFile());
            LOG.info("Process output for " + server.getName() + " redirected to: " + logFilePath);
        } else {
            LOG.warn("No specific working directory set for " + server.getName() + ", console output may not be captured to file.");
            // If no log file, output might go to IDE's console directly (less useful for external processes)
            // Or we could read streams here to populate in-memory buffer, but for long-running, file is better.
        }


        LOG.info("Starting process for MCP server: " + server.getName() + " with command: " + command + " in " + pb.directory());

        Process process = pb.start(); // Start the process
        server.setCurrentProcess(process);
        runningServers.put(server.getName(), server); // Register the running server
        server.setRunning(true);
        server.getConsoleOutputBuffer().setLength(0); // Clear old in-memory buffer on start

        notifyStatusChange(server.getName(), true); // Notify listeners (e.g., UI)

        // Submit a task to monitor the process for its termination.
        executorService.submit(() -> {
            try {
                int exitCode = process.waitFor(); // Wait for the process to exit
                LOG.info("MCP server " + server.getName() + " exited with code: " + exitCode);
            } catch (InterruptedException e) {
                LOG.warn("MCP server " + server.getName() + " process interrupted while waiting.", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
            } finally {
                // Cleanup and update status when the process terminates
                runningServers.remove(server.getName());
                server.setCurrentProcess(null);
                server.setRunning(false);
                notifyStatusChange(server.getName(), false);
            }
        });

        // Although output is redirected to file, keep a small buffer in memory for immediate access if needed
        // or for servers not using file redirection. For robustness, redirecting to file is preferred.
        if (logFilePath == null) { // If not redirected to file, read from streams
            executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        server.getConsoleOutputBuffer().append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    LOG.warn("Error reading stdout for server " + server.getName(), e);
                }
            });
            executorService.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        server.getConsoleOutputBuffer().append("[ERROR] ").append(line).append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    LOG.warn("Error reading stderr for server " + server.getName(), e);
                }
            });
        }
    }

    /**
     * Stops a running MCP server process.
     *
     * @param serverName The name of the MCP server to stop.
     */
    public void stopProcess(@NotNull String serverName) {
        MCPServer server = runningServers.get(serverName);
        if (server != null && server.getCurrentProcess() != null && server.getCurrentProcess().isAlive()) {
            LOG.info("Stopping process for MCP server: " + serverName);
            server.getCurrentProcess().destroy(); // Attempt graceful shutdown (SIGTERM)
            try {
                // Give a few seconds for graceful termination
                if (!server.getCurrentProcess().waitFor(5, TimeUnit.SECONDS)) {
                    LOG.warn("Process " + serverName + " did not terminate gracefully, forcing destruction.");
                    server.getCurrentProcess().destroyForcibly(); // Force kill (SIGKILL) if not graceful
                }
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for process " + serverName + " to stop.", e);
                server.getCurrentProcess().destroyForcibly(); // Ensure it's killed if interrupted
                Thread.currentThread().interrupt(); // Restore interrupt status
            } finally {
                // Ensure server is removed from running map and status updated regardless of how it stopped.
                runningServers.remove(serverName);
                server.setCurrentProcess(null);
                server.setRunning(false);
                notifyStatusChange(serverName, false);
            }
        } else {
            LOG.warn("Attempted to stop a non-existent or not-running server: " + serverName);
            // Even if not found in runningServers, ensure UI is updated if it was erroneously marked running.
            notifyStatusChange(serverName, false);
        }
    }

    /**
     * Disposes of the service, ensuring all running processes are forcefully terminated.
     * This is called by the IntelliJ platform when the application shuts down.
     */
    public void dispose() {
        LOG.info("Shutting down MCPProcessManager. Stopping all running servers.");
        // Iterate over a copy of the values to avoid ConcurrentModificationException during removal.
        runningServers.values().forEach(server -> {
            if (server.getCurrentProcess() != null && server.getCurrentProcess().isAlive()) {
                LOG.info("Forcibly destroying MCP server " + server.getName() + " on shutdown.");
                server.getCurrentProcess().destroyForcibly(); // Forcefully destroy on application shutdown
            }
        });
        executorService.shutdownNow(); // Shut down the thread pool immediately
        statusListeners.clear(); // Clear listeners
    }
}
