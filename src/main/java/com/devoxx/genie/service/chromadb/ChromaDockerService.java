package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.exception.ChromaDBException;
import com.devoxx.genie.service.chromadb.exception.DockerException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public final class ChromaDockerService {
    
    /** Single source of truth for the ChromaDB Docker image version. Referenced by the validator
     *  (status message) and its test, so bumping the version only requires editing this line. */
    public static final String CHROMA_VERSION = "0.6.2";
    public static final String CHROMADB_DOCKER_NAME = "chromadb/chroma";
    private static final String CHROMA_IMAGE = CHROMADB_DOCKER_NAME + ":" + CHROMA_VERSION;
    private static final String CONTAINER_NAME = "devoxx-genie-chromadb";

    public static final String FAILED_TO_PULL_CHROMA_DB_IMAGE = "Failed to pull ChromaDB image: ";
    public static final String FAILED_TO_START_CHROMA_DB = "Failed to start ChromaDB: ";
    public static final String FAILED_TO_CREATE_VOLUME_DIRECTORY = "Failed to create volume directory: ";

    // Docker states
    public static final String RUNNING = "running";
    public static final String EXITED = "exited";
    public static final String CREATED = "created";

    public void startChromaDB(Project project, ChromaDBStatusCallback callback) {
        startChromaDB(project, callback, null);
    }

    public void startChromaDB(Project project, ChromaDBStatusCallback callback, @Nullable ProgressIndicator indicator) {
        try {
            // We assume Docker is already installed and running
            if (isChromaDBRunning()) {
                log.debug("ChromaDB is already running");
                callback.onSuccess();
                return;
            }

            if (indicator != null) indicator.setText("Pulling ChromaDB image " + CHROMA_VERSION + " (may take several minutes on first install)...");
            pullChromaDockerImage(callback, indicator);
            if (indicator != null) indicator.setText("Starting ChromaDB container...");
            String volumePath = setupVolumeDirectory(project);
            startChromaContainer(volumePath, callback);

        } catch (ChromaDBException e) {
            log.error(FAILED_TO_START_CHROMA_DB + e.getMessage());
            callback.onError(FAILED_TO_START_CHROMA_DB + e.getMessage());
        } catch (DockerException e) {
            log.error(FAILED_TO_PULL_CHROMA_DB_IMAGE + e.getMessage());
            callback.onError(FAILED_TO_PULL_CHROMA_DB_IMAGE + e.getMessage());
        } catch (IOException e) {
            log.error(FAILED_TO_CREATE_VOLUME_DIRECTORY + e.getMessage());
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

    public void pullChromaDockerImage(ChromaDBStatusCallback callback) throws DockerException {
        pullChromaDockerImage(callback, null, null);
    }

    public void pullChromaDockerImage(ChromaDBStatusCallback callback, @Nullable ProgressIndicator indicator) throws DockerException {
        pullChromaDockerImage(callback, indicator, null);
    }

    /**
     * Pull the ChromaDB Docker image. Layer-level download progress is forwarded to:
     * <ul>
     *   <li>{@code indicator.setText2(...)} when {@code indicator != null} — surfaces in
     *       the IDE's background-task status bar.</li>
     *   <li>{@code progressListener.accept(...)} when non-null — lets callers surface the
     *       same text inline in their own UI (e.g. the RAG settings panel) so the user sees
     *       activity without having to look at the status bar spinner.</li>
     * </ul>
     * The listener is invoked on a Docker-API callback thread; UI-touching callers must
     * hop to the EDT themselves.
     */
    public void pullChromaDockerImage(ChromaDBStatusCallback callback,
                                      @Nullable ProgressIndicator indicator,
                                      @Nullable java.util.function.Consumer<String> progressListener) throws DockerException {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {
            ResultCallback.Adapter<PullResponseItem> progressCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    String status = item.getStatus();
                    if (status != null) {
                        String id = item.getId();
                        String text = id != null ? id + ": " + status : status;
                        if (indicator != null && !indicator.isCanceled()) {
                            indicator.setText2(text);
                        }
                        if (progressListener != null) {
                            progressListener.accept(text);
                        }
                    }
                    super.onNext(item);
                }
            };
            dockerClient.pullImageCmd(CHROMA_IMAGE).exec(progressCallback).awaitCompletion();
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
                String containerId;
                try (var createCmd = dockerClient.createContainerCmd(CHROMA_IMAGE)
                        .withName(CONTAINER_NAME)
                        .withHostConfig(hostConfig)) {
                    containerId = createCmd.exec().getId();
                }

                // Start the container
                dockerClient.startContainerCmd(containerId).exec();
                callback.onSuccess();
            } else {
                // Reuse existing container, but only if it was built from the current image.
                // Otherwise a previous plugin version's container (e.g. chromadb/chroma:0.6.2)
                // would be silently restarted, defeating the version bump and confusing users
                // who think they're now on the new version.
                Container existingContainer = existingContainers.getFirst();
                String existingImage = existingContainer.getImage();
                if (existingImage != null && !existingImage.equals(CHROMA_IMAGE)) {
                    callback.onError(String.format(
                            "Existing ChromaDB container is on image '%s' but this plugin version expects '%s'. " +
                                    "Remove the old container first:  docker rm -f %s  " +
                                    "(ChromaDB 0.x → 1.x changed the on-disk format — if you upgraded across that boundary, " +
                                    "also clear the data volume and re-index your project.)",
                            existingImage, CHROMA_IMAGE, CONTAINER_NAME));
                    return;
                }

                String containerState = existingContainer.getState();
                if (EXITED.equalsIgnoreCase(containerState) || CREATED.equalsIgnoreCase(containerState)) {
                    startExistingContainer(dockerClient, existingContainer.getId(), callback);
                } else if (RUNNING.equalsIgnoreCase(containerState)) {
                    callback.onSuccess();
                }
            }
        } catch (IOException e) {
            callback.onError("Failed to start ChromaDB container: " + e.getMessage());
            throw new ChromaDBException("Failed to start ChromaDB container", e);
        }
    }

    private void startExistingContainer(DockerClient dockerClient, String containerId, ChromaDBStatusCallback callback) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            callback.onSuccess();
        } catch (NotModifiedException ignored) {
            callback.onError("Container is already running or in transition");
        }
    }

    public void deleteCollectionData(@NotNull Project project, @NotNull String collectionName) {
        Path volumePath = Paths.get(PathManager.getSystemPath(), "DevoxxGenie", "chromadb", "data-" + project.getLocationHash());
        Path collectionPath = volumePath.resolve(collectionName);

        if (!Files.exists(collectionPath)) {
            log.debug("Collection directory does not exist: {}", collectionPath);
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
                            log.debug("Deleted: {}", path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {} : {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to delete collection data for: {}", collectionName, e);
        }
    }
}
