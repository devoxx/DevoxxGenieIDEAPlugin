package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for lazy credential loading (keychain prompt at IDE startup).
 * Determining which providers are available must never read secrets from the
 * credential store; keys may only be read when actually used (prompt execution).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LLMProviderServiceTest {

    @Mock
    private Application application;

    @Mock
    private CredentialService credentialService;

    @Mock
    private LLMModelRegistryService modelRegistryService;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private LLMProviderService providerService;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        lenient().when(application.getService(CredentialService.class)).thenReturn(credentialService);
        lenient().when(application.getService(LLMModelRegistryService.class)).thenReturn(modelRegistryService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        lenient().when(credentialService.getCredential(any())).thenReturn("");
        lenient().when(modelRegistryService.getModels()).thenReturn(List.of(
                LanguageModel.builder().provider(ModelProvider.Anthropic).modelName("claude-sonnet-4-6").apiKeyUsed(true).build(),
                LanguageModel.builder().provider(ModelProvider.OpenAI).modelName("gpt-5").apiKeyUsed(true).build()
        ));

        providerService = new LLMProviderService();
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void getAvailableModelProvidersShouldNeverReadCredentialStore() {
        stateService.setAnthropicEnabled(true);

        providerService.getAvailableModelProviders();

        verify(credentialService, never()).getCredential(any());
    }

    @Test
    void enabledCloudProviderIsAvailableWithoutStoredKey() {
        stateService.setAnthropicEnabled(true);

        List<ModelProvider> providers = providerService.getAvailableModelProviders();

        assertThat(providers).contains(ModelProvider.Anthropic);
    }

    @Test
    void disabledCloudProviderIsNotAvailableEvenWithStoredKey() {
        stateService.setOpenAIEnabled(false);
        lenient().when(credentialService.getCredential(CredentialKey.OPEN_AI_KEY)).thenReturn("sk-test");

        List<ModelProvider> providers = providerService.getAvailableModelProviders();

        assertThat(providers).doesNotContain(ModelProvider.OpenAI);
    }

    @Test
    void getApiKeyReadsCredentialStoreLazilyAtUseTime() {
        when(credentialService.getCredential(CredentialKey.ANTHROPIC_KEY)).thenReturn("sk-ant-test");

        assertThat(providerService.getApiKey(ModelProvider.Anthropic)).isEqualTo("sk-ant-test");
        verify(credentialService).getCredential(CredentialKey.ANTHROPIC_KEY);
    }
}
