package com.devoxx.genie.service.acp.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parameters for the ACP {@code session/update} notification, carrying
 * incremental updates (e.g. agent message chunks) for an active session.
 */
public class SessionUpdateParams {
    public String sessionId;
    public JsonNode update;
}
