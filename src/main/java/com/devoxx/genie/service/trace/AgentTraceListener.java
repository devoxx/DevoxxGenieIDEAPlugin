package com.devoxx.genie.service.trace;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

/**
 * Application-wide notifications about agent traces (task-280), so chat tabs can show or enable
 * their "Upload to Hugging Face" button without polling the database.
 */
public interface AgentTraceListener {

    Topic<AgentTraceListener> TOPIC = Topic.create("DevoxxGenie agent traces", AgentTraceListener.class);

    /** A turn of {@code conversationId} was written to {@code trace_events}. Fired on a background thread. */
    default void traceWritten(@NotNull String conversationId) {}

    /** Trace recording or Hugging Face upload settings were applied. */
    default void settingsChanged() {}
}
