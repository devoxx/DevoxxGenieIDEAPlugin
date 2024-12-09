package com.devoxx.genie.service.chromadb.model;

public record ChromaCollection(
    String id,
    String name,
    Integer dimension,
    String tenant,
    String database) { }