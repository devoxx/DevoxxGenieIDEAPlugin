package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.rag.RAGLogMessage;

/** Listener interface for RAG retrieval events published to {@code AppTopics.RAG_LOG_MSG}. */
public interface RAGLoggingMessage {
    void onRAGLoggingMessage(RAGLogMessage message);
}
