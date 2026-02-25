package com.devoxx.genie.service.acp.model;

import lombok.Getter;

/**
 * Identifies the ACP client to the agent during the {@code initialize} handshake.
 */

public class ClientInfo {
    @Getter
    private final String name;

    @Getter
    private final String version;

    public ClientInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }
}
