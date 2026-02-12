package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.CircularDependencyException;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.service.spec.SpecTaskRunnerListener;
import com.devoxx.genie.service.spec.SpecTaskRunnerService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Main panel for the Spec Browser tool window.
 * Shows tasks grouped by status in a tree, with a preview panel below.
 * Supports checkbox selection of To Do tasks for batch sequential execution.
 */
public class SpecBrowserPanel extends SimpleToolWindowPanel implements SpecTaskRunnerListener {

    // Preferred status ordering (others appended at end)
    private static final List<String> STATUS_ORDER = List.of("To Do", "In Progress", "Done");

    private static final int HORIZONTAL_LAYOUT_MIN_WIDTH = 600;

    private final Project project;
    private final Tree specTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final SpecPreviewPanel previewPanel;
    private final JBSplitter splitter;
    private final SpecTreeCellRenderer cellRenderer;
    private final SpecTaskRunnerProgressPanel progressPanel;

    // Checkbox tracking for To Do tasks
    private final Set<String> checkedTaskIds = new LinkedHashSet<>();

    // Toolbar actions (need references for enable/disable)
    private AnAction runSelectedAction;
    private AnAction runAllTodoAction;
    private AnAction cancelRunAction;

    // Execution mode ComboBox
    private JComboBox<String> executionModeCombo;
    private boolean refreshingCombo = false;
    private static final String LLM_PROVIDER_LABEL = "LLM Provider";

