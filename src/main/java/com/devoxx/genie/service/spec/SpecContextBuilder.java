package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
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
        if (spec.getMilestone() != null && !spec.getMilestone().isEmpty()) {
            sb.append("Milestone: ").append(spec.getMilestone()).append("\n");
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
            for (DefinitionOfDoneItem item : spec.getDefinitionOfDone()) {
                sb.append("- [").append(item.isChecked() ? "x" : " ").append("] ");
                sb.append(item.getText()).append("\n");
            }
        }

        // Dependencies
        if (spec.getDependencies() != null && !spec.getDependencies().isEmpty()) {
            sb.append("\n## Dependencies\n");
            for (String dep : spec.getDependencies()) {
                sb.append("- ").append(dep).append("\n");
            }
        }

        // References
        if (spec.getReferences() != null && !spec.getReferences().isEmpty()) {
            sb.append("\n## References\n");
            for (String ref : spec.getReferences()) {
                sb.append("- ").append(ref).append("\n");
            }
        }

        // Documentation
        if (spec.getDocumentation() != null && !spec.getDocumentation().isEmpty()) {
            sb.append("\n## Documentation\n");
            for (String doc : spec.getDocumentation()) {
                sb.append("- ").append(doc).append("\n");
            }
        }

        // Implementation Plan
        if (spec.getImplementationPlan() != null && !spec.getImplementationPlan().isEmpty()) {
            sb.append("\n## Implementation Plan\n");
            sb.append(spec.getImplementationPlan()).append("\n");
        }

        // Implementation Notes
        if (spec.getImplementationNotes() != null && !spec.getImplementationNotes().isEmpty()) {
            sb.append("\n## Implementation Notes\n");
            sb.append(spec.getImplementationNotes()).append("\n");
        }

        // Final Summary
        if (spec.getFinalSummary() != null && !spec.getFinalSummary().isEmpty()) {
            sb.append("\n## Final Summary\n");
            sb.append(spec.getFinalSummary()).append("\n");
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
        sb.append(".\n\n");
        sb.append("Follow the acceptance criteria exactly.\n\n");
        sb.append("As you work, use the backlog tools to keep the task updated:\n");
        sb.append("1. Use backlog_task_edit to set status to 'In Progress' when you start.\n");
        sb.append("2. Use backlog_task_edit with acceptanceCriteriaCheck to check off each criterion as you complete it.\n");
        sb.append("3. Use backlog_task_edit with notesAppend to record what you changed and why.\n");
        sb.append("4. When finished, use backlog_task_edit to write a finalSummary describing what was done.\n");
        sb.append("5. Use backlog_task_complete to mark the task as Done.\n");
        return sb.toString();
    }
}
