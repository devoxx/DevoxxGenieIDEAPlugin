package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecContextBuilderTest {

    @Test
    void shouldBuildCompleteContext() {
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
                .definitionOfDone(List.of("Unit tests pass", "No security issues"))
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
    }

    @Test
    void shouldBuildMinimalContext() {
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
    }

    @Test
    void shouldBuildAgentInstruction() {
        TaskSpec spec = TaskSpec.builder()
                .id("BACK-12")
                .title("Add authentication flow")
                .build();

        String instruction = SpecContextBuilder.buildAgentInstruction(spec);

        assertThat(instruction).contains("BACK-12");
        assertThat(instruction).contains("Add authentication flow");
        assertThat(instruction).contains("acceptance criteria");
    }
}
