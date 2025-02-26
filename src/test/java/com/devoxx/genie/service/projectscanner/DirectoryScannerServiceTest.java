package com.devoxx.genie.service.projectscanner;

import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DirectoryScannerServiceTest {

    private DirectoryScannerService scannerService;

    @Mock
    private VirtualFile mockFileShort;

    @Mock
    private VirtualFile mockFileLong;

    @Mock
    private VirtualFile mockFileMiddle;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        scannerService = new DirectoryScannerService();

        // Setup our mock directories with different paths
        when(mockFileShort.getPath()).thenReturn("/project");
        when(mockFileMiddle.getPath()).thenReturn("/project/src");
        when(mockFileLong.getPath()).thenReturn("/project/src/main/java");
    }

    @Test
    public void testAddDirectory_FirstAddition() {
        // Test adding a directory for the first time
        scannerService.addDirectory(mockFileShort);

        assertEquals(1, scannerService.getUniqueDirectories().size());
        assertTrue(scannerService.getUniqueDirectories().containsKey(Paths.get(mockFileShort.getPath()).normalize().toAbsolutePath().toString()));
        assertEquals(mockFileShort, scannerService.getUniqueDirectories().values().iterator().next());
    }

    @Test
    public void testAddDirectory_DuplicateAddition() {
        // Add a directory first time
        scannerService.addDirectory(mockFileShort);

        // Add the same directory again
        scannerService.addDirectory(mockFileShort);

        assertEquals(1, scannerService.getUniqueDirectories().size());
    }

    @Test
    public void testAddDirectory_MultipleDifferentDirectories() {
        // Add several different directories
        scannerService.addDirectory(mockFileShort);
        scannerService.addDirectory(mockFileMiddle);
        scannerService.addDirectory(mockFileLong);

        // Should have all three in the map
        assertEquals(3, scannerService.getUniqueDirectories().size());

        // Verify all paths are correctly normalized and stored
        String shortPath = Paths.get(mockFileShort.getPath()).normalize().toAbsolutePath().toString();
        String middlePath = Paths.get(mockFileMiddle.getPath()).normalize().toAbsolutePath().toString();
        String longPath = Paths.get(mockFileLong.getPath()).normalize().toAbsolutePath().toString();

        assertTrue(scannerService.getUniqueDirectories().containsKey(shortPath));
        assertTrue(scannerService.getUniqueDirectories().containsKey(middlePath));
        assertTrue(scannerService.getUniqueDirectories().containsKey(longPath));
    }

    @Test
    public void testGetHighestCommonRoot_EmptyMap() {
        // No directories added yet
        Optional<VirtualFile> result = scannerService.getHighestCommonRoot();

        // Should return empty Optional
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetHighestCommonRoot_SingleDirectory() {
        // Add only one directory
        scannerService.addDirectory(mockFileLong);

        // The only directory should be returned
        Optional<VirtualFile> result = scannerService.getHighestCommonRoot();

        assertTrue(result.isPresent());
        assertEquals(mockFileLong, result.get());
    }

    @Test
    public void testGetHighestCommonRoot_MultipleDirectories() {
        // Add directories with paths of different lengths
        scannerService.addDirectory(mockFileShort);  // Shortest path
        scannerService.addDirectory(mockFileMiddle);
        scannerService.addDirectory(mockFileLong);   // Longest path

        // Should return the directory with the shortest path
        Optional<VirtualFile> result = scannerService.getHighestCommonRoot();

        assertTrue(result.isPresent());
        assertEquals(mockFileShort, result.get());
    }

    @Test
    public void testGetHighestCommonRoot_DirectoriesInReverseOrder() {
        // Add directories in reverse order (longest path first)
        scannerService.addDirectory(mockFileLong);   // Longest path
        scannerService.addDirectory(mockFileMiddle);
        scannerService.addDirectory(mockFileShort);  // Shortest path

        // Should still return the directory with the shortest path
        Optional<VirtualFile> result = scannerService.getHighestCommonRoot();

        assertTrue(result.isPresent());
        assertEquals(mockFileShort, result.get());
    }

    @Test
    public void testNormalizationOfPaths() {
        // Create a mock with a path containing unnecessary components
        VirtualFile mockFileWithDots = mock(VirtualFile.class);
        when(mockFileWithDots.getPath()).thenReturn("/project/./unnecessary/../project");

        scannerService.addDirectory(mockFileWithDots);

        // The normalized path should be used as the key
        String normalizedPath = Paths.get("/project/project").normalize().toAbsolutePath().toString();
        assertTrue(scannerService.getUniqueDirectories().containsKey(normalizedPath));
    }

    @Test
    public void testAddDirectory_NullInput() {
        // Test behavior with null input
        assertThrows(IllegalArgumentException.class, () -> scannerService.addDirectory(null));
    }

    @Test
    public void testOverwriteWithSamePathDifferentFile() {
        // Create two different VirtualFile objects with the same path
        VirtualFile mockFile1 = mock(VirtualFile.class);
        VirtualFile mockFile2 = mock(VirtualFile.class);

        String commonPath = "/project/same/path";
        when(mockFile1.getPath()).thenReturn(commonPath);
        when(mockFile2.getPath()).thenReturn(commonPath);

        // Add the first file
        scannerService.addDirectory(mockFile1);

        // Verify it's there
        String normalizedPath = Paths.get(commonPath).normalize().toAbsolutePath().toString();
        assertEquals(mockFile1, scannerService.getUniqueDirectories().get(normalizedPath));

        // Add the second file with the same path
        scannerService.addDirectory(mockFile2);

        // Map size should still be 1
        assertEquals(1, scannerService.getUniqueDirectories().size());

        // The second file should have replaced the first
        assertEquals(mockFile2, scannerService.getUniqueDirectories().get(normalizedPath));
    }
}
