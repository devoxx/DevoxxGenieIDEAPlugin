package com.devoxx.genie.service.acp.model;

/**
 * Identifies the ACP client to the agent during the {@code initialize} handshake.
 */
public class ClientInfo {
    public String name;
    public String version;

    public ClientInfo() {}

    public ClientInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }
}
