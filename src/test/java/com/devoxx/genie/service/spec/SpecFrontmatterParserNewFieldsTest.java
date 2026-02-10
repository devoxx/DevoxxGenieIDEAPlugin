package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the new fields and section-aware parsing added to SpecFrontmatterParser.
 */
class SpecFrontmatterParserNewFieldsTest {

    @Test
    void shouldParseNewScalarFields() {
        String content = """
                ---
                id: TASK-1
                title: New fields test
                status: In Progress
                priority: high
                milestone: v1.0
                created_at: 2024-01-15
                updated_at: 2024-01-20
                parent_task_id: TASK-0
                ---

                Description text.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getMilestone()).isEqualTo("v1.0");
        assertThat(spec.getCreatedAt()).isEqualTo("2024-01-15");
        assertThat(spec.getUpdatedAt()).isEqualTo("2024-01-20");
        assertThat(spec.getParentTaskId()).isEqualTo("TASK-0");
    }

    @Test
    void shouldParseNewListFields() {
        String content = """
                ---
                id: TASK-2
                title: List fields test
                status: To Do
                references:
                  - https://example.com/doc1
                  - https://example.com/doc2
                documentation:
                  - docs/README.md
                  - docs/api.md
                ---

                Body.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getReferences()).containsExactly("https://example.com/doc1", "https://example.com/doc2");
        assertThat(spec.getDocumentation()).containsExactly("docs/README.md", "docs/api.md");
    }

    @Test
    void shouldParseSectionAwareBody() {
        String content = """
                ---
                id: TASK-3
                title: Sections test
                status: In Progress
                ---

                Intro text before sections.

                ## Acceptance Criteria

                - [x] First criterion met
                - [ ] Second criterion pending

                ## Definition of Done

                - [x] Tests written
                - [ ] Docs updated

                ## Implementation Plan

                1. Step one
                2. Step two

                ## Implementation Notes

                Some notes about the implementation.

                ## Final Summary

                Task completed successfully.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();

        // Description should be the intro text
        assertThat(spec.getDescription()).contains("Intro text before sections.");

        // Acceptance criteria parsed from ## section
        assertThat(spec.getAcceptanceCriteria()).hasSize(2);
        assertThat(spec.getAcceptanceCriteria().get(0).getText()).isEqualTo("First criterion met");
        assertThat(spec.getAcceptanceCriteria().get(0).isChecked()).isTrue();
        assertThat(spec.getAcceptanceCriteria().get(1).getText()).isEqualTo("Second criterion pending");
        assertThat(spec.getAcceptanceCriteria().get(1).isChecked()).isFalse();

        // Definition of done parsed from ## section
        assertThat(spec.getDefinitionOfDone()).hasSize(2);
        assertThat(spec.getDefinitionOfDone().get(0).getText()).isEqualTo("Tests written");
        assertThat(spec.getDefinitionOfDone().get(0).isChecked()).isTrue();
        assertThat(spec.getDefinitionOfDone().get(1).getText()).isEqualTo("Docs updated");
        assertThat(spec.getDefinitionOfDone().get(1).isChecked()).isFalse();

        // Implementation plan
        assertThat(spec.getImplementationPlan()).contains("1. Step one");
        assertThat(spec.getImplementationPlan()).contains("2. Step two");

        // Implementation notes
        assertThat(spec.getImplementationNotes()).contains("Some notes about the implementation.");

        // Final summary
        assertThat(spec.getFinalSummary()).contains("Task completed successfully.");
    }

    @Test
    void shouldParseDefinitionOfDoneWithCheckboxes() {
        String content = """
                ---
                id: TASK-4
                title: DoD checkbox test
                status: In Progress
                ---

                ## Definition of Done

                - [x] Code compiles
                - [X] Tests pass
                - [ ] PR reviewed
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getDefinitionOfDone()).hasSize(3);
        assertThat(spec.getDefinitionOfDone().get(0).isChecked()).isTrue();
        assertThat(spec.getDefinitionOfDone().get(1).isChecked()).isTrue();
        assertThat(spec.getDefinitionOfDone().get(2).isChecked()).isFalse();
        assertThat(spec.getCheckedDefinitionOfDoneCount()).isEqualTo(2);
    }

    @Test
    void shouldHandleBodyWithOnlyDescription() {
        String content = """
                ---
                id: TASK-5
                title: No sections
                status: To Do
                ---

                Just a plain description with no section headers.

                - [x] Some checkbox
                - [ ] Another checkbox
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getDescription()).contains("Just a plain description");
        // Checkboxes without section headers go to acceptance criteria (backward compat)
        assertThat(spec.getAcceptanceCriteria()).hasSize(2);
        // Definition of done should be empty since there's no ## section
        assertThat(spec.getDefinitionOfDone()).isEmpty();
    }

    @Test
    void shouldParseUnknownSectionsAsDescription() {
        String content = """
                ---
                id: TASK-6
                title: Custom sections
                status: To Do
                ---

                Intro.

                ## Custom Section

                Custom content here.

                ## Acceptance Criteria

                - [ ] Some criterion
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        // Custom section should be folded into description
        assertThat(spec.getDescription()).contains("Custom content here.");
        assertThat(spec.getAcceptanceCriteria()).hasSize(1);
    }
}
