package com.devoxx.genie.ui.settings.semanticsearch;

import com.devoxx.genie.service.chromadb.ChromaDBStatusCallback;
import com.devoxx.genie.service.chromadb.ChromaDockerService;
import com.devoxx.genie.service.semanticsearch.ProjectIndexerService;
import com.devoxx.genie.service.semanticsearch.validator.*;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.semanticsearch.table.ButtonEditor;
import com.devoxx.genie.ui.settings.semanticsearch.table.ButtonRenderer;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import com.intellij.ui.JBColor;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;

public class SemanticSearchSettingsComponent extends AbstractSettingsComponent {

    private static final String CHECK_MARK = "✓";
    private static final String X_MARK = "✗";
    private static final Color SUCCESS_COLOR = new Color(0, 153, 51); // Green
    private static final Color ERROR_COLOR = new Color(204, 0, 0);    // Red

    private final JButton startIndexerButton = new JButton("Start indexing");
    private final JProgressBar indexingProgress = new JProgressBar();
    private final JLabel indexingStatusLabel = new JLabel();

    @Getter
    private final JBCheckBox enableIndexerCheckBox = new JBCheckBox("Enable semantic search", stateService.getSemanticSearchEnabled());

    @Getter
    private final JBIntSpinner portIndexer = new JBIntSpinner(new UINumericRange(stateService.getIndexerPort(), 8000, 9000));

    @Getter
    private final JBIntSpinner maxResults = new JBIntSpinner(new UINumericRange(stateService.getIndexerMaxResults(), 1, 50));

    @Getter
    private final JSpinner minScoreField = new JSpinner(new SpinnerNumberModel(stateService.getIndexerMinScore().doubleValue(), 0.0d, 1.0d, 0.01d));

    private final JButton startChromaDBButton = new JButton("Pull/Start ChromaDB");

    private final Project project;
    private final JBTable collectionsTable;
    private final DefaultTableModel tableModel;
    private JLabel validationMessage;
    private boolean notOkay;

    public SemanticSearchSettingsComponent(Project project) {
        this.project = project;
        addListeners();

        String[] columnNames = {"Collection", "Indexed Segments", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0);
        collectionsTable = new JBTable(tableModel);
        setupTable();
    }

    @Override
    public JPanel createPanel() {

        JBLabel descriptionLabel = new JBLabel();
        descriptionLabel.setText("<html><body style='width: 100%;'>" +
                "We can index your project files using embeddings which allows us to provide semantic code search.<br>" +
                "The indexer uses the 'Scan & Copy Project' settings to exclude certain directories, files, and file extensions.<br>" +
                "The indexer will index your project files and store them in a local vector database for fast search results.<br>" +
                "The indexer will also index the project files in the background to keep the index up-to-date." +
                "</body></html>");
        descriptionLabel.setForeground(UIUtil.getContextHelpForeground());
        descriptionLabel.setBorder(JBUI.Borders.emptyBottom(10));

        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        JPanel enablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enablePanel.add(enableIndexerCheckBox);
        panel.add(enablePanel, gbc);
        enableIndexerCheckBox.addActionListener(e -> updateComponentsEnabled());

        gbc.gridy++;
        JBLabel infoLabel = new JBLabel();
        infoLabel.setText("<html><body style='width: 100%;'>" +
                "Semantic search allows you to find relevant code based on natural language queries.<BR>" +
                "When enabled, the indexer will maintain a searchable index of your project's code.</body></html>");
        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JXTitledSeparator("Semantic search settings"), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Chroma DB port"), gbc);
        gbc.gridx = 1;
        panel.add(portIndexer, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Set the minimum score used for semantic search results to be used.  Lower value will include more."), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Minimum score"), gbc);
        gbc.gridx = 1;
        panel.add(minScoreField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("How many results do you want to include in prompt context?"), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Maximum results"), gbc);
        gbc.gridx = 1;
        panel.add(maxResults, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JXTitledSeparator("Indexed projects"), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(collectionsTable), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JXTitledSeparator("Required Semantic Search Services"), gbc);

        validate();

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        if (notOkay) {
            panel.add(new JLabel("Please fix the issues above before starting the indexer"), gbc);
            startIndexerButton.setText("Re-check");
            startIndexerButton.addActionListener(e -> validate());
        } else {
            panel.add(startIndexerButton, gbc);
            startIndexerButton.addActionListener(e -> startIndexing());
        }

        updateComponentsEnabled();

        return panel;
    }

