package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListFilesToolExecutorTest {

    @Mock
    private Project project;

    private ListFilesToolExecutor executor;

    private ListFilesToolExecutor createTestableExecutor() {
        return new ListFilesToolExecutor(project) {
            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;  // Always allow in tests — access-denied tested separately
            }

            @Override
            String getRelativePath(VirtualFile file, VirtualFile ancestor) {
                return file.getName();
            }
        };
    }

    @BeforeEach
    void setUp() {
        executor = new ListFilesToolExecutor(project);
    }

    // --- Argument validation (execute method) ---

    @Test
    void execute_emptyArguments_delegatesToListFiles() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("list_files")
                .arguments("{}")
                .build();

        // Without IntelliJ platform, ReadAction will throw — caught by execute's try/catch
        String result = executor.execute(request, null);
        assertThat(result).isNotNull();
    }

    @Test
    void execute_invalidJson_handlesGracefully() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("list_files")
                .arguments("invalid")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).isNotNull();
    }

    // --- resolveTargetDir tests ---

    @Test
    void resolveTargetDir_nullPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        assertThat(ListFilesToolExecutor.resolveTargetDir(null, projectBase)).isSameAs(projectBase);
    }

    @Test
    void resolveTargetDir_emptyPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        assertThat(ListFilesToolExecutor.resolveTargetDir("", projectBase)).isSameAs(projectBase);
    }

    @Test
    void resolveTargetDir_blankPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        assertThat(ListFilesToolExecutor.resolveTargetDir("   ", projectBase)).isSameAs(projectBase);
    }

    @Test
    void resolveTargetDir_dotPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        assertThat(ListFilesToolExecutor.resolveTargetDir(".", projectBase)).isSameAs(projectBase);
    }

    @Test
    void resolveTargetDir_validSubPath_returnsSubDir() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile subDir = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("src")).thenReturn(subDir);

        assertThat(ListFilesToolExecutor.resolveTargetDir("src", projectBase)).isSameAs(subDir);
    }

    @Test
    void resolveTargetDir_nonExistentPath_returnsNull() {
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("missing")).thenReturn(null);

        assertThat(ListFilesToolExecutor.resolveTargetDir("missing", projectBase)).isNull();
    }

    // --- listFiles tests ---

    @Test
    void listFiles_nullProjectBase_returnsError() {
        String result = executor.listFiles(null, false, null);
        assertThat(result).contains("Error").contains("Project base directory not found");
    }

    @Test
    void listFiles_nonExistentPath_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("missing")).thenReturn(null);

        String result = executor.listFiles("missing", false, projectBase);
        assertThat(result).contains("Error").contains("Directory not found").contains("missing");
    }

    @Test
    void listFiles_pathIsFile_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("file.txt")).thenReturn(file);
        when(file.isDirectory()).thenReturn(false);

        String result = executor.listFiles("file.txt", false, projectBase);
        assertThat(result).contains("Error").contains("not a directory").contains("file.txt");
    }

    @Test
    void listFiles_pathOutsideProject_returnsAccessDenied() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile outsideDir = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("../outside")).thenReturn(outsideDir);
        when(outsideDir.isDirectory()).thenReturn(true);

        ListFilesToolExecutor spyExecutor = spy(executor);
        doReturn(false).when(spyExecutor).isAncestor(projectBase, outsideDir);

        String result = spyExecutor.listFiles("../outside", false, projectBase);
        assertThat(result).contains("Error").contains("Access denied").contains("outside the project root");
    }

    @Test
    void listFiles_emptyDirectory_returnsEmptyString() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);
        when(projectBase.getChildren()).thenReturn(new VirtualFile[0]);

        String result = testExecutor.listFiles(null, false, projectBase);
        assertThat(result).isEmpty();
    }

    @Test
    void listFiles_withFiles_listsFiles() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile file1 = createMockFile("Main.java");
        VirtualFile file2 = createMockFile("README.md");

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{file1, file2});

        String result = testExecutor.listFiles(null, false, projectBase);
        assertThat(result).contains("[FILE] Main.java");
        assertThat(result).contains("[FILE] README.md");
    }

    @Test
    void listFiles_withDirectories_listsDirs() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile srcDir = createMockDir("src");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[0]);

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{srcDir});

        String result = testExecutor.listFiles(null, false, projectBase);
        assertThat(result).contains("[DIR]  src");
    }

    @Test
    void listFiles_nonRecursive_doesNotListSubDirContents() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile srcDir = createMockDir("src");
        VirtualFile nestedFile = createMockFile("App.java");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[]{nestedFile});

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{srcDir});

        String result = testExecutor.listFiles(null, false, projectBase);
        assertThat(result).contains("[DIR]  src");
        assertThat(result).doesNotContain("App.java");
    }

    @Test
    void listFiles_recursive_listsSubDirContents() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile srcDir = createMockDir("src");
        VirtualFile nestedFile = createMockFile("App.java");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[]{nestedFile});

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{srcDir});

        String result = testExecutor.listFiles(null, true, projectBase);
        assertThat(result).contains("[DIR]  src");
        assertThat(result).contains("[FILE] App.java");
    }

    @Test
    void listFiles_mixedContent_listsFilesAndDirs() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile srcDir = createMockDir("src");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[0]);
        VirtualFile file = createMockFile("build.gradle");

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{srcDir, file});

        String result = testExecutor.listFiles(null, false, projectBase);
        assertThat(result).contains("[DIR]  src");
        assertThat(result).contains("[FILE] build.gradle");
    }

    @Test
    void listFiles_validSubDir_listsSubDirContents() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile subDir = mock(VirtualFile.class);

        when(projectBase.findFileByRelativePath("src")).thenReturn(subDir);
        when(subDir.isDirectory()).thenReturn(true);

        VirtualFile javaFile = createMockFile("Main.java");
        when(subDir.getChildren()).thenReturn(new VirtualFile[]{javaFile});

        String result = testExecutor.listFiles("src", false, projectBase);
        assertThat(result).contains("[FILE] Main.java");
    }

    // --- listDirectory tests ---

    @Test
    void listDirectory_nullChildren_doesNothing() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        when(dir.getChildren()).thenReturn(null);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        executor.listDirectory(dir, projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void listDirectory_emptyChildren_doesNothing() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        when(dir.getChildren()).thenReturn(new VirtualFile[0]);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        executor.listDirectory(dir, projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void listDirectory_stopsAtMaxEntries() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);

        VirtualFile file = createMockFile("extra.java");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{file});

        StringBuilder result = new StringBuilder();
        int[] count = {ListFilesToolExecutor.MAX_ENTRIES};

        testExecutor.listDirectory(dir, projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isEqualTo(ListFilesToolExecutor.MAX_ENTRIES);
    }

    @Test
    void listDirectory_nullRelativePath_skipsChild() {
        ListFilesToolExecutor testExecutor = new ListFilesToolExecutor(project) {
            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;
            }

            @Override
            String getRelativePath(VirtualFile file, VirtualFile ancestor) {
                return null;
            }
        };

        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = createMockFile("orphan.java");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{file});

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        testExecutor.listDirectory(dir, projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    // --- appendDirectory tests ---

    @Test
    void appendDirectory_skipsGitDir() {
        VirtualFile gitDir = createMockDir(".git");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        executor.appendDirectory(gitDir, ".git", projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void appendDirectory_skipsNodeModules() {
        VirtualFile nodeModules = createMockDir("node_modules");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        executor.appendDirectory(nodeModules, "node_modules", projectBase, false, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void appendDirectory_skipsAllSkipDirs() {
        VirtualFile projectBase = mock(VirtualFile.class);

        for (String skipDirName : ListFilesToolExecutor.SKIP_DIRS) {
            VirtualFile skipDir = createMockDir(skipDirName);
            StringBuilder result = new StringBuilder();
            int[] count = {0};

            executor.appendDirectory(skipDir, skipDirName, projectBase, false, result, count);

            assertThat(result).as("Should skip " + skipDirName).isEmpty();
            assertThat(count[0]).as("Count should be 0 for " + skipDirName).isZero();
        }
    }

    @Test
    void appendDirectory_normalDir_nonRecursive_appendsOnly() {
        VirtualFile srcDir = createMockDir("src");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[0]);
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        executor.appendDirectory(srcDir, "src", projectBase, false, result, count);

        assertThat(result.toString()).isEqualTo("[DIR]  src\n");
        assertThat(count[0]).isEqualTo(1);
        // Should NOT call getChildren when not recursive
        verify(srcDir, never()).getChildren();
    }

    @Test
    void appendDirectory_normalDir_recursive_recursesInto() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile srcDir = createMockDir("src");
        VirtualFile nestedFile = createMockFile("App.java");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[]{nestedFile});
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        testExecutor.appendDirectory(srcDir, "src", projectBase, true, result, count);

        assertThat(result.toString()).contains("[DIR]  src\n");
        assertThat(result.toString()).contains("[FILE] App.java\n");
        assertThat(count[0]).isEqualTo(2);
    }

    @Test
    void appendDirectory_deepRecursion() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);

        // Create nested structure: src/ -> main/ -> App.java
        VirtualFile appFile = createMockFile("App.java");
        VirtualFile mainDir = createMockDir("main");
        when(mainDir.getChildren()).thenReturn(new VirtualFile[]{appFile});

        VirtualFile srcDir = createMockDir("src");
        when(srcDir.getChildren()).thenReturn(new VirtualFile[]{mainDir});

        StringBuilder result = new StringBuilder();
        int[] count = {0};

        testExecutor.appendDirectory(srcDir, "src", projectBase, true, result, count);

        assertThat(result.toString()).contains("[DIR]  src");
        assertThat(result.toString()).contains("[DIR]  main");
        assertThat(result.toString()).contains("[FILE] App.java");
        assertThat(count[0]).isEqualTo(3);
    }

    // --- Truncation test ---

    @Test
    void listFiles_truncationMessage_whenMaxEntriesReached() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile[] files = new VirtualFile[ListFilesToolExecutor.MAX_ENTRIES + 10];
        for (int i = 0; i < files.length; i++) {
            files[i] = createMockFile("File" + i + ".java");
        }
        when(projectBase.getChildren()).thenReturn(files);

        String result = testExecutor.listFiles(null, false, projectBase);

        assertThat(result).contains("truncated");
        assertThat(result).contains("showing first " + ListFilesToolExecutor.MAX_ENTRIES);
    }

    @Test
    void listFiles_belowMaxEntries_noTruncationMessage() {
        ListFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.isDirectory()).thenReturn(true);

        VirtualFile file = createMockFile("Main.java");
        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{file});

        String result = testExecutor.listFiles(null, false, projectBase);

        assertThat(result).doesNotContain("truncated");
        assertThat(result).contains("[FILE] Main.java");
    }

    // --- SKIP_DIRS constant test ---

    @Test
    void skipDirs_containsExpectedDirectories() {
        assertThat(ListFilesToolExecutor.SKIP_DIRS).containsExactlyInAnyOrder(
                ".git", "node_modules", "build", "out", "target", ".idea", "bin", ".gradle"
        );
    }

    // --- Helpers ---

    private VirtualFile createMockFile(String name) {
        VirtualFile file = mock(VirtualFile.class);
        when(file.isDirectory()).thenReturn(false);
        when(file.getName()).thenReturn(name);
        return file;
    }

    private VirtualFile createMockDir(String name) {
        VirtualFile dir = mock(VirtualFile.class);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.getName()).thenReturn(name);
        return dir;
    }
}
