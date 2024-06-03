package com.devoxx.genie.service.settings.llm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
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
public final class LLMStateService implements PersistentStateComponent<LLMStateService> {

    public static LLMStateService getInstance() {
        return ApplicationManager.getApplication().getService(LLMStateService.class);
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

    // Enable stream mode
    private Boolean streamMode = STREAM_MODE;

    @Override
    public LLMStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LLMStateService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
