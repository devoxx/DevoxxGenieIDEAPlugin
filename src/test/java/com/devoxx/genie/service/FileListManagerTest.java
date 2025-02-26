package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.util.ImageUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class FileListManagerTest extends AbstractLightPlatformTestCase {

    private FileListManager fileListManager;

    @Mock
    private Project mockProject;

    @Mock
    private VirtualFile mockFile;

    @Mock
    private VirtualFile mockImageFile;

    @Mock
    private FileListObserver mockObserver;

    private static final String PROJECT_HASH = "project-hash-123";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create our test instance directly
        fileListManager = new FileListManager();

        // Set up basic mocks
        when(mockProject.getLocationHash()).thenReturn(PROJECT_HASH);

        // Set up file names
        when(mockFile.getName()).thenReturn("test.txt");
        when(mockImageFile.getName()).thenReturn("test.png");

        // Use MockedStatic for ImageUtil.isImageFile
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockImageFile)).thenReturn(true);

            // Add an observer to the manager
            fileListManager.addObserver(mockProject, mockObserver);
        }
    }

    @Test
    void testAddFile() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            fileListManager.addFile(mockProject, mockFile);

            List<VirtualFile> files = fileListManager.getFiles(mockProject);
            assertEquals("Should have one file after adding", 1, files.size());
            assertEquals("The file should be the one we added", mockFile, files.get(0));

            // Verify observer notification
            verify(mockObserver, times(1)).fileAdded(mockFile);
        }
    }

    @Test
    void testAddImageFile() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockImageFile)).thenReturn(true);

            fileListManager.addFile(mockProject, mockImageFile);

            List<VirtualFile> allFiles = fileListManager.getFiles(mockProject);
            List<VirtualFile> imageFiles = fileListManager.getImageFiles(mockProject);

            assertEquals("Should have one file total after adding", 1, allFiles.size());
            assertEquals("Should have one image file after adding", 1, imageFiles.size());
            assertEquals("The image file should be the one we added", mockImageFile, imageFiles.get(0));

            // Verify observer notification
            verify(mockObserver, times(1)).fileAdded(mockImageFile);
        }
    }

    @Test
    void testAddFiles() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            VirtualFile mockFile2 = mock(VirtualFile.class);
            when(mockFile2.getName()).thenReturn("test2.txt");

            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile2)).thenReturn(false);

            List<VirtualFile> filesToAdd = Arrays.asList(mockFile, mockFile2);
            fileListManager.addFiles(mockProject, filesToAdd);

            List<VirtualFile> files = fileListManager.getFiles(mockProject);
            assertEquals("Should have two files after adding", 2, files.size());
            assertTrue("Files should contain first file", files.contains(mockFile));
            assertTrue("Files should contain second file", files.contains(mockFile2));

            // Verify batch notification
            verify(mockObserver, times(1)).filesAdded(anyList());
        }
    }

    @Test
    void testAddFilesWithDuplicates() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            // Add a file first
            fileListManager.addFile(mockProject, mockFile);

            // Clear invocations to reset observer call count
            clearInvocations(mockObserver);

            // Try to add the same file again in a batch
            fileListManager.addFiles(mockProject, Collections.singletonList(mockFile));

            List<VirtualFile> files = fileListManager.getFiles(mockProject);
            assertEquals("Should still have one file after adding duplicate", 1, files.size());

            // Verify no observer notification for duplicates
            verify(mockObserver, never()).filesAdded(anyList());
        }
    }

    @Test
    void testRemoveFile() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            // Add and then remove a file
            fileListManager.addFile(mockProject, mockFile);
            fileListManager.removeFile(mockProject, mockFile);

            List<VirtualFile> files = fileListManager.getFiles(mockProject);
            assertTrue("File list should be empty after removal", files.isEmpty());
        }
    }

    @Test
    void testStoreAndGetPreviouslyAddedFiles() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            fileListManager.addFile(mockProject, mockFile);
            fileListManager.storeAddedFiles(mockProject);

            List<VirtualFile> previousFiles = fileListManager.getPreviouslyAddedFiles(mockProject);
            assertEquals("Should have one previously added file", 1, previousFiles.size());
            assertEquals("The previously added file should match", mockFile, previousFiles.get(0));
        }
    }

    @Test
    void testIsEmpty() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            assertTrue("Should be empty initially", fileListManager.isEmpty(mockProject));

            fileListManager.addFile(mockProject, mockFile);
            assertFalse("Should not be empty after adding file", fileListManager.isEmpty(mockProject));

            fileListManager.removeFile(mockProject, mockFile);
            assertTrue("Should be empty after removing file", fileListManager.isEmpty(mockProject));
        }
    }

    @Test
    void testSize() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockImageFile)).thenReturn(true);

            assertEquals("Initial size should be 0", 0, fileListManager.size(mockProject));

            fileListManager.addFile(mockProject, mockFile);
            assertEquals("Size should be 1 after adding file", 1, fileListManager.size(mockProject));

            fileListManager.addFile(mockProject, mockImageFile);
            assertEquals("Size should be 2 after adding another file", 2, fileListManager.size(mockProject));
        }
    }

    @Test
    void testContains() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            VirtualFile otherFile = mock(VirtualFile.class);
            when(otherFile.getName()).thenReturn("other.txt");
            imageUtilMock.when(() -> ImageUtil.isImageFile(otherFile)).thenReturn(false);

            assertFalse("Should not contain file initially", fileListManager.contains(mockProject, mockFile));

            fileListManager.addFile(mockProject, mockFile);
            assertTrue("Should contain file after adding it", fileListManager.contains(mockProject, mockFile));

            assertFalse("Should not contain a different file", fileListManager.contains(mockProject, otherFile));
        }
    }

    @Test
    void testClear() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockImageFile)).thenReturn(true);

            fileListManager.addFile(mockProject, mockFile);
            fileListManager.addFile(mockProject, mockImageFile);
            fileListManager.storeAddedFiles(mockProject);

            fileListManager.clear(mockProject);

            assertTrue("Files should be empty after clear", fileListManager.isEmpty(mockProject));
            assertTrue("Previously added files should be empty after clear",
                    fileListManager.getPreviouslyAddedFiles(mockProject).isEmpty());

            // Verify observer notification
            verify(mockObserver, times(1)).allFilesRemoved();
        }
    }

    @Test
    void testMultipleProjects() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            // Set up a second project
            Project mockProject2 = mock(Project.class);
            when(mockProject2.getLocationHash()).thenReturn("project-hash-456");

            VirtualFile mockFile2 = mock(VirtualFile.class);
            when(mockFile2.getName()).thenReturn("file2.txt");

            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile2)).thenReturn(false);

            // Add files to both projects
            fileListManager.addFile(mockProject, mockFile);
            fileListManager.addFile(mockProject2, mockFile2);

            // Check that files are kept separate
            List<VirtualFile> project1Files = fileListManager.getFiles(mockProject);
            List<VirtualFile> project2Files = fileListManager.getFiles(mockProject2);

            assertEquals("Project 1 should have 1 file", 1, project1Files.size());
            assertEquals("Project 1 should have the correct file", mockFile, project1Files.get(0));

            assertEquals("Project 2 should have 1 file", 1, project2Files.size());
            assertEquals("Project 2 should have the correct file", mockFile2, project2Files.get(0));

            // Clear one project and check that the other is unaffected
            fileListManager.clear(mockProject);
            assertTrue("Project 1 should be empty after clear", fileListManager.isEmpty(mockProject));
            assertFalse("Project 2 should not be affected by clearing Project 1",
                    fileListManager.isEmpty(mockProject2));
        }
    }

    @Test
    void testObserverNotifications() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            // Add a second observer
            FileListObserver mockObserver2 = mock(FileListObserver.class);
            fileListManager.addObserver(mockProject, mockObserver2);

            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile)).thenReturn(false);

            // Test single file add
            fileListManager.addFile(mockProject, mockFile);
            verify(mockObserver, times(1)).fileAdded(mockFile);
            verify(mockObserver2, times(1)).fileAdded(mockFile);

            // Test batch add
            VirtualFile mockFile2 = mock(VirtualFile.class);
            when(mockFile2.getName()).thenReturn("test2.txt");
            imageUtilMock.when(() -> ImageUtil.isImageFile(mockFile2)).thenReturn(false);

            fileListManager.addFiles(mockProject, Collections.singletonList(mockFile2));
            verify(mockObserver, times(1)).filesAdded(anyList());
            verify(mockObserver2, times(1)).filesAdded(anyList());

            // Test clear
            fileListManager.clear(mockProject);
            verify(mockObserver, times(1)).allFilesRemoved();
            verify(mockObserver2, times(1)).allFilesRemoved();
        }
    }
}
