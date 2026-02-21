package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecFrontmatterParserTest {

    @Test
    void shouldParseCompleteTaskSpec() {
        String content = """
                ---
                id: BACK-12
                title: "Add authentication flow"
                status: "In Progress"
                priority: high
                assignee:
                  - "@john"
                  - "@jane"
                labels:
                  - feature
                  - security
                dependencies:
                  - BACK-10
                  - BACK-11
                ---

                ## Description
                Implement OAuth2 authentication with Google and GitHub providers.

                ## Acceptance Criteria
                - [x] Users can sign in with Google
                - [ ] Users can sign in with GitHub
                - [x] Session tokens are stored securely
                - [ ] Logout clears all tokens
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/path/to/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("BACK-12");
        assertThat(spec.getTitle()).isEqualTo("Add authentication flow");
        assertThat(spec.getStatus()).isEqualTo("In Progress");
        assertThat(spec.getPriority()).isEqualTo("high");
        assertThat(spec.getAssignees()).containsExactly("@john", "@jane");
        assertThat(spec.getLabels()).containsExactly("feature", "security");
        assertThat(spec.getDependencies()).containsExactly("BACK-10", "BACK-11");
        assertThat(spec.getFilePath()).isEqualTo("/path/to/task.md");

        // Acceptance criteria
        List<AcceptanceCriterion> criteria = spec.getAcceptanceCriteria();
        assertThat(criteria).hasSize(4);
        assertThat(criteria.get(0).isChecked()).isTrue();
        assertThat(criteria.get(0).getText()).isEqualTo("Users can sign in with Google");
        assertThat(criteria.get(1).isChecked()).isFalse();
        assertThat(criteria.get(1).getText()).isEqualTo("Users can sign in with GitHub");
        assertThat(criteria.get(2).isChecked()).isTrue();
        assertThat(criteria.get(3).isChecked()).isFalse();
    }

    @Test
    void shouldParseMinimalSpec() {
        String content = """
                ---
                id: TASK-1
                title: Simple task
                status: To Do
                ---

                Just a simple task with no extras.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("TASK-1");
        assertThat(spec.getTitle()).isEqualTo("Simple task");
        assertThat(spec.getStatus()).isEqualTo("To Do");
        assertThat(spec.getAcceptanceCriteria()).isEmpty();
        assertThat(spec.getLabels()).isEmpty();
    }

    @Test
    void shouldReturnNullForNoFrontmatter() {
        String content = """
                # Just a regular markdown file

                No frontmatter here.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/readme.md");
        assertThat(spec).isNull();
    }

    @Test
    void shouldHandleQuotedAndUnquotedValues() {
        String content = """
                ---
                id: "BACK-5"
                title: 'Single quoted title'
                status: Done
                priority: "low"
                ---

                Body content.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("BACK-5");
        assertThat(spec.getTitle()).isEqualTo("Single quoted title");
        assertThat(spec.getStatus()).isEqualTo("Done");
        assertThat(spec.getPriority()).isEqualTo("low");
    }

    @Test
    void shouldHandleEmptyBody() {
        String content = """
                ---
                id: BACK-99
                title: Empty body task
                status: To Do
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("BACK-99");
        assertThat(spec.getDescription()).isEmpty();
    }

    @Test
    void shouldCountCheckedCriteria() {
        String content = """
                ---
                id: BACK-7
                title: Counting test
                status: In Progress
                ---

                - [x] Done item 1
                - [x] Done item 2
                - [ ] Not done item 3
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getAcceptanceCriteria()).hasSize(3);
        assertThat(spec.getCheckedAcceptanceCriteriaCount()).isEqualTo(2);
    }

    @Test
    void shouldParseUppercaseXInCheckbox() {
        String content = """
                ---
                id: BACK-8
                title: Uppercase X
                status: To Do
                ---

                - [X] Item with uppercase X
                - [ ] Unchecked item
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getAcceptanceCriteria()).hasSize(2);
        assertThat(spec.getAcceptanceCriteria().get(0).isChecked()).isTrue();
        assertThat(spec.getAcceptanceCriteria().get(1).isChecked()).isFalse();
    }

    @Test
    void shouldIgnoreUnknownFrontmatterFields() {
        String content = """
                ---
                id: BACK-42
                title: Unknown fields
                status: To Do
                custom_field: some value
                another_field:
                  - item1
                  - item2
                ---

                Body.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("BACK-42");
        assertThat(spec.getTitle()).isEqualTo("Unknown fields");
    }

    // --- stripQuotes() branch coverage ---

    @Test
    void stripQuotesEmptyString() {
        assertThat(SpecFrontmatterParser.stripQuotes("")).isEqualTo("");
    }

    @Test
    void stripQuotesSingleChar() {
        assertThat(SpecFrontmatterParser.stripQuotes("x")).isEqualTo("x");
    }

    @Test
    void stripQuotesMismatchedDoubleQuote() {
        // Starts with " but doesn't end with "
        assertThat(SpecFrontmatterParser.stripQuotes("\"hello")).isEqualTo("\"hello");
    }

    @Test
    void stripQuotesMismatchedSingleQuote() {
        // Starts with ' but doesn't end with '
        assertThat(SpecFrontmatterParser.stripQuotes("'hello")).isEqualTo("'hello");
    }

    @Test
    void stripQuotesUnquotedValue() {
        assertThat(SpecFrontmatterParser.stripQuotes("plain value")).isEqualTo("plain value");
    }

    @Test
    void stripQuotesDoubleQuoted() {
        assertThat(SpecFrontmatterParser.stripQuotes("\"quoted\"")).isEqualTo("quoted");
    }

    @Test
    void stripQuotesSingleQuoted() {
        assertThat(SpecFrontmatterParser.stripQuotes("'quoted'")).isEqualTo("quoted");
    }

    // --- applyScalarField() branch coverage: alternate key names ---

    @Test
    void shouldParseAlternateCreatedDateKey() {
        String content = """
                ---
                id: TASK-A
                title: Alt date
                status: To Do
                created_date: 2024-06-01
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getCreatedAt()).isEqualTo("2024-06-01");
    }

    @Test
    void shouldParseAlternateUpdatedDateKey() {
        String content = """
                ---
                id: TASK-B
                title: Alt date
                status: To Do
                updated_date: 2024-06-15
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getUpdatedAt()).isEqualTo("2024-06-15");
    }

    @Test
    void shouldParseAlternateParentKey() {
        String content = """
                ---
                id: TASK-C
                title: Alt parent
                status: To Do
                parent: TASK-0
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getParentTaskId()).isEqualTo("TASK-0");
    }

    @Test
    void shouldParseOrdinalWithInvalidNumber() {
        String content = """
                ---
                id: TASK-D
                title: Bad ordinal
                status: To Do
                ordinal: not_a_number
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        // Ordinal should fall back to builder default (1000) on NumberFormatException
        assertThat(spec.getOrdinal()).isEqualTo(1000);
    }

    // --- applyListField() branch coverage: alternate key names ---

    @Test
    void shouldParseAlternateListKeyNames() {
        String content = """
                ---
                id: TASK-E
                title: Alt list keys
                status: To Do
                label:
                  - bug
                dependency:
                  - TASK-1
                reference:
                  - http://example.com
                docs:
                  - readme.md
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getLabels()).containsExactly("bug");
        assertThat(spec.getDependencies()).containsExactly("TASK-1");
        assertThat(spec.getReferences()).containsExactly("http://example.com");
        assertThat(spec.getDocumentation()).containsExactly("readme.md");
    }

    @Test
    void shouldHandleExplicitEmptyArrayInFrontmatter() {
        // "labels: []" should result in an empty list (not null)
        String content = """
                ---
                id: TASK-X1
                title: Explicit empty array
                status: To Do
                labels: []
                assignees: []
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("TASK-X1");
        assertThat(spec.getLabels()).isEmpty();
        assertThat(spec.getAssignees()).isEmpty();
    }

    // --- parseFrontmatter() branch coverage ---

    @Test
    void shouldHandleFrontmatterLineWithoutColon() {
        // A line in frontmatter that doesn't contain a colon is ignored
        String content = """
                ---
                id: TASK-F
                title: Test
                status: To Do
                this line has no colon
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("TASK-F");
    }

    @Test
    void shouldHandleListItemWithoutPriorKey() {
        // A "- item" line when currentKey/currentList is null (before any key-value pair)
        String content = """
                ---
                - orphan item
                id: TASK-G
                title: Orphan list
                status: To Do
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getId()).isEqualTo("TASK-G");
    }

    @Test
    void shouldFlushListOnNextScalarField() {
        // Ensures the "flush previous list" branch is hit when a scalar follows a list
        String content = """
                ---
                id: TASK-H
                title: Flush test
                status: To Do
                labels:
                  - one
                  - two
                priority: high
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getLabels()).containsExactly("one", "two");
        assertThat(spec.getPriority()).isEqualTo("high");
    }

    @Test
    void shouldFlushTrailingList() {
        // Last field in frontmatter is a list — must be flushed after loop
        String content = """
                ---
                id: TASK-I
                title: Trailing list
                status: To Do
                labels:
                  - alpha
                  - beta
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getLabels()).containsExactly("alpha", "beta");
    }

    // --- parseSections() branch coverage ---

    @Test
    void shouldHandleSectionWithNoCheckboxes() {
        // Definition of Done section with no checkboxes → items list stays empty
        String content = """
                ---
                id: TASK-J
                title: Empty DoD
                status: To Do
                ---

                ## Definition of Done

                No checkboxes here, just text.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getDefinitionOfDone()).isEmpty();
    }

    @Test
    void shouldHandleAcceptanceCriteriaSectionWithNoCheckboxes() {
        String content = """
                ---
                id: TASK-K
                title: Empty AC
                status: To Do
                ---

                ## Acceptance Criteria

                No checkboxes at all.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getAcceptanceCriteria()).isEmpty();
    }

    @Test
    void shouldParseDodAlternateHeaderName() {
        String content = """
                ---
                id: TASK-L
                title: DoD alt
                status: To Do
                ---

                ## DoD

                - [x] Unit tests
                - [ ] Integration tests
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getDefinitionOfDone()).hasSize(2);
    }

    @Test
    void shouldParseAlternateSectionNames() {
        String content = """
                ---
                id: TASK-M
                title: Alt sections
                status: To Do
                ---

                ## Plan

                Step 1 do things.

                ## Notes

                Some notes here.

                ## Summary

                Final summary text.
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getImplementationPlan()).contains("Step 1 do things.");
        assertThat(spec.getImplementationNotes()).contains("Some notes here.");
        assertThat(spec.getFinalSummary()).contains("Final summary text.");
    }

    @Test
    void shouldHandleDescriptionSectionExplicitly() {
        String content = """
                ---
                id: TASK-N
                title: Explicit description
                status: To Do
                ---

                ## Description

                This is the explicit description section.

                ## Acceptance Criteria

                - [ ] Something
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getDescription()).contains("This is the explicit description section.");
    }

    @Test
    void shouldHandleEmptyPreSectionText() {
        // No text before the first ## header
        String content = """
                ---
                id: TASK-O
                title: No pre-text
                status: To Do
                ---
                ## Acceptance Criteria

                - [x] Done
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/test.md");
        assertThat(spec).isNotNull();
        assertThat(spec.getAcceptanceCriteria()).hasSize(1);
    }

    @Test
    void shouldGenerateDisplayLabel() {
        String content = """
                ---
                id: BACK-15
                title: Fix search bug
                status: To Do
                ---
                """;

        TaskSpec spec = SpecFrontmatterParser.parse(content, "/task.md");

        assertThat(spec).isNotNull();
        assertThat(spec.getDisplayLabel()).isEqualTo("BACK-15: Fix search bug");
    }
}
