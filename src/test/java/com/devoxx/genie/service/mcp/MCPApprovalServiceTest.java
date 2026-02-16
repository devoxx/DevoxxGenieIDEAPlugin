package com.devoxx.genie.service.mcp;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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
class MCPApprovalServiceTest {

    @Mock
    private Project project;

    @Mock
    private Application application;

    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<ApplicationManager> mockedAppManager;

    @BeforeEach
    void setUp() {
        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        mockedAppManager = Mockito.mockStatic(ApplicationManager.class);
        mockedAppManager.when(ApplicationManager::getApplication).thenReturn(application);
    }

    @AfterEach
    void tearDown() {
        mockedStateService.close();
        mockedAppManager.close();
    }

    @Test
    void requestApproval_headlessEnvironment_returnsTrue() {
        when(application.isHeadlessEnvironment()).thenReturn(true);
        when(stateService.getMcpApprovalRequired()).thenReturn(true);

        boolean result = MCPApprovalService.requestApproval(project, "testTool", "{}");
        assertThat(result).isTrue();
    }

    @Test
    void requestApproval_approvalNotRequired_returnsTrue() {
        when(application.isHeadlessEnvironment()).thenReturn(false);
        when(stateService.getMcpApprovalRequired()).thenReturn(false);

        boolean result = MCPApprovalService.requestApproval(project, "testTool", "{}");
        assertThat(result).isTrue();
    }

    @Test
    void requestApproval_headlessAndNotRequired_returnsTrue() {
        when(application.isHeadlessEnvironment()).thenReturn(true);
        when(stateService.getMcpApprovalRequired()).thenReturn(false);

        boolean result = MCPApprovalService.requestApproval(project, "testTool", "{}");
        assertThat(result).isTrue();
    }

    @Test
    void requestApproval_nullProject_headlessEnvironment_returnsTrue() {
        when(application.isHeadlessEnvironment()).thenReturn(true);
        when(stateService.getMcpApprovalRequired()).thenReturn(true);

        boolean result = MCPApprovalService.requestApproval(null, "testTool", "{}");
        assertThat(result).isTrue();
    }

    @Test
    void requestApproval_approvalRequired_nonHeadless_timesOut() {
        when(application.isHeadlessEnvironment()).thenReturn(false);
        when(stateService.getMcpApprovalRequired()).thenReturn(true);
        when(stateService.getMcpApprovalTimeout()).thenReturn(1); // 1 second timeout

        // invokeLater does nothing (never completes the future), so it will time out
        doNothing().when(application).invokeLater(any(Runnable.class));

        boolean result = MCPApprovalService.requestApproval(project, "testTool", "{\"arg\": \"value\"}");
        assertThat(result).isFalse();
    }

    @Test
    void requestApproval_approvalRequired_nonHeadless_withNullProject_timesOut() {
        when(application.isHeadlessEnvironment()).thenReturn(false);
        when(stateService.getMcpApprovalRequired()).thenReturn(true);
        when(stateService.getMcpApprovalTimeout()).thenReturn(1);

        doNothing().when(application).invokeLater(any(Runnable.class));

        boolean result = MCPApprovalService.requestApproval(null, "testTool", "{}");
        assertThat(result).isFalse();
    }
}
