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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchFilesToolExecutorTest {

    @Mock
    private Project project;

    private SearchFilesToolExecutor executor;

    /**
     * Creates a testable executor where VfsUtil calls are stubbed out
     * to avoid IntelliJ platform dependencies.
     */
    private SearchFilesToolExecutor createTestableExecutor() {
        return new SearchFilesToolExecutor(project) {
            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                // In tests, treat same-instance as ancestor
                return ancestor == descendant || ancestor.equals(descendant);
            }

            @Override
            String getRelativePath(VirtualFile file, VirtualFile ancestor) {
                return file.getName();
            }
        };
    }

    @BeforeEach
    void setUp() {
        executor = new SearchFilesToolExecutor(project);
    }

    // --- Argument validation tests (execute method) ---

    @Test
    void execute_missingPattern_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("pattern");
    }

    @Test
    void execute_emptyPattern_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{\"pattern\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("pattern");
    }

    @Test
    void execute_blankPattern_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{\"pattern\": \"   \"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("pattern");
    }

    @Test
    void execute_invalidRegex_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("search_files")
                .arguments("{\"pattern\": \"[invalid\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Invalid regex");
    }

    // --- compilePattern tests ---

    @Test
    void compilePattern_validPattern_returnsPattern() {
        Pattern result = SearchFilesToolExecutor.compilePattern("hello.*world");
        assertThat(result).isNotNull();
        assertThat(result.flags() & Pattern.CASE_INSENSITIVE).isNotZero();
    }

    @Test
    void compilePattern_invalidPattern_returnsNull() {
        Pattern result = SearchFilesToolExecutor.compilePattern("[unclosed");
        assertThat(result).isNull();
    }

    @Test
    void compilePattern_specialChars_compilesSuccessfully() {
        Pattern result = SearchFilesToolExecutor.compilePattern("import java\\.");
        assertThat(result).isNotNull();
    }

    // --- isBinaryFile tests ---

    @Test
    void isBinaryFile_javaExtension_returnsFalse() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("java");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isFalse();
    }

    @Test
    void isBinaryFile_jarExtension_returnsTrue() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("jar");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isTrue();
    }

    @Test
    void isBinaryFile_classExtension_returnsTrue() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("class");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isTrue();
    }

    @Test
    void isBinaryFile_pngExtension_returnsTrue() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("png");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isTrue();
    }

    @Test
    void isBinaryFile_nullExtension_returnsFalse() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn(null);
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isFalse();
    }

    @Test
    void isBinaryFile_upperCaseExtension_returnsTrue() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("PNG");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isTrue();
    }

    @Test
    void isBinaryFile_txtExtension_returnsFalse() {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getExtension()).thenReturn("txt");
        assertThat(SearchFilesToolExecutor.isBinaryFile(file)).isFalse();
    }

    // --- resolveSearchDir tests ---

    @Test
    void resolveSearchDir_nullPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir(null, projectBase);
        assertThat(result).isSameAs(projectBase);
    }

    @Test
    void resolveSearchDir_emptyPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir("", projectBase);
        assertThat(result).isSameAs(projectBase);
    }

    @Test
    void resolveSearchDir_blankPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir("   ", projectBase);
        assertThat(result).isSameAs(projectBase);
    }

    @Test
    void resolveSearchDir_dotPath_returnsProjectBase() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir(".", projectBase);
        assertThat(result).isSameAs(projectBase);
    }

    @Test
    void resolveSearchDir_validSubPath_returnsSubDir() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile subDir = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("src/main")).thenReturn(subDir);

        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir("src/main", projectBase);
        assertThat(result).isSameAs(subDir);
    }

    @Test
    void resolveSearchDir_nonExistentPath_returnsNull() {
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("nonexistent")).thenReturn(null);

        VirtualFile result = SearchFilesToolExecutor.resolveSearchDir("nonexistent", projectBase);
        assertThat(result).isNull();
    }

    // --- searchFiles tests ---

    @Test
    void searchFiles_nullProjectBase_returnsError() {
        Pattern regex = Pattern.compile("test");
        String result = executor.searchFiles("test", null, regex, null, null);
        assertThat(result).contains("Error").contains("Project base directory not found");
    }

    @Test
    void searchFiles_nonExistentPath_returnsError() {
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("nonexistent")).thenReturn(null);

        Pattern regex = Pattern.compile("test");
        String result = executor.searchFiles("test", "nonexistent", regex, null, projectBase);
        assertThat(result).contains("Error").contains("Directory not found").contains("nonexistent");
    }

    @Test
    void searchFiles_pathOutsideProject_returnsAccessDenied() {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile outsideDir = mock(VirtualFile.class);
        when(projectBase.findFileByRelativePath("../outside")).thenReturn(outsideDir);

        // Use a spy to control isAncestor
        SearchFilesToolExecutor spyExecutor = spy(executor);
        doReturn(false).when(spyExecutor).isAncestor(projectBase, outsideDir);

        Pattern regex = Pattern.compile("test");
        String result = spyExecutor.searchFiles("test", "../outside", regex, null, projectBase);
        assertThat(result).contains("Error").contains("Access denied").contains("outside the project root");
    }

    @Test
    void searchFiles_noMatches_returnsNoMatchesMessage() {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        when(projectBase.getChildren()).thenReturn(new VirtualFile[0]);

        Pattern regex = Pattern.compile("nonexistent");
        String result = testExecutor.searchFiles("nonexistent", null, regex, null, projectBase);
        assertThat(result).contains("No matches found for pattern: nonexistent");
    }

    @Test
    void searchFiles_withMatches_returnsResults() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile javaFile = createMockFile("Test.java", "java", "public class Test {}");

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{javaFile});

        Pattern regex = Pattern.compile("class");
        String result = testExecutor.searchFiles("class", null, regex, null, projectBase);

        assertThat(result).contains("Test.java:1:");
        assertThat(result).contains("public class Test");
    }

    @Test
    void searchFiles_withFilePattern_filtersResults() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile javaFile = createMockFile("Test.java", "java", "public class Test {}");
        VirtualFile txtFile = createMockFile("notes.txt", "txt", "class notes here");

        when(projectBase.getChildren()).thenReturn(new VirtualFile[]{javaFile, txtFile});

        PathMatcher javaMatcher = FileSystems.getDefault().getPathMatcher("glob:*.java");
        Pattern regex = Pattern.compile("class");
        String result = testExecutor.searchFiles("class", null, regex, javaMatcher, projectBase);

        assertThat(result).contains("Test.java");
        verify(txtFile, never()).getInputStream();
    }

    @Test
    void searchFiles_validSubDir_searchesInSubDir() throws IOException {
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile subDir = mock(VirtualFile.class);
        VirtualFile file = createMockFile("App.java", "java", "main method");

        when(projectBase.findFileByRelativePath("src")).thenReturn(subDir);
        when(subDir.getChildren()).thenReturn(new VirtualFile[]{file});

        // Use executor with custom getRelativePath that includes dir prefix
        SearchFilesToolExecutor testExecutor = new SearchFilesToolExecutor(project) {
            @Override
            boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
                return true;
            }

            @Override
            String getRelativePath(VirtualFile f, VirtualFile ancestor) {
                return "src/" + f.getName();
            }
        };

        Pattern regex = Pattern.compile("main");
        String result = testExecutor.searchFiles("main", "src", regex, null, projectBase);

        assertThat(result).contains("src/App.java:1: main method");
    }

    @Test
    void searchFiles_truncationMessage_whenMaxResultsReached() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile projectBase = mock(VirtualFile.class);

        // Create enough files to exceed MAX_RESULTS
        VirtualFile[] files = new VirtualFile[SearchFilesToolExecutor.MAX_RESULTS + 5];
        for (int i = 0; i < files.length; i++) {
            files[i] = createMockFile("File" + i + ".java", "java", "match in file " + i);
        }

        when(projectBase.getChildren()).thenReturn(files);

        Pattern regex = Pattern.compile("match");
        String result = testExecutor.searchFiles("match", null, regex, null, projectBase);

        assertThat(result).contains("truncated");
        assertThat(result).contains("showing first " + SearchFilesToolExecutor.MAX_RESULTS);
    }

    // --- searchInDirectory tests ---

    @Test
    void searchInDirectory_nullChildren_doesNothing() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        when(dir.getChildren()).thenReturn(null);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void searchInDirectory_emptyChildren_doesNothing() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        when(dir.getChildren()).thenReturn(new VirtualFile[0]);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void searchInDirectory_skipsGitDirectory() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile gitDir = mock(VirtualFile.class);

        when(gitDir.isDirectory()).thenReturn(true);
        when(gitDir.getName()).thenReturn(".git");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{gitDir});

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        verify(gitDir, never()).getChildren();
        assertThat(result).isEmpty();
    }

    @Test
    void searchInDirectory_skipsNodeModules() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile nodeModules = mock(VirtualFile.class);

        when(nodeModules.isDirectory()).thenReturn(true);
        when(nodeModules.getName()).thenReturn("node_modules");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{nodeModules});

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        verify(nodeModules, never()).getChildren();
    }

    @Test
    void searchInDirectory_skipsBinaryFiles() throws IOException {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile jarFile = mock(VirtualFile.class);

        when(jarFile.isDirectory()).thenReturn(false);
        when(jarFile.getExtension()).thenReturn("jar");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{jarFile});

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        verify(jarFile, never()).getInputStream();
        assertThat(result).isEmpty();
    }

    @Test
    void searchInDirectory_appliesFilePatternFilter() throws IOException {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile txtFile = mock(VirtualFile.class);

        when(txtFile.isDirectory()).thenReturn(false);
        when(txtFile.getExtension()).thenReturn("txt");
        when(txtFile.getName()).thenReturn("readme.txt");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{txtFile});

        PathMatcher javaMatcher = FileSystems.getDefault().getPathMatcher("glob:*.java");

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, javaMatcher, result, count);

        verify(txtFile, never()).getInputStream();
        assertThat(result).isEmpty();
    }

    @Test
    void searchInDirectory_fileMatchesFilePattern_isSearched() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile javaFile = createMockFile("Test.java", "java", "class body here");

        when(dir.getChildren()).thenReturn(new VirtualFile[]{javaFile});

        PathMatcher javaMatcher = FileSystems.getDefault().getPathMatcher("glob:*.java");

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("class");

        testExecutor.searchInDirectory(dir, projectBase, regex, javaMatcher, result, count);

        assertThat(result.toString()).contains("Test.java:1:");
        assertThat(count[0]).isEqualTo(1);
    }

    @Test
    void searchInDirectory_recursesIntoSubdirectories() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile subDir = mock(VirtualFile.class);
        VirtualFile fileInSubDir = createMockFile("Test.java", "java", "public class Test {}");

        when(subDir.isDirectory()).thenReturn(true);
        when(subDir.getName()).thenReturn("src");
        when(subDir.getChildren()).thenReturn(new VirtualFile[]{fileInSubDir});
        when(dir.getChildren()).thenReturn(new VirtualFile[]{subDir});

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("class");

        testExecutor.searchInDirectory(dir, projectBase, regex, null, result, count);

        assertThat(result.toString()).contains("Test.java:1:");
        assertThat(count[0]).isEqualTo(1);
    }

    @Test
    void searchInDirectory_stopsAtMaxResults() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        VirtualFile file = mock(VirtualFile.class);

        when(file.isDirectory()).thenReturn(false);
        when(file.getExtension()).thenReturn("java");
        when(dir.getChildren()).thenReturn(new VirtualFile[]{file});

        StringBuilder result = new StringBuilder();
        int[] count = {SearchFilesToolExecutor.MAX_RESULTS};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        // Should not even check the file since we're already at max
        verify(file, never()).getExtension();
    }

    @Test
    void searchInDirectory_allSkipDirs_skipsAll() {
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);

        VirtualFile[] skipDirs = SearchFilesToolExecutor.SKIP_DIRS.stream()
                .map(name -> {
                    VirtualFile d = mock(VirtualFile.class);
                    when(d.isDirectory()).thenReturn(true);
                    when(d.getName()).thenReturn(name);
                    return d;
                })
                .toArray(VirtualFile[]::new);

        when(dir.getChildren()).thenReturn(skipDirs);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInDirectory(dir, projectBase, regex, null, result, count);

        for (VirtualFile skipDir : skipDirs) {
            verify(skipDir, never()).getChildren();
        }
    }

    @Test
    void searchInDirectory_mixedFilesAndDirs() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile dir = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);

        VirtualFile subDir = mock(VirtualFile.class);
        when(subDir.isDirectory()).thenReturn(true);
        when(subDir.getName()).thenReturn("src");
        when(subDir.getChildren()).thenReturn(new VirtualFile[0]);

        VirtualFile javaFile = createMockFile("Test.java", "java", "public class Test {}");
        VirtualFile binaryFile = mock(VirtualFile.class);
        when(binaryFile.isDirectory()).thenReturn(false);
        when(binaryFile.getExtension()).thenReturn("class");

        when(dir.getChildren()).thenReturn(new VirtualFile[]{subDir, javaFile, binaryFile});

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("class");

        testExecutor.searchInDirectory(dir, projectBase, regex, null, result, count);

        assertThat(result.toString()).contains("Test.java:1:");
        assertThat(count[0]).isEqualTo(1);
        verify(binaryFile, never()).getInputStream();
        verify(subDir).getChildren();
    }

    // --- searchInFile tests ---

    @Test
    void searchInFile_matchesPattern_addsToResult() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("Test.java", "java",
                "line one\nfind this match\nline three\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("match");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result.toString()).contains("Test.java:2: find this match");
        assertThat(count[0]).isEqualTo(1);
    }

    @Test
    void searchInFile_multipleMatches_addsAll() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("file.txt", "txt",
                "hello world\ngoodbye world\nhello again\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("hello");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result.toString()).contains("file.txt:1: hello world");
        assertThat(result.toString()).contains("file.txt:3: hello again");
        assertThat(count[0]).isEqualTo(2);
    }

    @Test
    void searchInFile_noMatch_addsNothing() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("file.txt", "txt", "nothing to see here\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("missing");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void searchInFile_longLine_truncates() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        String longLine = "match_here" + "x".repeat(300);
        VirtualFile file = createMockFile("file.txt", "txt", longLine);
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("match_here");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        String resultStr = result.toString();
        assertThat(resultStr).contains("...");
        // The display portion should be truncated
        assertThat(resultStr.length()).isLessThan(longLine.length() + 50);
        assertThat(count[0]).isEqualTo(1);
    }

    @Test
    void searchInFile_nullRelativePath_skipsFile() throws IOException {
        // Use executor where getRelativePath returns null
        SearchFilesToolExecutor testExecutor = new SearchFilesToolExecutor(project) {
            @Override
            String getRelativePath(VirtualFile file, VirtualFile ancestor) {
                return null;
            }
        };

        VirtualFile file = createMockFile("file.txt", "txt", "some match here\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("match");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void searchInFile_ioException_skipsFile() throws IOException {
        VirtualFile file = mock(VirtualFile.class);
        VirtualFile projectBase = mock(VirtualFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Cannot read file"));

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("test");

        executor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    @Test
    void searchInFile_stopsAtMaxResults() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("file.txt", "txt", "match1\nmatch2\nmatch3\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {SearchFilesToolExecutor.MAX_RESULTS - 1};
        Pattern regex = Pattern.compile("match");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(count[0]).isEqualTo(SearchFilesToolExecutor.MAX_RESULTS);
        assertThat(result.toString()).contains("match1");
        assertThat(result.toString()).doesNotContain("match2");
    }

    @Test
    void searchInFile_caseInsensitive_matchesBothCases() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("file.txt", "txt",
                "Hello World\nhello world\nHELLO WORLD\n");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("hello", Pattern.CASE_INSENSITIVE);

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(count[0]).isEqualTo(3);
    }

    @Test
    void searchInFile_emptyFile_addsNothing() throws IOException {
        SearchFilesToolExecutor testExecutor = createTestableExecutor();
        VirtualFile file = createMockFile("empty.txt", "txt", "");
        VirtualFile projectBase = mock(VirtualFile.class);

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        Pattern regex = Pattern.compile("anything");

        testExecutor.searchInFile(file, projectBase, regex, result, count);

        assertThat(result).isEmpty();
        assertThat(count[0]).isZero();
    }

    // --- SKIP_DIRS constant tests ---

    @Test
    void skipDirs_containsExpectedDirectories() {
        assertThat(SearchFilesToolExecutor.SKIP_DIRS).containsExactlyInAnyOrder(
                ".git", "node_modules", "build", "out", "target", ".idea", "bin", ".gradle"
        );
    }

    // --- Helper ---

    private VirtualFile createMockFile(String name, String extension, String content) throws IOException {
        VirtualFile file = mock(VirtualFile.class);
        when(file.isDirectory()).thenReturn(false);
        when(file.getExtension()).thenReturn(extension);
        when(file.getName()).thenReturn(name);
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return file;
    }
}
