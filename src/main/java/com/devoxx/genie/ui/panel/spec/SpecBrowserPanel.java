package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.enumarations.ExecutionMode;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.CircularDependencyException;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.service.spec.SpecTaskRunnerListener;
import com.devoxx.genie.service.spec.SpecTaskRunnerService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import lombok.extern.slf4j.Slf4j;
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
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Main panel for the Spec Browser tool window.
 * Shows tasks grouped by status in a tree, with a preview panel below.
 * Supports checkbox selection of To Do tasks for batch sequential execution.
 */
@Slf4j
public class SpecBrowserPanel extends SimpleToolWindowPanel implements SpecTaskRunnerListener {

    // Preferred status ordering (others appended at end)
    private static final List<String> STATUS_ORDER = List.of("To Do", "In Progress", "Done");
    private static final String ARCHIVED_GROUP = "Archived";

    private static final int HORIZONTAL_LAYOUT_MIN_WIDTH = 600;
    public static final String CANNOT_RUN_TASKS = "Cannot run tasks: ";

    private final transient Project project;
    private final Tree specTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final SpecPreviewPanel previewPanel;
    private final JBSplitter splitter;
    private final transient SpecTreeCellRenderer cellRenderer;
    private final SpecTaskRunnerProgressPanel progressPanel;
    private final SpecStatisticsPanel statisticsPanel;

    // Checkbox tracking for To Do tasks
    private final Set<String> checkedTaskIds = new LinkedHashSet<>();

    // Toggle for showing archived tasks
    private boolean showArchived = false;

    // Toolbar actions (need references for enable/disable)
    private AnAction runSelectedAction;
    private AnAction runAllTodoAction;
    private AnAction runSelectedParallelAction;
    private AnAction runAllParallelAction;
    private AnAction cancelRunAction;

    // Execution mode ComboBox
    private JComboBox<String> executionModeCombo;
    private boolean refreshingCombo = false;
    private static final String LLM_PROVIDER_LABEL = "LLM Provider";
    private static final String CLI_PREFIX = "CLI: ";

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
        previewPanel.setOnArchiveAction(() -> {
            TaskSpec spec = previewPanel.getCurrentSpec();
            if (spec != null) archiveSingleTask(spec);
        });
        previewPanel.setOnUnarchiveAction(() -> {
            TaskSpec spec = previewPanel.getCurrentSpec();
            if (spec != null) unarchiveSingleTask(spec);
        });

