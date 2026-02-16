package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EditFileToolExecutorTest {

    @Mock
    private Project project;

    private EditFileToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new EditFileToolExecutor(project);
    }

    // --- execute() input validation tests ---

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_blankPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"  \", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_missingOldString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("old_string");
    }

    @Test
    void execute_emptyOldString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("old_string").contains("empty");
    }

    @Test
    void execute_missingNewString_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"foo\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("new_string");
    }

    @Test
    void execute_identicalStrings_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"same\", \"new_string\": \"same\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("identical");
    }

    @Test
    void execute_pathTraversal_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"../../../etc/passwd\", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_pathWithDoubleDotsInMiddle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"src/../../secret.txt\", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error");
    }

    // --- execute() end-to-end tests (with mocked ApplicationManager) ---

    @Test
    void execute_validRequest_editsFileSuccessfully() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"hello\", \"new_string\": \"goodbye\"}")
                .build();

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class);
             MockedStatic<WriteCommandAction> wcaMock = mockStatic(WriteCommandAction.class)) {

            Application app = mock(Application.class);
            appMock.when(ApplicationManager::getApplication).thenReturn(app);
            doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(app).invokeAndWait(any(Runnable.class));

            wcaMock.when(() -> WriteCommandAction.runWriteCommandAction(eq(project), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        ((Runnable) invocation.getArgument(1)).run();
                        return null;
                    });

            String result = testExecutor.execute(request, null);
            assertThat(result).contains("Successfully edited").contains("test.txt");
        }
    }

    @Test
    void execute_applicationManagerThrows_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("edit_file")
                .arguments("{\"path\": \"test.txt\", \"old_string\": \"foo\", \"new_string\": \"bar\"}")
                .build();

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class)) {
            appMock.when(ApplicationManager::getApplication)
                    .thenThrow(new RuntimeException("No application"));

            String result = executor.execute(request, null);
            assertThat(result).contains("Error").contains("Failed to edit file");
        }
    }

    // --- editFile() tests ---

    @Test
    void editFile_nullProjectBase_returnsError() {
        EditFileToolExecutor testExecutor = createTestableExecutor(null, null);

        String result = testExecutor.editFile("test.txt", "old", "new", false);
        assertThat(result).contains("Error").contains("Project base directory not found");
    }

    @Test
    void editFile_fileNotFound_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);
        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, null);

        String result = testExecutor.editFile("missing.txt", "old", "new", false);
        assertThat(result).contains("Error").contains("File not found").contains("missing.txt");
    }

    @Test
    void editFile_fileIsDirectory_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile dir = mock(VirtualFile.class);
        when(dir.exists()).thenReturn(true);
        when(dir.isDirectory()).thenReturn(true);

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, dir);

        String result = testExecutor.editFile("src", "old", "new", false);
        assertThat(result).contains("Error").contains("directory").contains("src");
    }

    @Test
    void editFile_pathOutsideProject_returnsAccessDenied() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("content");

        EditFileToolExecutor testExecutor = new EditFileToolExecutor(project) {
            @Override VirtualFile getProjectBaseDir() { return projectBase; }
            @Override VirtualFile findFile(VirtualFile base, String path) { return file; }
            @Override boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) { return false; }
        };

        String result = testExecutor.editFile("file.txt", "old", "new", false);
        assertThat(result).contains("Error").contains("Access denied").contains("outside the project root");
    }

    @Test
    void editFile_oldStringNotFound_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "xyz", "abc", false);
        assertThat(result).contains("Error").contains("old_string was not found").contains("test.txt");
    }

    @Test
    void editFile_singleOccurrence_replacesSuccessfully() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "hello", "goodbye", false);
        assertThat(result).contains("Successfully edited").contains("test.txt");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("goodbye world");
    }

    @Test
    void editFile_multipleOccurrences_noReplaceAll_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("foo bar foo baz foo");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "foo", "qux", false);
        assertThat(result).contains("Error").contains("3 occurrences").contains("test.txt")
                .contains("replace_all");
    }

    @Test
    void editFile_multipleOccurrences_withReplaceAll_replacesAll() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("foo bar foo baz foo");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "foo", "qux", true);
        assertThat(result).contains("Successfully replaced").contains("3 occurrences");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("qux bar qux baz qux");
    }

    @Test
    void editFile_singleOccurrence_withReplaceAll_reportsEditedNotReplaced() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "hello", "goodbye", true);
        // With replaceAll=true but only 1 occurrence, should say "edited" not "replaced N occurrences"
        assertThat(result).contains("Successfully edited").contains("test.txt");
    }

    @Test
    void editFile_replacementAtBeginning_works() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("START middle end");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "START", "BEGIN", false);
        assertThat(result).contains("Successfully edited");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("BEGIN middle end");
    }

    @Test
    void editFile_replacementAtEnd_works() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("start middle END");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "END", "FINISH", false);
        assertThat(result).contains("Successfully edited");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("start middle FINISH");
    }

    @Test
    void editFile_replaceWithEmpty_deletesOldString() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", " world", "", false);
        assertThat(result).contains("Successfully edited");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void editFile_multilineContent_works() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("line1\nline2\nline3\n");

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "line2", "replaced", false);
        assertThat(result).contains("Successfully edited");

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(file).setBinaryContent(captor.capture());
        assertThat(new String(captor.getValue(), StandardCharsets.UTF_8)).isEqualTo("line1\nreplaced\nline3\n");
    }

    @Test
    void editFile_setBinaryContentThrows_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFileWithContent("hello world");
        doThrow(new IOException("Disk full")).when(file).setBinaryContent(any());

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "hello", "goodbye", false);
        assertThat(result).contains("Error").contains("Failed to edit file").contains("Disk full");
    }

    @Test
    void editFile_contentsToByteArrayThrows_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = mock(VirtualFile.class);
        when(file.exists()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);
        when(file.contentsToByteArray()).thenThrow(new IOException("Cannot read"));

        EditFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.editFile("test.txt", "old", "new", false);
        assertThat(result).contains("Error").contains("Failed to edit file").contains("Cannot read");
    }

    // --- countOccurrences() tests ---

    @Test
    void countOccurrences_noMatch_returnsZero() {
        assertThat(EditFileToolExecutor.countOccurrences("hello world", "xyz")).isZero();
    }

    @Test
    void countOccurrences_singleMatch_returnsOne() {
        assertThat(EditFileToolExecutor.countOccurrences("hello world", "hello")).isEqualTo(1);
    }

    @Test
    void countOccurrences_multipleMatches_returnsCount() {
        assertThat(EditFileToolExecutor.countOccurrences("foo bar foo baz foo", "foo")).isEqualTo(3);
    }

    @Test
    void countOccurrences_overlappingNotCounted() {
        // "aaa" searching for "aa" should find 1 (non-overlapping), since idx advances by search.length()
        assertThat(EditFileToolExecutor.countOccurrences("aaa", "aa")).isEqualTo(1);
    }

    @Test
    void countOccurrences_emptyContent_returnsZero() {
        assertThat(EditFileToolExecutor.countOccurrences("", "foo")).isZero();
    }

    @Test
    void countOccurrences_matchAtBoundaries_countsCorrectly() {
        assertThat(EditFileToolExecutor.countOccurrences("xyzxyz", "xyz")).isEqualTo(2);
    }

    // --- Helper methods ---

    private VirtualFile createMockFileWithContent(String content) throws IOException {
        VirtualFile file = mock(VirtualFile.class);
        when(file.exists()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);
        when(file.contentsToByteArray()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private EditFileToolExecutor createTestableExecutor(VirtualFile projectBase, VirtualFile file) {
        return new EditFileToolExecutor(project) {
            @Override VirtualFile getProjectBaseDir() { return projectBase; }
            @Override VirtualFile findFile(VirtualFile base, String path) { return file; }
            @Override boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) { return true; }
        };
    }
}
