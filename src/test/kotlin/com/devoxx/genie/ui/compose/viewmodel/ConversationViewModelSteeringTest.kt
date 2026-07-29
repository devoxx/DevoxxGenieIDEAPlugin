package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.model.ConversationState
import dev.langchain4j.data.message.AiMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Issue #1241 steering UI sequence: when the user sends a message while an agent
 * task is running, the already-streamed content must be FROZEN in place, the
 * steering bubble inserted after it, and all subsequent AI output must continue
 * in a NEW area below the steering bubble — not in the original bubble above it.
 *
 * The streaming handler accumulates the full response across loop turns and
 * re-posts the complete text on every flush, so the continuation area subtracts
 * the frozen prefix via a content offset.
 */
class ConversationViewModelSteeringTest {

    private val viewModel = ConversationViewModel()

    private fun startStreamingMessage(id: String = "msg-1", prompt: String = "implement the endpoints") {
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder()
                .id(id)
                .userPrompt(prompt)
                .build()
        )
    }

    private fun streamContent(id: String, fullText: String) {
        viewModel.updateAiMessageContent(
            ChatMessageContext.builder()
                .id(id)
                .aiMessage(AiMessage.from(fullText))
                .build()
        )
    }

    private fun messages() = (viewModel.state as ConversationState.Chat).messages

    @Test
    fun `steering freezes streamed content and continues in a new area below the bubble`() {
        startStreamingMessage()
        streamContent("msg-1", "First part of the answer.")

        viewModel.addSteeringMessage("use snake_case for the API json")

        // Sequence: frozen copy, steering bubble, continuation area
        val messages = messages()
        assertThat(messages).hasSize(3)

        val frozen = messages[0]
        assertThat(frozen.userPrompt).isEqualTo("implement the endpoints")
        assertThat(frozen.aiResponseMarkdown).isEqualTo("First part of the answer.")
        assertThat(frozen.isStreaming).isFalse()
        assertThat(frozen.isLoadingIndicatorVisible).isFalse()
        assertThat(frozen.id).isNotEqualTo("msg-1")

        val steering = messages[1]
        assertThat(steering.userPrompt).isEqualTo("use snake_case for the API json")
        assertThat(steering.isSteeringOnly).isTrue()

        val continuation = messages[2]
        assertThat(continuation.id).isEqualTo("msg-1")
        assertThat(continuation.userPrompt).isEmpty()
        assertThat(continuation.aiResponseMarkdown).isEmpty()
    }

    @Test
    fun `content streamed after steering shows only the new part in the continuation`() {
        startStreamingMessage()
        streamContent("msg-1", "First part of the answer.")
        viewModel.addSteeringMessage("use snake_case")

        // The handler re-posts the FULL accumulated text
        streamContent("msg-1", "First part of the answer.\n\nContinued after steering.")

        val messages = messages()
        assertThat(messages[0].aiResponseMarkdown).isEqualTo("First part of the answer.")
        assertThat(messages[2].aiResponseMarkdown).isEqualTo("Continued after steering.")
    }

    @Test
    fun `second steering message splits again with accumulated offsets`() {
        startStreamingMessage()
        streamContent("msg-1", "Part one.")
        viewModel.addSteeringMessage("first correction")
        streamContent("msg-1", "Part one.\n\nPart two.")
        viewModel.addSteeringMessage("second correction")
        streamContent("msg-1", "Part one.\n\nPart two.\n\nPart three.")

        val messages = messages()
        assertThat(messages).hasSize(5)
        assertThat(messages[0].aiResponseMarkdown).isEqualTo("Part one.")
        assertThat(messages[1].userPrompt).isEqualTo("first correction")
        assertThat(messages[2].aiResponseMarkdown).isEqualTo("Part two.")
        assertThat(messages[3].userPrompt).isEqualTo("second correction")
        assertThat(messages[4].aiResponseMarkdown).isEqualTo("Part three.")
        assertThat(messages[4].id).isEqualTo("msg-1")
    }

    @Test
    fun `resolved activity entries freeze while unresolved ones follow the continuation`() {
        startStreamingMessage()
        viewModel.onActivityMessage(toolRequest("read_file", "{}", callNumber = 1))
        viewModel.onActivityMessage(toolResponse("read_file", "content", callNumber = 1))
        viewModel.onActivityMessage(toolRequest("run_command", "{}", callNumber = 2))

        viewModel.addSteeringMessage("use snake_case")

        val messages = messages()
        val frozen = messages[0]
        val continuation = messages[2]
        assertThat(frozen.activityEntries).hasSize(1)
        assertThat(frozen.activityEntries[0].toolName).isEqualTo("read_file")
        assertThat(frozen.activityEntries[0].status).isEqualTo(ActivityStatus.SUCCESS)
        assertThat(continuation.activityEntries).hasSize(1)
        assertThat(continuation.activityEntries[0].toolName).isEqualTo("run_command")
        assertThat(continuation.activityEntries[0].status).isEqualTo(ActivityStatus.RUNNING)

        // The pending tool resolves on the continuation, not the frozen copy
        viewModel.onActivityMessage(toolResponse("run_command", "done", callNumber = 2))
        val resolved = messages()[2].activityEntries[0]
        assertThat(resolved.status).isEqualTo(ActivityStatus.SUCCESS)
    }

    @Test
    fun `frozen copy is marked so an empty frozen area can be hidden`() {
        // Agent runs often produce only activity/tool output before the user steers —
        // the frozen copy then has no answer text and must not render an empty AI frame.
        startStreamingMessage()

        viewModel.addSteeringMessage("use snake_case")

        val messages = messages()
        assertThat(messages[0].isSteeringFrozen).isTrue()
        assertThat(messages[0].aiResponseMarkdown).isEmpty()
        assertThat(messages[2].isSteeringFrozen).isFalse()
    }

    @Test
    fun `steering without an active message appends a plain steering bubble`() {
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("done-1")
                .userPrompt("old prompt")
                .aiMessage(AiMessage.from("old answer"))
                .build()
        )

        viewModel.addSteeringMessage("late message")

        val messages = messages()
        assertThat(messages).hasSize(2)
        assertThat(messages[1].userPrompt).isEqualTo("late message")
        assertThat(messages[1].isSteeringOnly).isTrue()
    }

    @Test
    fun `removing an unconsumed steering bubble removes only that bubble`() {
        // A leftover steering message (run ended before the loop consumed it) is
        // resubmitted as a new prompt — its old bubble must go or the question shows twice.
        startStreamingMessage()
        streamContent("msg-1", "Answer to the first question.")
        viewModel.addSteeringMessage("late question")

        viewModel.removeSteeringMessage("late question")

        val messages = messages()
        assertThat(messages).hasSize(2)
        assertThat(messages.none { it.isSteeringOnly }).isTrue()
        assertThat(messages[0].aiResponseMarkdown).isEqualTo("Answer to the first question.")
        assertThat(messages[1].id).isEqualTo("msg-1")
    }

    @Test
    fun `removeSteeringMessage removes only the last matching bubble`() {
        startStreamingMessage()
        viewModel.addSteeringMessage("same text")
        viewModel.addSteeringMessage("same text")

        viewModel.removeSteeringMessage("same text")

        assertThat(messages().count { it.isSteeringOnly }).isEqualTo(1)
    }

    @Test
    fun `removeSteeringMessage with unknown text is a no-op`() {
        startStreamingMessage()
        viewModel.addSteeringMessage("kept")

        viewModel.removeSteeringMessage("never sent")

        assertThat(messages().count { it.isSteeringOnly }).isEqualTo(1)
    }

    private fun agentMessage(
        type: AgentType,
        customize: (ActivityMessage.ActivityMessageBuilder) -> ActivityMessage.ActivityMessageBuilder = { it },
    ): ActivityMessage =
        customize(
            ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(type)
        ).build()

    private fun toolRequest(toolName: String, arguments: String, callNumber: Int = 1): ActivityMessage =
        agentMessage(AgentType.TOOL_REQUEST) {
            it.toolName(toolName).arguments(arguments).callNumber(callNumber).maxCalls(50)
        }

    private fun toolResponse(toolName: String, result: String, callNumber: Int = 1): ActivityMessage =
        agentMessage(AgentType.TOOL_RESPONSE) {
            it.toolName(toolName).result(result).callNumber(callNumber).maxCalls(50)
        }
}
