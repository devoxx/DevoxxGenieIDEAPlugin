package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecContextBuilderTest {

    // ── buildContext: full spec ─────────────────────────────────────────

    @Test
    void buildContext_shouldBuildCompleteContext() {
        TaskSpec spec = TaskSpec.builder()
                .id("BACK-12")
                .title("Add authentication flow")
                .status("In Progress")
                .priority("high")
                .assignees(List.of("@john", "@jane"))
                .labels(List.of("feature", "security"))
                .dependencies(List.of("BACK-10", "BACK-11"))
                .description("Implement OAuth2 authentication.")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("Users can sign in with Google").checked(true).build(),
                        AcceptanceCriterion.builder().index(1).text("Users can sign in with GitHub").checked(false).build()
                ))
                .definitionOfDone(List.of(
                        DefinitionOfDoneItem.builder().index(0).text("Unit tests pass").checked(false).build(),
                        DefinitionOfDoneItem.builder().index(1).text("No security issues").checked(false).build()
                ))
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).startsWith("<TaskSpec>");
        assertThat(context).endsWith("</TaskSpec>");
        assertThat(context).contains("Task ID: BACK-12");
        assertThat(context).contains("Title: Add authentication flow");
        assertThat(context).contains("Status: In Progress");
        assertThat(context).contains("Priority: high");
        assertThat(context).contains("Assignees: @john, @jane");
        assertThat(context).contains("Labels: feature, security");
        assertThat(context).contains("## Description");
        assertThat(context).contains("Implement OAuth2 authentication.");
        assertThat(context).contains("## Acceptance Criteria");
        assertThat(context).contains("- [x] Users can sign in with Google");
        assertThat(context).contains("- [ ] Users can sign in with GitHub");
        assertThat(context).contains("## Definition of Done");
        assertThat(context).contains("- [ ] Unit tests pass");
        assertThat(context).contains("## Dependencies");
        assertThat(context).contains("- BACK-10");
        assertThat(context).contains("- BACK-11");
    }

    // ── buildContext: minimal spec ──────────────────────────────────────

    @Test
    void buildContext_shouldBuildMinimalContext() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Simple task")
                .status("To Do")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).startsWith("<TaskSpec>");
        assertThat(context).endsWith("</TaskSpec>");
        assertThat(context).contains("Task ID: TASK-1");
        assertThat(context).contains("Title: Simple task");
        assertThat(context).doesNotContain("## Acceptance Criteria");
        assertThat(context).doesNotContain("## Dependencies");
        assertThat(context).doesNotContain("## Description");
        assertThat(context).doesNotContain("## Definition of Done");
        assertThat(context).doesNotContain("## References");
        assertThat(context).doesNotContain("## Documentation");
        assertThat(context).doesNotContain("## Implementation Plan");
        assertThat(context).doesNotContain("## Implementation Notes");
        assertThat(context).doesNotContain("## Final Summary");
        assertThat(context).doesNotContain("Milestone:");
        assertThat(context).doesNotContain("Assignees:");
        assertThat(context).doesNotContain("Labels:");
    }

    // ── buildContext: null fields ───────────────────────────────────────

    @Test
    void buildContext_handlesAllNullFields() {
        TaskSpec spec = TaskSpec.builder().build();
        // Clear defaults to test null handling
        spec.setId(null);
        spec.setTitle(null);
        spec.setStatus(null);
        spec.setPriority(null);
        spec.setAssignees(null);
        spec.setLabels(null);
        spec.setDependencies(null);
        spec.setAcceptanceCriteria(null);
        spec.setDefinitionOfDone(null);
        spec.setDescription(null);
        spec.setMilestone(null);
        spec.setReferences(null);
        spec.setDocumentation(null);
        spec.setImplementationPlan(null);
        spec.setImplementationNotes(null);
        spec.setFinalSummary(null);

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).isEqualTo("<TaskSpec>\n</TaskSpec>");
    }

    // ── buildContext: milestone ─────────────────────────────────────────

    @Test
    void buildContext_includesMilestone() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .milestone("v2.0")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("Milestone: v2.0");
    }

    @Test
    void buildContext_omitsMilestone_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .milestone("")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("Milestone:");
    }

    @Test
    void buildContext_omitsMilestone_whenNull() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .milestone(null)
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("Milestone:");
    }

    // ── buildContext: references ────────────────────────────────────────

    @Test
    void buildContext_includesReferences() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .references(List.of("src/main/java/Foo.java", "https://docs.example.com"))
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("## References");
        assertThat(context).contains("- src/main/java/Foo.java");
        assertThat(context).contains("- https://docs.example.com");
    }

    @Test
    void buildContext_omitsReferences_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .references(List.of())
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## References");
    }

    // ── buildContext: documentation ─────────────────────────────────────

    @Test
    void buildContext_includesDocumentation() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .documentation(List.of("docs/architecture.md", "docs/api.md"))
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("## Documentation");
        assertThat(context).contains("- docs/architecture.md");
        assertThat(context).contains("- docs/api.md");
    }

    @Test
    void buildContext_omitsDocumentation_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .documentation(List.of())
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Documentation");
    }

    // ── buildContext: implementation plan ───────────────────────────────

    @Test
    void buildContext_includesImplementationPlan() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .implementationPlan("1. Create service\n2. Add tests\n3. Wire up UI")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("## Implementation Plan");
        assertThat(context).contains("1. Create service");
        assertThat(context).contains("2. Add tests");
    }

    @Test
    void buildContext_omitsImplementationPlan_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .implementationPlan("")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Implementation Plan");
    }

    // ── buildContext: implementation notes ──────────────────────────────

    @Test
    void buildContext_includesImplementationNotes() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .implementationNotes("Used factory pattern for extensibility.")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("## Implementation Notes");
        assertThat(context).contains("Used factory pattern for extensibility.");
    }

    @Test
    void buildContext_omitsImplementationNotes_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .implementationNotes("")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Implementation Notes");
    }

    // ── buildContext: final summary ─────────────────────────────────────

    @Test
    void buildContext_includesFinalSummary() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .finalSummary("Implemented OAuth2 with Google and GitHub providers.")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).contains("## Final Summary");
        assertThat(context).contains("Implemented OAuth2 with Google and GitHub providers.");
    }

    @Test
    void buildContext_omitsFinalSummary_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .finalSummary("")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Final Summary");
    }

    // ── buildContext: empty lists vs null lists ─────────────────────────

    @Test
    void buildContext_omitsSections_whenListsAreEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .assignees(List.of())
                .labels(List.of())
                .dependencies(List.of())
                .acceptanceCriteria(List.of())
                .definitionOfDone(List.of())
                .references(List.of())
                .documentation(List.of())
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("Assignees:");
        assertThat(context).doesNotContain("Labels:");
        assertThat(context).doesNotContain("## Dependencies");
        assertThat(context).doesNotContain("## Acceptance Criteria");
        assertThat(context).doesNotContain("## Definition of Done");
        assertThat(context).doesNotContain("## References");
        assertThat(context).doesNotContain("## Documentation");
    }

    // ── buildContext: description edge cases ────────────────────────────

    @Test
    void buildContext_omitsDescription_whenEmpty() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .description("")
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Description");
    }

    @Test
    void buildContext_omitsDescription_whenNull() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .description(null)
                .build();

        String context = SpecContextBuilder.buildContext(spec);

        assertThat(context).doesNotContain("## Description");
    }

    // ── buildAgentInstruction ───────────────────────────────────────────

    @Test
    void buildAgentInstruction_includesIdAndTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("BACK-12")
                .title("Add authentication flow")
                .build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).contains("BACK-12");
        assertThat(instruction).contains(": Add authentication flow");
        assertThat(instruction).contains("acceptance criteria");
        assertThat(instruction).contains("backlog_task_edit");
        assertThat(instruction).contains("In Progress");
        assertThat(instruction).contains("Done");
    }

    @Test
    void buildAgentInstruction_handlesNullId() {
        TaskSpec spec = TaskSpec.builder()
                .id(null)
                .title("Some task")
                .build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).contains("You are implementing task ");
        assertThat(instruction).contains(": Some task");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildAgentInstruction_handlesNullTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("T-1")
                .title(null)
                .build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).contains("T-1");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildAgentInstruction_handlesBothNull() {
        TaskSpec spec = TaskSpec.builder()
                .id(null)
                .title(null)
                .build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).startsWith("You are implementing task .\n");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildAgentInstruction_containsWorkflowSteps() {
        TaskSpec spec = TaskSpec.builder().id("T-1").build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).contains("backlog_task_edit");
        assertThat(instruction).contains("acceptanceCriteriaCheck");
        assertThat(instruction).contains("notesAppend");
        assertThat(instruction).contains("finalSummary");
        assertThat(instruction).contains("Do NOT use backlog_task_complete");
    }

    // ── buildCliInstruction ─────────────────────────────────────────────

    @Test
    void buildCliInstruction_includesIdAndTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("CLI-5")
                .title("Fix parsing bug")
                .build();

        String instruction = SpecContextBuilder.buildCliInstruction(spec);

        assertThat(instruction).contains("CLI-5");
        assertThat(instruction).contains(": Fix parsing bug");
        assertThat(instruction).contains("acceptance criteria");
    }

    @Test
    void buildCliInstruction_handlesNullId() {
        TaskSpec spec = TaskSpec.builder()
                .id(null)
                .title("Task without ID")
                .build();

        String instruction = SpecContextBuilder.buildCliInstruction(spec);

        assertThat(instruction).contains("You are implementing task ");
        assertThat(instruction).contains(": Task without ID");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildCliInstruction_handlesNullTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("CLI-1")
                .title(null)
                .build();

        String instruction = SpecContextBuilder.buildCliInstruction(spec);

        assertThat(instruction).contains("CLI-1");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildCliInstruction_handlesBothNull() {
        TaskSpec spec = TaskSpec.builder()
                .id(null)
                .title(null)
                .build();

        String instruction = SpecContextBuilder.buildCliInstruction(spec);

        assertThat(instruction).startsWith("You are implementing task .\n");
        assertThat(instruction).doesNotContain("null");
    }

    @Test
    void buildCliInstruction_containsWorkflowSteps() {
        TaskSpec spec = TaskSpec.builder().id("T-1").build();

        String instruction = SpecContextBuilder.buildCliInstruction(spec);

        assertThat(instruction).contains("Backlog MCP tools");
        assertThat(instruction).contains("backlog task_edit");
        assertThat(instruction).contains("acceptanceCriteriaCheck");
        assertThat(instruction).contains("notesAppend");
        assertThat(instruction).contains("finalSummary");
        assertThat(instruction).contains("In Progress");
        assertThat(instruction).contains("Done");
    }

    @Test
    void buildCliInstruction_mentionsMcpNotUnderscoreStyle() {
        TaskSpec spec = TaskSpec.builder().id("T-1").build();

        String cliInstruction = SpecContextBuilder.buildCliInstruction(spec);
        String agentInstruction = SpecContextBuilder.buildAgentInstruction(spec);

        // CLI uses "backlog task_edit" (MCP space-separated), agent uses "backlog_task_edit" (underscore)
        assertThat(cliInstruction).contains("backlog task_edit");
        assertThat(agentInstruction).contains("backlog_task_edit");
    }
}
