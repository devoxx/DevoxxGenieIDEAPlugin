package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.Command;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.AgentDefinition;
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
import com.intellij.util.xmlb.annotations.OptionTag;
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

    private List<Command> commands = new ArrayList<>();

    /**
     * Legacy field kept for XML state migration from pre-#1040 settings
     * (when commands were called "custom prompts"). On {@link #loadState} the value
     * is migrated into {@link #commands} and then cleared so it is not re-serialized.
     *
     * <p>Do not reference from new code — use {@link #getCommands()} / {@link #setCommands}.
     * Lombok-generated accessors are intentional: IntelliJ's {@code XmlSerializer} requires
     * a matching getter/setter pair to recognise the {@code customPrompts} option element
     * in legacy XML. The field defaults to {@code null} and is set back to {@code null} once
     * its contents have been migrated, so serialization (which skips null bean properties)
     * will not re-emit the legacy element.</p>
     */
    @Deprecated
    private List<Command> customPrompts;

    /**
     * Skill names that the user has explicitly disabled in the Skills settings panel.
     * Skills not in this set are considered active.
     */
    private Set<String> disabledSkillNames = new HashSet<>();

    // Commands that were once defaults but have been intentionally removed.
    // Entries here are pruned from any saved commands list on loadState so that
    // existing users don't continue to see them on the welcome page.
    private static final Set<String> REMOVED_DEFAULT_COMMANDS = Set.of("tdg");

    private final List<Command> defaultPrompts = Arrays.asList(
            new Command(TEST_COMMAND, TEST_PROMPT),
            new Command(EXPLAIN_COMMAND, EXPLAIN_PROMPT),
            new Command(REVIEW_COMMAND, REVIEW_PROMPT),
            new Command(FIND_COMMAND, FIND_PROMPT),
            new Command(SEARCH_COMMAND, SEARCH_PROMPT),
            new Command(HELP_COMMAND, HELP_PROMPT),
            new Command(INIT_COMMAND, INIT_PROMPT)
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
    // Null means "use the default context window"; a positive value overrides it for token calc / usage bar.
    private Integer customOpenAIContextWindow;
    // Cost in dollars per 1,000,000 tokens; null/0 means "no cost" (cost figure hidden in the bubble).
    private Double customOpenAIInputCost;
    private Double customOpenAIOutputCost;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("customOpenAIApiKey")
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
    private boolean isNvidiaEnabled = false;

    // ============================================================================
    // LEGACY-XML CREDENTIAL LANDING PADS
    // ----------------------------------------------------------------------------
    // The 19 fields below remain ONLY to receive plaintext credentials from
    // pre-PasswordSafe versions of DevoxxGenieSettingsPlugin.xml during the first
    // {@link #loadState} call after an upgrade. The migration routine in
    // {@link #migrateCredentialsToPasswordSafe} reads each field via reflection,
    // hands the value to {@link CredentialService} (which writes it to IntelliJ's
    // PasswordSafe), and then wipes the field. After migration the fields stay
    // empty for the lifetime of the install.
    //
    // Why @OptionTag + @Transient (on the hand-written accessors) instead of the
    // "obvious" approach of leaving fields plain or relying on Lombok-generated
    // accessors:
    //
    // IntelliJ's BeanBinding / XmlSerializer always prefers an accessor pair over
    // a same-named field when both exist (it deduplicates via accessor name). The
    // hand-written getters/setters intentionally read from / write to
    // PasswordSafe, so if BeanBinding picked up that pair as the property binding,
    // {@code getState()} would call {@code getOpenAIKey()} on save, read the live
    // secret from PasswordSafe, and serialise it right back into the XML — the
    // very leak the migration is meant to close.
    //
    // The fix has two parts, applied to every credential field:
    //   1. The hand-written accessors carry @com.intellij.util.xmlb.annotations.Transient
    //      so BeanBinding rejects them in PropertyCollector#isAcceptableProperty
    //      and removes them from the property map entirely.
    //   2. The fields carry @OptionTag(...) (a "store annotation") which forces
    //      PropertyCollector#doCollectOwnFields to register the *field* even though
    //      it is private. With the accessor pair gone from the map, the field
    //      escapes the dedup loop and becomes the sole binding for that property
    //      name. XmlSerializer then reads & writes the field directly — never
    //      touching PasswordSafe. Once the field has been wiped post-migration its
    //      empty value matches the default-bean's empty value and the
    //      SkipDefaultsSerializationFilter omits it from the XML.
    //
    // Lombok @Getter(NONE)/@Setter(NONE) keeps Lombok from auto-generating
    // accessors that would shadow ours.
    // ============================================================================
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("openAIKey")
    private String openAIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("mistralKey")
    private String mistralKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("anthropicKey")
    private String anthropicKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("groqKey")
    private String groqKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("deepInfraKey")
    private String deepInfraKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("geminiKey")
    private String geminiKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("deepSeekKey")
    private String deepSeekKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("openRouterKey")
    private String openRouterKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("grokKey")
    private String grokKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("kimiKey")
    private String kimiKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("glmKey")
    private String glmKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("nvidiaKey")
    private String nvidiaKey = "";
    private String azureOpenAIEndpoint = "";
    private String azureOpenAIDeployment = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("azureOpenAIKey")
    private String azureOpenAIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("awsAccessKeyId")
    private String awsAccessKeyId = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("awsSecretKey")
    private String awsSecretKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("awsBearerToken")
    private String awsBearerToken = "";
    private String awsProfileName = "";
    private String awsRegion = "";
    private AwsBedrockAuthMode awsBedrockAuthMode;

    // Search API Keys
    private Boolean isWebSearchEnabled = ENABLE_WEB_SEARCH;

    private boolean tavilySearchEnabled = false;
    private boolean googleSearchEnabled = false;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("googleSearchKey")
    private String googleSearchKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("googleCSIKey")
    private String googleCSIKey = "";
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("tavilySearchKey")
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

    // Show thinking/reasoning output of reasoning models (Ollama, OpenAI-compatible providers, Mistral)
    private Boolean showThinkingEnabled = false;

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
    /** Wall-clock cap for an entire agent/MCP conversation; safety net against silent hangs. */
    private Integer agentMaxExecutionTimeSeconds = AGENT_MAX_EXECUTION_SECONDS;

    // RAG retrieval-quality settings
    /**
     * When true, the user's prompt is paraphrased into multiple variants by the current chat
     * model before semantic search; per-variant results are fused via Reciprocal Rank Fusion.
     * Trades one extra LLM call (latency + cost) for substantially better retrieval on
     * meta-style queries ("where do we discuss X?", "list me all content about Y").
     */
    private Boolean ragQueryExpansionEnabled = false;
    /** Number of paraphrased variants to generate per query when expansion is enabled. */
    private Integer ragQueryExpansionN = 3;

    /**
     * Directories the RAG indexer should skip, in addition to the global "Scan & Copy Project"
     * exclusion list ({@link #excludedDirectories}). Matched by directory name anywhere in a
     * file's path. Default is empty so existing behavior is unchanged — users opt in to extra
     * exclusions via Settings → RAG (task-220).
     */
    private List<String> ragExcludedDirectories = new ArrayList<>();
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

    // Agent Team (multi-agent orchestration) settings — see docs/specs/agent-team-orchestration.md
    private Boolean agentTeamEnabled = false;
    /** When true, the orchestrating conversation loses direct write/run tools and must delegate. */
    private Boolean agentTeamPureCoordinator = true;
    /**
     * @deprecated superseded by the per-agent execution target
     * ({@link com.devoxx.genie.model.agent.AgentDefinition#getExecutionTarget()}, TASK-250).
     * Kept only so pre-existing XML state still deserializes; no longer read anywhere.
     */
    @Deprecated
    private Boolean agentTeamRemoteEnabled = false;
    /** Base URL of the DockerAgents orchestrator-api (compose publishes it on :8090). */
    private String agentTeamRemoteUrl = "http://localhost:8090";
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<AgentDefinition> agentDefinitions = new ArrayList<>();

    // Test execution settings
    private Boolean testExecutionEnabled = true;
    private Integer testExecutionTimeoutSeconds = TEST_EXECUTION_DEFAULT_TIMEOUT;
    private String testExecutionCustomCommand = "";

    // Command execution environment settings (issue #1027)
    private String agentShellEnvFile = "";
    private String agentShell = "";

    // Spec Driven Development settings
    private Boolean specBrowserEnabled = false;
    private String specDirectory = "backlog";
    private Boolean autoInjectSpecContext = true;
    private Integer specTaskRunnerTimeoutMinutes = 10;

    // Docker Agentic Platform (ap) CLI integration
    /** When false, the Agentic Platform tab is hidden from the DevoxxGenie tool window. */
    private Boolean apIntegrationEnabled = false;
    private String apCliPath = "";
    /**
     * Stored as {@link com.devoxx.genie.model.ap.ApAuthMode} name.
     * Default {@code CACHED_LOGIN} sets no env vars and lets the binary use whichever
     * credentials it finds on its own (TUI-cached tokens, Docker Desktop, …).
     */
    private String apAuthMode = "CACHED_LOGIN";
    private String apAccessToken = "";
    private String apRefreshToken = "";

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

    // Web search agent tool
    private Boolean webSearchAgentToolEnabled = false;

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

    // Tips cache
    private String tipsCachedJson = "";
    private long tipsLastFetchTimestamp = 0L;

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
        if (commands == null || commands.isEmpty()) {
            commands = new ArrayList<>(defaultPrompts);
        } else {
            // Prune commands that have been intentionally removed from the defaults.
            commands.removeIf(cmd -> REMOVED_DEFAULT_COMMANDS.contains(cmd.getName()));

            // Merge any default commands that are missing from the saved list so that
            // newly-added built-in commands (e.g. /search) appear for existing users.
            Set<String> existingNames = commands.stream()
                    .map(Command::getName)
                    .collect(java.util.stream.Collectors.toSet());
            for (Command defaultCmd : defaultPrompts) {
                if (!existingNames.contains(defaultCmd.getName())) {
                    commands.add(defaultCmd);
                }
            }
        }
    }

    /**
     * Migrates legacy {@code customPrompts} XML state (pre-#1040) into the renamed
     * {@link #commands} field. Idempotent and safe to call when there is nothing to migrate.
     *
     * <p>Lombok-generated accessors on the legacy field let IntelliJ's {@code XmlSerializer}
     * populate it during {@code loadState}; {@link XmlSerializerUtil#copyBean} then copies
     * it onto {@code this}. We migrate from {@code this.customPrompts} and clear both the
     * deserialized {@code state} copy and {@code this} so the legacy element is not
     * re-emitted on the next save.</p>
     */
    @SuppressWarnings("deprecation")
    private void migrateLegacyCustomPrompts(@NotNull DevoxxGenieStateService state) {
        // After copyBean(state, this), this.customPrompts and this.commands have the same
        // values as the deserialized state. The presence of legacy data in customPrompts is
        // the unambiguous signal that we are upgrading from pre-#1040 — the new commands
        // field could not have been set in that case, even though the constructor would have
        // pre-populated it with defaults.
        List<Command> legacy = this.customPrompts != null ? this.customPrompts : state.customPrompts;
        if (legacy != null && !legacy.isEmpty()) {
            // Replace whatever's in commands (it can only be the constructor-set defaults at
            // this point because pre-#1040 XML never contained a <option name="commands">
            // element) with the legacy values.
            commands = new ArrayList<>(legacy);
            // Clear so the legacy field does not get re-serialized.
            this.customPrompts = null;
            state.customPrompts = null;
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
        // Migrate legacy customPrompts -> commands (issue #1040) before populating defaults.
        migrateLegacyCustomPrompts(state);
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

    public List<AgentDefinition> getAgentDefinitions() {
        return agentDefinitions;
    }

    public void setAgentDefinitions(List<AgentDefinition> definitions) {
        this.agentDefinitions = definitions != null ? new ArrayList<>(definitions) : new ArrayList<>();
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
    // disabled on the corresponding fields. They carry
    // @com.intellij.util.xmlb.annotations.Transient so that BeanBinding rejects
    // them entirely. XmlSerializer therefore reads/writes only the underlying
    // private fields (which are forced into the property map via @OptionTag).
    // See the "LEGACY-XML CREDENTIAL LANDING PADS" comment block above for the
    // full rationale.
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

    @Transient @Override public @NotNull String getNvidiaKey()       { return creds().getCredential(CredentialKey.NVIDIA_KEY); }
    @Transient @Override public void          setNvidiaKey(String v) { creds().setCredential(CredentialKey.NVIDIA_KEY, v); }

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
     * <em>only when every key migrated cleanly</em>; on partial failure the flag stays
     * {@code false} and the remaining plaintext fields are left in place so the next
     * IDE startup can retry the migration.
     * <p>
     * The method is {@code synchronized} to be safe against concurrent project-open
     * calls (the {@link com.devoxx.genie.startup.CredentialMigrationStartupActivity}
     * fires once per project) and against the in-flight {@link #loadState} that
     * triggered the first migration attempt. Safe to call multiple times — every call
     * is a no-op once the flag is set or when PasswordSafe is unavailable.
     */
    public synchronized void migrateCredentialsToPasswordSafe() {
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
        if (service == null || !service.isAvailable()) {
            // CredentialService not registered (getService may return null on some IntelliJ
            // versions / test environments) or PasswordSafe not reachable in this process —
            // leave plaintext in place so the next normal IDE startup can complete the migration.
            return;
        }

        int migrated = 0;
        int skipped = 0;
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
                    } else {
                        // Already present in PasswordSafe (e.g. a retry after partial failure).
                        skipped++;
                    }
                    // Wipe plaintext copy whether or not we overwrote PasswordSafe.
                    field.set(this, "");
                }
            } catch (Throwable t) {
                failed++;
                LOG.warn("Failed to migrate credential field '" + key.getSubKey() + "' to PasswordSafe", t);
            }
        }
        if (failed == 0) {
            // All keys handled; mark migrated and ask IntelliJ to flush the now-sanitised XML.
            credentialsMigratedV1 = true;
            try {
                ApplicationManager.getApplication().saveSettings();
            } catch (Throwable t) {
                LOG.warn("Failed to flush settings after credential migration", t);
            }
            LOG.info("Credential migration complete: migrated=" + migrated
                    + " skipped=" + skipped);
        } else {
            // Leave the flag false so the next startup retries. Do NOT call saveSettings():
            // the un-wiped plaintext fields still hold the values we need on retry, and
            // saving now would persist the partial state — fine for security but wasteful.
            LOG.warn("Credential migration partially failed: migrated=" + migrated
                    + " skipped=" + skipped + " failed=" + failed + "; will retry on next startup");
        }
    }

    private static final Logger LOG = Logger.getInstance(DevoxxGenieStateService.class);
}
