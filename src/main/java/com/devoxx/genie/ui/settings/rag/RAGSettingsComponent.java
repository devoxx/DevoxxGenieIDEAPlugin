package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.devoxx.genie.service.rag.embedding.EmbeddingModelInstaller;
import com.devoxx.genie.service.rag.embedding.EmbeddingPrefixes;
import com.devoxx.genie.service.rag.embedding.EmbeddingProvider;
import com.devoxx.genie.service.rag.embedding.EmbeddingProviderSettings;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidationResult;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.ProjectScanSettingsService;
import com.devoxx.genie.ui.settings.rag.table.ButtonEditor;
import com.devoxx.genie.ui.settings.rag.table.ButtonRenderer;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
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
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Objects;


public class RAGSettingsComponent extends AbstractSettingsComponent {

    private static final int INSET_VALUE = 5;
    private static final String RAG_SETTINGS_SECTION_TITLE = "Retrieval-augmented Generation (RAG) settings";

    private final JBTextField testQueryField = new JBTextField();
    private final JButton testQueryButton = new JButton("Search");
    private final RagSearchResultsPanel testQueryResults = new RagSearchResultsPanel();
    private final JProgressBar testQueryProgress = new JProgressBar();

    private JProgressBar progressBar;
    private JLabel progressLabel;

    @Getter
    private final JBCheckBox enableIndexerCheckBox = new JBCheckBox("Enable feature", stateService.getRagEnabled());

    @Getter
    private final JBIntSpinner portIndexer = new JBIntSpinner(new UINumericRange(stateService.getIndexerPort(), 8000, 9000));

    @Getter
    private final JBIntSpinner maxResultsSpinner = new JBIntSpinner(new UINumericRange(stateService.getIndexerMaxResults(), 1, 50));

    /**
     * Suggestions only: the combo is editable and accepts any model name. Listing installed
     * models would need a network call, which must not happen on the EDT.
     */
    static final List<String> SUGGESTED_EMBEDDING_MODELS = EmbeddingProvider.OLLAMA.suggestedModels();

    private final ComboBox<EmbeddingProvider> embeddingProviderComboBox = new ComboBox<>();
    private final JBLabel embeddingProviderWarning = new JBLabel(AllIcons.General.Warning);
    private final ComboBox<String> embeddingModelComboBox = createEmbeddingModelComboBox(
            stateService.getRagEmbeddingModelName());
    private final JBTextField queryPrefixField = new JBTextField(24);
    private final JBTextField documentPrefixField = new JBTextField(24);
    private final JBIntSpinner outputDimensionSpinner = new JBIntSpinner(0, 0, 65536, 64);
    /** True while code fills the embedding fields, so their listeners don't react to it. */
    private boolean loadingEmbeddingSettings;
    private String prefixDefaultsModel = "";

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
    private final RagExcludedDirectoriesPanel ragExcludedDirsPanel;

