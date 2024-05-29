package com.devoxx.genie.service;

import com.devoxx.genie.ui.util.DoubleConverter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import static com.devoxx.genie.model.Constant.*;

@Getter
@Setter
@Service
@State(
    name = "com.devoxx.genie.ui.SettingsState",
    storages = @Storage("DevoxxGenieSettingsPlugin.xml")
)
public final class SettingsStateService implements PersistentStateComponent<SettingsStateService> {

    public static SettingsStateService getInstance() {
        return ApplicationManager.getApplication().getService(SettingsStateService.class);
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
    private String fireworksKey = "";
    private String deepInfraKey = "";
    private String geminiKey = "";

    // Search API Keys
    private Boolean hideSearchButtonsFlag = HIDE_SEARCH_BUTTONS;
    private String googleSearchKey = "";
    private String googleCSIKey = "";
    private String tavilySearchKey = "";

    // Prompt fields
    private String testPrompt = TEST_PROMPT;
    private String reviewPrompt = REVIEW_PROMPT;
    private String explainPrompt = EXPLAIN_PROMPT;
    private String customPrompt = CUSTOM_PROMPT;

    // LLM settings
    @OptionTag(converter = DoubleConverter.class)
    private Double temperature = TEMPERATURE;

    @OptionTag(converter = DoubleConverter.class)
    private Double topP = TOP_P;

    private Integer timeout = TIMEOUT;
    private Integer maxRetries = MAX_RETRIES;
    private Integer chatMemorySize = MAX_MEMORY;

    // Was unable to make it work with Integer for some unknown reason
    private String maxOutputTokens = MAX_OUTPUT_TOKENS.toString();

    // Last selected LLM provider and model name
    private String lastSelectedProvider;
    private String lastSelectedModel;

    // Enable stream mode
    private Boolean streamMode = STREAM_MODE;

    // Enable AST mode
    private Boolean astMode = AST_MODE;
    private Boolean astParentClass = AST_PARENT_CLASS;
    private Boolean astClassReference = AST_CLASS_REFERENCE;
    private Boolean astFieldReference = AST_FIELD_REFERENCE;

    @Override
    public SettingsStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsStateService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
