package com.devoxx.genie.service;

import com.devoxx.genie.model.rag.RAGLogMessage;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.rag.RAGEventPublisher;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static com.devoxx.genie.action.AddSnippetAction.SELECTED_TEXT_KEY;

/**
 * The message creation service for user and system messages.
 * Here's where also the basic prompt "engineering" is happening, including calling the AST magic.
 */
@Slf4j
public class MessageCreationService {

    public static final String SEMANTIC_RESULT = """
            File: %s%n
            Score: %.2f%n
            ```%s%n
            %s%n
            ```%n
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

        // Load deferred attached files content (runs on background thread, not EDT)
        resolvePendingAttachedFiles(chatMessageContext);

        String context = chatMessageContext.getFilesContext();
        if (context != null && !context.isEmpty()) {
            constructUserMessageWithFullContext(chatMessageContext, context);
        } else {
            constructUserMessageWithCombinedContext(chatMessageContext);
        }
        addImages(chatMessageContext);
    }

    private void resolvePendingAttachedFiles(@NotNull ChatMessageContext chatMessageContext) {
        List<VirtualFile> pending = chatMessageContext.getPendingAttachedFiles();
        if (pending != null && !pending.isEmpty()
                && (chatMessageContext.getFilesContext() == null || chatMessageContext.getFilesContext().isEmpty())) {
            String context = createAttachedFilesContext(chatMessageContext.getProject(), pending);
            chatMessageContext.setFilesContext(context);
            chatMessageContext.setPendingAttachedFiles(null);
        }
    }

    private void addImages(@NotNull ChatMessageContext chatMessageContext) {
        List<VirtualFile> imageFiles = FileListManager.getInstance().getImageFiles(chatMessageContext.getProject(), chatMessageContext.getTabId());
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

    /** Queries shorter than this are treated as follow-up clarifications ("explain", "more?",
     *  "and?") that should reuse the previous turn's context rather than triggering a new RAG
     *  retrieval. This keeps the prompt-cache hot for chatty back-and-forth and saves an
     *  Ollama embedding call per follow-up. */
    static final int RAG_MIN_QUERY_LENGTH = 15;

    private void constructUserMessageWithCombinedContext(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Constructing user message with combined context");

        StringBuilder stringBuilder = new StringBuilder();

        // Add system prompt for OpenAI o1 models
        if (ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
            String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt();
            stringBuilder.append("<SystemPrompt>").append(systemPrompt).append("</SystemPrompt>\n\n");
        }

        // NOTE: DEVOXXGENIE.md and CLAUDE.md/AGENTS.md content is now included in the system prompt
        // (set once per conversation in ChatMemoryManager.buildSystemPrompt()) rather than repeated
        // in every user message.

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

        // Retrieval context immediately precedes the user prompt. Two reasons:
        //   1. Providers that hit prompt caches (Anthropic, OpenAI) only cache the stable
        //      prefix; varying retrieval content at the top defeats that cache for everyone.
        //   2. Many local models pay more attention to whatever is closest to the user
        //      question — keeping RAG hits adjacent to the prompt improves grounding.
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated())
                && shouldRunRagFor(chatMessageContext.getUserPrompt())) {
            String semanticContext = addSemanticSearchResults(chatMessageContext);
            if (!semanticContext.isEmpty()) {
                stringBuilder.append("<SemanticContext>\n");
                stringBuilder.append(semanticContext);
                stringBuilder.append("\n</SemanticContext>");
            }
        }

        // Add the user's prompt, this MUST BE at the bottom of the prompt for some local models to understand!
        stringBuilder.append("<UserPrompt>\n").append(chatMessageContext.getUserPrompt()).append("\n</UserPrompt>\n\n");

        chatMessageContext.setUserMessage(UserMessage.from(stringBuilder.toString()));
    }

    /**
     * Decide whether to run a RAG retrieval for {@code userPrompt}. Skips very short
     * follow-up-style messages ({@code "more?"}, {@code "explain"}, {@code "why?"}) where
     * the LLM should rely on the prior turn's context instead. Visible for tests.
     */
    static boolean shouldRunRagFor(String userPrompt) {
        if (userPrompt == null) return false;
        return userPrompt.trim().length() >= RAG_MIN_QUERY_LENGTH;
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
        String userPrompt = chatMessageContext.getUserPrompt();
        long startNanos = System.nanoTime();
        List<SearchResult> searchResults = new ArrayList<>();

        try {
            SemanticSearchService semanticSearchService = SemanticSearchService.getInstance();
            searchResults = semanticSearchService.search(chatMessageContext.getProject(), userPrompt);

            // Task-209: emit feature_used with the real provider_type from the active model.
            com.devoxx.genie.service.analytics.FeatureUsageTracker.semanticSearchUsed(
                    chatMessageContext.getLanguageModel());

            if (!searchResults.isEmpty()) {
                List<SemanticFile> fileReferences = extractFileReferences(searchResults);
                chatMessageContext.setSemanticReferences(fileReferences);

                contextBuilder.append("Referenced files:\n");
                fileReferences.forEach(file -> contextBuilder.append("- ").append(file).append("\n"));
                contextBuilder.append("\n");

                String formattedResults = searchResults.stream()
                        .map(MessageCreationService::formatSemanticResult)
                        .collect(Collectors.joining("\n"));

                contextBuilder.append(formattedResults);

                long uniqueFiles = fileReferences.stream().map(SemanticFile::filePath).distinct().count();
                NotificationUtil.sendNotification(
                        chatMessageContext.getProject(),
                        String.format("Found %d relevant project file%s using RAG — see DevoxxGenie Logs (Show RAG Only) for details",
                                uniqueFiles, uniqueFiles > 1 ? "s" : "")
                );
            }
        } catch (Exception e) {
            log.warn("Failed to get semantic search results: " + e.getMessage());
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            RAGEventPublisher.publish(chatMessageContext.getProject(), userPrompt, searchResults, durationMs);
        }

        return contextBuilder.toString();
    }

    /**
     * Format a single search hit for inclusion in the prompt. Uses the chunk text returned
     * by the vector store — does NOT re-read the full file from disk.
     */
    private static @NotNull String formatSemanticResult(@NotNull SearchResult result) {
        String filePath = result.filePath() != null ? result.filePath() : "(unknown)";
        return SEMANTIC_RESULT.formatted(filePath, result.score(), inferFenceLanguage(filePath), result.content());
    }

    private static @NotNull String inferFenceLanguage(@NotNull String filePath) {
        int dot = filePath.lastIndexOf('.');
        if (dot < 0 || dot == filePath.length() - 1) return "";
        String ext = filePath.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "java" -> "java";
            case "kt", "kts" -> "kotlin";
            case "py" -> "python";
            case "js", "mjs", "cjs" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "jsx" -> "jsx";
            case "go" -> "go";
            case "rs" -> "rust";
            case "cpp", "cc", "cxx", "hpp", "h" -> "cpp";
            case "c" -> "c";
            case "php" -> "php";
            case "rb" -> "ruby";
            case "scala" -> "scala";
            case "sh", "bash" -> "bash";
            case "sql" -> "sql";
            case "yml", "yaml" -> "yaml";
            case "json" -> "json";
            case "xml" -> "xml";
            case "html", "htm" -> "html";
            case "css" -> "css";
            case "md" -> "markdown";
            default -> ext;
        };
    }

    public static List<SemanticFile> extractFileReferences(@NotNull List<SearchResult> searchResults) {
        return searchResults.stream()
                .map(r -> new SemanticFile(r.filePath(), r.score()))
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


