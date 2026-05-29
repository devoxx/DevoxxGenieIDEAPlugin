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
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
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

    @Getter
    private final JBCheckBox queryExpansionCheckBox = new JBCheckBox(
            "Enable LLM query expansion (one extra LLM call per RAG search)",
            Boolean.TRUE.equals(stateService.getRagQueryExpansionEnabled()));

    @Getter
    private final JBIntSpinner queryExpansionVariantsSpinner = new JBIntSpinner(
            new UINumericRange(
                    stateService.getRagQueryExpansionN() == null ? 3 : stateService.getRagQueryExpansionN(),
                    1, 10));

    @Getter
    private final JBCheckBox rerankCheckBox = new JBCheckBox(
            "Rerank results (extra Ollama call per retrieval)",
            Boolean.TRUE.equals(stateService.getRerankResults()));

    @Getter
    private final JBTextField rerankerModelField = new JBTextField(
            stateService.getRerankerModelName(), 20);

    @Getter
    private final JBIntSpinner rerankerShortlistSpinner = new JBIntSpinner(
            new UINumericRange(
                    stateService.getRerankerShortlistSize() == null ? 30 : stateService.getRerankerShortlistSize(),
                    1, 200));

    @Getter
    private final JBIntSpinner rerankerTimeoutSpinner = new JBIntSpinner(
            new UINumericRange(
                    stateService.getRerankerTimeoutMs() == null ? 2000 : stateService.getRerankerTimeoutMs(),
                    100, 60_000));

    @Getter
    private final RagExcludedDirectoriesPanel ragExcludedDirsPanel;

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
        this.ragExcludedDirsPanel = new RagExcludedDirectoriesPanel(
                project, stateService.getRagExcludedDirectories());

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
        // Variants spinner is only meaningful when query expansion is on.
        queryExpansionCheckBox.addActionListener(e -> updateComponentsEnabled());
        // Reranker model/shortlist/timeout fields are only meaningful when the master toggle is on.
        rerankCheckBox.addActionListener(e -> updateComponentsEnabled());
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
        // Use the wrapping help-text component for the same width-constrained behaviour as the
        // per-setting help rows; explicit line breaks not needed since JTextArea word-wraps.
        addHelpText(panel, gbc,
                "Retrieval-augmented Generation (RAG) leverages semantic search to find relevant code " +
                "based on your queries. The indexer respects the \"Scan & Copy Project\" exclusion " +
                "settings AND the RAG-specific \"Excluded directories\" list below (project-relative " +
                "path prefixes), and stores the indexed files in a local ChromaDB vector database.");
        addHelpText(panel, gbc,
                "How RAG is used at chat time:\n" +
                " • Agent mode OFF — top-K relevant chunks are injected automatically into every " +
                "prompt context (passive retrieval).\n" +
                " • Agent mode ON — RAG is exposed to the LLM as a `semantic_search` agent tool " +
                "(the passive injection is suppressed to avoid duplicate context). The agent decides " +
                "when to call it. The tool can be individually enabled/disabled under " +
                "Settings → DevoxxGenie → Agent Mode → Built-in Tools.");
    }

    private void addRAGSettingsSection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, RAG_SETTINGS_SECTION_TITLE);
        addSettingRow(panel, gbc, "Chroma DB port", leftAligned(portIndexer));
        addSettingRow(panel, gbc, "Minimum score", leftAligned(minScoreField));
        addHelpText(panel, gbc, "Set the minimum score threshold for semantic search results. A lower value will include more results.");
        addSettingRow(panel, gbc, "Maximum results", leftAligned(maxResultsSpinner));
        addHelpText(panel, gbc, "How many results do you want to include in prompt window context?");

        addSettingRow(panel, gbc, "Query expansion", leftAligned(queryExpansionCheckBox));
        addHelpText(panel, gbc, "Paraphrase the query into multiple variants and fuse the per-variant results " +
                "(Reciprocal Rank Fusion). Improves retrieval on meta-style questions such as " +
                "\"where do we discuss X?\" at the cost of one extra LLM call per RAG search.");
        addSettingRow(panel, gbc, "Number of variants", leftAligned(queryExpansionVariantsSpinner));

        // Reranker (task-214): optional cross-encoder stage that reorders the retrieval
        // shortlist before it reaches the prompt. Local-first via an Ollama-hosted model.
        addSettingRow(panel, gbc, "Rerank results", leftAligned(rerankCheckBox));
        addHelpText(panel, gbc, "Reorder retrieval hits using a local Ollama-hosted reranker model. " +
                "Retrieval returns a wider shortlist; the reranker picks the top \"Maximum results\" " +
                "for the prompt. On timeout the original retrieval order is used.");
        addSettingRow(panel, gbc, "Reranker model", leftAligned(rerankerModelField));
        addHelpText(panel, gbc, "Ollama generative chat model used for relevance scoring " +
                "(default 'llama3.2:1b'; 'qwen2.5:0.5b' is also a good fit). Cross-encoder " +
                "models like 'bge-reranker' DO NOT work — they are served via /api/embeddings, " +
                "and this reranker uses /api/generate. The validator below checks the model is " +
                "pulled and offers a one-click pull.");
        addSettingRow(panel, gbc, "Reranker shortlist size", leftAligned(rerankerShortlistSpinner));
        addHelpText(panel, gbc, "How many candidates retrieval returns to the reranker. " +
                "The reranker truncates to \"Maximum results\" before the prompt sees them.");
        addSettingRow(panel, gbc, "Reranker timeout (ms)", leftAligned(rerankerTimeoutSpinner));
        addHelpText(panel, gbc, "Wall-clock budget for the reranker. On timeout, original retrieval order is used.");

        // RAG-specific directory exclusion (task-220) — layered on top of the global
        // "Scan & Copy Project" list so users can keep project context broad while keeping
        // RAG narrow.
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(ragExcludedDirsPanel, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        addHelpText(panel, gbc,
                "Directories the RAG indexer should skip, in addition to the \"Scan & Copy " +
                "Project\" exclusions. Use Browse... in Add/Edit to pick a directory — its " +
                "absolute path is inserted so the entry uniquely identifies the directory and " +
                "the project it belongs to. You can also manually type a project-relative path " +
                "(e.g. \"docs/book\") if you prefer the entry to survive moving the project. " +
                "A file is excluded when its absolute or project-relative path equals an entry " +
                "or starts with one followed by \"/\" (case-sensitive). Changes take effect on " +
                "the next \"Start Indexing\" or file-watcher reindex — no automatic reindex " +
                "is triggered when you edit this list.");
    }

    /**
     * Add a wrapping help-text row under a setting. JLabel doesn't word-wrap reliably even
     * with the HTML-and-100%-width trick (BasicHTML computes preferred width from the full
     * text, so the dialog grows to fit). A non-editable JTextArea styled like a label DOES
     * wrap natively when the layout gives it a constrained width, so use that.
     */
    private void addHelpText(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, @NotNull String text) {
        JTextArea helpArea = new JTextArea(text);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setEditable(false);
        helpArea.setFocusable(false);
        helpArea.setOpaque(false);
        helpArea.setBorder(null);
        helpArea.setFont(UIManager.getFont("Label.font"));
        helpArea.setForeground(UIUtil.getContextHelpForeground());
        // (0, ...) preferred width — let the GridBag column dictate width; height grows as needed
        helpArea.setPreferredSize(null);
        helpArea.setMinimumSize(new Dimension(0, 0));

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(helpArea, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
    }

    /**
     * Wrap a spinner/checkbox in a left-aligned FlowLayout panel so it keeps its natural
     * width when the enclosing column uses {@code GridBagConstraints.HORIZONTAL} fill. Without
     * this, JSpinner stretches to fill the whole column and the digits become hard to spot.
     */
    private static @NotNull JComponent leftAligned(@NotNull JComponent component) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.add(component);
        return wrapper;
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
        queryExpansionCheckBox.setEnabled(enabled);
        // Variants spinner is doubly-gated: master switch + the expansion sub-switch.
        queryExpansionVariantsSpinner.setEnabled(enabled && queryExpansionCheckBox.isSelected());
        rerankCheckBox.setEnabled(enabled);
        boolean rerankActive = enabled && rerankCheckBox.isSelected();
        rerankerModelField.setEnabled(rerankActive);
        rerankerShortlistSpinner.setEnabled(rerankActive);
        rerankerTimeoutSpinner.setEnabled(rerankActive);
    }

    private void setupTable() {
        TableColumn actionColumn = collectionsTable.getColumnModel().getColumn(2);
        actionColumn.setCellRenderer(new ButtonRenderer());
        buttonEditor = new ButtonEditor(project, collectionsTable, tableModel);
        actionColumn.setCellEditor(buttonEditor);
        collectionsTable.setVisible(false);
    }

    /**
     * Add/edit/remove panel for the RAG-specific directory exclusion list (task-220). Mirrors
     * the pattern used by the project-scanner's {@code ExcludedDirectoriesPanel} in
     * {@code CopyProjectSettingsComponent} so the two lists look and feel identical.
     *
     * <p>The edit dialog offers an optional "Browse..." button that opens IntelliJ's directory
     * chooser scoped to the project root. The chosen directory's name (last path segment) is
     * inserted into the editable text field; matching is segment-based so only the name is
     * stored. Manual typing remains supported — Browse is purely a convenience.
     */
    public static class RagExcludedDirectoriesPanel extends AddEditRemovePanel<String> {
        private final Project project;

        public RagExcludedDirectoriesPanel(Project project, List<String> initialData) {
            super(new RagExcludedDirectoriesModel(), initialData, "Excluded directories");
            this.project = project;
            setPreferredSize(new Dimension(400, 160));
        }

        @Override
        protected String addItem() {
            return showEditDialog("");
        }

        @Override
        protected boolean removeItem(String item) {
            return true;
        }

        @Override
        protected String editItem(String item) {
            return showEditDialog(item);
        }

        private @Nullable String showEditDialog(String initialValue) {
            JBTextField field = new JBTextField(initialValue);
            JButton browseButton = new JButton("Browse...");
            browseButton.setToolTipText(
                    "Pick a directory from the project; its name will be inserted into the field above. " +
                    "Optional — you can still type a directory name directly.");
            browseButton.addActionListener(e -> {
                FileChooserDescriptor descriptor =
                        FileChooserDescriptorFactory.createSingleFolderDescriptor();
                descriptor.setTitle("Select Directory to Exclude from RAG Indexing");
                String basePath = project.getBasePath();
                VirtualFile toSelect = basePath != null
                        ? LocalFileSystem.getInstance().findFileByPath(basePath)
                        : null;
                VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
                if (chosen != null) {
                    // Insert the absolute path so the entry shows exactly which directory in
                    // which project the user picked — no ambiguity with same-named dirs
                    // elsewhere. Users who prefer portability can still manually type a
                    // project-relative path like "docs/book"; the matcher accepts both.
                    field.setText(chosen.getPath());
                    field.requestFocusInWindow();
                }
            });

            JPanel inputRow = new JPanel(new BorderLayout(5, 0));
            inputRow.add(field, BorderLayout.CENTER);
            inputRow.add(browseButton, BorderLayout.EAST);

            JPanel dialogPanel = new JPanel(new BorderLayout(0, 5));
            dialogPanel.add(new JLabel("Directory:"), BorderLayout.NORTH);
            dialogPanel.add(inputRow, BorderLayout.CENTER);
            dialogPanel.setBorder(JBUI.Borders.empty(10));
            // Give the row a sensible width so the text field doesn't collapse to a few pixels
            // when the dialog opens with an empty value.
            dialogPanel.setPreferredSize(new Dimension(420, 80));

            int result = JOptionPane.showConfirmDialog(this, dialogPanel, "Enter Directory",
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String trimmed = field.getText().trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
            return null;
        }
    }

    private static class RagExcludedDirectoriesModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int columnIndex) {
            return "Directory";
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }
}
