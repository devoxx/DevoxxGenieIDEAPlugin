package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.StreamingException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.compose.ConversationViewController;
import com.devoxx.genie.ui.compose.model.TerminalState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Handles streaming responses from the LLM.
 * Processes tokens as they arrive and manages completion.
 */
@Slf4j
public class StreamingResponseHandler implements StreamingChatResponseHandler {

    /**
     * Schedules a one-shot flush of buffered tokens to the UI. Injectable so tests can
     * run flush tasks deterministically on the test thread (Mockito static mocks are
     * thread-local and would not be visible on a background scheduler thread).
     */
    @FunctionalInterface
    public interface FlushScheduler {
        void schedule(@NotNull Runnable task, long delayMillis);
    }

    /** Cadence at which buffered partial tokens are flushed to the UI. */
    static final long FLUSH_INTERVAL_MS = 75;

    private final ChatMessageContext context;
    private final long startTime;
    private final Project project;
    private final Consumer<ChatResponse> onCompleteCallback;
    private final Consumer<Throwable> onErrorCallback;
    private volatile boolean isStopped = false;
    private final ConversationViewController conversationViewController;
    private final FlushScheduler flushScheduler;
    private final long flushIntervalMs;

    // Track if we've added the initial message and accumulate the streamed tokens.
    // The accumulator is appended on the langchain4j thread and read on the flush
    // scheduler thread (and on stop), so all access is synchronized on the instance.
    private volatile boolean hasAddedInitialMessage = false;
    private volatile boolean isCompleted = false;
    private final java.util.concurrent.atomic.AtomicBoolean flushScheduled =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private final StringBuilder accumulatedResponse = new StringBuilder();

    /**
     * Creates a new streaming response handler
     *
     * @param context The chat message context
     * @param conversationViewController The web view controller to display conversation (can be null in tests)
     * @param onCompleteCallback Called when streaming completes successfully
     * @param onErrorCallback Called when streaming encounters an error
     */
    public StreamingResponseHandler(
            @NotNull ChatMessageContext context,
            ConversationViewController conversationViewController,
            @NotNull Consumer<ChatResponse> onCompleteCallback,
            @NotNull Consumer<Throwable> onErrorCallback) {
        this(context, conversationViewController, onCompleteCallback, onErrorCallback,
                StreamingResponseHandler::scheduleOnSharedExecutor, FLUSH_INTERVAL_MS);
    }

    /**
     * Test-visible constructor allowing the flush scheduler and cadence to be injected.
     */
    StreamingResponseHandler(
            @NotNull ChatMessageContext context,
            ConversationViewController conversationViewController,
            @NotNull Consumer<ChatResponse> onCompleteCallback,
            @NotNull Consumer<Throwable> onErrorCallback,
            @NotNull FlushScheduler flushScheduler,
            long flushIntervalMs) {
        log.debug("Created streaming handler for context {}", context.getId());
        this.context = context;
        this.project = context.getProject();
        this.onCompleteCallback = onCompleteCallback;
        this.onErrorCallback = onErrorCallback;
        this.startTime = System.currentTimeMillis();
        this.conversationViewController = conversationViewController;
        this.flushScheduler = flushScheduler;
        this.flushIntervalMs = flushIntervalMs;
    }

    /**
     * Default scheduler: one-shot task on the application-wide scheduled executor,
     * so no dedicated timer thread lives while streaming is idle.
     */
    private static void scheduleOnSharedExecutor(@NotNull Runnable task, long delayMillis) {
        com.intellij.util.concurrency.AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (isStopped) {
            return;
        }

        log.debug("Received partial response: '{}...'",
                partialResponse.substring(0, Math.min(20, partialResponse.length())));

        // Accumulate the response tokens
        synchronized (accumulatedResponse) {
            accumulatedResponse.append(partialResponse);
        }

        if (conversationViewController == null) {
            // Still update the message in context even without UI
            context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(getAccumulatedText()));
            hasAddedInitialMessage = true;
            return;
        }

