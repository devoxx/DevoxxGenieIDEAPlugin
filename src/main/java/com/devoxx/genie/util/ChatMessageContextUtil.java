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

        ChatMessageContext context = ChatMessageContext.builder()
            .project(project)
            .id(String.valueOf(System.currentTimeMillis()))
            .userPrompt(userPromptText)
            .languageModel(languageModel)
            .webSearchRequested(stateService.getWebSearchActivated() && (stateService.isGoogleSearchEnabled() || stateService.isTavilySearchEnabled()))
            .totalFileCount(FileListManager.getInstance().size())
            .executionTimeMs(0)
            .cost(0)
            .build();

        boolean isStreamMode = stateService.getStreamMode() && actionCommand.equals(Constant.SUBMIT_ACTION);
        if (isStreamMode) {
            context.setStreamingChatLanguageModel(chatModelProvider.getStreamingChatLanguageModel(context));
        } else {
            context.setChatLanguageModel(chatModelProvider.getChatLanguageModel(context));
        }

        context.setTimeout(stateService.getTimeout() == ZERO_SECONDS ? SIXTY_SECONDS : stateService.getTimeout());

        setWindowContext(context, userPromptText, editorFileButtonManager, projectContext, isProjectContextAdded);

        return context;
    }

    /**
     * Set the window context.
     *
     * @param chatMessageContext      the chat message context
     * @param userPrompt              the user prompt
     * @param editorFileButtonManager the editor file button manager
     * @param projectContext          the project context
     * @param isProjectContextAdded   the is project context added
     */
    private static void setWindowContext(@NotNull ChatMessageContext chatMessageContext,
                                         String userPrompt,
                                         EditorFileButtonManager editorFileButtonManager,
                                         String projectContext,
                                         boolean isProjectContextAdded) {

        if (projectContext != null && isProjectContextAdded) {
            chatMessageContext.setContext(projectContext);
        } else {
            Editor selectedTextEditor = editorFileButtonManager.getSelectedTextEditor();

            // Add files to the context
            List<VirtualFile> files = FileListManager.getInstance().getFiles();
            if (!files.isEmpty()) {
                addSelectedFiles(chatMessageContext, userPrompt, files);
            }

            // Set the context based on the selected code snippet or the complete file
            if (selectedTextEditor != null) {
                addEditorInfoToMessageContext(selectedTextEditor, chatMessageContext);
            }
        }
    }

    /**
     * Add the user selected files to chat message context.
     * @param chatMessageContext the chat message context
     * @param userPrompt the user prompt
     * @param files the add files
     */
    private static void addSelectedFiles(@NotNull ChatMessageContext chatMessageContext,
                                         String userPrompt,
                                         List<VirtualFile> files) {
        chatMessageContext.setEditorInfo(new EditorInfo(files));

        MessageCreationService.getInstance().createUserPromptWithContextAsync(chatMessageContext.getProject(), userPrompt, files)
            .thenAccept(chatMessageContext::setContext)
            .exceptionally(ex -> {
                ErrorHandler.handleError(chatMessageContext.getProject(), ex);
                return null;
            });
    }

    private static void addEditorInfoToMessageContext(Editor editor,
                                                      @NotNull ChatMessageContext chatMessageContext) {
        EditorInfo editorInfo = EditorUtil.getEditorInfo(editor);
        chatMessageContext.setEditorInfo(editorInfo);
    }

    /**
     * Check if the language model is an OpenAI O1 model because that doesn't support system prompts.
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
