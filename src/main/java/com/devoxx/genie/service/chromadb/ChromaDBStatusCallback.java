package com.devoxx.genie.service.chromadb;

public interface ChromaDBStatusCallback {
    void onSuccess();
    void onError(String message);
}
