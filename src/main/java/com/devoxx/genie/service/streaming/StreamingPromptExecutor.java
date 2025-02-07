package com.devoxx.genie.service.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.ChatMemoryService;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StreamingPromptExecutor {

    private final ChatMemoryService chatMemoryService;
    private final MessageCreationService messageCreationService;
    private StreamingResponseHandler currentStreamingHandler;

    public StreamingPromptExecutor() {
        this.chatMemoryService = ChatMemoryService.getInstance();
        this.messageCreationService = MessageCreationService.getInstance();
    }

    /**
     * Execute the streaming prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the enable buttons
     */
    public void execute(@NotNull ChatMessageContext chatMessageContext,
                        PromptOutputPanel promptOutputPanel,
                        Runnable enableButtons) {
        StreamingChatLanguageModel streamingChatLanguageModel = chatMessageContext.getStreamingChatLanguageModel();
        if (streamingChatLanguageModel == null) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                "Streaming model not available, please select another provider or turn off streaming mode.");
            enableButtons.run();
            return;
        }

        prepareMemory(chatMessageContext);
        promptOutputPanel.addUserPrompt(chatMessageContext);

        currentStreamingHandler = new StreamingResponseHandler(chatMessageContext, promptOutputPanel, enableButtons);

        List<ChatMessage> messages = chatMemoryService.messages(chatMessageContext.getProject());
        streamingChatLanguageModel.chat(messages, currentStreamingHandler);
    }

    /**
     * Prepare memory.
     *
     * @param chatMessageContext the chat message context
     */
    private void prepareMemory(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMemoryService.isEmpty(chatMessageContext.getProject())) {
            chatMemoryService.add(
                chatMessageContext.getProject(),
                new SystemMessage(DevoxxGenieStateService.getInstance().getSystemPrompt())
            );
        }

        messageCreationService.addUserMessageToContext(chatMessageContext);
        chatMemoryService.add(chatMessageContext.getProject(), chatMessageContext.getUserMessage());
    }

    /**
     * Stop streaming.
     */
    public void stopStreaming() {
        if (currentStreamingHandler != null) {
            currentStreamingHandler.stop();
            currentStreamingHandler = null;
        }
    }
}
