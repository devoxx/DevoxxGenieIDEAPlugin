package com.devoxx.genie.service.acp.model;

import lombok.Getter;

import java.util.List;

/**
 * Parameters for the ACP {@code session/prompt} request, containing
 * the session identifier and the user's prompt as a list of content blocks.
 */
public class SessionPromptParams {
    @Getter
    public String sessionId;

    @Getter
    public List<ContentBlock> prompt;

    public SessionPromptParams(String sessionId, String text) {
        this.sessionId = sessionId;
        ContentBlock block = new ContentBlock();
        block.type = "text";
        block.text = text;
        this.prompt = List.of(block);
    }
}
