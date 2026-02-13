package com.devoxx.genie.service.acp.model;

/**
 * Parameters for the ACP {@code initialize} request, sent by the client
 * to establish the protocol version and declare client capabilities.
 */
public class InitializeParams {
    public int protocolVersion;
    public ClientCapabilities clientCapabilities;
    public ClientInfo clientInfo;

    public InitializeParams() {}

    public InitializeParams(int protocolVersion, ClientCapabilities clientCapabilities, ClientInfo clientInfo) {
        this.protocolVersion = protocolVersion;
        this.clientCapabilities = clientCapabilities;
        this.clientInfo = clientInfo;
    }
}
