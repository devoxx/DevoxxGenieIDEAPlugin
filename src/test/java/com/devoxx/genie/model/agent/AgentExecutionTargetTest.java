package com.devoxx.genie.model.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExecutionTargetTest {

    @Test
    void fromString_isNullSafeCaseInsensitiveAndDefaultsToInProcess() {
        assertThat(AgentExecutionTarget.fromString("DOCKER_AGENTS"))
                .isEqualTo(AgentExecutionTarget.DOCKER_AGENTS);
        assertThat(AgentExecutionTarget.fromString(" docker_agents "))
                .isEqualTo(AgentExecutionTarget.DOCKER_AGENTS);
        assertThat(AgentExecutionTarget.fromString("IN_PROCESS"))
                .isEqualTo(AgentExecutionTarget.IN_PROCESS);
        assertThat(AgentExecutionTarget.fromString(null))
                .isEqualTo(AgentExecutionTarget.IN_PROCESS);
        assertThat(AgentExecutionTarget.fromString(""))
                .isEqualTo(AgentExecutionTarget.IN_PROCESS);
        assertThat(AgentExecutionTarget.fromString("something-legacy"))
                .isEqualTo(AgentExecutionTarget.IN_PROCESS);
    }

    @Test
    void agentDefinition_defaultsToInProcess() {
        assertThat(AgentDefinition.builder().name("x").build().effectiveExecutionTarget())
                .isEqualTo(AgentExecutionTarget.IN_PROCESS);
        AgentDefinition def = new AgentDefinition();
        assertThat(def.effectiveExecutionTarget()).isEqualTo(AgentExecutionTarget.IN_PROCESS);
    }
}
