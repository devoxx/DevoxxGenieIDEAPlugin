package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ConversationState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.StreamingChatModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.ResourceBundle

class ConversationViewModelTest {

    @Test
    fun `clearConversation during restore keeps chat state and never flashes welcome`() {
        val viewModel = ConversationViewModel()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hello")
                .aiMessage(AiMessage.from("hi"))
                .build()
        )

        // Simulates ConversationHistoryManager.restoreConversation → clearWithoutWelcome
        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()

        val midRestore = viewModel.state
        assertThat(midRestore).isInstanceOf(ConversationState.Chat::class.java)
        assertThat((midRestore as ConversationState.Chat).messages).isEmpty()
        assertThat(midRestore.isRestoringConversation).isTrue()

        // Restored messages arrive, then the flag is cleared
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("restored-1")
                .userPrompt("old prompt")
                .aiMessage(AiMessage.from("old answer"))
                .build()
        )
        viewModel.setRestoringConversation(false)

        val afterRestore = viewModel.state as ConversationState.Chat
        assertThat(afterRestore.messages).hasSize(1)
        assertThat(afterRestore.isRestoringConversation).isFalse()
    }

    @Test
    fun `clearConversation outside restore returns to welcome`() {
        val viewModel = ConversationViewModel()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hello")
                .aiMessage(AiMessage.from("hi"))
                .build()
        )

        viewModel.clearConversation()

        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

    @Test
    fun `live user prompt ends the restore window so a later clear shows welcome`() {
        val viewModel = ConversationViewModel()

        // First prompt of a new conversation goes through clearWithoutWelcome
        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder().id("msg-1").userPrompt("hi").build()
        )

        assertThat(viewModel.state).isInstanceOf(ConversationState.Chat::class.java)

        // New Conversation afterwards must show the welcome screen again
        viewModel.clearConversation()
        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

    @Test
    fun `explicit welcome request clears a stale restore flag`() {
        val viewModel = ConversationViewModel()
        viewModel.setRestoringConversation(true)

        viewModel.loadWelcomeContent(ResourceBundle.getBundle("messages"))

        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
        // Flag was reset — a subsequent clear must also show welcome, not an empty chat
        viewModel.clearConversation()
        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

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

    @Test
    fun `tool response does not add a duplicate activity entry, only the request is shown`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"git status"}"""))
        viewModel.onActivityMessage(toolResponse("run_command", "On branch master"))

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].toolName).isEqualTo("run_command")
        assertThat(entries[0].arguments).contains("git status")
    }

    @Test
    fun `hide loading indicator also clears the streaming flag`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hi")
                .streamingChatModel(mock(StreamingChatModel::class.java))
                .build()
        )

        val before = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(before.isStreaming).isTrue()

        // Complete, error and stop all funnel through hideLoadingIndicator — clearing the
        // streaming flag here keeps one lifecycle for both in-flight indicators.
        viewModel.hideLoadingIndicator("msg-1")

        val after = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(after.isStreaming).isFalse()
        assertThat(after.isLoadingIndicatorVisible).isFalse()
    }

    @Test
    fun `ide scale is read at construction and refreshed on appearance settings change`() {
        var scale = 1f
        val viewModel = ConversationViewModel(readIdeScale = { scale })
        assertThat(viewModel.ideScale).isEqualTo(1f)

        // user zooms the IDE in (Appearance → Zoom IDE)
        scale = 1.5f
        viewModel.onAppearanceSettingsChanged()
        assertThat(viewModel.ideScale).isEqualTo(1.5f)

        // and back out again
        scale = 0.9f
        viewModel.onAppearanceSettingsChanged()
        assertThat(viewModel.ideScale).isEqualTo(0.9f)
    }

    @Test
    fun `ide scale defaults to 1 when the platform is unavailable`() {
        // default provider runs without an IntelliJ Application in unit tests
        val viewModel = ConversationViewModel()
        assertThat(viewModel.ideScale).isEqualTo(1f)
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

    private fun toolResponse(toolName: String, result: String): ActivityMessage =
        ActivityMessage.builder()
            .source(ActivitySource.AGENT)
            .agentType(AgentType.TOOL_RESPONSE)
            .toolName(toolName)
            .result(result)
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
