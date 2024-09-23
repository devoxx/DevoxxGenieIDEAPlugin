package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        if (context != null && !context.isEmpty()) {
            userMessage = constructUserMessageWithFullContext(chatMessageContext, context);
        } else {
            userMessage = constructUserMessageWithEditorContent(chatMessageContext);
        }

        return userMessage;
    }

    private @NotNull UserMessage constructUserMessageWithEditorContent(@NotNull ChatMessageContext chatMessageContext) {
        StringBuilder stringBuilder = new StringBuilder();

        // Add system prompt to user message if the AI model is o1
        if (ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
            String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt();
            stringBuilder.append("<SystemPrompt>").append(systemPrompt).append("</SystemPrompt>\n\n");
        }

        // The user prompt is always appended
        appendIfNotEmpty(stringBuilder, "<UserPrompt>" + chatMessageContext.getUserPrompt() + "</UserPrompt>");

        // Add the editor content or selected text
        String editorContent = getEditorContentOrSelectedText(chatMessageContext);

        if (!editorContent.isEmpty()) {
            // Add the context prompt if it is not empty
            appendIfNotEmpty(stringBuilder, "<context>");
            appendIfNotEmpty(stringBuilder, CONTEXT_PROMPT);
            appendIfNotEmpty(stringBuilder, editorContent);
            appendIfNotEmpty(stringBuilder, "</context>");
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
            contentBuilder.append("<SelectedText>\n")
                .append(editorInfo.getSelectedText())
                .append("</SelectedText>\n\n");
        }

        // Add content of selected files
        List<VirtualFile> selectedFiles = editorInfo.getSelectedFiles();
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            contentBuilder.append("<FileContents>\n");
            for (VirtualFile file : selectedFiles) {
                contentBuilder.append("File: ").append(file.getName()).append("\n")
                    .append(readFileContent(file))
                    .append("\n\n");
            }
            contentBuilder.append("\n</FileContents>\n");
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
     * Construct a user message with full context.
     *
     * @param chatMessageContext the chat message context
     * @param context            the context
     * @return the user message
     */
    private @NotNull UserMessage constructUserMessageWithFullContext(@NotNull ChatMessageContext chatMessageContext,
                                                                     String context) {
        StringBuilder stringBuilder = new StringBuilder();

        // Check if this is the first message in the conversation, if so add the context
        if (ChatMemoryService.getInstance().messages(chatMessageContext.getProject()).size() == 1) {
            stringBuilder.append("<Context>");
            stringBuilder.append(context);
            stringBuilder.append("</Context>\n\n");
            stringBuilder.append("=========================================\n\n");
        }

        stringBuilder.append("<UserPrompt>");
        stringBuilder.append("User Question: ");
        stringBuilder.append(chatMessageContext.getUserPrompt());
        stringBuilder.append("</UserPrompt>");

        UserMessage userMessage = new UserMessage("user_message", stringBuilder.toString());
        chatMessageContext.setUserMessage(userMessage);
        return userMessage;
    }

    /**
     * Create user prompt with context.
     *
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
     * Append the text to the string builder if it is not empty.
     *
     * @param sb   the string builder
     * @param text the text
     */
    private void appendIfNotEmpty(StringBuilder sb, String text) {
        if (text != null && !text.isEmpty()) {
            sb.append(text).append("\n");
        }
    }
}
