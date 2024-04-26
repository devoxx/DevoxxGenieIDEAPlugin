package com.devoxx.genie.ui;

import com.devoxx.genie.ui.component.PlaceholderTextArea;
import com.devoxx.genie.ui.renderer.FileListCellRenderer;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.PartialNameFileFinderUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

/**
 * The factory class for creating a panel with a text field for filtering files and a list of open files
 */
public class FileSelectionPanelFactory implements DumbAware {

    private static final int DOUBLE_CLICK = 2;

    private FileSelectionPanelFactory() {
    }

    /**
     * Creates a panel with a text field for filtering files and a list of files
     * @param project The current project
     * @return The panel
     */
    public static JPanel createPanel(Project project) {
        JBList<VirtualFile> resultList = createResultList(project);
        JPanel mainPanel = new JPanel(new BorderLayout());
        PlaceholderTextArea filterInputField = new PlaceholderTextArea();
        filterInputField.setPlaceholder("Filter files by name");
        filterInputField.setPreferredSize(new Dimension(0, 30));
        filterInputField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                    String filter = filterInputField.getText().trim().toLowerCase();
                    if (filter.isEmpty() || filter.length() < 3) {
                        return;
                    }
                    PartialNameFileFinderUtil partialNameFileFinderUtil = new PartialNameFileFinderUtil();
                    Collection<VirtualFile> filteredFiles = partialNameFileFinderUtil.findAndProcessFilesByPartName(project, filter);
                    DefaultListModel<VirtualFile> listModel = (DefaultListModel<VirtualFile>) resultList.getModel();
                    listModel.clear();
                    for (VirtualFile file : filteredFiles) {
                        listModel.addElement(file);
                    }
                });
            }
        });

        mainPanel.add(filterInputField, BorderLayout.NORTH);
        mainPanel.add(new JBScrollPane(resultList), BorderLayout.CENTER);
        return mainPanel;
    }

    /**
     * Creates a list of open files and files from the FilenameIndex search
     * @param project The current project
     * @return The list of files
     */
    private static JBList<VirtualFile> createResultList(Project project) {
        DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
        JBList<VirtualFile> resultList = new JBList<>(listModel);
        resultList.setCellRenderer(new FileListCellRenderer(project));

        addMouseListenerToResultList(project, resultList);
        populateListModelWithOpenFiles(project, listModel);

        return resultList;
    }

    /**
     * Adds a mouse listener to the result list to open the selected file
     * @param project The current project
     * @param resultList The list of files
     */
    private static void addMouseListenerToResultList(Project project,
                                                     JBList<VirtualFile> resultList) {
        resultList.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == DOUBLE_CLICK) {
                openSelectedFile(project, resultList);
            }
            }
        });
    }

    /**
     * Opens the selected file in the editor
     * @param project The current project
     * @param resultList The list of files
     */
    private static void openSelectedFile(Project project, JBList<VirtualFile> resultList) {
        VirtualFile selectedFile = resultList.getSelectedValue();
        MessageBus messageBus = project.getMessageBus();
        messageBus.syncPublisher(AppTopics.FILE_SELECTION_TOPIC).fileSelected(selectedFile);
    }

    /**
     * Populates the list model with the open files
     * @param project The current project
     * @param listModel The list model
     */
    private static void populateListModelWithOpenFiles(Project project,
                                                       DefaultListModel<VirtualFile> listModel) {
        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
            listModel.addElement(file);
        }
    }
}

