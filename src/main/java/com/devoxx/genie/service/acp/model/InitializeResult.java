package com.devoxx.genie.service.acp.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Result returned by the agent in response to the {@code initialize} request,
 * confirming the protocol version in use.
 */
public class InitializeResult {
    @Getter @Setter
    private String protocolVersion;
}
