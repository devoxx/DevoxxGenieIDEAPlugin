package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
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
}
