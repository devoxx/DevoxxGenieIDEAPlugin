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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachedProjectScannerTest {

    @Mock
    private VirtualFile baseDir;

    private MockedStatic<VfsUtilCore> vfsUtilStatic;
    private CachedProjectScanner scanner;

    @BeforeEach
    void setUp() {
        when(baseDir.getPath()).thenReturn("/project");
        when(baseDir.getTimeStamp()).thenReturn(1000L);
        when(baseDir.findChild(".gitignore")).thenReturn(null);

        vfsUtilStatic = mockStatic(VfsUtilCore.class);
        vfsUtilStatic.when(() -> VfsUtilCore.visitChildrenRecursively(eq(baseDir), any(VirtualFileVisitor.class)))
                .then(invocation -> null);

        CachedProjectScanner.clearCache();
        scanner = new CachedProjectScanner(baseDir);
    }

    @AfterEach
    void tearDown() {
        vfsUtilStatic.close();
        CachedProjectScanner.clearCache();
    }

    @Test
    void scanDirectoryWithCache_emptyDirectory_returnsEmptyList() {
        when(baseDir.getChildren()).thenReturn(new VirtualFile[0]);

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectoryWithCache_nullChildren_returnsEmptyList() {
        when(baseDir.getChildren()).thenReturn(null);

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectoryWithCache_singleFile_returnsFile() {
        VirtualFile file = mockFile("/project/Main.java");
        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{file});

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).containsExactly(file);
    }

    @Test
    void scanDirectoryWithCache_multipleFiles_returnsAllFiles() {
        VirtualFile fileA = mockFile("/project/A.java");
        VirtualFile fileB = mockFile("/project/B.java");
        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{fileA, fileB});

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).containsExactlyInAnyOrder(fileA, fileB);
    }

    @Test
    void scanDirectoryWithCache_usesCachedResult_onSecondCallWithSameTimestamp() {
        VirtualFile file = mockFile("/project/Main.java");
        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{file});

        // First call populates the cache
        List<VirtualFile> first = scanner.scanDirectoryWithCache();
        assertThat(first).hasSize(1);

        // Swap out what getChildren returns; cache should still be used
        when(baseDir.getChildren()).thenReturn(new VirtualFile[0]);
        List<VirtualFile> second = scanner.scanDirectoryWithCache();

        assertThat(second).hasSize(1);
    }

    @Test
    void scanDirectoryWithCache_differentTimestamp_rescansDirectory() {
        VirtualFile file = mockFile("/project/Main.java");
        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{file});

        List<VirtualFile> first = scanner.scanDirectoryWithCache();
        assertThat(first).hasSize(1);

        // Simulate the directory being modified (new timestamp)
        when(baseDir.getTimeStamp()).thenReturn(2000L);
        when(baseDir.getChildren()).thenReturn(new VirtualFile[0]);

        CachedProjectScanner freshScanner = new CachedProjectScanner(baseDir);
        List<VirtualFile> second = freshScanner.scanDirectoryWithCache();

        assertThat(second).isEmpty();
    }

    @Test
    void scanDirectoryWithCache_subdirectory_recursesAndCollectsFiles() {
        VirtualFile subDir = mockDir("/project/src");
        VirtualFile file = mockFile("/project/src/Main.java");

        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{subDir});
        when(subDir.getChildren()).thenReturn(new VirtualFile[]{file});

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).containsExactly(file);
    }

    @Test
    void scanDirectoryWithCache_directoryOnlyNoFiles_returnsEmptyList() {
        VirtualFile subDir = mockDir("/project/empty");
        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{subDir});
        when(subDir.getChildren()).thenReturn(new VirtualFile[0]);

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).isEmpty();
    }

    @Test
    void scanDirectoryWithCache_largeDirectory_parallelBatchingCollectsAllFiles() {
        // 25 files triggers the parallel processing branch (threshold > 20)
        VirtualFile[] children = new VirtualFile[25];
        for (int i = 0; i < 25; i++) {
            children[i] = mockFile("/project/File" + i + ".java");
        }
        when(baseDir.getChildren()).thenReturn(children);

        List<VirtualFile> result = scanner.scanDirectoryWithCache();

        assertThat(result).hasSize(25);
    }

    @Test
    void scanDirectoryWithCache_gitignoreRespected_excludesIgnoredDirectory() throws Exception {
        VirtualFile gitignoreFile = mock(VirtualFile.class);
        when(baseDir.findChild(".gitignore")).thenReturn(gitignoreFile);
        when(gitignoreFile.getPath()).thenReturn("/project/.gitignore");
        vfsUtilStatic.when(() -> VfsUtilCore.loadText(gitignoreFile)).thenReturn("build/\n");

        VirtualFile buildDir = mockDir("/project/build");
        VirtualFile srcDir = mockDir("/project/src");
        VirtualFile srcFile = mockFile("/project/src/Main.java");

        when(baseDir.getChildren()).thenReturn(new VirtualFile[]{buildDir, srcDir});
        when(srcDir.getChildren()).thenReturn(new VirtualFile[]{srcFile});

        // Create a new scanner so the GitignoreParser sees the updated .gitignore stub
        CachedProjectScanner scannerWithGitignore = new CachedProjectScanner(baseDir);
        List<VirtualFile> result = scannerWithGitignore.scanDirectoryWithCache();

        assertThat(result).containsExactly(srcFile);
        assertThat(result).doesNotContain(buildDir);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private VirtualFile mockFile(String path) {
        VirtualFile f = mock(VirtualFile.class);
        when(f.getPath()).thenReturn(path);
        when(f.isDirectory()).thenReturn(false);
        return f;
    }

    private VirtualFile mockDir(String path) {
        VirtualFile d = mock(VirtualFile.class);
        when(d.getPath()).thenReturn(path);
        when(d.isDirectory()).thenReturn(true);
        return d;
    }
}
