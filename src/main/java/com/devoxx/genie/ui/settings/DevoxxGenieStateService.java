package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.devoxx.genie.model.Constant.*;

@Getter
@Setter
@State(
    name = "com.devoxx.genie.ui.SettingsState",
    storages = @Storage("DevoxxGenieSettingsPlugin.xml")
)
public final class DevoxxGenieStateService implements PersistentStateComponent<DevoxxGenieStateService>, DevoxxGenieSettingsService {

    public static DevoxxGenieStateService getInstance() {
        return ApplicationManager.getApplication().getService(DevoxxGenieStateService.class);
    }

    private List<CustomPrompt> customPrompts = new ArrayList<>();

    private List<LanguageModel> languageModels = new ArrayList<>();

    private Boolean showExecutionTime = true;

    // Local LLM URL fields
    private String ollamaModelUrl = OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = LMSTUDIO_MODEL_URL;
    private String gpt4allModelUrl = GPT4ALL_MODEL_URL;
    private String janModelUrl = JAN_MODEL_URL;
    private String exoModelUrl = EXO_MODEL_URL;
    private String llamaCPPUrl = LLAMA_CPP_MODEL_URL;

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

    // Last selected language model
    private Map<String, String> lastSelectedProvider;
    private Map<String, String> lastSelectedLanguageModel;

    // Enable stream mode
    private Boolean streamMode = STREAM_MODE;

    // LLM settings
    private Double temperature = TEMPERATURE;
    private Double topP = TOP_P;

    private Integer timeout = TIMEOUT;
    private Integer maxRetries = MAX_RETRIES;
    private Integer chatMemorySize = MAX_MEMORY;
    private Integer maxOutputTokens = MAX_OUTPUT_TOKENS;

    private String systemPrompt = SYSTEM_PROMPT;
    private String testPrompt = TEST_PROMPT;
    private String reviewPrompt = REVIEW_PROMPT;
    private String explainPrompt = EXPLAIN_PROMPT;

    private Boolean excludeJavaDoc = false;

    private List<String> excludedDirectories = new ArrayList<>(Arrays.asList(
        "build", ".git", "bin", "out", "target", "node_modules", ".idea"
    ));

    private List<String> includedFileExtensions = new ArrayList<>(Arrays.asList(
        "java", "kt", "groovy", "scala", "xml", "json", "yaml", "yml", "properties", "txt", "md"
    ));

    private Map<String, Double> modelInputCosts = new HashMap<>();
    private Map<String, Double> modelOutputCosts = new HashMap<>();

    private Map<String, Integer> modelWindowContexts = new HashMap<>();
    private Integer defaultWindowContext = 8000;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<Runnable> loadListeners = new ArrayList<>();

    public DevoxxGenieStateService() {
        initializeDefaultPrompts();
    }

    private void initializeDefaultPrompts() {
        if (customPrompts.isEmpty()) {
            customPrompts.add(new CustomPrompt("test", TEST_PROMPT));
            customPrompts.add(new CustomPrompt("explain", EXPLAIN_PROMPT));
            customPrompts.add(new CustomPrompt("review", REVIEW_PROMPT));
        }
    }

    @Override
    public DevoxxGenieStateService getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull DevoxxGenieStateService state) {
        XmlSerializerUtil.copyBean(state, this);
        initializeDefaultCostsIfEmpty();
        initializeDefaultPrompts();

        // Notify all listeners that the state has been loaded
        for (Runnable listener : loadListeners) {
            listener.run();
        }
    }

    public void addLoadListener(Runnable listener) {
        loadListeners.add(listener);
    }

    public void setModelCost(ModelProvider provider,
                             String modelName,
                             double inputCost,
                             double outputCost) {
        if (DefaultLLMSettingsUtil.isApiKeyBasedProvider(provider)) {
            String key = provider.getName() + ":" + modelName;
            modelInputCosts.put(key, inputCost);
            modelOutputCosts.put(key, outputCost);
        }
    }

    public double getModelInputCost(@NotNull ModelProvider provider, String modelName) {
        String key = provider.getName() + ":" + modelName;
        double cost = modelInputCosts.getOrDefault(key, 0.0);
        if (cost == 0.0) {
            DefaultLLMSettingsUtil.CostKey costKey = new DefaultLLMSettingsUtil.CostKey(provider, modelName);
            cost = DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.getOrDefault(costKey, 0.0);
            if (cost == 0.0) {
                // Fallback to similar model names
                for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.entrySet()) {
                    if (entry.getKey().provider == provider && entry.getKey().modelName.startsWith(modelName.split("-")[0])) {
                        cost = entry.getValue();
                        break;
                    }
                }
            }
        }
        return cost;
    }

    private void initializeDefaultCostsIfEmpty() {
        if (modelInputCosts.isEmpty()) {
            DefaultLLMSettingsUtil.initializeDefaultCosts();
            for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.entrySet()) {
                String key = entry.getKey().provider.getName() + ":" + entry.getKey().modelName;
                modelInputCosts.put(key, entry.getValue());
            }
        }
        if (modelOutputCosts.isEmpty()) {
            DefaultLLMSettingsUtil.initializeDefaultCosts();
            for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_OUTPUT_COSTS.entrySet()) {
                String key = entry.getKey().provider.getName() + ":" + entry.getKey().modelName;
                modelOutputCosts.put(key, entry.getValue());
            }
        }
    }

    public void setModelWindowContext(ModelProvider provider, String modelName, int windowContext) {
        if (DefaultLLMSettingsUtil.isApiKeyBasedProvider(provider)) {
            String key = provider.getName() + ":" + modelName;
            modelWindowContexts.put(key, windowContext);
        }
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull List<LanguageModel> getLanguageModels() {
        return new ArrayList<>(languageModels);
    }

    public void setLanguageModels(List<LanguageModel> models) {
        this.languageModels = new ArrayList<>(models);
    }

    public void setSelectedLanguageModel(@NotNull String projectLocation, String selectedLanguageModel) {
        if (lastSelectedLanguageModel == null) {
            lastSelectedLanguageModel = new HashMap<>();
        }
        lastSelectedLanguageModel.put(projectLocation, selectedLanguageModel);
    }

    public String getSelectedLanguageModel(@NotNull String projectLocation) {
        if (lastSelectedLanguageModel != null) {
            return lastSelectedLanguageModel.getOrDefault(projectLocation, "");
        } else {
            return "";
        }
    }

    public void setSelectedProvider(@NotNull String projectLocation, String selectedProvider) {
        if (lastSelectedProvider == null) {
            lastSelectedProvider = new HashMap<>();
        }
        lastSelectedProvider.put(projectLocation, selectedProvider);
    }

    public String getSelectedProvider(@NotNull String projectLocation) {
        if (lastSelectedProvider != null) {
            return lastSelectedProvider.getOrDefault(projectLocation, ModelProvider.Ollama.getName());
        } else {
            return ModelProvider.Ollama.getName();
        }
    }
}
