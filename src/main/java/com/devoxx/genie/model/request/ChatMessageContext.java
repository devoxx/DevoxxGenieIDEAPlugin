package com.devoxx.genie.model.request;

import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ChatMessageContext {
    private final LocalDateTime createdOn = LocalDateTime.now();
    private String name;
    private Project project;
    private String llmProvider;
    private String modelName;
    private Integer timeout;
    private String userPrompt;
    private UserMessage userMessage;
    private AiMessage aiMessage;
    private String context;
    private EditorInfo editorInfo;
    private ChatLanguageModel chatLanguageModel;

    public boolean hasFiles() {
        return editorInfo != null && editorInfo.getSelectedFiles() != null && !editorInfo.getSelectedFiles().isEmpty();
    }
}
