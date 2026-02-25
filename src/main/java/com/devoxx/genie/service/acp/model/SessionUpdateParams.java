package com.devoxx.genie.service.acp.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * Parameters for the ACP {@code session/update} notification, carrying
 * incremental updates (e.g. agent message chunks) for an active session.
 */
public class SessionUpdateParams {
    @Getter
    public JsonNode update;
}
