package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

import static com.devoxx.genie.util.ImageUtil.isImageFile;

public class ImagePreviewHandler implements DropTargetListener {

    private final CommandAutoCompleteTextField dropTextArea;
    private JBPopup currentPopup;
    private final Project project;
    private static final Color HOVER_BACKGROUND = new JBColor(new Color(0, 122, 255, 30), new Color(0, 122, 255, 30));
    private static final Color DEFAULT_BACKGROUND = new JBColor(JBColor.background(), JBColor.background());

    public ImagePreviewHandler(Project project, @NotNull CommandAutoCompleteTextField dropTextArea) {
        this.project = project;
        this.dropTextArea = dropTextArea;

        DropTarget dropTarget = new DropTarget(dropTextArea, DnDConstants.ACTION_COPY_OR_MOVE, this, true);

        // Add a border that highlights when dragging
        dropTextArea.setDropTarget(dropTarget);
        dropTextArea.setBorder(BorderFactory.createLineBorder(JBColor.gray));
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (canAcceptDrop(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            dropTextArea.setBackground(HOVER_BACKGROUND);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (canAcceptDrop(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        if (canAcceptDrop(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        hidePreview();
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            dropTextArea.setBackground(DEFAULT_BACKGROUND);
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = dtde.getTransferable();

            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        for (File file : files) {
                            VirtualFile fileByIoFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                            if (fileByIoFile != null) {
                                FileListManager.getInstance().addFile(project, fileByIoFile);
                            } else {
                                NotificationUtil.sendNotification(project, "File type not supported: " + file.getName());
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        dtde.dropComplete(true);
                    }
                }.execute();
            } else {
                dtde.dropComplete(false);
            }
        } catch (Exception e) {
            dtde.dropComplete(false);
        }
    }

    private boolean canAcceptDrop(@NotNull DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
               dtde.isDataFlavorSupported(DataFlavor.imageFlavor);
    }

    private void hidePreview() {
        if (currentPopup != null && !currentPopup.isDisposed()) {
            currentPopup.cancel();
            currentPopup = null;
        }
    }
}
