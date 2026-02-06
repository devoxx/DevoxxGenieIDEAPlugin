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
                UserMessage userMessage = context.getUserMessage();

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
                    chatMemoryService.addMessage(context.getProject(), UserMessage.from(escapedContents));
                } else {
                    String cleanValue = TemplateVariableEscaper.escape(userMessage.singleText());
                    chatMemoryService.addMessage(context.getProject(), UserMessage.from(cleanValue));
                }

                log.debug("Successfully added user message to memory");
            } else {
                log.warn("Attempted to add null user message to memory for context ID: {}", context.getId());
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to add user message to memory", e);
        }
    }

    /**
     * Removes only the last AI message from memory
     * @param context The chat message context containing the AI message to remove
     */
    public void removeLastAIMessage(@NotNull ChatMessageContext context) {
        try {
            if (context.getAiMessage() != null) {
                chatMemoryService.removeMessages(
                        context.getProject(),
                        List.of(context.getAiMessage())
                );
                log.debug("Removed last AI message from memory");
            }
        } catch (Exception e) {
            throw new MemoryException("Failed to remove last AI message from memory", e);
        }
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
        if (ChatMessageContextUtil.isOpenAIo1Model(model)) {
            return false;
        }

        // Check for Bedrock Mistral AI model
        //        if (context.getChatLanguageModel() instanceof BedrockChatModel bedrockChatModel) {
        //            // TODO Test if this refactoring still works because BedrockMistralChatModel is deprecated
        //            return bedrockChatModel.provider().name().startsWith("mistral.");
        //        }

        return true;
    }

    /**
     * Builds system prompt with optional MCP instructions
     * @param context The context for building the system prompt
     * @return The complete system prompt
     */
    private String buildSystemPrompt(@NotNull ChatMessageContext context) {
        String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt() + MARKDOWN;

        // Add MCP instructions to system prompt if MCP is enabled
        if (MCPService.isMCPEnabled()) {
            systemPrompt += "<MCP_INSTRUCTION>The project base directory is " +
                    context.getProject().getBasePath() +
                    "\nMake sure to use this information for your MCP tooling calls\n" +
                    "</MCP_INSTRUCTION>";
            MCPService.logDebug("Added MCP instructions to system prompt");
        }

        return systemPrompt;
    }
}