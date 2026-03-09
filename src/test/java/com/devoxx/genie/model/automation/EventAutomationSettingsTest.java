package com.devoxx.genie.model.automation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventAutomationSettingsTest {

    @Test
    void defaultMappings_onlyReferenceSupportedEventTypes() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();

        for (EventAgentMapping mapping : defaults) {
            // Should not throw IllegalArgumentException
            IdeEventType eventType = IdeEventType.valueOf(mapping.getEventType());
            assertThat(eventType).as("Event type %s must be valid", mapping.getEventType()).isNotNull();
        }
    }

    @Test
    void defaultMappings_onlyReferenceSupportedAgentTypes() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();

        for (EventAgentMapping mapping : defaults) {
            AgentType agentType = AgentType.valueOf(mapping.getAgentType());
            assertThat(agentType).as("Agent type %s must be valid", mapping.getAgentType()).isNotNull();
        }
    }

    @Test
    void defaultMappings_allDisabledByDefault() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();

        assertThat(defaults).isNotEmpty();
        assertThat(defaults).allSatisfy(m ->
                assertThat(m.isEnabled()).as("Default mapping %s should be disabled", m.getEventType()).isFalse());
    }

    @Test
    void defaultMappings_allHaveNonBlankPrompt() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();

        assertThat(defaults).allSatisfy(m ->
                assertThat(m.getPrompt()).as("Mapping %s should have a prompt", m.getEventType()).isNotBlank());
    }

    @Test
    void defaultMappings_containExpectedEvents() {
        List<EventAgentMapping> defaults = EventAutomationSettings.getDefaultMappings();
        List<String> eventTypes = defaults.stream().map(EventAgentMapping::getEventType).toList();

        assertThat(eventTypes).containsExactlyInAnyOrder(
                "BEFORE_COMMIT",
                "BUILD_FAILED",
                "TEST_FAILED",
                "FILE_OPENED"
        );
    }

    @Test
    void addAndRemoveMapping() {
        EventAutomationSettings settings = new EventAutomationSettings();
        assertThat(settings.getMappings()).isEmpty();

        EventAgentMapping mapping = EventAgentMapping.builder()
                .enabled(true)
                .eventType(IdeEventType.FILE_SAVED.name())
                .agentType(AgentType.CUSTOM.name())
                .prompt("Test prompt")
                .autoRun(false)
                .build();

        settings.addMapping(mapping);
        assertThat(settings.getMappings()).hasSize(1);
        assertThat(settings.getMapping(0)).isEqualTo(mapping);

        assertThat(settings.removeMapping(0)).isTrue();
        assertThat(settings.getMappings()).isEmpty();
    }

    @Test
    void getMapping_outOfBounds_returnsNull() {
        EventAutomationSettings settings = new EventAutomationSettings();
        assertThat(settings.getMapping(-1)).isNull();
        assertThat(settings.getMapping(0)).isNull();
    }

    @Test
    void removeMapping_outOfBounds_returnsFalse() {
        EventAutomationSettings settings = new EventAutomationSettings();
        assertThat(settings.removeMapping(-1)).isFalse();
        assertThat(settings.removeMapping(0)).isFalse();
    }
}
