package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.MemoryException;
import com.devoxx.genie.service.skill.SkillRegistry;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.TemplateVariableEscaper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.model.Constant.MARKDOWN;

/**
 * Manages high-level chat memory operations and coordinates memory lifecycle.
 * This class is responsible for business logic around memory operations,
 * including system prompt handling, context preparation, and conversation management.
 */
@Slf4j
public class ChatMemoryManager {

    private final ChatMemoryService chatMemoryService;

    public static ChatMemoryManager getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryManager.class);
    }

    public ChatMemoryManager() {
        this.chatMemoryService = ChatMemoryService.getInstance();
    }

    /**
     * Initializes memory for a project with the configured size
     * @param project The project to initialize memory for
     */
    public void initializeMemory(@NotNull Project project) {
        initializeMemoryByKey(project.getLocationHash());
    }

    /**
     * Initializes memory for a composite key (e.g., projectHash-tabId) with the configured size
     * @param memoryKey The composite key
     */
    public void initializeMemoryByKey(@NotNull String memoryKey) {
        try {
            int chatMemorySize = DevoxxGenieStateService.getInstance().getChatMemorySize();
            if (chatMemorySize <= 0) {
                chatMemorySize = 10;
            }
            chatMemoryService.initializeByKey(memoryKey, chatMemorySize);
            log.debug("Chat memory initialized for key: {}", memoryKey);
        } catch (Exception e) {
            throw new MemoryException("Failed to initialize chat memory", e);
        }
    }

    /**
     * Prepares memory for a conversation context, adding system message if needed
     * @param context The chat message context to prepare memory for
     */
    public void prepareMemory(@NotNull ChatMessageContext context) {
        try {
            String memoryKey = context.getMemoryKey();

            // If memory isn't initialized yet, do it now
            if (!chatMemoryService.hasMemory(memoryKey)) {
                initializeMemoryByKey(memoryKey);
            }

            // Issue #1193: a tool loop that died between writing the AiMessage(tool_calls)
            // and its tool results (hallucinated tool name, round-trip limit, streaming
            // error) leaves a dangling tool_use tail that makes OpenAI-compatible providers
            // reject every subsequent request. Heal the conversation before each new prompt.
            sanitizeOrphanedToolMessages(memoryKey);

            if (chatMemoryService.isEmptyByKey(memoryKey)) {
                log.debug("Preparing memory with initial system message if needed");

                if (shouldIncludeSystemMessage(context)) {
                    String systemPrompt = buildSystemPrompt(context);
                    chatMemoryService.addMessageByKey(memoryKey, SystemMessage.from(systemPrompt));
                    log.debug("Added system message to memory");
                    publishSystemPromptToActivityLog(context.getProject(), systemPrompt);
                }
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to prepare memory", e);
        }
    }

    /**
     * Adds AI response to memory from the provided context
     * @param context The chat message context containing the AI message
     */
    public void addAiResponse(@NotNull ChatMessageContext context) {
        try {
            if (context.getAiMessage() != null) {
                log.debug("Adding AI response to memory for context ID: {}", context.getId());
                chatMemoryService.addMessageByKey(context.getMemoryKey(), context.getAiMessage());
                log.debug("Successfully added AI response to memory");
            } else {
                log.warn("Attempted to add null AI message to memory for context ID: {}", context.getId());
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to add AI response to memory", e);
        }
    }

    /**
     * Removes the most recent conversation exchange (user and AI messages) from memory
     * @param context The chat message context for the exchange to remove
     */
    public void removeLastExchange(@NotNull ChatMessageContext context) {
        try {
            List<ChatMessage> messagesToRemove = new ArrayList<>();

            if (context.getAiMessage() != null) {
                messagesToRemove.add(context.getAiMessage());
            }

            if (context.getUserMessage() != null) {
                messagesToRemove.add(context.getUserMessage());
            }

            if (!messagesToRemove.isEmpty()) {
                chatMemoryService.removeMessagesByKey(context.getMemoryKey(), messagesToRemove);
                log.debug("Removed last exchange from memory");
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to remove last exchange from memory", e);
        }
    }

    /**
     * Removes only the last user message from memory
     * @param context The chat message context containing the user message to remove
     */
    public void removeLastUserMessage(@NotNull ChatMessageContext context) {
        try {
            if (context.getUserMessage() != null) {
                chatMemoryService.removeMessagesByKey(
                        context.getMemoryKey(),
                        List.of(context.getUserMessage())
                );
                log.debug("Removed last user message from memory");
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to remove last user message from memory", e);
        }
    }

    /**
     * Drops a dangling tool_use left in memory when a tool loop is cancelled mid-flight.
     * Such a tail (AiMessage with unanswered tool requests, or orphan tool results) breaks
     * the Anthropic/OpenAI contract and fails every later request until restart.
     * No-op when the tail is valid.
     */
    public void sanitizeOrphanedToolMessages(@NotNull String memoryKey) {
        try {
            if (!chatMemoryService.hasMemory(memoryKey)) {
                return;
            }

            List<ChatMessage> messages = chatMemoryService.getMessagesByKey(memoryKey);
            if (messages.isEmpty()) {
                return;
            }

            List<ChatMessage> toRemove = new ArrayList<>();

            // collect trailing tool results
            int i = messages.size() - 1;
            java.util.Set<String> trailingResultIds = new java.util.HashSet<>();
            while (i >= 0 && messages.get(i) instanceof ToolExecutionResultMessage resultMessage) {
                trailingResultIds.add(resultMessage.id());
                i--;
            }

            // AiMessage requesting tools: valid only if every request has a matching result
            if (i >= 0 && messages.get(i) instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                boolean allAnswered = true;
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    if (!trailingResultIds.contains(request.id())) {
                        allAnswered = false;
                        break;
                    }
                }
                if (!allAnswered) {
                    // dangling tool_use: drop it and any partial results
                    toRemove.add(messages.get(i));
                    for (int j = i + 1; j < messages.size(); j++) {
                        toRemove.add(messages.get(j));
                    }
                }
            } else if (!trailingResultIds.isEmpty()) {
                // tool results without an originating tool_use: drop them
                for (int j = i + 1; j < messages.size(); j++) {
                    toRemove.add(messages.get(j));
                }
            }

            if (!toRemove.isEmpty()) {
                chatMemoryService.removeMessagesByKey(memoryKey, toRemove);
                log.info("Sanitized {} orphaned tool message(s) from memory key {} after cancellation",
                        toRemove.size(), memoryKey);
            }
        } catch (Exception e) {
            // best-effort: don't let cleanup mask the cancellation
            log.warn("Failed to sanitize orphaned tool messages for memory key {}", memoryKey, e);
        }
    }

    /**
     * Adds user message to memory from the provided context
     * @param context The chat message context containing the user message
     */
    public void addUserMessage(@NotNull ChatMessageContext context) {
        try {
            if (context.getUserMessage() == null && context.getUserPrompt() != null) {
                // Create user message if not already set
                context.setUserMessage(UserMessage.from(TemplateVariableEscaper.escape(context.getUserPrompt())));
            }

            if (context.getUserMessage() != null) {
                log.debug("Adding user message to memory for context ID: {}", context.getId());
                chatMemoryService.addMessageByKey(context.getMemoryKey(), buildEscapedUserMessage(context.getUserMessage()));
                log.debug("Successfully added user message to memory");
            } else {
                log.warn("Attempted to add null user message to memory for context ID: {}", context.getId());
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to add user message to memory", e);
        }
    }

    /**
     * Escapes template variables in a user message, preserving multimodal content.
     * @param userMessage The user message to escape
     * @return A new UserMessage with escaped text content
     */
    private @NonNull UserMessage buildEscapedUserMessage(@NotNull UserMessage userMessage) {
        if (!userMessage.hasSingleText()) {
            // Multimodal message (contains images) — preserve all content types
            List<Content> escapedContents = new ArrayList<>();
            for (Content content : userMessage.contents()) {
                if (content instanceof TextContent textContent) {
                    escapedContents.add(TextContent.from(TemplateVariableEscaper.escape(textContent.text())));
                } else {
                    escapedContents.add(content);
                }
            }
            return UserMessage.from(escapedContents);
        }
        return UserMessage.from(TemplateVariableEscaper.escape(userMessage.singleText()));
    }

    /**
     * Removes the last message from memory regardless of type
     * @param project The project to remove the last message from
     */
    public void removeLastMessage(@NotNull Project project) {
        removeLastMessageByKey(project.getLocationHash());
    }

    /**
     * Removes the last message from memory by key
     * @param memoryKey The memory key
     */
    public void removeLastMessageByKey(@NotNull String memoryKey) {
        try {
            chatMemoryService.removeLastMessageByKey(memoryKey);
            log.debug("Removed last message from memory");
        } catch (Exception e) {
            throw new MemoryException("Failed to remove last message from memory", e);
        }
    }

    /**
     * Gets all messages from memory for a project
     * @param project The project to get messages for
     * @return List of chat messages
     */
    public List<ChatMessage> getMessages(@NotNull Project project) {
        return getMessagesByKey(project.getLocationHash());
    }

    /**
     * Gets all messages from memory for a given key
     * @param memoryKey The memory key
     * @return List of chat messages
     */
    public List<ChatMessage> getMessagesByKey(@NotNull String memoryKey) {
        try {
            return chatMemoryService.getMessagesByKey(memoryKey);
        } catch (Exception e) {
            throw new MemoryException("Failed to get messages from memory", e);
        }
    }

    /**
     * Gets the chat memory instance for a project
     * @param projectHash The hash of the project to get memory for
     * @return ChatMemory instance
     */
    public ChatMemory getChatMemory(String projectHash) {
        try {
            return chatMemoryService.get(projectHash);
        } catch (Exception e) {
            throw new MemoryException("Failed to get chat memory", e);
        }
    }

    /**
     * Restores a conversation from a saved model
     * @param project The project to restore the conversation for
     * @param conversation The conversation to restore
     */
    public void restoreConversation(@NotNull Project project, @NotNull Conversation conversation) {
        restoreConversationByKey(project.getLocationHash(), conversation);
    }

    /**
     * Restores a conversation from a saved model into a specific memory key
     * @param memoryKey The memory key (e.g., projectHash-tabId)
     * @param conversation The conversation to restore
     */
    public void restoreConversationByKey(@NotNull String memoryKey, @NotNull Conversation conversation) {
        try {
            // Initialize (or re-initialize) memory for this key — ensures it exists even if
            // the tab was just opened or conversation is restored from history into a fresh session
            initializeMemoryByKey(memoryKey);

            for (com.devoxx.genie.model.conversation.ChatMessage message : conversation.getMessages()) {
                if (message.isUser()) {
                    chatMemoryService.addMessageByKey(memoryKey, UserMessage.from(message.getContent()));
                } else {
                    chatMemoryService.addMessageByKey(memoryKey, AiMessage.from(message.getContent()));
                }
            }
            log.debug("Restored conversation from saved model for key: {}", memoryKey);
        } catch (Exception e) {
            throw new MemoryException("Failed to restore conversation", e);
        }
    }

    /**
     * Removes a memory entry entirely (used for tab cleanup)
     * @param memoryKey The memory key to remove
     */
    public void removeMemory(@NotNull String memoryKey) {
        chatMemoryService.removeByKey(memoryKey);
    }

    /**
     * Determines if a system message should be included based on model type
     * @param context The context containing model information
     * @return true if system message should be included, false otherwise
     */
    private boolean shouldIncludeSystemMessage(@NotNull ChatMessageContext context) {
        LanguageModel model = context.getLanguageModel();
        // OpenAI o1 models do not support system messages
        return !ChatMessageContextUtil.isOpenAIo1Model(model);
    }

    /**
     * Builds system prompt with optional MCP instructions
     * @param context The context for building the system prompt
     * @return The complete system prompt
     */
    private String buildSystemPrompt(@NotNull ChatMessageContext context) {
        String prompt = buildAugmentedSystemPrompt(context.getProject());
        // task-209 analytics signal — mirrors the gate used inside buildAugmentedSystemPrompt.
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseDevoxxGenieMdInPrompt())) {
            String md = readDevoxxGenieMdFile(context.getProject());
            if (md != null && !md.isEmpty()) {
                context.setDevoxxGenieMdUsed(true);
            }
        }
        return prompt;
    }

    /**
     * Publishes the conversation's system prompt to the Agent/MCP log panel so users can
     * inspect the exact instructions the model received. Fired once per conversation, when
     * the system message is first added to memory. Published unconditionally (not gated by
     * the agent-debug-logs setting) since it's a one-time informational entry that answers
     * "what system prompt is this chat actually using?".
     *
     * @param project the project that owns the conversation
     * @param systemPrompt the fully built system prompt added to memory
     */
    private void publishSystemPromptToActivityLog(@NotNull Project project, @NotNull String systemPrompt) {
        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(AgentType.SYSTEM_PROMPT)
                    .result(systemPrompt)
                    .projectLocationHash(project.getLocationHash())
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish system prompt to activity log", e);
        }
    }

    public static @NotNull String buildAugmentedSystemPrompt(@NotNull Project project) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String systemPrompt = state.getSystemPrompt() + MARKDOWN;
        String projectPath = project.getBasePath();

        // Always tell the LLM the project root when tools are active
        if (Boolean.TRUE.equals(state.getAgentModeEnabled())) {
            systemPrompt += "\n<PROJECT_ROOT>" + projectPath + "</PROJECT_ROOT>" +
                    "\nAll file paths in tool calls are relative to this project root directory.\n";
        }

        // Add test execution instruction if enabled
        if (Boolean.TRUE.equals(state.getTestExecutionEnabled())) {
            systemPrompt += """
                    <TESTING_INSTRUCTION>
                    After modifying code using write_file or edit_file, run relevant tests
                    using the run_tests tool to verify your changes. If tests fail, analyze
                    the failures, fix the code, and re-run tests until they pass.
                    </TESTING_INSTRUCTION>
                    """;
        }

        // Add MCP instructions to system prompt if MCP is enabled
        if (MCPService.isMCPEnabled()) {
            systemPrompt += "<MCP_INSTRUCTION>The project base directory is " +
                    projectPath +
                    "\nMake sure to use this information for your MCP tooling calls\n" +
                    "</MCP_INSTRUCTION>";
            MCPService.logDebug("Added MCP instructions to system prompt");
        }

        // Add RAG/semantic_search instruction when both agent mode and RAG are active.
        // Small models reliably pick `search_files` (regex grep) for conceptual queries
        // unless they're told the project has a semantic index; tool descriptions alone
        // aren't enough signal. This mirrors the <TESTING_INSTRUCTION> / <MCP_INSTRUCTION>
        // pattern used for other tools that need an explicit nudge.
        if (Boolean.TRUE.equals(state.getAgentModeEnabled())
                && Boolean.TRUE.equals(state.getRagEnabled())) {
            systemPrompt += """
                    <RAG_INSTRUCTION>
                    This project has a semantic vector index of its content. For any user
                    question about what the project content discusses, mentions, covers, or
                    explains — for example "which slides discuss X", "where do we explain Y",
                    "find anything about Z" — call the `semantic_search` tool FIRST with a
                    natural-language query. Only fall back to `search_files` (regex grep)
                    when you need to locate a known exact string, or when `semantic_search`
                    returns no useful hits.
                    </RAG_INSTRUCTION>
                    """;
        }

        systemPrompt += getDevoxxGenieMdSection(project, state);
        systemPrompt += getClaudeOrAgentsMdSection(project, state);
        systemPrompt += getSkillsSection(project, state);

        return TemplateVariableEscaper.escape(systemPrompt);
    }

    private static String getDevoxxGenieMdSection(@NotNull Project project, @NotNull DevoxxGenieStateService state) {
        if (!Boolean.TRUE.equals(state.getUseDevoxxGenieMdInPrompt())) {
            return "";
        }
        String content = readDevoxxGenieMdFile(project);
        return (content != null && !content.isEmpty()) ? "\n<ProjectContext>\n" + content + "\n</ProjectContext>\n" : "";
    }

    private static String getClaudeOrAgentsMdSection(@NotNull Project project, @NotNull DevoxxGenieStateService state) {
        if (!Boolean.TRUE.equals(state.getUseClaudeOrAgentsMdInPrompt())) {
            return "";
        }
        String content = readClaudeOrAgentsMdFile(project);
        return (content != null && !content.isEmpty()) ? "\n<ProjectContext>\n" + content + "\n</ProjectContext>\n" : "";
    }

    // Append langchain4j Skills system-prompt fragment (issue #1040). Only when agent
    // mode is enabled — skills are wired into the agent tool chain, so listing them
    // outside agent mode would be misleading.
    private static String getSkillsSection(@NotNull Project project, @NotNull DevoxxGenieStateService state) {
        if (!Boolean.TRUE.equals(state.getAgentModeEnabled()) || project.isDefault()) {
            return "";
        }
        try {
            String fragment = SkillRegistry.getInstance(project).getSystemPromptFragment();
            if (!fragment.isEmpty()) {
                return "\nYou have access to the following skills:\n"
                        + fragment
                        + "\nWhen the user's request relates to one of these skills, activate it first using the `"
                        + SkillRegistry.ACTIVATE_SKILL_TOOL_NAME
                        + "` tool before proceeding.\n";
            }
        } catch (Exception e) {
            log.warn("Failed to append Skills system-prompt fragment", e);
        }
        return "";
    }

    /**
     * Read the content of DEVOXXGENIE.md file from the project root directory.
     *
     * @param project the project
     * @return the content of DEVOXXGENIE.md file or null if file not found or can't be read
     */
    @Nullable
    static String readDevoxxGenieMdFile(Project project) {
        try {
            if (project == null || project.getBasePath() == null) {
                log.warn("Project or base path is null");
                return null;
            }

            Path devoxxGenieMdPath = Paths.get(project.getBasePath(), "DEVOXXGENIE.md");
            if (!Files.exists(devoxxGenieMdPath)) {
                log.debug("DEVOXXGENIE.md file not found in project root: {}", devoxxGenieMdPath);
                return null;
            }

            return Files.readString(devoxxGenieMdPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read DEVOXXGENIE.md file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Read the content of CLAUDE.md or AGENTS.md file from the project root directory.
     * CLAUDE.md has priority - if both files exist, only CLAUDE.md is read and AGENTS.md is skipped.
     *
     * @param project the project
     * @return the content of CLAUDE.md or AGENTS.md file or null if neither file is found or can't be read
     */
    @Nullable
    static String readClaudeOrAgentsMdFile(Project project) {
        try {
            if (project == null || project.getBasePath() == null) {
                log.warn("Project or base path is null");
                return null;
            }

            // Try CLAUDE.md first (priority)
            Path claudeMdPath = Paths.get(project.getBasePath(), "CLAUDE.md");
            if (Files.exists(claudeMdPath)) {
                log.debug("Found CLAUDE.md file in project root, using it (AGENTS.md will be skipped if present)");
                return Files.readString(claudeMdPath, StandardCharsets.UTF_8);
            }

            // If CLAUDE.md doesn't exist, try AGENTS.md
            Path agentsMdPath = Paths.get(project.getBasePath(), "AGENTS.md");
            if (Files.exists(agentsMdPath)) {
                log.debug("Found AGENTS.md file in project root");
                return Files.readString(agentsMdPath, StandardCharsets.UTF_8);
            }

            log.debug("Neither CLAUDE.md nor AGENTS.md file found in project root");
            return null;
        } catch (IOException e) {
            log.warn("Failed to read CLAUDE.md or AGENTS.md file: {}", e.getMessage());
            return null;
        }
    }
}
