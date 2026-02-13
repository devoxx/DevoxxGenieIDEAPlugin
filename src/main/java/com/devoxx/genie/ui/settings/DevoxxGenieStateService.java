package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.SubAgentConfig;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.model.spec.CliToolConfig;
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
import org.jetbrains.annotations.Nullable;

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

    private String submitShortcutWindows = "shift ENTER";
    private String submitShortcutMac = "shift ENTER";
    private String submitShortcutLinux = "shift ENTER";
    
    private String newlineShortcutWindows = "ctrl ENTER";
    private String newlineShortcutMac = "meta ENTER";  // Command+Enter on Mac
    private String newlineShortcutLinux = "ctrl ENTER";

    // Default excluded files for scan project
    private List<String> excludedFiles = new ArrayList<>(Arrays.asList(
            "package-lock.json", "yarn.lock", ".env", "build.gradle", "settings.gradle"
    ));

    private List<CustomPrompt> customPrompts = new ArrayList<>();

    private final List<CustomPrompt> defaultPrompts = Arrays.asList(
            new CustomPrompt(TEST_COMMAND, TEST_PROMPT),
            new CustomPrompt(EXPLAIN_COMMAND, EXPLAIN_PROMPT),
            new CustomPrompt(REVIEW_COMMAND, REVIEW_PROMPT),
            new CustomPrompt(TDG_COMMAND, TDG_PROMPT),
            new CustomPrompt(FIND_COMMAND, FIND_PROMPT),
            new CustomPrompt(HELP_COMMAND, HELP_PROMPT),
            new CustomPrompt(INIT_COMMAND, INIT_PROMPT)
    );

    private List<LanguageModel> languageModels = new ArrayList<>();

    private Boolean useFileInEditor = true;

    // Settings panel
    private Boolean ragEnabled = false;

    // Search panel
    private Boolean ragActivated = false;
    private Boolean webSearchActivated = false;

    // Indexer
    private Integer indexerPort = 8000;
    private Integer indexerMaxResults = 10;
    private Double indexerMinScore = 0.7;

    // Local LLM URL fields
    private String ollamaModelUrl = OLLAMA_MODEL_URL;
    private String lmstudioModelUrl = LMSTUDIO_MODEL_URL;
    private Integer lmStudioFallbackContextLength;
    private String gpt4allModelUrl = GPT4ALL_MODEL_URL;
    private String janModelUrl = JAN_MODEL_URL;
    private String llamaCPPUrl = LLAMA_CPP_MODEL_URL;

    // Local custom OpenAI-compliant LLM fields
    private String customOpenAIUrl = "";
    private String customOpenAIModelName = "";
    private String customOpenAIApiKey = "";

    // Local LLM Providers
    private boolean isOllamaEnabled = true;
    private boolean isLmStudioEnabled = true;
    private boolean isGpt4AllEnabled = true;
    private boolean isJanEnabled = true;
    private boolean isLlamaCPPEnabled = true;

    // Local custom OpenAI-compliant LLM fields
    private boolean isCustomOpenAIUrlEnabled = false;
    private boolean isCustomOpenAIForceHttp11 = false;
    private boolean isCustomOpenAIModelNameEnabled = false;
    private boolean isCustomOpenAIApiKeyEnabled = false;

    // Remote LLM Providers
    private boolean isOpenAIEnabled = false;
    private boolean isMistralEnabled = false;
    private boolean isAnthropicEnabled = false;
    private boolean isGroqEnabled = false;
    private boolean isDeepInfraEnabled = false;
    private boolean isGoogleEnabled = false;
    private boolean isDeepSeekEnabled = false;
    private boolean isOpenRouterEnabled = false;
    private boolean isGrokEnabled = false;
    private boolean isKimiEnabled = false;
    private boolean isGlmEnabled = false;

    // LLM API Keys
    private String openAIKey = "";
    private String mistralKey = "";
    private String anthropicKey = "";
    private String groqKey = "";
    private String deepInfraKey = "";
    private String geminiKey = "";
    private String deepSeekKey = "";
    private String openRouterKey = "";
    private String grokKey = "";
    private String kimiKey = "";
    private String glmKey = "";
    private String azureOpenAIEndpoint = "";
    private String azureOpenAIDeployment = "";
    private String azureOpenAIKey = "";
    private String awsAccessKeyId = "";
    private String awsSecretKey = "";
    private String awsProfileName = "";
    private String awsRegion = "";

    // Search API Keys
    private Boolean isWebSearchEnabled = ENABLE_WEB_SEARCH;

    private boolean tavilySearchEnabled = false;
    private boolean googleSearchEnabled = false;

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

    // DEVOXXGENIE.md generation options
    private Boolean createDevoxxGenieMd = false;
    private Boolean includeProjectTree = false;
    private Integer projectTreeDepth = 3;
    private Boolean useDevoxxGenieMdInPrompt = false;

    // CLAUDE.md / AGENTS.md inclusion option
    private Boolean useClaudeOrAgentsMdInPrompt = true;

    private Boolean showAzureOpenAIFields = false;
    private Boolean showAwsFields = false;
    private Boolean shouldPowerFromAWSProfile = false;
    private Boolean shouldEnableAWSRegionalInference = true;

    @Setter
    private Boolean useGitIgnore = true;

    private List<String> excludedDirectories = new ArrayList<>(Arrays.asList(
            "build", ".git", "bin", "out", "target", "node_modules", ".idea"
    ));

    private List<String> includedFileExtensions = new ArrayList<>(Arrays.asList(
            "java", "kt", "groovy", "scala", "xml", "json", "yaml", "yml", "properties", "txt", "md", "js", "ts", "css", "scss", "html"
    ));

    private Map<String, Double> modelInputCosts = new HashMap<>();
    private Map<String, Double> modelOutputCosts = new HashMap<>();

    private Map<String, Integer> modelWindowContexts = new HashMap<>();
    private Integer defaultWindowContext = 8000;
    
    // MCP settings
    private MCPSettings mcpSettings = new MCPSettings();
    private Boolean mcpEnabled = false;
    private Boolean mcpDebugLogsEnabled = false;
    private Boolean mcpApprovalRequired = true;
    private Integer mcpApprovalTimeout = MCP_APPROVAL_TIMEOUT;

    // Agent mode settings
    private Boolean agentModeEnabled = false;
    private Integer agentMaxToolCalls = AGENT_MAX_TOOL_CALLS;
    private Boolean agentAutoApproveReadOnly = false;
    private Boolean agentWriteApprovalRequired = true;
    private Boolean agentDebugLogsEnabled = false;
    private List<String> disabledAgentTools = new ArrayList<>();

    // PSI (Program Structure Interface) tools
    private Boolean psiToolsEnabled = true;

    // Parallel exploration (sub-agent) settings
    private Boolean parallelExploreEnabled = true;
    private Integer subAgentMaxToolCalls = SUB_AGENT_MAX_TOOL_CALLS;
    @Getter(AccessLevel.NONE)
    private Integer subAgentParallelism = SUB_AGENT_DEFAULT_PARALLELISM;
    private Integer subAgentTimeoutSeconds = SUB_AGENT_TIMEOUT_SECONDS;
    private String subAgentModelProvider = "";
    private String subAgentModelName = "";
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<SubAgentConfig> subAgentConfigs = new ArrayList<>();

    // Test execution settings
    private Boolean testExecutionEnabled = true;
    private Integer testExecutionTimeoutSeconds = TEST_EXECUTION_DEFAULT_TIMEOUT;
    private String testExecutionCustomCommand = "";

    // Spec Driven Development settings
    private Boolean specBrowserEnabled = false;
    private String specDirectory = "backlog";
    private Boolean autoInjectSpecContext = true;
    private Integer specTaskRunnerTimeoutMinutes = 10;

    // CLI tool runner settings for spec tasks
    private List<CliToolConfig> cliTools = new ArrayList<>();
    private String specRunnerMode = "llm";          // "llm" or "cli"
    private String specSelectedCliTool = "";         // name of selected CLI tool

    // ACP tool runner settings
    private List<com.devoxx.genie.model.spec.AcpToolConfig> acpTools = new ArrayList<>();

    // Inline completion settings
    private String inlineCompletionProvider = "";  // "", "Ollama", or "LMStudio"
    private String inlineCompletionModel = "";
    private Integer inlineCompletionMaxTokens = 64;
    private Integer inlineCompletionTimeoutMs = 5000;
    private Double inlineCompletionTemperature = 0.0;
    private Integer inlineCompletionDebounceMs = 300;

    // Welcome content cache
    private String welcomeContentCachedJson = "";
    private long welcomeContentLastFetchTimestamp = 0L;
    private String welcomeContentPluginVersion = "";

    // Appearance settings
    private Double lineHeight = 1.6;  // Default line height multiplier
    private Integer messagePadding = 10;  // Padding inside messages in px
    private Integer messageMargin = 10;  // Margin between messages in px
    private Integer borderWidth = 4;  // Border width in px
    private Integer cornerRadius = 4;  // Border radius in px

    private String userMessageBorderColor = "#FF5400";  // Devoxx orange
    private String assistantMessageBorderColor = "#0095C9";  // Devoxx blue
    private String userMessageBackgroundColor = "#fff9f0";  // Light theme default
    private String assistantMessageBackgroundColor = "#f0f7ff";  // Light theme default
    private String userMessageTextColor = "#000000";  // Default text color for user messages
    private String assistantMessageTextColor = "#000000";  // Default text color for assistant messages
    @Getter
    @Setter
    private Boolean useCustomFontSize = false;
    private Integer customFontSize = 14;
    @Getter
    @Setter
    private Boolean useCustomCodeFontSize = false;
    private Integer customCodeFontSize = 14;
    @Getter
    @Setter
    private Boolean useRoundedCorners = true;
    
    @Getter
    @Setter
    private Boolean useCustomColors = false;

    @Setter(AccessLevel.NONE)
    private List<Runnable> loadListeners = new ArrayList<>();

    public DevoxxGenieStateService() {
        initializeUserPrompt();
    }

    private void initializeUserPrompt() {
        //If User prompt happens to be empty then we load the default list
        if (customPrompts == null || customPrompts.isEmpty()) {
            customPrompts = new ArrayList<>(defaultPrompts);
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
        initializeUserPrompt();

        // Notify all listeners that the state has been loaded
        for (Runnable listener : loadListeners) {
            listener.run();
        }
    }

    public void addLoadListener(Runnable listener) {
        loadListeners.add(listener);
    }

    private void initializeDefaultCostsIfEmpty() {
        for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_INPUT_COSTS.entrySet()) {
            String key = entry.getKey().provider().getName() + ":" + entry.getKey().modelName();
            modelInputCosts.put(key, entry.getValue());
        }
        for (Map.Entry<DefaultLLMSettingsUtil.CostKey, Double> entry : DefaultLLMSettingsUtil.DEFAULT_OUTPUT_COSTS.entrySet()) {
            String key = entry.getKey().provider().getName() + ":" + entry.getKey().modelName();
            modelOutputCosts.put(key, entry.getValue());
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

    public boolean isAzureOpenAIEnabled() {
        return showAzureOpenAIFields &&
                !azureOpenAIKey.isEmpty() &&
                !azureOpenAIEndpoint.isEmpty() &&
                !azureOpenAIDeployment.isEmpty();
    }

    public boolean isAwsEnabled() {
        return showAwsFields &&
                ((!awsAccessKeyId.isEmpty() && !awsSecretKey.isEmpty())
                || (shouldPowerFromAWSProfile && !awsProfileName.isEmpty()))
                && !awsRegion.isEmpty();
    }

    public List<SubAgentConfig> getSubAgentConfigs() {
        return subAgentConfigs;
    }

    /**
     * Sets the sub-agent configs and syncs the parallelism field to match the config count.
     */
    public void setSubAgentConfigs(List<SubAgentConfig> configs) {
        this.subAgentConfigs = configs != null ? new ArrayList<>(configs) : new ArrayList<>();
        this.subAgentParallelism = Math.max(1, this.subAgentConfigs.size());
    }

    /**
     * Returns the effective parallelism: driven by configs size when configs exist,
     * otherwise falls back to the stored field value.
     */
    public Integer getSubAgentParallelism() {
        if (subAgentConfigs != null && !subAgentConfigs.isEmpty()) {
            return subAgentConfigs.size();
        }
        return subAgentParallelism != null ? subAgentParallelism : SUB_AGENT_DEFAULT_PARALLELISM;
    }

    /**
     * Returns the SubAgentConfig for the given agent index.
     * Falls back to the default provider/model if no per-agent config exists or the config is empty.
     */
    public @NotNull SubAgentConfig getEffectiveSubAgentConfig(int index) {
        if (subAgentConfigs != null && index >= 0 && index < subAgentConfigs.size()) {
            SubAgentConfig cfg = subAgentConfigs.get(index);
            if (cfg != null && cfg.getModelProvider() != null && !cfg.getModelProvider().isEmpty()) {
                return cfg;
            }
        }
        // Fall back to default
        return new SubAgentConfig(
                subAgentModelProvider != null ? subAgentModelProvider : "",
                subAgentModelName != null ? subAgentModelName : ""
        );
    }

    public @Nullable String getConfigValue(@NotNull String key) {
        return switch (key) {
            case "janModelUrl" -> getJanModelUrl();
            case "gpt4allModelUrl" -> getGpt4allModelUrl();
            case "lmStudioModelUrl" -> getLmstudioModelUrl();
            case "ollamaModelUrl" -> getOllamaModelUrl();
            default -> null;
        };
    }
}
