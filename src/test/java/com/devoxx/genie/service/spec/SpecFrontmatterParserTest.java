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
