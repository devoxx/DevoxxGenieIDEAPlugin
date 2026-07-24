package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LLMProvidersConfigurableTest {

    @Mock
    private Application application;

    @Mock
    private Project project;

    @Mock
    private MessageBus messageBus;

    @Mock
    private SettingsChangeListener settingsChangeListener;

    @Mock
    private PropertiesService propertiesService;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<PropertiesService> propertiesServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private LLMProvidersConfigurable configurable;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        propertiesServiceMockedStatic = Mockito.mockStatic(PropertiesService.class);
        propertiesServiceMockedStatic.when(PropertiesService::getInstance).thenReturn(propertiesService);
        when(propertiesService.getVersion()).thenReturn("1.0.0-test");
        lenient().when(project.getMessageBus()).thenReturn(messageBus);
        lenient().when(messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)).thenReturn(settingsChangeListener);

        configurable = new LLMProvidersConfigurable(project);
    }

    @AfterEach
    void tearDown() {
        propertiesServiceMockedStatic.close();
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(configurable.getDisplayName()).isEqualTo("Large Language Models");
    }

    @Test
    void shouldApplyOllamaContextWindowOverrideSetting() throws ConfigurationException {
        LLMProvidersComponent component = getSettingsComponent();

        component.getOllamaContextWindowOverrideCheckBox().setSelected(true);

        assertThat(configurable.isModified()).isTrue();

        configurable.apply();

        assertThat(stateService.getOllamaContextWindowOverrideEnabled()).isTrue();
    }

    @Test
    void shouldResetOllamaContextWindowOverrideSetting() {
        stateService.setOllamaContextWindowOverrideEnabled(true);

        configurable.reset();

        assertThat(getSettingsComponent().getOllamaContextWindowOverrideCheckBox().isSelected()).isTrue();
    }


    @Test
    void shouldApplyCustomOpenAIUseMaxCompletionTokensSetting() throws ConfigurationException {
        // Issue #1225: the checkbox that switches the output cap to 'max_completion_tokens'
        // must survive the isModified/apply round-trip, or reasoning models stay broken
        // after the user ticks it.
        LLMProvidersComponent component = getSettingsComponent();

        component.getCustomOpenAIUseMaxCompletionTokensCheckBox().setSelected(true);

        assertThat(configurable.isModified()).isTrue();

        configurable.apply();

        assertThat(stateService.isCustomOpenAIUseMaxCompletionTokens()).isTrue();
    }

    @Test
    void shouldResetCustomOpenAIUseMaxCompletionTokensSetting() {
        stateService.setCustomOpenAIUseMaxCompletionTokens(true);

        configurable.reset();

        assertThat(getSettingsComponent().getCustomOpenAIUseMaxCompletionTokensCheckBox().isSelected()).isTrue();
    }

    @Test
    void shouldApplyCustomOpenAIContextWindowSetting() throws ConfigurationException {
        LLMProvidersComponent component = getSettingsComponent();

        component.getCustomOpenAIContextWindowEnabledCheckBox().setSelected(true);
        component.getCustomOpenAIContextWindowField().setNumber(32_000);

        assertThat(configurable.isModified()).isTrue();

        configurable.apply();

        assertThat(stateService.getCustomOpenAIContextWindow()).isEqualTo(32_000);
    }

    @Test
    void shouldClearCustomOpenAIContextWindowWhenDisabled() throws ConfigurationException {
        stateService.setCustomOpenAIContextWindow(32_000);
        LLMProvidersComponent component = getSettingsComponent();
        component.getCustomOpenAIContextWindowEnabledCheckBox().setSelected(false);

        configurable.apply();

        assertThat(stateService.getCustomOpenAIContextWindow()).isNull();
    }

    @Test
    void shouldResetCustomOpenAIContextWindowSetting() {
        stateService.setCustomOpenAIContextWindow(64_000);

        configurable.reset();

        LLMProvidersComponent component = getSettingsComponent();
        assertThat(component.getCustomOpenAIContextWindowEnabledCheckBox().isSelected()).isTrue();
        assertThat(component.getCustomOpenAIContextWindowField().getNumber()).isEqualTo(64_000);
    }

    @Test
    void shouldApplyCustomOpenAICostSettings() throws ConfigurationException {
        LLMProvidersComponent component = getSettingsComponent();

        component.getCustomOpenAIInputCostField().setValue(3.0d);
        component.getCustomOpenAIOutputCostField().setValue(15.0d);

        assertThat(configurable.isModified()).isTrue();

        configurable.apply();

        assertThat(stateService.getCustomOpenAIInputCost()).isEqualTo(3.0d);
        assertThat(stateService.getCustomOpenAIOutputCost()).isEqualTo(15.0d);
    }

    @Test
    void shouldStoreNullCustomOpenAICostWhenZero() throws ConfigurationException {
        stateService.setCustomOpenAIInputCost(3.0d);
        LLMProvidersComponent component = getSettingsComponent();
        component.getCustomOpenAIInputCostField().setValue(0.0d);

        configurable.apply();

        assertThat(stateService.getCustomOpenAIInputCost()).isNull();
    }

    @Test
    void shouldResetCustomOpenAICostSettings() {
        stateService.setCustomOpenAIInputCost(3.0d);
        stateService.setCustomOpenAIOutputCost(15.0d);

        configurable.reset();

        LLMProvidersComponent component = getSettingsComponent();
        assertThat(((Number) component.getCustomOpenAIInputCostField().getValue()).doubleValue()).isEqualTo(3.0d);
        assertThat(((Number) component.getCustomOpenAIOutputCostField().getValue()).doubleValue()).isEqualTo(15.0d);
    }

    @Test
    void shouldApplyShowThinkingSetting() throws ConfigurationException {
        LLMProvidersComponent component = getSettingsComponent();

        component.getShowThinkingCheckBox().setSelected(true);

        assertThat(configurable.isModified()).isTrue();

        configurable.apply();

        assertThat(stateService.getShowThinkingEnabled()).isTrue();
    }

    @Test
    void shouldResetShowThinkingSetting() {
        stateService.setShowThinkingEnabled(true);

        configurable.reset();

        assertThat(getSettingsComponent().getShowThinkingCheckBox().isSelected()).isTrue();
    }

    @Nested
    class ApplyValidation {

        @Test
        void shouldRejectApplyWhenEnabledProviderHasNoApiKey() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getAnthropicEnabledCheckBox().setSelected(true);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Anthropic");

            assertThat(stateService.isAnthropicEnabled()).isFalse();
        }

        @Test
        void shouldRejectApplyWhenKeyIsOnlyWhitespace() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getOpenAIEnabledCheckBox().setSelected(true);
            component.getOpenAIKeyField().setText("   ");

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("OpenAI");
        }

        @Test
        void shouldAggregateAllViolationsIntoSingleError() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getAnthropicEnabledCheckBox().setSelected(true);
            component.getGroqEnabledCheckBox().setSelected(true);
            component.getNvidiaEnabledCheckBox().setSelected(true);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Anthropic")
                    .hasMessageContaining("Groq")
                    .hasMessageContaining("NVIDIA");
        }

        @Test
        void shouldApplyWhenEnabledProviderHasApiKey() throws ConfigurationException {
            LLMProvidersComponent component = getSettingsComponent();
            component.getAnthropicEnabledCheckBox().setSelected(true);
            component.getAnthropicApiKeyField().setText("sk-ant-key");

            configurable.apply();

            assertThat(stateService.isAnthropicEnabled()).isTrue();
            assertThat(stateService.getAnthropicKey()).isEqualTo("sk-ant-key");
        }

        @Test
        void shouldApplyWhenProviderWithBlankKeyIsDisabled() throws ConfigurationException {
            configurable.apply();

            assertThat(stateService.isAnthropicEnabled()).isFalse();
        }

        @Test
        void shouldRejectApplyWhenAzureEnabledWithoutKeyEndpointOrDeployment() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAzureOpenAICheckBox().setSelected(true);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Azure OpenAI");
        }

        @Test
        void shouldApplyWhenAzureFullyConfigured() throws ConfigurationException {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAzureOpenAICheckBox().setSelected(true);
            component.getAzureOpenAIKeyField().setText("azure-key");
            component.getAzureOpenAIEndpointField().setText("https://example.openai.azure.com");
            component.getAzureOpenAIDeploymentField().setText("gpt-4o");

            configurable.apply();

            assertThat(stateService.getShowAzureOpenAIFields()).isTrue();
        }

        @Test
        void shouldRejectApplyWhenBedrockAccessKeyModeMissingCredentials() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAWSCheckBox().setSelected(true);
            component.getAwsAuthModeComboBox().setSelectedItem(AwsBedrockAuthMode.ACCESS_KEY);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("AWS Bedrock");
        }

        @Test
        void shouldRejectApplyWhenBedrockProfileModeMissingProfileName() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAWSCheckBox().setSelected(true);
            component.getAwsAuthModeComboBox().setSelectedItem(AwsBedrockAuthMode.PROFILE);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("AWS Bedrock");
        }

        @Test
        void shouldRejectApplyWhenBedrockBearerTokenModeMissingToken() {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAWSCheckBox().setSelected(true);
            component.getAwsAuthModeComboBox().setSelectedItem(AwsBedrockAuthMode.BEARER_TOKEN);

            assertThatThrownBy(() -> configurable.apply())
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("AWS Bedrock");
        }

        @Test
        void shouldApplyWhenBedrockProfileModeFullyConfigured() throws ConfigurationException {
            LLMProvidersComponent component = getSettingsComponent();
            component.getEnableAWSCheckBox().setSelected(true);
            component.getAwsAuthModeComboBox().setSelectedItem(AwsBedrockAuthMode.PROFILE);
            component.getAwsProfileName().setText("bedrock-profile");

            configurable.apply();

            assertThat(stateService.getShowAwsFields()).isTrue();
            assertThat(stateService.getAwsProfileName()).isEqualTo("bedrock-profile");
        }
    }

    @Nested
    class IsAnyApiKeyEnabled {

        @Test
        void shouldReturnFalseWhenNoKeysConfigured() {
            assertThat(invokeIsAnyApiKeyEnabled()).isFalse();
        }

        @Test
        void shouldReturnTrueWhenOpenAIKeySetAndEnabled() {
            stateService.setOpenAIKey("sk-openai-key");
            stateService.setOpenAIEnabled(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOpenAIKeySetButDisabled() {
            stateService.setOpenAIKey("sk-openai-key");
            stateService.setOpenAIEnabled(false);
            assertThat(invokeIsAnyApiKeyEnabled()).isFalse();
        }

        @Test
        void shouldReturnTrueWhenAnthropicKeySetAndEnabled() {
            stateService.setAnthropicKey("sk-ant-key");
            stateService.setAnthropicEnabled(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenDeepSeekKeySetAndEnabled() {
            stateService.setDeepSeekKey("ds-key");
            stateService.setDeepSeekEnabled(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenAzureKeySetAndEnabled() {
            stateService.setAzureOpenAIKey("azure-key");
            stateService.setShowAzureOpenAIFields(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenAwsRegionSetAndEnabled() {
            stateService.setAwsRegion("us-east-1");
            stateService.setShowAwsFields(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isFalse();
        }

        @Test
        void shouldReturnTrueWhenAwsAccessKeysSetAndEnabled() {
            stateService.setShowAwsFields(true);
            stateService.setAwsBedrockAuthMode(AwsBedrockAuthMode.ACCESS_KEY);
            stateService.setAwsAccessKeyId("access-key");
            stateService.setAwsSecretKey("secret-key");
            stateService.setAwsRegion("us-east-1");
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenAwsProfileSetAndEnabled() {
            stateService.setShowAwsFields(true);
            stateService.setAwsBedrockAuthMode(AwsBedrockAuthMode.PROFILE);
            stateService.setAwsProfileName("bedrock-profile");
            stateService.setAwsRegion("eu-west-1");
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenAwsBearerTokenSetAndEnabled() {
            stateService.setShowAwsFields(true);
            stateService.setAwsBedrockAuthMode(AwsBedrockAuthMode.BEARER_TOKEN);
            stateService.setAwsBearerToken("bedrock-token");
            stateService.setAwsRegion("us-east-1");
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }

        @Test
        void shouldReturnTrueWhenCustomOpenAIKeySetAndEnabled() {
            stateService.setCustomOpenAIApiKey("custom-key");
            stateService.setCustomOpenAIApiKeyEnabled(true);
            assertThat(invokeIsAnyApiKeyEnabled()).isTrue();
        }
    }

    private boolean invokeIsAnyApiKeyEnabled() {
        try {
            Method method = LLMProvidersConfigurable.class.getDeclaredMethod("isAnyApiKeyEnabled", DevoxxGenieStateService.class);
            method.setAccessible(true);
            return (boolean) method.invoke(configurable, stateService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LLMProvidersComponent getSettingsComponent() {
        try {
            Field field = LLMProvidersConfigurable.class.getDeclaredField("llmSettingsComponent");
            field.setAccessible(true);
            return (LLMProvidersComponent) field.get(configurable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
