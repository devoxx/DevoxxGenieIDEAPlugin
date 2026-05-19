package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.SubAgentConfig;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.automation.EventAutomationSettings;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
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
    private Boolean ollamaContextWindowOverrideEnabled = false;
    private String lmstudioModelUrl = LMSTUDIO_MODEL_URL;
    private Integer lmStudioFallbackContextLength;
    private String gpt4allModelUrl = GPT4ALL_MODEL_URL;
    private String janModelUrl = JAN_MODEL_URL;
    private String llamaCPPUrl = LLAMA_CPP_MODEL_URL;
    private String exoModelUrl = EXO_MODEL_URL;

    // Local custom OpenAI-compliant LLM fields
    private String customOpenAIUrl = "";
    private String customOpenAIModelName = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String customOpenAIApiKey = "";

    // Local LLM Providers
    private boolean isOllamaEnabled = true;
    private boolean isLmStudioEnabled = true;
    private boolean isGpt4AllEnabled = true;
    private boolean isJanEnabled = true;
    private boolean isLlamaCPPEnabled = true;
    private boolean isExoEnabled = false;

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

    // LLM API Keys — stored in IntelliJ PasswordSafe via {@link CredentialService}.
    // The fields below remain only as one-shot landing pads for legacy plaintext XML so
    // that the credential-migration routine in {@link #loadState} can read the value and
    // move it to PasswordSafe. After migration each field is wiped and stays empty.
    // Lombok's generated accessors are disabled; the hand-written ones below delegate to
    // {@link CredentialService}. Field-level @Transient is intentionally NOT used so that
    // XmlSerializer still populates the field on first deserialization of legacy XML;
    // the @Transient annotations are placed on the accessor methods so that subsequent
    // saves never re-emit the secret.
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String openAIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String mistralKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String anthropicKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String groqKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String deepInfraKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String geminiKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String deepSeekKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String openRouterKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String grokKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String kimiKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String glmKey = "";
    private String azureOpenAIEndpoint = "";
    private String azureOpenAIDeployment = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String azureOpenAIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String awsAccessKeyId = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String awsSecretKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String awsBearerToken = "";
    private String awsProfileName = "";
    private String awsRegion = "";
    private AwsBedrockAuthMode awsBedrockAuthMode;

    // Search API Keys
    private Boolean isWebSearchEnabled = ENABLE_WEB_SEARCH;

    private boolean tavilySearchEnabled = false;
    private boolean googleSearchEnabled = false;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String googleSearchKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String googleCSIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private String tavilySearchKey = "";
    private Integer maxSearchResults = MAX_SEARCH_RESULTS;

    /**
     * Set to {@code true} after the one-shot credential migration has moved every plaintext
     * key out of XML and into PasswordSafe. Persisted so the migration only ever runs once
     * per install. Public read access is via {@link #isCredentialsMigratedV1()}; we keep
     * Lombok's accessor for serialization purposes.
     */
    private boolean credentialsMigratedV1 = false;

    // Global fallback fields
    private static final String DEFAULT_PROVIDER = ModelProvider.Ollama.getName();
    private static final String DEFAULT_LANGUAGE_MODEL = "";

    private String lastGlobalProvider = DEFAULT_PROVIDER;
    private String lastGlobalLanguageModel = DEFAULT_LANGUAGE_MODEL;
    // Last selected language model
    private Map<String, String> lastSelectedProvider;
    private Map<String, String> lastSelectedLanguageModel;

    // Per-project open tab descriptors: projectHash -> list of tabId strings
    private Map<String, List<String>> openTabIds;

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

    // Show/hide Calc Tokens button in footer
    private Boolean showCalcTokensButton = false;

    // Show/hide Add File button in footer
    private Boolean showAddFileButton = true;
    
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
    private Boolean showToolActivityInChat = false;
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
    private String specExecutionMode = "SEQUENTIAL"; // "SEQUENTIAL" or "PARALLEL"
    private Integer specMaxConcurrency = 4;          // max parallel tasks per layer (1–8)

    // Security scanning settings
    private Boolean securityScanEnabled = false;
    private Boolean securityScanCreateSpecTasks = true;
    private String gitleaksPath = "";
    private String opengrepPath = "";
    private String trivyPath = "";
    private Boolean gitleaksScanToolEnabled = true;
    private Boolean opengrepScanToolEnabled = true;
    private Boolean trivyScanToolEnabled = true;

    // ACP tool runner settings (used by ACP Runners LLM provider in chat panel)
    private List<com.devoxx.genie.model.spec.AcpToolConfig> acpTools = new ArrayList<>();

    // Event Automation settings
    private EventAutomationSettings eventAutomationSettings = new EventAutomationSettings();
    private Boolean eventAutomationEnabled = false;

    // Anonymous usage analytics (LLM provider + model only)
    // See task-206. Endpoint is the GenieBuilder Cloudflare worker reused for DevoxxGenie via app_name segmentation.
    private Boolean analyticsEnabled = true;
    private Boolean analyticsNoticeShown = false;
    private Boolean analyticsNoticeAcknowledged = false;
    private String analyticsClientId = "";
    private String analyticsEndpoint = "https://delicate-morning-ff55.devoxx.workers.dev";

    /**
     * Returns the persisted anonymous client id, generating a new UUID on first access
     * and persisting it so it stays stable across IDE restarts and plugin updates.
     */
    public String getAnalyticsClientId() {
        if (analyticsClientId == null || analyticsClientId.isEmpty()) {
            analyticsClientId = UUID.randomUUID().toString();
        }
        return analyticsClientId;
    }

    // Inline completion settings
    private String inlineCompletionProvider = "";  // "", "Ollama", or "LMStudio"
    private String inlineCompletionModel = "";
    private Integer inlineCompletionMaxTokens = 64;
    private Integer inlineCompletionTimeoutMs = 5000;
    private Double inlineCompletionTemperature = 0.0;
    private Integer inlineCompletionDebounceMs = 300;


    // Model config cache
    private String modelConfigCachedJson = "";
    private long modelConfigLastFetchTimestamp = 0L;
    private String modelConfigPluginVersion = "";

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
        if (awsBedrockAuthMode == null) {
            awsBedrockAuthMode = Boolean.TRUE.equals(shouldPowerFromAWSProfile)
                    ? AwsBedrockAuthMode.PROFILE
                    : AwsBedrockAuthMode.defaultMode();
        }
        shouldPowerFromAWSProfile = getAwsBedrockAuthMode() == AwsBedrockAuthMode.PROFILE;
        initializeDefaultCostsIfEmpty();
        initializeUserPrompt();

        // Migrate plaintext credentials from XML into PasswordSafe (idempotent).
        // Runs synchronously so callers never observe a window where the State has
        // plaintext but PasswordSafe does not yet.
        if (!credentialsMigratedV1) {
            migrateCredentialsToPasswordSafe();
        }

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
        lastGlobalLanguageModel = selectedLanguageModel;
    }

    public String getSelectedLanguageModel(@NotNull String projectLocation) {
        if (lastSelectedLanguageModel != null && lastSelectedLanguageModel.containsKey(projectLocation)) {
            return lastSelectedLanguageModel.get(projectLocation);
        } else if (lastGlobalLanguageModel != null && !lastGlobalLanguageModel.isEmpty()) {
            return lastGlobalLanguageModel;
        } else {
            return DEFAULT_LANGUAGE_MODEL;
        }
    }

    public void setSelectedProvider(@NotNull String projectLocation, String selectedProvider) {
        if (lastSelectedProvider == null) {
            lastSelectedProvider = new HashMap<>();
        }
        lastSelectedProvider.put(projectLocation, selectedProvider);
        lastGlobalProvider = selectedProvider;
    }

    public String getSelectedProvider(@NotNull String projectLocation) {
        if (lastSelectedProvider != null && lastSelectedProvider.containsKey(projectLocation)) {
            return lastSelectedProvider.get(projectLocation);
        } else if  (lastGlobalProvider != null &&  !lastGlobalProvider.isEmpty()) {
            return lastGlobalProvider;
        } else {
            return DEFAULT_PROVIDER;
        }
    }

    /**
     * Save open tab IDs for a project (for restoring tabs across restarts).
     */
    public void setOpenTabIds(@NotNull String projectHash, @NotNull List<String> tabIds) {
        if (openTabIds == null) {
            openTabIds = new HashMap<>();
        }
        openTabIds.put(projectHash, new ArrayList<>(tabIds));
    }

    /**
     * Get saved open tab IDs for a project.
     */
    @NotNull
    public List<String> getOpenTabIds(@NotNull String projectHash) {
        if (openTabIds != null) {
            List<String> ids = openTabIds.get(projectHash);
            if (ids != null) {
                return new ArrayList<>(ids);
            }
        }
        return new ArrayList<>();
    }

    public boolean isAzureOpenAIEnabled() {
        return showAzureOpenAIFields &&
                !getAzureOpenAIKey().isEmpty() &&
                !azureOpenAIEndpoint.isEmpty() &&
                !azureOpenAIDeployment.isEmpty();
    }

    public boolean isAwsEnabled() {
        if (!showAwsFields || awsRegion.isEmpty()) {
            return false;
        }

        return switch (getAwsBedrockAuthMode()) {
            case ACCESS_KEY -> !getAwsAccessKeyId().isEmpty() && !getAwsSecretKey().isEmpty();
            case PROFILE -> !awsProfileName.isEmpty();
            case BEARER_TOKEN -> !getAwsBearerToken().isEmpty();
        };
    }

    @Override
    public @NotNull AwsBedrockAuthMode getAwsBedrockAuthMode() {
        if (awsBedrockAuthMode == null) {
            return Boolean.TRUE.equals(shouldPowerFromAWSProfile)
                    ? AwsBedrockAuthMode.PROFILE
                    : AwsBedrockAuthMode.defaultMode();
        }
        return awsBedrockAuthMode;
    }

    @Override
    public void setAwsBedrockAuthMode(AwsBedrockAuthMode authMode) {
        this.awsBedrockAuthMode = authMode != null ? authMode : AwsBedrockAuthMode.defaultMode();
        this.shouldPowerFromAWSProfile = this.awsBedrockAuthMode == AwsBedrockAuthMode.PROFILE;
    }

    public void setShouldPowerFromAWSProfile(Boolean shouldPowerFromAWSProfile) {
        this.shouldPowerFromAWSProfile = shouldPowerFromAWSProfile;
        if (Boolean.TRUE.equals(shouldPowerFromAWSProfile)) {
            this.awsBedrockAuthMode = AwsBedrockAuthMode.PROFILE;
        } else if (this.awsBedrockAuthMode == null || this.awsBedrockAuthMode == AwsBedrockAuthMode.PROFILE) {
            this.awsBedrockAuthMode = AwsBedrockAuthMode.ACCESS_KEY;
        }
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
            case "exoModelUrl" -> getExoModelUrl();
            default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Credential accessors — delegate to PasswordSafe via CredentialService.
    // All 19 accessors below replace the Lombok-generated ones that have been
    // disabled on the corresponding fields. They are marked @Transient so that
    // XmlSerializer skips them on save (so the secret never ends up in
    // DevoxxGenieSettingsPlugin.xml again).
    // ------------------------------------------------------------------

    /**
     * Lookup helper for the {@link CredentialService}. Returns a per-instance no-op
     * fallback when either the IntelliJ {@code Application} container is not yet
     * initialised or the {@code CredentialService} is not registered (unit tests that
     * instantiate {@code DevoxxGenieStateService} directly or via the platform test
     * framework without registering the service). The fallback is instance-scoped so
     * that test cases do not bleed credential state into one another.
     */
    private @NotNull CredentialService creds() {
        Application app = ApplicationManager.getApplication();
        if (app != null) {
            try {
                CredentialService service = app.getService(CredentialService.class);
                if (service != null) {
                    return service;
                }
            } catch (Throwable ignored) {
                // fall through to in-memory fallback
            }
        }
        return testFallbackCredentialService;
    }

    /**
     * In-memory {@link CredentialService} used only when the IntelliJ application
     * container is unavailable (unit tests). Per-instance so that a fresh
     * {@code new DevoxxGenieStateService()} in a test {@code @BeforeEach} starts
     * with a clean credential slate.
     */
    @Transient
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final CredentialService testFallbackCredentialService = new CredentialService() {
        private final Map<CredentialKey, String> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override public @NotNull String getCredential(@NotNull CredentialKey key) {
            return store.getOrDefault(key, "");
        }
        @Override public void setCredential(@NotNull CredentialKey key, @Nullable String value) {
            if (value == null || value.isEmpty()) store.remove(key);
            else store.put(key, value);
        }
        @Override public void removeCredential(@NotNull CredentialKey key) {
            store.remove(key);
        }
        @Override public boolean isAvailable() {
            return false;
        }
    };

    @Transient @Override public @NotNull String getOpenAIKey()        { return creds().getCredential(CredentialKey.OPEN_AI_KEY); }
    @Transient @Override public void          setOpenAIKey(String v)  { creds().setCredential(CredentialKey.OPEN_AI_KEY, v); }

    @Transient @Override public @NotNull String getMistralKey()       { return creds().getCredential(CredentialKey.MISTRAL_KEY); }
    @Transient @Override public void          setMistralKey(String v) { creds().setCredential(CredentialKey.MISTRAL_KEY, v); }

    @Transient @Override public @NotNull String getAnthropicKey()       { return creds().getCredential(CredentialKey.ANTHROPIC_KEY); }
    @Transient @Override public void          setAnthropicKey(String v) { creds().setCredential(CredentialKey.ANTHROPIC_KEY, v); }

    @Transient @Override public @NotNull String getGroqKey()       { return creds().getCredential(CredentialKey.GROQ_KEY); }
    @Transient @Override public void          setGroqKey(String v) { creds().setCredential(CredentialKey.GROQ_KEY, v); }

    @Transient @Override public @NotNull String getDeepInfraKey()       { return creds().getCredential(CredentialKey.DEEP_INFRA_KEY); }
    @Transient @Override public void          setDeepInfraKey(String v) { creds().setCredential(CredentialKey.DEEP_INFRA_KEY, v); }

    @Transient @Override public @NotNull String getGeminiKey()       { return creds().getCredential(CredentialKey.GEMINI_KEY); }
    @Transient @Override public void          setGeminiKey(String v) { creds().setCredential(CredentialKey.GEMINI_KEY, v); }

    @Transient @Override public @NotNull String getDeepSeekKey()       { return creds().getCredential(CredentialKey.DEEP_SEEK_KEY); }
    @Transient @Override public void          setDeepSeekKey(String v) { creds().setCredential(CredentialKey.DEEP_SEEK_KEY, v); }

    @Transient @Override public @NotNull String getOpenRouterKey()       { return creds().getCredential(CredentialKey.OPEN_ROUTER_KEY); }
    @Transient @Override public void          setOpenRouterKey(String v) { creds().setCredential(CredentialKey.OPEN_ROUTER_KEY, v); }

    @Transient @Override public @NotNull String getGrokKey()       { return creds().getCredential(CredentialKey.GROK_KEY); }
    @Transient @Override public void          setGrokKey(String v) { creds().setCredential(CredentialKey.GROK_KEY, v); }

    @Transient @Override public @NotNull String getKimiKey()       { return creds().getCredential(CredentialKey.KIMI_KEY); }
    @Transient @Override public void          setKimiKey(String v) { creds().setCredential(CredentialKey.KIMI_KEY, v); }

    @Transient @Override public @NotNull String getGlmKey()       { return creds().getCredential(CredentialKey.GLM_KEY); }
    @Transient @Override public void          setGlmKey(String v) { creds().setCredential(CredentialKey.GLM_KEY, v); }

    @Transient @Override public @NotNull String getAzureOpenAIKey()       { return creds().getCredential(CredentialKey.AZURE_OPEN_AI_KEY); }
    @Transient @Override public void          setAzureOpenAIKey(String v) { creds().setCredential(CredentialKey.AZURE_OPEN_AI_KEY, v); }

    @Transient @Override public @NotNull String getAwsAccessKeyId()       { return creds().getCredential(CredentialKey.AWS_ACCESS_KEY_ID); }
    @Transient @Override public void          setAwsAccessKeyId(String v) { creds().setCredential(CredentialKey.AWS_ACCESS_KEY_ID, v); }

    @Transient @Override public @NotNull String getAwsSecretKey()       { return creds().getCredential(CredentialKey.AWS_SECRET_KEY); }
    @Transient @Override public void          setAwsSecretKey(String v) { creds().setCredential(CredentialKey.AWS_SECRET_KEY, v); }

    @Transient @Override public @NotNull String getAwsBearerToken()       { return creds().getCredential(CredentialKey.AWS_BEARER_TOKEN); }
    @Transient @Override public void          setAwsBearerToken(String v) { creds().setCredential(CredentialKey.AWS_BEARER_TOKEN, v); }

    @Transient @Override public @NotNull String getCustomOpenAIApiKey()       { return creds().getCredential(CredentialKey.CUSTOM_OPEN_AI_KEY); }
    @Transient @Override public void          setCustomOpenAIApiKey(String v) { creds().setCredential(CredentialKey.CUSTOM_OPEN_AI_KEY, v); }

    @Transient @Override public @NotNull String getGoogleSearchKey()       { return creds().getCredential(CredentialKey.GOOGLE_SEARCH_KEY); }
    @Transient @Override public void          setGoogleSearchKey(String v) { creds().setCredential(CredentialKey.GOOGLE_SEARCH_KEY, v); }

    @Transient @Override public @NotNull String getGoogleCSIKey()       { return creds().getCredential(CredentialKey.GOOGLE_CSI_KEY); }
    @Transient @Override public void          setGoogleCSIKey(String v) { creds().setCredential(CredentialKey.GOOGLE_CSI_KEY, v); }

    @Transient @Override public @NotNull String getTavilySearchKey()       { return creds().getCredential(CredentialKey.TAVILY_SEARCH_KEY); }
    @Transient @Override public void          setTavilySearchKey(String v) { creds().setCredential(CredentialKey.TAVILY_SEARCH_KEY, v); }

    // ------------------------------------------------------------------
    // One-shot migration from plaintext XML fields into PasswordSafe.
    // ------------------------------------------------------------------

    /**
     * Walks every {@link CredentialKey}, reads the matching legacy field via reflection,
     * and — if non-blank — hands it to {@link CredentialService} (which writes it to
     * PasswordSafe) before wiping the legacy field. Sets {@link #credentialsMigratedV1}
     * on success and asks IntelliJ to flush state so the now-blank XML hits disk
     * immediately.
     * <p>
     * Non-fatal: any per-field failure is logged and skipped; the migration flag is only
     * raised when the entire pass completes. Safe to call multiple times — calls are
     * no-ops once the flag is set or when PasswordSafe is unavailable.
     */
    public void migrateCredentialsToPasswordSafe() {
        if (credentialsMigratedV1) {
            return;
        }
        CredentialService service;
        try {
            service = CredentialService.getInstance();
        } catch (Throwable t) {
            LOG.warn("CredentialService unavailable; deferring plaintext-credential migration", t);
            return;
        }
        if (!service.isAvailable()) {
            // PasswordSafe not reachable in this process — leave plaintext in place so the
            // next normal IDE startup can complete the migration.
            return;
        }

        int migrated = 0;
        int failed = 0;
        for (CredentialKey key : CredentialKey.values()) {
            try {
                Field field = DevoxxGenieStateService.class.getDeclaredField(key.getSubKey());
                field.setAccessible(true);
                Object raw = field.get(this);
                String legacy = (raw == null) ? "" : raw.toString();
                if (!legacy.isBlank()) {
                    String existing = service.getCredential(key);
                    if (existing == null || existing.isEmpty()) {
                        service.setCredential(key, legacy);
                        migrated++;
                    }
                    // Wipe plaintext copy whether or not we overwrote PasswordSafe.
                    field.set(this, "");
                }
            } catch (Throwable t) {
                failed++;
                LOG.warn("Failed to migrate credential field '" + key.getSubKey() + "' to PasswordSafe", t);
            }
        }
        credentialsMigratedV1 = true;
        try {
            ApplicationManager.getApplication().saveSettings();
        } catch (Throwable t) {
            LOG.warn("Failed to flush settings after credential migration", t);
        }
        LOG.info("Credential migration complete: migrated=" + migrated + " failed=" + failed);
    }

    private static final Logger LOG = Logger.getInstance(DevoxxGenieStateService.class);
}
