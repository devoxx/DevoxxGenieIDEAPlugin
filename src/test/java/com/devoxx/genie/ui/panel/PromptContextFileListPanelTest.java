package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.FileListObserver;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for PromptContextFileListPanel.
 * <p>
 * NOTE: We do NOT use @ExtendWith(MockitoExtension.class) because the
 * PromptContextFileListPanel constructor creates JBScrollPane which requires
 * IntelliJ platform's ModalityState to be fully initialized. We mock
 * ModalityState.defaultModalityState() to avoid the IllegalStateException.
 * All mocks are created manually in setUp().
 */
class PromptContextFileListPanelTest {

    private Project project;
    private Application application;
    private FileListManager fileListManager;

    private MockedStatic<ApplicationManager> appManagerMockedStatic;
    private MockedStatic<FileListManager> fileListManagerMockedStatic;
    private MockedStatic<ModalityState> modalityStateMockedStatic;

    private PromptContextFileListPanel panel;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        application = mock(Application.class);
        fileListManager = mock(FileListManager.class);

        when(project.getLocationHash()).thenReturn("test-hash");

        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(FileListManager.class)).thenReturn(fileListManager);

        fileListManagerMockedStatic = Mockito.mockStatic(FileListManager.class);
        fileListManagerMockedStatic.when(FileListManager::getInstance).thenReturn(fileListManager);

        // Mock ModalityState to avoid JBScrollPane initialization failure on macOS
        ModalityState modalityState = mock(ModalityState.class);
        modalityStateMockedStatic = Mockito.mockStatic(ModalityState.class);
        modalityStateMockedStatic.when(ModalityState::defaultModalityState).thenReturn(modalityState);
        modalityStateMockedStatic.when(ModalityState::nonModal).thenReturn(modalityState);

        // Initially empty
        lenient().when(fileListManager.isEmpty(project)).thenReturn(true);
        lenient().when(fileListManager.size(project)).thenReturn(0);

        panel = new PromptContextFileListPanel(project);
    }

    @AfterEach
    void tearDown() {
        modalityStateMockedStatic.close();
        fileListManagerMockedStatic.close();
        appManagerMockedStatic.close();
    }

    @Test
    void testConstructor_RegistersAsObserver() {
        verify(fileListManager).addObserver(eq(project), eq(panel));
    }

    @Test
    void testConstructor_SetsBoxLayout() {
        assertThat(panel.getLayout()).isNotNull();
        assertThat(panel.getLayout()).isInstanceOf(javax.swing.BoxLayout.class);
    }

    @Test
    void testConstructor_HasOneChild() {
        // The panel should contain exactly one child: the filesScrollPane
        assertThat(panel.getComponentCount()).isEqualTo(1);
    }

    @Test
    void testAllFilesRemoved_ClearsPanel() {
        when(fileListManager.isEmpty(project)).thenReturn(true);

        panel.allFilesRemoved();

        // After all files removed, the scroll pane should still be there but hidden
        assertThat(panel.getComponentCount()).isEqualTo(1);
    }

    @Test
    void testOnFileRemoved_DelegatesToFileListManager() {
        VirtualFile mockFile = mock(VirtualFile.class);
        when(mockFile.getName()).thenReturn("test.java");

        when(fileListManager.isEmpty(project)).thenReturn(true);

        panel.onFileRemoved(mockFile);

        verify(fileListManager).removeFile(project, mockFile);
    }

    @Test
    void testImplementsFileListObserver() {
        assertThat(panel).isInstanceOf(FileListObserver.class);
    }

    @Test
    void testImplementsFileRemoveListener() {
        assertThat(panel).isInstanceOf(com.devoxx.genie.ui.listener.FileRemoveListener.class);
    }

    @Test
    void testAllFilesRemoved_MultipleCallsDoNotThrow() {
        when(fileListManager.isEmpty(project)).thenReturn(true);

        // Calling allFilesRemoved multiple times should not crash
        panel.allFilesRemoved();
        panel.allFilesRemoved();
        panel.allFilesRemoved();

        assertThat(panel.getComponentCount()).isEqualTo(1);
    }

    @Test
    void testOnFileRemoved_MultipleFiles_EachDelegated() {
        VirtualFile file1 = mock(VirtualFile.class);
        VirtualFile file2 = mock(VirtualFile.class);
        when(file1.getName()).thenReturn("File1.java");
        when(file2.getName()).thenReturn("File2.java");

        when(fileListManager.isEmpty(project)).thenReturn(true);

        panel.onFileRemoved(file1);
        panel.onFileRemoved(file2);

        verify(fileListManager).removeFile(project, file1);
        verify(fileListManager).removeFile(project, file2);
    }
}
