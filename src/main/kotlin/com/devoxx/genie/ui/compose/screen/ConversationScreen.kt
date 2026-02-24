package com.devoxx.genie.ui.compose.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devoxx.genie.ui.compose.model.ConversationState
import com.devoxx.genie.ui.compose.theme.DevoxxGenieTheme
import com.devoxx.genie.ui.compose.viewmodel.ConversationViewModel

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onFileClick: (String) -> Unit,
    onCustomPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state

    DevoxxGenieTheme(darkTheme = viewModel.isDarkTheme) {
        when (val s = state) {
            is ConversationState.Welcome -> {
                WelcomeScreen(
                    resourceBundle = s.resourceBundle,
                    customPrompts = s.customPrompts,
                    onCustomPromptClick = onCustomPromptClick,
                    modifier = modifier,
                )
            }
            is ConversationState.Chat -> {
                ChatScreen(
                    messages = s.messages,
                    onFileClick = onFileClick,
                    modifier = modifier,
                )
            }
        }
    }
}
