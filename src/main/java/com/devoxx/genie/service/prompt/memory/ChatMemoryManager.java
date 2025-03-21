package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.model.Constant.MARKDOWN;

/**
 * Central orchestrator for all chat memory operations.
 * Coordinates memory initialization, maintenance, and interactions with the chat memory store.
 */
@Slf4j
public class ChatMemoryManager {

    private final ChatMemoryService chatMemoryService;
    private final MessageCreationService messageCreationService;

    public static ChatMemoryManager getInstance() {
        return ApplicationManager.getApplication().getService(ChatMemoryManager.class);
    }

    public ChatMemoryManager() {
        this.chatMemoryService = ChatMemoryService.getInstance();
        this.messageCreationService = MessageCreationService.getInstance();
    }

    /**
     * Initialize the chat memory for a project.
     * Should be called when starting a new conversation or session.
     *
     * @param project The project to initialize memory for
     */
    public void initializeMemory(@NotNull Project project) {
        int chatMemorySize = DevoxxGenieStateService.getInstance().getChatMemorySize();
        chatMemoryService.initialize(project, chatMemorySize);
        log.debug("Chat memory initialized for project: " + project.getLocationHash());
    }

    /**
     * Prepare the memory for a new conversation.
     * Adds system message if needed based on the language model.
     *
     * @param chatMessageContext The context for the chat message
     */
    public void prepareMemory(@NotNull ChatMessageContext chatMessageContext) {
        Project project = chatMessageContext.getProject();
        
        // If memory isn't initialized yet, do it now
        if (chatMemoryService.isEmpty(project)) {
            log.debug("Preparing memory with initial system message if needed");

            if (shouldIncludeSystemMessage(chatMessageContext)) {
                String systemPrompt = buildSystemPrompt(chatMessageContext);
                chatMemoryService.add(project, SystemMessage.from(systemPrompt));
                log.debug("Added system message to memory");
            }
        }
    }

    /**
     * Add a user message to the chat memory.
     *
     * @param chatMessageContext The context containing the user message
     */
    public void addUserMessage(@NotNull ChatMessageContext chatMessageContext) {
        messageCreationService.addUserMessageToContext(chatMessageContext);
        Project project = chatMessageContext.getProject();
        chatMemoryService.add(project, chatMessageContext.getUserMessage());
        log.debug("Added user message to memory");
    }

    /**
     * Add an AI response to the chat memory.
     *
     * @param chatMessageContext The context containing the AI response
     */
    public void addAiResponse(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMessageContext.getAiMessage() != null) {
            Project project = chatMessageContext.getProject();
            chatMemoryService.add(project, chatMessageContext.getAiMessage());
            log.debug("Added AI response to memory");
        }
    }

    /**
     * Remove the last exchange (user question and AI response) from memory.
     * Useful in case of errors or cancellations.
     *
     * @param chatMessageContext The context containing the exchange to remove
     */
    public void removeLastExchange(@NotNull ChatMessageContext chatMessageContext) {
        Project project = chatMessageContext.getProject();
        List<ChatMessage> messagesToRemove = new ArrayList<>();
        
        if (chatMessageContext.getAiMessage() != null) {
            messagesToRemove.add(chatMessageContext.getAiMessage());
        }
        
        if (chatMessageContext.getUserMessage() != null) {
            messagesToRemove.add(chatMessageContext.getUserMessage());
        }
        
        chatMemoryService.removeMessages(project, messagesToRemove);
        log.debug("Removed last exchange from memory");
    }

    /**
     * Remove the last message from memory.
     *
     * @param project The project to remove the message from
     */
    public void removeLastMessage(@NotNull Project project) {
        chatMemoryService.removeLastMessage(project);
        log.debug("Removed last message from memory");
    }

    /**
     * Clear all messages from memory for a project.
     *
     * @param project The project to clear memory for
     */
    public void clearMemory(@NotNull Project project) {
        chatMemoryService.clear(project);
        log.debug("Cleared all messages from memory for project: " + project.getLocationHash());
    }

    /**
     * Get all messages from memory for a project.
     *
     * @param project The project to get messages for
     * @return List of chat messages
     */
    public List<ChatMessage> getMessages(@NotNull Project project) {
        return chatMemoryService.getMessages(project);
    }

    /**
     * Check if memory is empty for a project.
     *
     * @param project The project to check
     * @return True if memory is empty
     */
    public boolean isMemoryEmpty(@NotNull Project project) {
        return chatMemoryService.isEmpty(project);
    }

    /**
     * Get the ChatMemory object for a project.
     *
     * @param projectHash The project hash to get memory for
     * @return The ChatMemory object
     */
    public ChatMemory getChatMemory(String projectHash) {
        return chatMemoryService.get(projectHash);
    }

    /**
     * Restore a conversation from a saved model.
     *
     * @param project The project to restore the conversation for
     * @param conversation The conversation to restore
     */
    public void restoreConversation(@NotNull Project project, @NotNull Conversation conversation) {
        chatMemoryService.restoreConversation(project, conversation);
        log.debug("Restored conversation from saved model");
    }

    /**
     * Determine if a system message should be included based on the model.
     *
     * @param chatMessageContext The chat message context
     * @return True if a system message should be included
     */
    private boolean shouldIncludeSystemMessage(@NotNull ChatMessageContext chatMessageContext) {
        LanguageModel model = chatMessageContext.getLanguageModel();
        
        // If the language model is OpenAI o1 model, do not include system message
        if (ChatMessageContextUtil.isOpenAIo1Model(model)) {
            return false;
        }

        // Check for Bedrock Mistral AI model
        if (chatMessageContext.getChatLanguageModel() instanceof BedrockChatModel bedrockChatModel) {
            // TODO Test if this refactoring still works because BedrockMistralChatModel is deprecated
            return bedrockChatModel.provider().name().startsWith("mistral.");
        }

        return true;
    }

    /**
     * Build the system prompt with appropriate instructions.
     *
     * @param chatMessageContext The chat message context
     * @return The complete system prompt
     */
    private String buildSystemPrompt(@NotNull ChatMessageContext chatMessageContext) {
        String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt() + MARKDOWN;

        // Add MCP instructions to system prompt if MCP is enabled
        if (MCPService.isMCPEnabled()) {
            systemPrompt += "<MCP_INSTRUCTION>The project base directory is " + 
                            chatMessageContext.getProject().getBasePath() +
                            "\nMake sure to use this information for your MCP tooling calls\n" +
                            "</MCP_INSTRUCTION>";
            MCPService.logDebug("Added MCP instructions to system prompt");
        }
        
        return systemPrompt;
    }
}
