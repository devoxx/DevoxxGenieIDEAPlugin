package com.devoxx.genie.service.mcp;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

public class ApprovalRequiredToolProvider implements ToolProvider {

    private final ToolProvider delegate;
    private final Project project;
    private final ApprovalChecker approvalChecker;

    /**
     * Functional interface for checking tool execution approval.
     */
    @FunctionalInterface
    interface ApprovalChecker {
        boolean requestApproval(@Nullable Project project, @NotNull String toolName, @NotNull String arguments);
    }

    public ApprovalRequiredToolProvider(ToolProvider delegate, Project project) {
        this(delegate, project, MCPApprovalService::requestApproval);
    }

    ApprovalRequiredToolProvider(ToolProvider delegate, @Nullable Project project, ApprovalChecker approvalChecker) {
        this.delegate = delegate;
        this.project = project;
        this.approvalChecker = approvalChecker;
    }

    @Override
    public ToolProviderResult provideTools(@NotNull ToolProviderRequest request) {
        ToolProviderResult delegateResult = delegate.provideTools(request);

        ToolProviderResult.Builder builder = ToolProviderResult.builder();

        for (var entry : delegateResult.tools().entrySet()) {
            ToolSpecification spec = entry.getKey();
            ToolExecutor originalExecutor = entry.getValue();

            // Wrap the original executor
            ToolExecutor approvalExecutor = (toolExecutionRequest, memoryId) -> {
                boolean approved = approvalChecker.requestApproval(
                        project,
                        toolExecutionRequest.name(),
                        toolExecutionRequest.arguments()
                );
                if (approved) {
                    MCPService.logDebug("MCP tool execution approved: " + toolExecutionRequest.name());
                    return originalExecutor.execute(toolExecutionRequest, memoryId);
                } else {
                    MCPService.logDebug("MCP tool execution denied: " + toolExecutionRequest.name());
                    return "Tool execution was denied by the user.";
                }
            };

            builder.add(spec, approvalExecutor);
        }

        return builder.build();
    }
}
