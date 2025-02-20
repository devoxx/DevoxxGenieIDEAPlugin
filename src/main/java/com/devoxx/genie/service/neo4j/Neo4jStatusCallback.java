package com.devoxx.genie.service.neo4j;

public interface Neo4jStatusCallback {
    void onSuccess();
    void onError(String message);
}
