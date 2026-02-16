package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.BacklogDocument;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BacklogDocumentToolExecutorTest {

    @Mock
    private Project project;

    @Mock
    private SpecService specService;

    private BacklogDocumentToolExecutor executor;
    private MockedStatic<SpecService> mockedSpecService;

    @BeforeEach
    void setUp() {
        mockedSpecService = Mockito.mockStatic(SpecService.class);
        mockedSpecService.when(() -> SpecService.getInstance(project)).thenReturn(specService);
        executor = new BacklogDocumentToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        mockedSpecService.close();
    }

    @Test
    void execute_unknownToolName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_unknown")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Unknown document tool");
    }

    // --- listDocuments ---

    @Test
    void listDocuments_noDocs_returnsEmptyMessage() {
        when(specService.listDocuments(any())).thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No documents found");
    }

    @Test
    void listDocuments_withDocs_returnsFormattedList() {
        BacklogDocument doc1 = BacklogDocument.builder().id("DOC-1").title("Design Doc").build();
        BacklogDocument doc2 = BacklogDocument.builder().id("DOC-2").title(null).build();
        when(specService.listDocuments(any())).thenReturn(List.of(doc1, doc2));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Documents (2)");
        assertThat(result).contains("DOC-1: Design Doc");
        assertThat(result).contains("- DOC-2");
    }

    @Test
    void listDocuments_withSearchFilter_passesFilter() {
        when(specService.listDocuments(eq("design"))).thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_list")
                .arguments("{\"search\": \"design\"}")
                .build();

        executor.execute(request, null);
        verify(specService).listDocuments("design");
    }

    // --- viewDocument ---

    @Test
    void viewDocument_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_view")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void viewDocument_notFound_returnsError() {
        when(specService.getDocument("DOC-99")).thenReturn(null);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_view")
                .arguments("{\"id\": \"DOC-99\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("DOC-99").contains("not found");
    }

    @Test
    void viewDocument_found_returnsFormattedContent() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-1").title("My Doc").content("Some content here").build();
        when(specService.getDocument("DOC-1")).thenReturn(doc);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_view")
                .arguments("{\"id\": \"DOC-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("# My Doc");
        assertThat(result).contains("ID: DOC-1");
        assertThat(result).contains("Some content here");
    }

    @Test
    void viewDocument_nullTitle_showsUntitled() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-1").title(null).content("Content").build();
        when(specService.getDocument("DOC-1")).thenReturn(doc);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_view")
                .arguments("{\"id\": \"DOC-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("# Untitled");
    }

    @Test
    void viewDocument_nullContent_showsNoContent() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-1").title("Doc").content(null).build();
        when(specService.getDocument("DOC-1")).thenReturn(doc);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_view")
                .arguments("{\"id\": \"DOC-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("# Doc");
        assertThat(result).contains("ID: DOC-1");
        // Should not throw, just no content section
    }

    // --- createDocument ---

    @Test
    void createDocument_missingTitle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_create")
                .arguments("{\"content\": \"some content\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("title");
    }

    @Test
    void createDocument_missingContent_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_create")
                .arguments("{\"title\": \"My Doc\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("content");
    }

    @Test
    void createDocument_success() throws Exception {
        BacklogDocument created = BacklogDocument.builder()
                .id("DOC-1").title("My Doc").filePath("/backlog/docs/doc-1.md").build();
        when(specService.createDocument("My Doc", "Document content")).thenReturn(created);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_create")
                .arguments("{\"title\": \"My Doc\", \"content\": \"Document content\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Created document DOC-1");
        assertThat(result).contains("My Doc");
        assertThat(result).contains("/backlog/docs/doc-1.md");
    }

    // --- updateDocument ---

    @Test
    void updateDocument_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_update")
                .arguments("{\"content\": \"new content\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void updateDocument_missingContent_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_update")
                .arguments("{\"id\": \"DOC-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("content");
    }

    @Test
    void updateDocument_success() throws Exception {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_update")
                .arguments("{\"id\": \"DOC-1\", \"content\": \"Updated content\", \"title\": \"New Title\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Updated document DOC-1 successfully");
        verify(specService).updateDocument("DOC-1", "Updated content", "New Title");
    }

    // --- searchDocuments ---

    @Test
    void searchDocuments_missingQuery_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_search")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("query");
    }

    @Test
    void searchDocuments_noResults_returnsNotFoundMessage() {
        when(specService.searchDocuments(eq("nothing"), anyInt())).thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_search")
                .arguments("{\"query\": \"nothing\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No documents found matching query: nothing");
    }

    @Test
    void searchDocuments_withResults_returnsFormattedList() {
        BacklogDocument doc = BacklogDocument.builder().id("DOC-1").title("Design Doc").build();
        when(specService.searchDocuments(eq("design"), anyInt())).thenReturn(List.of(doc));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_search")
                .arguments("{\"query\": \"design\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Search results (1)");
        assertThat(result).contains("DOC-1: Design Doc");
    }

    @Test
    void execute_exceptionInHandler_returnsErrorMessage() throws Exception {
        when(specService.createDocument(any(), any())).thenThrow(new RuntimeException("disk full"));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_document_create")
                .arguments("{\"title\": \"Doc\", \"content\": \"Content\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("disk full");
    }
}
