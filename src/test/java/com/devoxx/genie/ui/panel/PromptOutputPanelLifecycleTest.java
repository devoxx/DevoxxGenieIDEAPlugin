package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.panel.conversation.ConversationPanel;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Swing calls {@code removeNotify()} whenever a component leaves the UI tree, not only when a chat
 * tab is closed (for example when the tool window is hidden, re-docked or re-laid out). Disposing
 * the conversation panel there disconnected the tab's ChatService from the message bus: the chat
 * kept rendering, but no later turn was saved to the conversation history and no agent trace was
 * written. The panel must only be disposed when its tab is really closed.
 */
class PromptOutputPanelLifecycleTest {

    private MockedStatic<ApplicationManager> applicationManager;
    private PromptPanelRegistry registry;
    private ConversationPanel conversationPanel;
    private PromptOutputPanel panel;

    @BeforeEach
    void setUp() throws Exception {
        registry = mock(PromptPanelRegistry.class);
        Application application = mock(Application.class);
        when(application.getService(PromptPanelRegistry.class)).thenReturn(registry);
        applicationManager = Mockito.mockStatic(ApplicationManager.class);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(application);

        // Build the panel without its constructor, which needs the full Compose UI.
        panel = mock(PromptOutputPanel.class, CALLS_REAL_METHODS);
        conversationPanel = mock(ConversationPanel.class);
        setField("conversationPanel", conversationPanel);
        setField("project", mock(Project.class));
    }

    @AfterEach
    void tearDown() {
        applicationManager.close();
    }

    @Test
    void detachingFromTheUiTree_doesNotDisposeTheTab() {
        // removeNotify() runs on every detach (hide, re-dock, re-layout), so the panel must not tear
        // itself down there; disposal belongs to dispose(), called when the tab closes.
        assertThatThrownBy(() -> PromptOutputPanel.class.getDeclaredMethod("removeNotify"))
                .as("PromptOutputPanel must not override removeNotify()")
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void dispose_disposesTheConversation_andUnregistersThePanel() {
        panel.dispose();

        verify(conversationPanel).dispose();
        verify(registry).unregisterPanel(any(), eq(panel));
    }

    private void setField(String name, Object value) throws Exception {
        Field field = PromptOutputPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(panel, value);
    }
}
