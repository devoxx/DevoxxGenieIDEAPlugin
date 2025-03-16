package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProjectScannerServiceTest  {

    private ProjectScannerService projectScannerService;
    private FileScanner mockFileScanner;
    private ContentExtractor mockContentExtractor;
    private TokenCalculator mockTokenCalculator;
    private Project mockProject;
    private VirtualFile mockDirectory;
    private VirtualFile mockFile;
    private VirtualFile mockRootDirectory;
    private VirtualFile mockNonIncludedFile;
    private DevoxxGenieStateService mockStateService;
    private ProjectFileIndex mockProjectFileIndex;

    @BeforeEach
    public void init() {
        // Create mocks
        mockFileScanner = mock(FileScanner.class);
        mockContentExtractor = mock(ContentExtractor.class);
        mockTokenCalculator = mock(TokenCalculator.class);
        mockProject = mock(Project.class);
        mockDirectory = mock(VirtualFile.class);
        mockFile = mock(VirtualFile.class);
        mockRootDirectory = mock(VirtualFile.class);
        mockProjectFileIndex = mock(ProjectFileIndex.class);
        mockNonIncludedFile = mock(VirtualFile.class);

        // Setup DevoxxGenieStateService mock
        mockStateService = mock(DevoxxGenieStateService.class);

        // Add default behavior to prevent NPEs
        when(mockStateService.getExcludedDirectories()).thenReturn(List.of());
        when(mockStateService.getExcludedFiles()).thenReturn(List.of());
        when(mockStateService.getIncludedFileExtensions()).thenReturn(List.of("java", "kt", "xml"));
        when(mockStateService.getUseGitIgnore()).thenReturn(false);
        when(mockStateService.getExcludeJavaDoc()).thenReturn(false);

        // Set up common behavior
        when(mockDirectory.isDirectory()).thenReturn(true);
        when(mockDirectory.getName()).thenReturn("testDir");
        when(mockDirectory.getPath()).thenReturn("/project/testDir");

        when(mockFile.isDirectory()).thenReturn(false);
        when(mockFile.getName()).thenReturn("TestFile.java");
        when(mockFile.getPath()).thenReturn("/project/TestFile.java");

        when(mockNonIncludedFile.isDirectory()).thenReturn(false);
        when(mockNonIncludedFile.getName()).thenReturn("README.md");
        when(mockNonIncludedFile.getPath()).thenReturn("/project/README.md");

        when(mockRootDirectory.isDirectory()).thenReturn(true);
        when(mockRootDirectory.getName()).thenReturn("project");
        when(mockRootDirectory.getPath()).thenReturn("/project");

        // Set up FileScanner behavior
        when(mockFileScanner.scanProjectModules(mockProject)).thenReturn(mockRootDirectory);
        when(mockFileScanner.generateSourceTreeRecursive(any(VirtualFile.class), anyInt())).thenReturn("testDir/\n  TestFile.java\n");
        when(mockFileScanner.shouldIncludeFile(mockFile)).thenReturn(true);
        when(mockFileScanner.shouldIncludeFile(mockNonIncludedFile)).thenReturn(false);

        List<Path> includedFiles = new ArrayList<>();
        includedFiles.add(Paths.get("/project/TestFile.java"));
        when(mockFileScanner.getIncludedFiles()).thenReturn(includedFiles);

        // Set up ContentExtractor behavior
        when(mockContentExtractor.extractFileContent(mockFile)).thenReturn("\n--- /project/TestFile.java ---\npublic class TestFile {}");
        when(mockContentExtractor.combineContent(anyString(), anyString())).thenReturn("Directory Structure:\ntestDir/\n  TestFile.java\n\nFile Contents:\n\n--- /project/TestFile.java ---\npublic class TestFile {}");

        // Set up TokenCalculator behavior
        when(mockTokenCalculator.calculateTokens(anyString())).thenReturn(50);
        when(mockTokenCalculator.truncateToTokens(anyString(), anyInt(), anyBoolean())).thenAnswer(invocation -> {
            String content = invocation.getArgument(0);
            int maxTokens = invocation.getArgument(1);
            boolean isTokenCalculation = invocation.getArgument(2);

            // Simple simulation of truncation
            if (mockTokenCalculator.calculateTokens(content) > maxTokens) {
                return isTokenCalculation ?
                        "Truncated content" :
                        "Truncated content\n--- Project context truncated due to token limit ---\n";
            }
            return content;
        });

        projectScannerService = new ProjectScannerService();
        projectScannerService.setFileScanner(mockFileScanner);
        projectScannerService.setContentExtractor(mockContentExtractor);
        projectScannerService.setTokenCalculator(mockTokenCalculator);
        projectScannerService.setProjectFileIndex(mockProjectFileIndex);
    }

    @Test
    void testGetInstance() {
        try (MockedStatic<ApplicationManager> applicationManagerMock = mockStatic(ApplicationManager.class)) {
            // Mock the Application instance
            com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(mockApplication);

            ProjectScannerService mockService = mock(ProjectScannerService.class);
            when(mockApplication.getService(ProjectScannerService.class)).thenReturn(mockService);

            // Test
            ProjectScannerService result = ProjectScannerService.getInstance();

            // Verify
            assertNotNull(result);
            assertEquals(mockService, result);
            verify(mockApplication).getService(ProjectScannerService.class);
        }
    }

    @Test
    void testScanContent_WithNullDirectory() {
        // Setup
        List<VirtualFile> fileList = new ArrayList<>();
        fileList.add(mockFile);
        when(mockFileScanner.scanDirectory(eq(mockProjectFileIndex), eq(mockRootDirectory), any(ScanContentResult.class))).thenReturn(fileList);

        // Test - now we can directly call the public method
        String result = projectScannerService.scanContent(mockProject, null, 100, false, new ScanContentResult());

        // Verify
        assertNotNull(result);
        verify(mockFileScanner).scanProjectModules(mockProject);
        verify(mockFileScanner).generateSourceTreeRecursive(mockRootDirectory, 0);
        verify(mockFileScanner).scanDirectory(eq(mockProjectFileIndex), eq(mockRootDirectory), any(ScanContentResult.class));
        verify(mockContentExtractor).extractFileContent(mockFile);
        verify(mockContentExtractor).combineContent(anyString(), anyString());
        verify(mockTokenCalculator).truncateToTokens(anyString(), eq(100), eq(false));
    }

    @Test
    void testScanContent_WithDirectory() {
        // Setup
        List<VirtualFile> fileList = new ArrayList<>();
        fileList.add(mockFile);
        when(mockFileScanner.scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class))).thenReturn(fileList);

        // Test
        String result = projectScannerService.scanContent(mockProject, mockDirectory, 100, false, new ScanContentResult());

        // Verify
        assertNotNull(result);
        verify(mockFileScanner, never()).scanProjectModules(mockProject); // Should not be called with directory
        verify(mockFileScanner).generateSourceTreeRecursive(mockDirectory, 0);
        verify(mockFileScanner).scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class));
        verify(mockContentExtractor).extractFileContent(mockFile);
        verify(mockContentExtractor).combineContent(anyString(), anyString());
        verify(mockTokenCalculator).truncateToTokens(anyString(), eq(100), eq(false));
    }

    @Test
    void testScanContent_WithSingleFile() {
        // Test - direct method call
        String result = projectScannerService.scanContent(mockProject, mockFile, 100, false, new ScanContentResult());

        // Verify
        assertNotNull(result);
        assertTrue(result.startsWith("File:"));
        assertTrue(result.contains(mockFile.getName()));
        verify(mockFileScanner).shouldIncludeFile(mockFile);
        verify(mockContentExtractor).extractFileContent(mockFile);
        verify(mockFileScanner, never()).generateSourceTreeRecursive(any(), anyInt());
        verify(mockFileScanner, never()).scanDirectory(any(), any(), any());
        verify(mockContentExtractor, never()).combineContent(anyString(), anyString());
    }

    @Test
    void testHandleSingleFile_WithIncludedFile() {
        // Setup
        when(mockFileScanner.shouldIncludeFile(mockFile)).thenReturn(true);

        // Test - direct method call now possible
        String result = projectScannerService.handleSingleFile(mockFile);

        // Verify
        assertNotNull(result);
        assertTrue(result.startsWith("File:"));
        assertTrue(result.contains(mockFile.getName()));
        assertTrue(result.contains("File Contents:"));
        verify(mockFileScanner).shouldIncludeFile(mockFile);
        verify(mockContentExtractor).extractFileContent(mockFile);
    }

    @Test
    void testHandleSingleFile_WithExcludedFile() {
        // Setup
        when(mockFileScanner.shouldIncludeFile(mockNonIncludedFile)).thenReturn(false);

        // Test - direct method call
        String result = projectScannerService.handleSingleFile(mockNonIncludedFile);

        // Verify
        assertNotNull(result);
        assertTrue(result.startsWith("File:"));
        assertTrue(result.contains(mockNonIncludedFile.getName()));
        assertTrue(result.contains("File Contents:"));
        verify(mockFileScanner).shouldIncludeFile(mockNonIncludedFile);
        verify(mockContentExtractor, never()).extractFileContent(mockNonIncludedFile);
    }

    @Test
    void testExtractAllFileContents_WithMultipleFiles() {
        // Setup
        List<VirtualFile> fileList = new ArrayList<>();
        fileList.add(mockFile);

        VirtualFile mockFile2 = mock(VirtualFile.class);
        when(mockFile2.isDirectory()).thenReturn(false);
        when(mockFile2.getName()).thenReturn("AnotherFile.java");
        when(mockFile2.getPath()).thenReturn("/project/AnotherFile.java");
        when(mockContentExtractor.extractFileContent(mockFile2)).thenReturn("\n--- /project/AnotherFile.java ---\npublic class AnotherFile {}");

        fileList.add(mockFile2);

        // Test - direct method call
        String result = projectScannerService.extractAllFileContents(fileList);

        // Verify
        assertNotNull(result);
        verify(mockContentExtractor).extractFileContent(mockFile);
        verify(mockContentExtractor).extractFileContent(mockFile2);
        assertTrue(result.contains("TestFile.java"));
        assertTrue(result.contains("AnotherFile.java"));
    }

    @Test
    void testExtractAllFileContents_WithEmptyList() {
        // Setup
        List<VirtualFile> fileList = new ArrayList<>();

        // Test - direct method call
        String result = projectScannerService.extractAllFileContents(fileList);

        // Verify
        assertNotNull(result);
        assertEquals("", result);
        verify(mockContentExtractor, never()).extractFileContent(any());
    }

    @Test
    void testScanContent_DirectoryWithNoFiles() {
        try (MockedStatic<DevoxxGenieStateService> mockedSettings = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ProjectFileIndex> projectFileIndexMock = mockStatic(ProjectFileIndex.class)) {

            // First, set up your state service mock
            mockedSettings.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);

            // CRITICAL: Mock ProjectFileIndex.getInstance() to return a specific mock instance
            // Make sure this is defined BEFORE calling any method that might use it
            projectFileIndexMock.when(() -> ProjectFileIndex.getInstance(eq(mockProject)))
                    .thenReturn(mockProjectFileIndex);

            // Setup
            List<VirtualFile> emptyFileList = new ArrayList<>();

            // Configure mock behaviors
            when(mockFileScanner.generateSourceTreeRecursive(mockDirectory, 0)).thenReturn("testDir/\n");
            when(mockFileScanner.scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class))).thenReturn(emptyFileList);
            when(mockContentExtractor.combineContent(anyString(), eq(""))).thenReturn("Directory Structure:\ntestDir/\n\nFile Contents:\n");

            // Test
            String result = projectScannerService.scanContent(mockProject, mockDirectory, 100, false, new ScanContentResult());

            // Verify
            assertNotNull(result);
            verify(mockFileScanner).scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class));
            verify(mockContentExtractor, never()).extractFileContent(any());
            verify(mockContentExtractor).combineContent("testDir/\n", "");
        }
    }

    @Test
    void testScanProject_WithDirectory() {
        try (MockedStatic<ApplicationManager> applicationManagerMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> mockedSettings = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ProjectFileIndex> projectFileIndexMock = mockStatic(ProjectFileIndex.class)) {

            // Create a test-specific subclass that doesn't use ReadAction
            ProjectScannerService testScannerService = new ProjectScannerService() {
                @Override
                public ScanContentResult scanProject(Project project, VirtualFile startDirectory,
                                                     int windowContextMaxTokens, boolean isTokenCalculation) {
                    ScanContentResult scanContentResult = new ScanContentResult();

                    // Direct implementation without ReadAction
                    getFileScanner().reset();
                    getFileScanner().initGitignoreParser(project, startDirectory);
                    String content = scanContent(project, startDirectory, windowContextMaxTokens, isTokenCalculation, scanContentResult);

                    scanContentResult.setTokenCount(getTokenCalculator().calculateTokens(content));
                    scanContentResult.setContent(content);
                    scanContentResult.setFileCount(getFileScanner().getFileCount());
                    scanContentResult.setSkippedFileCount(getFileScanner().getSkippedFileCount());
                    scanContentResult.setSkippedDirectoryCount(getFileScanner().getSkippedDirectoryCount());
                    getFileScanner().getIncludedFiles().forEach(scanContentResult::addFile);

                    return scanContentResult;
                }
            };

            // Set our mocks on the testScannerService
            testScannerService.setFileScanner(mockFileScanner);
            testScannerService.setContentExtractor(mockContentExtractor);
            testScannerService.setTokenCalculator(mockTokenCalculator);
            testScannerService.setProjectFileIndex(mockProjectFileIndex);

            // Setup
            List<VirtualFile> fileList = new ArrayList<>();
            fileList.add(mockFile);

            when(mockFileScanner.scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class))).thenReturn(fileList);
            when(mockFileScanner.getFileCount()).thenReturn(1);
            when(mockFileScanner.getSkippedFileCount()).thenReturn(0);
            when(mockFileScanner.getSkippedDirectoryCount()).thenReturn(0);

            // Test
            ScanContentResult result = testScannerService.scanProject(mockProject, mockDirectory, 100, false);

            // Verify
            assertNotNull(result);
            assertEquals(50, result.getTokenCount());
            assertEquals(1, result.getFileCount());
            assertEquals(0, result.getSkippedFileCount());
            assertEquals(0, result.getSkippedDirectoryCount());

            // Verify correct methods were called
            verify(mockFileScanner).reset();
            verify(mockFileScanner).initGitignoreParser(mockProject, mockDirectory);
            verify(mockFileScanner).generateSourceTreeRecursive(mockDirectory, 0);
            verify(mockFileScanner).scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class));
            verify(mockContentExtractor).extractFileContent(mockFile);
            verify(mockContentExtractor).combineContent(anyString(), anyString());
            verify(mockTokenCalculator, atLeastOnce()).calculateTokens(anyString());
            verify(mockTokenCalculator).truncateToTokens(anyString(), eq(100), eq(false));
        }
    }

    @Test
    void testScanProject_WithTokenLimitExceeded() {
        try (MockedStatic<ApplicationManager> applicationManagerMock = mockStatic(ApplicationManager.class);
             MockedStatic<DevoxxGenieStateService> mockedSettings = mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ProjectFileIndex> projectFileIndexMock = mockStatic(ProjectFileIndex.class)) {

            // Create a test-specific subclass that doesn't use ReadAction
            ProjectScannerService testScannerService = new ProjectScannerService() {
                @Override
                public ScanContentResult scanProject(Project project, VirtualFile startDirectory,
                                                     int windowContextMaxTokens, boolean isTokenCalculation) {
                    ScanContentResult scanContentResult = new ScanContentResult();

                    // Direct implementation without ReadAction
                    getFileScanner().reset();
                    getFileScanner().initGitignoreParser(project, startDirectory);
                    String content = scanContent(project, startDirectory, windowContextMaxTokens, isTokenCalculation, scanContentResult);

                    scanContentResult.setTokenCount(getTokenCalculator().calculateTokens(content));
                    scanContentResult.setContent(content);
                    scanContentResult.setFileCount(getFileScanner().getFileCount());
                    scanContentResult.setSkippedFileCount(getFileScanner().getSkippedFileCount());
                    scanContentResult.setSkippedDirectoryCount(getFileScanner().getSkippedDirectoryCount());
                    getFileScanner().getIncludedFiles().forEach(scanContentResult::addFile);

                    return scanContentResult;
                }
            };

            // Set our mocks on the testScannerService
            testScannerService.setFileScanner(mockFileScanner);
            testScannerService.setContentExtractor(mockContentExtractor);
            testScannerService.setTokenCalculator(mockTokenCalculator);
            testScannerService.setProjectFileIndex(mockProjectFileIndex);

            // Setup
            List<VirtualFile> fileList = new ArrayList<>();
            fileList.add(mockFile);

            when(mockFileScanner.scanDirectory(eq(mockProjectFileIndex), eq(mockDirectory), any(ScanContentResult.class))).thenReturn(fileList);
            when(mockFileScanner.getFileCount()).thenReturn(1);
            when(mockFileScanner.getSkippedFileCount()).thenReturn(0);
            when(mockFileScanner.getSkippedDirectoryCount()).thenReturn(0);

            // Make token count exceed limit
            when(mockTokenCalculator.calculateTokens(anyString())).thenReturn(150);

            // Test
            ScanContentResult result = testScannerService.scanProject(mockProject, mockDirectory, 100, false);

            // Verify
            assertNotNull(result);
            assertEquals(150, result.getTokenCount());
            assertTrue(result.getContent().contains("--- Project context truncated due to token limit ---"));

            // Verify correct methods were called
            verify(mockTokenCalculator).truncateToTokens(anyString(), eq(100), eq(false));
        }
    }
}
