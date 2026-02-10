package com.devoxx.genie.model.spec;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed task spec file compatible with Backlog.md format.
 * Task files use YAML frontmatter for metadata and markdown body for description.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSpec {
    private String id;
    private String title;
    @Builder.Default
    private String status = "To Do";
    @Builder.Default
    private String priority = "medium";
    @Builder.Default
    private List<String> assignees = new ArrayList<>();
    @Builder.Default
    private List<String> labels = new ArrayList<>();
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();
    @Builder.Default
    private List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<>();
    @Builder.Default
    private List<DefinitionOfDoneItem> definitionOfDone = new ArrayList<>();
    private String description;
    private String filePath;
    private transient VirtualFile virtualFile;
    private long lastModified;

    // New fields for full Backlog.md compatibility
    private String milestone;
    private String createdAt;
    private String updatedAt;
    private String parentTaskId;
    @Builder.Default
    private List<String> references = new ArrayList<>();
    @Builder.Default
    private List<String> documentation = new ArrayList<>();
    private String implementationPlan;
    private String implementationNotes;
    private String finalSummary;
    @Builder.Default
    private int ordinal = 1000;

    /**
     * Returns the number of checked acceptance criteria.
     */
    public long getCheckedAcceptanceCriteriaCount() {
        return acceptanceCriteria.stream().filter(AcceptanceCriterion::isChecked).count();
    }

    /**
     * Returns the number of checked definition-of-done items.
     */
    public long getCheckedDefinitionOfDoneCount() {
        return definitionOfDone.stream().filter(DefinitionOfDoneItem::isChecked).count();
    }

    /**
     * Returns a short display label: "ID: Title".
     */
    public String getDisplayLabel() {
        if (id != null && title != null) {
            return id + ": " + title;
        }
        return title != null ? title : (id != null ? id : "Untitled");
    }
}
