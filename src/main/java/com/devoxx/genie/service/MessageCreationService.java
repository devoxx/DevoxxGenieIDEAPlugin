package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.devoxx.genie.action.AddSnippetAction.SELECTED_TEXT_KEY;

/**
 * The message creation service for user and system messages.
 * Here's where also the basic prompt "engineering" is happening, including calling the AST magic.
 */
public class MessageCreationService {


    public static final String CONTEXT_PROMPT = "Context: \n";

    @NotNull
    public static MessageCreationService getInstance() {
        return ApplicationManager.getApplication().getService(MessageCreationService.class);
    }

    @NotNull
    public UserMessage createUserMessage(@NotNull ChatMessageContext chatMessageContext) {
        UserMessage userMessage;
        String context = chatMessageContext.getContext();

        if (context != null && !context.isEmpty() && !chatMessageContext.isFullProjectContextAdded()) {
            // This is likely the full project context scenario
            userMessage = constructUserMessageWithFullContext(chatMessageContext, context);
            chatMessageContext.setFullProjectContextAdded(true);
        } else {
            // Here we include the editor content instead
            userMessage = constructUserMessageWithEditorContent(chatMessageContext);
        }

        return userMessage;
    }

    private @NotNull UserMessage constructUserMessageWithEditorContent(@NotNull ChatMessageContext chatMessageContext) {
        StringBuilder stringBuilder = new StringBuilder();

        // The user prompt is always added
        appendIfNotEmpty(stringBuilder, chatMessageContext.getUserPrompt());


        // Add the editor content or selected text
        String editorContent = getEditorContentOrSelectedText(chatMessageContext);

        if (!editorContent.isEmpty()) {
            // Add the context prompt if it is not empty
            appendIfNotEmpty(stringBuilder, CONTEXT_PROMPT);
            appendIfNotEmpty(stringBuilder, editorContent);
        }

        if (DevoxxGenieSettingsServiceProvider.getInstance().getAstMode()) {
            addASTContext(chatMessageContext, stringBuilder);
        }

        UserMessage userMessage = new UserMessage(stringBuilder.toString());
        chatMessageContext.setUserMessage(userMessage);
        return userMessage;
    }

    private @NotNull String getEditorContentOrSelectedText(@NotNull ChatMessageContext chatMessageContext) {
        EditorInfo editorInfo = chatMessageContext.getEditorInfo();
        if (editorInfo == null) {
            return "";
        }

        StringBuilder contentBuilder = new StringBuilder();

        // Add selected text if present
        if (editorInfo.getSelectedText() != null && !editorInfo.getSelectedText().isEmpty()) {
            contentBuilder.append("Selected Text:\n")
                .append(editorInfo.getSelectedText())
                .append("\n\n");
        }

        // Add content of selected files
        List<VirtualFile> selectedFiles = editorInfo.getSelectedFiles();
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            contentBuilder.append("File Contents:\n");
            for (VirtualFile file : selectedFiles) {
                contentBuilder.append("File: ").append(file.getName()).append("\n")
                    .append(readFileContent(file))
                    .append("\n\n");
            }
        }

        return contentBuilder.toString().trim();
    }

    private @NotNull String readFileContent(@NotNull VirtualFile file) {
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * Construct user message with full context.
     * @param chatMessageContext the chat message context
     * @param context the context
     * @return the user message
     */
    private @NotNull UserMessage constructUserMessageWithFullContext(@NotNull ChatMessageContext chatMessageContext,
                                                                     String context) {
        StringBuilder stringBuilder = new StringBuilder();

        // Check if this is the first message in the conversation
        if (ChatMemoryService.getInstance().messages().size() == 1) {
            stringBuilder.append(context);
            stringBuilder.append("\n\n");
            stringBuilder.append("=========================================\n\n");
        }

        stringBuilder.append("User Question: ");
        stringBuilder.append(chatMessageContext.getUserPrompt());

        UserMessage userMessage = new UserMessage("user_message", stringBuilder.toString());
        chatMessageContext.setUserMessage(userMessage);
        return userMessage;
    }

    /**
     * Create user prompt with context.
     * @param project    the project
     * @param userPrompt the user prompt
     * @param files      the files
     * @return the user prompt with context
     */
    public @NotNull CompletableFuture<String> createUserPromptWithContextAsync(Project project,
                                                                               String userPrompt,
                                                                               @NotNull List<VirtualFile> files) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder userPromptContext = new StringBuilder();
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();

            for (VirtualFile file : files) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    if (file.getFileType().getName().equals("UNKNOWN")) {
                        userPromptContext.append("Filename: ").append(file.getName()).append("\n");
                        userPromptContext.append("Code Snippet: ").append(file.getUserData(SELECTED_TEXT_KEY)).append("\n");
                    } else {
                        Document document = fileDocumentManager.getDocument(file);
                        if (document != null) {
                            userPromptContext.append("Filename: ").append(file.getName()).append("\n");
                            String content = document.getText();
                            userPromptContext.append(content).append("\n");
                        } else {
                            NotificationUtil.sendNotification(project, "Error reading file: " + file.getName());
                        }
                    }
                });
            }

            userPromptContext.append(userPrompt);
            return userPromptContext.toString();
        });
    }

    /**
     * Add AST prompt context (selected code snippet) to the chat message.
     * @param chatMessageContext the chat message context
     * @param sb                 the string builder
     */
    private void addASTContext(@NotNull ChatMessageContext chatMessageContext,
                                      @NotNull StringBuilder sb) {
        sb.append("\n\nRelated classes:\n\n");
        List<VirtualFile> tempFiles = new ArrayList<>();

        chatMessageContext.getEditorInfo().getSelectedFiles().forEach(file ->
            PSIAnalyzerService.getInstance().analyze(chatMessageContext.getProject(), file)
                .ifPresent(psiClasses ->
                    psiClasses.forEach(psiClass -> {
                        tempFiles.add(psiClass.getContainingFile().getVirtualFile());
                        sb.append(psiClass.getText()).append("\n");
                    })));

        chatMessageContext.getEditorInfo().getSelectedFiles().addAll(tempFiles);
    }

    /**
     * Append the text to the string builder if it is not empty.
     * @param sb   the string builder
     * @param text the text
     */
    private void appendIfNotEmpty(StringBuilder sb, String text) {
        if (text != null && !text.isEmpty()) {
            sb.append(text).append("\n");
        }
    }
}
