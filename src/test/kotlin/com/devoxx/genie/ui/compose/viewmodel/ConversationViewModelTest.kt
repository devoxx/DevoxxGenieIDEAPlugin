package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ConversationState
import dev.langchain4j.data.message.AiMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConversationViewModelTest {

    @Test
    fun `theme change updates theme flag without dropping visible chat messages`() {
        val viewModel = ConversationViewModel()
        val initialTheme = viewModel.isDarkTheme

        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("Show me an example")
                .aiMessage(AiMessage.from("```java\nclass Demo {}\n```"))
                .executionTimeMs(1250)
                .build()
        )

        val beforeThemeChange = viewModel.state as ConversationState.Chat
        assertThat(beforeThemeChange.messages).hasSize(1)

        viewModel.onThemeChanged(!initialTheme)

        assertThat(viewModel.isDarkTheme).isEqualTo(!initialTheme)
        val afterThemeChange = viewModel.state as ConversationState.Chat
        assertThat(afterThemeChange.messages).containsExactlyElementsOf(beforeThemeChange.messages)
    }

    @Test
    fun `when show tool activity is off only agent reasoning is shown not tool calls`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { false })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("list_files", "src/main/java"))
        viewModel.onActivityMessage(intermediateResponse("I'll explore the project structure first."))

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].source).isEqualTo("AGENT")
        assertThat(entries[0].content).isEqualTo("I'll explore the project structure first.")
        assertThat(entries[0].toolName).isNull()
    }

    @Test
    fun `when show tool activity is on both tool calls and agent reasoning are shown`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("list_files", "src/main/java"))
        viewModel.onActivityMessage(intermediateResponse("I'll explore the project structure first."))

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(2)
        assertThat(entries.map { it.toolName }).contains("list_files")
    }

    private fun activeMessageEntries(viewModel: ConversationViewModel) =
        (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }.activityEntries

    private fun toolRequest(toolName: String, arguments: String): ActivityMessage =
        ActivityMessage.builder()
            .source(ActivitySource.AGENT)
            .agentType(AgentType.TOOL_REQUEST)
            .toolName(toolName)
            .arguments(arguments)
            .callNumber(1)
            .maxCalls(50)
            .build()

    private fun intermediateResponse(text: String): ActivityMessage =
        ActivityMessage.builder()
            .source(ActivitySource.AGENT)
            .agentType(AgentType.INTERMEDIATE_RESPONSE)
            .result(text)
            .build()
}
