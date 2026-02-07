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
class AgentApprovalProviderTest {

    @Mock
    private Project project;
    @Mock
    private ToolProviderRequest providerRequest;
    @Mock
    private Application application;
    @Mock
    private DevoxxGenieStateService stateService;

    private ToolSpecification readFileSpec;
    private ToolSpecification writeFileSpec;
    private ToolSpecification editFileSpec;
    private ToolSpecification listFilesSpec;
    private ToolSpecification searchFilesSpec;
    private ToolSpecification runCommandSpec;
    private ToolExecutor mockExecutor;
    private ToolProvider delegate;

    @BeforeEach
    void setUp() {
        readFileSpec = createSpec("read_file");
        writeFileSpec = createSpec("write_file");
        editFileSpec = createSpec("edit_file");
        listFilesSpec = createSpec("list_files");
        searchFilesSpec = createSpec("search_files");
        runCommandSpec = createSpec("run_command");
        mockExecutor = (req, id) -> "executed: " + req.name();

        delegate = req -> ToolProviderResult.builder()
                .add(readFileSpec, mockExecutor)
                .add(writeFileSpec, mockExecutor)
                .add(editFileSpec, mockExecutor)
                .add(listFilesSpec, mockExecutor)
                .add(searchFilesSpec, mockExecutor)
                .add(runCommandSpec, mockExecutor)
                .build();
    }

    @Test
    void autoApproveReadOnly_readFileTool_autoApproved() {
        // Read-only tools should be auto-approved when autoApproveReadOnly is true,
        // without calling AgentApprovalService at all
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            ToolExecutor readExecutor = result.toolExecutorByName("read_file");
            assertThat(readExecutor).isNotNull();

            String output = readExecutor.execute(createExecRequest("read_file"), null);
            assertThat(output).isEqualTo("executed: read_file");

            // AgentApprovalService should NOT have been called for read-only tools
            agentApprovalMock.verifyNoInteractions();
        }
    }

    @Test
    void autoApproveReadOnly_writeFileTool_requiresApproval() {
        // Write tools should always go through AgentApprovalService
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            agentApprovalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("write_file"), anyString())).thenReturn(true);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            ToolExecutor writeExecutor = result.toolExecutorByName("write_file");
            assertThat(writeExecutor).isNotNull();

            String output = writeExecutor.execute(createExecRequest("write_file"), null);
            assertThat(output).isEqualTo("executed: write_file");

            // AgentApprovalService SHOULD have been called for write tool
            agentApprovalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("write_file"), anyString()));
        }
    }

    @Test
    void writeToolDenied_returnsDenialMessage() {
        // When user denies approval, the tool should return a denial message
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            agentApprovalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("write_file"), anyString())).thenReturn(false);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            ToolExecutor writeExecutor = result.toolExecutorByName("write_file");
            String output = writeExecutor.execute(createExecRequest("write_file"), null);
            assertThat(output).isEqualTo("Tool execution was denied by the user.");
        }
    }

    @Test
    void autoApproveReadOnly_editFileTool_requiresApproval() {
        // edit_file is a write tool and should require approval
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            agentApprovalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("edit_file"), anyString())).thenReturn(true);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            ToolExecutor editExecutor = result.toolExecutorByName("edit_file");
            assertThat(editExecutor).isNotNull();

            String output = editExecutor.execute(createExecRequest("edit_file"), null);
            assertThat(output).isEqualTo("executed: edit_file");

            // AgentApprovalService SHOULD have been called for edit tool
            agentApprovalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("edit_file"), anyString()));
        }
    }

    @Test
    void autoApproveOff_allToolsRequireApproval() {
        // When auto-approve read-only is off, ALL tools go through approval
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            agentApprovalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), anyString(), anyString())).thenReturn(true);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, false);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            for (String toolName : new String[]{"read_file", "write_file", "edit_file", "list_files", "search_files", "run_command"}) {
                ToolExecutor executor = result.toolExecutorByName(toolName);
                assertThat(executor).isNotNull();
                String output = executor.execute(createExecRequest(toolName), null);
                assertThat(output).isEqualTo("executed: " + toolName);
            }

            // All 6 tools should have called approval
            agentApprovalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), anyString(), anyString()), times(6));
        }
    }

    @Test
    void provideTools_preservesAllToolSpecs() {
        AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            ToolProviderResult result = approvalProvider.provideTools(providerRequest);
            assertThat(result.tools()).hasSize(6);
        }
    }

    @Test
    void runCommandTool_requiresApproval() {
        // run_command is a write tool and should require approval
        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> agentApprovalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            agentApprovalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString())).thenReturn(true);

            AgentApprovalProvider approvalProvider = new AgentApprovalProvider(delegate, project, true);
            ToolProviderResult result = approvalProvider.provideTools(providerRequest);

            ToolExecutor cmdExecutor = result.toolExecutorByName("run_command");
            String output = cmdExecutor.execute(createExecRequest("run_command"), null);
            assertThat(output).isEqualTo("executed: run_command");

            agentApprovalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString()));
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
