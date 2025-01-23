package com.devoxx.genie.util;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChatMessageContextUtil {

    public static final int ZERO_SECONDS = 0;
    public static final int SIXTY_SECONDS = 60;

    private ChatMessageContextUtil() {
    }

    public static @NotNull ChatMessageContext createContext(Project project,
                                                            String userPromptText,
                                                            LanguageModel languageModel,
                                                            ChatModelProvider chatModelProvider,
                                                            @NotNull String actionCommand,
                                                            EditorFileButtonManager editorFileButtonManager,
                                                            String projectContext,
                                                            boolean isProjectContextAdded) {

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                .project(project)
                .id(String.valueOf(System.currentTimeMillis()))
                .userPrompt(userPromptText)
                .languageModel(languageModel)
                .webSearchRequested(stateService.getWebSearchActivated() && (stateService.isGoogleSearchEnabled() || stateService.isTavilySearchEnabled()))
                .executionTimeMs(0)
                .cost(0)
                .build();

        boolean isStreamMode = stateService.getStreamMode() && actionCommand.equals(Constant.SUBMIT_ACTION);
        if (isStreamMode) {
            chatMessageContext.setStreamingChatLanguageModel(chatModelProvider.getStreamingChatLanguageModel(chatMessageContext));
        } else {
            chatMessageContext.setChatLanguageModel(chatModelProvider.getChatLanguageModel(chatMessageContext));
        }

        chatMessageContext.setTimeout(stateService.getTimeout() == ZERO_SECONDS ? SIXTY_SECONDS : stateService.getTimeout());

        setWindowContext(chatMessageContext, editorFileButtonManager, projectContext, isProjectContextAdded);

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
    private static void setWindowContext(@NotNull ChatMessageContext chatMessageContext,
                                         EditorFileButtonManager editorFileButtonManager,
                                         String projectContext,
                                         boolean isProjectContextAdded) {

        // First handle any existing project context
        if (projectContext != null && isProjectContextAdded) {
            // If the full project is added as context, set it and ignore any attached files
            chatMessageContext.setFilesContext(projectContext);
        } else {
            // We don't include separate added files to the context if the full project is already included
            processAttachedFiles(chatMessageContext);

            // Set editor info if available
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();
            if (selectedTextEditor != null) {
                addEditorInfoToMessageContext(selectedTextEditor, chatMessageContext);
            }
        }
    }

    private static void processAttachedFiles(@NotNull ChatMessageContext chatMessageContext) {
        List<VirtualFile> files = FileListManager.getInstance().getFiles(chatMessageContext.getProject());
        if (!files.isEmpty()) {
            try {
                String newContext = MessageCreationService
                        .getInstance()
                        .createAttachedFilesContext(chatMessageContext.getProject(), files);

                chatMessageContext.setFilesContext(newContext);
            } catch (Exception ex) {
                ErrorHandler.handleError(chatMessageContext.getProject(), ex);
            }
        }
    }

    private static void addEditorInfoToMessageContext(Editor editor,
                                                      @NotNull ChatMessageContext chatMessageContext) {
        EditorInfo editorInfo = EditorUtil.getEditorInfo(editor);
        chatMessageContext.setEditorInfo(editorInfo);
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
                languageModel.getModelName().toLowerCase().startsWith("o1-");
    }
}
