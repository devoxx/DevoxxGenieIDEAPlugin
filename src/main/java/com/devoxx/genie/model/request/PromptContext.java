package com.devoxx.genie.model.request;

import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PromptContext {
    private Project project;
    private String userPrompt;
    private EditorInfo editorInfo;
    private ChatLanguageModel chatLanguageModel;
}