    /**
     * Disable/enable all relevant components based on the checkbox state
     */
    private void updateComponentsEnabled() {
        boolean enabled = enableIndexerCheckBox.isSelected();
        portIndexer.setEnabled(enabled);
        maxResults.setEnabled(enabled);
        minScoreField.setEnabled(enabled);
        startIndexerButton.setEnabled(enabled);
        collectionsTable.setEnabled(enabled);
    }

    private void validate() {
        ValidationResult result = ValidationService.getInstance().validateSemanticSearch();

        // Hide collections table if not all validators pass
        if (collectionsTable != null) {
            collectionsTable.setVisible(result.isValid());
        }

        if (result.isValid() && collectionsTable != null) {
            // Load collections only when all validators pass
            ((ButtonEditor) collectionsTable.getColumnModel()
                    .getColumn(2).getCellEditor())
                    .safeLoadCollections();
        } else {
            result.statuses().forEach(validator -> validationCheck(validator, new GridBagConstraints()));
        }
    }

    private void setupTable() {
        TableColumn actionColumn = collectionsTable.getColumnModel().getColumn(2);
        actionColumn.setCellRenderer(new ButtonRenderer());
        actionColumn.setCellEditor(new ButtonEditor(project, collectionsTable, tableModel));

        collectionsTable.setVisible(false);
    }

