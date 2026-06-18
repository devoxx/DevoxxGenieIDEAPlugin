package com.devoxx.genie.service.agent;

import com.intellij.openapi.project.Project;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Wraps tools with conditional approval logic.
 * Read-only tools (read_file, list_files, search_files, fetch_page) can be auto-approved.
 * Write tools (write_file, run_command) always require approval.
 */
@Slf4j
public class AgentApprovalProvider implements ToolProvider {

    private static final Set<String> READ_ONLY_TOOLS = Set.of(
            "read_file", "list_files", "search_files", "fetch_page", "semantic_search",
            "web_search",
            "find_symbols", "document_symbols", "find_references", "find_definition", "find_implementations",
            "backlog_task_list", "backlog_task_search", "backlog_task_view",
            "backlog_document_list", "backlog_document_view", "backlog_document_search",
            "backlog_milestone_list",
            "run_security_scan", "run_gitleaks_scan", "run_opengrep_scan", "run_trivy_scan"
    );

    private final ToolProvider delegate;
    private final Project project;
    private final boolean autoApproveReadOnly;

    public AgentApprovalProvider(@NotNull ToolProvider delegate,
                                 @NotNull Project project,
                                 boolean autoApproveReadOnly) {
        this.delegate = delegate;
        this.project = project;
        this.autoApproveReadOnly = autoApproveReadOnly;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult result = delegate.provideTools(request);
        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (AiServiceTool tool : result.aiServiceTools()) {
            ToolExecutor original = tool.toolExecutor();

            // Implement both ToolExecutor methods. langchain4j invokes executeWithContext()
            // (see ToolService), and some executors — notably the langchain4j-skills
            // activate_skill executor — only implement executeWithContext() and throw
            // IllegalStateException("executeWithContext must be called instead") from
            // execute(). Forwarding the InvocationContext keeps those tools working and
            // preserves the original ToolExecutionResult (including attributes such as the
            // activated-skill marker). See issue #1040 regression.
            ToolExecutor approvalExecutor = new ToolExecutor() {
                @Override
                public String execute(ToolExecutionRequest toolRequest, Object memoryId) {
                    String denial = denialMessageOrNull(toolRequest);
                    if (denial != null) {
                        return denial;
                    }
                    return original.execute(toolRequest, memoryId);
                }

                @Override
                public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest,
                                                              InvocationContext context) {
                    String denial = denialMessageOrNull(toolRequest);
                    if (denial != null) {
                        return ToolExecutionResult.builder().resultText(denial).build();
                    }
                    return original.executeWithContext(toolRequest, context);
                }
            };

            builder.add(tool.toBuilder().toolExecutor(approvalExecutor).build());
        }

        return builder.build();
    }

    /**
     * Runs the approval gate for a single tool invocation.
     *
     * @return {@code null} when the tool may proceed, or the denial message to return to the
     *         LLM when the user declined.
     */
    @Nullable
    private String denialMessageOrNull(@NotNull ToolExecutionRequest toolRequest) {
        boolean isReadOnly = READ_ONLY_TOOLS.contains(toolRequest.name());
        boolean needsApproval = !(autoApproveReadOnly && isReadOnly);

        if (needsApproval) {
            boolean approved = AgentApprovalService.requestApproval(
                    project, toolRequest.name(), toolRequest.arguments());
            if (!approved) {
                log.debug("Agent tool execution denied: {}", toolRequest.name());
                return "Tool execution was denied by the user.";
            }
        }

        log.debug("Agent tool execution approved: {}", toolRequest.name());
        return null;
    }
}
