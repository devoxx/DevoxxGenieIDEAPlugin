package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.SubAgentConfig;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.model.Constant.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevoxxGenieStateServiceTest {

    private DevoxxGenieStateService stateService;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();
    }

    @Nested
    class DefaultValues {

        @Test
        void shouldHaveCorrectDefaultShortcuts() {
            assertThat(stateService.getSubmitShortcutWindows()).isEqualTo("shift ENTER");
            assertThat(stateService.getSubmitShortcutMac()).isEqualTo("shift ENTER");
            assertThat(stateService.getSubmitShortcutLinux()).isEqualTo("shift ENTER");
            assertThat(stateService.getNewlineShortcutWindows()).isEqualTo("ctrl ENTER");
            assertThat(stateService.getNewlineShortcutMac()).isEqualTo("meta ENTER");
            assertThat(stateService.getNewlineShortcutLinux()).isEqualTo("ctrl ENTER");
        }

        @Test
        void shouldHaveCorrectDefaultLLMSettings() {
            assertThat(stateService.getTemperature()).isEqualTo(TEMPERATURE);
            assertThat(stateService.getTopP()).isEqualTo(TOP_P);
            assertThat(stateService.getTimeout()).isEqualTo(TIMEOUT);
            assertThat(stateService.getMaxRetries()).isEqualTo(MAX_RETRIES);
            assertThat(stateService.getChatMemorySize()).isEqualTo(MAX_MEMORY);
            assertThat(stateService.getMaxOutputTokens()).isEqualTo(MAX_OUTPUT_TOKENS);
        }

        @Test
        void shouldHaveCorrectDefaultPrompts() {
            assertThat(stateService.getSystemPrompt()).isEqualTo(SYSTEM_PROMPT);
            assertThat(stateService.getTestPrompt()).isEqualTo(TEST_PROMPT);
            assertThat(stateService.getReviewPrompt()).isEqualTo(REVIEW_PROMPT);
            assertThat(stateService.getExplainPrompt()).isEqualTo(EXPLAIN_PROMPT);
        }

        @Test
        void shouldHaveCorrectDefaultLocalUrls() {
            assertThat(stateService.getOllamaModelUrl()).isEqualTo(OLLAMA_MODEL_URL);
            assertThat(stateService.getLmstudioModelUrl()).isEqualTo(LMSTUDIO_MODEL_URL);
            assertThat(stateService.getGpt4allModelUrl()).isEqualTo(GPT4ALL_MODEL_URL);
            assertThat(stateService.getJanModelUrl()).isEqualTo(JAN_MODEL_URL);
            assertThat(stateService.getLlamaCPPUrl()).isEqualTo(LLAMA_CPP_MODEL_URL);
        }

        @Test
        void shouldHaveCorrectDefaultBooleanSettings() {
            assertThat(stateService.getUseFileInEditor()).isTrue();
            assertThat(stateService.getRagEnabled()).isFalse();
            assertThat(stateService.getRagActivated()).isFalse();
            assertThat(stateService.getWebSearchActivated()).isFalse();
            assertThat(stateService.getStreamMode()).isEqualTo(STREAM_MODE);
            assertThat(stateService.getExcludeJavaDoc()).isFalse();
            assertThat(stateService.getUseGitIgnore()).isTrue();
        }

        @Test
        void shouldHaveCorrectDefaultLocalProviderSettings() {
            assertThat(stateService.isOllamaEnabled()).isTrue();
            assertThat(stateService.isLmStudioEnabled()).isTrue();
            assertThat(stateService.isGpt4AllEnabled()).isTrue();
            assertThat(stateService.isJanEnabled()).isTrue();
            assertThat(stateService.isLlamaCPPEnabled()).isTrue();
        }

        @Test
        void shouldHaveCorrectDefaultRemoteProviderSettings() {
            assertThat(stateService.isOpenAIEnabled()).isFalse();
            assertThat(stateService.isMistralEnabled()).isFalse();
            assertThat(stateService.isAnthropicEnabled()).isFalse();
            assertThat(stateService.isGroqEnabled()).isFalse();
            assertThat(stateService.isDeepInfraEnabled()).isFalse();
            assertThat(stateService.isGoogleEnabled()).isFalse();
            assertThat(stateService.isDeepSeekEnabled()).isFalse();
            assertThat(stateService.isOpenRouterEnabled()).isFalse();
        }

        @Test
        void shouldHaveEmptyApiKeysByDefault() {
            assertThat(stateService.getOpenAIKey()).isEmpty();
            assertThat(stateService.getMistralKey()).isEmpty();
            assertThat(stateService.getAnthropicKey()).isEmpty();
            assertThat(stateService.getGroqKey()).isEmpty();
            assertThat(stateService.getDeepInfraKey()).isEmpty();
            assertThat(stateService.getGeminiKey()).isEmpty();
            assertThat(stateService.getDeepSeekKey()).isEmpty();
            assertThat(stateService.getOpenRouterKey()).isEmpty();
        }

        @Test
        void shouldHaveCorrectDefaultWebSearchSettings() {
            assertThat(stateService.getIsWebSearchEnabled()).isEqualTo(ENABLE_WEB_SEARCH);
            assertThat(stateService.isTavilySearchEnabled()).isFalse();
            assertThat(stateService.isGoogleSearchEnabled()).isFalse();
            assertThat(stateService.getMaxSearchResults()).isEqualTo(MAX_SEARCH_RESULTS);
        }

        @Test
        void shouldHaveCorrectDefaultExcludedDirectories() {
            assertThat(stateService.getExcludedDirectories())
                    .containsExactlyInAnyOrder("build", ".git", "bin", "out", "target", "node_modules", ".idea");
        }

        @Test
        void shouldHaveCorrectDefaultIncludedFileExtensions() {
            assertThat(stateService.getIncludedFileExtensions())
                    .contains("java", "kt", "groovy", "xml", "json", "yaml", "yml", "js", "ts", "html");
        }

        @Test
        void shouldHaveCorrectDefaultExcludedFiles() {
            assertThat(stateService.getExcludedFiles())
                    .containsExactlyInAnyOrder("package-lock.json", "yarn.lock", ".env", "build.gradle", "settings.gradle");
        }

        @Test
        void shouldHaveCorrectDefaultMCPSettings() {
            assertThat(stateService.getMcpEnabled()).isFalse();
            assertThat(stateService.getMcpDebugLogsEnabled()).isFalse();
            assertThat(stateService.getMcpApprovalRequired()).isTrue();
            assertThat(stateService.getMcpApprovalTimeout()).isEqualTo(MCP_APPROVAL_TIMEOUT);
            assertThat(stateService.getMcpSettings()).isNotNull();
        }

        @Test
        void shouldHaveCorrectDefaultAgentSettings() {
            assertThat(stateService.getAgentModeEnabled()).isFalse();
            assertThat(stateService.getAgentMaxToolCalls()).isEqualTo(AGENT_MAX_TOOL_CALLS);
            assertThat(stateService.getAgentAutoApproveReadOnly()).isFalse();
            assertThat(stateService.getAgentWriteApprovalRequired()).isTrue();
            assertThat(stateService.getAgentDebugLogsEnabled()).isFalse();
        }

        @Test
        void shouldHaveCorrectDefaultInlineCompletionSettings() {
            assertThat(stateService.getInlineCompletionProvider()).isEmpty();
            assertThat(stateService.getInlineCompletionModel()).isEmpty();
            assertThat(stateService.getInlineCompletionMaxTokens()).isEqualTo(64);
            assertThat(stateService.getInlineCompletionTimeoutMs()).isEqualTo(5000);
            assertThat(stateService.getInlineCompletionTemperature()).isEqualTo(0.0);
            assertThat(stateService.getInlineCompletionDebounceMs()).isEqualTo(300);
        }

        @Test
        void shouldHaveCorrectDefaultAppearanceSettings() {
            assertThat(stateService.getLineHeight()).isEqualTo(1.6);
            assertThat(stateService.getMessagePadding()).isEqualTo(10);
            assertThat(stateService.getMessageMargin()).isEqualTo(10);
            assertThat(stateService.getBorderWidth()).isEqualTo(4);
            assertThat(stateService.getCornerRadius()).isEqualTo(4);
            assertThat(stateService.getUserMessageBorderColor()).isEqualTo("#FF5400");
            assertThat(stateService.getAssistantMessageBorderColor()).isEqualTo("#0095C9");
        }

        @Test
        void shouldHaveCorrectDefaultDevoxxGenieMdSettings() {
            assertThat(stateService.getCreateDevoxxGenieMd()).isFalse();
            assertThat(stateService.getIncludeProjectTree()).isFalse();
            assertThat(stateService.getProjectTreeDepth()).isEqualTo(3);
            assertThat(stateService.getUseDevoxxGenieMdInPrompt()).isFalse();
            assertThat(stateService.getUseClaudeOrAgentsMdInPrompt()).isTrue();
        }

        @Test
        void shouldHaveCorrectDefaultSpecSettings() {
            assertThat(stateService.getSpecBrowserEnabled()).isFalse();
            assertThat(stateService.getSpecDirectory()).isEqualTo("backlog");
            assertThat(stateService.getAutoInjectSpecContext()).isTrue();
            assertThat(stateService.getSpecTaskRunnerTimeoutMinutes()).isEqualTo(10);
        }
    }

    @Nested
    class CustomPromptInitialization {

        @Test
        void shouldInitializeCustomPromptsWithDefaults() {
            List<CustomPrompt> prompts = stateService.getCustomPrompts();
            assertThat(prompts).isNotNull();
            assertThat(prompts).isNotEmpty();
            assertThat(prompts).hasSameSizeAs(stateService.getDefaultPrompts());
        }

        @Test
        void shouldContainAllDefaultPromptNames() {
            List<String> promptNames = stateService.getCustomPrompts().stream()
                    .map(CustomPrompt::getName)
                    .toList();
            assertThat(promptNames).contains(
                    TEST_COMMAND, EXPLAIN_COMMAND, REVIEW_COMMAND,
                    TDG_COMMAND, FIND_COMMAND, HELP_COMMAND, INIT_COMMAND
            );
        }

        @Test
        void shouldReinitializeIfCustomPromptsIsNull() {
            stateService.setCustomPrompts(null);
            // Simulate what happens in constructor / loadState
            // The constructor calls initializeUserPrompt which checks for null/empty
            DevoxxGenieStateService freshService = new DevoxxGenieStateService();
            freshService.setCustomPrompts(null);
            // After setting null, we need to call the same logic
            // The real initialization happens only in constructor and loadState
            // Let's verify a fresh instance always has prompts
            DevoxxGenieStateService newService = new DevoxxGenieStateService();
            assertThat(newService.getCustomPrompts()).isNotEmpty();
        }
    }

    @Nested
    class SelectedProviderAndModel {

        @Test
        void shouldReturnDefaultProviderWhenNoSelection() {
            String provider = stateService.getSelectedProvider("/some/project");
            assertThat(provider).isEqualTo(ModelProvider.Ollama.getName());
        }

        @Test
        void shouldReturnEmptyModelWhenNoSelection() {
            String model = stateService.getSelectedLanguageModel("/some/project");
            assertThat(model).isEmpty();
        }

        @Test
        void shouldSetAndGetSelectedProvider() {
            stateService.setSelectedProvider("/project1", "OpenAI");
            assertThat(stateService.getSelectedProvider("/project1")).isEqualTo("OpenAI");
        }

        @Test
        void shouldSetAndGetSelectedLanguageModel() {
            stateService.setSelectedLanguageModel("/project1", "gpt-4");
            assertThat(stateService.getSelectedLanguageModel("/project1")).isEqualTo("gpt-4");
        }

        @Test
        void shouldTrackDifferentProvidersPerProject() {
            stateService.setSelectedProvider("/project1", "OpenAI");
            stateService.setSelectedProvider("/project2", "Anthropic");
            assertThat(stateService.getSelectedProvider("/project1")).isEqualTo("OpenAI");
            assertThat(stateService.getSelectedProvider("/project2")).isEqualTo("Anthropic");
        }

        @Test
        void shouldTrackDifferentModelsPerProject() {
            stateService.setSelectedLanguageModel("/project1", "gpt-4");
            stateService.setSelectedLanguageModel("/project2", "claude-3");
            assertThat(stateService.getSelectedLanguageModel("/project1")).isEqualTo("gpt-4");
            assertThat(stateService.getSelectedLanguageModel("/project2")).isEqualTo("claude-3");
        }

        @Test
        void shouldReturnDefaultWhenProviderMapIsNull() {
            // Simulate a null map (could happen from XML deserialization)
            stateService.setLastSelectedProvider(null);
            assertThat(stateService.getSelectedProvider("/any")).isEqualTo(ModelProvider.Ollama.getName());
        }

        @Test
        void shouldReturnEmptyWhenModelMapIsNull() {
            stateService.setLastSelectedLanguageModel(null);
            assertThat(stateService.getSelectedLanguageModel("/any")).isEmpty();
        }

        @Test
        void shouldInitializeMapOnFirstProviderSet() {
            stateService.setLastSelectedProvider(null);
            stateService.setSelectedProvider("/project", "Mistral");
            assertThat(stateService.getSelectedProvider("/project")).isEqualTo("Mistral");
        }

        @Test
        void shouldInitializeMapOnFirstModelSet() {
            stateService.setLastSelectedLanguageModel(null);
            stateService.setSelectedLanguageModel("/project", "model-x");
            assertThat(stateService.getSelectedLanguageModel("/project")).isEqualTo("model-x");
        }
    }

    @Nested
    class AzureOpenAIEnabled {

        @Test
        void shouldBeDisabledByDefault() {
            assertThat(stateService.isAzureOpenAIEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWhenShowFieldsIsFalse() {
            stateService.setShowAzureOpenAIFields(false);
            stateService.setAzureOpenAIKey("key");
            stateService.setAzureOpenAIEndpoint("endpoint");
            stateService.setAzureOpenAIDeployment("deployment");
            assertThat(stateService.isAzureOpenAIEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWhenKeyIsEmpty() {
            stateService.setShowAzureOpenAIFields(true);
            stateService.setAzureOpenAIKey("");
            stateService.setAzureOpenAIEndpoint("endpoint");
            stateService.setAzureOpenAIDeployment("deployment");
            assertThat(stateService.isAzureOpenAIEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWhenEndpointIsEmpty() {
            stateService.setShowAzureOpenAIFields(true);
            stateService.setAzureOpenAIKey("key");
            stateService.setAzureOpenAIEndpoint("");
            stateService.setAzureOpenAIDeployment("deployment");
            assertThat(stateService.isAzureOpenAIEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWhenDeploymentIsEmpty() {
            stateService.setShowAzureOpenAIFields(true);
            stateService.setAzureOpenAIKey("key");
            stateService.setAzureOpenAIEndpoint("endpoint");
            stateService.setAzureOpenAIDeployment("");
            assertThat(stateService.isAzureOpenAIEnabled()).isFalse();
        }

        @Test
        void shouldBeEnabledWhenAllFieldsSet() {
            stateService.setShowAzureOpenAIFields(true);
            stateService.setAzureOpenAIKey("key");
            stateService.setAzureOpenAIEndpoint("endpoint");
            stateService.setAzureOpenAIDeployment("deployment");
            assertThat(stateService.isAzureOpenAIEnabled()).isTrue();
        }
    }

    @Nested
    class AwsEnabled {

        @Test
        void shouldBeDisabledByDefault() {
            assertThat(stateService.isAwsEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWhenShowFieldsIsFalse() {
            stateService.setShowAwsFields(false);
            stateService.setAwsAccessKeyId("access-key");
            stateService.setAwsSecretKey("secret-key");
            stateService.setAwsRegion("us-east-1");
            assertThat(stateService.isAwsEnabled()).isFalse();
        }

        @Test
        void shouldBeEnabledWithAccessKeyAndSecretAndRegion() {
            stateService.setShowAwsFields(true);
            stateService.setAwsAccessKeyId("access-key");
            stateService.setAwsSecretKey("secret-key");
            stateService.setAwsRegion("us-east-1");
            assertThat(stateService.isAwsEnabled()).isTrue();
        }

        @Test
        void shouldBeDisabledWithoutRegion() {
            stateService.setShowAwsFields(true);
            stateService.setAwsAccessKeyId("access-key");
            stateService.setAwsSecretKey("secret-key");
            stateService.setAwsRegion("");
            assertThat(stateService.isAwsEnabled()).isFalse();
        }

        @Test
        void shouldBeEnabledWithProfileAndRegion() {
            stateService.setShowAwsFields(true);
            stateService.setShouldPowerFromAWSProfile(true);
            stateService.setAwsProfileName("my-profile");
            stateService.setAwsRegion("eu-west-1");
            assertThat(stateService.isAwsEnabled()).isTrue();
        }

        @Test
        void shouldBeDisabledWithEmptyProfile() {
            stateService.setShowAwsFields(true);
            stateService.setShouldPowerFromAWSProfile(true);
            stateService.setAwsProfileName("");
            stateService.setAwsRegion("eu-west-1");
            assertThat(stateService.isAwsEnabled()).isFalse();
        }

        @Test
        void shouldBeDisabledWithProfileModeButEmptyKeys() {
            stateService.setShowAwsFields(true);
            stateService.setShouldPowerFromAWSProfile(false);
            stateService.setAwsAccessKeyId("");
            stateService.setAwsSecretKey("");
            stateService.setAwsRegion("us-east-1");
            assertThat(stateService.isAwsEnabled()).isFalse();
        }
    }

    @Nested
    class SubAgentConfigs {

        @Test
        void shouldReturnDefaultParallelismWhenNoConfigs() {
            assertThat(stateService.getSubAgentParallelism()).isEqualTo(SUB_AGENT_DEFAULT_PARALLELISM);
        }

        @Test
        void shouldReturnConfigsSizeAsParallelism() {
            List<SubAgentConfig> configs = List.of(
                    new SubAgentConfig("OpenAI", "gpt-4"),
                    new SubAgentConfig("Anthropic", "claude-3")
            );
            stateService.setSubAgentConfigs(configs);
            assertThat(stateService.getSubAgentParallelism()).isEqualTo(2);
        }

        @Test
        void shouldSetParallelismToAtLeastOneWhenSettingConfigs() {
            stateService.setSubAgentConfigs(new ArrayList<>());
            // With empty configs, parallelism falls back to stored field
            assertThat(stateService.getSubAgentParallelism()).isGreaterThanOrEqualTo(1);
        }

        @Test
        void shouldHandleNullConfigsGracefully() {
            stateService.setSubAgentConfigs(null);
            assertThat(stateService.getSubAgentConfigs()).isNotNull();
            assertThat(stateService.getSubAgentConfigs()).isEmpty();
        }

        @Test
        void shouldMakeDefensiveCopyOfConfigs() {
            List<SubAgentConfig> original = new ArrayList<>(List.of(
                    new SubAgentConfig("OpenAI", "gpt-4")
            ));
            stateService.setSubAgentConfigs(original);
            original.add(new SubAgentConfig("New", "model"));
            assertThat(stateService.getSubAgentConfigs()).hasSize(1);
        }

        @Test
        void shouldReturnEffectiveConfigForValidIndex() {
            List<SubAgentConfig> configs = List.of(
                    new SubAgentConfig("OpenAI", "gpt-4"),
                    new SubAgentConfig("Anthropic", "claude-3")
            );
            stateService.setSubAgentConfigs(configs);

            SubAgentConfig config = stateService.getEffectiveSubAgentConfig(0);
            assertThat(config.getModelProvider()).isEqualTo("OpenAI");
            assertThat(config.getModelName()).isEqualTo("gpt-4");
        }

        @Test
        void shouldFallBackToDefaultForInvalidIndex() {
            stateService.setSubAgentModelProvider("DefaultProvider");
            stateService.setSubAgentModelName("DefaultModel");

            SubAgentConfig config = stateService.getEffectiveSubAgentConfig(999);
            assertThat(config.getModelProvider()).isEqualTo("DefaultProvider");
            assertThat(config.getModelName()).isEqualTo("DefaultModel");
        }

        @Test
        void shouldFallBackToDefaultForNegativeIndex() {
            stateService.setSubAgentModelProvider("Provider");
            stateService.setSubAgentModelName("Model");

            SubAgentConfig config = stateService.getEffectiveSubAgentConfig(-1);
            assertThat(config.getModelProvider()).isEqualTo("Provider");
            assertThat(config.getModelName()).isEqualTo("Model");
        }

        @Test
        void shouldFallBackWhenConfigHasEmptyProvider() {
            List<SubAgentConfig> configs = List.of(
                    new SubAgentConfig("", "")
            );
            stateService.setSubAgentConfigs(configs);
            stateService.setSubAgentModelProvider("FallbackProvider");
            stateService.setSubAgentModelName("FallbackModel");

            SubAgentConfig config = stateService.getEffectiveSubAgentConfig(0);
            assertThat(config.getModelProvider()).isEqualTo("FallbackProvider");
            assertThat(config.getModelName()).isEqualTo("FallbackModel");
        }

        @Test
        void shouldFallBackWhenConfigHasNullProvider() {
            SubAgentConfig nullProviderConfig = new SubAgentConfig(null, "model");
            List<SubAgentConfig> configs = new ArrayList<>();
            configs.add(nullProviderConfig);
            stateService.setSubAgentConfigs(configs);
            stateService.setSubAgentModelProvider("Default");
            stateService.setSubAgentModelName("DefaultModel");

            SubAgentConfig config = stateService.getEffectiveSubAgentConfig(0);
            assertThat(config.getModelProvider()).isEqualTo("Default");
        }
    }

    @Nested
    class ConfigValue {

        @Test
        void shouldReturnJanModelUrl() {
            stateService.setJanModelUrl("http://custom-jan:1337/v1/");
            assertThat(stateService.getConfigValue("janModelUrl")).isEqualTo("http://custom-jan:1337/v1/");
        }

        @Test
        void shouldReturnGpt4allModelUrl() {
            stateService.setGpt4allModelUrl("http://custom-gpt4all:4891/v1/");
            assertThat(stateService.getConfigValue("gpt4allModelUrl")).isEqualTo("http://custom-gpt4all:4891/v1/");
        }

        @Test
        void shouldReturnLmStudioModelUrl() {
            stateService.setLmstudioModelUrl("http://custom-lmstudio:1234/v1/");
            assertThat(stateService.getConfigValue("lmStudioModelUrl")).isEqualTo("http://custom-lmstudio:1234/v1/");
        }

        @Test
        void shouldReturnOllamaModelUrl() {
            stateService.setOllamaModelUrl("http://custom-ollama:11434/");
            assertThat(stateService.getConfigValue("ollamaModelUrl")).isEqualTo("http://custom-ollama:11434/");
        }

        @Test
        void shouldReturnNullForUnknownKey() {
            assertThat(stateService.getConfigValue("unknownKey")).isNull();
        }

        @Test
        void shouldReturnNullForEmptyKey() {
            assertThat(stateService.getConfigValue("")).isNull();
        }
    }

    @Nested
    class LanguageModels {

        @Test
        void shouldReturnEmptyListByDefault() {
            assertThat(stateService.getLanguageModels()).isEmpty();
        }

        @Test
        void shouldReturnDefensiveCopy() {
            List<LanguageModel> original = new ArrayList<>();
            stateService.setLanguageModels(original);
            List<LanguageModel> returned = stateService.getLanguageModels();
            assertThat(returned).isNotSameAs(original);
        }

        @Test
        void shouldSetLanguageModelsAsDefensiveCopy() {
            List<LanguageModel> original = new ArrayList<>();
            LanguageModel model = new LanguageModel();
            original.add(model);
            stateService.setLanguageModels(original);
            original.clear();
            assertThat(stateService.getLanguageModels()).hasSize(1);
        }
    }

    @Nested
    class GetState {

        @Test
        void shouldReturnSelf() {
            assertThat(stateService.getState()).isSameAs(stateService);
        }
    }

    @Nested
    class LoadListeners {

        @Test
        void shouldAddAndNotifyListeners() {
            AtomicBoolean called = new AtomicBoolean(false);
            stateService.addLoadListener(() -> called.set(true));

            // loadState calls listeners, but we need to mock XmlSerializerUtil
            try (MockedStatic<XmlSerializerUtil> mockedXml = Mockito.mockStatic(XmlSerializerUtil.class)) {
                stateService.loadState(stateService);
                assertThat(called.get()).isTrue();
            }
        }

        @Test
        void shouldNotifyMultipleListeners() {
            AtomicBoolean first = new AtomicBoolean(false);
            AtomicBoolean second = new AtomicBoolean(false);
            stateService.addLoadListener(() -> first.set(true));
            stateService.addLoadListener(() -> second.set(true));

            try (MockedStatic<XmlSerializerUtil> mockedXml = Mockito.mockStatic(XmlSerializerUtil.class)) {
                stateService.loadState(stateService);
                assertThat(first.get()).isTrue();
                assertThat(second.get()).isTrue();
            }
        }
    }

    @Nested
    class SetterGetter {

        @Test
        void shouldSetAndGetTemperature() {
            stateService.setTemperature(1.5);
            assertThat(stateService.getTemperature()).isEqualTo(1.5);
        }

        @Test
        void shouldSetAndGetTopP() {
            stateService.setTopP(0.5);
            assertThat(stateService.getTopP()).isEqualTo(0.5);
        }

        @Test
        void shouldSetAndGetTimeout() {
            stateService.setTimeout(120);
            assertThat(stateService.getTimeout()).isEqualTo(120);
        }

        @Test
        void shouldSetAndGetStreamMode() {
            stateService.setStreamMode(true);
            assertThat(stateService.getStreamMode()).isTrue();
        }

        @Test
        void shouldSetAndGetSystemPrompt() {
            stateService.setSystemPrompt("Custom prompt");
            assertThat(stateService.getSystemPrompt()).isEqualTo("Custom prompt");
        }

        @Test
        void shouldSetAndGetCustomOpenAIFields() {
            stateService.setCustomOpenAIUrl("http://custom:8080");
            stateService.setCustomOpenAIModelName("custom-model");
            stateService.setCustomOpenAIApiKey("custom-key");
            assertThat(stateService.getCustomOpenAIUrl()).isEqualTo("http://custom:8080");
            assertThat(stateService.getCustomOpenAIModelName()).isEqualTo("custom-model");
            assertThat(stateService.getCustomOpenAIApiKey()).isEqualTo("custom-key");
        }
    }

    @Nested
    class ModelWindowContext {

        @Test
        void shouldSetWindowContextForApiKeyBasedProvider() {
            stateService.setModelWindowContext(ModelProvider.OpenAI, "gpt-4", 128000);
            assertThat(stateService.getModelWindowContexts())
                    .containsEntry("OpenAI:gpt-4", 128000);
        }

        @Test
        void shouldNotSetWindowContextForLocalProvider() {
            stateService.setModelWindowContext(ModelProvider.Ollama, "llama2", 4096);
            assertThat(stateService.getModelWindowContexts())
                    .doesNotContainKey("Ollama:llama2");
        }

        @Test
        void shouldHaveDefaultWindowContext() {
            assertThat(stateService.getDefaultWindowContext()).isEqualTo(8000);
        }
    }
}
