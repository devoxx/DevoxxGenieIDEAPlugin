package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Static factory for all 17 backlog tool specifications.
 * Tool names use the "backlog_" prefix.
 */
public final class BacklogToolSpecifications {

    private BacklogToolSpecifications() {
    }

    // ===== Task Tools (7) =====

    public static ToolSpecification taskCreate() {
        return ToolSpecification.builder()
                .name("backlog_task_create")
                .description("Create a new task in the project backlog. Returns the created task details including generated ID.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("title", "Task title (required)")
                        .addStringProperty("description", "Task description")
                        .addProperty("labels", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Labels/tags for the task")
                                .build())
                        .addProperty("assignee", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Assignees for the task")
                                .build())
                        .addStringProperty("priority", "Priority: high, medium, or low")
                        .addStringProperty("status", "Status: Draft, To Do, In Progress, or Done")
                        .addStringProperty("milestone", "Milestone label")
                        .addProperty("acceptanceCriteria", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Acceptance criteria items")
                                .build())
                        .addProperty("dependencies", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Task IDs this task depends on")
                                .build())
                        .addProperty("references", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Reference URLs or file paths")
                                .build())
                        .addProperty("documentation", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Documentation URLs or file paths")
                                .build())
                        .addStringProperty("parentTaskId", "Parent task ID for subtasks")
                        .required("title")
                        .build())
                .build();
    }

    public static ToolSpecification taskList() {
        return ToolSpecification.builder()
                .name("backlog_task_list")
                .description("List tasks from the project backlog with optional filtering by status, assignee, labels, or search text.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("status", "Filter by status (e.g. 'To Do', 'In Progress')")
                        .addStringProperty("assignee", "Filter by assignee name")
                        .addProperty("labels", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Filter by labels (all must match)")
                                .build())
                        .addStringProperty("search", "Search text to match in title/description")
                        .addProperty("limit", JsonIntegerSchema.builder()
                                .description("Maximum number of results")
                                .build())
                        .build())
                .build();
    }

