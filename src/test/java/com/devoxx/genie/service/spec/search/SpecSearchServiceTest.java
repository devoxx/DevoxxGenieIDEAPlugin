package com.devoxx.genie.service.spec.search;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpecSearchServiceTest {

    @Test
    void buildSearchPayloadShouldIncludeTitle() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Add authentication")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("Add authentication");
    }

    @Test
    void buildSearchPayloadShouldIncludeDescription() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .description("Implement JWT-based authentication for REST API")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("JWT-based authentication");
    }

    @Test
    void buildSearchPayloadShouldIncludeLabels() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .labels(List.of("security", "api"))
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("security");
        assertThat(payload).contains("api");
    }

    @Test
    void buildSearchPayloadShouldIncludeAcceptanceCriteria() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("POST /auth/login works").checked(false).build(),
                        AcceptanceCriterion.builder().index(1).text("JWT tokens expire after 24h").checked(false).build()
                ))
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("POST /auth/login works");
        assertThat(payload).contains("JWT tokens expire after 24h");
    }

    @Test
    void buildSearchPayloadShouldIncludeMilestone() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .milestone("v2.0")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("v2.0");
    }

    @Test
    void buildSearchPayloadShouldWeightTitleByRepeating() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Authentication")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        // Title should appear twice for weighting
        int firstIdx = payload.indexOf("Authentication");
        int secondIdx = payload.indexOf("Authentication", firstIdx + 1);
        assertThat(secondIdx).isGreaterThan(firstIdx);
    }

    @Test
    void buildSearchPayloadShouldHandleNullFields() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        // Should not throw, may be empty
        assertThat(payload).isNotNull();
    }

    @Test
    void buildSearchPayloadShouldIncludeImplementationPlan() {
        TaskSpec spec = TaskSpec.builder()
                .id("TASK-1")
                .title("Auth")
                .implementationPlan("1. Add spring security dependency\n2. Create auth controller")
                .build();

        String payload = SpecSearchService.buildSearchPayload(spec);

        assertThat(payload).contains("spring security");
    }
}
