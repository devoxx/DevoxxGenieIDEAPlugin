package com.devoxx.genie.ui.settings.llmconfig;

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
    name = "com.devoxx.genie.ui.LLMConfigState",
    storages = @Storage("DevoxxGenieLLMConfigPlugin.xml")
)
public final class LLMConfigStateService implements PersistentStateComponent<LLMConfigStateService> {

    public static LLMConfigStateService getInstance() {
        return ApplicationManager.getApplication().getService(LLMConfigStateService.class);
    }

    // LLM settings
    private Double temperature = TEMPERATURE;
    private Double topP = TOP_P;

    private Integer timeout = TIMEOUT;
    private Integer maxRetries = MAX_RETRIES;
    private Integer chatMemorySize = MAX_MEMORY;
    private Integer maxOutputTokens = MAX_OUTPUT_TOKENS;

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
