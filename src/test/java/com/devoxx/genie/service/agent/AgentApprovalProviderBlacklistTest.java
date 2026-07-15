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
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.devoxx.genie.model.Constant.COMMAND_BLACKLIST_ACTION_ASK;
import static com.devoxx.genie.model.Constant.COMMAND_BLACKLIST_ACTION_BLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Regression tests for issue #1209: a blacklisted run_command (e.g. "git reset --hard")
 * must never execute silently, even when the user has disabled write approvals
 * (auto-approve). Depending on settings it is either blocked outright or forces
 * the approval dialog.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentApprovalProviderBlacklistTest {

    private static final List<String> BLACKLIST = List.of("git reset --hard", "rm -rf");

    @Mock
    private Project project;
    @Mock
    private Application application;
    @Mock
    private DevoxxGenieStateService stateService;

    private ToolProvider delegate;

    @BeforeEach
    void setUp() {
        ToolSpecification runCommandSpec = ToolSpecification.builder()
                .name("run_command")
                .description("Run a terminal command")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        ToolSpecification writeFileSpec = ToolSpecification.builder()
                .name("write_file")
                .description("Write a file")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        ToolExecutor echoExecutor = (req, id) -> "executed: " + req.name();

        delegate = req -> ToolProviderResult.builder()
                .add(runCommandSpec, echoExecutor)
                .add(writeFileSpec, echoExecutor)
                .build();

        when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        when(stateService.getAgentCommandBlacklist()).thenReturn(BLACKLIST);
    }

    @Test
    void blockMode_blacklistedCommand_isBlockedWithoutExecutionOrApproval() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_BLOCK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(execRequest("{\"command\": \"git reset --hard\"}"), null);

            assertThat(output)
                    .doesNotContain("executed:")
                    .contains("blocked")
                    .contains("git reset --hard");
            // No approval dialog in block mode: the command is refused outright
            approvalMock.verifyNoInteractions();
        }
    }

    @Test
    void blockMode_blacklistedCommandInsideCompoundCommand_isBlocked() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_BLOCK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(
                    execRequest("{\"command\": \"cd frontend && git reset --hard HEAD~1\"}"), null);

            assertThat(output).doesNotContain("executed:").contains("blocked");
            approvalMock.verifyNoInteractions();
        }
    }

    @Test
    void askMode_blacklistedCommand_forcesApprovalEvenWhenAutoApproved() {
        // This is the exact scenario from issue #1209: write approval is disabled
        // (auto-approve), yet the blacklisted command must still hit the approval gate.
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_ASK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            approvalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString(), eq("git reset --hard")))
                    .thenReturn(true);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(execRequest("{\"command\": \"git reset --hard\"}"), null);

            assertThat(output).isEqualTo("executed: run_command");
            // The forced (blacklist-aware) approval overload must have been used
            approvalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString(), eq("git reset --hard")));
        }
    }

    @Test
    void askMode_blacklistedCommandDenied_returnsDenialMessage() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_ASK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            approvalMock.when(() -> AgentApprovalService.requestApproval(
                    any(), anyString(), anyString(), anyString())).thenReturn(false);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(execRequest("{\"command\": \"rm -rf build\"}"), null);

            assertThat(output).isEqualTo("Tool execution was denied by the user.");
        }
    }

    @Test
    void nonBlacklistedCommand_followsNormalApprovalFlow() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_BLOCK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            approvalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString())).thenReturn(true);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(execRequest("{\"command\": \"git status\"}"), null);

            assertThat(output).isEqualTo("executed: run_command");
            // Normal (non-forced) approval path
            approvalMock.verify(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString()));
        }
    }

    @Test
    void blacklistDoesNotAffectOtherWriteTools() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_BLOCK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            approvalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("write_file"), anyString())).thenReturn(true);

            AgentApprovalProvider provider = new AgentApprovalProvider(delegate, project, true);
            ToolExecutor executor = provider.provideTools(null).toolExecutorByName("write_file");

            // write_file args may contain blacklisted text as file *content*; must not be blocked
            String output = executor.execute(ToolExecutionRequest.builder()
                    .name("write_file")
                    .arguments("{\"path\": \"cleanup.sh\", \"content\": \"git reset --hard\"}")
                    .build(), null);

            assertThat(output).isEqualTo("executed: write_file");
        }
    }

    @Test
    void executeWithContext_blockMode_blacklistedCommand_isBlocked() {
        when(stateService.getAgentCommandBlacklistAction()).thenReturn(COMMAND_BLACKLIST_ACTION_BLOCK);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);

            ToolExecutor executor = provideRunCommandExecutor();
            dev.langchain4j.service.tool.ToolExecutionResult result =
                    executor.executeWithContext(execRequest("{\"command\": \"git reset --hard\"}"), null);

            assertThat(result.resultText()).doesNotContain("executed:").contains("blocked");
            approvalMock.verifyNoInteractions();
        }
    }

    @Test
    void missingStateService_failsOpenToNormalApprovalFlow() {
        // If settings cannot be read (e.g. very early startup), the blacklist is skipped
        // and the regular approval flow still protects the user.
        when(application.getService(DevoxxGenieStateService.class)).thenReturn(null);

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<AgentApprovalService> approvalMock = mockStatic(AgentApprovalService.class)) {
            appMock.when(ApplicationManager::getApplication).thenReturn(application);
            approvalMock.when(() -> AgentApprovalService.requestApproval(
                    eq(project), eq("run_command"), anyString())).thenReturn(true);

            ToolExecutor executor = provideRunCommandExecutor();
            String output = executor.execute(execRequest("{\"command\": \"git reset --hard\"}"), null);

            assertThat(output).isEqualTo("executed: run_command");
        }
    }

    private ToolExecutor provideRunCommandExecutor() {
        AgentApprovalProvider provider = new AgentApprovalProvider(delegate, project, true);
        return provider.provideTools(null).toolExecutorByName("run_command");
    }

    private ToolExecutionRequest execRequest(String arguments) {
        return ToolExecutionRequest.builder()
                .name("run_command")
                .arguments(arguments)
                .build();
    }
}
