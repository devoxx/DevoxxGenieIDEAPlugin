package com.devoxx.genie.model.spec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the backlog model classes: DefinitionOfDoneItem, BacklogDocument, BacklogConfig.
 */
class BacklogModelTest {

    @Test
    void definitionOfDoneItemShouldBuildCorrectly() {
        DefinitionOfDoneItem item = DefinitionOfDoneItem.builder()
                .index(0)
                .text("Tests written")
                .checked(true)
                .build();

        assertThat(item.getIndex()).isEqualTo(0);
        assertThat(item.getText()).isEqualTo("Tests written");
        assertThat(item.isChecked()).isTrue();
    }

    @Test
    void backlogDocumentShouldBuildCorrectly() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-1")
                .title("Architecture")
                .content("# Overview\nSome content")
                .filePath("/path/to/doc.md")
                .lastModified(1234567890L)
                .build();

        assertThat(doc.getId()).isEqualTo("DOC-1");
        assertThat(doc.getTitle()).isEqualTo("Architecture");
        assertThat(doc.getContent()).contains("Overview");
        assertThat(doc.getFilePath()).isEqualTo("/path/to/doc.md");
        assertThat(doc.getLastModified()).isEqualTo(1234567890L);
    }

    @Test
    void taskSpecShouldSupportNewFields() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Test task")
                .milestone("v1.0")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-15")
                .parentTaskId("TASK-0")
                .references(List.of("ref1", "ref2"))
                .documentation(List.of("doc1"))
                .implementationPlan("Step 1: Do thing")
                .implementationNotes("Note: Important detail")
                .finalSummary("Done.")
                .definitionOfDone(List.of(
                        DefinitionOfDoneItem.builder().index(0).text("Tests pass").checked(true).build(),
                        DefinitionOfDoneItem.builder().index(1).text("Reviewed").checked(false).build()
                ))
                .build();

        assertThat(spec.getMilestone()).isEqualTo("v1.0");
        assertThat(spec.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(spec.getUpdatedAt()).isEqualTo("2024-01-15");
        assertThat(spec.getParentTaskId()).isEqualTo("TASK-0");
        assertThat(spec.getReferences()).containsExactly("ref1", "ref2");
        assertThat(spec.getDocumentation()).containsExactly("doc1");
        assertThat(spec.getImplementationPlan()).isEqualTo("Step 1: Do thing");
        assertThat(spec.getImplementationNotes()).isEqualTo("Note: Important detail");
        assertThat(spec.getFinalSummary()).isEqualTo("Done.");
        assertThat(spec.getCheckedDefinitionOfDoneCount()).isEqualTo(1);
    }

    @Test
    void backlogConfigShouldHaveDefaults() {
        BacklogConfig config = BacklogConfig.builder().build();

        assertThat(config.getDefaultStatus()).isEqualTo("To Do");
        assertThat(config.getTaskPrefix()).isEqualTo("task");
        assertThat(config.getStatuses()).contains("To Do", "In Progress", "Done");
    }

    @Test
    void backlogMilestoneShouldBuildCorrectly() {
        BacklogConfig.BacklogMilestone milestone = BacklogConfig.BacklogMilestone.builder()
                .name("v2.0")
                .description("Major release")
                .build();

        assertThat(milestone.getName()).isEqualTo("v2.0");
        assertThat(milestone.getDescription()).isEqualTo("Major release");
    }
}
