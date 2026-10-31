package com.devoxx.genie.util;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.button.EditorFileButtonManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.EditorUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devoxx.genie.action.AddSnippetAction.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Issue #1320: the editor selection must reach the LLM on the turn it is submitted with,
 * and must not wipe files the user attached explicitly.
 */
class ChatMessageContextUtilTest {

    private static final String TAB_ID = "tab-1";

    private MockedStatic<FileListManager> fileListManagerStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceStatic;
    private MockedStatic<EditorUtil> editorUtilStatic;

    private Project project;
    private FileListManager fileListManager;
    private DevoxxGenieStateService stateService;
    private final List<VirtualFile> attachedFiles = new ArrayList<>();

    @BeforeEach
    void setUp() {
        project = mock(Project.class);

        // FileListManager backed by a real list so ordering between "attach" and "collect" is observable
        fileListManager = mock(FileListManager.class);
        when(fileListManager.getNonImageFiles(any(Project.class), any())).thenAnswer(inv -> List.copyOf(attachedFiles));
        when(fileListManager.getImageFiles(any(Project.class), any())).thenReturn(List.of());
        when(fileListManager.contains(any(Project.class), any(), any())).thenAnswer(inv -> attachedFiles.contains(inv.getArgument(2)));
        doAnswer(inv -> attachedFiles.add(inv.getArgument(2))).when(fileListManager).addFile(any(Project.class), any(), any());
        doAnswer(inv -> attachedFiles.remove(inv.getArgument(2))).when(fileListManager).removeFileAndRefresh(any(Project.class), any(), any());
        doAnswer(inv -> { attachedFiles.clear(); return null; }).when(fileListManager).clearNonImageFiles(any(Project.class), any());
        fileListManagerStatic = mockStatic(FileListManager.class);
        fileListManagerStatic.when(FileListManager::getInstance).thenReturn(fileListManager);

        stateService = mock(DevoxxGenieStateService.class);
        when(stateService.getUseFileInEditor()).thenReturn(false);
        stateServiceStatic = mockStatic(DevoxxGenieStateService.class);
        stateServiceStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        editorUtilStatic = mockStatic(EditorUtil.class);
        editorUtilStatic.when(() -> EditorUtil.getEditorInfo(any())).thenReturn(new EditorInfo());
    }

    @AfterEach
    void tearDown() {
        editorUtilStatic.close();
        stateServiceStatic.close();
        fileListManagerStatic.close();
    }

    @Test
    void editorSelectionIsIncludedOnTheSameTurn() {
        VirtualFile editorFile = fileWithUserData("AbstractChatFontSizeAction.java");
        ChatMessageContext context = newContext();

        ChatMessageContextUtil.setWindowContext(context, buttonManagerFor(editorWithSelection(editorFile)), null, false);

        assertNotNull(context.getPendingAttachedFiles(), "selection must be part of this turn's context, not the next one");
        assertTrue(context.getPendingAttachedFiles().contains(editorFile));
        assertEquals("return ActionUpdateThread.EDT;", editorFile.getUserData(SELECTED_TEXT_KEY));
        assertEquals(32, editorFile.getUserData(SELECTION_START_LINE_KEY));
        assertEquals(35, editorFile.getUserData(SELECTION_END_LINE_KEY));
    }

    @Test
    void editorSelectionDoesNotWipeUserAttachedFiles() {
        VirtualFile userFile = fileWithUserData("UserPicked.java");
        attachedFiles.add(userFile);
        VirtualFile editorFile = fileWithUserData("AbstractChatFontSizeAction.java");
        ChatMessageContext context = newContext();

        ChatMessageContextUtil.setWindowContext(context, buttonManagerFor(editorWithSelection(editorFile)), null, false);

        assertEquals(List.of(userFile, editorFile), context.getPendingAttachedFiles());
        assertEquals(List.of(userFile, editorFile), attachedFiles, "user-attached files must survive the prompt");
    }

    @Test
    void newSelectionReplacesPreviouslyAutoAttachedEditorFile() {
        VirtualFile previous = fileWithUserData("Previous.java");
        previous.putUserData(AUTO_ATTACHED_KEY, true);
        attachedFiles.add(previous);
        VirtualFile editorFile = fileWithUserData("Current.java");
        ChatMessageContext context = newContext();

        ChatMessageContextUtil.setWindowContext(context, buttonManagerFor(editorWithSelection(editorFile)), null, false);

        assertEquals(List.of(editorFile), context.getPendingAttachedFiles());
    }

    @Test
    void noSelectionAndUseFileInEditorOffAttachesNothing() {
        VirtualFile editorFile = fileWithUserData("Foo.java");
        Editor editor = editorWithSelection(editorFile);
        when(editor.getSelectionModel().getSelectedText()).thenReturn(null);
        ChatMessageContext context = newContext();

        ChatMessageContextUtil.setWindowContext(context, buttonManagerFor(editor), null, false);

        assertNull(context.getPendingAttachedFiles());
        assertTrue(attachedFiles.isEmpty());
    }

    private ChatMessageContext newContext() {
        return ChatMessageContext.builder().project(project).tabId(TAB_ID).build();
    }

    private static EditorFileButtonManager buttonManagerFor(Editor editor) {
        EditorFileButtonManager manager = mock(EditorFileButtonManager.class);
        when(manager.getSelectedTextEditor()).thenReturn(editor);
        return manager;
    }

    /** Editor whose selection covers 0-based lines 32..35. */
    private static Editor editorWithSelection(VirtualFile file) {
        Editor editor = mock(Editor.class);
        SelectionModel selectionModel = mock(SelectionModel.class);
        Document document = mock(Document.class);
        when(editor.getVirtualFile()).thenReturn(file);
        when(editor.getDocument()).thenReturn(document);
        when(editor.getSelectionModel()).thenReturn(selectionModel);
        when(selectionModel.getSelectedText()).thenReturn("return ActionUpdateThread.EDT;");
        when(selectionModel.getSelectionStart()).thenReturn(1000);
        when(selectionModel.getSelectionEnd()).thenReturn(1100);
        when(document.getLineNumber(1000)).thenReturn(32);
        when(document.getLineNumber(1100)).thenReturn(35);
        return editor;
    }

    /** VirtualFile mock with working user data, since the selection metadata lives there. */
    @SuppressWarnings("unchecked")
    private static VirtualFile fileWithUserData(String name) {
        VirtualFile file = mock(VirtualFile.class);
        Map<Key<?>, Object> userData = new HashMap<>();
        when(file.getName()).thenReturn(name);
        doAnswer(inv -> {
            Object value = inv.getArgument(1);
            if (value == null) userData.remove(inv.<Key<?>>getArgument(0)); else userData.put(inv.getArgument(0), value);
            return null;
        }).when(file).putUserData(any(Key.class), any());
        when(file.getUserData(any(Key.class))).thenAnswer(inv -> userData.get(inv.<Key<?>>getArgument(0)));
        return file;
    }
}
