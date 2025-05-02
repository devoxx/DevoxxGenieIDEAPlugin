package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.webview.ConversationWebViewController;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * A test implementation of StreamingResponseHandler that doesn't require a real webViewController.
 * Only used for testing to avoid the need for complex reflection.
 */
public class TestStreamingResponseHandler extends StreamingResponseHandler {
    /**
     * Creates a test streaming response handler that works with a null webViewController.
     *
     * @param context The chat message context
     * @param onCompleteCallback Called when streaming completes successfully
     * @param onErrorCallback Called when streaming encounters an error
     */
    public TestStreamingResponseHandler(
            @NotNull ChatMessageContext context,
            @NotNull Consumer<ChatResponse> onCompleteCallback,
            @NotNull Consumer<Throwable> onErrorCallback) {
        super(context, null, onCompleteCallback, onErrorCallback);
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        // Do nothing in the test implementation to avoid NullPointerException
        // due to null webViewController
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        // We can use the parent method since it will eventually call the callback
        // even with a null webViewController in the constructor
        super.onCompleteResponse(response);
    }
}
