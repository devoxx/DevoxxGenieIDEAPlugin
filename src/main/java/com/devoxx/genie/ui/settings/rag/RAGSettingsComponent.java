package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.RagValidatorService;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidationResult;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.rag.table.ButtonEditor;
import com.devoxx.genie.ui.settings.rag.table.ButtonRenderer;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Objects;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

public class RAGSettingsComponent extends AbstractSettingsComponent {

    private static final int INSET_VALUE = 5;
    private static final String RAG_SETTINGS_SECTION_TITLE = "Retrieval-augmented Generation (RAG) settings";

    private JProgressBar progressBar;
    private JLabel progressLabel;

    @Getter
    private final JBCheckBox enableIndexerCheckBox = new JBCheckBox("Enable feature", stateService.getRagEnabled());

    @Getter
    private final JBIntSpinner portIndexer = new JBIntSpinner(new UINumericRange(stateService.getIndexerPort(), 8000, 9000));

    @Getter
    private final JBIntSpinner maxResultsSpinner = new JBIntSpinner(new UINumericRange(stateService.getIndexerMaxResults(), 1, 50));

    @Getter
    private final JSpinner minScoreField = new JSpinner(new SpinnerNumberModel(stateService.getIndexerMinScore().doubleValue(), 0.0d, 1.0d, 0.01d));

    private final Project project;
    private JButton startIndexButton;
    private final JButton actionButton = new JButton();
    private JBTable collectionsTable;
    private DefaultTableModel tableModel;
    private final JPanel validationPanel;
    private final RAGSettingsHandler validationHandler;
    private ButtonEditor buttonEditor;

    public RAGSettingsComponent(Project project) {
        this.project = project;
        this.validationPanel = new JPanel();
        this.validationHandler = new RAGSettingsHandler(project, validationPanel, this);

        initializeComponents();
        addListeners();
        setupTable();
        loadCollections();
        setupProgressBar();
    }

    private void initializeComponents() {
        startIndexButton = new JButton("Start Indexing");
        startIndexButton.setVisible(false);
        String[] columnNames = {"Collection", "Indexed Segments", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0);
        collectionsTable = new JBTable(tableModel);
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/rag";
    }

    @Override
    public void addListeners() {
        startIndexButton.addActionListener(e -> startIndexing());
        enableIndexerCheckBox.addActionListener(e -> {
            updateComponentsEnabled();
            if (enableIndexerCheckBox.isSelected()) {
                validationHandler.performValidation();
            } else {
                startIndexButton.setVisible(false);
            }
        });
    }

    private void addProgressSection(@NotNull JPanel panel, @NotNull GridBagConstraints gbc) {
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        // Reset grid width to span across all columns
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        // Allow the panel to expand horizontally
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(progressPanel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
    }

    private void loadCollections() {
        buttonEditor.safeLoadCollections();
        collectionsTable.setVisible(true); // Show the table after loading data
    }

    private void setupProgressBar() {
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressLabel = new JLabel("Ready to index");
        progressLabel.setVisible(false);
        
        // Set preferred size to ensure the progress bar is visible when shown
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressLabel.setPreferredSize(new Dimension(300, 20));
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = createDefaultGbc();

        addEnablePanel(panel, gbc);
        addInfoLabel(panel, gbc);
        addRAGSettingsSection(panel, gbc);
        addIndexedProjectsSection(panel, gbc);
        addStartIndexButton(panel, gbc);
        addProgressSection(panel, gbc);
        addValidationSection(panel, gbc);

        return panel;
    }

    private @NotNull GridBagConstraints createDefaultGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(INSET_VALUE);
        return gbc;
    }

    private void addEnablePanel(@NotNull JPanel panel, GridBagConstraints gbc) {
        JPanel enablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enablePanel.add(enableIndexerCheckBox);
        panel.add(enablePanel, gbc);
        enableIndexerCheckBox.addActionListener(e -> updateComponentsEnabled());
        gbc.gridy++;
    }

