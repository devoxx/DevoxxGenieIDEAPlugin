package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentToolProviderFactoryTest {

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;
    @Mock
    private Application application;
    @Mock
    private DevoxxGenieStateService stateService;

    @BeforeEach
    void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        when(project.getBasePath()).thenReturn("/tmp/test-project");
    }

    @Test
    void createToolProvider_agentDisabled_returnsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(false);

            ToolProvider result = AgentToolProviderFactory.createToolProvider(project);
            assertThat(result).isNull();
        }
    }

    @Test
    void createToolProvider_agentEnabled_returnsProvider() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentAutoApproveReadOnly()).thenReturn(false);
            when(stateService.getAgentMaxToolCalls()).thenReturn(25);
            when(stateService.getMcpEnabled()).thenReturn(false);
            when(stateService.getMcpSettings()).thenReturn(new MCPSettings());

            ToolProvider result = AgentToolProviderFactory.createToolProvider(project);
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(AgentLoopTracker.class);
        }
    }

    @Test
    void createToolProvider_agentEnabledNullSettings_returnsProviderWithDefaults() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentAutoApproveReadOnly()).thenReturn(null);
            when(stateService.getAgentMaxToolCalls()).thenReturn(null);
            when(stateService.getMcpEnabled()).thenReturn(false);
            when(stateService.getMcpSettings()).thenReturn(new MCPSettings());

            ToolProvider result = AgentToolProviderFactory.createToolProvider(project);
            assertThat(result).isNotNull();
        }
    }
}