    public SpecBrowserPanel(@NotNull Project project) {
        super(true, true);
        this.project = project;

        // Tree setup
        rootNode = new DefaultMutableTreeNode("Specs");
        treeModel = new DefaultTreeModel(rootNode);
        specTree = new Tree(treeModel);
        specTree.setRootVisible(false);
        specTree.setShowsRootHandles(true);
        cellRenderer = new SpecTreeCellRenderer();
        cellRenderer.setCheckedTaskIds(checkedTaskIds);
        specTree.setCellRenderer(cellRenderer);

        // Preview panel
        previewPanel = new SpecPreviewPanel(project);
        previewPanel.setOnImplementAction(this::implementCurrentSpec);

        // Tree selection listener
        specTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof TaskSpec spec) {
                    previewPanel.showSpec(spec);
                    openTaskFile(spec);
                } else {
                    previewPanel.showEmptyState();
                }
            } else {
                previewPanel.showEmptyState();
            }
        });

        // Mouse listener for checkbox toggling on To Do tasks
        specTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleCheckboxClick(e);
            }
        });

        // Progress panel (shown above splitter during batch runs)
        progressPanel = new SpecTaskRunnerProgressPanel();

        // Layout with splitter — orientation adapts to available width
        splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(new JBScrollPane(specTree));
        splitter.setSecondComponent(previewPanel);

        // Main content: progress panel + splitter
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(progressPanel, BorderLayout.NORTH);
        contentWrapper.add(splitter, BorderLayout.CENTER);
        setContent(contentWrapper);

        // Switch between vertical (narrow/sidebar) and horizontal (wide/bottom) layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSplitterOrientation();
            }
        });

        // Toolbar
        setupToolbar();

        // Listen for spec changes
        SpecService specService = SpecService.getInstance(project);
        specService.addChangeListener(() -> ApplicationManager.getApplication().invokeLater(this::refreshTree));

        // Register as runner listener
        SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
        runner.addListener(this);

        // Refresh execution mode combo when settings change (e.g., CLI tools added/removed)
        // SpecSettingsConfigurable publishes on the project message bus
        project.getMessageBus()
                .connect()
                .subscribe(AppTopics.SETTINGS_CHANGED_TOPIC,
                        (com.devoxx.genie.ui.listener.SettingsChangeListener) hasKey ->
                                ApplicationManager.getApplication().invokeLater(this::refreshExecutionModeCombo));

        // Initial load
        refreshTree();
    }

    private void setupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Refresh", "Refresh specs from disk", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshSpecs();
            }
        });

        actionGroup.addSeparator();

        runSelectedAction = new AnAction("Run Selected", "Run checked To Do tasks sequentially", AllIcons.Actions.Execute) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runSelectedTasks();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
                e.getPresentation().setEnabled(!runner.isRunning() && !checkedTaskIds.isEmpty());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        actionGroup.add(runSelectedAction);

        runAllTodoAction = new AnAction("Run All To Do", "Run all To Do tasks sequentially", AllIcons.Actions.RunAll) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runAllTodoTasks();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
                e.getPresentation().setEnabled(!runner.isRunning());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        actionGroup.add(runAllTodoAction);

        cancelRunAction = new AnAction("Cancel Run", "Stop after current task finishes", AllIcons.Actions.Suspend) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                SpecTaskRunnerService.getInstance(project).cancel();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                SpecTaskRunnerService runner = SpecTaskRunnerService.getInstance(project);
                e.getPresentation().setVisible(runner.isRunning());
                e.getPresentation().setEnabled(runner.isRunning());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        actionGroup.add(cancelRunAction);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("SpecBrowser", actionGroup, true);
        toolbar.setTargetComponent(this);

        // Build a composite toolbar with the execution mode ComboBox + action buttons
        executionModeCombo = new JComboBox<>();
        executionModeCombo.setToolTipText("Select execution mode: LLM Provider or external CLI tool");
        refreshExecutionModeCombo();
        executionModeCombo.addActionListener(e -> onExecutionModeChanged());

        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(executionModeCombo, BorderLayout.WEST);
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
        setToolbar(toolbarPanel);
    }

    private void refreshExecutionModeCombo() {
        if (executionModeCombo == null) return;

        // Suppress listener during programmatic updates
        refreshingCombo = true;
        try {
            executionModeCombo.removeAllItems();
            executionModeCombo.addItem(LLM_PROVIDER_LABEL);

            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            List<CliToolConfig> tools = stateService.getCliTools();
            if (tools != null) {
                for (CliToolConfig tool : tools) {
                    if (tool.isEnabled()) {
                        executionModeCombo.addItem("CLI: " + tool.getName());
                    }
                }
            }

            // Restore selection
            String mode = stateService.getSpecRunnerMode();
            if ("cli".equalsIgnoreCase(mode)) {
                String selectedTool = stateService.getSpecSelectedCliTool();
                String label = "CLI: " + selectedTool;
                for (int i = 0; i < executionModeCombo.getItemCount(); i++) {
                    if (label.equals(executionModeCombo.getItemAt(i))) {
                        executionModeCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
            executionModeCombo.setSelectedIndex(0);
        } finally {
            refreshingCombo = false;
        }
    }

    private void onExecutionModeChanged() {
        if (executionModeCombo == null || refreshingCombo) return;
        String selected = (String) executionModeCombo.getSelectedItem();
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        if (selected == null || LLM_PROVIDER_LABEL.equals(selected)) {
            stateService.setSpecRunnerMode("llm");
            stateService.setSpecSelectedCliTool("");
        } else if (selected.startsWith("CLI: ")) {
            stateService.setSpecRunnerMode("cli");
            stateService.setSpecSelectedCliTool(selected.substring("CLI: ".length()));
        }
    }

    // ===== Checkbox Handling =====

    private void handleCheckboxClick(MouseEvent e) {
        TreePath path = specTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof TaskSpec spec)) return;

        // Only To Do tasks have checkboxes
        if (!"To Do".equalsIgnoreCase(spec.getStatus()) || spec.getId() == null) return;

        // Check if click is in the icon area (roughly first 20px of the row)
        Rectangle bounds = specTree.getPathBounds(path);
        if (bounds == null) return;

        int iconAreaWidth = 20;
        if (e.getX() < bounds.x + iconAreaWidth) {
            // Toggle checkbox
            if (checkedTaskIds.contains(spec.getId())) {
                checkedTaskIds.remove(spec.getId());
            } else {
                checkedTaskIds.add(spec.getId());
            }
            specTree.repaint();
        }
    }

    /**
     * Get all checked tasks from the tree.
     */
    private @NotNull List<TaskSpec> getCheckedTasks() {
        List<TaskSpec> tasks = new ArrayList<>();
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode statusNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            for (int j = 0; j < statusNode.getChildCount(); j++) {
                DefaultMutableTreeNode taskNode = (DefaultMutableTreeNode) statusNode.getChildAt(j);
                if (taskNode.getUserObject() instanceof TaskSpec spec) {
                    if (spec.getId() != null && checkedTaskIds.contains(spec.getId())) {
                        tasks.add(spec);
                    }
                }
            }
        }
        return tasks;
    }

    // ===== Task Runner Actions =====

    private void runSelectedTasks() {
        List<TaskSpec> tasks = getCheckedTasks();
        if (tasks.isEmpty()) {
            NotificationUtil.sendWarningNotification(project, "No tasks selected. Check the boxes next to To Do tasks first.");
            return;
        }
        try {
            SpecTaskRunnerService.getInstance(project).runTasks(tasks);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CircularDependencyException) {
                NotificationUtil.sendErrorNotification(project,
                        "Cannot run tasks: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void runAllTodoTasks() {
        try {
            SpecTaskRunnerService.getInstance(project).runAllTodoTasks();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CircularDependencyException) {
                NotificationUtil.sendErrorNotification(project,
                        "Cannot run tasks: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    // ===== SpecTaskRunnerListener =====

    @Override
    public void onRunStarted(int totalTasks) {
        NotificationUtil.sendNotification(project, "Starting batch run of " + totalTasks + " task(s)");
    }

    @Override
    public void onTaskStarted(TaskSpec task, int index, int total) {
        progressPanel.update(task, index, total);
        NotificationUtil.sendNotification(project,
                String.format("Task %d/%d started: %s", index + 1, total, task.getDisplayLabel()));
    }

    @Override
    public void onTaskCompleted(TaskSpec task, int index, int total) {
        // Tree will refresh via SpecService change listener
    }

    @Override
    public void onTaskSkipped(TaskSpec task, int index, int total, String reason) {
        NotificationUtil.sendWarningNotification(project,
                String.format("Task %s skipped: %s", task.getDisplayLabel(), reason));
    }

    @Override
    public void onRunFinished(int completed, int skipped, int total, SpecTaskRunnerService.RunnerState finalState) {
        progressPanel.showCompleted(completed, skipped, total);
        checkedTaskIds.clear();
        specTree.repaint();

        String msg = switch (finalState) {
            case ALL_COMPLETED -> String.format("Batch run complete: %d/%d tasks done", completed, total);
            case CANCELLED -> String.format("Batch run cancelled: %d/%d done, %d skipped", completed, total, skipped);
            case ERROR -> String.format("Batch run stopped due to error: %d/%d done, %d skipped. Check the CLI tool's authentication and configuration.", completed, total, skipped);
            default -> "Batch run finished";
        };
        if (finalState == SpecTaskRunnerService.RunnerState.ERROR) {
            NotificationUtil.sendErrorNotification(project, msg);
        } else {
            NotificationUtil.sendNotification(project, msg);
        }
    }

    // ===== Existing Methods =====

    /**
     * Refresh specs from disk and rebuild the tree.
     */
    public void refreshSpecs() {
        SpecService.getInstance(project).refresh();
    }

    private void updateSplitterOrientation() {
        int width = getWidth();
        boolean shouldBeVertical = width < HORIZONTAL_LAYOUT_MIN_WIDTH;
        if (splitter.getOrientation() != shouldBeVertical) {
            splitter.setOrientation(shouldBeVertical);
            if (!shouldBeVertical) {
                // Horizontal: tree on left gets ~40%, details on right gets ~60%
                splitter.setProportion(0.4f);
            } else {
                // Vertical: tree on top gets ~50%
                splitter.setProportion(0.5f);
            }
        }
    }

    private void refreshTree() {
        rootNode.removeAllChildren();

        List<TaskSpec> specs = SpecService.getInstance(project).getAllSpecs();

        // Remove checked IDs that no longer exist or are no longer To Do
        Set<String> validIds = new HashSet<>();
        for (TaskSpec spec : specs) {
            if (spec.getId() != null && "To Do".equalsIgnoreCase(spec.getStatus())) {
                validIds.add(spec.getId());
            }
        }
        checkedTaskIds.retainAll(validIds);

        // Group by status
        Map<String, List<TaskSpec>> grouped = new LinkedHashMap<>();
        // Add known statuses first to maintain order
        for (String status : STATUS_ORDER) {
            grouped.put(status, new ArrayList<>());
        }

        for (TaskSpec spec : specs) {
            String status = spec.getStatus() != null ? spec.getStatus() : "To Do";
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(spec);
        }

        // Build tree nodes, sorting tasks by ID number within each status group
        for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet()) {
            List<TaskSpec> statusSpecs = entry.getValue();
            if (statusSpecs.isEmpty()) {
                continue;
            }

            statusSpecs.sort((a, b) -> Integer.compare(extractTaskNumber(a.getId()), extractTaskNumber(b.getId())));

            DefaultMutableTreeNode statusNode = new DefaultMutableTreeNode(entry.getKey());
            for (TaskSpec spec : statusSpecs) {
                statusNode.add(new DefaultMutableTreeNode(spec));
            }
            rootNode.add(statusNode);
        }

        treeModel.reload();

        // Expand all status groups
        for (int i = 0; i < specTree.getRowCount(); i++) {
            specTree.expandRow(i);
        }
    }

    private static int extractTaskNumber(String id) {
        if (id == null) return Integer.MAX_VALUE;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(id);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return Integer.MAX_VALUE;
    }

    private void openTaskFile(@NotNull TaskSpec spec) {
        if (spec.getFilePath() == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(spec.getFilePath());
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, false);
        }
    }

    private void implementCurrentSpec() {
        TaskSpec spec = previewPanel.getCurrentSpec();
        if (spec == null) {
            return;
        }

        // Build a prompt that includes full task details and an implementation instruction
        String taskDetails = SpecContextBuilder.buildContext(spec);
        String instruction = SpecContextBuilder.buildAgentInstruction(spec);
        String prompt = instruction + "\n\n" + taskDetails + "\n\nPlease implement the task described above, satisfying all acceptance criteria.";

        // Submit via the prompt submission topic
        project.getMessageBus()
                .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                .onPromptSubmitted(project, prompt);
    }
}
