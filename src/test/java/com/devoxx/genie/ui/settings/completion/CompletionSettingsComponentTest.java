package com.devoxx.genie.ui.settings.completion;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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

import javax.swing.*;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompletionSettingsComponentTest {

    @Mock
    private Application application;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private CompletionSettingsComponent component;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        // The loadModelsForProvider calls executeOnPooledThread - mock it to run inline
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            // Don't run the actual model loading - it would fail without real servers
            return null;
        }).when(application).executeOnPooledThread(any(Runnable.class));

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        component = new CompletionSettingsComponent();
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldCreatePanel() {
        assertThat(component.createPanel()).isNotNull();
    }

    @Nested
    class ProviderDisplayNameMapping {

        @Test
        void shouldMapOllamaToDisplayName() throws Exception {
            String result = invokeProviderToDisplayName("Ollama");
            assertThat(result).isEqualTo("Ollama");
        }

        @Test
        void shouldMapLMStudioToDisplayName() throws Exception {
            String result = invokeProviderToDisplayName("LMStudio");
            assertThat(result).isEqualTo("LM Studio");
        }

        @Test
        void shouldMapEmptyToNone() throws Exception {
            String result = invokeProviderToDisplayName("");
            assertThat(result).isEqualTo("None");
        }

        @Test
        void shouldMapUnknownToNone() throws Exception {
            String result = invokeProviderToDisplayName("Unknown");
            assertThat(result).isEqualTo("None");
        }

        @Test
        void shouldMapNoneDisplayToEmpty() throws Exception {
            String result = invokeDisplayNameToProvider("None");
            assertThat(result).isEmpty();
        }

        @Test
        void shouldMapOllamaDisplayToOllama() throws Exception {
            String result = invokeDisplayNameToProvider("Ollama");
            assertThat(result).isEqualTo("Ollama");
        }

        @Test
        void shouldMapLMStudioDisplayToLMStudio() throws Exception {
            String result = invokeDisplayNameToProvider("LM Studio");
            assertThat(result).isEqualTo("LMStudio");
        }

        private String invokeProviderToDisplayName(String provider) throws Exception {
            Method method = CompletionSettingsComponent.class.getDeclaredMethod("providerToDisplayName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, provider);
        }

        private String invokeDisplayNameToProvider(String displayName) throws Exception {
            Method method = CompletionSettingsComponent.class.getDeclaredMethod("displayNameToProvider", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, displayName);
        }
    }

    @Nested
    class IsModified {

        @Test
        void shouldNotBeModifiedWhenProviderMatchesState() {
            // Default state: provider is "", display name is "None"
            // Component is initialized with "None" selected
            assertThat(component.isModified()).isFalse();
        }

        @Test
        void shouldBeModifiedWhenProviderChanges() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("Ollama");
            assertThat(component.isModified()).isTrue();
        }

        @Test
        void shouldBeModifiedWhenMaxTokensChanges() {
            JBIntSpinnerHelper.setNumber(component, "maxTokensSpinner", 128);
            assertThat(component.isModified()).isTrue();
        }

        @Test
        void shouldBeModifiedWhenTimeoutChanges() {
            JBIntSpinnerHelper.setNumber(component, "timeoutSpinner", 10000);
            assertThat(component.isModified()).isTrue();
        }

        @Test
        void shouldBeModifiedWhenDebounceChanges() {
            JBIntSpinnerHelper.setNumber(component, "debounceSpinner", 500);
            assertThat(component.isModified()).isTrue();
        }
    }

    @Nested
    class Apply {

        @Test
        void shouldApplyProviderOllama() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("Ollama");
            component.apply();
            assertThat(stateService.getInlineCompletionProvider()).isEqualTo("Ollama");
        }

        @Test
        void shouldApplyProviderNone() {
            stateService.setInlineCompletionProvider("Ollama");
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("None");
            component.apply();
            assertThat(stateService.getInlineCompletionProvider()).isEmpty();
        }

        @Test
        void shouldApplyProviderLMStudio() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("LM Studio");
            component.apply();
            assertThat(stateService.getInlineCompletionProvider()).isEqualTo("LMStudio");
        }

        @Test
        void shouldApplyMaxTokens() {
            JBIntSpinnerHelper.setNumber(component, "maxTokensSpinner", 128);
            component.apply();
            assertThat(stateService.getInlineCompletionMaxTokens()).isEqualTo(128);
        }

        @Test
        void shouldApplyTimeout() {
            JBIntSpinnerHelper.setNumber(component, "timeoutSpinner", 15000);
            component.apply();
            assertThat(stateService.getInlineCompletionTimeoutMs()).isEqualTo(15000);
        }

        @Test
        void shouldApplyDebounce() {
            JBIntSpinnerHelper.setNumber(component, "debounceSpinner", 600);
            component.apply();
            assertThat(stateService.getInlineCompletionDebounceMs()).isEqualTo(600);
        }
    }

    @Nested
    class Reset {

        @Test
        void shouldResetProvider() {
            stateService.setInlineCompletionProvider("Ollama");
            component.reset();
            JComboBox<String> providerComboBox = getProviderComboBox();
            assertThat(providerComboBox.getSelectedItem()).isEqualTo("Ollama");
        }

        @Test
        void shouldResetMaxTokens() {
            stateService.setInlineCompletionMaxTokens(200);
            component.reset();
            assertThat(JBIntSpinnerHelper.getNumber(component, "maxTokensSpinner")).isEqualTo(200);
        }

        @Test
        void shouldResetTimeout() {
            stateService.setInlineCompletionTimeoutMs(20000);
            component.reset();
            assertThat(JBIntSpinnerHelper.getNumber(component, "timeoutSpinner")).isEqualTo(20000);
        }

        @Test
        void shouldResetDebounce() {
            stateService.setInlineCompletionDebounceMs(1000);
            component.reset();
            assertThat(JBIntSpinnerHelper.getNumber(component, "debounceSpinner")).isEqualTo(1000);
        }
    }

    @Nested
    class EnabledState {

        @Test
        void shouldDisableComponentsWhenNoneSelected() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("None");
            // Trigger the listener
            invokeUpdateEnabledState();

            assertThat(getModelComboBox().isEnabled()).isFalse();
            assertThat(getRefreshButton().isEnabled()).isFalse();
        }

        @Test
        void shouldEnableComponentsWhenProviderSelected() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("Ollama");
            invokeUpdateEnabledState();

            assertThat(getModelComboBox().isEnabled()).isTrue();
            assertThat(getRefreshButton().isEnabled()).isTrue();
        }
    }

    @Nested
    class ApplyThenIsModified {

        @Test
        void shouldNotBeModifiedAfterApply() {
            JComboBox<String> providerComboBox = getProviderComboBox();
            providerComboBox.setSelectedItem("Ollama");
            assertThat(component.isModified()).isTrue();
            component.apply();
            assertThat(component.isModified()).isFalse();
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> getProviderComboBox() {
        try {
            java.lang.reflect.Field field = CompletionSettingsComponent.class.getDeclaredField("providerComboBox");
            field.setAccessible(true);
            return (JComboBox<String>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> getModelComboBox() {
        try {
            java.lang.reflect.Field field = CompletionSettingsComponent.class.getDeclaredField("modelComboBox");
            field.setAccessible(true);
            return (JComboBox<String>) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JButton getRefreshButton() {
        try {
            java.lang.reflect.Field field = CompletionSettingsComponent.class.getDeclaredField("refreshButton");
            field.setAccessible(true);
            return (JButton) field.get(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeUpdateEnabledState() {
        try {
            Method method = CompletionSettingsComponent.class.getDeclaredMethod("updateEnabledState");
            method.setAccessible(true);
            method.invoke(component);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper class to access JBIntSpinner fields via reflection.
     */
    static class JBIntSpinnerHelper {
        static void setNumber(CompletionSettingsComponent component, String fieldName, int value) {
            try {
                java.lang.reflect.Field field = CompletionSettingsComponent.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                com.intellij.ui.JBIntSpinner spinner = (com.intellij.ui.JBIntSpinner) field.get(component);
                spinner.setNumber(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static int getNumber(CompletionSettingsComponent component, String fieldName) {
            try {
                java.lang.reflect.Field field = CompletionSettingsComponent.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                com.intellij.ui.JBIntSpinner spinner = (com.intellij.ui.JBIntSpinner) field.get(component);
                return spinner.getNumber();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
