package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ContentExtractorTest {

    private ContentExtractor contentExtractor;
    private VirtualFile mockFile;
    private DevoxxGenieStateService mockStateService;

    @BeforeEach
    public void setUp() {
        contentExtractor = new ContentExtractor();
        mockFile = mock(VirtualFile.class);
        mockStateService = mock(DevoxxGenieStateService.class);

        // Setup basic mock behavior
        when(mockFile.getPath()).thenReturn("/test/path/TestFile.java");
    }

    @Test
    public void testExtractFileContent_Success() throws IOException {
        try (MockedStatic<ReadAction> readActionMock = mockStatic(ReadAction.class);
             MockedStatic<DevoxxGenieStateService> stateServiceMock = mockStatic(DevoxxGenieStateService.class)) {

            // Mock DevoxxGenieStateService
            stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            when(mockStateService.getExcludeJavaDoc()).thenReturn(true);

            // Setup content for the mock file
            String fileContent = "public class Test { }";
            InputStream contentStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
            when(mockFile.getInputStream()).thenReturn(contentStream);

            // Execute
            String result = contentExtractor.extractFileContent(mockFile);

            // Verify
            assertTrue(result.contains("/test/path/TestFile.java"));
            assertTrue(result.contains("public class Test { }"));
        }
    }

    @Test
    public void testExtractFileContent_IOExceptionHandling() throws IOException {
        // Setup exception throwing when attempting to read the file
        when(mockFile.getInputStream()).thenThrow(new IOException("Test IO exception"));

        // Execute
        String result = contentExtractor.extractFileContent(mockFile);

        // Verify
        assertTrue(result.contains("/test/path/TestFile.java"));
        assertTrue(result.contains("Error reading file content: Test IO exception"));
    }

    @Test
    public void testExtractFileContent_WithOtherExceptionHandling() throws IOException {
        // Setup a RuntimeException when trying to read the file
        when(mockFile.getInputStream()).thenThrow(new RuntimeException("Unexpected error"));

        // Execute
        String result = contentExtractor.extractFileContent(mockFile);

        // Verify
        assertTrue(result.contains("/test/path/TestFile.java"));
        assertTrue(result.contains("Error reading file content: Unexpected error"));
    }

    @Test
    public void testCombineContent() {
        // Setup test data
        String directoryStructure = "src/\n  main/\n  test/\n";
        String fileContents = "--- TestFile.java ---\npublic class TestFile { }";

        // Execute
        String result = contentExtractor.combineContent(directoryStructure, fileContents);

        // Verify
        assertTrue(result.startsWith("Directory Structure:"));
        assertTrue(result.contains(directoryStructure));
        assertTrue(result.contains("File Contents:"));
        assertTrue(result.contains(fileContents));
    }

    @Test
    public void testJavadocRemoval_WhenEnabled() throws IOException {
        // Setup content with Javadoc
        String fileContent = "/**\n * Test Javadoc comment\n */\npublic class Test {\n    // Regular comment\n    private String field;\n}";
        InputStream contentStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        when(mockFile.getInputStream()).thenReturn(contentStream);

        // Mock DevoxxGenieStateService
        try (MockedStatic<DevoxxGenieStateService> mockedStatic = mockStatic(DevoxxGenieStateService.class)) {
            mockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            when(mockStateService.getExcludeJavaDoc()).thenReturn(true);

            // Execute
            String result = contentExtractor.extractFileContent(mockFile);

            // Verify Javadoc is removed
            assertFalse(result.contains("* Test Javadoc comment"));
            assertTrue(result.contains("public class Test {"));
            assertTrue(result.contains("// Regular comment"));
        }
    }

    @Test
    public void testJavadocRetention_WhenDisabled() throws IOException {
        // Setup content with Javadoc
        String fileContent = "/**\n * Test Javadoc comment\n */\npublic class Test {\n    // Regular comment\n    private String field;\n}";
        InputStream contentStream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        when(mockFile.getInputStream()).thenReturn(contentStream);

        // Mock DevoxxGenieStateService
        try (MockedStatic<DevoxxGenieStateService> mockedStatic = mockStatic(DevoxxGenieStateService.class)) {
            mockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            when(mockStateService.getExcludeJavaDoc()).thenReturn(false);

            // Execute
            String result = contentExtractor.extractFileContent(mockFile);

            // Verify Javadoc is retained
            assertTrue(result.contains("* Test Javadoc comment"));
            assertTrue(result.contains("public class Test {"));
            assertTrue(result.contains("// Regular comment"));
        }
    }
}
