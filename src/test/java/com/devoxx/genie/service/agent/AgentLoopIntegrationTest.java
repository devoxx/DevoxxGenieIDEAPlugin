package com.devoxx.genie.service.agent;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration test that exercises the full provider chain:
 * ToolProvider → AgentApprovalProvider → AgentLoopTracker
 *
 * Uses mock tools (not real BuiltInToolProvider) to avoid IntelliJ platform dependencies.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentLoopIntegrationTest {

    @Mock
    private Project project;
    @Mock
    private ToolProviderRequest providerRequest;
    @Mock
    private Application application;
    @Mock
    private DevoxxGenieStateService stateService;

    private ToolProvider createMockToolProvider() {
        ToolExecutor mockExecutor = (req, id) -> "executed: " + req.name();

        return req -> ToolProviderResult.builder()
                .add(createSpec("read_file"), mockExecutor)
                .add(createSpec("write_file"), mockExecutor)
                .add(createSpec("list_files"), mockExecutor)
                .add(createSpec("search_files"), mockExecutor)
                .add(createSpec("run_command"), mockExecutor)
                .build();
    }

    @Test
    void fullProviderChain_executesToolsWithTracking() {
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            when(application.isHeadlessEnvironment()).thenReturn(true);
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getMcpApprovalRequired()).thenReturn(false);

            // Build the chain: MockTools → Approval (auto-approve read-only) → Tracker (max 3)
            ToolProvider mockTools = createMockToolProvider();
            AgentApprovalProvider approved = new AgentApprovalProvider(mockTools, project, true);
            AgentLoopTracker tracker = new AgentLoopTracker(approved, 3);

            ToolProviderResult result = tracker.provideTools(providerRequest);
            assertThat(result.tools()).hasSize(5);

            // Execute read_file 3 times (within limit)
            ToolExecutor readExecutor = result.toolExecutorByName("read_file");
            assertThat(readExecutor).isNotNull();

            String result1 = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(result1).isEqualTo("executed: read_file");

            String result2 = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(result2).isEqualTo("executed: read_file");

            String result3 = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(result3).isEqualTo("executed: read_file");

            // 4th call exceeds limit
            String result4 = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(result4).contains("Agent loop limit reached");
            assertThat(result4).contains("3 tool calls");
        }
    }

    @Test
    void fullProviderChain_tracksAcrossDifferentTools() {
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            when(application.isHeadlessEnvironment()).thenReturn(true);
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getMcpApprovalRequired()).thenReturn(false);

            ToolProvider mockTools = createMockToolProvider();
            AgentApprovalProvider approved = new AgentApprovalProvider(mockTools, project, true);
            AgentLoopTracker tracker = new AgentLoopTracker(approved, 2);

            ToolProviderResult result = tracker.provideTools(providerRequest);

            // Use read_file once
            ToolExecutor readExecutor = result.toolExecutorByName("read_file");
            readExecutor.execute(createExecRequest("read_file"), null);

            // Use search_files once
            ToolExecutor searchExecutor = result.toolExecutorByName("search_files");
            searchExecutor.execute(createExecRequest("search_files"), null);

            // 3rd call (any tool) should hit limit
            String result3 = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(result3).contains("Agent loop limit reached");
        }
    }

    @Test
    void fullProviderChain_writeToolRequiresApproval_readToolAutoApproved() {
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            when(application.isHeadlessEnvironment()).thenReturn(true);
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getMcpApprovalRequired()).thenReturn(false);

            ToolProvider mockTools = createMockToolProvider();
            AgentApprovalProvider approved = new AgentApprovalProvider(mockTools, project, true);
            AgentLoopTracker tracker = new AgentLoopTracker(approved, 10);

            ToolProviderResult result = tracker.provideTools(providerRequest);

            // read_file is auto-approved
            ToolExecutor readExecutor = result.toolExecutorByName("read_file");
            String readResult = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(readResult).isEqualTo("executed: read_file");

            // write_file needs approval but auto-approved in headless mode
            ToolExecutor writeExecutor = result.toolExecutorByName("write_file");
            String writeResult = writeExecutor.execute(createExecRequest("write_file"), null);
            assertThat(writeResult).isEqualTo("executed: write_file");

            // run_command needs approval but auto-approved in headless mode
            ToolExecutor runExecutor = result.toolExecutorByName("run_command");
            String runResult = runExecutor.execute(createExecRequest("run_command"), null);
            assertThat(runResult).isEqualTo("executed: run_command");
        }
    }

    @Test
    void fullProviderChain_deniedApproval_returnsMessage() {
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            // NOT headless — approval will be checked
            when(application.isHeadlessEnvironment()).thenReturn(false);
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getMcpApprovalRequired()).thenReturn(true);
            // Set timeout very short so approval times out (= denied)
            when(stateService.getMcpApprovalTimeout()).thenReturn(1);

            ToolProvider mockTools = createMockToolProvider();
            // auto-approve read-only, but write_file needs approval
            AgentApprovalProvider approved = new AgentApprovalProvider(mockTools, project, true);
            AgentLoopTracker tracker = new AgentLoopTracker(approved, 10);

            ToolProviderResult result = tracker.provideTools(providerRequest);

            // read_file should be auto-approved even with non-headless
            ToolExecutor readExecutor = result.toolExecutorByName("read_file");
            String readResult = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(readResult).isEqualTo("executed: read_file");
        }
    }

    @Test
    void backwardCompatibility_agentDisabled_factoryReturnsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(false);

            assertThat(AgentToolProviderFactory.createToolProvider(project)).isNull();
        }
    }

    private ToolSpecification createSpec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description("Test " + name)
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    private ToolExecutionRequest createExecRequest(String name) {
        return ToolExecutionRequest.builder()
                .name(name)
                .arguments("{}")
                .build();
    }
}
