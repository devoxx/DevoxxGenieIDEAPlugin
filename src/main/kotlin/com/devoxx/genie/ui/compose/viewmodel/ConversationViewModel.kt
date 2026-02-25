package com.devoxx.genie.ui.compose.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.devoxx.genie.model.LanguageModel
import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.*
import com.devoxx.genie.ui.settings.DevoxxGenieStateService
import com.intellij.openapi.vfs.VirtualFile
import java.util.ResourceBundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ConversationViewModel {

    var state: ConversationState by mutableStateOf(
        ConversationState.Welcome(
            resourceBundle = ResourceBundle.getBundle("messages"),
            customPrompts = loadCustomPrompts(),
        )
    )
        private set

    var isDarkTheme: Boolean by mutableStateOf(
        com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme()
    )
        private set

    var customFontSize: Int by mutableStateOf(readFontSize())
        private set

    var customCodeFontSize: Int by mutableStateOf(readCodeFontSize())
        private set

    fun onThemeChanged(dark: Boolean) {
        isDarkTheme = dark
    }

    fun onAppearanceSettingsChanged() {
        customFontSize = readFontSize()
        customCodeFontSize = readCodeFontSize()
    }

    private fun readFontSize(): Int {
        return try {
            val state = DevoxxGenieStateService.getInstance()
            if (state.useCustomFontSize == true) state.customFontSize ?: 13 else 13
        } catch (_: Exception) { 13 }
    }

    private fun readCodeFontSize(): Int {
        return try {
            val state = DevoxxGenieStateService.getInstance()
            if (state.useCustomCodeFontSize == true) state.customCodeFontSize ?: 12 else 12
        } catch (_: Exception) { 12 }
    }

    private var activeMessageId: String? = null
    private val activityDeactivated = AtomicBoolean(false)
    private val activityGeneration = AtomicInteger(0)

    fun loadWelcomeContent(resourceBundle: ResourceBundle) {
        state = ConversationState.Welcome(
            resourceBundle = resourceBundle,
            customPrompts = loadCustomPrompts(),
        )
    }

    fun updateCustomPrompts(resourceBundle: ResourceBundle) {
        val current = state
        if (current is ConversationState.Welcome) {
            state = current.copy(customPrompts = loadCustomPrompts())
        }
    }

    fun addUserPromptMessage(context: ChatMessageContext) {
        // Deactivate previous activity handlers
        deactivateActivityHandlers()

        val newMessage = MessageUiModel(
            id = context.id,
            userPrompt = context.userPrompt ?: "",
            isLoadingIndicatorVisible = true,
            isStreaming = context.streamingChatModel != null,
            modelName = formatModelDisplayName(context.languageModel),
        )

        val currentState = state
        val messages = when (currentState) {
            is ConversationState.Chat -> currentState.messages + newMessage
            is ConversationState.Welcome -> listOf(newMessage)
        }
        state = ConversationState.Chat(messages = messages)

        // Re-activate with the new message ID
        activityDeactivated.set(false)
        activityGeneration.incrementAndGet()
        activeMessageId = context.id
    }

    fun updateAiMessageContent(context: ChatMessageContext) {
        val aiText = context.aiMessage?.text() ?: return
        updateMessage(context.id) { msg ->
            msg.copy(
                aiResponseMarkdown = aiText,
                executionTimeMs = context.executionTimeMs,
                tokenUsage = context.tokenUsage?.let {
                    TokenUsageInfo(
                        inputTokens = it.inputTokenCount()?.toLong() ?: 0,
                        outputTokens = it.outputTokenCount()?.toLong() ?: 0,
                        cost = context.cost,
                    )
                } ?: msg.tokenUsage,
                modelName = formatModelDisplayName(context.languageModel).ifEmpty { msg.modelName },
            )
        }
    }

    fun addChatMessage(context: ChatMessageContext) {
        val message = MessageUiModel(
            id = context.id,
            userPrompt = context.userPrompt ?: "",
            aiResponseMarkdown = context.aiMessage?.text() ?: "",
            modelName = formatModelDisplayName(context.languageModel),
            executionTimeMs = context.executionTimeMs,
            tokenUsage = context.tokenUsage?.let {
                TokenUsageInfo(
                    inputTokens = it.inputTokenCount()?.toLong() ?: 0,
                    outputTokens = it.outputTokenCount()?.toLong() ?: 0,
                    cost = context.cost,
                )
            } ?: TokenUsageInfo(),
        )

        val currentState = state
        val messages = when (currentState) {
            is ConversationState.Chat -> currentState.messages + message
            is ConversationState.Welcome -> listOf(message)
        }
        state = ConversationState.Chat(messages = messages)
    }

    fun addSystemMessage(markdownContent: String) {
        val message = MessageUiModel(
            id = "system_${System.currentTimeMillis()}",
            userPrompt = "",
            aiResponseMarkdown = markdownContent,
        )

        val currentState = state
        val messages = when (currentState) {
            is ConversationState.Chat -> currentState.messages + message
            is ConversationState.Welcome -> listOf(message)
        }
        state = ConversationState.Chat(messages = messages)
    }

    fun addFileReferences(context: ChatMessageContext, files: List<VirtualFile>) {
        val fileModels = files.map { file ->
            FileReferenceUiModel(
                filePath = file.path,
                fileName = file.name,
            )
        }
        updateMessage(context.id) { msg ->
            msg.copy(fileReferences = msg.fileReferences + fileModels)
        }
    }

    fun onActivityMessage(message: ActivityMessage) {
        if (activityDeactivated.get()) return

        val msgId = activeMessageId ?: return

        // Agent intermediate responses (reasoning text) — show as activity entry
        // instead of appending to aiResponseMarkdown (which gets overwritten by streaming updates)
        if (message.source == ActivitySource.AGENT &&
            message.agentType == AgentType.INTERMEDIATE_RESPONSE
        ) {
            val reasoning = message.result ?: return
            val entry = ActivityEntryUiModel(
                source = "AGENT",
                content = reasoning,
            )
            updateMessage(msgId) { msg ->
                msg.copy(activityEntries = msg.activityEntries + entry)
            }
            return
        }

        val entry = ActivityEntryUiModel(
            source = message.source?.name ?: "UNKNOWN",
            content = message.content ?: "",
            toolName = message.toolName,
            arguments = message.arguments,
            result = message.result,
            callNumber = message.callNumber,
            maxCalls = message.maxCalls,
        )

        updateMessage(msgId) { msg ->
            msg.copy(activityEntries = msg.activityEntries + entry)
        }
    }

    fun deactivateActivityHandlers() {
        activityDeactivated.set(true)
        activityGeneration.incrementAndGet()
        activeMessageId = null
    }

    fun hideLoadingIndicator(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(isLoadingIndicatorVisible = false)
        }
    }

    fun markMCPLogsAsCompleted(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(mcpLogsCompleted = true)
        }
    }

    fun clearConversation() {
        val current = state
        val rb = when (current) {
            is ConversationState.Welcome -> current.resourceBundle
            is ConversationState.Chat -> ResourceBundle.getBundle("messages")
        }
        state = ConversationState.Welcome(
            resourceBundle = rb,
            customPrompts = loadCustomPrompts(),
        )
    }

    fun setRestoringConversation(restoring: Boolean) {
        val current = state
        if (current is ConversationState.Chat) {
            state = current.copy(isRestoringConversation = restoring)
        }
    }

    // No-ops — these were needed for JCEF but not for Compose
    fun cancelPendingWelcomeLoad() { /* no-op */ }
    fun refreshForNewConversation() { /* no-op */ }

    private fun updateMessage(messageId: String, transform: (MessageUiModel) -> MessageUiModel) {
        val current = state
        if (current is ConversationState.Chat) {
            val updated = current.messages.map { msg ->
                if (msg.id == messageId) transform(msg) else msg
            }
            state = current.copy(messages = updated)
        }
    }

    private fun formatModelDisplayName(model: LanguageModel?): String {
        val name = model?.modelName ?: return ""
        val provider = model.provider?.name ?: return name
        return "$provider : $name"
    }

    private fun loadCustomPrompts(): List<CustomPromptUi> {
        return try {
            DevoxxGenieStateService.getInstance()
                .customPrompts
                .map { CustomPromptUi(name = it.name, prompt = it.prompt) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
