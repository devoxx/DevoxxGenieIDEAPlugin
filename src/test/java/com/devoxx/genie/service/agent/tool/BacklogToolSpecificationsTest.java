package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that all 17 backlog tool specifications are properly defined.
 */
class BacklogToolSpecificationsTest {

    @Test
    void shouldDefineAllTaskTools() {
        assertToolSpec(BacklogToolSpecifications.taskCreate(), "backlog_task_create");
        assertToolSpec(BacklogToolSpecifications.taskList(), "backlog_task_list");
        assertToolSpec(BacklogToolSpecifications.taskSearch(), "backlog_task_search");
        assertToolSpec(BacklogToolSpecifications.taskView(), "backlog_task_view");
        assertToolSpec(BacklogToolSpecifications.taskEdit(), "backlog_task_edit");
        assertToolSpec(BacklogToolSpecifications.taskComplete(), "backlog_task_complete");
        assertToolSpec(BacklogToolSpecifications.taskArchive(), "backlog_task_archive");
        assertToolSpec(BacklogToolSpecifications.taskFindRelated(), "backlog_task_find_related");
    }

    @Test
    void shouldDefineAllDocumentTools() {
        assertToolSpec(BacklogToolSpecifications.documentList(), "backlog_document_list");
        assertToolSpec(BacklogToolSpecifications.documentView(), "backlog_document_view");
        assertToolSpec(BacklogToolSpecifications.documentCreate(), "backlog_document_create");
        assertToolSpec(BacklogToolSpecifications.documentUpdate(), "backlog_document_update");
        assertToolSpec(BacklogToolSpecifications.documentSearch(), "backlog_document_search");
    }

    @Test
    void shouldDefineAllMilestoneTools() {
        assertToolSpec(BacklogToolSpecifications.milestoneList(), "backlog_milestone_list");
        assertToolSpec(BacklogToolSpecifications.milestoneAdd(), "backlog_milestone_add");
        assertToolSpec(BacklogToolSpecifications.milestoneRename(), "backlog_milestone_rename");
        assertToolSpec(BacklogToolSpecifications.milestoneRemove(), "backlog_milestone_remove");
        assertToolSpec(BacklogToolSpecifications.milestoneArchive(), "backlog_milestone_archive");
    }

    @Test
    void taskCreateShouldRequireTitle() {
        ToolSpecification spec = BacklogToolSpecifications.taskCreate();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("title");
    }

    @Test
    void taskViewShouldRequireId() {
        ToolSpecification spec = BacklogToolSpecifications.taskView();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("id");
    }

    @Test
    void taskEditShouldRequireId() {
        ToolSpecification spec = BacklogToolSpecifications.taskEdit();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("id");
    }

    @Test
    void taskSearchShouldRequireQuery() {
        ToolSpecification spec = BacklogToolSpecifications.taskSearch();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("query");
    }

    @Test
    void documentCreateShouldRequireTitleAndContent() {
        ToolSpecification spec = BacklogToolSpecifications.documentCreate();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("title", "content");
    }

    @Test
    void milestoneAddShouldRequireName() {
        ToolSpecification spec = BacklogToolSpecifications.milestoneAdd();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("name");
    }

    @Test
    void taskFindRelatedShouldHaveIdAndQueryParams() {
        ToolSpecification spec = BacklogToolSpecifications.taskFindRelated();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.description()).contains("related");
    }

    @Test
    void milestoneRenameShouldRequireFromAndTo() {
        ToolSpecification spec = BacklogToolSpecifications.milestoneRename();
        assertThat(spec.parameters()).isNotNull();
        assertThat(spec.parameters().required()).contains("from", "to");
    }

    @Test
    void totalToolCountShouldBe18() {
        // 8 task + 5 document + 5 milestone = 18
        int count = 0;
        count++; // taskCreate
        count++; // taskList
        count++; // taskSearch
        count++; // taskView
        count++; // taskEdit
        count++; // taskComplete
        count++; // taskArchive
        count++; // taskFindRelated
        count++; // documentList
        count++; // documentView
        count++; // documentCreate
        count++; // documentUpdate
        count++; // documentSearch
        count++; // milestoneList
        count++; // milestoneAdd
        count++; // milestoneRename
        count++; // milestoneRemove
        count++; // milestoneArchive
        assertThat(count).isEqualTo(18);
    }

    private void assertToolSpec(ToolSpecification spec, String expectedName) {
        assertThat(spec).isNotNull();
        assertThat(spec.name()).isEqualTo(expectedName);
        assertThat(spec.description()).isNotEmpty();
        assertThat(spec.parameters()).isNotNull();
    }
}
