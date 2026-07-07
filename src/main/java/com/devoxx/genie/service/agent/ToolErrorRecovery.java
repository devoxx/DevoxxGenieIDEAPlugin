package com.devoxx.genie.service.agent;

import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

/**
 * Configures tool-error recovery on tool-using {@link AiServices} builders (issue #1193).
 *
 * <p>Langchain4j's defaults <em>throw</em> when the model requests a tool that isn't
 * registered ({@code HallucinatedToolNameStrategy.THROW_EXCEPTION}) or sends unparsable
 * arguments (default {@code ToolArgumentsErrorHandler} rethrows). Both throws happen
 * <em>after</em> the AiMessage(tool_calls) has been written to chat memory but
 * <em>before</em> any tool result is written, leaving a dangling tool_use tail that
 * makes OpenAI-compatible providers (DeepSeek, OpenAI, …) reject every subsequent
 * request with "An assistant message with 'tool_calls' must be followed by tool
 * messages responding to each 'tool_call_id'".
 *
 * <p>Instead of throwing, return the error as a tool result: chat memory stays
 * consistent and the model gets feedback so it can self-correct (e.g. pick an
 * existing tool or fix its arguments).
 */
public final class ToolErrorRecovery {

    private ToolErrorRecovery() {
    }

    /**
     * Applies the recovery strategies to the given builder. Must be called on every
     * tool-using {@code AiServices} builder (streaming, non-streaming, sub-agent).
     *
     * @param builder the builder to configure
     * @return the same builder, for chaining
     */
    public static <T> AiServices<T> configure(AiServices<T> builder) {
        return builder
                .hallucinatedToolNameStrategy(toolRequest -> ToolExecutionResultMessage.from(
                        toolRequest,
                        "Error: there is no tool called '" + toolRequest.name()
                                + "'. Only use the tools that were provided to you."))
                .toolArgumentsErrorHandler((error, errorContext) -> ToolErrorHandlerResult.text(
                        "Error: invalid arguments for tool '"
                                + errorContext.toolExecutionRequest().name() + "': " + error.getMessage()));
    }
}
