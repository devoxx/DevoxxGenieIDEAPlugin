package com.devoxx.genie.service.mcp;

import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MCPServiceTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    @Mock
    private MessageBus messageBus;

    @Mock
    private SettingsChangeListener settingsChangePublisher;

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<ApplicationManager> mockedAppManager;

    @BeforeEach
    void setUp() {
        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        mockedAppManager = Mockito.mockStatic(ApplicationManager.class);
        mockedAppManager.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getMessageBus()).thenReturn(messageBus);
        when(messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)).thenReturn(settingsChangePublisher);
    }

    @AfterEach
    void tearDown() {
        mockedStateService.close();
        mockedAppManager.close();
    }

    // -- isMCPEnabled --

    @Test
    void isMCPEnabled_returnsTrue_whenSettingsEnabled() {
        when(stateService.getMcpEnabled()).thenReturn(true);
        assertThat(MCPService.isMCPEnabled()).isTrue();
    }

    @Test
    void isMCPEnabled_returnsFalse_whenSettingsDisabled() {
        when(stateService.getMcpEnabled()).thenReturn(false);
        assertThat(MCPService.isMCPEnabled()).isFalse();
    }

    // -- isDebugLogsEnabled --

    @Test
    void isDebugLogsEnabled_returnsTrue_whenBothMcpEnabledAndDebugEnabled() {
        when(stateService.getMcpEnabled()).thenReturn(true);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(true);
        assertThat(MCPService.isDebugLogsEnabled()).isTrue();
    }

    @Test
    void isDebugLogsEnabled_returnsFalse_whenMcpDisabled() {
        when(stateService.getMcpEnabled()).thenReturn(false);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(true);
        assertThat(MCPService.isDebugLogsEnabled()).isFalse();
    }

    @Test
    void isDebugLogsEnabled_returnsFalse_whenDebugDisabled() {
        when(stateService.getMcpEnabled()).thenReturn(true);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(false);
        assertThat(MCPService.isDebugLogsEnabled()).isFalse();
    }

    @Test
    void isDebugLogsEnabled_returnsFalse_whenBothDisabled() {
        when(stateService.getMcpEnabled()).thenReturn(false);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(false);
        assertThat(MCPService.isDebugLogsEnabled()).isFalse();
    }

    // -- refreshToolWindowVisibility --

    @Test
    void refreshToolWindowVisibility_publishesSettingsChangedEvent() {
        MCPService.refreshToolWindowVisibility();
        verify(settingsChangePublisher).settingsChanged(true);
    }

    // -- resetNotificationFlag --

    @Test
    void resetNotificationFlag_doesNotThrow() {
        // resetNotificationFlag sets a private static field; just verify it doesn't throw
        MCPService.resetNotificationFlag();
    }

    // -- logDebug --

    @Test
    void logDebug_whenDebugEnabled_doesNotThrow() {
        when(stateService.getMcpEnabled()).thenReturn(true);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(true);

        // Should not throw; logs at info level internally
        MCPService.logDebug("test message");
    }

    @Test
    void logDebug_whenDebugDisabled_doesNotThrow() {
        when(stateService.getMcpEnabled()).thenReturn(false);
        when(stateService.getMcpDebugLogsEnabled()).thenReturn(false);

        // Should not throw; logs at debug level internally
        MCPService.logDebug("test message");
    }
}
