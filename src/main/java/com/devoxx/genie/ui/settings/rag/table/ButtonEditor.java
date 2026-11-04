package com.devoxx.genie.ui.settings.rag.table;

import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.chromadb.ChromaDockerService;
import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ButtonEditor extends DefaultCellEditor {

    private static final String DELETE_LABEL = "Delete";

    private boolean isPushed;
    private final JButton button;
    private final JBTable collectionsTable;
    private final DefaultTableModel tableModel;
    private final transient Project project;
    private final transient ChromaDockerService dockerService;
    /** Loads overlap when validation refreshes quickly; only the newest may update the table. */
    private final AtomicInteger latestLoad = new AtomicInteger();

    public ButtonEditor(Project project,
                             JBTable collectionsTable,
                             DefaultTableModel tableModel) {
        super(new JCheckBox());

        this.project = project;
        this.collectionsTable = collectionsTable;
        this.tableModel = tableModel;

        this.dockerService = ApplicationManager.getApplication().getService(ChromaDockerService.class);

        button = new JButton();
        button.addActionListener(e -> {
            isPushed = true;
            fireEditingStopped();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected, int row, int column) {
        if (isSelected) {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
        } else {
            button.setForeground(table.getForeground());
            button.setBackground(table.getBackground());
        }
        button.setText(DELETE_LABEL);
        isPushed = false;
        return button;
    }

    private boolean confirmDeletion(String collectionId) {
        return Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete collection '" + collectionId + "' and all associated data?",
                "Confirm Collection Deletion",
                Messages.getQuestionIcon()
        ) == Messages.YES;
    }

    @Override
    public Object getCellEditorValue() {
        if (isPushed) {
            try {
                // Get row and collection info before deleting
                int row = collectionsTable.getSelectedRow();
                if (row < 0) {
                    return DELETE_LABEL;
                }

                String collectionName = (String) tableModel.getValueAt(row, 0);
                if (collectionName != null && confirmDeletion(collectionName)) {
                    // Delete collection first
                    ChromaDBManager.getInstance(project).deleteCollection(collectionName);

                    // Delete the associated volume data
                    dockerService.deleteCollectionData(project, collectionName);

                    // Then reload table data
                    safeLoadCollections();
                }
            } catch (IOException e) {
                NotificationUtil.sendNotification(project, "Failed to delete collection: " + e.getMessage());
            }
        }
        isPushed = false;
        return DELETE_LABEL;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        return super.stopCellEditing();
    }

    /** Calls ChromaDB, so this runs off the EDT. */
    private @NotNull List<Object[]> loadCollectionRows() {
        if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
            return List.of();
        }
        ChromaDBManager chroma = ChromaDBManager.getInstance(project);
        List<Object[]> rows = new ArrayList<>();
        try {
            for (ChromaCollection collection : chroma.listCollections()) {
                rows.add(new Object[]{collection.name(), countDocuments(chroma, collection), DELETE_LABEL});
            }
        } catch (IOException e) {
            // ChromaDB unreachable is the common case here (container not running, wrong
            // port, etc.) — the validator panel ("ChromaDB is not running" + Start button)
            // already tells the user this. Surfacing it again as a balloon would just be
            // noise, and {@code safeLoadCollections} is called on settings-panel open AND
            // after each indexing run, so the duplicate was firing every time.
            log.debug("Failed to load Chroma collections (panel will show validator error): {}", e.getMessage());
            return List.of();
        }
        return rows;
    }

    /** If a collection's documents can't be counted, it's still listed, with a count of 0. */
    private static int countDocuments(@NotNull ChromaDBManager chroma, @NotNull ChromaCollection collection) {
        try {
            return chroma.countDocuments(collection.id());
        } catch (IOException e) {
            return 0;
        }
    }

    private void showCollectionRows(@NotNull List<Object[]> rows) {
        tableModel.setRowCount(0);
        rows.forEach(tableModel::addRow);
        tableModel.fireTableDataChanged();
        if (collectionsTable != null) {
            collectionsTable.revalidate();
            collectionsTable.repaint();
        }
    }

    public void safeLoadCollections() {
        int load = latestLoad.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<Object[]> rows = loadCollectionRows();
            // ModalityState.any(): the Settings dialog is modal, and the table must update while it's open.
            ApplicationManager.getApplication().invokeLater(() -> {
                if (load == latestLoad.get()) {
                    showCollectionRows(rows);
                }
            }, ModalityState.any());
        });
    }
}
