package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.devoxx.genie.util.FileUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.Gray;

import javax.swing.*;
import java.awt.*;

public class FileListCellRenderer extends DefaultListCellRenderer {
    private final Project project;
    private static final Color PATH_COLOR = Gray._128;
    private static final Font MONO_FONT = new Font("JetBrains Mono", Font.PLAIN, 12);

    public FileListCellRenderer(Project project) {
        this.project = project;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof VirtualFile file) {
            Box panel = Box.createHorizontalBox();
            panel.setOpaque(true);

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
            } else {
                panel.setBackground(list.getBackground());
            }

            // File name with icon
            JLabel fileNameLabel = new JLabel(file.getName(), FileTypeIconUtil.getFileTypeIcon(file), SwingConstants.LEFT);
            fileNameLabel.setFont(MONO_FONT);
            fileNameLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

            // Path label
            JLabel pathLabel = new JLabel();
            String fullPath = FileUtil.getRelativePath(project, file);
            if (!fullPath.equals(file.getName())) {
                String path = fullPath.substring(0, fullPath.lastIndexOf(file.getName()));
                pathLabel.setText(path);
                pathLabel.setFont(MONO_FONT);
                pathLabel.setForeground(isSelected ? list.getSelectionForeground() : PATH_COLOR);
            }

            // Create tooltip with full path
            String tooltipText = String.format("<html><body style='width: 300px'><pre>%s</pre></body></html>",
                    file.getPath().replace("<", "&lt;").replace(">", "&gt;"));

            // Set tooltip on both labels
            fileNameLabel.setToolTipText(tooltipText);
            pathLabel.setToolTipText(tooltipText);

            panel.add(fileNameLabel);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(pathLabel);
            panel.add(Box.createHorizontalGlue());

            panel.setMinimumSize(new Dimension(0, 20));
            panel.setPreferredSize(new Dimension(list.getWidth(), 20));

            // Make the panel itself show the tooltip
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setOpaque(true);
            wrapperPanel.setBackground(panel.getBackground());
            wrapperPanel.add(panel, BorderLayout.CENTER);
            wrapperPanel.setToolTipText(tooltipText);

            return wrapperPanel;
        }

        return label;
    }
}