    private final Project project;
    private JButton startIndexButton;
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
                project, new java.util.ArrayList<>(ProjectScanSettingsService.getInstance(project).getRagExcludedDirectories()));

        initializeComponents();
        resetEmbeddingSettings();
        addEmbeddingListeners();
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
        addTestQuerySection(panel, gbc);

        // Takes any spare height, so sections stay at the top instead of spreading apart.
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private void addTestQuerySection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, "Test semantic search");
        addHelpText(panel, gbc, "Search the current project's index using saved RAG settings. Apply setting changes before testing.");
        JPanel queryRow = new JPanel(new BorderLayout(5, 0));
        queryRow.add(testQueryField, BorderLayout.CENTER);
        queryRow.add(testQueryButton, BorderLayout.EAST);
        addSettingRow(panel, gbc, "Test query", queryRow);
        testQueryProgress.setIndeterminate(true);
        testQueryProgress.setVisible(false);
        JPanel output = new JPanel(new BorderLayout(0, 5));
        output.add(testQueryProgress, BorderLayout.NORTH);
        output.add(testQueryResults, BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        panel.add(output, gbc);
        gbc.gridy++;
        testQueryButton.addActionListener(e -> runTestQuery());
        testQueryField.addActionListener(e -> {
            if (testQueryButton.isEnabled()) runTestQuery();
        });
    }

    private void runTestQuery() {
        String query = testQueryField.getText().trim();
        if (query.isEmpty()) {
            testQueryResults.showMessage("Enter a query to search the project index.");
            return;
        }
        testQueryButton.setEnabled(false);
        testQueryProgress.setVisible(true);
        testQueryResults.showMessage("Searching the project index...");
        new SwingWorker<List<SearchResult>, Void>() {
            @Override
            protected List<SearchResult> doInBackground() {
                return SemanticSearchService.getInstance().search(project, query);
            }

            @Override
            protected void done() {
                try {
                    testQueryResults.showResults(query, get(), project.getBasePath());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    testQueryResults.showMessage("Search interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    testQueryResults.showMessage("Search failed: " +
                            (cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()));
                } finally {
                    testQueryProgress.setVisible(false);
                    testQueryButton.setEnabled(true);
                }
            }
        }.execute();
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
        addSettingRow(panel, gbc, "Embedding provider", leftAligned(embeddingProviderComboBox));
        addSettingRow(panel, gbc, "", leftAligned(embeddingProviderWarning));
        addHelpText(panel, gbc, "Providers enabled in the LLM settings that can create embeddings. " +
                "RAG uses the URL and API key configured there.");
        addSettingRow(panel, gbc, "Embedding model", leftAligned(embeddingModelComboBox));
        addHelpText(panel, gbc, "Model used to embed project files and queries. " +
                "Changing the provider, model, document prefix or output dimension requires a full " +
                "re-index; until then search keeps using the settings the index was built with.");
        addSettingRow(panel, gbc, "Query prefix", leftAligned(queryPrefixField));
        addSettingRow(panel, gbc, "Document prefix", leftAligned(documentPrefixField));
        addHelpText(panel, gbc, "Text added before search queries and indexed chunks when embedding, " +
                "for models that expect it. Filled in for known models, and never stored or shown. " +
                "Include any trailing space the model needs.");
        addSettingRow(panel, gbc, "Output dimension", leftAligned(outputDimensionSpinner));
        addHelpText(panel, gbc, "Shorten embeddings to this size to save memory, for models that support " +
                "it (Matryoshka). 0 keeps the model's own size.");
        addSettingRow(panel, gbc, "Minimum score", leftAligned(minScoreField));
        addHelpText(panel, gbc, "Set the minimum score threshold for semantic search results. A lower value will include more results.");
        addSettingRow(panel, gbc, "Maximum results", leftAligned(maxResultsSpinner));
        addHelpText(panel, gbc, "How many results do you want to include in prompt window context?");

        addSettingRow(panel, gbc, "Query expansion", leftAligned(queryExpansionCheckBox));
        addHelpText(panel, gbc, "Paraphrase the query into multiple variants and fuse the per-variant results " +
                "(Reciprocal Rank Fusion). Improves retrieval on meta-style questions such as " +
                "\"where do we discuss X?\" at the cost of one extra LLM call per RAG search.");
        addSettingRow(panel, gbc, "Number of variants", leftAligned(queryExpansionVariantsSpinner));

        // RAG-specific directory exclusion (task-220) — layered on top of the project
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
        gbc.weighty = 0;
        panel.add(new JBScrollPane(collectionsTable), gbc);
        gbc.gridy++;
    }

    private void addValidationSection(JPanel panel, GridBagConstraints gbc) {
        addSection(panel, gbc, "Required RAG Services");
        validationHandler.performValidation();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;

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
        
        // The model may need downloading, which can take minutes and asks the user first. Done
        // before the indexing task, off the EDT, with ModalityState.any() so it isn't held back
        // by the modal Settings dialog.
        startIndexButton.setEnabled(false);
        progressLabel.setText("Checking embedding model...");
        progressLabel.setVisible(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            EmbeddingModelInstaller.Result model = EmbeddingModelDownloadPrompt.ensureInstalled(project, panel,
                    status -> ApplicationManager.getApplication().invokeLater(
                            () -> progressLabel.setText(status), ModalityState.any()));
            ApplicationManager.getApplication().invokeLater(() -> {
                startIndexButton.setEnabled(true);
                if (project.isDisposed()) {
                    return;
                }
                if (model.ready()) {
                    refreshValidation();
                    runIndexingTask();
                } else {
                    progressLabel.setText(model.problem());
                    NotificationUtil.sendNotification(project, model.problem());
                }
            }, ModalityState.any());
        });
    }

    private void runIndexingTask() {
        // Set indexing flag
        isIndexing = true;

        // Make sure progress components are visible before starting the indexing process
        setStartButtons(false);
        
        // Run the indexing process as a cancellable background task so the IDE shows
        // determinate progress (n/total files, current file name) via the ProgressIndicator
        // that ProjectIndexerService.indexFiles() picks up from the progress context.
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Indexing project for RAG", true) {
            private Exception failure;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ProjectIndexerService.getInstance().indexFiles(project, true, progressBar, progressLabel);
                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    failure = e;
                }
            }

            @Override
            public void onCancel() {
                // Cancellation via the IDE progress UI must stop the indexer loop too.
                ProjectIndexerService.getInstance().cancelIndexing();
            }

            @Override
            public void onFinished() {
                // Runs on the EDT for both completion and cancellation.
                isIndexing = false;
                setStartButtons(true);

                if (failure != null) {
                    NotificationUtil.sendNotification(project, "Error indexing project: " + failure.getMessage());
                    return;
                }

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

    /** Called on the EDT with the result of one validation run. */
    void applyValidationResult(@NotNull ValidationResult result) {
        startIndexButton.setVisible(enableIndexerCheckBox.isSelected() && canStartIndexing(result.statuses()));
        panel.revalidate();
        panel.repaint();
        buttonEditor.safeLoadCollections();
    }

    /** A missing embedding model doesn't block indexing: Start Indexing offers to download it. */
    static boolean canStartIndexing(@NotNull List<ValidatorStatus> statuses) {
        return statuses.stream().allMatch(status ->
                status.isValid() || status.action() == ValidationActionType.PULL_EMBEDDING_MODEL);
    }

    /**
     * Disable/enable all relevant components based on the checkbox state
     */
    private void updateComponentsEnabled() {
        boolean enabled = enableIndexerCheckBox.isSelected();
        portIndexer.setEnabled(enabled);
        embeddingProviderComboBox.setEnabled(enabled);
        embeddingModelComboBox.setEnabled(enabled);
        queryPrefixField.setEnabled(enabled);
        documentPrefixField.setEnabled(enabled);
        outputDimensionSpinner.setEnabled(enabled);
        maxResultsSpinner.setEnabled(enabled);
        minScoreField.setEnabled(enabled);
        collectionsTable.setEnabled(enabled);
        queryExpansionCheckBox.setEnabled(enabled);
        // Variants spinner is doubly-gated: master switch + the expansion sub-switch.
        queryExpansionVariantsSpinner.setEnabled(enabled && queryExpansionCheckBox.isSelected());
    }

    static @NotNull ComboBox<String> createEmbeddingModelComboBox(@Nullable String modelName) {
        ComboBox<String> combo = new ComboBox<>(SUGGESTED_EMBEDDING_MODELS.toArray(String[]::new));
        combo.setEditable(true);
        setEmbeddingModelName(combo, modelName);
        return combo;
    }

    /**
     * The model name as typed, including text not yet committed with Enter. For Ollama, blank
     * means the default model; other providers have no default, so blank stays blank.
     */
    public @NotNull String getEmbeddingModelName() {
        if (selectedEmbeddingProvider() == EmbeddingProvider.OLLAMA) {
            return getEmbeddingModelName(embeddingModelComboBox);
        }
        return Objects.toString(embeddingModelComboBox.getEditor().getItem(), "").trim();
    }

    private void showEmbeddingModelName(@Nullable String modelName) {
        if (selectedEmbeddingProvider() == EmbeddingProvider.OLLAMA) {
            setEmbeddingModelName(embeddingModelComboBox, modelName);
        } else {
            String name = Objects.toString(modelName, "");
            embeddingModelComboBox.setSelectedItem(name);
            embeddingModelComboBox.getEditor().setItem(name);
        }
    }

    private @NotNull EmbeddingProvider selectedEmbeddingProvider() {
        return embeddingProviderComboBox.getItem() == null
                ? EmbeddingProvider.OLLAMA
                : embeddingProviderComboBox.getItem();
    }

    private void addEmbeddingListeners() {
        embeddingProviderComboBox.addActionListener(e -> {
            if (loadingEmbeddingSettings) return;
            EmbeddingProvider provider = selectedEmbeddingProvider();
            showSuggestedModels(provider);
            // Back on the saved provider, show its saved model; otherwise its default.
            showEmbeddingModelName(provider == EmbeddingProvider.fromId(stateService.getRagEmbeddingProvider())
                    ? stateService.getRagEmbeddingModelName()
                    : provider.defaultModel());
            updateEmbeddingProviderWarning();
            followModelPrefixDefaults();
        });
        Component editor = embeddingModelComboBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent textEditor) {
            textEditor.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent event) {
                    if (!loadingEmbeddingSettings) followModelPrefixDefaults();
                }
            });
        }
    }

    /** Prefix fields follow the model's defaults, unless the user typed their own. */
    private void followModelPrefixDefaults() {
        String model = getEmbeddingModelName();
        queryPrefixField.setText(EmbeddingPrefixes.afterModelChange(queryPrefixField.getText(),
                EmbeddingPrefixes.defaultQueryPrefix(prefixDefaultsModel), EmbeddingPrefixes.defaultQueryPrefix(model)));
        documentPrefixField.setText(EmbeddingPrefixes.afterModelChange(documentPrefixField.getText(),
                EmbeddingPrefixes.defaultDocumentPrefix(prefixDefaultsModel), EmbeddingPrefixes.defaultDocumentPrefix(model)));
        prefixDefaultsModel = model;
    }

    private void showSuggestedModels(@NotNull EmbeddingProvider provider) {
        boolean wasLoading = loadingEmbeddingSettings;
        loadingEmbeddingSettings = true;
        embeddingModelComboBox.removeAllItems();
        provider.suggestedModels().forEach(embeddingModelComboBox::addItem);
        loadingEmbeddingSettings = wasLoading;
    }

    private void updateEmbeddingProviderWarning() {
        EmbeddingProvider provider = selectedEmbeddingProvider();
        boolean warn = provider.sendsCodeOffMachine(EmbeddingProviderSettings.baseUrl(provider, stateService));
        embeddingProviderWarning.setVisible(warn);
        embeddingProviderWarning.setText(warn ? offMachineWarning(provider) : "");
    }

    static @NotNull String offMachineWarning(@NotNull EmbeddingProvider provider) {
        return "Your project's source code and search queries will be sent to " + provider
                + ", which may charge for it.";
    }

    /** Fills the embedding fields from the saved settings. */
    public void resetEmbeddingSettings() {
        loadingEmbeddingSettings = true;
        try {
            EmbeddingProvider provider = EmbeddingProvider.fromId(stateService.getRagEmbeddingProvider());
            embeddingProviderComboBox.removeAllItems();
            EmbeddingProviderSettings.selectable(stateService, provider).forEach(embeddingProviderComboBox::addItem);
            embeddingProviderComboBox.setItem(provider);
            showSuggestedModels(provider);
            showEmbeddingModelName(stateService.getRagEmbeddingModelName());
            String model = getEmbeddingModelName();
            queryPrefixField.setText(EmbeddingPrefixes.resolve(stateService.getRagQueryPrefix(),
                    EmbeddingPrefixes.defaultQueryPrefix(model)));
            documentPrefixField.setText(EmbeddingPrefixes.resolve(stateService.getRagDocumentPrefix(),
                    EmbeddingPrefixes.defaultDocumentPrefix(model)));
            Integer dimension = stateService.getRagOutputDimension();
            outputDimensionSpinner.setNumber(dimension == null ? 0 : dimension);
            prefixDefaultsModel = model;
            updateEmbeddingProviderWarning();
        } finally {
            loadingEmbeddingSettings = false;
        }
    }

    /** Saves the embedding fields. */
    public void applyEmbeddingSettings() {
        String model = getEmbeddingModelName();
        stateService.setRagEmbeddingProvider(selectedEmbeddingProvider().id());
        stateService.setRagEmbeddingModelName(model);
        stateService.setRagQueryPrefix(EmbeddingPrefixes.toStored(queryPrefixField.getText(),
                EmbeddingPrefixes.defaultQueryPrefix(model)));
        stateService.setRagDocumentPrefix(EmbeddingPrefixes.toStored(documentPrefixField.getText(),
                EmbeddingPrefixes.defaultDocumentPrefix(model)));
        stateService.setRagOutputDimension(outputDimensionSpinner.getNumber() == 0 ? null : outputDimensionSpinner.getNumber());
    }

    public boolean isEmbeddingSettingsModified() {
        String model = getEmbeddingModelName();
        Integer savedDimension = stateService.getRagOutputDimension();
        return selectedEmbeddingProvider() != EmbeddingProvider.fromId(stateService.getRagEmbeddingProvider())
                || !model.equals(savedModelName())
                || !Objects.equals(EmbeddingPrefixes.toStored(queryPrefixField.getText(),
                        EmbeddingPrefixes.defaultQueryPrefix(model)), stateService.getRagQueryPrefix())
                || !Objects.equals(EmbeddingPrefixes.toStored(documentPrefixField.getText(),
                        EmbeddingPrefixes.defaultDocumentPrefix(model)), stateService.getRagDocumentPrefix())
                || outputDimensionSpinner.getNumber() != (savedDimension == null ? 0 : savedDimension);
    }

    private @NotNull String savedModelName() {
        String saved = stateService.getRagEmbeddingModelName();
        return selectedEmbeddingProvider() == EmbeddingProvider.OLLAMA
                ? EmbeddingFingerprint.ollama(saved).modelName()
                : Objects.toString(saved, "").trim();
    }

    /** Re-runs the required services check, for example after the provider changes. */
    public void refreshValidation() {
        validationHandler.performValidation();
    }

    static @NotNull String getEmbeddingModelName(@NotNull ComboBox<String> combo) {
        Object item = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
        return EmbeddingFingerprint.ollama(Objects.toString(item, "")).modelName();
    }

    static void setEmbeddingModelName(@NotNull ComboBox<String> combo, @Nullable String modelName) {
        String name = EmbeddingFingerprint.ollama(modelName).modelName();
        // Editable combos accept items outside the model; set the editor too so a custom
        // name is shown even before the combo commits it.
        combo.setSelectedItem(name);
        combo.getEditor().setItem(name);
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
