package com.devoxx.genie.service.agent;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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
            ToolSpecification spec = tool.toolSpecification();
            ToolExecutor original = tool.toolExecutor();

            // Override executeWithContext (not just legacy execute) so skill-backed tools
            // (e.g. activate_skill from langchain4j-skills, which throw from execute() with
            // "executeWithContext must be called instead") keep working through this wrapper.
            ToolExecutor approvalExecutor = new ToolExecutor() {
                @Override
                public String execute(ToolExecutionRequest toolRequest, Object memoryId) {
                    if (isDenied(toolRequest)) {
                        return "Tool execution was denied by the user.";
                    }
                    return original.execute(toolRequest, memoryId);
                }

                @Override
                public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest, InvocationContext context) {
                    if (isDenied(toolRequest)) {
                        return ToolExecutionResult.builder()
                                .resultText("Tool execution was denied by the user.")
                                .build();
                    }
                    return original.executeWithContext(toolRequest, context);
                }
            };

            builder.add(tool.toBuilder().toolExecutor(approvalExecutor).build());
        }

        return builder.build();
    }

    /**
     * Determines whether the given tool request must be denied. Read-only tools may be
     * auto-approved; everything else requires explicit user approval. Returns {@code true}
     * when the user denied the request (so the caller should short-circuit).
     */
    private boolean isDenied(@NotNull ToolExecutionRequest toolRequest) {
        boolean isReadOnly = READ_ONLY_TOOLS.contains(toolRequest.name());
        boolean needsApproval = !(autoApproveReadOnly && isReadOnly);

        if (needsApproval) {
            boolean approved = AgentApprovalService.requestApproval(
                    project, toolRequest.name(), toolRequest.arguments());
            if (!approved) {
                log.debug("Agent tool execution denied: {}", toolRequest.name());
                return true;
            }
        }

        log.debug("Agent tool execution approved: {}", toolRequest.name());
        return false;
    }
}
