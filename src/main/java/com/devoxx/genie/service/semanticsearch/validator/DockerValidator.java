package com.devoxx.genie.service.semanticsearch.validator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerValidator implements Validator {

    @Override
    public boolean isValid() {
        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
           return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return "Docker running check";
    }

    public String getCommand() {
        return ValidatorType.DOCKER.name().toLowerCase();
    }
}
