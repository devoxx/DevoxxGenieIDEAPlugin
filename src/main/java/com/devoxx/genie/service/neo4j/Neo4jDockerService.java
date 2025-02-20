package com.devoxx.genie.service.neo4j;

import com.devoxx.genie.service.neo4j.exception.DockerException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public final class Neo4jDockerService {
    private static final Logger LOG = Logger.getInstance(com.devoxx.genie.service.neo4j.Neo4jDockerService.class);

    private static final String NEO4J_IMAGE = "neo4j/neo4j:5.21";
    private static final String CONTAINER_NAME = "devoxx-genie-neo4j";

    public static final String FAILED_TO_PULL_NEO4J_IMAGE = "Failed to pull Neo4J image: ";
    public static final String FAILED_TO_START_NEO4J = "Failed to start Neo4J: ";
    public static final String FAILED_TO_CREATE_VOLUME_DIRECTORY = "Failed to create volume directory: ";

    public static final String NEO4J_DOCKER_NAME = "neo4j/neo4j";

    // Docker states
    public static final String RUNNING = "running";
    public static final String EXITED = "exited";
    public static final String CREATED = "created";

    public void startNeo4J(Project project, Neo4jStatusCallback callback) {
        try {
            // We assume Docker is already installed and running
            if (isNeo4JRunning()) {
                LOG.debug("Neo4j is already running");
                callback.onSuccess();
                return;
            }

            pullNeo4jDockerImage(callback);
            String volumePath = setupVolumeDirectory(project);
            startNeo4jContainer(volumePath, callback);

        } catch (com.devoxx.genie.service.neo4j.exception.Neo4jException e) {
            LOG.error(FAILED_TO_START_NEO4J + e.getMessage());
            callback.onError(FAILED_TO_START_NEO4J + e.getMessage());
        } catch (DockerException e) {
            LOG.error(FAILED_TO_PULL_NEO4J_IMAGE + e.getMessage());
            callback.onError(FAILED_TO_PULL_NEO4J_IMAGE + e.getMessage());
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
    public void pullNeo4jDockerImage(Neo4jStatusCallback callback) throws DockerException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {
            dockerClient.pullImageCmd(NEO4J_IMAGE).start().awaitCompletion();
        } catch (IOException e) {
            callback.onError(FAILED_TO_PULL_NEO4J_IMAGE + e.getMessage());
            throw new DockerException("Failed to pull Neo4J image", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerException("Docker pull command was interrupted", e);
        }
    }

    /**
     * Check if the Neo4J container is already running
     *
     * @return true if the container is running, false otherwise
     * @throws IOException if an I/O error occurs
     */
    private boolean isNeo4JRunning() throws IOException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {

            return dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .anyMatch(container -> container.getImage().contains(NEO4J_DOCKER_NAME) &&
                            RUNNING.equalsIgnoreCase(container.getState()));
        }
    }

    /**
     * Start the Neo4j container
     *
     * @param volumePath the path to the volume directory
     * @param callback   the callback to notify the status
     * @throws com.devoxx.genie.service.neo4j.exception.Neo4jException if an error occurs while starting the container
     */
    private void startNeo4jContainer(String volumePath, Neo4jStatusCallback callback) throws com.devoxx.genie.service.neo4j.exception.Neo4jException {
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
                CreateContainerResponse container = dockerClient.createContainerCmd(NEO4J_IMAGE)
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
            callback.onError("Failed to start Neo4J container: " + e.getMessage());
            throw new com.devoxx.genie.service.neo4j.exception.Neo4jException("Failed to start Neo4J container", e);
        }
    }

//    public void deleteCollectionData(@NotNull Project project, @NotNull String collectionName) {
//        Path volumePath = Paths.get(PathManager.getSystemPath(), "DevoxxGenie", "chromadb", "data-" + project.getLocationHash());
//        Path collectionPath = volumePath.resolve(collectionName);
//
//        if (!Files.exists(collectionPath)) {
//            LOG.debug("Collection directory does not exist: " + collectionPath);
//            return;
//        }
//
//        ApplicationManager.getApplication().invokeLater(() ->
//                deleteCollectionData(collectionPath, collectionName));
//    }
//
//    private void deleteCollectionData(@NotNull Path collectionPath, @NotNull String collectionName) {
//        try (Stream<Path> pathStream = Files.walk(collectionPath)) {
//            pathStream
//                    .sorted(Comparator.reverseOrder())
//                    .forEach(path -> {
//                        try {
//                            Files.delete(path);
//                            LOG.debug("Deleted: " + path);
//                        } catch (IOException e) {
//                            LOG.warn("Failed to delete: " + path, e);
//                        }
//                    });
//        } catch (IOException e) {
//            LOG.error("Failed to delete collection data for: " + collectionName, e);
//        }
//    }
}