    private void validationCheck(@NotNull ValidatorStatus validatorStatus,
                                 @NotNull GridBagConstraints gbc) {
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;

        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        JLabel nameLabel = new JLabel(validatorStatus.name());
        JLabel statusLabel = new JLabel();
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, 14));

        boolean isValid = validatorStatus.isValid();
        if (isValid) {
            statusLabel.setText(CHECK_MARK);
            statusLabel.setForeground(SUCCESS_COLOR);
        } else {
            statusLabel.setText(X_MARK);
            statusLabel.setForeground(ERROR_COLOR);
        }

        statusPanel.add(nameLabel, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.EAST);
        panel.add(statusPanel, gbc);

        gbc.gridx = 1;
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel messageLabel = new JLabel();
        messageLabel.setForeground(isValid ? SUCCESS_COLOR : ERROR_COLOR);

        String statusMessage = getStatusMessage(validatorStatus, isValid);
        notOkay = statusMessage.contains("not");
        messageLabel.setText(statusMessage);
        messagePanel.add(messageLabel);

        // Add ChromaDB button if validation fails
        if (!isValid && validatorStatus.name().equals("chromadb")) {
            validationMessage = messageLabel;
            startChromaDBButton.addActionListener(e -> startChromaDB());
            messagePanel.add(startChromaDBButton);

            // TODO: Implement ChromaDB validation
//            if (!chromaValidator.isImagePresent()) {
//                startChromaDBButton.setText("Pull ChromaDB image");
//            } else if (!chromaValidator.isContainerRunning()) {
//                startChromaDBButton.setText("Start ChromaDB");
//            }

            startChromaDBButton.addActionListener(e -> startChromaDB());
            messagePanel.add(startChromaDBButton);
        }

        panel.add(messagePanel, gbc);
    }

    private void startChromaDB() {
        ChromeDBValidator validator = new ChromeDBValidator();
        String taskTitle = validator.isImagePresent() ? "Starting chromaDB" : "Pulling chromaDB image";

        ProgressManager.getInstance().run(new Task.Backgroundable(project, taskTitle) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {

                indicator.setIndeterminate(true);

                if (!validator.isImagePresent()) {
                    indicator.setText("Pulling ChromaDB image...");
                } else {
                    indicator.setText("Starting ChromaDB container...");
                }

                new ChromaDockerService().startChromaDB(project, new ChromaDBStatusCallback() {
                    @Override
                    public void onSuccess() {
                        // Update UI in EDT
                        ApplicationManager.getApplication().invokeLater(() -> {
                            validationMessage.setForeground(JBColor.GREEN);
                            validationMessage.setText("ChromaDB is running correctly");
                            validationMessage.setVisible(true);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Update UI in EDT
                        ApplicationManager.getApplication().invokeLater(() -> {
                            validationMessage.setForeground(JBColor.RED);
                            validationMessage.setText("Error: " + message);
                            validationMessage.setVisible(true);
                        });
                    }
                });

                // Wait for ChromaDB to be fully operational with timeout
                indicator.setText("Waiting for ChromaDB to be ready...");
                boolean isReady = waitForChromaDB(indicator);

                // Refresh validation on EDT
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isReady) {
                        String successMessage = validator.isImagePresent() ?
                                "ChromaDB started successfully" :
                                "ChromaDB image pulled and started successfully";
                        NotificationUtil.sendNotification(project, successMessage);

                        // Re-run validation
                        validate();

                        // Load collections only after ChromaDB is confirmed ready
                        ((ButtonEditor) collectionsTable.getColumnModel()
                                .getColumn(2).getCellEditor())
                                .safeLoadCollections();
                    } else {
                        NotificationUtil.sendNotification(project,
                                "ChromaDB started but not responding. Please check Docker logs.");
                    }
                });
            }
        });
    }

    private boolean waitForChromaDB(@NotNull ProgressIndicator indicator) {
        ChromeDBValidator validator = new ChromeDBValidator();
        int maxAttempts = 3;
        int attempts = 0;

        while (attempts < maxAttempts) {
            if (indicator.isCanceled()) {
                return false;
            }

            if (validator.isValid()) {
                return true;
            }

            try {
                Thread.sleep(1000); // Wait 1 second between checks
                attempts++;
                indicator.setFraction((double) attempts / maxAttempts);
                indicator.setText("Waiting for ChromaDB to be ready... (" + attempts + "/" + maxAttempts + " seconds)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    private @NotNull String getStatusMessage(ValidatorStatus validatorStatus, boolean isValid) {
        if (isValid) {
            return "Running";
        }

        return switch (validatorStatus.name()) {
            case "docker" -> "Docker is not running. Please start Docker Desktop";
            case "chromadb" -> validatorStatus.message();
            case "ollama" -> "Ollama is not running. Please start Ollama service";
            case "nomic" -> "Nomic model is not available. Please run: ollama pull nomic-embed-text";
            default -> "Not available";
        };
    }

    private void startIndexing() {
        startIndexerButton.setEnabled(false);
        indexingProgress.setVisible(true);
        indexingStatusLabel.setText("Indexing in progress...");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Indexing project") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    // Start indexing
                    ProjectIndexerService indexer = ProjectIndexerService.getInstance();
                    indexer.init(project);
                    indexer.indexFiles(project.getBasePath(), false, indicator);

                    // Update UI on EDT
                    ApplicationManager.getApplication().invokeLater(() -> indexingComplete());
                } catch (Exception e) {
                    // Update UI on EDT
                    ApplicationManager.getApplication().invokeLater(() -> indexingFailed(e.getMessage()));
                }
            }
        });
    }

    private void indexingComplete() {
        indexingProgress.setVisible(false);
        indexingStatusLabel.setText("Indexing completed");
        indexingStatusLabel.setForeground(SUCCESS_COLOR);
    }

    private void indexingFailed(String error) {
        indexingProgress.setVisible(false);
        indexingStatusLabel.setText("Indexing failed: " + error);
        indexingStatusLabel.setForeground(ERROR_COLOR);
    }
}