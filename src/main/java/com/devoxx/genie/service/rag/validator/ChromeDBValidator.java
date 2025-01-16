package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ChromeDBValidator implements Validator {

    public static final String CHROMA_DB_CHROMA = "chromadb/chroma";
    public static final String RUNNING = "running";
    public static final String CONTAINER_NAME = "devoxx-genie-chromadb";

    private String message;
    private ValidationActionType action = ValidationActionType.OK;

    @Override
    public boolean isValid() {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {
            // First check if Docker is running and we can connect
            if (!isDockerRunning(dockerClient)) {
                this.message = "Docker is not running. Please start Docker first.";
                return false;
            }

            // Check if ChromaDB image exists
            if (!isChromaDBImagePresent(dockerClient)) {
                this.message = "ChromaDB Docker image not found";
                this.action = ValidationActionType.PULL_CHROMA_DB;
                return false;
            }

            // Get all containers (running and stopped)
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withNameFilter(List.of(CONTAINER_NAME))
                    .exec();

            // Check container status
            if (containers.isEmpty()) {
                this.message = "ChromaDB container not found";
                this.action = ValidationActionType.START_CHROMA_DB;
                return false;
            }

            Container container = containers.get(0);
            if (!RUNNING.equalsIgnoreCase(container.getState())) {
                this.message = "ChromaDB container is not running";
                this.action = ValidationActionType.START_CHROMA_DB;
                return false;
            }

            // Verify port configuration
            Integer dbPort = DevoxxGenieStateService.getInstance().getIndexerPort();
            if (!isContainerRunningOnCorrectPort(container, dbPort)) {
                this.message = "ChromaDB container not running on configured port " + dbPort;
                this.action = ValidationActionType.START_CHROMA_DB;
                return false;
            }

            this.message = "ChromaDB is running";
            return true;

        } catch (Exception e) {
            this.message = "Failed to verify ChromaDB status: " + e.getMessage();
            return false;
        }
    }

    private boolean isDockerRunning(@NotNull DockerClient dockerClient) {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isChromaDBImagePresent(@NotNull DockerClient dockerClient) {
        List<Image> images = dockerClient.listImagesCmd().exec();
        return images.stream()
                .anyMatch(image -> image.getRepoTags() != null &&
                        Arrays.stream(image.getRepoTags())
                                .anyMatch(tag -> tag.contains(CHROMA_DB_CHROMA)));
    }

    private boolean isContainerRunningOnCorrectPort(@NotNull Container container, int expectedPort) {
        return Arrays.stream(container.getPorts())
                .anyMatch(port -> port.getPublicPort() != null &&
                        port.getPublicPort() == expectedPort);
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public String getErrorMessage() {
        return this.message;
    }

    @Override
    public ValidatorType getCommand() {
        return ValidatorType.CHROMADB;
    }

    @Override
    public ValidationActionType getAction() {
        return this.action;
    }
}