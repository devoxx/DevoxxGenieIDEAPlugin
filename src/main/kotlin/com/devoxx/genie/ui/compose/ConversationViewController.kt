package com.devoxx.genie.ui.compose

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.TerminalState
import com.intellij.openapi.vfs.VirtualFile
import java.util.ResourceBundle
import javax.swing.JComponent

/**
 * Abstraction over the conversation rendering layer.
 * Implemented by [ComposeConversationViewController] (Compose for Desktop)
 * and formerly by ConversationWebViewController (JCEF/WebView).
 */
interface ConversationViewController {

    /** Whether the underlying UI is fully initialised and ready for content. */
    fun isInitialized(): Boolean

    /** Runs [callback] once the UI is ready. May execute immediately. */
    fun ensureBrowserInitialized(callback: Runnable)

    /** Release resources. */
    fun dispose()

    /** Returns the Swing component that hosts the conversation UI. */
    fun getComponent(): JComponent

    // ---- content ----

    fun loadWelcomeContent(resourceBundle: ResourceBundle)
    fun updateCustomPrompts(resourceBundle: ResourceBundle)
    fun cancelPendingWelcomeLoad()

    fun addChatMessage(chatMessageContext: ChatMessageContext)
    fun addUserPromptMessage(chatMessageContext: ChatMessageContext)

    /**
     * Adds a user bubble for a steering message sent while an agent task is
     * running (issue #1241). Unlike [addUserPromptMessage] this must not redirect
     * the in-flight streaming target or reset the activity handlers.
     */
    fun addSteeringMessage(text: String)

    /**
     * Adds a bubble for a queued prompt — an independent next question submitted
     * while a task runs, executed after the current run completes (issue #1241).
     */
    fun addQueuedPromptMessage(text: String)

    /**
     * Removes the last steering/queued bubble with the given text — used when a
     * leftover steering message or queued prompt is resubmitted as a regular prompt.
     */
    fun removeSteeringMessage(text: String)

    fun updateAiMessageContent(chatMessageContext: ChatMessageContext)
    fun addFileReferences(chatMessageContext: ChatMessageContext, files: List<VirtualFile>)

    /** Adds a system/help message consisting only of markdown (no user prompt). */
    fun addSystemMessage(markdownContent: String)

    // ---- activity / loading ----

    fun onActivityMessage(message: ActivityMessage)
    fun onActivityMessage(message: ActivityMessage, expectedGeneration: Int)
    fun currentActivityGeneration(): Int
    fun deactivateActivityHandlers()
    fun hideLoadingIndicator(messageId: String)
    fun markMCPLogsAsCompleted(messageId: String)

    /**
     * Marks a message with an explicit terminal state (stopped / error / loop limit).
     * Terminal states are final — the first one set wins. [errorText] is only used
     * for [TerminalState.ERROR].
     */
    fun setTerminalState(messageId: String, state: TerminalState, errorText: String?)

    // ---- conversation lifecycle ----

    fun clearConversation()
    fun refreshForNewConversation()
    fun setRestoringConversation(restoring: Boolean)

    // ---- theme ----

    fun themeChanged(isDarkTheme: Boolean)

    /** Called when appearance settings (font size, etc.) change. */
    fun appearanceSettingsChanged()
}
