package com.devoxx.genie.ui.component;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.FileListObserver;
import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.devoxx.genie.ui.listener.FileSelectionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * Here we have a panel that displays a list of files that are selected by the user.
 * These files are used as context for the prompt input.
 */
public class PromptContextFileListPanel extends JPanel
    implements FileRemoveListener, FileSelectionListener, FileListObserver {

    private final FileListManager fileListManager;
    private final JBScrollPane filesScrollPane;
    private final transient Project project;

    public PromptContextFileListPanel(Project project) {
        this.project = project;
        fileListManager = FileListManager.getInstance();
        fileListManager.addObserver(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Subscribe to file selection events and handle them in this class
        project.getMessageBus().connect().subscribe(AppTopics.FILE_SELECTION_TOPIC, this);

        // Wrap the filesPanel in a JBScrollPane
        filesScrollPane = new JBScrollPane(this);
        filesScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        filesScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        filesScrollPane.setBorder(null);
        filesScrollPane.setMinimumSize(new Dimension(0, 60));
        filesScrollPane.setPreferredSize(new Dimension(0, 60));
        filesScrollPane.setVisible(false);
    }

    @Override
    public void fileAdded(VirtualFile file) {
        updateFilesPanelVisibility();
        FileEntryComponent fileLabel = new FileEntryComponent(project, file, this, null);
        add(fileLabel);
        updateUIState();
    }

    @Override
    public void allFilesRemoved() {
        removeAll();
        updateFilesPanelVisibility();
        updateUIState();
    }

    private void updateFilesPanelVisibility() {
        if (fileListManager.isEmpty()) {
            filesScrollPane.setVisible(false);
            filesScrollPane.setPreferredSize(new Dimension(0, 0));
        } else {
            filesScrollPane.setVisible(true);
            int heightPerFile = 30;
            int totalHeight = heightPerFile * fileListManager.size();
            int maxHeight = heightPerFile * 3;
            int prefHeight = Math.min(totalHeight, maxHeight);
            filesScrollPane.setPreferredSize(new Dimension(getPreferredSize().width, prefHeight));
        }
        filesScrollPane.revalidate();
        filesScrollPane.repaint();
    }

    @Override
    public void onFileRemoved(VirtualFile file) {
        fileListManager.removeFile(file);
        removeFromFilesPanel(file);
        updateFilesPanelVisibility();
        updateUIState();
    }

    private void removeFromFilesPanel(VirtualFile file) {
        for (Component component : getComponents()) {
            if (component instanceof FileEntryComponent fileEntryComponent &&
                fileEntryComponent.getVirtualFile().equals(file)) {
                remove(fileEntryComponent);
                break;
            }
        }
    }

    @Override
    public void fileSelected(VirtualFile selectedFile) {
        if (!fileListManager.contains(selectedFile)) {
            fileListManager.addFile(selectedFile);
            updateFilesPanelVisibility();
            FileEntryComponent fileLabel = new FileEntryComponent(project, selectedFile, this, null);
            add(fileLabel);
            updateUIState();
        }
    }

    private void updateUIState() {
        revalidate();
        repaint();
    }
}
