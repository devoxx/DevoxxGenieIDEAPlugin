package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.trace.view.AgentRun;
import com.devoxx.genie.ui.trace.TraceExportActions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Enumeration;

/**
 * The agent run view (TASK-283): a summary header, a tree of turns, steps, tool calls and
 * sub-agents on the left (single-line rows), and the selected row's full text on the right.
 * Reasoning, arguments and long results only appear in the detail pane, so the tree stays compact.
 */
final class AgentRunPanel extends JPanel {

    private static final SimpleTextAttributes WARNING_ATTRIBUTES = new SimpleTextAttributes(
            SimpleTextAttributes.STYLE_PLAIN, new JBColor(new Color(0xB2, 0x6A, 0x00), new Color(0xFF, 0xB4, 0x50)));

    private final Tree tree;
    private final JBTextArea detail = new JBTextArea();

    AgentRunPanel(@NotNull Project project, @NotNull Conversation conversation, @NotNull AgentRun run) {
        super(new BorderLayout());

        SimpleColoredComponent summary = new SimpleColoredComponent();
        summary.append(AgentRunText.summary(run.summary()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        ActionLink rawJsonl = new ActionLink("Raw JSONL",
                (ActionListener) e -> TraceExportActions.preview(project, conversation));
        rawJsonl.setToolTipText("Open this trace as Session Trace JSONL, exactly as it would be exported");
        JPanel header = new JPanel(new BorderLayout(JBUI.scale(12), 0));
        header.setBorder(JBUI.Borders.compound(JBUI.Borders.customLineBottom(JBColor.border()),
                JBUI.Borders.empty(6, 10)));
        header.add(summary, BorderLayout.CENTER);
        header.add(rawJsonl, BorderLayout.EAST);

        DefaultMutableTreeNode root = AgentRunTreeBuilder.build(run);
        tree = new Tree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new RowRenderer());
        tree.addTreeSelectionListener(e -> showDetail(e.getNewLeadSelectionPath()));

        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(JBFont.create(new Font(Font.MONOSPACED, Font.PLAIN, JBFont.label().getSize())));
        detail.setBorder(JBUI.Borders.empty(8));
        detail.setText(run.turns().isEmpty() ? "No agent trace was recorded for this conversation."
                : "Select a step or tool call to see its full text.");

        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.45f);
        splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(tree, true));
        splitter.setSecondComponent(new JBScrollPane(detail));

        add(header, BorderLayout.NORTH);
        add(splitter, BorderLayout.CENTER);

        expandDefaults(root);
        if (tree.getRowCount() > 1) {
            tree.setSelectionRow(1); // The first step of the first turn.
        }
    }

    @NotNull JComponent focusComponent() {
        return tree;
    }

    private void showDetail(TreePath path) {
        if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) {
            return;
        }
        detail.setText(AgentRunText.detail(node.getUserObject()));
        detail.setCaretPosition(0);
    }

    private void expandDefaults(DefaultMutableTreeNode root) {
        Enumeration<?> nodes = root.breadthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node != root && AgentRunTreeBuilder.expandedByDefault(node)) {
                tree.expandPath(new TreePath(node.getPath()));
            }
        }
    }

    /** Single-line rows, appended as plain text (no HTML) so trace content can't be interpreted as markup. */
    private static final class RowRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded,
                                          boolean leaf, int row, boolean hasFocus) {
            if (!(value instanceof DefaultMutableTreeNode node)) {
                return;
            }
            Object item = node.getUserObject();
            AgentRunText.Row text = AgentRunText.row(item);
            setIcon(iconFor(item, text.style()));
            SimpleTextAttributes main = switch (text.style()) {
                case ERROR -> SimpleTextAttributes.ERROR_ATTRIBUTES;
                case WARNING -> WARNING_ATTRIBUTES;
                case MUTED -> SimpleTextAttributes.GRAYED_ATTRIBUTES;
                case NORMAL -> item instanceof AgentRun.Turn
                        ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
            };
            append(text.text(), main);
            if (!text.suffix().isEmpty()) {
                append("   " + text.suffix(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }

        private static Icon iconFor(Object item, AgentRunText.Style style) {
            if (item instanceof AgentRun.Turn) return AllIcons.Actions.Execute;
            if (item instanceof AgentRun.Step) return AllIcons.General.Information;
            if (item instanceof AgentRun.SubAgentRun) return AllIcons.Actions.ShowAsTree;
            return switch (style) {
                case ERROR -> AllIcons.General.Error;
                case WARNING -> AllIcons.General.Warning;
                default -> AllIcons.General.InspectionsOK;
            };
        }
    }
}
