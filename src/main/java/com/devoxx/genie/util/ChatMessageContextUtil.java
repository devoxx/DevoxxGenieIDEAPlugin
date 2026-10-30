package com.devoxx.genie.util;

import com.devoxx.genie.model.ChatContextParameters;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import dev.langchain4j.data.message.UserMessage;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.action.AddSnippetAction.*;

@Slf4j
public class ChatMessageContextUtil {

    public static final int ZERO_SECONDS = 0;
    public static final int SIXTY_SECONDS = 60;

    private ChatMessageContextUtil() {
    }

    public static @NotNull ChatMessageContext createContext(@NotNull ChatContextParameters chatContextParameters) {

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // ragActivated is now derived from the master ragEnabled flag (task-222). The
        // chat-area per-session toggle was removed; RAG is either configured (enabled in
        // settings) or it isn't.
        boolean ragActivated = Boolean.TRUE.equals(stateService.getRagEnabled());
        boolean webSearchActivated = Boolean.TRUE.equals(stateService.getWebSearchActivated());

        ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                .project(chatContextParameters.project())
                .id(String.valueOf(System.currentTimeMillis()))
                .tabId(chatContextParameters.tabId())
                .userPrompt(chatContextParameters.userPromptText())
                .languageModel(chatContextParameters.languageModel())
                // task-209: mirror the chat-panel toggles onto the context so analytics reads them at completion.
                .ragActivated(ragActivated)
                .webSearchActivated(webSearchActivated)
                .webSearchRequested(webSearchActivated && (stateService.isGoogleSearchEnabled() || stateService.isTavilySearchEnabled()))
                .executionTimeMs(0)
                .cost(0)
                .build();

        // CLI Runners and ACP Runners bypass Langchain4J — no chat model needed
        if (chatContextParameters.languageModel().getProvider() != ModelProvider.CLIRunners &&
            chatContextParameters.languageModel().getProvider() != ModelProvider.ACPRunners) {
            if (Boolean.TRUE.equals(stateService.getStreamMode())) {
                chatMessageContext.setStreamingChatModel(chatContextParameters.chatModelProvider().getStreamingChatLanguageModel(chatMessageContext));
            } else {
                chatMessageContext.setChatModel(chatContextParameters.chatModelProvider().getChatLanguageModel(chatMessageContext));
            }
        }

        chatMessageContext.setTimeout(stateService.getTimeout() == ZERO_SECONDS ? SIXTY_SECONDS : stateService.getTimeout());

        setWindowContext(chatMessageContext,
                         chatContextParameters.editorFileButtonManager(),
                         chatContextParameters.projectContext(),
                         chatContextParameters.isProjectContextAdded());

        return chatMessageContext;
    }

    /**
     * Set the window context.
     *
     * @param chatMessageContext      the chat message context
     * @param editorFileButtonManager the editor file button manager
     * @param projectContext          the project context
     * @param isProjectContextAdded   the is project context added
     */
    static void setWindowContext(@NotNull ChatMessageContext chatMessageContext,
                                 EditorFileButtonManager editorFileButtonManager,
                                 String projectContext,
                                 boolean isProjectContextAdded) {

        // First handle any existing project context
        if (projectContext != null && isProjectContextAdded) {
            // If the full project is added as context, set it and ignore any attached files
            chatMessageContext.setFilesContext(projectContext);
            // task-209 analytics signal — no content, only a boolean flag
            chatMessageContext.setProjectContextFullUsed(true);
        } else {
            // Attach the editor selection (or open file) BEFORE collecting the attached files,
            // otherwise it only reaches the LLM on the next prompt (issue #1320).
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();
            if (selectedTextEditor != null) {
                addDefaultEditorInfoToMessageContext(selectedTextEditor, chatMessageContext);
            }

            // We don't include separate added files to the context if the full project is already included
            processAttachedFiles(chatMessageContext);
        }
    }