        // Tree selection listener
        specTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof TaskSpec spec) {
                    // Determine if this task is in the Archived group
                    DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                    boolean isArchived = parentNode != null && ARCHIVED_GROUP.equals(parentNode.getUserObject());
                    previewPanel.showSpec(spec, isArchived);
                    openTaskFile(spec);
                } else {
                    previewPanel.showEmptyState();
                }
            } else {
                previewPanel.showEmptyState();
            }
        });

        // Mouse listener for checkbox toggling on To Do tasks + right-click context menu
        specTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleCheckboxClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showTreeContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showTreeContextMenu(e);
            }
        });

        // Progress panel (shown above splitter during batch runs)
        progressPanel = new SpecTaskRunnerProgressPanel();

        // Statistics panel (collapsible overview at top)
        statisticsPanel = new SpecStatisticsPanel();

        // Layout with splitter — orientation adapts to available width
        splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(new JBScrollPane(specTree));
        splitter.setSecondComponent(previewPanel);

        // Main content: statistics + progress + splitter
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(statisticsPanel);
        topPanel.add(progressPanel);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(topPanel, BorderLayout.NORTH);
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

        actionGroup.addSeparator();

        runSelectedParallelAction = new AnAction(
                "Run Selected (Parallel)",
                "Run checked To Do tasks in parallel — independent tasks execute concurrently",
                DevoxxGenieIconsUtil.RunParallelIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runSelectedTasksParallel();
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
        actionGroup.add(runSelectedParallelAction);

        runAllParallelAction = new AnAction(
                "Run All (Parallel)",
                "Run all To Do tasks in parallel — independent tasks execute concurrently",
                DevoxxGenieIconsUtil.RunParallelIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runAllTodoTasksParallel();
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
        actionGroup.add(runAllParallelAction);

        actionGroup.addSeparator();

        // Archive actions
        actionGroup.add(new AnAction("Archive Done", "Archive all completed tasks", AllIcons.Actions.MoveToBottomRight) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                archiveAllDoneTasks();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                SpecService specService = SpecService.getInstance(project);
                long doneCount = specService.getAllSpecs().stream()
                        .filter(s -> "Done".equalsIgnoreCase(s.getStatus()))
                        .count();
                e.getPresentation().setEnabled(doneCount > 0);
                e.getPresentation().setText(doneCount > 0 ? "Archive Done (" + doneCount + ")" : "Archive Done");
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        actionGroup.add(new ToggleAction("Show Archived", "Toggle visibility of archived tasks", AllIcons.Actions.ShowAsTree) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return showArchived;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                showArchived = state;
                refreshTree();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        // Security scan action
        actionGroup.add(new AnAction("Security Scan", "Run gitleaks, opengrep, trivy security scanners",
                AllIcons.General.InspectionsEye) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runSecurityScan();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                boolean enabled = Boolean.TRUE.equals(state.getSecurityScanEnabled());
                e.getPresentation().setVisible(enabled);
                if (enabled) {
                    com.devoxx.genie.service.security.SecurityScannerService scannerService =
                            com.devoxx.genie.service.security.SecurityScannerService.getInstance(project);
                    e.getPresentation().setEnabled(!scannerService.isRunning());
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });

        actionGroup.addSeparator();

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

            // Add CLI tools
            List<CliToolConfig> cliTools = stateService.getCliTools();
            if (cliTools != null) {
                for (CliToolConfig tool : cliTools) {
                    if (tool.isEnabled()) {
                        executionModeCombo.addItem(CLI_PREFIX + tool.getName());
                    }
                }
            }

            // Restore selection
            String mode = stateService.getSpecRunnerMode();
            if ("cli".equalsIgnoreCase(mode)) {
                String selectedTool = stateService.getSpecSelectedCliTool();
                String label = CLI_PREFIX + selectedTool;
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
        } else if (selected.startsWith(CLI_PREFIX)) {
            stateService.setSpecRunnerMode("cli");
            stateService.setSpecSelectedCliTool(selected.substring(CLI_PREFIX.length()));
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
                if (taskNode.getUserObject() instanceof TaskSpec spec &&
                    spec.getId() != null && checkedTaskIds.contains(spec.getId())) {
                    tasks.add(spec);
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
                        CANNOT_RUN_TASKS + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void runSecurityScan() {
        com.devoxx.genie.service.security.SecurityScannerService scannerService =
                com.devoxx.genie.service.security.SecurityScannerService.getInstance(project);

        scannerService.runScan(new com.devoxx.genie.service.security.SecurityScanListener() {
            @Override
            public void onScanStarted() {
                progressPanel.update("Starting security scan...", 0, 3);
            }

            @Override
            public void onScannerStarted(String name, int index, int total) {
                progressPanel.update("Scanning with " + name + "...", index, total);
            }

            @Override
            public void onScannerCompleted(String name, int findingsCount) {
                log.info("{} completed with {} findings", name, findingsCount);
            }

            @Override
            public void onScannerSkipped(String name, String reason) {
                log.info("{} skipped: {}", name, reason);
            }

            @Override
            public void onTasksCreated(int count) {
                if (count > 0) {
                    refreshSpecs();
                }
            }

            @Override
            public void onScanCompleted(com.devoxx.genie.model.security.SecurityScanResult result) {
                int total = result.getFindings().size();
                String msg = "Security scan complete: " + total + " finding(s)";
                if (!result.getErrors().isEmpty()) {
                    msg += ", " + result.getErrors().size() + " error(s)";
                }
                progressPanel.showCompleted(total, result.getErrors().size(), total + result.getErrors().size());
                NotificationUtil.sendNotification(project, msg);
            }

            @Override
            public void onScanFailed(String error) {
                progressPanel.hidePanel();
                NotificationUtil.sendErrorNotification(project, "Security scan failed: " + error);
            }
        }, null);
    }

    private void runAllTodoTasks() {
        try {
            SpecTaskRunnerService.getInstance(project).runAllTodoTasks();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CircularDependencyException) {
                NotificationUtil.sendErrorNotification(project,
                        CANNOT_RUN_TASKS + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void runSelectedTasksParallel() {
        List<TaskSpec> tasks = getCheckedTasks();
        if (tasks.isEmpty()) {
            NotificationUtil.sendWarningNotification(project,
                    "No tasks selected. Check the boxes next to To Do tasks first.");
            return;
        }
        try {
            SpecTaskRunnerService.getInstance(project).runTasks(tasks, ExecutionMode.PARALLEL);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CircularDependencyException) {
                NotificationUtil.sendErrorNotification(project,
                        CANNOT_RUN_TASKS + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void runAllTodoTasksParallel() {
        try {
            SpecTaskRunnerService.getInstance(project).runAllTodoTasksParallel();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CircularDependencyException) {
                NotificationUtil.sendErrorNotification(project,
                        CANNOT_RUN_TASKS + e.getMessage());
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

    // ===== Archive Actions =====

    private void archiveAllDoneTasks() {
        SpecService specService = SpecService.getInstance(project);
        long doneCount = specService.getAllSpecs().stream()
                .filter(s -> "Done".equalsIgnoreCase(s.getStatus()))
                .count();

        if (doneCount == 0) {
            NotificationUtil.sendWarningNotification(project, "No completed tasks to archive.");
            return;
        }

        int result = Messages.showYesNoDialog(
                project,
                "Archive " + doneCount + " completed task(s)?\n\nArchived tasks are moved to the archive/tasks/ directory and hidden from the main view.",
                "Archive Done Tasks",
                "Archive",
                "Cancel",
                Messages.getQuestionIcon());

        if (result != Messages.YES) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                int archived = specService.archiveDoneTasks();
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendNotification(project, archived + " task(s) archived successfully."));
            } catch (IOException ex) {
                log.warn("Failed to archive done tasks", ex);
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendErrorNotification(project, "Failed to archive tasks: " + ex.getMessage()));
            }
        });
    }

    private void archiveSingleTask(@NotNull TaskSpec spec) {
        int result = Messages.showYesNoDialog(
                project,
                "Archive task " + spec.getDisplayLabel() + "?",
                "Archive Task",
                "Archive",
                "Cancel",
                Messages.getQuestionIcon());

        if (result != Messages.YES) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SpecService.getInstance(project).archiveTask(spec.getId());
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendNotification(project, "Task " + spec.getId() + " archived."));
            } catch (IOException ex) {
                log.warn("Failed to archive task: {}", spec.getId(), ex);
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendErrorNotification(project, "Failed to archive task: " + ex.getMessage()));
            }
        });
    }

    private void unarchiveSingleTask(@NotNull TaskSpec spec) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SpecService.getInstance(project).unarchiveTask(spec.getId());
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendNotification(project, "Task " + spec.getId() + " restored from archive."));
            } catch (IOException ex) {
                log.warn("Failed to unarchive task: {}", spec.getId(), ex);
                ApplicationManager.getApplication().invokeLater(() ->
                        NotificationUtil.sendErrorNotification(project, "Failed to unarchive task: " + ex.getMessage()));
            }
        });
    }

    private void showTreeContextMenu(MouseEvent e) {
        TreePath path = specTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!(node.getUserObject() instanceof TaskSpec spec)) return;

        // Select the node under the cursor
        specTree.setSelectionPath(path);

        JPopupMenu popup = new JPopupMenu();

        // Check if this is an archived task (parent node text == "Archived")
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        boolean isArchived = parentNode != null && ARCHIVED_GROUP.equals(parentNode.getUserObject());

        if (isArchived) {
            JMenuItem unarchiveItem = new JMenuItem("Restore from Archive");
            unarchiveItem.setIcon(AllIcons.Actions.Undo);
            unarchiveItem.addActionListener(a -> unarchiveSingleTask(spec));
            popup.add(unarchiveItem);
        } else {
            JMenuItem archiveItem = new JMenuItem("Archive");
            archiveItem.setIcon(AllIcons.Actions.MoveToBottomRight);
            archiveItem.addActionListener(a -> archiveSingleTask(spec));
            popup.add(archiveItem);
        }

        popup.show(specTree, e.getX(), e.getY());
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

        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> specs = specService.getAllSpecs();
        List<TaskSpec> archivedTasks = showArchived ? specService.getArchivedTasks() : List.of();

        // Update statistics panel with current task data and archived count
        statisticsPanel.update(specs, archivedTasks.size());

        // Remove checked IDs that no longer exist or are no longer To Do
        retainValidCheckedIds(specs);

        // Group by status and build tree nodes
        Map<String, List<TaskSpec>> grouped = groupByStatus(specs);
        buildStatusTreeNodes(grouped);

        // Add archived tasks group when toggled on
        if (showArchived && !archivedTasks.isEmpty()) {
            buildSortedTreeNode(ARCHIVED_GROUP, archivedTasks);
        }

        treeModel.reload();

        // Expand all status groups
        for (int i = 0; i < specTree.getRowCount(); i++) {
            specTree.expandRow(i);
        }
    }

    private void retainValidCheckedIds(List<TaskSpec> specs) {
        Set<String> validIds = new HashSet<>();
        for (TaskSpec spec : specs) {
            if (spec.getId() != null && "To Do".equalsIgnoreCase(spec.getStatus())) {
                validIds.add(spec.getId());
            }
        }
        checkedTaskIds.retainAll(validIds);
    }

    private Map<String, List<TaskSpec>> groupByStatus(List<TaskSpec> specs) {
        Map<String, List<TaskSpec>> grouped = new LinkedHashMap<>();
        for (String status : STATUS_ORDER) {
            grouped.put(status, new ArrayList<>());
        }
        for (TaskSpec spec : specs) {
            String status = spec.getStatus() != null ? spec.getStatus() : "To Do";
            grouped.computeIfAbsent(status, k -> new ArrayList<>()).add(spec);
        }
        return grouped;
    }

    private void buildStatusTreeNodes(Map<String, List<TaskSpec>> grouped) {
        for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                buildSortedTreeNode(entry.getKey(), entry.getValue());
            }
        }
    }

    private void buildSortedTreeNode(String groupName, List<TaskSpec> specs) {
        specs.sort((a, b) -> Integer.compare(extractTaskNumber(a.getId()), extractTaskNumber(b.getId())));
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(groupName);
        for (TaskSpec spec : specs) {
            node.add(new DefaultMutableTreeNode(spec));
        }
        rootNode.add(node);
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