    public static ToolSpecification taskSearch() {
        return ToolSpecification.builder()
                .name("backlog_task_search")
                .description("Search tasks by title and description with optional status/priority filters.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query", "Search query (required)")
                        .addStringProperty("status", "Filter by status")
                        .addStringProperty("priority", "Filter by priority")
                        .addProperty("limit", JsonIntegerSchema.builder()
                                .description("Maximum number of results")
                                .build())
                        .required("query")
                        .build())
                .build();
    }

    public static ToolSpecification taskView() {
        return ToolSpecification.builder()
                .name("backlog_task_view")
                .description("View full details of a specific task by ID.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Task ID (required)")
                        .required("id")
                        .build())
                .build();
    }

    public static ToolSpecification taskEdit() {
        return ToolSpecification.builder()
                .name("backlog_task_edit")
                .description("Edit a task's metadata, implementation plan/notes, dependencies, and acceptance criteria. " +
                        "Only provided fields are updated; omitted fields remain unchanged.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Task ID (required)")
                        .addStringProperty("title", "New title")
                        .addStringProperty("description", "New description")
                        .addStringProperty("status", "New status")
                        .addStringProperty("priority", "New priority")
                        .addProperty("assignee", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("New assignee list (replaces existing)")
                                .build())
                        .addProperty("labels", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("New labels list (replaces existing)")
                                .build())
                        .addStringProperty("milestone", "New milestone")
                        .addProperty("dependencies", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("New dependencies list (replaces existing)")
                                .build())
                        .addProperty("acceptanceCriteriaAdd", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Acceptance criteria to add")
                                .build())
                        .addProperty("acceptanceCriteriaCheck", JsonArraySchema.builder()
                                .items(JsonIntegerSchema.builder().build())
                                .description("1-based indices of acceptance criteria to check")
                                .build())
                        .addProperty("acceptanceCriteriaUncheck", JsonArraySchema.builder()
                                .items(JsonIntegerSchema.builder().build())
                                .description("1-based indices of acceptance criteria to uncheck")
                                .build())
                        .addStringProperty("planSet", "Set the implementation plan content")
                        .addProperty("planAppend", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Text blocks to append to the implementation plan")
                                .build())
                        .addProperty("planClear", JsonBooleanSchema.builder()
                                .description("Clear the implementation plan")
                                .build())
                        .addStringProperty("notesSet", "Set the implementation notes content")
                        .addProperty("notesAppend", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder().build())
                                .description("Text blocks to append to the implementation notes")
                                .build())
                        .addProperty("notesClear", JsonBooleanSchema.builder()
                                .description("Clear the implementation notes")
                                .build())
                        .addStringProperty("finalSummary", "Set the final summary")
                        .required("id")
                        .build())
                .build();
    }

    public static ToolSpecification taskComplete() {
        return ToolSpecification.builder()
                .name("backlog_task_complete")
                .description("Complete a task: sets status to 'Done' and moves the file to the completed directory.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Task ID (required)")
                        .required("id")
                        .build())
                .build();
    }

    public static ToolSpecification taskArchive() {
        return ToolSpecification.builder()
                .name("backlog_task_archive")
                .description("Archive a task by moving it to the archive directory.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Task ID (required)")
                        .required("id")
                        .build())
                .build();
    }

    public static ToolSpecification taskFindRelated() {
        return ToolSpecification.builder()
                .name("backlog_task_find_related")
                .description("Find tasks that are semantically related to a given task or query. " +
                        "Uses BM25 ranking over task titles, descriptions, labels, and acceptance criteria. " +
                        "Provide either a task ID (to find tasks related to that task) or a free-text query (to search by topic). " +
                        "Returns ranked results with relevance scores.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Task ID to find related tasks for (searches by that task's content)")
                        .addStringProperty("query", "Free-text query to find related tasks (alternative to id)")
                        .addProperty("limit", JsonIntegerSchema.builder()
                                .description("Maximum number of related tasks to return (default: 3)")
                                .build())
                        .build())
                .build();
    }

    // ===== Document Tools (5) =====

    public static ToolSpecification documentList() {
        return ToolSpecification.builder()
                .name("backlog_document_list")
                .description("List documents in the project backlog with optional search filter.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("search", "Optional search text to filter documents")
                        .build())
                .build();
    }

    public static ToolSpecification documentView() {
        return ToolSpecification.builder()
                .name("backlog_document_view")
                .description("View full contents of a specific document by ID.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Document ID (required)")
                        .required("id")
                        .build())
                .build();
    }

    public static ToolSpecification documentCreate() {
        return ToolSpecification.builder()
                .name("backlog_document_create")
                .description("Create a new document in the project backlog.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("title", "Document title (required)")
                        .addStringProperty("content", "Document content in markdown (required)")
                        .required("title", "content")
                        .build())
                .build();
    }

    public static ToolSpecification documentUpdate() {
        return ToolSpecification.builder()
                .name("backlog_document_update")
                .description("Update an existing document's content and optionally its title.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("id", "Document ID (required)")
                        .addStringProperty("content", "New document content (required)")
                        .addStringProperty("title", "New document title")
                        .required("id", "content")
                        .build())
                .build();
    }

    public static ToolSpecification documentSearch() {
        return ToolSpecification.builder()
                .name("backlog_document_search")
                .description("Search documents by title and content.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query", "Search query (required)")
                        .addProperty("limit", JsonIntegerSchema.builder()
                                .description("Maximum number of results")
                                .build())
                        .required("query")
                        .build())
                .build();
    }

    // ===== Milestone Tools (5) =====

    public static ToolSpecification milestoneList() {
        return ToolSpecification.builder()
                .name("backlog_milestone_list")
                .description("List all milestones from the backlog configuration.")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    public static ToolSpecification milestoneAdd() {
        return ToolSpecification.builder()
                .name("backlog_milestone_add")
                .description("Add a new milestone to the backlog configuration.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("name", "Milestone name (required)")
                        .addStringProperty("description", "Milestone description")
                        .required("name")
                        .build())
                .build();
    }

    public static ToolSpecification milestoneRename() {
        return ToolSpecification.builder()
                .name("backlog_milestone_rename")
                .description("Rename a milestone in the backlog configuration, optionally updating all tasks that reference it.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("from", "Current milestone name (required)")
                        .addStringProperty("to", "New milestone name (required)")
                        .addProperty("updateTasks", JsonBooleanSchema.builder()
                                .description("Whether to update tasks referencing this milestone (default: true)")
                                .build())
                        .required("from", "to")
                        .build())
                .build();
    }

    public static ToolSpecification milestoneRemove() {
        return ToolSpecification.builder()
                .name("backlog_milestone_remove")
                .description("Remove a milestone from the backlog configuration. Optionally clear, keep, or reassign tasks.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("name", "Milestone name to remove (required)")
                        .addStringProperty("taskHandling", "What to do with tasks: 'clear' (default), 'keep', or 'reassign'")
                        .addStringProperty("reassignTo", "Target milestone name when taskHandling is 'reassign'")
                        .required("name")
                        .build())
                .build();
    }

    public static ToolSpecification milestoneArchive() {
        return ToolSpecification.builder()
                .name("backlog_milestone_archive")
                .description("Archive a milestone by removing it from active configuration.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("name", "Milestone name to archive (required)")
                        .required("name")
                        .build())
                .build();
    }
}
