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
import org.jetbrains.annotations.NotNull;

@Service
@State(name = "com.devoxx.genie.ui.SettingsState", storages = @Storage("DevoxxGenieSettingsPlugin.xml"))
public final class SettingsState implements PersistentStateComponent<SettingsState> {

    private String ollamaModelUrl = Constant.OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = Constant.LMSTUDIO_MODEL_URL;
    private String gpt4allModelUrl = Constant.GPT4ALL_MODEL_URL;

    @OptionTag(converter = DoubleConverter.class)
    private Double temperature = Constant.TEMPERATURE;
    @OptionTag(converter = DoubleConverter.class)
    private Double topP = Constant.TOP_P;

    private Integer timeout = Constant.TIMEOUT;
    private Integer maxRetries = Constant.MAX_RETRIES;

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

    public String getOllamaModelUrl() {
        return ollamaModelUrl;
    }

    public void setOllamaModelUrl(String ollamaModelUrl) {
        this.ollamaModelUrl = ollamaModelUrl;
    }

    public String getLmstudioModelUrl() {
        return lmstudioModelUrl;
    }

    public void setLmstudioModelUrl(String lmstudioModelUrl) {
        this.lmstudioModelUrl = lmstudioModelUrl;
    }

    public String getGpt4allModelUrl() {
        return gpt4allModelUrl;
    }

    public void setGpt4allModelUrl(String gpt4allModelUrl) {
        this.gpt4allModelUrl = gpt4allModelUrl;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}
