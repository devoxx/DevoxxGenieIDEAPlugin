package com.devoxx.genie.ui.settings.rag.table;

import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.chromadb.ChromaDockerService;
import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ButtonEditor extends DefaultCellEditor {

    private static final String DELETE_LABEL = "Delete";

    private boolean isPushed;
    private final JButton button;
    private final JBTable collectionsTable;
    private final DefaultTableModel tableModel;
    private final transient Project project;
    private final transient ChromaDockerService dockerService;

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

                String collectionId = (String) tableModel.getValueAt(row, 0);
                if (collectionId != null && confirmDeletion(collectionId)) {
                    // Delete collection first
                    ChromaDBManager.getInstance(project).deleteCollection(collectionId);

                    // Delete the associated volume data
                    dockerService.deleteCollectionData(project, collectionId);

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

    private void addCollectionRowSafely(ChromaCollection collection) {
        try {
            int totalDocs = ChromaDBManager.getInstance(project).countDocuments(collection.id());
            tableModel.addRow(new Object[]{
                    collection.name(),
                    totalDocs,
                    DELETE_LABEL
            });
        } catch (IOException e) {
            // If we can't count documents for a specific collection, still add it with 0 count
            tableModel.addRow(new Object[]{
                    collection.name(),
                    0,
                    DELETE_LABEL
            });
        }
    }

    public void safeLoadCollections() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // Clear table first
                tableModel.setRowCount(0);

                // Check if RAG is enabled in settings
                if (Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getRagEnabled())) {
                    // Don't try to load collections if RAG is disabled
                    tableModel.fireTableDataChanged();
                    return;
                }

                // Load collections
                List<ChromaCollection> collections = ChromaDBManager.getInstance(project).listCollections();

                // Add rows safely
                for (ChromaCollection collection : collections) {
                    addCollectionRowSafely(collection);
                }

                // Notify table of data change
                tableModel.fireTableDataChanged();

                // Force the table to repaint
                if (collectionsTable != null) {
                    collectionsTable.revalidate();
                    collectionsTable.repaint();
                }
            } catch (IOException e) {
                // Don't show error during startup
                NotificationUtil.sendNotification(project,
                        "Failed to load collections: " + e.getMessage());
            }
        });
    }
}
