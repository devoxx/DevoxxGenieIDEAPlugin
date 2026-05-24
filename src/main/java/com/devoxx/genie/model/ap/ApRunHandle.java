package com.devoxx.genie.model.ap;

/**
 * Metadata returned when an {@code ap run} starts.
 * Populated from the first JSON object on stdout before the stream events begin.
 */
public record ApRunHandle(String sessionId,
                          String agentId,
                          String agentName,
                          String projectId,
                          String openUrl) {
}
