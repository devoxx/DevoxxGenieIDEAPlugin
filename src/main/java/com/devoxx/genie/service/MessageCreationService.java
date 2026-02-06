package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.ImageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devoxx.genie.action.AddSnippetAction.SELECTED_TEXT_KEY;

/**
 * The message creation service for user and system messages.
 * Here's where also the basic prompt "engineering" is happening, including calling the AST magic.
 */
@Slf4j
public class MessageCreationService {

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
     * IMPORTANT: This method should be called only once per user message to avoid duplicates
     * in the chat memory. It is called by ChatMemoryManager.addUserMessage().
     *
     * @param chatMessageContext the chat message context
     */
    public void addUserMessageToContext(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Adding user message to context: userPrompt={}", chatMessageContext.getUserPrompt());

        // Check if user message already exists to prevent duplicates
        if (chatMessageContext.getUserMessage() != null) {
            // Message already exists, skip creating another one
            return;
        }

        String context = chatMessageContext.getFilesContext();
        if (context != null && !context.isEmpty()) {
            constructUserMessageWithFullContext(chatMessageContext, context);
        } else {
            constructUserMessageWithCombinedContext(chatMessageContext);
        }
        addImages(chatMessageContext);
    }

    private void addImages(@NotNull ChatMessageContext chatMessageContext) {
        List<VirtualFile> imageFiles = FileListManager.getInstance().getImageFiles(chatMessageContext.getProject());
        log.debug("addImages: found {} image files in FileListManager", imageFiles.size());

        if (!imageFiles.isEmpty()) {
            List<Content> contents = new ArrayList<>();
            contents.add(TextContent.from(chatMessageContext.getUserMessage().singleText()));

            for (VirtualFile imageFile : imageFiles) {
                try {
                    byte[] imageData = imageFile.contentsToByteArray();
                    String base64Image = Base64.getEncoder().encodeToString(imageData);
                    String mimeType = ImageUtil.getImageMimeType(imageFile);
                    contents.add(ImageContent.from(base64Image, mimeType));
                    log.debug("addImages: added image {} ({}, {} bytes)", imageFile.getName(), mimeType, imageData.length);
                } catch (IOException e) {
                    log.error("Failed to read image file: {}", imageFile.getName());
                }
            }

            chatMessageContext.setUserMessage(UserMessage.from(contents));
            log.debug("addImages: created multimodal UserMessage with {} contents (hasSingleText={})",
                    contents.size(), chatMessageContext.getUserMessage().hasSingleText());
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
        log.debug("Constructing user message with full context");
        StringBuilder stringBuilder = new StringBuilder();

        if (!context.isEmpty()) {
            stringBuilder.append("<Context>");
            stringBuilder.append(context);
            stringBuilder.append("</Context>\n");
        }

        stringBuilder.append("<UserPrompt>");
        stringBuilder.append(chatMessageContext.getUserPrompt());
        stringBuilder.append("</UserPrompt>");

        chatMessageContext.setUserMessage(UserMessage.from(stringBuilder.toString()));
    }

    private void constructUserMessageWithCombinedContext(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Constructing user message with combined context");

        StringBuilder stringBuilder = new StringBuilder();

        // Add system prompt for OpenAI o1 models
        if (ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
            String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt();
            stringBuilder.append("<SystemPrompt>").append(systemPrompt).append("</SystemPrompt>\n\n");
        }
        
        // Check if DEVOXXGENIE.md should be included in the prompt
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getUseDevoxxGenieMdInPrompt())) {
            // Try to read DEVOXXGENIE.md from project root
            String devoxxGenieMdContent = readDevoxxGenieMdFile(chatMessageContext.getProject());
            if (devoxxGenieMdContent != null && !devoxxGenieMdContent.isEmpty()) {
                stringBuilder.append("<ProjectContext>\n");
                stringBuilder.append(devoxxGenieMdContent);
                stringBuilder.append("\n</ProjectContext>\n\n");
            }
        }

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            // Semantic search is enabled, add search results
            String semanticContext = addSemanticSearchResults(chatMessageContext);
            if (!semanticContext.isEmpty()) {
                stringBuilder.append("<SemanticContext>\n");
                stringBuilder.append(semanticContext);
                stringBuilder.append("\n</SemanticContext>");
            }
        }

        if (MCPService.isMCPEnabled()) {
            // We'll add more info about the project path so tools can use this info.
            stringBuilder
                    .append("<ProjectPath>\n")
                    .append(chatMessageContext.getProject().getBasePath())
                    .append("</ProjectPath>");
        }

        // Add editor content or selected text
        String editorContent = getEditorContentOrSelectedText(chatMessageContext);
        if (!editorContent.isEmpty()) {
            stringBuilder.append(editorContent);
        }

        // Add the user's prompt, this MUST BE at the bottom of the prompt for some local models to understand!
        stringBuilder.append("<UserPrompt>\n").append(chatMessageContext.getUserPrompt()).append("\n</UserPrompt>\n\n");

        chatMessageContext.setUserMessage(UserMessage.from(stringBuilder.toString()));
    }

    /**
     * Create user message with project content based on semantic search results.
     *
     * @param chatMessageContext the chat message context
     * @return the user message
     */
    private @NotNull String addSemanticSearchResults(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Adding semantic search results to user message");

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
            log.warn("Failed to get semantic search results: " + e.getMessage());
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
        if (editorInfo == null ||
            Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getUseFileInEditor())) {
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
                contentBuilder.append("File: ").append(file.getCanonicalPath()).append("\n")
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
     * Read the content of DEVOXXGENIE.md file from the project root directory.
     *
     * @param project the project
     * @return the content of DEVOXXGENIE.md file or null if file not found or can't be read
     */
    private @Nullable String readDevoxxGenieMdFile(Project project) {
        try {
            if (project == null || project.getBasePath() == null) {
                log.warn("Project or base path is null");
                return null;
            }
            
            Path devoxxGenieMdPath = Paths.get(project.getBasePath(), "DEVOXXGENIE.md");
            if (!Files.exists(devoxxGenieMdPath)) {
                log.debug("DEVOXXGENIE.md file not found in project root: " + devoxxGenieMdPath);
                return null;
            }
            
            return Files.readString(devoxxGenieMdPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read DEVOXXGENIE.md file: " + e.getMessage());
            return null;
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
                    userPromptContext.append("File: ").append(file.getCanonicalPath()).append("\n");
                    userPromptContext.append("Code Snippet: ").append(file.getUserData(SELECTED_TEXT_KEY)).append("\n");
                } else {
                    Document document = fileDocumentManager.getDocument(file);
                    if (document != null) {
                        userPromptContext.append("Filename: ").append(file.getCanonicalPath()).append("\n");
                        String content = document.getText();
                        userPromptContext.append(content).append("\n");
                    } else if (!ImageUtil.isImageFile(file)){
                        NotificationUtil.sendNotification(project, "File type not supported: " + file.getName());
                    }
                }
            });
        }

        return userPromptContext.toString();
    }
}


