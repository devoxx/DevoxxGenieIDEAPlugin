package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * TASK-244: the Agent Team orchestrator fragment (mandate + live agent catalog) is
 * appended to the system prompt only when both agent mode and team mode are enabled.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentTeamPromptTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private List<AgentDefinition> stored;

    @BeforeEach
    void setUp() {
        stored = new ArrayList<>();
        when(stateService.getAgentDefinitions()).thenAnswer(inv -> stored);
        doAnswer(inv -> {
            stored = new ArrayList<>((List<AgentDefinition>) inv.getArgument(0));
            return null;
        }).when(stateService).setAgentDefinitions(anyList());

        when(stateService.getSystemPrompt()).thenReturn("Base prompt.");
        when(project.getBasePath()).thenReturn("/tmp/project");
        when(project.getLocationHash()).thenReturn("hash");
        when(project.isDefault()).thenReturn(true); // skip SkillRegistry platform access
    }

    @Test
    void teamModeOn_appendsMandateAndCatalog() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentTeamEnabled()).thenReturn(true);

            String prompt = ChatMemoryManager.buildAugmentedSystemPrompt(project);

            assertThat(prompt)
                    .contains("<AGENT_TEAM_INSTRUCTION>")
                    .contains("CORE MANDATE")
                    .contains("delegate_task")
                    .contains("| reviewer |")
                    .contains("| implementer |");
        }
    }

    @Test
    void directSpecialistSelected_appendsPersonaInsteadOfTeamFragment() {
        // TASK-249: "Agent Team" provider + a specialist as the model → the conversation
        // runs AS that agent; its persona replaces the coordinator mandate.
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentTeamEnabled()).thenReturn(true);
            when(project.getLocationHash()).thenReturn("hash");
            when(stateService.getSelectedProvider("hash")).thenReturn("Agent Team");
            when(stateService.getSelectedLanguageModel("hash")).thenReturn("reviewer");

            String prompt = ChatMemoryManager.buildAugmentedSystemPrompt(project);

            assertThat(prompt)
                    .contains("<AGENT_PERSONA agent=\"reviewer\">")
                    .contains("senior code reviewer")
                    .doesNotContain("<AGENT_TEAM_INSTRUCTION>");
        }
    }

    @Test
    void orchestratorSelected_keepsTeamFragment() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentTeamEnabled()).thenReturn(true);
            when(project.getLocationHash()).thenReturn("hash");
            when(stateService.getSelectedProvider("hash")).thenReturn("Agent Team");
            when(stateService.getSelectedLanguageModel("hash")).thenReturn("orchestrator");

            String prompt = ChatMemoryManager.buildAugmentedSystemPrompt(project);

            assertThat(prompt)
                    .contains("<AGENT_TEAM_INSTRUCTION>")
                    .doesNotContain("<AGENT_PERSONA");
        }
    }

    @Test
    void teamModeOff_noFragment() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(true);
            when(stateService.getAgentTeamEnabled()).thenReturn(false);

            assertThat(ChatMemoryManager.buildAugmentedSystemPrompt(project))
                    .doesNotContain("<AGENT_TEAM_INSTRUCTION>");
        }
    }

    @Test
    void agentModeOff_noFragmentEvenIfTeamEnabled() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getAgentModeEnabled()).thenReturn(false);
            when(stateService.getAgentTeamEnabled()).thenReturn(true);

            assertThat(ChatMemoryManager.buildAugmentedSystemPrompt(project))
                    .doesNotContain("<AGENT_TEAM_INSTRUCTION>");
        }
    }
}
