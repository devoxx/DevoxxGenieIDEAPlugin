package com.devoxx.genie.service.acp.model;

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
