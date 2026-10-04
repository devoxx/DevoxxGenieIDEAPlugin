package com.devoxx.genie.util;

import com.devoxx.genie.model.ChatContextParameters;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageContextUtilTest {

    private static final String TAB_ID = "tab-1";

    @Mock
    private Project project;
    @Mock
    private LanguageModel languageModel;
    @Mock
    private EditorFileButtonManager editorFileButtonManager;
    @Mock
    private Editor editor;
    @Mock
    private Document document;
    @Mock
    private SelectionModel selectionModel;
    @Mock
    private VirtualFile virtualFile;
    @Mock
    private VirtualFile attachedFile;
    @Mock
    private FileType fileType;
    @Mock
    private FileDocumentManager fileDocumentManager;
    @Mock
    private Application application;
    @Mock
    private DevoxxGenieStateService stateService;
    @Mock
    private FileListManager fileListManager;

    @Test
    void capturesCurrentSelectionAndConsumesAttachedFilesOnce() {
        when(languageModel.getProvider()).thenReturn(ModelProvider.CLIRunners);
        when(stateService.getTimeout()).thenReturn(60);
        when(editorFileButtonManager.getSelectedTextEditor()).thenReturn(editor);
        when(editor.getDocument()).thenReturn(document);
        when(editor.getSelectionModel()).thenReturn(selectionModel);
        when(selectionModel.getSelectedText()).thenReturn("selected code").thenReturn(null);
        when(selectionModel.getSelectionStart()).thenReturn(11);
        when(selectionModel.getSelectionEnd()).thenReturn(25);
        when(document.getLineNumber(11)).thenReturn(2);
        when(document.getLineNumber(24)).thenReturn(4);
        when(fileDocumentManager.getFile(document)).thenReturn(virtualFile);
        when(virtualFile.getCanonicalPath()).thenReturn(null);
        when(virtualFile.getPath()).thenReturn("/project/src/Main.java");
        when(virtualFile.getFileType()).thenReturn(fileType);
        when(fileType.getName()).thenReturn("JAVA");
        List<VirtualFile> attachedFiles = new ArrayList<>(List.of(attachedFile));
        when(fileListManager.getNonImageFiles(project, TAB_ID)).thenReturn(attachedFiles);
        when(fileListManager.getImageFiles(project, TAB_ID)).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            attachedFiles.clear();
            return null;
        }).when(fileListManager).clearNonImageFiles(project, TAB_ID);

        ChatContextParameters parameters = new ChatContextParameters(
                project,
                "Explain this selected code",
                languageModel,
                null,
                "Enter",
                editorFileButtonManager,
                null,
                false,
                TAB_ID);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
             MockedStatic<FileListManager> fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class);
             MockedStatic<ApplicationManager> applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
             MockedStatic<FileDocumentManager> fileDocumentManagerMockedStatic = Mockito.mockStatic(FileDocumentManager.class)) {

            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(fileListManager);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
            fileDocumentManagerMockedStatic.when(FileDocumentManager::getInstance).thenReturn(fileDocumentManager);
            doAnswer(invocation -> {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }).when(application).runReadAction(any(Runnable.class));

            ChatMessageContext firstPrompt = ChatMessageContextUtil.createContext(parameters);
            ChatMessageContext nextPrompt = ChatMessageContextUtil.createContext(parameters);

            assertNotNull(firstPrompt.getEditorInfo());
            assertEquals("selected code", firstPrompt.getEditorInfo().getSelectedText());
            assertEquals("/project/src/Main.java", firstPrompt.getEditorInfo().getSelectionFilePath());
            assertEquals(3, firstPrompt.getEditorInfo().getSelectionStartLine());
            assertEquals(5, firstPrompt.getEditorInfo().getSelectionEndLine());
            assertEquals(Collections.singletonList(virtualFile), firstPrompt.getEditorInfo().getSelectedFiles());
            assertNotNull(nextPrompt.getEditorInfo());
            assertNull(nextPrompt.getEditorInfo().getSelectedText());
            assertNull(nextPrompt.getEditorInfo().getSelectionFilePath());
            assertNull(nextPrompt.getEditorInfo().getSelectionStartLine());
            assertNull(nextPrompt.getEditorInfo().getSelectionEndLine());
            assertEquals(List.of(attachedFile), firstPrompt.getPendingAttachedFiles());
            assertNull(nextPrompt.getPendingAttachedFiles());
            verify(fileListManager, times(2)).clearNonImageFiles(project, TAB_ID);
            verify(fileListManager, never()).addFile(any(Project.class), anyString(), any(VirtualFile.class));
        }
    }

    @Test
    void fullProjectContextDoesNotRequireEditorFileButtonManager() {
        when(languageModel.getProvider()).thenReturn(ModelProvider.CLIRunners);
        when(stateService.getTimeout()).thenReturn(60);

        ChatContextParameters parameters = new ChatContextParameters(
                project,
                "Ask about the project",
                languageModel,
                null,
                "Enter",
                null,
                "full project context",
                true,
                TAB_ID);

        try (MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            ChatMessageContext context = ChatMessageContextUtil.createContext(parameters);

            assertEquals("full project context", context.getFilesContext());
            assertTrue(context.isProjectContextFullUsed());
            assertNull(context.getEditorInfo());
        }
    }
}
