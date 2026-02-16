package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.listener.NewlineShortcutChangeListener;
import com.devoxx.genie.ui.listener.ShortcutChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
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

import static com.devoxx.genie.model.Constant.SYSTEM_PROMPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptSettingsConfigurableTest {

    @Mock
    private Application application;

    @Mock
    private Project project;

    @Mock
    private MessageBus messageBus;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private PromptSettingsConfigurable configurable;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        lenient().when(project.getMessageBus()).thenReturn(messageBus);

        // Mock the sync publishers for shortcut topics
        ShortcutChangeListener shortcutListener = mock(ShortcutChangeListener.class);
        NewlineShortcutChangeListener newlineListener = mock(NewlineShortcutChangeListener.class);
        lenient().when(messageBus.syncPublisher(AppTopics.SHORTCUT_CHANGED_TOPIC)).thenReturn(shortcutListener);
        lenient().when(messageBus.syncPublisher(AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC)).thenReturn(newlineListener);

        configurable = new PromptSettingsConfigurable(project);
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(configurable.getDisplayName()).isEqualTo("Prompts");
    }

    // Note: createComponent() is not tested here because it calls createPanel()
    // which creates KeyboardShortcutPanel requiring SystemInfo and a full Swing environment.

    @Nested
    class IsModified {

        @Test
        void shouldDetectSystemPromptChange() {
            PromptSettingsComponent component = getComponent();
            component.getSystemPromptField().setText("Modified prompt");
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldNotBeModifiedWhenPromptSame() {
            PromptSettingsComponent component = getComponent();
            component.getSystemPromptField().setText(SYSTEM_PROMPT);
            // Shortcuts are initialized from stateService, so if shortcuts match,
            // and prompt matches, it should not be modified
            syncComponentShortcutsToState(component);
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void shouldDetectCreateDevoxxGenieMdChange() {
            PromptSettingsComponent component = getComponent();
            component.getCreateDevoxxGenieMdCheckbox().setSelected(!stateService.getCreateDevoxxGenieMd());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectIncludeProjectTreeChange() {
            PromptSettingsComponent component = getComponent();
            component.getIncludeProjectTreeCheckbox().setSelected(!stateService.getIncludeProjectTree());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectProjectTreeDepthChange() {
            PromptSettingsComponent component = getComponent();
            component.getProjectTreeDepthSpinner().setValue(8);
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectUseDevoxxGenieMdInPromptChange() {
            PromptSettingsComponent component = getComponent();
            component.getUseDevoxxGenieMdInPromptCheckbox().setSelected(!stateService.getUseDevoxxGenieMdInPrompt());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectUseClaudeOrAgentsMdChange() {
            PromptSettingsComponent component = getComponent();
            component.getUseClaudeOrAgentsMdInPromptCheckbox().setSelected(!stateService.getUseClaudeOrAgentsMdInPrompt());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectSubmitShortcutChange() {
            PromptSettingsComponent component = getComponent();
            component.setSubmitShortcutWindows("ctrl ENTER");
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectNewlineShortcutChange() {
            PromptSettingsComponent component = getComponent();
            component.setNewlineShortcutWindows("alt ENTER");
            assertThat(configurable.isModified()).isTrue();
        }
    }

    @Nested
    class Apply {

        @Test
        void shouldApplySystemPrompt() {
            PromptSettingsComponent component = getComponent();
            component.getSystemPromptField().setText("New system prompt");
            configurable.apply();
            assertThat(stateService.getSystemPrompt()).isEqualTo("New system prompt");
        }

        @Test
        void shouldApplyCreateDevoxxGenieMd() {
            PromptSettingsComponent component = getComponent();
            component.getCreateDevoxxGenieMdCheckbox().setSelected(true);
            configurable.apply();
            assertThat(stateService.getCreateDevoxxGenieMd()).isTrue();
        }

        @Test
        void shouldApplyIncludeProjectTree() {
            PromptSettingsComponent component = getComponent();
            component.getIncludeProjectTreeCheckbox().setSelected(true);
            configurable.apply();
            assertThat(stateService.getIncludeProjectTree()).isTrue();
        }

        @Test
        void shouldApplyProjectTreeDepth() {
            PromptSettingsComponent component = getComponent();
            component.getProjectTreeDepthSpinner().setValue(7);
            configurable.apply();
            assertThat(stateService.getProjectTreeDepth()).isEqualTo(7);
        }

        @Test
        void shouldApplyUseDevoxxGenieMdInPrompt() {
            PromptSettingsComponent component = getComponent();
            component.getUseDevoxxGenieMdInPromptCheckbox().setSelected(true);
            configurable.apply();
            assertThat(stateService.getUseDevoxxGenieMdInPrompt()).isTrue();
        }

        @Test
        void shouldApplyUseClaudeOrAgentsMd() {
            PromptSettingsComponent component = getComponent();
            component.getUseClaudeOrAgentsMdInPromptCheckbox().setSelected(false);
            configurable.apply();
            assertThat(stateService.getUseClaudeOrAgentsMdInPrompt()).isFalse();
        }

        @Test
        void shouldNotChangePromptWhenUnmodified() {
            PromptSettingsComponent component = getComponent();
            String originalPrompt = stateService.getSystemPrompt();
            component.getSystemPromptField().setText(originalPrompt);
            configurable.apply();
            assertThat(stateService.getSystemPrompt()).isEqualTo(originalPrompt);
        }
    }

    @Nested
    class Reset {

        @Test
        void shouldResetSystemPrompt() {
            stateService.setSystemPrompt("Custom prompt");
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getSystemPromptField().getText()).isEqualTo("Custom prompt");
        }

        @Test
        void shouldResetCreateDevoxxGenieMd() {
            stateService.setCreateDevoxxGenieMd(true);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getCreateDevoxxGenieMdCheckbox().isSelected()).isTrue();
        }

        @Test
        void shouldResetIncludeProjectTree() {
            stateService.setIncludeProjectTree(true);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getIncludeProjectTreeCheckbox().isSelected()).isTrue();
        }

        @Test
        void shouldResetProjectTreeDepth() {
            stateService.setProjectTreeDepth(5);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getProjectTreeDepthSpinner().getValue()).isEqualTo(5);
        }

        @Test
        void shouldResetUseDevoxxGenieMdInPrompt() {
            stateService.setUseDevoxxGenieMdInPrompt(true);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getUseDevoxxGenieMdInPromptCheckbox().isSelected()).isTrue();
        }

        @Test
        void shouldResetUseClaudeOrAgentsMd() {
            stateService.setUseClaudeOrAgentsMdInPrompt(false);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getUseClaudeOrAgentsMdInPromptCheckbox().isSelected()).isFalse();
        }

        @Test
        void shouldResetShortcuts() {
            stateService.setSubmitShortcutWindows("ctrl ENTER");
            stateService.setSubmitShortcutMac("ctrl ENTER");
            stateService.setSubmitShortcutLinux("ctrl ENTER");
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getSubmitShortcutWindows()).isEqualTo("ctrl ENTER");
            assertThat(component.getSubmitShortcutMac()).isEqualTo("ctrl ENTER");
            assertThat(component.getSubmitShortcutLinux()).isEqualTo("ctrl ENTER");
        }

        @Test
        void shouldResetNewlineShortcuts() {
            stateService.setNewlineShortcutWindows("alt ENTER");
            stateService.setNewlineShortcutMac("alt ENTER");
            stateService.setNewlineShortcutLinux("alt ENTER");
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getNewlineShortcutWindows()).isEqualTo("alt ENTER");
            assertThat(component.getNewlineShortcutMac()).isEqualTo("alt ENTER");
            assertThat(component.getNewlineShortcutLinux()).isEqualTo("alt ENTER");
        }

        @Test
        void shouldEnableRelatedFieldsWhenCreateMdEnabled() {
            stateService.setCreateDevoxxGenieMd(true);
            stateService.setIncludeProjectTree(true);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getIncludeProjectTreeCheckbox().isEnabled()).isTrue();
            assertThat(component.getProjectTreeDepthSpinner().isEnabled()).isTrue();
            assertThat(component.getUseDevoxxGenieMdInPromptCheckbox().isEnabled()).isTrue();
            assertThat(component.getCreateDevoxxGenieMdButton().isEnabled()).isTrue();
        }

        @Test
        void shouldDisableRelatedFieldsWhenCreateMdDisabled() {
            stateService.setCreateDevoxxGenieMd(false);
            configurable.reset();
            PromptSettingsComponent component = getComponent();
            assertThat(component.getIncludeProjectTreeCheckbox().isEnabled()).isFalse();
            assertThat(component.getUseDevoxxGenieMdInPromptCheckbox().isEnabled()).isFalse();
            assertThat(component.getCreateDevoxxGenieMdButton().isEnabled()).isFalse();
        }
    }

    @Nested
    class UpdateTextAreaIfModified {

        @Test
        void shouldUpdateWhenTextIsModified() {
            JTextArea textArea = new JTextArea("new value");
            final String[] captured = {null};
            configurable.updateTextAreaIfModified(textArea, "old value", v -> captured[0] = v);
            assertThat(captured[0]).isEqualTo("new value");
        }

        @Test
        void shouldNotUpdateWhenTextIsUnmodified() {
            JTextArea textArea = new JTextArea("same value");
            final boolean[] wasCalled = {false};
            configurable.updateTextAreaIfModified(textArea, "same value", v -> wasCalled[0] = true);
            assertThat(wasCalled[0]).isFalse();
        }

        @Test
        void shouldHandleNullTextArea() {
            JTextArea textArea = new JTextArea();
            textArea.setText(null);
            final boolean[] wasCalled = {false};
            // When text is null, should not call update
            configurable.updateTextAreaIfModified(textArea, "old", v -> wasCalled[0] = true);
            // JTextArea.getText() never returns null, it returns "" for null input
            // So it will be called if "" != "old"
            assertThat(wasCalled[0]).isTrue();
        }
    }

    private void syncComponentShortcutsToState(PromptSettingsComponent component) {
        component.setSubmitShortcutWindows(stateService.getSubmitShortcutWindows());
        component.setSubmitShortcutMac(stateService.getSubmitShortcutMac());
        component.setSubmitShortcutLinux(stateService.getSubmitShortcutLinux());
        component.setNewlineShortcutWindows(stateService.getNewlineShortcutWindows());
        component.setNewlineShortcutMac(stateService.getNewlineShortcutMac());
        component.setNewlineShortcutLinux(stateService.getNewlineShortcutLinux());
    }

    private PromptSettingsComponent getComponent() {
        try {
            java.lang.reflect.Field field = PromptSettingsConfigurable.class.getDeclaredField("promptSettingsComponent");
            field.setAccessible(true);
            return (PromptSettingsComponent) field.get(configurable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
