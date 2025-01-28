package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devoxx.genie.action.AddSnippetAction.SELECTED_TEXT_KEY;

/**
 * The message creation service for user and system messages.
 * Here's where also the basic prompt "engineering" is happening, including calling the AST magic.
 */
public class MessageCreationService {

    private static final Logger LOG = Logger.getInstance(MessageCreationService.class.getName());

    private static final String GIT_DIFF_INSTRUCTIONS = """
            Please analyze the code and provide ONLY the modified code in your response.
            Do not include any explanations or comments.
            The response should contain just the modified code wrapped in a code block using the appropriate language identifier.
            If multiple files need to be modified, provide each file's content in a separate code block.
            """;

    public static final String SEMANTIC_RESULT = """
            File: %s
            Score: %.2f
            ```java
            %s
            ```
            """;

    @NotNull
    public static MessageCreationService getInstance() {
        return ApplicationManager.getApplication().getService(MessageCreationService.class);
    }

    /**
     * Create user message.
     *
     * @param chatMessageContext the chat message context
     */
    public void addUserMessageToContext(@NotNull ChatMessageContext chatMessageContext) {
        String context = chatMessageContext.getFilesContext();
        if (context != null && !context.isEmpty()) {
            constructUserMessageWithFullContext(chatMessageContext, context);
        } else {
            constructUserMessageWithCombinedContext(chatMessageContext);
        }
    }

    /**
     * Construct a user message with full context.
     *
     * @param chatMessageContext the chat message context
     * @param context            the context
     */
    private void constructUserMessageWithFullContext(@NotNull ChatMessageContext chatMessageContext,
                                                     String context) {
        LOG.debug("Constructing user message with full context");
        StringBuilder stringBuilder = new StringBuilder();

        // If git diff is enabled, add special instructions at the beginning
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseSimpleDiff())) {
            stringBuilder.append("<DiffInstructions>").append(GIT_DIFF_INSTRUCTIONS).append("</DiffInstructions>\n\n");
        }

        stringBuilder.append("<Context>");
        stringBuilder.append(context);
        stringBuilder.append("</Context>\n\n");

        stringBuilder.append("<UserPrompt>");
        stringBuilder.append(chatMessageContext.getUserPrompt());
        stringBuilder.append("</UserPrompt>");

        chatMessageContext.setUserMessage(UserMessage.from(stringBuilder.toString()));
    }

    private void constructUserMessageWithCombinedContext(@NotNull ChatMessageContext chatMessageContext) {
        LOG.debug("Constructing user message with combined context");

        StringBuilder stringBuilder = new StringBuilder();

        // Add system prompt for OpenAI o1 models
        if (ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
            String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt();
            stringBuilder.append("<SystemPrompt>").append(systemPrompt).append("</SystemPrompt>\n\n");
        }

        // If git diff is enabled, add special instructions
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getGitDiffActivated())) {
            // Git diff is enabled, add special instructions at the beginning
            stringBuilder.append("<DiffInstructions>").append(GIT_DIFF_INSTRUCTIONS).append("</DiffInstructions>\n\n");
        } else if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            // Semantic search is enabled, add search results
            String semanticContext = addSemanticSearchResults(chatMessageContext);
            if (!semanticContext.isEmpty()) {
                stringBuilder.append("<SemanticContext>\n");
                stringBuilder.append(semanticContext);
                stringBuilder.append("\n</SemanticContext>");
            }
        }

        // Add the user's prompt
        stringBuilder.append("<UserPrompt>\n").append(chatMessageContext.getUserPrompt()).append("\n</UserPrompt>\n\n");

        // Add editor content or selected text
        String editorContent = getEditorContentOrSelectedText(chatMessageContext);
        if (!editorContent.isEmpty()) {
            stringBuilder.append(editorContent);
        }

        chatMessageContext.setUserMessage(UserMessage.from(stringBuilder.toString()));
    }

    /**
     * Create user message with project content based on semantic search results.
     *
     * @param chatMessageContext the chat message context
     * @return the user message
     */
    private @NotNull String addSemanticSearchResults(@NotNull ChatMessageContext chatMessageContext) {
        LOG.debug("Adding semantic search results to user message");

        StringBuilder contextBuilder = new StringBuilder();

        try {
            SemanticSearchService semanticSearchService = SemanticSearchService.getInstance();

            // Get semantic search results from ChromaDB
            Map<String, SearchResult> searchResults =
                    semanticSearchService.search(chatMessageContext.getProject(), chatMessageContext.getUserPrompt());

            if (!searchResults.isEmpty()) {
                List<SemanticFile> fileReferences = extractFileReferences(searchResults);

                // Store references in chat message context for UI use
                chatMessageContext.setSemanticReferences(fileReferences);

                contextBuilder.append("Referenced files:\n");
                fileReferences.forEach(file -> contextBuilder.append("- ").append(file).append("\n"));
                contextBuilder.append("\n");

                Set<Map.Entry<String, SearchResult>> entries = searchResults.entrySet();
                // Format search results
                String formattedResults = entries.stream()
                        .map(MessageCreationService::getFileContent)
                        .collect(Collectors.joining("\n"));

                contextBuilder.append(formattedResults);

                // Log the number of relevant snippets found
                NotificationUtil.sendNotification(
                        chatMessageContext.getProject(),
                        String.format("Found %d relevant project file%s using RAG", searchResults.size(), searchResults.size() > 1 ? "s" : "")
                );
            }
        } catch (Exception e) {
            LOG.warn("Failed to get semantic search results: " + e.getMessage());
        }

        return contextBuilder.toString();
    }

    private static @NotNull String getFileContent(Map.@NotNull Entry<String, SearchResult> entry) {
        String fileContent;
        try {
            fileContent = Files.readString(Paths.get(entry.getKey()));
        } catch (IOException e) {
            return "";
        }
        return SEMANTIC_RESULT.formatted(entry.getKey(), entry.getValue().score(), fileContent);
    }

    public static List<SemanticFile> extractFileReferences(@NotNull Map<String, SearchResult> searchResults) {
        return searchResults.keySet().stream()
                .map(value -> new SemanticFile(value, searchResults.get(value).score()))
                .toList();
    }

    /**
     * Get the editor content or selected text.
     *
     * @param chatMessageContext the chat message context
     * @return the editor content or selected text
     */
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
                    .append("\n</SelectedText>\n\n");
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
     * Create attached files context.
     *
     * @param project the project
     * @param files   the files
     * @return the user prompt with context
     */
    public @NotNull String createAttachedFilesContext(Project project,
                                                      @NotNull List<VirtualFile> files) {
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
                        NotificationUtil.sendNotification(project, "File type not supported: " + file.getName());
                    }
                }
            });
        }

        return userPromptContext.toString();
    }
}


