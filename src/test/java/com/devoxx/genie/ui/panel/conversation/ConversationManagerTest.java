package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.ChatService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-284: the tab's "View agent run" button is refreshed whenever the tab switches to another
 * conversation. The "+" button calls {@link ConversationManager#startNewConversation()} directly,
 * so before this the button kept the previous conversation's trace state.
 */
class ConversationManagerTest {

    private MockedStatic<FileListManager> fileListManager;
    private MockedStatic<ChatMemoryService> chatMemoryService;
    private MockedStatic<ConversationTabRegistry> tabRegistry;
    private MockedStatic<ApplicationManager> applicationManager;
    private ConversationManager manager;
    private final AtomicInteger notified = new AtomicInteger();

    @BeforeEach
    void setUp() {
        fileListManager = mockStatic(FileListManager.class);
        fileListManager.when(FileListManager::getInstance).thenReturn(mock(FileListManager.class));
        chatMemoryService = mockStatic(ChatMemoryService.class);
        chatMemoryService.when(ChatMemoryService::getInstance).thenReturn(mock(ChatMemoryService.class));
        ConversationTabRegistry registry = mock(ConversationTabRegistry.class);
        when(registry.getContentsForProject(any())).thenReturn(List.of());
        tabRegistry = mockStatic(ConversationTabRegistry.class);
        tabRegistry.when(ConversationTabRegistry::getInstance).thenReturn(registry);
        applicationManager = mockStatic(ApplicationManager.class);
        applicationManager.when(ApplicationManager::getApplication).thenReturn(mock(Application.class));

        Project project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("hash");
        manager = new ConversationManager(project, mock(ChatService.class), mock(ConversationHistoryManager.class),
                mock(MessageRenderer.class), new JLabel(), "tab-1");
        manager.setOwnPanel(mock(PromptOutputPanel.class));
        manager.setCurrentConversationListener(notified::incrementAndGet);
    }

    @AfterEach
    void tearDown() {
        fileListManager.close();
        chatMemoryService.close();
        tabRegistry.close();
        applicationManager.close();
    }

    @Test
    void startingANewConversationNotifiesTheListener() {
        Conversation previous = new Conversation();
        previous.setId("previous");
        manager.setCurrentConversation(previous);

        manager.startNewConversation();

        assertThat(manager.getCurrentConversation()).isNull();
        assertThat(notified).hasValue(1);
    }

    @Test
    void selectingAConversationFromHistoryNotifiesTheListener() {
        Conversation restored = new Conversation();
        restored.setId("restored");
        restored.setTitle("Restored conversation");

        manager.onConversationSelected(restored);

        assertThat(manager.getCurrentConversation()).isSameAs(restored);
        assertThat(notified).hasValue(1);
    }

    @Test
    void noListenerIsFine() {
        manager.setCurrentConversationListener(null);

        manager.startNewConversation();

        assertThat(manager.getCurrentConversation()).isNull();
    }
}
