package com.devoxx.genie.model.request;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Setter
@Getter
public class ChatMessageContext {
    private final LocalDateTime createdOn = LocalDateTime.now();
    private String name;
    private Project project;
    private Integer timeout;
    private String userPrompt;
    private UserMessage userMessage;
    private AiMessage aiMessage;
    private String context;
    private EditorInfo editorInfo;
    private LanguageModel languageModel;
    private ChatLanguageModel chatLanguageModel;
    private StreamingChatLanguageModel streamingChatLanguageModel;
    private boolean webSearchRequested;
    private boolean fullProjectContextAdded = false;

    public boolean hasFiles() {
        return editorInfo != null && editorInfo.getSelectedFiles() != null && !editorInfo.getSelectedFiles().isEmpty();
    }
}
