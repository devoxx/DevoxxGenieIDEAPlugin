package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.BacklogDocument;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Executes the 5 backlog document tools: list, view, create, update, search.
 */
@Slf4j
public class BacklogDocumentToolExecutor implements ToolExecutor {

    private final Project project;

    public BacklogDocumentToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            return switch (request.name()) {
                case "backlog_document_list" -> listDocuments(request.arguments());
                case "backlog_document_view" -> viewDocument(request.arguments());
                case "backlog_document_create" -> createDocument(request.arguments());
                case "backlog_document_update" -> updateDocument(request.arguments());
                case "backlog_document_search" -> searchDocuments(request.arguments());
                default -> "Error: Unknown document tool: " + request.name();
            };
        } catch (Exception e) {
            log.warn("Error executing backlog document tool: {}", request.name(), e);
            return "Error: " + e.getMessage();
        }
    }

    private @NotNull String listDocuments(@NotNull String arguments) {
        String search = ToolArgumentParser.getString(arguments, "search");

        SpecService specService = SpecService.getInstance(project);
        List<BacklogDocument> docs = specService.listDocuments(search);

        if (docs.isEmpty()) {
            return "No documents found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Documents (").append(docs.size()).append("):\n\n");
        for (BacklogDocument doc : docs) {
            sb.append("- ").append(doc.getId());
            if (doc.getTitle() != null) {
                sb.append(": ").append(doc.getTitle());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private @NotNull String viewDocument(@NotNull String arguments) {
        String id = ToolArgumentParser.getString(arguments, "id");
        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        BacklogDocument doc = specService.getDocument(id);

        if (doc == null) {
            return "Error: Document with ID '" + id + "' not found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(doc.getTitle() != null ? doc.getTitle() : "Untitled").append("\n");
        sb.append("ID: ").append(doc.getId()).append("\n\n");
        if (doc.getContent() != null) {
            sb.append(doc.getContent());
        }

        return sb.toString();
    }

    private @NotNull String createDocument(@NotNull String arguments) throws Exception {
        String title = ToolArgumentParser.getString(arguments, "title");
        String content = ToolArgumentParser.getString(arguments, "content");

        if (title == null || title.isEmpty()) {
            return "Error: 'title' parameter is required.";
        }
        if (content == null || content.isEmpty()) {
            return "Error: 'content' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        BacklogDocument doc = specService.createDocument(title, content);

        return "Created document " + doc.getId() + ": " + doc.getTitle() + "\nFile: " + doc.getFilePath();
    }

    private @NotNull String updateDocument(@NotNull String arguments) throws Exception {
        String id = ToolArgumentParser.getString(arguments, "id");
        String content = ToolArgumentParser.getString(arguments, "content");
        String title = ToolArgumentParser.getString(arguments, "title");

        if (id == null || id.isEmpty()) {
            return "Error: 'id' parameter is required.";
        }
        if (content == null || content.isEmpty()) {
            return "Error: 'content' parameter is required.";
        }

        SpecService specService = SpecService.getInstance(project);
        specService.updateDocument(id, content, title);

        return "Updated document " + id + " successfully.";
    }

    private @NotNull String searchDocuments(@NotNull String arguments) {
        String query = ToolArgumentParser.getString(arguments, "query");
        if (query == null || query.isEmpty()) {
            return "Error: 'query' parameter is required.";
        }

        int limit = ToolArgumentParser.getInt(arguments, "limit", 0);

        SpecService specService = SpecService.getInstance(project);
        List<BacklogDocument> results = specService.searchDocuments(query, limit);

        if (results.isEmpty()) {
            return "No documents found matching query: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Search results (").append(results.size()).append("):\n\n");
        for (BacklogDocument doc : results) {
            sb.append("- ").append(doc.getId());
            if (doc.getTitle() != null) {
                sb.append(": ").append(doc.getTitle());
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
