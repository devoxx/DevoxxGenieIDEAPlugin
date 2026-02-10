package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.topic.AppTopics;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;

/**
 * Main panel for the Spec Browser tool window.
 * Shows tasks grouped by status in a tree, with a preview panel below.
 */
public class SpecBrowserPanel extends SimpleToolWindowPanel {

    // Preferred status ordering (others appended at end)
    private static final List<String> STATUS_ORDER = List.of("To Do", "In Progress", "Done");

    private static final int HORIZONTAL_LAYOUT_MIN_WIDTH = 600;

    private final Project project;
    private final Tree specTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final SpecPreviewPanel previewPanel;
    private final JBSplitter splitter;

    public SpecBrowserPanel(@NotNull Project project) {
        super(true, true);
        this.project = project;

        // Tree setup
        rootNode = new DefaultMutableTreeNode("Specs");
        treeModel = new DefaultTreeModel(rootNode);
        specTree = new Tree(treeModel);
        specTree.setRootVisible(false);
        specTree.setShowsRootHandles(true);
        specTree.setCellRenderer(new SpecTreeCellRenderer());

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

        // Layout with splitter — orientation adapts to available width
        splitter = new JBSplitter(true, 0.5f);
        splitter.setFirstComponent(new JBScrollPane(specTree));
        splitter.setSecondComponent(previewPanel);
        setContent(splitter);

        // Switch between vertical (narrow/sidebar) and horizontal (wide/bottom) layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSplitterOrientation();
            }
        });

        // Toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction("Refresh", "Refresh specs from disk", com.intellij.icons.AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshSpecs();
            }
        });
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("SpecBrowser", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());

        // Listen for spec changes
        SpecService specService = SpecService.getInstance(project);
        specService.addChangeListener(() -> ApplicationManager.getApplication().invokeLater(this::refreshTree));

        // Initial load
        refreshTree();
    }

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
