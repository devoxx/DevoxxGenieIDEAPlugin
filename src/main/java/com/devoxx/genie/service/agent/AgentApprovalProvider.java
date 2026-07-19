package com.devoxx.genie.service.agent;

import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.devoxx.genie.model.Constant.COMMAND_BLACKLIST_ACTION_BLOCK;

/**
 * Wraps tools with conditional approval logic.
 * Read-only tools (read_file, list_files, search_files, fetch_page) can be auto-approved.
 * Write tools (write_file, run_command) always require approval.
 * run_command is additionally checked against the user's command blacklist (issue #1209),
 * which either blocks matching commands or forces the approval dialog even when write
 * approvals are auto-approved.
 */
@Slf4j
public class AgentApprovalProvider implements ToolProvider {

    private static final String DENIED_BY_USER_MESSAGE = "Tool execution was denied by the user.";

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
                    String denial = denialMessage(toolRequest);
                    if (denial != null) {
                        return denial;
                    }
                    return original.execute(toolRequest, memoryId);
                }

                @Override
                public ToolExecutionResult executeWithContext(ToolExecutionRequest toolRequest, InvocationContext context) {
                    String denial = denialMessage(toolRequest);
                    if (denial != null) {
                        return ToolExecutionResult.builder()
                                .resultText(denial)
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
     * Determines whether the given tool request must be denied. run_command is first
     * checked against the user's command blacklist (issue #1209): a match either blocks
     * the command outright or forces the approval dialog even when write approvals are
     * auto-approved. Read-only tools may be auto-approved; everything else requires
     * explicit user approval. Returns the message to hand back to the LLM when denied,
     * or {@code null} when execution may proceed.
     */
    private @Nullable String denialMessage(@NotNull ToolExecutionRequest toolRequest) {
        String blacklistedPattern = null;
        if ("run_command".equals(toolRequest.name())) {
            DevoxxGenieStateService state = safeStateService();
            if (state != null) {
                String command = ToolArgumentParser.getString(toolRequest.arguments(), "command");
                blacklistedPattern = CommandBlacklist.findMatch(command, state.getAgentCommandBlacklist())
                        .orElse(null);
                if (blacklistedPattern != null
                        && COMMAND_BLACKLIST_ACTION_BLOCK.equals(state.getAgentCommandBlacklistAction())) {
                    log.info("Agent run_command blocked by blacklist pattern '{}': {}", blacklistedPattern, command);
                    return "Error: Command blocked by user policy — it matches the blacklisted pattern '"
                            + blacklistedPattern + "' (Settings → Agent → Approval). Do not retry this "
                            + "command or variations of it; ask the user to run it manually if it is really needed.";
                }
            }
        }

        boolean isReadOnly = READ_ONLY_TOOLS.contains(toolRequest.name());
        boolean needsApproval = blacklistedPattern != null || !(autoApproveReadOnly && isReadOnly);

        if (needsApproval) {
            boolean approved = blacklistedPattern != null
                    ? AgentApprovalService.requestApproval(
                            project, toolRequest.name(), toolRequest.arguments(), blacklistedPattern)
                    : AgentApprovalService.requestApproval(
                            project, toolRequest.name(), toolRequest.arguments());
            if (!approved) {
                log.debug("Agent tool execution denied: {}", toolRequest.name());
                return DENIED_BY_USER_MESSAGE;
            }
        }

        log.debug("Agent tool execution approved: {}", toolRequest.name());
        return null;
    }

    /**
     * The blacklist is a safety net, not a critical path: if settings cannot be read
     * (e.g. very early startup or headless tests without the service registered), fall
     * back to the normal approval flow instead of failing the tool call.
     */
    private @Nullable DevoxxGenieStateService safeStateService() {
        try {
            return DevoxxGenieStateService.getInstance();
        } catch (Exception e) {
            log.debug("Command blacklist check skipped: settings unavailable", e);
            return null;
        }
    }
}
