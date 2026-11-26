package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.service.trace.view.AgentRun;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

/**
 * Builds the agent run view's tree (TASK-283): turns, then each turn's steps, then each step's
 * tool calls, with the sub-agent runs of a {@code parallel_explore} call (and their own steps and
 * calls) under that call. Each node's user object is the {@link AgentRun} record it shows.
 */
final class AgentRunTreeBuilder {

    private AgentRunTreeBuilder() {
    }

    /** The (hidden) root; its children are the turns. */
    static @NotNull DefaultMutableTreeNode build(@NotNull AgentRun run) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(run);
        for (AgentRun.Turn turn : run.turns()) {
            DefaultMutableTreeNode turnNode = new DefaultMutableTreeNode(turn);
            addSteps(turnNode, turn.steps());
            for (AgentRun.SubAgentRun sub : turn.unattachedSubAgents()) {
                turnNode.add(subAgentNode(sub));
            }
            root.add(turnNode);
        }
        return root;
    }

    private static void addSteps(DefaultMutableTreeNode parent, List<AgentRun.Step> steps) {
        for (AgentRun.Step step : steps) {
            DefaultMutableTreeNode stepNode = new DefaultMutableTreeNode(step);
            for (AgentRun.ToolCallNode call : step.toolCalls()) {
                DefaultMutableTreeNode callNode = new DefaultMutableTreeNode(call);
                for (AgentRun.SubAgentRun sub : call.subAgents()) {
                    callNode.add(subAgentNode(sub));
                }
                stepNode.add(callNode);
            }
            parent.add(stepNode);
        }
    }

    private static DefaultMutableTreeNode subAgentNode(AgentRun.SubAgentRun sub) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(sub);
        addSteps(node, sub.steps());
        return node;
    }

    /** Whether a node starts expanded: turns and the main agent's steps; sub-agents start collapsed. */
    static boolean expandedByDefault(@NotNull DefaultMutableTreeNode node) {
        Object value = node.getUserObject();
        if (value instanceof AgentRun.Turn) {
            return true;
        }
        if (value instanceof AgentRun.Step) {
            for (Object ancestor : node.getUserObjectPath()) {
                if (ancestor instanceof AgentRun.SubAgentRun) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
