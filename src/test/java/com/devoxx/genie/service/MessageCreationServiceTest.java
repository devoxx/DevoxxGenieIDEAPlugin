package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.ImageUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.devoxx.genie.action.AddSnippetAction.SELECTED_TEXT_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageCreationServiceTest {

    private MessageCreationService messageCreationService;

    @Mock
    private Project mockProject;

    @Mock
    private ChatMessageContext mockChatMessageContext;

    @Mock
    private LanguageModel mockLanguageModel;

    @Mock
    private FileListManager mockFileListManager;

    @Mock
    private VirtualFile mockVirtualFile;

    @Mock
    private Document mockDocument;

    @Mock
    private FileDocumentManager mockFileDocumentManager;

    @Mock
    private DevoxxGenieStateService mockStateService;

    @Mock
    private SemanticSearchService mockSemanticSearchService;

    @Mock
    private EditorInfo mockEditorInfo;

    @BeforeEach
    void setUp() {
        messageCreationService = new MessageCreationService();

        // Set up common stubs that many tests will need
        lenient().when(mockChatMessageContext.getProject()).thenReturn(mockProject);
        lenient().when(mockChatMessageContext.getUserPrompt()).thenReturn("Test prompt");
        lenient().when(mockChatMessageContext.getLanguageModel()).thenReturn(mockLanguageModel);
    }

    @Test
    void testGetInstance() {
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {
            com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);

            doReturn(messageCreationService).when(mockApplication).getService(MessageCreationService.class);

            MessageCreationService result = MessageCreationService.getInstance();
            assertNotNull(result);
            assertEquals(messageCreationService, result);
        }
    }

    @Test
    void testAddUserMessageToContextWithFullContext() {
        String context = "Full context content";
        when(mockChatMessageContext.getFilesContext()).thenReturn(context);
        when(mockChatMessageContext.getProject()).thenReturn(mockProject);
        when(mockChatMessageContext.getUserPrompt()).thenReturn("Test prompt");

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            when(mockStateService.getUseSimpleDiff()).thenReturn(false);

            try (MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class)) {
                fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);
                when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());

                messageCreationService.addUserMessageToContext(mockChatMessageContext);

                verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
            }
        }
    }

    @Test
    void testAddUserMessageToContextWithEmptyContext() {
        when(mockChatMessageContext.getFilesContext()).thenReturn(null);
        when(mockChatMessageContext.getEditorInfo()).thenReturn(mockEditorInfo);
        when(mockEditorInfo.getSelectedText()).thenReturn(null);
        when(mockEditorInfo.getSelectedFiles()).thenReturn(null);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ChatMessageContextUtil> chatMessageContextUtilMockedStatic = Mockito.mockStatic(ChatMessageContextUtil.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            chatMessageContextUtilMockedStatic.when(() -> ChatMessageContextUtil.isOpenAIo1Model(any())).thenReturn(false);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);

            when(mockStateService.getGitDiffActivated()).thenReturn(false);
            when(mockStateService.getRagActivated()).thenReturn(false);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
        }
    }

    @Test
    void testAddImages() {
        when(mockChatMessageContext.getFilesContext()).thenReturn(null);
        when(mockChatMessageContext.getEditorInfo()).thenReturn(mockEditorInfo);

        List<VirtualFile> imageFiles = Collections.singletonList(mockVirtualFile);
        byte[] testImageData = "test image data".getBytes(StandardCharsets.UTF_8);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ChatMessageContextUtil> chatMessageContextUtilMockedStatic = Mockito.mockStatic(ChatMessageContextUtil.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class);
             MockedStatic<ImageUtil> imageUtilMockedStatic = Mockito.mockStatic(ImageUtil.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            chatMessageContextUtilMockedStatic.when(() -> ChatMessageContextUtil.isOpenAIo1Model(any())).thenReturn(false);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);

            when(mockStateService.getGitDiffActivated()).thenReturn(false);
            when(mockStateService.getRagActivated()).thenReturn(false);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(imageFiles);
            when(mockVirtualFile.contentsToByteArray()).thenReturn(testImageData);
            when(mockChatMessageContext.getUserMessage()).thenReturn(UserMessage.from("Test message"));

            imageUtilMockedStatic.when(() -> ImageUtil.getImageMimeType(any())).thenReturn("image/jpeg");

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext, times(2)).setUserMessage(any(UserMessage.class));
        } catch (IOException e) {
            fail("Exception should not be thrown", e);
        }
    }

    @Test
    void testConstructUserMessageWithFullContextWithGitDiff() {
        String context = "Full context content";
        when(mockChatMessageContext.getFilesContext()).thenReturn(context);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);

            when(mockStateService.getUseSimpleDiff()).thenReturn(true);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
        }
    }

    @Test
    void testConstructUserMessageWithCombinedContextWithRag() {
        when(mockChatMessageContext.getFilesContext()).thenReturn(null);
        when(mockChatMessageContext.getEditorInfo()).thenReturn(mockEditorInfo);
        when(mockEditorInfo.getSelectedText()).thenReturn(null);
        when(mockEditorInfo.getSelectedFiles()).thenReturn(null);

        Map<String, SearchResult> searchResults = new HashMap<>();
        searchResults.put("testFile.java", new SearchResult(0.95, "test content"));

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ChatMessageContextUtil> chatMessageContextUtilMockedStatic = Mockito.mockStatic(ChatMessageContextUtil.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class);
             MockedStatic<SemanticSearchService> semanticSearchServiceMockedStatic = Mockito.mockStatic(SemanticSearchService.class);
             MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class);
             MockedStatic<Paths> pathsMockedStatic = Mockito.mockStatic(Paths.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            chatMessageContextUtilMockedStatic.when(() -> ChatMessageContextUtil.isOpenAIo1Model(any())).thenReturn(false);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);
            semanticSearchServiceMockedStatic.when(SemanticSearchService::getInstance).thenReturn(mockSemanticSearchService);

            when(mockStateService.getGitDiffActivated()).thenReturn(false);
            when(mockStateService.getRagActivated()).thenReturn(true);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());
            when(mockSemanticSearchService.search(any(), any())).thenReturn(searchResults);

            Path mockPath = mock(Path.class);
            pathsMockedStatic.when(() -> Paths.get(anyString())).thenReturn(mockPath);
            filesMockedStatic.when(() -> Files.readString(any(Path.class))).thenReturn("File content");

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext).setSemanticReferences(anyList());
            verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
            notificationUtilMockedStatic.verify(() -> NotificationUtil.sendNotification(any(), anyString()));
        }
    }

    @Test
    void testEditorContentWithSelectedText() {
        when(mockChatMessageContext.getFilesContext()).thenReturn(null);
        when(mockChatMessageContext.getEditorInfo()).thenReturn(mockEditorInfo);
        when(mockEditorInfo.getSelectedText()).thenReturn("Selected text content");
        when(mockEditorInfo.getSelectedFiles()).thenReturn(null);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ChatMessageContextUtil> chatMessageContextUtilMockedStatic = Mockito.mockStatic(ChatMessageContextUtil.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            chatMessageContextUtilMockedStatic.when(() -> ChatMessageContextUtil.isOpenAIo1Model(any())).thenReturn(false);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);

            when(mockStateService.getGitDiffActivated()).thenReturn(false);
            when(mockStateService.getRagActivated()).thenReturn(false);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
        }
    }

    @Test
    void testEditorContentWithSelectedFiles() {
        when(mockChatMessageContext.getFilesContext()).thenReturn(null);
        when(mockChatMessageContext.getEditorInfo()).thenReturn(mockEditorInfo);
        when(mockEditorInfo.getSelectedText()).thenReturn(null);

        List<VirtualFile> selectedFiles = Collections.singletonList(mockVirtualFile);
        when(mockEditorInfo.getSelectedFiles()).thenReturn(selectedFiles);
        when(mockVirtualFile.getName()).thenReturn("TestFile.java");

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<ChatMessageContextUtil> chatMessageContextUtilMockedStatic = Mockito.mockStatic(ChatMessageContextUtil.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(mockStateService);
            chatMessageContextUtilMockedStatic.when(() -> ChatMessageContextUtil.isOpenAIo1Model(any())).thenReturn(false);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(mockFileListManager);

            when(mockStateService.getGitDiffActivated()).thenReturn(false);
            when(mockStateService.getRagActivated()).thenReturn(false);
            when(mockFileListManager.getImageFiles(any(Project.class))).thenReturn(Collections.emptyList());

            // mock file content
            byte[] fileContent = "Test file content".getBytes(StandardCharsets.UTF_8);
            when(mockVirtualFile.contentsToByteArray()).thenReturn(fileContent);

            messageCreationService.addUserMessageToContext(mockChatMessageContext);

            verify(mockChatMessageContext).setUserMessage(any(UserMessage.class));
        } catch (IOException e) {
            fail("Exception should not be thrown", e);
        }
    }

    @Test
    void testCreateAttachedFilesContext() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(mockVirtualFile);

        try (MockedStatic<FileDocumentManager> fileDocumentManagerMockedStatic = Mockito.mockStatic(FileDocumentManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<ImageUtil> imageUtilMockedStatic = Mockito.mockStatic(ImageUtil.class)) {

            fileDocumentManagerMockedStatic.when(FileDocumentManager::getInstance).thenReturn(mockFileDocumentManager);
            com.intellij.openapi.application.Application mockAppManager = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockAppManager);

            // Mock runReadAction to immediately execute the runnable
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(mockAppManager).runReadAction(any(Runnable.class));

            when(mockVirtualFile.getFileType()).thenReturn(new com.intellij.openapi.fileTypes.FileType() {
                @Override
                public @NotNull String getName() {
                    return "JAVA";
                }

                @Override
                public @NotNull String getDescription() {
                    return "";
                }

                @Override
                public @NotNull String getDefaultExtension() {
                    return "java";
                }

                @Override
                public javax.swing.Icon getIcon() {
                    return null;
                }

                @Override
                public boolean isBinary() {
                    return false;
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }
            });

            when(mockVirtualFile.getName()).thenReturn("TestFile.java");
            when(mockFileDocumentManager.getDocument(mockVirtualFile)).thenReturn(mockDocument);
            when(mockDocument.getText()).thenReturn("Test file content");

            String result = messageCreationService.createAttachedFilesContext(mockProject, files);

            assertNotNull(result);
            assertTrue(result.contains("Filename: TestFile.java"));
            assertTrue(result.contains("Test file content"));
        }
    }

    @Test
    void testCreateAttachedFilesContextWithUnknownFile() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(mockVirtualFile);

        try (MockedStatic<FileDocumentManager> fileDocumentManagerMockedStatic = Mockito.mockStatic(FileDocumentManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class)) {

            fileDocumentManagerMockedStatic.when(FileDocumentManager::getInstance).thenReturn(mockFileDocumentManager);
            com.intellij.openapi.application.Application mockAppManager = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockAppManager);

            // Mock runReadAction to immediately execute the runnable
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(mockAppManager).runReadAction(any(Runnable.class));

            when(mockVirtualFile.getFileType()).thenReturn(new com.intellij.openapi.fileTypes.FileType() {
                @Override
                public @NotNull String getName() {
                    return "UNKNOWN";
                }

                @Override
                public @NotNull String getDescription() {
                    return "";
                }

                @Override
                public @NotNull String getDefaultExtension() {
                    return "";
                }

                @Override
                public javax.swing.Icon getIcon() {
                    return null;
                }

                @Override
                public boolean isBinary() {
                    return false;
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }
            });

            when(mockVirtualFile.getName()).thenReturn("Unknown.file");
            when(mockVirtualFile.getUserData(SELECTED_TEXT_KEY)).thenReturn("Selected snippet content");

            String result = messageCreationService.createAttachedFilesContext(mockProject, files);

            assertNotNull(result);
            assertTrue(result.contains("Filename: Unknown.file"));
            assertTrue(result.contains("Code Snippet: Selected snippet content"));
        }
    }

    @Test
    void testCreateAttachedFilesContextWithUnsupportedType() {
        List<VirtualFile> files = new ArrayList<>();
        files.add(mockVirtualFile);

        try (MockedStatic<FileDocumentManager> fileDocumentManagerMockedStatic = Mockito.mockStatic(FileDocumentManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<ImageUtil> imageUtilMockedStatic = Mockito.mockStatic(ImageUtil.class);
             MockedStatic<NotificationUtil> notificationUtilMockedStatic = Mockito.mockStatic(NotificationUtil.class)) {

            fileDocumentManagerMockedStatic.when(FileDocumentManager::getInstance).thenReturn(mockFileDocumentManager);
            com.intellij.openapi.application.Application mockAppManager = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockAppManager);

            // Mock runReadAction to immediately execute the runnable
            doAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(mockAppManager).runReadAction(any(Runnable.class));

            when(mockVirtualFile.getFileType()).thenReturn(new com.intellij.openapi.fileTypes.FileType() {
                @Override
                public @NotNull String getName() {
                    return "BINARY";
                }

                @Override
                public @NotNull String getDescription() {
                    return "";
                }

                @Override
                public @NotNull String getDefaultExtension() {
                    return "bin";
                }

                @Override
                public javax.swing.Icon getIcon() {
                    return null;
                }

                @Override
                public boolean isBinary() {
                    return true;
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }
            });

            when(mockVirtualFile.getName()).thenReturn("Binary.bin");
            when(mockFileDocumentManager.getDocument(mockVirtualFile)).thenReturn(null);
            imageUtilMockedStatic.when(() -> ImageUtil.isImageFile(any())).thenReturn(false);

            String result = messageCreationService.createAttachedFilesContext(mockProject, files);

            assertNotNull(result);
            // Should be empty as file type is not supported and notification was sent
            assertEquals("", result);
            notificationUtilMockedStatic.verify(() -> NotificationUtil.sendNotification(mockProject, "File type not supported: Binary.bin"));
        }
    }

    @Test
    void testExtractFileReferences() {
        Map<String, SearchResult> searchResults = new HashMap<>();
        searchResults.put("file1.java", new SearchResult(0.95, "content1"));
        searchResults.put("file2.java", new SearchResult(0.85, "content2"));

        List<SemanticFile> result = MessageCreationService.extractFileReferences(searchResults);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(file -> file.filePath().equals("file1.java") && Math.abs(file.score() - 0.95) < 0.001));
        assertTrue(result.stream().anyMatch(file -> file.filePath().equals("file2.java") && Math.abs(file.score() - 0.85) < 0.001));
    }
}
