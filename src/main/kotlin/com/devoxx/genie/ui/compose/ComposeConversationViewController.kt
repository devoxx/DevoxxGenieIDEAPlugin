package com.devoxx.genie.ui.compose

import androidx.compose.ui.awt.ComposePanel
import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.screen.ConversationScreen
import com.devoxx.genie.ui.compose.viewmodel.ConversationViewModel
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.ResourceBundle
import javax.swing.JComponent

/**
 * Compose for Desktop implementation of [ConversationViewController].
 * Replaces the JCEF-based ConversationWebViewController.
 */
class ComposeConversationViewController(
    private val project: Project? = null,
    private val onCustomPromptClick: (String) -> Unit = {},
) : ConversationViewController {

    private val viewModel = ConversationViewModel()

    private val composePanel: ComposePanel = ComposePanel().apply {
        setContent {
            ConversationScreen(
                viewModel = viewModel,
                onFileClick = ::openFileInEditor,
                onCustomPromptClick = onCustomPromptClick,
            )
        }
    }

    private fun openFileInEditor(path: String) {
        val proj = project ?: return
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        FileEditorManager.getInstance(proj).openTextEditor(
            OpenFileDescriptor(proj, virtualFile),
            true,
        )
    }

    // ---- ConversationViewController ----

    override fun isInitialized(): Boolean = true

    override fun ensureBrowserInitialized(callback: Runnable) = callback.run()

    override fun dispose() { /* ComposePanel lifecycle is managed by Swing */ }

    override fun getComponent(): JComponent = composePanel

    override fun loadWelcomeContent(resourceBundle: ResourceBundle) {
        viewModel.loadWelcomeContent(resourceBundle)
    }

    override fun updateCustomPrompts(resourceBundle: ResourceBundle) {
        viewModel.updateCustomPrompts(resourceBundle)
    }

    override fun cancelPendingWelcomeLoad() {
        viewModel.cancelPendingWelcomeLoad()
    }

    override fun addChatMessage(chatMessageContext: ChatMessageContext) {
        viewModel.addChatMessage(chatMessageContext)
    }

    override fun addUserPromptMessage(chatMessageContext: ChatMessageContext) {
        viewModel.addUserPromptMessage(chatMessageContext)
    }

    override fun updateAiMessageContent(chatMessageContext: ChatMessageContext) {
        viewModel.updateAiMessageContent(chatMessageContext)
    }

    override fun addFileReferences(chatMessageContext: ChatMessageContext, files: List<VirtualFile>) {
        viewModel.addFileReferences(chatMessageContext, files)
    }

    override fun addSystemMessage(markdownContent: String) {
        viewModel.addSystemMessage(markdownContent)
    }

    override fun onActivityMessage(message: ActivityMessage) {
        viewModel.onActivityMessage(message)
    }

    override fun deactivateActivityHandlers() {
        viewModel.deactivateActivityHandlers()
    }

    override fun hideLoadingIndicator(messageId: String) {
        viewModel.hideLoadingIndicator(messageId)
    }

    override fun markMCPLogsAsCompleted(messageId: String) {
        viewModel.markMCPLogsAsCompleted(messageId)
    }

    override fun clearConversation() {
        viewModel.clearConversation()
    }

    override fun refreshForNewConversation() {
        viewModel.refreshForNewConversation()
    }

    override fun setRestoringConversation(restoring: Boolean) {
        viewModel.setRestoringConversation(restoring)
    }

    override fun themeChanged(isDarkTheme: Boolean) {
        viewModel.onThemeChanged(isDarkTheme)
    }
}
