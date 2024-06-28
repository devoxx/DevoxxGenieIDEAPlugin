package com.devoxx.genie.ui.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;

@Getter
@Setter
@State(
    name = "com.devoxx.genie.ui.SettingsState",
    storages = @Storage("DevoxxGenieSettingsPlugin.xml")
)
public final class DevoxxGenieStateService implements PersistentStateComponent<DevoxxGenieStateService> {

    public static DevoxxGenieStateService getInstance() {
        return ApplicationManager.getApplication().getService(DevoxxGenieStateService.class);
    }

    // Local LLM URL fields
    private String ollamaModelUrl = OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = LMSTUDIO_MODEL_URL;
    private String gpt4allModelUrl = GPT4ALL_MODEL_URL;
    private String janModelUrl = JAN_MODEL_URL;

    // LLM API Keys
    private String openAIKey = "";
    private String mistralKey = "";
    private String anthropicKey = "";
    private String groqKey = "";
    private String deepInfraKey = "";
    private String geminiKey = "";

    // Search API Keys
    private Boolean hideSearchButtonsFlag = HIDE_SEARCH_BUTTONS;
    private String googleSearchKey = "";
    private String googleCSIKey = "";
    private String tavilySearchKey = "";
    private Integer maxSearchResults = MAX_SEARCH_RESULTS;

    // Last selected LLM provider and model name
    private String lastSelectedProvider = "";
    private String lastSelectedModel = "";

    // Enable stream mode
    private Boolean streamMode = STREAM_MODE;

    // LLM settings
    private Double temperature = TEMPERATURE;
    private Double topP = TOP_P;

    private Integer timeout = TIMEOUT;
    private Integer maxRetries = MAX_RETRIES;
    private Integer chatMemorySize = MAX_MEMORY;
    private Integer maxOutputTokens = MAX_OUTPUT_TOKENS;

    // Enable AST mode
    private Boolean astMode = AST_MODE;
    private Boolean astParentClass = AST_PARENT_CLASS;
    private Boolean astClassReference = AST_CLASS_REFERENCE;
    private Boolean astFieldReference = AST_FIELD_REFERENCE;

    private String systemPrompt = SYSTEM_PROMPT;
    private String testPrompt = TEST_PROMPT;
    private String reviewPrompt = REVIEW_PROMPT;
    private String explainPrompt = EXPLAIN_PROMPT;
    private String customPrompt = CUSTOM_PROMPT;

    private List<String> excludedDirectories = new ArrayList<>(Arrays.asList(
        "build", ".git", "bin", "out", "target", "node_modules", ".idea"
    ));

    private List<String> includedFileExtensions = new ArrayList<>(Arrays.asList(
        "java", "kt", "groovy", "scala", "xml", "json", "yaml", "yml", "properties", "txt", "md"
    ));

    @Override
    public DevoxxGenieStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DevoxxGenieStateService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
