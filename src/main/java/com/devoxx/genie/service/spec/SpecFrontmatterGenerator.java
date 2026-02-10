package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Generates Backlog.md-compatible markdown files from model objects.
 * Produces YAML frontmatter + markdown body with structured sections.
 */
public final class SpecFrontmatterGenerator {

    private SpecFrontmatterGenerator() {
    }

    /**
     * Generate the full markdown file content for a TaskSpec.
     */
    public static @NotNull String generate(@NotNull TaskSpec spec) {
        StringBuilder sb = new StringBuilder();

        // YAML frontmatter
        sb.append("---\n");
        appendScalar(sb, "id", spec.getId());
        appendScalar(sb, "title", spec.getTitle());
        appendScalar(sb, "status", spec.getStatus());
        appendScalar(sb, "priority", spec.getPriority());
        appendScalar(sb, "milestone", spec.getMilestone());
        appendScalar(sb, "parent_task_id", spec.getParentTaskId());
        appendList(sb, "assignee", spec.getAssignees());
        appendScalarQuoted(sb, "created_date", spec.getCreatedAt());
        appendScalarQuoted(sb, "updated_date", spec.getUpdatedAt());
        appendList(sb, "labels", spec.getLabels());
        appendList(sb, "dependencies", spec.getDependencies());
        appendList(sb, "references", spec.getReferences());
        appendList(sb, "documentation", spec.getDocumentation());
        sb.append("ordinal: ").append(spec.getOrdinal()).append("\n");
        sb.append("---\n\n");

        // Description
        if (spec.getDescription() != null && !spec.getDescription().isEmpty()) {
            sb.append(spec.getDescription()).append("\n\n");
        }

        // Acceptance Criteria
        if (spec.getAcceptanceCriteria() != null && !spec.getAcceptanceCriteria().isEmpty()) {
            sb.append("## Acceptance Criteria\n\n");
            for (AcceptanceCriterion ac : spec.getAcceptanceCriteria()) {
                sb.append("- [").append(ac.isChecked() ? "x" : " ").append("] ");
                sb.append(ac.getText()).append("\n");
            }
            sb.append("\n");
        }

        // Definition of Done
        if (spec.getDefinitionOfDone() != null && !spec.getDefinitionOfDone().isEmpty()) {
            sb.append("## Definition of Done\n\n");
            for (DefinitionOfDoneItem item : spec.getDefinitionOfDone()) {
                sb.append("- [").append(item.isChecked() ? "x" : " ").append("] ");
                sb.append(item.getText()).append("\n");
            }
            sb.append("\n");
        }

        // Implementation Plan
        if (spec.getImplementationPlan() != null && !spec.getImplementationPlan().isEmpty()) {
            sb.append("## Implementation Plan\n\n");
            sb.append(spec.getImplementationPlan()).append("\n\n");
        }

        // Implementation Notes
        if (spec.getImplementationNotes() != null && !spec.getImplementationNotes().isEmpty()) {
            sb.append("## Implementation Notes\n\n");
            sb.append(spec.getImplementationNotes()).append("\n\n");
        }

        // Final Summary
        if (spec.getFinalSummary() != null && !spec.getFinalSummary().isEmpty()) {
            sb.append("## Final Summary\n\n");
            sb.append(spec.getFinalSummary()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Generate markdown content for a BacklogDocument.
     */
    public static @NotNull String generateDocument(@NotNull BacklogDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        appendScalar(sb, "id", doc.getId());
        appendScalar(sb, "title", doc.getTitle());
        sb.append("---\n\n");

        if (doc.getContent() != null && !doc.getContent().isEmpty()) {
            sb.append(doc.getContent()).append("\n");
        }

        return sb.toString();
    }

    private static void appendScalar(@NotNull StringBuilder sb, @NotNull String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(key).append(": ").append(quoteIfNeeded(value)).append("\n");
        }
    }

    /**
     * Appends a scalar value always wrapped in single quotes (used for dates).
     */
    private static void appendScalarQuoted(@NotNull StringBuilder sb, @NotNull String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(key).append(": '").append(value).append("'\n");
        }
    }

    /**
     * Appends a list field. Empty or null lists are output as "key: []".
     */
    private static void appendList(@NotNull StringBuilder sb, @NotNull String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            sb.append(key).append(": []\n");
        } else {
            sb.append(key).append(":\n");
            for (String value : values) {
                sb.append("  - ").append(quoteIfNeeded(value)).append("\n");
            }
        }
    }

    private static @NotNull String quoteIfNeeded(@NotNull String value) {
        // Quote values that contain special YAML characters
        if (value.contains(":") || value.contains("#") || value.contains("'")
                || value.contains("\"") || value.contains("\n")
                || value.startsWith("{") || value.startsWith("[")
                || value.startsWith("*") || value.startsWith("&")) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
