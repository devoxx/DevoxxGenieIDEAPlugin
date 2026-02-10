package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecContextBuilder;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * Main panel for the Spec Browser tool window.
 * Shows tasks grouped by status in a tree, with a preview panel below.
 */
public class SpecBrowserPanel extends SimpleToolWindowPanel {

    // Preferred status ordering (others appended at end)
    private static final List<String> STATUS_ORDER = List.of("To Do", "In Progress", "Done");

    private final Project project;
    private final Tree specTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final SpecPreviewPanel previewPanel;

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
                } else {
                    previewPanel.showEmptyState();
                }
            } else {
                previewPanel.showEmptyState();
            }
        });

        // Layout with splitter
        JBSplitter splitter = new JBSplitter(true, 0.6f);
        splitter.setFirstComponent(new JBScrollPane(specTree));
        splitter.setSecondComponent(previewPanel);
        setContent(splitter);

        // Toolbar
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new AnAction("Refresh", "Refresh specs from disk", null) {
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

        // Build tree nodes
        for (Map.Entry<String, List<TaskSpec>> entry : grouped.entrySet()) {
            List<TaskSpec> statusSpecs = entry.getValue();
            if (statusSpecs.isEmpty()) {
                continue;
            }

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

    private void implementCurrentSpec() {
        TaskSpec spec = previewPanel.getCurrentSpec();
        if (spec == null) {
            return;
        }

        // Build a prompt that includes the spec context and an implementation instruction
        String specContext = SpecContextBuilder.buildAgentInstruction(spec);
        String prompt = specContext + "\n\nPlease implement the task described above, satisfying all acceptance criteria.";

        // Submit via the prompt submission topic
        project.getMessageBus()
                .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                .onPromptSubmitted(project, prompt);
    }
}
