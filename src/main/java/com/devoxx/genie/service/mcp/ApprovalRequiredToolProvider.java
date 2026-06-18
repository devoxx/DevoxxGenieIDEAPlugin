package com.devoxx.genie.service.mcp;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutionResult;
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

        for (AiServiceTool tool : delegateResult.aiServiceTools()) {
            ToolSpecification spec = tool.toolSpecification();
            ToolExecutor originalExecutor = tool.toolExecutor();

            // Wrap the original executor. We override executeWithContext (not just the
            // legacy execute) so tools that require the new contract — e.g. skill-backed
            // tools that throw "executeWithContext must be called instead" from execute() —
            // continue to work through this approval wrapper.
            ToolExecutor approvalExecutor = new ToolExecutor() {
                @Override
                public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
                    if (isApproved(toolExecutionRequest)) {
                        return originalExecutor.execute(toolExecutionRequest, memoryId);
                    }
                    return "Tool execution was denied by the user.";
                }

                @Override
                public ToolExecutionResult executeWithContext(ToolExecutionRequest toolExecutionRequest, InvocationContext context) {
                    if (isApproved(toolExecutionRequest)) {
                        return originalExecutor.executeWithContext(toolExecutionRequest, context);
                    }
                    return ToolExecutionResult.builder()
                            .resultText("Tool execution was denied by the user.")
                            .build();
                }
            };

            builder.add(tool.toBuilder().toolExecutor(approvalExecutor).build());
        }

        return builder.build();
    }

    /**
     * Asks the user (via {@link ApprovalChecker}) whether the given MCP tool call may run.
     * Logs the outcome and returns {@code true} when approved.
     */
    private boolean isApproved(@NotNull ToolExecutionRequest toolExecutionRequest) {
        boolean approved = approvalChecker.requestApproval(
                project,
                toolExecutionRequest.name(),
                toolExecutionRequest.arguments()
        );
        if (approved) {
            MCPService.logDebug("MCP tool execution approved: " + toolExecutionRequest.name());
        } else {
            MCPService.logDebug("MCP tool execution denied: " + toolExecutionRequest.name());
        }
        return approved;
    }
}
