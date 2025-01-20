package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.exception.ChromaDBException;
import com.devoxx.genie.service.chromadb.exception.DockerException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public final class ChromaDockerService {
    private static final Logger LOG = Logger.getInstance(ChromaDockerService.class);

    private static final String CHROMA_IMAGE = "chromadb/chroma:latest";
    private static final String CONTAINER_NAME = "devoxx-genie-chromadb";

    public static final String FAILED_TO_PULL_CHROMA_DB_IMAGE = "Failed to pull ChromaDB image: ";
    public static final String FAILED_TO_START_CHROMA_DB = "Failed to start ChromaDB: ";
    public static final String FAILED_TO_CREATE_VOLUME_DIRECTORY = "Failed to create volume directory: ";

    public static final String CHROMADB_DOCKER_NAME = "chromadb/chroma";

    // Docker states
    public static final String RUNNING = "running";
    public static final String EXITED = "exited";
    public static final String CREATED = "created";

    public void startChromaDB(Project project, ChromaDBStatusCallback callback) {
        try {
            // We assume Docker is already installed and running
            if (isChromaDBRunning()) {
                LOG.debug("ChromaDB is already running");
                callback.onSuccess();
                return;
            }

            pullChromaDockerImage(callback);
            String volumePath = setupVolumeDirectory(project);
            startChromaContainer(volumePath, callback);

        } catch (ChromaDBException e) {
            LOG.error(FAILED_TO_START_CHROMA_DB + e.getMessage());
            callback.onError(FAILED_TO_START_CHROMA_DB + e.getMessage());
        } catch (DockerException e) {
            LOG.error(FAILED_TO_PULL_CHROMA_DB_IMAGE + e.getMessage());
            callback.onError(FAILED_TO_PULL_CHROMA_DB_IMAGE + e.getMessage());
        } catch (IOException e) {
            LOG.error(FAILED_TO_CREATE_VOLUME_DIRECTORY + e.getMessage());
            callback.onError(FAILED_TO_CREATE_VOLUME_DIRECTORY + e.getMessage());
        }
    }

    /**
     * Create a ChromaDB volume directory for persistent storage
     *
     * @param project the current project
     * @return the path to the volume directory
     * @throws IOException if an I/O error occurs
     */
    private @NotNull String setupVolumeDirectory(@NotNull Project project) throws IOException {
        // Get the plugin's data directory using PathManager
        Path pluginDataPath = Paths.get(PathManager.getSystemPath(), "DevoxxGenie", "chromadb");

        // Create a project-specific subdirectory using project hash to avoid name conflicts
        int projectId = project.hashCode();
        Path volumePath = pluginDataPath.resolve("data-" + projectId);

        // Create all necessary directories
        Files.createDirectories(volumePath);

        return volumePath.toString();
    }

    /**
     * Pull the ChromaDB Docker image
     *
     * @param callback the callback to notify the status
     * @throws DockerException if an error occurs while pulling the image
     */
    public void pullChromaDockerImage(ChromaDBStatusCallback callback) throws DockerException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {
            dockerClient.pullImageCmd(CHROMA_IMAGE).start().awaitCompletion();
        } catch (IOException e) {
            callback.onError(FAILED_TO_PULL_CHROMA_DB_IMAGE + e.getMessage());
            throw new DockerException("Failed to pull ChromaDB image", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerException("Docker pull command was interrupted", e);
        }
    }

    /**
     * Check if the ChromaDB container is already running
     *
     * @return true if the container is running, false otherwise
     * @throws IOException if an I/O error occurs
     */
    private boolean isChromaDBRunning() throws IOException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {

            return dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .anyMatch(container -> container.getImage().contains(CHROMADB_DOCKER_NAME) &&
                            RUNNING.equalsIgnoreCase(container.getState()));
        }
    }

    /**
     * Start the ChromaDB container
     *
     * @param volumePath the path to the volume directory
     * @param callback   the callback to notify the status
     * @throws ChromaDBException if an error occurs while starting the container
     */
    private void startChromaContainer(String volumePath, ChromaDBStatusCallback callback) throws ChromaDBException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {

            // First, check if container exists
            List<Container> existingContainers = dockerClient.listContainersCmd()
                    .withShowAll(true)  // This shows both running and stopped containers
                    .withNameFilter(List.of(CONTAINER_NAME))
                    .exec();

            if (existingContainers.isEmpty()) {
                Integer port = DevoxxGenieStateService.getInstance().getIndexerPort();

                HostConfig hostConfig = new HostConfig()
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(8000), ExposedPort.tcp(port)))
                        .withBinds(new Bind(volumePath, new Volume("/chroma/chroma")));

                // Create new container if none exists
                CreateContainerResponse container = dockerClient.createContainerCmd(CHROMA_IMAGE)
                        .withName(CONTAINER_NAME)
                        .withHostConfig(hostConfig)
                        .exec();

                // Start the container
                dockerClient.startContainerCmd(container.getId()).exec();
            } else {
                // Reuse existing container
                Container existingContainer = existingContainers.get(0);

                // Check the actual container state
                String containerState = existingContainer.getState();

                // Only start if the container is stopped or created
                if (EXITED.equalsIgnoreCase(containerState) || CREATED.equalsIgnoreCase(containerState)) {
                    try {
                        dockerClient.startContainerCmd(existingContainer.getId()).exec();
                        callback.onSuccess();
                    } catch (NotModifiedException ignored) {
                        callback.onError("Container is already running or in transition");
                    }
                }
            }
        } catch (IOException e) {
            callback.onError("Failed to start ChromaDB container: " + e.getMessage());
            throw new ChromaDBException("Failed to start ChromaDB container", e);
        }
    }

    public void deleteCollectionData(@NotNull Project project, @NotNull String collectionName) {
        Path volumePath = Paths.get(PathManager.getSystemPath(), "DevoxxGenie", "chromadb", "data-" + project.getLocationHash());
        Path collectionPath = volumePath.resolve(collectionName);

        if (!Files.exists(collectionPath)) {
            LOG.debug("Collection directory does not exist: " + collectionPath);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() ->
                deleteCollectionData(collectionPath, collectionName));
    }

    private void deleteCollectionData(@NotNull Path collectionPath, @NotNull String collectionName) {
        try (Stream<Path> pathStream = Files.walk(collectionPath)) {
            pathStream
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            LOG.debug("Deleted: " + path);
                        } catch (IOException e) {
                            LOG.warn("Failed to delete: " + path, e);
                        }
                    });
        } catch (IOException e) {
            LOG.error("Failed to delete collection data for: " + collectionName, e);
        }
    }
}
