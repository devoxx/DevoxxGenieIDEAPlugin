package com.devoxx.genie.service.neo4j.exception;

public class DockerException extends Exception {

    public DockerException(String message) {
        super(message);
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }
}