    private void addInfoLabel(@NotNull JPanel panel, GridBagConstraints gbc) {
        JBLabel infoLabel = new JBLabel("<html><body style='width: 100%;'>" +
                "Retrieval-augmented Generation (RAG) leverages semantic search to find relevant code<BR>" +
                "based on your queries.<BR>" +
                "The indexer uses the \"Scan & Copy Project\" settings to exclude specific directories,<BR>" +
                "files, and extensions, and stores the indexed files in a local ChromaDB vector database." +
                "</body></html>");
        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);
        gbc.gridy++;
    }

    private void addRAGSettingsSection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, RAG_SETTINGS_SECTION_TITLE);
        addSettingRow(panel, gbc, "Chroma DB port", portIndexer);
        addSettingRow(panel, gbc, "Minimum score", minScoreField);
        addSettingRow(panel, gbc, "Set the minimum score threshold for semantic search results. A lower value will include more results.");
        addSettingRow(panel, gbc, "Maximum results", maxResultsSpinner);
        addSettingRow(panel, gbc, "How many results do you want to include in prompt window context?");
    }

    private void addIndexedProjectsSection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, "Indexed projects");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(collectionsTable), gbc);
        gbc.gridy++;
    }

    private void addValidationSection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, "Required RAG Services");
        validationHandler.performValidation();
        updateActionButtonState();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Set a fixed height for the validationPanel
        int fixedHeight = 100; // Adjust this value as needed
        validationPanel.setPreferredSize(new Dimension(validationPanel.getPreferredSize().width, fixedHeight));

        panel.add(validationPanel, gbc);
        gbc.gridy++;
    }

    private void addStartIndexButton(@NotNull JPanel panel, @NotNull GridBagConstraints gbc) {
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_START;
        startIndexButton.setVisible(false);
        panel.add(startIndexButton, gbc);
        gbc.gridy++;
    }

    // Flag to track if indexing is currently running
    private boolean isIndexing = false;
    
    private void startIndexing() {
        if (!enableIndexerCheckBox.isSelected()) {
            return;
        }
        
        // If indexing is already running, stop it
        if (isIndexing) {
            // Request cancellation
            ProjectIndexerService.getInstance().cancelIndexing();
            
            // Update UI to show we're stopping
            SwingUtilities.invokeLater(() -> {
                progressLabel.setText("Stopping indexing process...");
            });
            
            return;
        }
        
        // Set indexing flag
        isIndexing = true;
        
        // Make sure progress components are visible before starting the indexing process
        setStartButtons(false);
        
        // Run the indexing process in a background thread to avoid blocking the UI
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // Run the indexing process
                ProjectIndexerService.getInstance().indexFiles(project, true, progressBar, progressLabel);
                
                // Update UI after indexing is complete
                SwingUtilities.invokeLater(() -> {
                    // Reset indexing flag
                    isIndexing = false;
                    
                    // First reset the buttons
                    setStartButtons(true);
                    
                    // Clear the table before reloading
                    tableModel.setRowCount(0);
                    tableModel.fireTableDataChanged();
                    
                    // Add a small delay to ensure ChromaDB has time to update its state
                    Timer timer = new Timer(500, e -> {
                        // Reload collections
                        loadCollections();
                        
                        // Force table repaint
                        collectionsTable.revalidate();
                        collectionsTable.repaint();
                        
                        // Show a notification that indexing is complete
                        if (ProjectIndexerService.getInstance().isIndexingCancelled()) {
                            NotificationUtil.sendNotification(project, "Project indexing was cancelled");
                        } else {
                            NotificationUtil.sendNotification(project, "Project indexing completed successfully");
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    // Reset indexing flag
                    isIndexing = false;
                    
                    NotificationUtil.sendNotification(project, "Error indexing project: " + e.getMessage());
                    setStartButtons(true);
                });
            }
        });
    }

    private void setStartButtons(boolean state) {
        if (SwingUtilities.isEventDispatchThread()) {
            // If we're already on the EDT, update directly
            updateButtonsState(state);
        } else {
            // Otherwise, use invokeLater
            SwingUtilities.invokeLater(() -> updateButtonsState(state));
        }
    }
    
    private void updateButtonsState(boolean state) {
        // Update button text based on indexing state
        if (state) {
            // If state is true, we're either not indexing or ready to start
            startIndexButton.setText("Start Indexing");
            isIndexing = false;
        } else {
            // If state is false, we're currently indexing
            startIndexButton.setText("Stop Indexing");
            isIndexing = true;
        }
        
        progressBar.setVisible(!state);
        progressLabel.setVisible(!state);
    }

    private void updateActionButtonState() {
        getApplication().invokeLater(() -> {
            ValidationResult result = RagValidatorService.getInstance().validate();
            ActionButtonState actionState = determineActionState(result);
            actionButton.setVisible(actionState != null);
            if (actionState != null) {
                actionButton.setText(actionState.getText());
                actionButton.addActionListener(e -> {
                    validationHandler.handleValidationAction(
                            Objects.requireNonNull(result.statuses()
                                    .stream()
                                    .filter(status -> !status.isValid() &&
                                            status.action() != ValidationActionType.OK)
                                    .findFirst()
                                    .orElse(null)));
                    validationHandler.performValidation();
                });
            }
            actionButton.setEnabled(enableIndexerCheckBox.isSelected());
        });
    }

    private @Nullable ActionButtonState determineActionState(@NotNull ValidationResult result) {
        for (ValidatorStatus status : result.statuses()) {
            if (!status.isValid()) {
                switch (status.action()) {
                    case PULL_CHROMA_DB:
                        return ActionButtonState.PULL_CHROMA_DB;
                    case START_CHROMA_DB:
                        return ActionButtonState.START_CHROMA_DB;
                    case OK:
                        break;
                    default:
                        return ActionButtonState.START_INDEXING;
                }
            }
        }
        return null;
    }

    public void updateValidationStatus() {
        updateStartIndexButtonVisibility();
        updateActionButtonState();
        buttonEditor.safeLoadCollections();
    }

    private void updateStartIndexButtonVisibility() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ValidationResult result = RagValidatorService.getInstance().validate();
            boolean shouldShowButton = enableIndexerCheckBox.isSelected() && result.isValid();
            startIndexButton.setVisible(shouldShowButton);
            panel.revalidate();
            panel.repaint();
        });
    }

    /**
     * Disable/enable all relevant components based on the checkbox state
     */
    private void updateComponentsEnabled() {
        boolean enabled = enableIndexerCheckBox.isSelected();
        portIndexer.setEnabled(enabled);
        maxResultsSpinner.setEnabled(enabled);
        minScoreField.setEnabled(enabled);
        actionButton.setEnabled(enabled);
        collectionsTable.setEnabled(enabled);
    }

    private void setupTable() {
        TableColumn actionColumn = collectionsTable.getColumnModel().getColumn(2);
        actionColumn.setCellRenderer(new ButtonRenderer());
        buttonEditor = new ButtonEditor(project, collectionsTable, tableModel);
        actionColumn.setCellEditor(buttonEditor);
        collectionsTable.setVisible(false);
    }
}
