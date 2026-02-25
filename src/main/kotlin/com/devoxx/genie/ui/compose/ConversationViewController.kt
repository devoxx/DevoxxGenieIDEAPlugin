package com.devoxx.genie.ui.compose

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.request.ChatMessageContext
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
    fun updateAiMessageContent(chatMessageContext: ChatMessageContext)
    fun addFileReferences(chatMessageContext: ChatMessageContext, files: List<VirtualFile>)

    /** Adds a system/help message consisting only of markdown (no user prompt). */
    fun addSystemMessage(markdownContent: String)

    // ---- activity / loading ----

    fun onActivityMessage(message: ActivityMessage)
    fun deactivateActivityHandlers()
    fun hideLoadingIndicator(messageId: String)
    fun markMCPLogsAsCompleted(messageId: String)

    // ---- conversation lifecycle ----

    fun clearConversation()
    fun refreshForNewConversation()
    fun setRestoringConversation(restoring: Boolean)

    // ---- theme ----

    fun themeChanged(isDarkTheme: Boolean)

    /** Called when appearance settings (font size, etc.) change. */
    fun appearanceSettingsChanged()
}
