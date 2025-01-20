package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;

public class DockerValidator implements Validator {

    private String message;

    @Override
    public boolean isValid() {
        try (DockerClient dockerClient = DockerUtil.getDockerClient()) {
           return true;
        } catch (Exception e) {
            this.message = "Docker is not installed";
            return false;
        }
    }

    @Override
    public String getMessage() {
        return "Docker is installed";
    }

    @Override
    public String getErrorMessage() {
        return this.message;
    }

    @Override
    public ValidatorType getCommand() {
        return ValidatorType.DOCKER;
    }
}
