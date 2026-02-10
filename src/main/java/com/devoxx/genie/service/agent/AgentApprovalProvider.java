package com.devoxx.genie.service.agent;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Wraps tools with conditional approval logic.
 * Read-only tools (read_file, list_files, search_files) can be auto-approved.
 * Write tools (write_file, run_command) always require approval.
 */
@Slf4j
public class AgentApprovalProvider implements ToolProvider {

    private static final Set<String> READ_ONLY_TOOLS = Set.of(
            "read_file", "list_files", "search_files",
            "backlog_task_list", "backlog_task_search", "backlog_task_view",
            "backlog_document_list", "backlog_document_view", "backlog_document_search",
            "backlog_milestone_list"
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

        for (var entry : result.tools().entrySet()) {
            ToolSpecification spec = entry.getKey();
            ToolExecutor original = entry.getValue();

            ToolExecutor approvalExecutor = (toolRequest, memoryId) -> {
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
                return original.execute(toolRequest, memoryId);
            };

            builder.add(spec, approvalExecutor);
        }

        return builder.build();
    }
}
