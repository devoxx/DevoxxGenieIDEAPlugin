package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ReadFileToolExecutorTest {

    @Mock
    private Project project;

    private ReadFileToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ReadFileToolExecutor(project);
    }

    // --- execute() input validation tests ---

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_emptyPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_blankPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"   \"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_nullPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": null}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error");
    }

    // --- execute() end-to-end tests (with mocked ReadAction) ---

    @SuppressWarnings("unchecked")
    @Test
    void execute_validRequest_readsFileSuccessfully() throws Exception {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFile("file content here");

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"test.txt\"}")
                .build();

        try (MockedStatic<ReadAction> readActionMock = mockStatic(ReadAction.class)) {
            readActionMock.when(() -> ReadAction.compute(any(ThrowableComputable.class)))
                    .thenAnswer(invocation -> {
                        ThrowableComputable<String, Exception> computable = invocation.getArgument(0);
                        return computable.compute();
                    });

            String result = testExecutor.execute(request, null);
            assertThat(result).isEqualTo("file content here");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void execute_readActionThrows_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"test.txt\"}")
                .build();

        try (MockedStatic<ReadAction> readActionMock = mockStatic(ReadAction.class)) {
            readActionMock.when(() -> ReadAction.compute(any(ThrowableComputable.class)))
                    .thenThrow(new RuntimeException("Read lock failed"));

            String result = executor.execute(request, null);
            assertThat(result).contains("Error").contains("Failed to read file");
        }
    }

    // --- readFile() tests ---

    @Test
    void readFile_nullProjectBase_returnsError() {
        ReadFileToolExecutor testExecutor = createTestableExecutor(null, null);

        String result = testExecutor.readFile("test.txt");
        assertThat(result).contains("Error").contains("Project base directory not found");
    }

    @Test
    void readFile_fileNotFound_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);
        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, null);

        String result = testExecutor.readFile("missing.txt");
        assertThat(result).contains("Error").contains("File not found").contains("missing.txt");
    }

    @Test
    void readFile_pathOutsideProject_returnsAccessDenied() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFile("content");

        ReadFileToolExecutor testExecutor = new ReadFileToolExecutor(project) {
            @Override VirtualFile getProjectBaseDir() { return projectBase; }
            @Override VirtualFile findFile(VirtualFile base, String path) { return file; }
            @Override boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) { return false; }
        };

        String result = testExecutor.readFile("file.txt");
        assertThat(result).contains("Error").contains("Access denied").contains("outside the project root");
    }

    @Test
    void readFile_pathIsDirectory_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile dir = mock(VirtualFile.class);
        when(dir.isDirectory()).thenReturn(true);

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, dir);

        String result = testExecutor.readFile("src");
        assertThat(result).contains("Error").contains("directory").contains("src");
    }

    @Test
    void readFile_validFile_returnsContent() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFile("hello world");

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("test.txt");
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void readFile_emptyFile_returnsEmptyString() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFile("");

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("empty.txt");
        assertThat(result).isEmpty();
    }

    @Test
    void readFile_multilineContent_returnsFullContent() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        String content = "line1\nline2\nline3\n";
        VirtualFile file = createMockFile(content);

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("multiline.txt");
        assertThat(result).isEqualTo(content);
    }

    @Test
    void readFile_unicodeContent_returnsCorrectly() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        String content = "Hello \u00e9\u00e8\u00ea \u4e16\u754c \ud83d\ude00";
        VirtualFile file = createMockFile(content);

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("unicode.txt");
        assertThat(result).isEqualTo(content);
    }

    @Test
    void readFile_contentsToByteArrayThrows_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = mock(VirtualFile.class);
        when(file.isDirectory()).thenReturn(false);
        when(file.contentsToByteArray()).thenThrow(new IOException("Cannot read"));

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("broken.txt");
        assertThat(result).contains("Error").contains("Failed to read file").contains("Cannot read");
    }

    @Test
    void readFile_largeContent_returnsFullContent() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        String content = "x".repeat(50000);
        VirtualFile file = createMockFile(content);

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("large.txt");
        assertThat(result).hasSize(50000);
    }

    @Test
    void readFile_fileWithSpecialChars_returnsContent() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        String content = "tab\there\nnewline\r\nwindows\rold-mac";
        VirtualFile file = createMockFile(content);

        ReadFileToolExecutor testExecutor = createTestableExecutor(projectBase, file);

        String result = testExecutor.readFile("special.txt");
        assertThat(result).isEqualTo(content);
    }

    // --- Helper methods ---

    private VirtualFile createMockFile(String content) throws IOException {
        VirtualFile file = mock(VirtualFile.class);
        when(file.isDirectory()).thenReturn(false);
        when(file.contentsToByteArray()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private ReadFileToolExecutor createTestableExecutor(VirtualFile projectBase, VirtualFile file) {
        return new ReadFileToolExecutor(project) {
            @Override VirtualFile getProjectBaseDir() { return projectBase; }
            @Override VirtualFile findFile(VirtualFile base, String path) { return file; }
            @Override boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) { return true; }
        };
    }
}
