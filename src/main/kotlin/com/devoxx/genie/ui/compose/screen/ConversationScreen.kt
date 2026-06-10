package com.devoxx.genie.ui.compose.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.devoxx.genie.ui.compose.model.ConversationState
import com.devoxx.genie.ui.compose.theme.DevoxxGenieTheme
import com.devoxx.genie.ui.compose.util.IdeAnimations
import com.devoxx.genie.ui.compose.viewmodel.ConversationViewModel

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onFileClick: (String) -> Unit,
    onCustomPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state

    DevoxxGenieTheme(
        darkTheme = viewModel.isDarkTheme,
        bodyFontSize = viewModel.customFontSize,
        codeFontSize = viewModel.customCodeFontSize,
        ideScale = viewModel.ideScale,
    ) {
        val animationsEnabled = remember { IdeAnimations.enabled() }

        // Crossfade between the Welcome and Chat screens instead of hard-swapping.
        // contentKey keys the transition on the state *type* only: payload updates
        // (streaming tokens, blog/skill refreshes) recompose the current screen in
        // place, and only an actual Welcome <-> Chat switch animates. The exiting
        // screen keeps rendering its last state while fading out.
        AnimatedContent(
            targetState = state,
            contentKey = { it is ConversationState.Chat },
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(IdeAnimations.SCREEN_TRANSITION_MS)) togetherWith
                            fadeOut(tween(IdeAnimations.SCREEN_TRANSITION_MS))
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            modifier = modifier,
        ) { s ->
            when (s) {
                is ConversationState.Welcome -> {
                    WelcomeScreen(
                        resourceBundle = s.resourceBundle,
                        customPrompts = s.customPrompts,
                        skills = s.skills,
                        blogPosts = s.blogPosts,
                        onCustomPromptClick = onCustomPromptClick,
                    )
                }
                is ConversationState.Chat -> {
                    ChatScreen(
                        messages = s.messages,
                        onFileClick = onFileClick,
                        isRestoring = s.isRestoringConversation,
                    )
                }
            }
        }
    }
}
