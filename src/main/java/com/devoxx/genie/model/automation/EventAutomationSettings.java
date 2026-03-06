package com.devoxx.genie.model.automation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for all event-agent automation mappings.
 */
@Data
public class EventAutomationSettings {

    private List<EventAgentMapping> mappings = new ArrayList<>();

    public void addMapping(EventAgentMapping mapping) {
        mappings.add(mapping);
    }

    public boolean removeMapping(int index) {
        if (index >= 0 && index < mappings.size()) {
            mappings.remove(index);
            return true;
        }
        return false;
    }

    public EventAgentMapping getMapping(int index) {
        if (index >= 0 && index < mappings.size()) {
            return mappings.get(index);
        }
        return null;
    }

    /**
     * Returns the default set of mappings that ship with the plugin.
     * These serve as suggested automations that users can enable.
     */
    public static List<EventAgentMapping> getDefaultMappings() {
        List<EventAgentMapping> defaults = new ArrayList<>();

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.BEFORE_COMMIT.name())
                .agentType(AgentType.CODE_REVIEW.name())
                .prompt(AgentType.CODE_REVIEW.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.BUILD_FAILED.name())
                .agentType(AgentType.BUILD_FIX.name())
                .prompt(AgentType.BUILD_FIX.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.TEST_FAILED.name())
                .agentType(AgentType.DEBUG.name())
                .prompt(AgentType.DEBUG.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.FILE_CREATED.name())
                .agentType(AgentType.SCAFFOLDER.name())
                .prompt(AgentType.SCAFFOLDER.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.FILE_OPENED.name())
                .agentType(AgentType.EXPLAINER.name())
                .prompt(AgentType.EXPLAINER.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.METHOD_ADDED.name())
                .agentType(AgentType.TEST_GENERATOR.name())
                .prompt(AgentType.TEST_GENERATOR.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.GRADLE_SYNC.name())
                .agentType(AgentType.DEPENDENCY_CHECK.name())
                .prompt(AgentType.DEPENDENCY_CHECK.getDefaultPrompt())
                .autoRun(false)
                .build());

        defaults.add(EventAgentMapping.builder()
                .enabled(false)
                .eventType(IdeEventType.PROJECT_OPENED.name())
                .agentType(AgentType.ONBOARDING.name())
                .prompt(AgentType.ONBOARDING.getDefaultPrompt())
                .autoRun(false)
                .build());

        return defaults;
    }
}
