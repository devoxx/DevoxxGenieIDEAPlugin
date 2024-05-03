package com.devoxx.genie.model.request;

import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class PromptContext {
    private Project project;
    private String userPrompt;
    private String context;
    private EditorInfo editorInfo;
    private String llmProvider;
    private String modelName;
    private ChatLanguageModel chatLanguageModel;
    private LocalDateTime createdOn = LocalDateTime.now();
}
