package com.devoxx.genie.service.settings.llmconfig;

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
    name = "com.devoxx.genie.ui.LLMConfigState",
    storages = @Storage("DevoxxGenieLLMConfigPlugin.xml")
)
public final class LLMConfigStateService implements PersistentStateComponent<LLMConfigStateService> {

    public static LLMConfigStateService getInstance() {
        return ApplicationManager.getApplication().getService(LLMConfigStateService.class);
    }

    // Prompt fields
    private String systemPrompt = SYSTEM_PROMPT;
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

    // Enable AST mode
    private Boolean astMode = AST_MODE;
    private Boolean astParentClass = AST_PARENT_CLASS;
    private Boolean astClassReference = AST_CLASS_REFERENCE;
    private Boolean astFieldReference = AST_FIELD_REFERENCE;

    @Override
    public LLMConfigStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LLMConfigStateService state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
