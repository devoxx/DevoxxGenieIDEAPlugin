package com.devoxx.genie.service.acp.model;

import java.util.List;

public class SessionPromptParams {
    public String sessionId;
    public List<ContentBlock> prompt;

    public SessionPromptParams() {}

    public SessionPromptParams(String sessionId, String text) {
        this.sessionId = sessionId;
        ContentBlock block = new ContentBlock();
        block.type = "text";
        block.text = text;
        this.prompt = List.of(block);
    }
}
