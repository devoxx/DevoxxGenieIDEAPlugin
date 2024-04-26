package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;

public class FileListCellRenderer extends DefaultListCellRenderer {

    private final Project project;

    public FileListCellRenderer(Project project) {
        this.project = project;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof VirtualFile file) {
            label.setIcon(FileTypeIconUtil.getFileTypeIcon(project, file));
            label.setText(file.getName());
        }

        return label;
    }
}
