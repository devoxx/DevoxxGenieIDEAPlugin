package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

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
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the enable buttons
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
        streamingChatLanguageModel.generate(chatMemoryService.messages(chatMessageContext.getProject()), currentStreamingHandler);
    }

    /**
     * Prepare memory.
     * @param chatMessageContext the chat message context
     */
    private void prepareMemory(@NotNull ChatMessageContext chatMessageContext) {
        if (chatMemoryService.isEmpty(chatMessageContext.getProject())) {
            chatMemoryService.add(
                chatMessageContext.getProject(),
                new SystemMessage(DevoxxGenieSettingsServiceProvider.getInstance().getSystemPrompt())
            );
        }

        UserMessage userMessage = messageCreationService.createUserMessage(chatMessageContext);
        chatMemoryService.add(chatMessageContext.getProject(), userMessage);
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
