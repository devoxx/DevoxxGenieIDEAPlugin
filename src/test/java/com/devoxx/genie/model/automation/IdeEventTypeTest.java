package com.devoxx.genie.model.automation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class IdeEventTypeTest {

    /**
     * Every enum value must have a wired listener. This set must match
     * exactly what PostStartupActivity + plugin.xml register.
     */
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "BEFORE_COMMIT",
            "FILE_SAVED",
            "FILE_OPENED",
            "BUILD_FAILED",
            "BUILD_SUCCEEDED",
            "TEST_FAILED",
            "TEST_SUITE_PASSED",
            "PROCESS_CRASHED"
    );

    @Test
    void allEnumValues_areSupportedByListeners() {
        Set<String> enumNames = Arrays.stream(IdeEventType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(enumNames).isEqualTo(SUPPORTED_EVENTS);
    }

    @Test
    void everyEvent_hasNonBlankDisplayNameAndDescription() {
        for (IdeEventType event : IdeEventType.values()) {
            assertThat(event.getDisplayName()).as(event.name()).isNotBlank();
            assertThat(event.getDescription()).as(event.name()).isNotBlank();
            assertThat(event.getCategory()).as(event.name()).isNotNull();
        }
    }

    @Test
    void categories_matchExpected() {
        Set<String> categories = Arrays.stream(IdeEventType.Category.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(categories).containsExactlyInAnyOrder("VCS", "FILE", "BUILD", "TEST", "DEBUG");
    }
}
