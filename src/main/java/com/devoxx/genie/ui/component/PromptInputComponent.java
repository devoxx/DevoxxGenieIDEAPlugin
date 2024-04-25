package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.topic.FileSelectionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

public class PromptInputComponent extends JPanel implements FileSelectionListener, FileRemoveListener {

    private final PlaceholderTextArea promptInputArea;
    private final JPanel filesPanel = new JPanel();
    private final JBScrollPane filesScrollPane;
    private final Project project;

    @Getter
    @Setter
    private List<VirtualFile> files = new ArrayList<>();

    public PromptInputComponent(Project project) {
        super();
        this.project = project;
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));

        // The prompt input area
        this.promptInputArea = new PlaceholderTextArea();
        this.promptInputArea.setLineWrap(true);
        this.promptInputArea.setWrapStyleWord(true);
        this.promptInputArea.setRows(3);
        this.promptInputArea.setAutoscrolls(false);
        this.add(promptInputArea, BorderLayout.CENTER);

        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
        this.promptInputArea.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        // The files panel where the selected files are displayed
        filesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));

        // Wrap the filesPanel in a JScrollPane
        filesScrollPane = new JBScrollPane(filesPanel);
        filesScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        filesScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        filesScrollPane.setBorder(null);
        filesScrollPane.setMinimumSize(new Dimension(0, 60));
        filesScrollPane.setPreferredSize(new Dimension(0, 60)); // Initially set to zero size
        filesScrollPane.setVisible(false); // Initially invisible

        // Instead of adding filesPanel directly, add the JScrollPane
        this.add(filesScrollPane, BorderLayout.NORTH);

        // Subscribe to file selection events and handle them in this class
        project.getMessageBus().connect().subscribe(AppTopics.FILE_SELECTION_TOPIC, this);
        JBSplitter splitter = new JBSplitter(true, 0.5f);
        promptInputArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                promptInputArea.setBorder(BorderFactory.createLineBorder(
                    new JBColor(new Color(37, 150, 190), new Color(37, 150, 190)))
                );
                promptInputArea.requestFocusInWindow();
            }

            @Override
            public void focusLost(FocusEvent e) {
                promptInputArea.setBorder(null);
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        int height = super.getPreferredSize().height;  // Default height
        int filesHeight = files.size() * 30;  // Calculate additional height based on files
        return new Dimension(super.getPreferredSize().width, height + filesHeight);
    }

    @Override
    public void fileSelected(VirtualFile selectedFile) {
        synchronized (project) {
            if (!files.contains(selectedFile)) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    files.add(selectedFile);
                    updateFilesPanelVisibility();
                    JFileEntryComponent fileLabel = new JFileEntryComponent(project, selectedFile, this);
                    filesPanel.add(fileLabel);
                    filesPanel.revalidate();
                    filesPanel.repaint();
                });
            }
        }
    }

    private void updateFilesPanelVisibility() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (files.isEmpty()) {
                filesScrollPane.setVisible(false);
                filesScrollPane.setPreferredSize(new Dimension(0, 0));
            } else {
                filesScrollPane.setVisible(true);
                int heightPerFile = 30;
                int totalHeight = heightPerFile * files.size();
                int maxHeight = heightPerFile * 3;
                int prefHeight = Math.min(totalHeight, maxHeight);
                filesScrollPane.setPreferredSize(new Dimension(filesPanel.getPreferredSize().width, prefHeight));
            }
            filesScrollPane.revalidate();
            filesScrollPane.repaint();
        });
    }

    public String getText() {
        return this.promptInputArea.getText();
    }

    public void setText(String text) {
        this.promptInputArea.setText(text);
    }

    @Override
    public void onFileRemoved(VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.files.remove(file);
            updateFilesPanelVisibility();
            filesPanel.repaint();
        });
    }
}
