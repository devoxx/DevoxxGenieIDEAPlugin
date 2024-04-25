package com.devoxx.genie.ui.renderer;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;

public class FileListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof VirtualFile file) {
            FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
            Icon icon = fileType.getIcon();
            label.setIcon(icon);
            label.setText(file.getName());
        }

        return label;
    }
}
