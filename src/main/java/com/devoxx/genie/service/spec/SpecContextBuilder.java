package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import org.jetbrains.annotations.NotNull;

/**
 * Builds structured context sections from a TaskSpec for injection into LLM prompts.
 */
public final class SpecContextBuilder {

    private SpecContextBuilder() {
    }

    /**
     * Build the full {@code <TaskSpec>} context block for a task.
     */
    public static @NotNull String buildContext(@NotNull TaskSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("<TaskSpec>\n");

        if (spec.getId() != null) {
            sb.append("Task ID: ").append(spec.getId()).append("\n");
        }
        if (spec.getTitle() != null) {
            sb.append("Title: ").append(spec.getTitle()).append("\n");
        }
        if (spec.getStatus() != null) {
            sb.append("Status: ").append(spec.getStatus()).append("\n");
        }
        if (spec.getPriority() != null) {
            sb.append("Priority: ").append(spec.getPriority()).append("\n");
        }
        if (spec.getAssignees() != null && !spec.getAssignees().isEmpty()) {
            sb.append("Assignees: ").append(String.join(", ", spec.getAssignees())).append("\n");
        }
        if (spec.getLabels() != null && !spec.getLabels().isEmpty()) {
            sb.append("Labels: ").append(String.join(", ", spec.getLabels())).append("\n");
        }

        // Description
        if (spec.getDescription() != null && !spec.getDescription().isEmpty()) {
            sb.append("\n## Description\n");
            sb.append(spec.getDescription()).append("\n");
        }

        // Acceptance Criteria
        if (spec.getAcceptanceCriteria() != null && !spec.getAcceptanceCriteria().isEmpty()) {
            sb.append("\n## Acceptance Criteria\n");
            for (AcceptanceCriterion ac : spec.getAcceptanceCriteria()) {
                sb.append("- [").append(ac.isChecked() ? "x" : " ").append("] ");
                sb.append(ac.getText()).append("\n");
            }
        }

        // Definition of Done
        if (spec.getDefinitionOfDone() != null && !spec.getDefinitionOfDone().isEmpty()) {
            sb.append("\n## Definition of Done\n");
            for (String item : spec.getDefinitionOfDone()) {
                sb.append("- [ ] ").append(item).append("\n");
            }
        }

        // Dependencies
        if (spec.getDependencies() != null && !spec.getDependencies().isEmpty()) {
            sb.append("\n## Dependencies\n");
            for (String dep : spec.getDependencies()) {
                sb.append("- ").append(dep).append("\n");
            }
        }

        sb.append("</TaskSpec>");
        return sb.toString();
    }

    /**
     * Build an agent instruction prefix for working on a specific task.
     */
    public static @NotNull String buildAgentInstruction(@NotNull TaskSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are implementing task ");
        if (spec.getId() != null) {
            sb.append(spec.getId());
        }
        if (spec.getTitle() != null) {
            sb.append(": ").append(spec.getTitle());
        }
        sb.append(".\n");
        sb.append("Follow the acceptance criteria exactly. ");
        sb.append("When done, summarize which criteria are met and which need manual verification.");
        return sb.toString();
    }
}
