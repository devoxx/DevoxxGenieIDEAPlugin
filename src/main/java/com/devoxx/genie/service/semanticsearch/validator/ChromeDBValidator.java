package com.devoxx.genie.service.semanticsearch.validator;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.Arrays;
import java.util.List;

public class ChromeDBValidator implements Validator {

    String message = null;

    @Override
    public boolean isValid() {

        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
            // Check if ChromaDB image exists
            List<Image> images = dockerClient.listImagesCmd().exec();

            boolean chromaImageExists = images.stream()
                    .anyMatch(image -> image.getRepoTags() != null &&
                            Arrays.stream(image.getRepoTags())
                                    .anyMatch(tag -> tag.contains("chromadb/chroma")));

            if (!chromaImageExists) {
                this.message = "ChromaDB docker image not found";
                return false;
            }

            // Check if ChromaDB container is running
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)  // Show both running and stopped containers
                    .exec();

            boolean chromaContainerExists = containers.stream()
                    .anyMatch(container -> container.getImage().contains("chromadb/chroma"));

            Integer dbPort = DevoxxGenieStateService.getInstance().getIndexerPort();

            if (!chromaContainerExists) {
                this.message = "ChromaDB container not found";
                return false;
            }

            if (containers.stream()
                          .filter(container -> container.getImage().contains("chromadb/chroma"))
                          .filter(container -> Arrays.stream(container.getPorts())
                                  .anyMatch(port -> port.getPublicPort() != null && port.getPublicPort().equals(dbPort)))
                                  .noneMatch(container -> "running".equalsIgnoreCase(container.getState()))) {
                this.message = "ChromaDB container not running on port " + dbPort;
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Add these new methods
    public boolean isImagePresent() {
        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
            List<Image> images = dockerClient.listImagesCmd().exec();
            return images.stream()
                    .anyMatch(image -> image.getRepoTags() != null &&
                            Arrays.stream(image.getRepoTags())
                                    .anyMatch(tag -> tag.contains("chromadb/chroma")));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isContainerRunning() {
        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec();
            return containers.stream()
                    .anyMatch(container -> container.getImage().contains("chromadb/chroma") &&
                            "running".equalsIgnoreCase(container.getState()));
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return "ChromaDB running check";
    }

    public String getValidationMessage() {
        return this.message;
    }

    public String getCommand() {
        return ValidatorType.CHROMADB.name().toLowerCase();
    }
}
