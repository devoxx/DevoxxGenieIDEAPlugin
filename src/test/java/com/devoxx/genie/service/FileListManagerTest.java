package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.util.ImageUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.devoxx.genie.action.AddSnippetAction.*;
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

    /**
     * Reproduces issue #783: Adding text selection to prompt context doesn't reset.
     *
     * Scenario:
     * 1. User selects code lines 10-20 from a file and adds it to context
     *    → selection metadata (SELECTED_TEXT_KEY, line numbers) is stored on the VirtualFile
     * 2. User removes the file from context
     * 3. User re-adds the complete file (no selection)
     * 4. BUG: The file still shows old line numbers because userData was never cleared
     */
    @Test
    void testRemoveFile_shouldClearSelectionMetadata_issue783() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            // Use a real LightVirtualFile so putUserData/getUserData works
            LightVirtualFile realFile = new LightVirtualFile("MyFile.java", "public class MyFile {}");
            imageUtilMock.when(() -> ImageUtil.isImageFile(realFile)).thenReturn(false);

            // Step 1: Simulate adding a code selection — this is what
            // ChatMessageContextUtil.addDefaultEditorInfoToMessageContext() does
            realFile.putUserData(ORIGINAL_FILE_KEY, realFile);
            realFile.putUserData(SELECTED_TEXT_KEY, "selected code");
            realFile.putUserData(SELECTION_START_KEY, 0);
            realFile.putUserData(SELECTION_END_KEY, 13);
            realFile.putUserData(SELECTION_START_LINE_KEY, 10);
            realFile.putUserData(SELECTION_END_LINE_KEY, 20);

            fileListManager.addFile(mockProject, realFile);

            // Verify the selection metadata is present
            assertNotNull("Selection text should be set", realFile.getUserData(SELECTED_TEXT_KEY));
            assertEquals("Start line should be 10", Integer.valueOf(10), realFile.getUserData(SELECTION_START_LINE_KEY));
            assertEquals("End line should be 20", Integer.valueOf(20), realFile.getUserData(SELECTION_END_LINE_KEY));

            // Step 2: Remove the file from context
            fileListManager.removeFile(mockProject, realFile);
            assertTrue("File should be removed from list", fileListManager.isEmpty(mockProject));

            // Step 3: Verify selection metadata is cleared after removal
            // This is the core assertion for issue #783:
            // After removing a file from context, the selection metadata should be cleared
            // so that re-adding the file shows it as a complete file, not a code snippet
            assertNull("SELECTED_TEXT_KEY should be cleared after removeFile",
                    realFile.getUserData(SELECTED_TEXT_KEY));
            assertNull("SELECTION_START_LINE_KEY should be cleared after removeFile",
                    realFile.getUserData(SELECTION_START_LINE_KEY));
            assertNull("SELECTION_END_LINE_KEY should be cleared after removeFile",
                    realFile.getUserData(SELECTION_END_LINE_KEY));
            assertNull("SELECTION_START_KEY should be cleared after removeFile",
                    realFile.getUserData(SELECTION_START_KEY));
            assertNull("SELECTION_END_KEY should be cleared after removeFile",
                    realFile.getUserData(SELECTION_END_KEY));
            assertNull("ORIGINAL_FILE_KEY should be cleared after removeFile",
                    realFile.getUserData(ORIGINAL_FILE_KEY));
        }
    }

    /**
     * Reproduces the second part of issue #783: clearing all files should also
     * clear selection metadata from each file.
     */
    @Test
    void testClear_shouldClearSelectionMetadata_issue783() {
        try (MockedStatic<ImageUtil> imageUtilMock = Mockito.mockStatic(ImageUtil.class)) {
            LightVirtualFile realFile = new LightVirtualFile("MyFile.java", "public class MyFile {}");
            imageUtilMock.when(() -> ImageUtil.isImageFile(realFile)).thenReturn(false);

            // Add file with selection metadata
            realFile.putUserData(SELECTED_TEXT_KEY, "selected code");
            realFile.putUserData(SELECTION_START_LINE_KEY, 5);
            realFile.putUserData(SELECTION_END_LINE_KEY, 15);

            fileListManager.addFile(mockProject, realFile);

            // Clear all files
            fileListManager.clear(mockProject);

            // Selection metadata should be gone
            assertNull("SELECTED_TEXT_KEY should be cleared after clear()",
                    realFile.getUserData(SELECTED_TEXT_KEY));
            assertNull("SELECTION_START_LINE_KEY should be cleared after clear()",
                    realFile.getUserData(SELECTION_START_LINE_KEY));
            assertNull("SELECTION_END_LINE_KEY should be cleared after clear()",
                    realFile.getUserData(SELECTION_END_LINE_KEY));
        }
    }
}
