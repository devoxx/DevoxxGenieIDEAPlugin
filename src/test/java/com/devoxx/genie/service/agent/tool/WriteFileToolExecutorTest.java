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
class WriteFileToolExecutorTest {

    @Mock
    private Project project;

    private WriteFileToolExecutor executor;

    private WriteFileToolExecutor createTestableExecutor() {
        return new WriteFileToolExecutor(project) {
            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;  // Always allow in tests — access-denied tested separately
            }

            @Override
            VirtualFile resolveParentDir(String path, VirtualFile projectBase) {
                if (!path.contains("/")) {
                    return projectBase;
                }
                // Return a mock for subdirectory paths
                VirtualFile parentDir = mock(VirtualFile.class);
                when(parentDir.isDirectory()).thenReturn(true);
                return parentDir;
            }
        };
    }

    @BeforeEach
    void setUp() {
        executor = new WriteFileToolExecutor(project);
    }

    // --- execute() input validation tests ---

    @Test
    void execute_missingPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"content\": \"hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_nullPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": null, \"content\": \"hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_blankPath_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"   \", \"content\": \"hello\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_missingContent_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"test.txt\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("content");
    }

    @Test
    void execute_pathTraversal_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"../../../etc/passwd\", \"content\": \"pwned\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_pathWithDoubleDotsInMiddle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"src/../../secret.txt\", \"content\": \"data\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path traversal");
    }

    @Test
    void execute_emptyArguments_returnsPathError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("path");
    }

    @Test
    void execute_invalidJson_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("not json")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error");
    }

    // --- writeFile() tests ---

    @Test
    void writeFile_nullProjectBase_returnsError() {
        WriteFileToolExecutor testExecutor = new WriteFileToolExecutor(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return null;
            }
        };

        String result = testExecutor.writeFile("test.txt", "content");
        assertThat(result).contains("Error").contains("Project base directory not found");
    }

    @Test
    void writeFile_simpleFile_createsNewFile() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(null);
        when(projectBase.createChildData(any(), eq("test.txt"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String result = testExecutor.writeFile("test.txt", "hello world");
        assertThat(result).contains("Successfully wrote").contains("11 characters").contains("test.txt");

        verify(newFile).setBinaryContent("hello world".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeFile_existingFile_overwritesContent() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile existingFile = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(existingFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String result = testExecutor.writeFile("test.txt", "new content");
        assertThat(result).contains("Successfully wrote").contains("11 characters");

        verify(existingFile).setBinaryContent("new content".getBytes(StandardCharsets.UTF_8));
        verify(projectBase, never()).createChildData(any(), anyString());
    }

    @Test
    void writeFile_emptyContent_writesEmptyFile() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(null);
        when(projectBase.createChildData(any(), eq("test.txt"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String result = testExecutor.writeFile("test.txt", "");
        assertThat(result).contains("Successfully wrote").contains("0 characters");

        verify(newFile).setBinaryContent("".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeFile_pathWithSubdirectory_createsFileInSubdir() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile parentDir = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(parentDir.findChild("Main.java")).thenReturn(null);
        when(parentDir.createChildData(any(), eq("Main.java"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithResolvedParent(projectBase, parentDir);

        String result = testExecutor.writeFile("src/Main.java", "class Main {}");
        assertThat(result).contains("Successfully wrote").contains("13 characters").contains("src/Main.java");

        verify(newFile).setBinaryContent("class Main {}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeFile_deepNestedPath_createsFileInDeepSubdir() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile parentDir = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(parentDir.findChild("Test.java")).thenReturn(null);
        when(parentDir.createChildData(any(), eq("Test.java"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithResolvedParent(projectBase, parentDir);

        String result = testExecutor.writeFile("src/main/java/Test.java", "class Test {}");
        assertThat(result).contains("Successfully wrote").contains("src/main/java/Test.java");
    }

    @Test
    void writeFile_resolveParentDirReturnsNull_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);

        WriteFileToolExecutor testExecutor = new WriteFileToolExecutor(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return projectBase;
            }

            @Override
            VirtualFile resolveParentDir(String path, VirtualFile base) {
                return null;
            }

            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;
            }
        };

        String result = testExecutor.writeFile("nonexistent/dir/file.txt", "content");
        assertThat(result).contains("Error").contains("Failed to create parent directories")
                .contains("nonexistent/dir/file.txt");
    }

    @Test
    void writeFile_pathOutsideProject_returnsAccessDenied() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile parentDir = mock(VirtualFile.class);

        WriteFileToolExecutor testExecutor = new WriteFileToolExecutor(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return projectBase;
            }

            @Override
            VirtualFile resolveParentDir(String path, VirtualFile base) {
                return parentDir;
            }

            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return false;
            }
        };

        String result = testExecutor.writeFile("file.txt", "content");
        assertThat(result).contains("Error").contains("Access denied").contains("outside the project root");
    }

    @Test
    void writeFile_createChildDataThrowsException_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(null);
        when(projectBase.createChildData(any(), eq("test.txt")))
                .thenThrow(new IOException("Permission denied"));

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String result = testExecutor.writeFile("test.txt", "content");
        assertThat(result).contains("Error").contains("Failed to write file").contains("Permission denied");
    }

    @Test
    void writeFile_setBinaryContentThrowsException_returnsError() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile existingFile = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(existingFile);
        doThrow(new IOException("Disk full")).when(existingFile).setBinaryContent(any());

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String result = testExecutor.writeFile("test.txt", "content");
        assertThat(result).contains("Error").contains("Failed to write file").contains("Disk full");
    }

    @Test
    void writeFile_largeContent_reportsCorrectCharCount() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(projectBase.findChild("big.txt")).thenReturn(null);
        when(projectBase.createChildData(any(), eq("big.txt"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        String largeContent = "x".repeat(10000);
        String result = testExecutor.writeFile("big.txt", largeContent);
        assertThat(result).contains("Successfully wrote").contains("10000 characters");
    }

    // --- extractFileName() tests ---

    @Test
    void extractFileName_simpleFilename_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName("test.txt")).isEqualTo("test.txt");
    }

    @Test
    void extractFileName_pathWithOneDir_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName("src/Main.java")).isEqualTo("Main.java");
    }

    @Test
    void extractFileName_deepPath_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName("src/main/java/com/Test.java")).isEqualTo("Test.java");
    }

    @Test
    void extractFileName_trailingSlash_returnsEmpty() {
        assertThat(WriteFileToolExecutor.extractFileName("src/")).isEmpty();
    }

    @Test
    void extractFileName_noExtension_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName("Makefile")).isEqualTo("Makefile");
    }

    @Test
    void extractFileName_dotFile_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName(".gitignore")).isEqualTo(".gitignore");
    }

    @Test
    void extractFileName_dotFileInDir_returnsFilename() {
        assertThat(WriteFileToolExecutor.extractFileName("config/.env")).isEqualTo(".env");
    }

    // --- execute() end-to-end tests (with mocked ApplicationManager) ---

    @Test
    void execute_validRequest_writesFileSuccessfully() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile newFile = mock(VirtualFile.class);

        when(projectBase.findChild("test.txt")).thenReturn(null);
        when(projectBase.createChildData(any(), eq("test.txt"))).thenReturn(newFile);

        WriteFileToolExecutor testExecutor = createTestableExecutorWithBase(projectBase);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"test.txt\", \"content\": \"hello\"}")
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
            assertThat(result).contains("Successfully wrote").contains("5 characters");
        }
    }

    @Test
    void execute_applicationManagerThrows_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"path\": \"test.txt\", \"content\": \"hello\"}")
                .build();

        try (MockedStatic<ApplicationManager> appMock = mockStatic(ApplicationManager.class)) {
            appMock.when(ApplicationManager::getApplication)
                    .thenThrow(new RuntimeException("No application"));

            String result = executor.execute(request, null);
            assertThat(result).contains("Error").contains("Failed to write file");
        }
    }

    // --- resolveParentDir() default behavior tests ---

    @Test
    void resolveParentDir_noSlash_returnsProjectBase() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);

        // Use the real executor's resolveParentDir for paths without slashes
        WriteFileToolExecutor spyExecutor = spy(executor);
        VirtualFile result = spyExecutor.resolveParentDir("test.txt", projectBase);
        assertThat(result).isSameAs(projectBase);
    }

    // --- Helper methods ---

    private WriteFileToolExecutor createTestableExecutorWithBase(VirtualFile projectBase) {
        return new WriteFileToolExecutor(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return projectBase;
            }

            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;
            }

            @Override
            VirtualFile resolveParentDir(String path, VirtualFile base) {
                // For simple filenames (no slash), return projectBase
                return base;
            }
        };
    }

    private WriteFileToolExecutor createTestableExecutorWithResolvedParent(
            VirtualFile projectBase, VirtualFile parentDir) {
        return new WriteFileToolExecutor(project) {
            @Override
            VirtualFile getProjectBaseDir() {
                return projectBase;
            }

            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;
            }

            @Override
            VirtualFile resolveParentDir(String path, VirtualFile base) {
                if (!path.contains("/")) {
                    return base;
                }
                return parentDir;
            }
        };
    }
}
