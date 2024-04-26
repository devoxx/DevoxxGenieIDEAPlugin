package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.listener.FileRemoveListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.listener.FileSelectionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * Here we have a panel that displays a list of files that are selected by the user.
 * These files are used as context for the prompt input.
 */
public class PromptContextFileListPanel extends JPanel implements FileRemoveListener, FileSelectionListener {

    @Getter
    private final List<VirtualFile> files = new ArrayList<>();

    private final JBScrollPane filesScrollPane;
    private final transient Project project;

    public PromptContextFileListPanel(Project project) {
        this.project = project;
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

    private void updateFilesPanelVisibility() {
        if (files.isEmpty()) {
            filesScrollPane.setVisible(false);
            filesScrollPane.setPreferredSize(new Dimension(0, 0));
        } else {
            filesScrollPane.setVisible(true);
            int heightPerFile = 30;
            int totalHeight = heightPerFile * files.size();
            int maxHeight = heightPerFile * 3;
            int prefHeight = Math.min(totalHeight, maxHeight);
            filesScrollPane.setPreferredSize(new Dimension(getPreferredSize().width, prefHeight));
        }
        filesScrollPane.revalidate();
        filesScrollPane.repaint();
    }

    @Override
    public void onFileRemoved(VirtualFile file) {
        this.files.remove(file);
        removeFromFilesPanel(file);
        updateFilesPanelVisibility();
        revalidate();
        repaint();
    }

    private void removeFromFilesPanel(VirtualFile file) {
        for (Component component : getComponents()) {
            if (component instanceof JFileEntryComponent fileEntryComponent &&
                fileEntryComponent.getVirtualFile().equals(file)) {
                remove(fileEntryComponent);
                break;
            }
        }
    }

    @Override
    public void fileSelected(VirtualFile selectedFile) {
        if (!files.contains(selectedFile)) {
            files.add(selectedFile);
            updateFilesPanelVisibility();
            JFileEntryComponent fileLabel = new JFileEntryComponent(project, selectedFile, this);
            add(fileLabel);
            revalidate();
            repaint();
        }
    }
}
