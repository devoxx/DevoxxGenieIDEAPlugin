package com.devoxx.genie.service.settings.prompts;

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
    name = "com.devoxx.genie.ui.PromptSettingsState",
    storages = @Storage("DevoxxGeniePromptSettingsPlugin.xml")
)
public final class PromptSettingsStateService implements PersistentStateComponent<PromptSettingsStateService> {

    // Prompt fields
    private String systemPrompt = SYSTEM_PROMPT;
    private String testPrompt = TEST_PROMPT;
    private String reviewPrompt = REVIEW_PROMPT;
    private String explainPrompt = EXPLAIN_PROMPT;
    private String customPrompt = CUSTOM_PROMPT;

    public static PromptSettingsStateService getInstance() {
        return ApplicationManager.getApplication().getService(PromptSettingsStateService.class);
    }

    @Override
    public PromptSettingsStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PromptSettingsStateService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
