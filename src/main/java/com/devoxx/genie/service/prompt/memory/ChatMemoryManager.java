package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.MemoryException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.TemplateVariableEscaper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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
        try {
            int chatMemorySize = DevoxxGenieStateService.getInstance().getChatMemorySize();
            if (chatMemorySize <= 0) {
                chatMemorySize = 10;
            }
            chatMemoryService.initialize(project, chatMemorySize);
            log.debug("Chat memory initialized for project: {}", project.getLocationHash());
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
            Project project = context.getProject();

            // If memory isn't initialized yet, do it now
            if (chatMemoryService.isEmpty(project)) {
                log.debug("Preparing memory with initial system message if needed");

                if (shouldIncludeSystemMessage(context)) {
                    String systemPrompt = buildSystemPrompt(context);
                    chatMemoryService.addMessage(project, SystemMessage.from(systemPrompt));
                    log.debug("Added system message to memory");
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
                chatMemoryService.addMessage(context.getProject(), context.getAiMessage());
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
                chatMemoryService.removeMessages(context.getProject(), messagesToRemove);
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
                chatMemoryService.removeMessages(
                        context.getProject(),
                        List.of(context.getUserMessage())
                );
                log.debug("Removed last user message from memory");
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to remove last user message from memory", e);
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
                chatMemoryService.addMessage(context.getProject(), buildEscapedUserMessage(context.getUserMessage()));
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
        try {
            chatMemoryService.removeLastMessage(project);
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
        try {
            return chatMemoryService.getMessages(project);
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
        try {
            // First clear existing memory
            chatMemoryService.clearMemory(project);

            // Convert and add each message
            for (com.devoxx.genie.model.conversation.ChatMessage message : conversation.getMessages()) {
                if (message.isUser()) {
                    chatMemoryService.addMessage(project, UserMessage.from(message.getContent()));
                } else {
                    chatMemoryService.addMessage(project, AiMessage.from(message.getContent()));
                }
            }
            log.debug("Restored conversation from saved model");
        } catch (Exception e) {
            throw new MemoryException("Failed to restore conversation", e);
        }
    }

    /**
     * Determines if a system message should be included based on model type
     * @param context The context containing model information
     * @return true if system message should be included, false otherwise
     */
    private boolean shouldIncludeSystemMessage(@NotNull ChatMessageContext context) {
        LanguageModel model = context.getLanguageModel();
        // If the language model is OpenAI o1 model, do not include system message
        return ChatMessageContextUtil.isOpenAIo1Model(model);
    }

    /**
     * Builds system prompt with optional MCP instructions
     * @param context The context for building the system prompt
     * @return The complete system prompt
     */
    private String buildSystemPrompt(@NotNull ChatMessageContext context) {
        String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt() + MARKDOWN;
        String projectPath = context.getProject().getBasePath();

        // Always tell the LLM the project root when tools are active
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled())) {
            systemPrompt += "\n<PROJECT_ROOT>" + projectPath + "</PROJECT_ROOT>" +
                    "\nAll file paths in tool calls are relative to this project root directory.\n";
        }

        // Add test execution instruction if enabled
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getTestExecutionEnabled())) {
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

        // Add DEVOXXGENIE.md content to system prompt (once per conversation)
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseDevoxxGenieMdInPrompt())) {
            String devoxxGenieMdContent = readDevoxxGenieMdFile(context.getProject());
            if (devoxxGenieMdContent != null && !devoxxGenieMdContent.isEmpty()) {
                systemPrompt += "\n<ProjectContext>\n" + devoxxGenieMdContent + "\n</ProjectContext>\n";
            }
        }

        // Add CLAUDE.md or AGENTS.md content to system prompt (once per conversation)
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseClaudeOrAgentsMdInPrompt())) {
            String claudeOrAgentsMdContent = readClaudeOrAgentsMdFile(context.getProject());
            if (claudeOrAgentsMdContent != null && !claudeOrAgentsMdContent.isEmpty()) {
                systemPrompt += "\n<ProjectContext>\n" + claudeOrAgentsMdContent + "\n</ProjectContext>\n";
            }
        }

        return TemplateVariableEscaper.escape(systemPrompt);
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