package com.devoxx.genie.ui.compose.viewmodel

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
}