    /**
     * Process attached files.
     * Include all non-image files that have been added to the current conversation.
     * Image files are handled separately by MessageCreationService.addImages().
     * Files are only cleared when starting a new conversation.
     * @param chatMessageContext the chat message context
     */
    private static void processAttachedFiles(@NotNull ChatMessageContext chatMessageContext) {
        FileListManager fileListManager = FileListManager.getInstance();
        String tabId = chatMessageContext.getTabId();
        // Only process non-image files for text context; images are handled by addImages()
        List<VirtualFile> files = fileListManager.getNonImageFiles(chatMessageContext.getProject(), tabId);

        log.debug("processAttachedFiles: {} non-image files, {} image files",
                files.size(),
                fileListManager.getImageFiles(chatMessageContext.getProject(), tabId).size());

        if (!files.isEmpty()) {
            // Defer file content loading to background thread to avoid EDT freeze on large files
            chatMessageContext.setPendingAttachedFiles(new ArrayList<>(files));
            // task-209 analytics signal — no content, only a boolean flag
            chatMessageContext.setProjectContextSelectedUsed(true);
        }
    }

    /**
     * Attach the current editor file to the conversation: always when text is selected, otherwise only
     * when the "include open file" setting is on. The file auto-attached on a previous prompt is replaced;
     * files the user attached explicitly are never removed.
     */
    private static void addDefaultEditorInfoToMessageContext(Editor editor,
                                                             @NotNull ChatMessageContext chatMessageContext) {

        EditorInfo editorInfo = EditorUtil.getEditorInfo(editor);
        chatMessageContext.setEditorInfo(editorInfo);

        VirtualFile originalFile = editor.getVirtualFile();
        if (originalFile == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        boolean hasSelection = selectedText != null && !selectedText.isEmpty();
        if (!hasSelection && !Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseFileInEditor())) {
            // Nothing new to attach; keep a previously attached selection available for follow-up prompts
            return;
        }

        Project project = chatMessageContext.getProject();
        String tabId = chatMessageContext.getTabId();
        FileListManager fileListManager = FileListManager.getInstance();

        boolean userAttached = fileListManager.contains(project, tabId, originalFile) &&
                               !Boolean.TRUE.equals(originalFile.getUserData(AUTO_ATTACHED_KEY));
        if (userAttached && !hasSelection) {
            return;
        }

        // Drop the previous auto-attached editor file, or the current file so it is re-added with fresh metadata
        for (VirtualFile file : new ArrayList<>(fileListManager.getNonImageFiles(project, tabId))) {
            if (file.equals(originalFile) || Boolean.TRUE.equals(file.getUserData(AUTO_ATTACHED_KEY))) {
                fileListManager.removeFileAndRefresh(project, tabId, file);
            }
        }

        if (hasSelection) {
            Document document = editor.getDocument();
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            originalFile.putUserData(ORIGINAL_FILE_KEY, originalFile);
            originalFile.putUserData(SELECTED_TEXT_KEY, selectedText);
            originalFile.putUserData(SELECTION_START_KEY, startOffset);
            originalFile.putUserData(SELECTION_END_KEY, endOffset);
            originalFile.putUserData(SELECTION_START_LINE_KEY, document.getLineNumber(startOffset));
            originalFile.putUserData(SELECTION_END_LINE_KEY, document.getLineNumber(endOffset));
        }
        originalFile.putUserData(AUTO_ATTACHED_KEY, userAttached ? null : Boolean.TRUE);
        fileListManager.addFile(project, tabId, originalFile);
    }

    /**
     * Check if the chat message context contains multimodal content (e.g., images).
     *
     * @param context the chat message context
     * @return true if the user message contains non-text content
     */
    public static boolean hasMultimodalContent(@NotNull ChatMessageContext context) {
        UserMessage userMessage = context.getUserMessage();
        return userMessage != null && !userMessage.hasSingleText();
    }

    /**
     * Check if the language model is an OpenAI O1 model because that doesn't support system prompts.
     *
     * @param languageModel the language model
     * @return true if the language model is an OpenAI O1 model
     */
    public static boolean isOpenAIo1Model(LanguageModel languageModel) {
        return languageModel != null &&
                languageModel.getProvider() == ModelProvider.OpenAI &&
                languageModel.getModelName() != null &&
                languageModel.getModelName().toLowerCase().startsWith("o1");
    }
}
