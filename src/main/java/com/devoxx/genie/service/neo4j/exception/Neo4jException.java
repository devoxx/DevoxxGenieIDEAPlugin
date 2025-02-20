package com.devoxx.genie.service.neo4j.exception;

public class Neo4jException extends Exception {

    public Neo4jException(String message) {
        super(message);
    }

    public Neo4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