        if (!hasAddedInitialMessage) {
            // Paint the very first token immediately so the user sees the response start,
            // then fall into the batched cadence for everything that follows.
            hasAddedInitialMessage = true;
            flushToUi();
        } else if (flushScheduled.compareAndSet(false, true)) {
            // Fast providers can deliver hundreds of tokens per second; posting one EDT
            // update per token floods the EDT and re-parses the full markdown each time.
            // Arm a single one-shot flush instead; tokens arriving meanwhile just land
            // in the accumulator and ride along with the armed flush.
            flushScheduler.schedule(this::runScheduledFlush, flushIntervalMs);
        }
    }

    private void runScheduledFlush() {
        flushScheduled.set(false);
        if (isStopped || isCompleted) {
            return;
        }
        flushToUi();
    }

    /**
     * Posts the current accumulated text to the UI. The snapshot is taken on the calling
     * thread; only the EDT runnable touches the view, matching the pre-batching contract.
     */
    private void flushToUi() {
        String fullText = getAccumulatedText();
        ApplicationManager.getApplication().invokeLater(() -> {
            context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(fullText));
            conversationViewController.updateAiMessageContent(context);
        });
    }

    private String getAccumulatedText() {
        synchronized (accumulatedResponse) {
            return accumulatedResponse.toString();
        }
    }

    /**
     * Invoked at each agent-loop turn boundary — i.e. when the LLM returns an intermediate
     * response that requests tool execution. langchain4j keeps streaming the next turn's
     * text through {@link #onPartialResponse(String)} into the same accumulator, so without
     * a marker here the reasoning of one turn would run straight into the next. We append a
     * blank-line separator so each turn renders as its own paragraph in the chat panel.
     */
    public void onIntermediateResponse(ChatResponse response) {
        if (isStopped) {
            return;
        }
        // Only separate turns that actually produced reasoning text; a tool-only turn
        // (empty text) shouldn't leave a dangling blank line.
        synchronized (accumulatedResponse) {
            if (!accumulatedResponse.isEmpty() && !accumulatedResponse.toString().endsWith("\n\n")) {
                accumulatedResponse.append("\n\n");
            }
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        if (isStopped) {
            return;
        }

        // Suppress any in-flight batched flush; the unconditional final update below
        // renders the complete accumulated text.
        isCompleted = true;

        try {
            long endTime = System.currentTimeMillis();
            context.setExecutionTimeMs(endTime - startTime);

            // Capture token usage from the final ChatResponse so the chat panel can show
            // input/output token counts, cost, and the used window context. The non-streaming
            // path already does this; without it streaming responses (the default) would never
            // report any token metrics. Local providers may return a null TokenUsage.
            if (response.tokenUsage() != null) {
                context.setTokenUsageAndCost(response.tokenUsage());
            }

            // In agent mode the LLM streams intermediate reasoning (e.g. "Let me take a
            // look...") before each tool call. langchain4j delivers those tokens through
            // onPartialResponse (so they accumulate here), but the final ChatResponse only
            // carries the LAST turn's AiMessage. Using response.aiMessage() directly would
            // therefore erase all intermediate reasoning from the chat panel. Prefer the
            // accumulated text whenever we streamed partials, so the full visible turn is
            // what we render and persist.
            String accumulatedText = getAccumulatedText();
            if (hasAddedInitialMessage && !accumulatedText.isEmpty()) {
                context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(accumulatedText));
            } else {
                context.setAiMessage(response.aiMessage());
            }

            // Update the view with the final response (if viewController is available)
            if (conversationViewController != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // If we've already shown partial responses, just update the AI content
                    // Otherwise add a new message pair (when we get complete response without partials)
                    if (hasAddedInitialMessage) {
                        conversationViewController.updateAiMessageContent(context);
                    } else {
                        conversationViewController.addChatMessage(context);
                    }
                    // Mark MCP logs as completed now that streaming is finished
                    conversationViewController.markMCPLogsAsCompleted(context.getId());
                    // Hide loading indicator on successful completion
                    conversationViewController.hideLoadingIndicator(context.getId());
                });
            }

            project.getMessageBus()
                .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                .onNewConversation(context);

            ChatMemoryManager.getInstance().addAiResponse(context);
            
            // Add file references if any (tab-aware)
            String tabIdForFiles = context.getTabId();
            if (!FileListManager.getInstance().isEmpty(context.getProject(), tabIdForFiles) && conversationViewController != null) {
                ApplicationManager.getApplication().invokeLater(() ->
                    // Add file references to the web view instead of creating a dialog
                    conversationViewController.addFileReferences(context,
                        FileListManager.getInstance().getFiles(context.getProject(), tabIdForFiles))
                );
            }
            
            log.debug("Streaming completed for context {}", context.getId());
            onCompleteCallback.accept(response);
        } catch (Exception e) {
            log.error("Error processing streaming completion", e);
            onErrorCallback.accept(e);
        }
    }

    @Override
    public void onError(@NotNull Throwable error) {
        log.error("Streaming error for context {}: {}", context.getId(), error.getMessage());

        // Persist any answer already streamed before the failure so the run survives in
        // conversation history. Must run before the UI teardown below.
        persistPartialResponseOnError();

        // Deactivate activity handlers BEFORE hiding to prevent race condition
        if (conversationViewController != null) {
            conversationViewController.deactivateActivityHandlers();
        }

        // Hide the loading indicator in the WebView
        hideLoadingIndicator();

        // Durable in-chat record of the failure (red error card with Retry), posted on
        // the EDT like every other Compose state mutation. Note this intentionally
        // coexists with the PromptErrorHandler.handleException call below, which also
        // sets ERROR via the panel registry: this direct call runs first and derives the
        // text from the RAW provider error, while handleException only sees the generic
        // StreamingException wrapper ("Error during streaming response"). Terminal states
        // are final, so the more specific text set here wins and the second set is a no-op.
        if (conversationViewController != null) {
            String errorText = PromptErrorHandler.userFacingMessage(error);
            ApplicationManager.getApplication().invokeLater(() ->
                conversationViewController.setTerminalState(context.getId(), TerminalState.ERROR, errorText));
        }

        StreamingException streamingError = new StreamingException(
            "Error during streaming response", error);
        PromptErrorHandler.handleException(context.getProject(), streamingError, context);
        onErrorCallback.accept(streamingError);
    }

    /**
     * Persists a run that produced visible answer text but then failed. Conversation
     * persistence is normally triggered only from {@link #onCompleteResponse} (which
     * publishes {@link AppTopics#CONVERSATION_TOPIC} → {@code ChatService.saveConversation}).
     * Without this, an agent/streaming run that streamed a visible answer and then hit a
     * trailing provider error (e.g. NVIDIA 404 / ConnectException mid tool-loop) would be
     * dropped from conversation history entirely, even though the user already saw the
     * answer. Mirrors how {@link #stop()} preserves the partial text in the live view.
     */
    private void persistPartialResponseOnError() {
        if (isStopped || isCompleted) {
            // stop() finalizes its own message; a completed run already persisted. Avoid
            // re-publishing a fresh conversation for a trailing/duplicate error callback.
            return;
        }
        String partial = getAccumulatedText();
        if (partial.isEmpty()) {
            // Provider failed before producing any text — there is nothing worth saving.
            return;
        }
        try {
            context.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(partial));
            project.getMessageBus()
                    .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                    .onNewConversation(context);
            log.debug("Persisted partial response to history after streaming error for context {}", context.getId());
        } catch (Exception e) {
            // Best-effort: a persistence failure must not mask the original streaming error.
            log.warn("Failed to persist partial response after streaming error for context {}", context.getId(), e);
        }
    }

    /**
     * Hides the "Thinking..." loading indicator and any agent activity in the WebView.
     * Delegates to ConversationViewController.hideLoadingIndicator().
     */
    private void hideLoadingIndicator() {
        if (conversationViewController == null) {
            return;
        }
        conversationViewController.hideLoadingIndicator(context.getId());
    }

    /**
     * Stops the streaming response handler.
     * Deactivates activity handlers before hiding the indicator to prevent
     * stale events from re-showing it (EDT queue race condition).
     */
    public void stop() {
        if (!isStopped) {
            isStopped = true;
            log.info("Stopping streaming handler for context {}, deactivating activity handlers", context.getId());

            // Render any tokens still sitting in the batch buffer — without this, up to
            // one flush interval of trailing text would silently vanish on stop.
            if (conversationViewController != null && hasAddedInitialMessage) {
                flushToUi();
            }

            // Clean up partial response from memory using the tab-aware memory key
            if (context.getAiMessage() != null) {
                ChatMemoryService.getInstance().removeLastMessageByKey(context.getMemoryKey());
                log.debug("Cleaned up partial AI response from memory for key {}", context.getMemoryKey());
            }

            // Deactivate activity handlers BEFORE hiding to prevent race condition
            if (conversationViewController != null) {
                conversationViewController.deactivateActivityHandlers();
            }

            // Hide the loading indicator in the WebView
            hideLoadingIndicator();

            // Leave a visible, durable "stopped" marker on the message; the partial
            // text flushed above stays in place. Posted via invokeLater so it executes
            // AFTER the final flushToUi() above (also queued on the EDT) — otherwise the
            // STOPPED guard in the view model would reject that last partial update.
            if (conversationViewController != null) {
                ApplicationManager.getApplication().invokeLater(() ->
                    conversationViewController.setTerminalState(context.getId(), TerminalState.STOPPED, null));
            }
        }
    }
}
