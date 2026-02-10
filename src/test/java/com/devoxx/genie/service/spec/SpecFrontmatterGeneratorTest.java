package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.BacklogDocument;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecFrontmatterGeneratorTest {

    @Test
    void shouldGenerateCompleteTaskSpec() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Implement login")
                .status("In Progress")
                .priority("high")
                .milestone("v1.0")
                .parentTaskId("TASK-0")
                .createdAt("2024-01-15")
                .updatedAt("2024-01-20")
                .assignees(List.of("@alice", "@bob"))
                .labels(List.of("feature", "auth"))
                .dependencies(List.of("TASK-0"))
                .references(List.of("https://docs.example.com/auth"))
                .documentation(List.of("docs/auth.md"))
                .description("Implement OAuth2 login flow.")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("Google SSO works").checked(true).build(),
                        AcceptanceCriterion.builder().index(1).text("GitHub SSO works").checked(false).build()
                ))
                .definitionOfDone(List.of(
                        DefinitionOfDoneItem.builder().index(0).text("Tests pass").checked(true).build(),
                        DefinitionOfDoneItem.builder().index(1).text("Code reviewed").checked(false).build()
                ))
                .implementationPlan("1. Add OAuth provider\n2. Create callback handler")
                .implementationNotes("Using spring-security-oauth2")
                .finalSummary("Completed with both SSO providers.")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);

        // Frontmatter
        assertThat(result).startsWith("---\n");
        assertThat(result).contains("id: TASK-1\n");
        assertThat(result).contains("title: Implement login\n");
        assertThat(result).contains("status: In Progress\n");
        assertThat(result).contains("priority: high\n");
        assertThat(result).contains("milestone: v1.0\n");
        assertThat(result).contains("parent_task_id: TASK-0\n");
        assertThat(result).contains("created_date: '2024-01-15'\n");
        assertThat(result).contains("updated_date: '2024-01-20'\n");
        assertThat(result).contains("  - @alice\n");
        assertThat(result).contains("  - @bob\n");
        assertThat(result).contains("  - feature\n");
        assertThat(result).contains("  - auth\n");
        assertThat(result).contains("  - \"https://docs.example.com/auth\"\n");
        assertThat(result).contains("  - docs/auth.md\n");

        // Body sections
        assertThat(result).contains("Implement OAuth2 login flow.");
        assertThat(result).contains("## Acceptance Criteria");
        assertThat(result).contains("- [x] Google SSO works");
        assertThat(result).contains("- [ ] GitHub SSO works");
        assertThat(result).contains("## Definition of Done");
        assertThat(result).contains("- [x] Tests pass");
        assertThat(result).contains("- [ ] Code reviewed");
        assertThat(result).contains("## Implementation Plan");
        assertThat(result).contains("1. Add OAuth provider");
        assertThat(result).contains("## Implementation Notes");
        assertThat(result).contains("Using spring-security-oauth2");
        assertThat(result).contains("## Final Summary");
        assertThat(result).contains("Completed with both SSO providers.");
    }

    @Test
    void shouldGenerateMinimalTaskSpec() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-2")
                .title("Simple task")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);

        assertThat(result).startsWith("---\n");
        assertThat(result).contains("id: TASK-2\n");
        assertThat(result).contains("title: Simple task\n");
        assertThat(result).contains("assignee: []\n");
        assertThat(result).contains("labels: []\n");
        assertThat(result).contains("dependencies: []\n");
        assertThat(result).contains("ordinal: 1000\n");
        assertThat(result).contains("---\n");
        assertThat(result).doesNotContain("## Acceptance Criteria");
        assertThat(result).doesNotContain("## Implementation Plan");
    }

    @Test
    void shouldGenerateDocument() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-1")
                .title("Architecture Overview")
                .content("# Architecture\n\nThis is the overview.")
                .build();

        String result = SpecFrontmatterGenerator.generateDocument(doc);

        assertThat(result).startsWith("---\n");
        assertThat(result).contains("id: DOC-1\n");
        assertThat(result).contains("title: Architecture Overview\n");
        assertThat(result).contains("---\n");
        assertThat(result).contains("# Architecture\n\nThis is the overview.");
    }

    @Test
    void shouldQuoteSpecialCharacters() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-3")
                .title("Fix: colon in title")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);

        // Title with colon should be quoted
        assertThat(result).contains("title: \"Fix: colon in title\"");
    }

    @Test
    void shouldRoundTripThroughParserAndGenerator() {
        TaskSpec original = TaskSpec.builder()
                .id("TASK-10")
                .title("Round trip test")
                .status("In Progress")
                .priority("high")
                .milestone("v2.0")
                .assignees(List.of("@dev"))
                .labels(List.of("test"))
                .dependencies(List.of("TASK-9"))
                .description("Test round-trip parsing.")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("Parser works").checked(true).build()
                ))
                .build();

        String generated = SpecFrontmatterGenerator.generate(original);
        TaskSpec parsed = SpecFrontmatterParser.parse(generated, "/test.md");

        assertThat(parsed).isNotNull();
        assertThat(parsed.getId()).isEqualTo("TASK-10");
        assertThat(parsed.getTitle()).isEqualTo("Round trip test");
        assertThat(parsed.getStatus()).isEqualTo("In Progress");
        assertThat(parsed.getPriority()).isEqualTo("high");
        assertThat(parsed.getMilestone()).isEqualTo("v2.0");
        assertThat(parsed.getAssignees()).containsExactly("@dev");
        assertThat(parsed.getLabels()).containsExactly("test");
        assertThat(parsed.getDependencies()).containsExactly("TASK-9");
        assertThat(parsed.getAcceptanceCriteria()).hasSize(1);
        assertThat(parsed.getAcceptanceCriteria().get(0).isChecked()).isTrue();
        assertThat(parsed.getAcceptanceCriteria().get(0).getText()).isEqualTo("Parser works");
    }
}
