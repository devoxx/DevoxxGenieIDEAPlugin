package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLMProviderServiceRouterTest {

    private MockedStatic<ApplicationManager> appManager;
    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private CredentialService credentialService;
    private DevoxxGenieStateService state;
    private LLMProviderService providerService;

    @BeforeEach
    void setUp() {
        Application application = mock(Application.class);
        appManager = Mockito.mockStatic(ApplicationManager.class);
        appManager.when(ApplicationManager::getApplication).thenReturn(application);

        state = new DevoxxGenieStateService();
        credentialService = mock(CredentialService.class);
        when(credentialService.getCredential(any(CredentialKey.class))).thenReturn("");
        when(application.getService(DevoxxGenieStateService.class)).thenReturn(state);
        when(application.getService(CredentialService.class)).thenReturn(credentialService);

        LLMModelRegistryService registry = mock(LLMModelRegistryService.class);
        when(registry.getModels()).thenReturn(List.<LanguageModel>of());
        when(application.getService(LLMModelRegistryService.class)).thenReturn(registry);

        stateStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        providerService = new LLMProviderService();
    }

    @AfterEach
    void tearDown() {
        stateStatic.close();
        appManager.close();
    }

    @Test
    void enabledRouter_listedWithoutModelsOrCredentials() {
        state.setRouterEnabled(ModelProvider.OpenRouter, true);

        assertThat(providerService.getAvailableModelProviders()).contains(ModelProvider.OpenRouter);

        verify(credentialService, never()).getCredential(any(CredentialKey.class));
        verify(credentialService, never()).getCredential(anyString());
    }

    @Test
    void disabledRouter_notListed() {
        state.setRouterEnabled(ModelProvider.OpenRouter, false);

        assertThat(providerService.getAvailableModelProviders()).doesNotContain(ModelProvider.OpenRouter);
    }

    @Test
    void genericRouter_isApiKeyBased_andSupported_andRefreshable() {
        assertThat(LLMProviderService.requiresApiKey(ModelProvider.OpenRouter)).isTrue();
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.OpenRouter)).isTrue();
    }

    @Test
    void cloudflare_unchangedAtGenericSites() {
        assertThat(DefaultLLMSettingsUtil.isApiKeyBasedProvider(ModelProvider.Cloudflare)).isFalse();
    }
}
