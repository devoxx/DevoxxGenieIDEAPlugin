package com.devoxx.genie.ui.component;

import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieIcons.CloseSmalllIcon;

/**
 * Class uses to display a file entry in the list of files with label and remove button.
 */
@Getter
public class JFileEntryComponent extends JPanel {

    private final VirtualFile virtualFile;

    public JFileEntryComponent(Project project,
                               VirtualFile virtualFile,
                               FileRemoveListener fileRemoveListener) {
        this.virtualFile = virtualFile;

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JButton fileNameBtn = createButton(
            new JButton(virtualFile.getName(), FileTypeIconUtil.getFileTypeIcon(project, virtualFile))
        );
        fileNameBtn.addActionListener(e -> FileEditorManager.getInstance(project).openFile(virtualFile, true));
        add(fileNameBtn);

        JButton removeBtn = createButton(new JHoverButton(CloseSmalllIcon, true));
        removeBtn.addActionListener(e -> fileRemoveListener.onFileRemoved(virtualFile));
        add(removeBtn);

    }

    private @NotNull JButton createButton(JButton button) {
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(JBUI.emptyInsets());
        return button;
    }
}
