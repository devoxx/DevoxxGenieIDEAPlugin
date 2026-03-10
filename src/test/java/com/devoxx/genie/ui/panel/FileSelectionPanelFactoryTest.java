package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for the key-navigation handler methods in FileSelectionPanelFactory.
 * <p>
 * The two package-private methods under test—handleFilterFieldKeyPressed and
 * handleResultListKeyPressed—were extracted from the anonymous KeyAdapter
 * implementations to reduce cognitive complexity (java:S3776).
 */
class FileSelectionPanelFactoryTest {

    private static final String TEST_TAB_ID = "test-tab-id";

    private Project project;
    private FileListManager fileListManager;
    private MockedStatic<FileListManager> fileListManagerMock;
    private MockedStatic<ConversationTabRegistry> tabRegistryMock;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        fileListManager = mock(FileListManager.class);
        fileListManagerMock = Mockito.mockStatic(FileListManager.class);
        fileListManagerMock.when(FileListManager::getInstance).thenReturn(fileListManager);

        ConversationTabRegistry registry = mock(ConversationTabRegistry.class);
        when(registry.getActiveTabId(any())).thenReturn(TEST_TAB_ID);
        tabRegistryMock = Mockito.mockStatic(ConversationTabRegistry.class);
        tabRegistryMock.when(ConversationTabRegistry::getInstance).thenReturn(registry);
    }

    @AfterEach
    void tearDown() {
        tabRegistryMock.close();
        fileListManagerMock.close();
    }

    // -----------------------------------------------------------------------
    // handleFilterFieldKeyPressed
    // -----------------------------------------------------------------------

    @Test
    void filterField_downKey_emptyList_doesNotConsume() {
        JBList<VirtualFile> resultList = new JBList<>(new DefaultListModel<>());
        KeyEvent event = keyEvent(KeyEvent.VK_DOWN);

        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isFalse();
    }

    @Test
    void filterField_downKey_noSelection_selectsFirstItemAndConsumes() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.clearSelection();

        KeyEvent event = keyEvent(KeyEvent.VK_DOWN);
        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isTrue();
        assertThat(resultList.getSelectedIndex()).isEqualTo(0);
    }

    @Test
    void filterField_downKey_existingSelection_doesNotChangeSelectionAndConsumes() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(1);

        KeyEvent event = keyEvent(KeyEvent.VK_DOWN);
        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isTrue();
        assertThat(resultList.getSelectedIndex()).isEqualTo(1);
    }

    @Test
    void filterField_enterKey_emptyList_doesNotConsume() {
        JBList<VirtualFile> resultList = new JBList<>(new DefaultListModel<>());
        KeyEvent event = keyEvent(KeyEvent.VK_ENTER);

        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isFalse();
        verifyNoInteractions(fileListManager);
    }

    @Test
    void filterField_enterKey_withResults_noSelection_selectsFirstAndAddsFile() {
        VirtualFile file = mock(VirtualFile.class);
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(file);
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.clearSelection();

        when(fileListManager.contains(project, TEST_TAB_ID, file)).thenReturn(false);

        KeyEvent event = keyEvent(KeyEvent.VK_ENTER);
        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isTrue();
        verify(fileListManager).addFile(project, TEST_TAB_ID, file);
    }

    @Test
    void filterField_enterKey_fileAlreadyAdded_doesNotAddAgain() {
        VirtualFile file = mock(VirtualFile.class);
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(file);
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(0);

        when(fileListManager.contains(project, TEST_TAB_ID, file)).thenReturn(true);

        KeyEvent event = keyEvent(KeyEvent.VK_ENTER);
        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isTrue();
        verify(fileListManager, never()).addFile(any(), any(), any());
    }

    @Test
    void filterField_otherKey_doesNothing() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);

        KeyEvent event = keyEvent(KeyEvent.VK_ESCAPE);
        FileSelectionPanelFactory.handleFilterFieldKeyPressed(event, resultList, project);

        assertThat(event.isConsumed()).isFalse();
        verifyNoInteractions(fileListManager);
    }

    // -----------------------------------------------------------------------
    // handleResultListKeyPressed
    // -----------------------------------------------------------------------

    @Test
    void resultList_upKey_atFirstItem_consumesEvent() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(0);

        KeyEvent event = keyEvent(KeyEvent.VK_UP);
        FileSelectionPanelFactory.handleResultListKeyPressed(event, new JBTextField(), resultList, project);

        assertThat(event.isConsumed()).isTrue();
    }

    @Test
    void resultList_upKey_notAtFirstItem_doesNotConsume() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(1);

        KeyEvent event = keyEvent(KeyEvent.VK_UP);
        FileSelectionPanelFactory.handleResultListKeyPressed(event, new JBTextField(), resultList, project);

        assertThat(event.isConsumed()).isFalse();
    }

    @Test
    void resultList_enterKey_addsSelectedFileAndConsumes() {
        VirtualFile file = mock(VirtualFile.class);
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(file);
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(0);

        when(fileListManager.contains(project, TEST_TAB_ID, file)).thenReturn(false);

        KeyEvent event = keyEvent(KeyEvent.VK_ENTER);
        FileSelectionPanelFactory.handleResultListKeyPressed(event, new JBTextField(), resultList, project);

        assertThat(event.isConsumed()).isTrue();
        verify(fileListManager).addFile(project, TEST_TAB_ID, file);
    }

    @Test
    void resultList_enterKey_fileAlreadyAdded_doesNotAddAgain() {
        VirtualFile file = mock(VirtualFile.class);
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(file);
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(0);

        when(fileListManager.contains(project, TEST_TAB_ID, file)).thenReturn(true);

        KeyEvent event = keyEvent(KeyEvent.VK_ENTER);
        FileSelectionPanelFactory.handleResultListKeyPressed(event, new JBTextField(), resultList, project);

        assertThat(event.isConsumed()).isTrue();
        verify(fileListManager, never()).addFile(any(), any(), any());
    }

    @Test
    void resultList_otherKey_doesNothing() {
        DefaultListModel<VirtualFile> model = new DefaultListModel<>();
        model.addElement(mock(VirtualFile.class));
        JBList<VirtualFile> resultList = new JBList<>(model);
        resultList.setSelectedIndex(0);

        KeyEvent event = keyEvent(KeyEvent.VK_ESCAPE);
        FileSelectionPanelFactory.handleResultListKeyPressed(event, new JBTextField(), resultList, project);

        assertThat(event.isConsumed()).isFalse();
        verifyNoInteractions(fileListManager);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static KeyEvent keyEvent(int keyCode) {
        return new KeyEvent(new JTextField(), KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }
}
