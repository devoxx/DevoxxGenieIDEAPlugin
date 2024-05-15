package com.devoxx.genie.ui;

import com.devoxx.genie.model.Constant;
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

@Getter
@Setter
@Service
@State(name = "com.devoxx.genie.ui.SettingsState", storages = @Storage("DevoxxGenieSettingsPlugin.xml"))
public final class SettingsState implements PersistentStateComponent<SettingsState> {

    // LLM URL fields
    private String ollamaModelUrl = Constant.OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = Constant.LMSTUDIO_MODEL_URL;
    private String gpt4allModelUrl = Constant.GPT4ALL_MODEL_URL;

    // LLM API Keys
    private String openAIKey = "";
    private String mistralKey = "";
    private String anthropicKey = "";
    private String groqKey = "";
    private String fireworksKey = "";
    private String deepInfraKey = "";

    // Prompt fields
    private String testPrompt = Constant.TEST_PROMPT;
    private String reviewPrompt = Constant.REVIEW_PROMPT;
    private String explainPrompt = Constant.EXPLAIN_PROMPT;
    private String customPrompt = Constant.CUSTOM_PROMPT;

    // LLM settings
    @OptionTag(converter = DoubleConverter.class)
    private Double temperature = Constant.TEMPERATURE;

    @OptionTag(converter = DoubleConverter.class)
    private Double topP = Constant.TOP_P;

    private Integer timeout = Constant.TIMEOUT;
    private Integer maxRetries = Constant.MAX_RETRIES;
    private Integer maxOutputTokens = Constant.MAX_OUTPUT_TOKENS;
    private Integer maxMemory = Constant.MAX_MEMORY;

    private String lastSelectedProvider;
    private String lastSelectedModel;

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
