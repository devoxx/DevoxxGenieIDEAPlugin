package com.devoxx.genie.service.chromadb.exception;

public class ChromaDBException extends Exception {

    public ChromaDBException(String message) {
        super(message);
    }

    public ChromaDBException(String message, Throwable cause) {
        super(message, cause);
    }
}
