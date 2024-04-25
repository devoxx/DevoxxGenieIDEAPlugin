package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.util.DevoxxGenieIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.closeSmalllIcon;

public class JFileEntryComponent extends JPanel {

    public JFileEntryComponent(Project project,
                               VirtualFile virtualFile,
                               FileRemoveListener fileRemoveListener) {

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JButton fileNameBtn = createButton(
            new JButton(virtualFile.getName(), FileTypeManager.getInstance().getFileTypeByFile(virtualFile).getIcon())
        );
        fileNameBtn.addActionListener(e -> FileEditorManager.getInstance(project).openFile(virtualFile, true));

        JButton removeBtn = createButton(new JHoverButton(closeSmalllIcon, true));
        removeBtn.addActionListener(e -> fileRemoveListener.onFileRemoved(virtualFile));
    }

    private @NotNull JButton createButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(JBUI.emptyInsets());
        add(button);
        return button;
    }
}
