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

    // --- quoteIfNeeded() branch coverage ---

    @Test
    void shouldQuoteValueWithHash() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-4")
                .title("Fix # issue")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"Fix # issue\"");
    }

    @Test
    void shouldQuoteValueWithSingleQuote() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-5")
                .title("It's a test")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"It's a test\"");
    }

    @Test
    void shouldQuoteAndEscapeDoubleQuote() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-6")
                .title("Say \"hello\"")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"Say \\\"hello\\\"\"");
    }

    @Test
    void shouldQuoteValueWithNewline() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-7")
                .title("Line1\nLine2")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"Line1\nLine2\"");
    }

    @Test
    void shouldQuoteValueStartingWithCurlyBrace() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-8")
                .title("{flow}")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"{flow}\"");
    }

    @Test
    void shouldQuoteValueStartingWithSquareBracket() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-9")
                .title("[WIP] Task")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"[WIP] Task\"");
    }

    @Test
    void shouldQuoteValueStartingWithAsterisk() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-10a")
                .title("*important*")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"*important*\"");
    }

    @Test
    void shouldQuoteValueStartingWithAmpersand() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-11")
                .title("&anchor")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"&anchor\"");
    }

    @Test
    void shouldEscapeBackslashInQuotedValue() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-12")
                .title("path\\to: file")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: \"path\\\\to: file\"");
    }

    @Test
    void shouldNotQuotePlainValue() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-13")
                .title("Simple task")
                .status("To Do")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("title: Simple task\n");
        assertThat(result).doesNotContain("title: \"Simple task\"");
    }

    @Test
    void shouldQuoteSpecialCharsInListValues() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-14")
                .title("Test")
                .status("To Do")
                .labels(List.of("feature: auth", "tag#1"))
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("  - \"feature: auth\"");
        assertThat(result).contains("  - \"tag#1\"");
    }

    // --- generate() null/empty field branch coverage ---

    @Test
    void shouldSkipEmptyDescription() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-15")
                .title("No desc")
                .status("To Do")
                .description("")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("---\n\n");
        // After the closing frontmatter, there should be no description text
        String afterFrontmatter = result.substring(result.lastIndexOf("---\n") + 4);
        assertThat(afterFrontmatter.trim()).isEmpty();
    }

    @Test
    void shouldSkipEmptyAcceptanceCriteria() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-16")
                .title("No AC")
                .status("To Do")
                .acceptanceCriteria(List.of())
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("## Acceptance Criteria");
    }

    @Test
    void shouldSkipEmptyDefinitionOfDone() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-17")
                .title("No DoD")
                .status("To Do")
                .definitionOfDone(List.of())
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("## Definition of Done");
    }

    @Test
    void shouldSkipEmptyImplementationPlan() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-18")
                .title("No plan")
                .status("To Do")
                .implementationPlan("")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("## Implementation Plan");
    }

    @Test
    void shouldSkipEmptyImplementationNotes() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-19")
                .title("No notes")
                .status("To Do")
                .implementationNotes("")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("## Implementation Notes");
    }

    @Test
    void shouldSkipEmptyFinalSummary() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-20")
                .title("No summary")
                .status("To Do")
                .finalSummary("")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("## Final Summary");
    }

    @Test
    void shouldSkipNullOptionalScalarFields() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-21")
                .title("Minimal")
                .status("To Do")
                .priority(null)
                .milestone(null)
                .parentTaskId(null)
                .createdAt(null)
                .updatedAt(null)
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("priority:");
        assertThat(result).doesNotContain("milestone:");
        assertThat(result).doesNotContain("parent_task_id:");
        assertThat(result).doesNotContain("created_date:");
        assertThat(result).doesNotContain("updated_date:");
    }

    @Test
    void shouldSkipEmptyStringScalarFields() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-22")
                .title("Empty fields")
                .status("To Do")
                .priority("")
                .milestone("")
                .parentTaskId("")
                .createdAt("")
                .updatedAt("")
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).doesNotContain("priority:");
        assertThat(result).doesNotContain("milestone:");
        assertThat(result).doesNotContain("parent_task_id:");
        assertThat(result).doesNotContain("created_date:");
        assertThat(result).doesNotContain("updated_date:");
    }

    @Test
    void shouldHandleNullLists() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-23")
                .title("Null lists")
                .status("To Do")
                .assignees(null)
                .labels(null)
                .dependencies(null)
                .references(null)
                .documentation(null)
                .build();

        String result = SpecFrontmatterGenerator.generate(spec);
        assertThat(result).contains("assignee: []\n");
        assertThat(result).contains("labels: []\n");
        assertThat(result).contains("dependencies: []\n");
        assertThat(result).contains("references: []\n");
        assertThat(result).contains("documentation: []\n");
    }

    // --- generateDocument() branch coverage ---

    @Test
    void shouldGenerateDocumentWithNullContent() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-2")
                .title("Empty doc")
                .content(null)
                .build();

        String result = SpecFrontmatterGenerator.generateDocument(doc);
        assertThat(result).contains("id: DOC-2\n");
        assertThat(result).contains("title: Empty doc\n");
        String afterFrontmatter = result.substring(result.lastIndexOf("---\n") + 4);
        assertThat(afterFrontmatter.trim()).isEmpty();
    }

    @Test
    void shouldGenerateDocumentWithEmptyContent() {
        BacklogDocument doc = BacklogDocument.builder()
                .id("DOC-3")
                .title("Blank doc")
                .content("")
                .build();

        String result = SpecFrontmatterGenerator.generateDocument(doc);
        String afterFrontmatter = result.substring(result.lastIndexOf("---\n") + 4);
        assertThat(afterFrontmatter.trim()).isEmpty();
    }

    @Test
    void shouldGenerateDocumentWithNullIdAndTitle() {
        BacklogDocument doc = BacklogDocument.builder()
                .content("Some content")
                .build();

        String result = SpecFrontmatterGenerator.generateDocument(doc);
        assertThat(result).doesNotContain("id:");
        assertThat(result).doesNotContain("title:");
        assertThat(result).contains("Some content");
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
