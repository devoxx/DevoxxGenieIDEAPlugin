package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.GitHubRepo;
import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.HttpRequests;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

// Assuming Gson is available in the IntelliJ platform's classpath.
// If not, you'd need to add it as a dependency or use a simpler JSON parser.
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * An application service responsible for searching, downloading, building, and installing MCP servers from GitHub.
 */
@Service
@Slf4j
public final class MCPInstallerService {

    // GitHub API URL to search for Java repositories with "mcp-server" keyword or topic, sorted by stars.
    // Using `q=mcp-server+language:java` as a general search. Could also add `topic:mcp-server` for more precision if used by repos.
    private static final String GITHUB_SEARCH_URL = "https://api.github.com/search/repositories?q=mcp-server+language:java&sort=stars&order=desc";

    // Base directory for installing MCP servers
    private static final Path INSTALL_BASE_DIR = Paths.get(System.getProperty("user.home"), ".devoxx-genie", "mcp-servers");
    private final Gson gson = new Gson(); // Gson instance for JSON parsing

    public MCPInstallerService() {
        // Ensure the base installation directory exists on service creation.
        try {
            Files.createDirectories(INSTALL_BASE_DIR);
        } catch (IOException e) {
            log.error("Failed to create base installation directory: {}", INSTALL_BASE_DIR, e);
        }
    }

