package com.devoxx.genie.service.debug;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.debug.RawTrafficType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Captures the full request/response exchanged with the LLM provider and publishes it (redacted)
 * to the Activity Log panel, for debugging provider-specific issues. Only active when
 * "Raw Request/Response Logging" is enabled in Settings — unlike {@link com.devoxx.genie.service.mcp.MCPListenerService},
 * this can carry the complete prompt and response text, so it is opt-in.
 * <p>
 * langchain4j's request/response classes expose data through fluent accessors (e.g. {@code text()})
 * rather than bean-style getters, so a conventional {@link ObjectMapper} would serialize them to
 * "{}". The mapper here is configured to walk private fields directly instead.
 */
@Slf4j
public class RawTrafficListenerService implements ChatModelListener {

    private static final ObjectMapper RAW_MAPPER = createRawMapper();
    private static final int SUMMARY_MAX_LEN = 80;
    private static final int STACK_TRACE_FRAME_LIMIT = 10;

    private static @NotNull ObjectMapper createRawMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    private final Supplier<Boolean> enabledSupplier;
    private final Consumer<ActivityMessage> publisher;

    public RawTrafficListenerService() {
        this(
                () -> Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRawRequestResponseLoggingEnabled()),
                RawTrafficListenerService::publish
        );
    }

    RawTrafficListenerService(Supplier<Boolean> enabledSupplier, Consumer<ActivityMessage> publisher) {
        this.enabledSupplier = enabledSupplier;
        this.publisher = publisher;
    }

    @Override
    public void onRequest(@NotNull ChatModelRequestContext requestContext) {
        if (!enabledSupplier.get()) {
            return;
        }
        try {
            ChatRequest chatRequest = requestContext.chatRequest();
            String json = SecretRedactor.redact(RAW_MAPPER.writeValueAsString(chatRequest));
            publisher.accept(ActivityMessage.builder()
                    .source(ActivitySource.RAW)
                    .rawTrafficType(RawTrafficType.REQUEST)
                    .summary(summarizeRequest(chatRequest))
                    .content(json)
                    .build());
        } catch (Throwable t) {
            log.debug("Failed to capture raw LLM request", t);
        }
    }

    @Override
    public void onResponse(@NotNull ChatModelResponseContext responseContext) {
        if (!enabledSupplier.get()) {
            return;
        }
        try {
            ChatResponse chatResponse = responseContext.chatResponse();
            String json = SecretRedactor.redact(RAW_MAPPER.writeValueAsString(chatResponse));
            publisher.accept(ActivityMessage.builder()
                    .source(ActivitySource.RAW)
                    .rawTrafficType(RawTrafficType.RESPONSE)
                    .summary(summarizeResponse(chatResponse))
                    .content(json)
                    .build());
        } catch (Throwable t) {
            log.debug("Failed to capture raw LLM response", t);
        }
    }

    @Override
    public void onError(@NotNull ChatModelErrorContext errorContext) {
        if (!enabledSupplier.get()) {
            return;
        }
        try {
            Throwable error = errorContext.error();
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("errorType", error == null ? "unknown" : error.getClass().getName());
            errorMap.put("message", error == null ? null : error.getMessage());
            if (error != null) {
                List<String> topFrames = new ArrayList<>();
                StackTraceElement[] stackTrace = error.getStackTrace();
                for (int i = 0; i < Math.min(STACK_TRACE_FRAME_LIMIT, stackTrace.length); i++) {
                    topFrames.add(stackTrace[i].toString());
                }
                errorMap.put("stackTraceTop", topFrames);
            }
            String json = SecretRedactor.redact(RAW_MAPPER.writeValueAsString(errorMap));
            publisher.accept(ActivityMessage.builder()
                    .source(ActivitySource.RAW)
                    .rawTrafficType(RawTrafficType.ERROR)
                    .summary("ERROR → " + (error == null ? "unknown"
                            : error.getClass().getSimpleName() + ": " + summarize(error.getMessage())))
                    .content(json)
                    .build());
        } catch (Throwable t) {
            log.debug("Failed to capture raw LLM error", t);
        }
    }

    private static @NotNull String summarizeRequest(@NotNull ChatRequest request) {
        List<ChatMessage> messages = request.messages();
        int count = messages == null ? 0 : messages.size();
        String preview = messages == null || messages.isEmpty() ? "" : previewOf(messages.get(messages.size() - 1));
        StringBuilder sb = new StringBuilder("REQUEST → ").append(count).append(" message").append(count == 1 ? "" : "s");
        if (!preview.isEmpty()) {
            sb.append(": ").append(preview);
        }
        return sb.toString();
    }

    private static @NotNull String summarizeResponse(@NotNull ChatResponse response) {
        StringBuilder sb = new StringBuilder("RESPONSE");
        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            sb.append(" → tokens in/out: ")
              .append(tokenUsage.inputTokenCount() == null ? "?" : tokenUsage.inputTokenCount())
              .append("/")
              .append(tokenUsage.outputTokenCount() == null ? "?" : tokenUsage.outputTokenCount());
        }
        AiMessage aiMessage = response.aiMessage();
        if (aiMessage != null) {
            String preview = previewOf(aiMessage);
            if (!preview.isEmpty()) {
                sb.append(" — ").append(preview);
            } else if (aiMessage.hasToolExecutionRequests()) {
                sb.append(" — ").append(aiMessage.toolExecutionRequests().size()).append(" tool call(s)");
            }
        }
        return sb.toString();
    }

    private static @NotNull String previewOf(@NotNull ChatMessage message) {
        String text = null;
        if (message instanceof SystemMessage systemMessage) {
            text = systemMessage.text();
        } else if (message instanceof UserMessage userMessage && userMessage.hasSingleText()) {
            text = userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            text = aiMessage.text();
        } else if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            text = toolExecutionResultMessage.text();
        }
        return text == null ? "" : summarize(text);
    }

    private static @NotNull String summarize(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() > SUMMARY_MAX_LEN ? oneLine.substring(0, SUMMARY_MAX_LEN - 1) + "…" : oneLine;
    }

    private static void publish(@NotNull ActivityMessage message) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                .onActivityMessage(message);
    }
}
