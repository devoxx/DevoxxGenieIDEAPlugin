package com.devoxx.genie.service.acp.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Result returned by the agent in response to a {@code session/new} request,
 * containing the identifier for the newly created session.
 */
public class SessionNewResult {
    @Getter @Setter
    private String sessionId;
}
