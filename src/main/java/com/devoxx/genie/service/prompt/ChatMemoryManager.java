package com.devoxx.genie.service.prompt;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import org.jetbrains.annotations.NotNull;

import static com.devoxx.genie.model.Constant.MARKDOWN;

/**
 * Manager for consistent chat memory operations across different prompt execution strategies.
 */
public class ChatMemoryManager {
    private static final Logger LOG = Logger.getInstance(ChatMemoryManager.class);

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
     * Prepares the memory for a new conversation if needed by adding system message.
     *
     * @param chatMessageContext The context of the chat message
     */
    public void prepareMemory(@NotNull ChatMessageContext chatMessageContext) {
        Project project = chatMessageContext.getProject();
        
        // Add System Message if ChatMemoryService is empty
        if (chatMemoryService.isEmpty(project)) {
            LOG.debug("ChatMemoryService is empty, adding a new SystemMessage");

            if (includeSystemMessage(chatMessageContext)) {
                String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt() + MARKDOWN;

                // Add MCP instructions to system prompt if needed
                appendMcpInstructionsIfRequired(chatMessageContext, systemPrompt);
                
                chatMemoryService.add(project, SystemMessage.from(systemPrompt));
            }
        }
    }

    /**
     * Adds the user message to the chat memory.
     *
     * @param chatMessageContext The context of the chat message
     */
    public void addUserMessage(@NotNull ChatMessageContext chatMessageContext) {
        messageCreationService.addUserMessageToContext(chatMessageContext);
        Project project = chatMessageContext.getProject();
        chatMemoryService.add(project, chatMessageContext.getUserMessage());
    }

    /**
     * Adds the AI response to the chat memory.
     *
     * @param chatMessageContext The context of the chat message containing the AI response
     */
    public void addAiResponse(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMessageContext.getAiMessage() != null) {
            Project project = chatMessageContext.getProject();
            chatMemoryService.add(project, chatMessageContext.getAiMessage());
        }
    }

//    /**
//     * Remove the last exchange from memory in case of an error or cancellation.
//     *
//     * @param chatMessageContext The context to remove
//     */
//    public void removeLastExchange(@NotNull ChatMessageContext chatMessageContext) {
//        Project project = chatMessageContext.getProject();
//        chatMemoryService.remove(chatMessageContext);
//    }

    private boolean includeSystemMessage(@NotNull ChatMessageContext chatMessageContext) {
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

    private void appendMcpInstructionsIfRequired(@NotNull ChatMessageContext chatMessageContext, String systemPrompt) {
        // Add MCP instructions logic can be moved here from the NonStreamingPromptExecutionService
        // This is a placeholder for the implementation
    }
}