    /**
     * Searches GitHub for repositories tagged as 'mcp-server' and written in Java.
     * Results are sorted by stars in descending order.
     *
     * @param indicator ProgressIndicator to update UI during the API call.
     * @return A list of {@link GitHubRepo} objects found.
     * @throws IOException If there's an error connecting to GitHub or parsing the response.
     */
    public List<GitHubRepo> searchMcpServersOnGitHub(@NotNull ProgressIndicator indicator) throws IOException {
        indicator.setText("Connecting to GitHub API...");
        List<GitHubRepo> repos = new ArrayList<>();
        try {
            // Use IntelliJ's HttpRequests utility for robust HTTP communication.
            String jsonResponse = HttpRequests.request(GITHUB_SEARCH_URL).readString();
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray items = response.getAsJsonArray("items");

            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                GitHubRepo repo = GitHubRepo.builder()
                        .name(item.get("name").getAsString())
                        .fullName(item.get("full_name").getAsString())
                        .description(item.has("description") && !item.get("description").isJsonNull() ? item.get("description").getAsString() : "No description")
                        .htmlUrl(item.get("html_url").getAsString())
                        .cloneUrl(item.get("clone_url").getAsString())
                        .stars(item.get("stargazers_count").getAsInt())
                        .updatedAt(formatGitHubDate(item.get("updated_at").getAsString()))
                        .defaultBranch(item.has("default_branch") ? item.get("default_branch").getAsString() : "master") // Default to master if not found
                        .build();
                repos.add(repo);
            }
        } catch (IOException e) {
            log.error("Error fetching GitHub repositories: {}", e.getMessage(), e);
            throw e; // Re-throw to be handled by the caller (e.g., UI error dialog)
        }
        return repos;
    }

    /**
     * Formats GitHub's ISO 8601 date string to a more human-readable format.
     * @param githubDate The date string from GitHub (e.g., "2023-10-27T10:00:00Z").
     * @return Formatted date string (e.g., "2023-10-27 10:00").
     */
    private String formatGitHubDate(String githubDate) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(githubDate, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            log.warn("Failed to parse GitHub date format: {}", githubDate, e);
            return githubDate; // Return original if parsing fails
        }
    }

    /**
     * Installs an MCP server from a given GitHub repository.
     * This involves cloning, building (Maven/Gradle), locating the JAR, and setting up an MCPServer configuration.
     *
     * @param repo The GitHub repository to install.
     * @param indicator ProgressIndicator to update UI during installation steps.
     * @return A configured {@link MCPServer} instance if successful, null otherwise.
     * @throws IOException If any I/O error occurs during cloning, building, or file operations.
     * @throws InterruptedException If the build process is interrupted.
     */
    @Nullable
    public MCPServer installMcpServer(@NotNull GitHubRepo repo, @NotNull ProgressIndicator indicator) throws IOException, InterruptedException {
        String repoName = repo.getName();
        // Create a unique installation directory for the repository
        Path installDir = INSTALL_BASE_DIR.resolve(repoName);
        Files.createDirectories(installDir);

        try {
            // Step 1: Clone the repository
            indicator.setText("Cloning " + repo.getFullName() + "...");
            indicator.setFraction(0.1);
            executeCommand(installDir.getParent(), List.of("git", "clone", repo.getCloneUrl(), repoName), indicator);
            indicator.setFraction(0.3);

            // Step 2: Detect build system and build the project
            Path pomFile = installDir.resolve("pom.xml");
            Path buildGradleFile = installDir.resolve("build.gradle");
            Path buildJarPath = null; // To store the path to the located runnable JAR

            if (Files.exists(pomFile)) {
                indicator.setText("Building with Maven...");
                // Verify Maven is installed and accessible in PATH
                if (!isCommandAvailable("mvn", indicator)) {
                    throw new IOException("Maven (mvn) command not found. Please ensure Maven is installed and in your system PATH.");
                }
                // Execute Maven clean install, skipping tests for faster build
                executeCommand(installDir, List.of("mvn", "clean", "install", "-DskipTests"), indicator);
                buildJarPath = findLargestJar(installDir.resolve("target")); // Standard Maven output directory
            } else if (Files.exists(buildGradleFile)) {
                indicator.setText("Building with Gradle...");
                // Check for gradlew script first, then global gradle command
                String gradleCommand = SystemInfo.isWindows ? "gradlew.bat" : "gradlew";
                Path gradlewScript = installDir.resolve(gradleCommand);
                if (Files.exists(gradlewScript)) {
                    // Use gradlew for convenience, skipping tests
                    executeCommand(installDir, List.of(gradlewScript.toAbsolutePath().toString(), "build", "-x", "test"), indicator);
                } else if (isCommandAvailable("gradle", indicator)) {
                    // Fallback to global gradle command, skipping tests
                    executeCommand(installDir, List.of("gradle", "build", "-x", "test"), indicator);
                } else {
                    throw new IOException("Gradle (gradle or gradlew) command not found. Please install Gradle or ensure gradlew is present in the repository.");
                }
                buildJarPath = findLargestJar(installDir.resolve("build").resolve("libs")); // Standard Gradle output directory
            } else {
                throw new IOException("No supported build system (Maven or Gradle) detected in the repository.");
            }
            indicator.setFraction(0.8);

            if (buildJarPath == null) {
                throw new IOException("Failed to find a runnable JAR after building the project. Check project build output.");
            }

            // Step 3: Copy the built JAR to a standard name within the installation directory
            Path finalJarPath = installDir.resolve(repoName + ".jar");
            Files.copy(buildJarPath, finalJarPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            indicator.setFraction(0.9);

            // Step 4: Create and return the MCPServer configuration object
            MCPServer newServer = MCPServer.builder()
                    .enabled(true) // Enable by default upon installation
                    .name(repoName)
                    .transportType(MCPServer.TransportType.STDIO) // Installed JARs are run as STDIO processes
                    .command("java") // Default command to run Java JARs
                    .args(List.of("-jar", finalJarPath.getFileName().toString())) // Arguments for the command
                    .workingDirectory(installDir.toString()) // Set the installed repo folder as working directory
                    .installationPathString(finalJarPath.toString()) // Store the actual JAR path for future reference
                    .gitHubUrl(repo.getHtmlUrl()) // Store original GitHub URL for provenance
                    .repositoryName(repoName) // Store repo name
                    .build();

            indicator.setText("Installation complete.");
            indicator.setFraction(1.0);
            log.info("Successfully installed MCP server {} from {}", repoName, repo.getHtmlUrl());
            return newServer;

        } catch (Exception e) {
            log.error("Installation failed for {}: {}", repo.getFullName(), e.getMessage(), e);
            // Clean up the partially created directory on failure
            deleteDirectory(installDir);
            throw e; // Re-throw the exception to be handled by the UI
        }
    }

    /**
     * Executes an external command using ProcessBuilder.
     *
     * @param workingDir The directory where the command should be executed.
     * @param command The command and its arguments.
     * @param indicator ProgressIndicator to allow cancellation and log output.
     * @throws IOException If an I/O error occurs starting the process.
     * @throws InterruptedException If the process is interrupted.
     */
    private void executeCommand(@NotNull Path workingDir, @NotNull List<String> command, @NotNull ProgressIndicator indicator) throws IOException, InterruptedException {
        log.debug("Executing command in {}: {}", workingDir, String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true); // Merge stderr into stdout for easier logging

        Process process = pb.start();

        // Read process output in a separate thread to prevent deadlock and log it.
        // This also allows for cancellation checks.
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.trace("[BUILD OUTPUT]: {}", line); // Log all build output as trace
                    if (indicator.isCanceled()) {
                        log.info("Build process cancelled by user.");
                        process.destroyForcibly(); // Terminate the process if cancelled
                        break;
                    }
                }
            } catch (IOException e) {
                log.warn("Error reading command output: {}", e.getMessage());
            }
        });
        outputReader.start();

        // Wait for the process to complete, with a timeout.
        boolean finished = process.waitFor(30, TimeUnit.MINUTES); // Max 30 minutes for a build
        if (!finished) {
            process.destroyForcibly(); // Forcefully terminate if timeout occurs
            throw new IOException("Command timed out after 30 minutes: " + String.join(" ", command));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": " + String.join(" ", command));
        }
    }

    /**
     * Checks if a given command is available in the system's PATH.
     *
     * @param command The command to check (e.g., "git", "mvn", "gradle").
     * @param indicator ProgressIndicator to check for cancellation.
     * @return True if the command is found, false otherwise.
     * @throws IOException If an I/O error occurs during the check.
     * @throws InterruptedException If the check process is interrupted.
     */
    private boolean isCommandAvailable(@NotNull String command, @NotNull ProgressIndicator indicator) throws IOException, InterruptedException {
        try {
            ProcessBuilder pb;
            if (SystemInfo.isWindows) {
                pb = new ProcessBuilder("where", command); // Windows command to locate executables
            } else {
                pb = new ProcessBuilder("which", command); // Unix/Linux/macOS command to locate executables
            }
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0; // An exit code of 0 means the command was found
        } catch (IOException e) {
            log.warn("Error checking command availability for {}: {}", command, e.getMessage());
            return false; // Command not found or other I/O error
        }
    }

    /**
     * Finds the largest JAR file in a given directory, typically assumed to be the fat/runnable JAR.
     * Filters out common non-runnable JARs (sources, javadoc).
     *
     * @param searchDir The directory to search within (e.g., `target` for Maven, `build/libs` for Gradle).
     * @return The path to the largest JAR file, or null if none found.
     * @throws IOException If an I/O error occurs while traversing the directory.
     */
    @Nullable
    private Path findLargestJar(@NotNull Path searchDir) throws IOException {
        if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
            log.warn("Search directory for JAR does not exist or is not a directory: {}", searchDir);
            return null;
        }
        try (Stream<Path> walk = Files.walk(searchDir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("sources") && !p.getFileName().toString().contains("javadoc"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.size(p); // Compare by file size (largest is likely the fat JAR)
                        } catch (IOException e) {
                            log.warn("Could not get size of file {}: {}", p, e.getMessage());
                            return -1L; // Treat as smallest if size can't be read
                        }
                    }))
                    .orElse(null);
        }
    }

    /**
     * Recursively deletes a directory and its contents.
     * Used for cleanup after failed installations.
     *
     * @param directory The path to the directory to delete.
     */
    private void deleteDirectory(@NotNull Path directory) {
        if (Files.exists(directory)) {
            try {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder()) // Sort in reverse order to delete files before directories
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Cleaned up directory: {}", directory);
            } catch (IOException e) {
                log.error("Failed to delete directory: {}", directory, e);
            }
        }
    }
}