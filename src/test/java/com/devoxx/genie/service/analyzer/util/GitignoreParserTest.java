package com.devoxx.genie.service.analyzer.util;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GitignoreParserTest {

    @Mock
    private VirtualFile baseDir;

    @Mock
    private VirtualFile gitignoreFile;

    private MockedStatic<VfsUtilCore> vfsUtilStatic;

    @BeforeEach
    void setUp() {
        when(baseDir.getPath()).thenReturn("/project");

        vfsUtilStatic = mockStatic(VfsUtilCore.class);
        // By default, no nested gitignores (the visitor is simply invoked but does nothing)
        vfsUtilStatic.when(() -> VfsUtilCore.visitChildrenRecursively(eq(baseDir), any(VirtualFileVisitor.class)))
                .then(invocation -> null);
    }

    @AfterEach
    void tearDown() {
        vfsUtilStatic.close();
    }

    @Test
    void shouldIgnore_whenNoGitignore_nothingIsIgnored() {
        when(baseDir.findChild(".gitignore")).thenReturn(null);

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("src/Main.java", false)).isFalse();
        assertThat(parser.shouldIgnore("build", true)).isFalse();
    }

    @Test
    void shouldIgnore_directoryPattern_matchesDirectory() throws Exception {
        setupGitignoreContent("build/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build", true)).isTrue();
        assertThat(parser.shouldIgnore("build/output.jar", false)).isTrue();
    }

    @Test
    void shouldIgnore_emptyPath_returnsFalse() throws Exception {
        setupGitignoreContent("build/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("", false)).isFalse();
    }

    @Test
    void shouldIgnore_multipleDirectoryPatterns_allMatch() throws Exception {
        setupGitignoreContent("build/\n.gradle/\nout/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build", true)).isTrue();
        assertThat(parser.shouldIgnore(".gradle", true)).isTrue();
        assertThat(parser.shouldIgnore("out", true)).isTrue();
        assertThat(parser.shouldIgnore("src", true)).isFalse();
    }

    @Test
    void shouldIgnore_fileUnderIgnoredDirectory_isIgnored() throws Exception {
        setupGitignoreContent("build/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build/classes/Main.class", false)).isTrue();
        assertThat(parser.shouldIgnore("build/libs/app.jar", false)).isTrue();
    }

    @Test
    void shouldIgnore_dotGitIgnoredDir_matchesSubFiles() throws Exception {
        setupGitignoreContent(".idea/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore(".idea", true)).isTrue();
        assertThat(parser.shouldIgnore(".idea/workspace.xml", false)).isTrue();
    }

    @Test
    void shouldIgnore_commentLines_areSkipped() throws Exception {
        setupGitignoreContent("# This is a comment\nbuild/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build", true)).isTrue();
        // Comments and non-ignored paths should not be matched
        assertThat(parser.shouldIgnore("src", true)).isFalse();
    }

    @Test
    void shouldIgnore_emptyLines_areSkipped() throws Exception {
        setupGitignoreContent("\n\nbuild/\n\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build", true)).isTrue();
        assertThat(parser.shouldIgnore("src", true)).isFalse();
    }

    @Test
    void shouldIgnore_pathWithLeadingSlash_isNormalized() throws Exception {
        setupGitignoreContent("build/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        // Leading slash should be stripped during normalization
        assertThat(parser.shouldIgnore("/build", true)).isTrue();
        assertThat(parser.shouldIgnore("/build/output.jar", false)).isTrue();
    }

    @Test
    void shouldIgnore_deeplyNestedFilesUnderIgnoredDir() throws Exception {
        setupGitignoreContent("build/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("build/a/b/c/d.txt", false)).isTrue();
    }

    @Test
    void shouldIgnore_nonIgnoredPath_returnsFalse() throws Exception {
        setupGitignoreContent("build/\n.gradle/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        assertThat(parser.shouldIgnore("src/Main.java", false)).isFalse();
        assertThat(parser.shouldIgnore("README.md", false)).isFalse();
        assertThat(parser.shouldIgnore("lib", true)).isFalse();
    }

    @Test
    void shouldIgnore_directlyExcludedDirFastPath_topLevelMatch() throws Exception {
        setupGitignoreContent("node_modules/\ntarget/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        // Top-level directories
        assertThat(parser.shouldIgnore("node_modules", true)).isTrue();
        assertThat(parser.shouldIgnore("target", true)).isTrue();
        // Sub-paths
        assertThat(parser.shouldIgnore("node_modules/package/index.js", false)).isTrue();
        assertThat(parser.shouldIgnore("target/classes/App.class", false)).isTrue();
    }

    @Test
    void shouldIgnore_negationPattern_unignoresSpecificFile() throws Exception {
        // Use directory-prefixed patterns so GlobTool produces correct regex (avoids the *.ext optimization path)
        setupGitignoreContent("src/*.log\n!src/important.log\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        // important.log is explicitly un-ignored by the negation pattern
        assertThat(parser.shouldIgnore("src/important.log", false)).isFalse();
        // other .log files in src/ are still ignored
        assertThat(parser.shouldIgnore("src/debug.log", false)).isTrue();
        assertThat(parser.shouldIgnore("src/error.log", false)).isTrue();
    }

    @Test
    void shouldIgnore_negationPatternOnDirectory_unignoresDirectory() throws Exception {
        setupGitignoreContent("build/\n!build/reports/\n");

        GitignoreParser parser = new GitignoreParser(baseDir);

        // The build directory itself is excluded
        assertThat(parser.shouldIgnore("build", true)).isTrue();
        // build/classes is under an excluded dir
        assertThat(parser.shouldIgnore("build/classes", true)).isTrue();
    }

    private void setupGitignoreContent(String content) throws Exception {
        when(baseDir.findChild(".gitignore")).thenReturn(gitignoreFile);
        when(gitignoreFile.getPath()).thenReturn("/project/.gitignore");
        vfsUtilStatic.when(() -> VfsUtilCore.loadText(gitignoreFile)).thenReturn(content);
    }
}
