package com.devoxx.genie.chatmodel.agentteam;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * TASK-249: the "Agent Team" pseudo-provider lists agents as models and resolves them
 * to their bound real providers.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentTeamChatModelFactoryTest {

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
    }

    private MockedStatic<DevoxxGenieStateService> mockState() {
        MockedStatic<DevoxxGenieStateService> mocked = mockStatic(DevoxxGenieStateService.class);
        mocked.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        return mocked;
    }

    @Test
    void getModels_listsAgentsWithOrchestratorFirst() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            List<LanguageModel> models = new AgentTeamChatModelFactory().getModels();

            assertThat(models).extracting(LanguageModel::getModelName)
                    .containsExactly("orchestrator", "architect", "implementer", "reviewer", "documentalist");
            assertThat(models).allMatch(m -> m.getProvider() == ModelProvider.AgentTeam);
            assertThat(models.get(0).getDisplayName()).contains("coordinates team");
        }
    }

    @Test
    void getModels_omitsDisabledAgents() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            AgentTeamChatModelFactory factory = new AgentTeamChatModelFactory();
            factory.getModels(); // seed
            stored.stream().filter(d -> d.getName().equals("documentalist"))
                    .forEach(d -> d.setEnabled(false));

            assertThat(factory.getModels()).extracting(LanguageModel::getModelName)
                    .doesNotContain("documentalist");
        }
    }

    @Test
    void resolveBinding_usesTheAgentsBoundProvider() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            new AgentTeamChatModelFactory().getModels(); // seeds the registry
            stored.stream().filter(d -> d.getName().equals("reviewer")).forEach(d -> {
                d.setModelProvider("Anthropic");
                d.setModelName("claude-sonnet-4-5");
            });

            AgentTeamChatModelFactory.ResolvedBinding binding =
                    AgentTeamChatModelFactory.resolveBinding("reviewer");

            assertThat(binding.providerName()).isEqualTo("Anthropic");
            assertThat(binding.modelName()).isEqualTo("claude-sonnet-4-5");
            assertThat(binding.factory()).isNotNull();
        }
    }

    @Test
    void resolveBinding_unknownAgent_throwsActionableError() {
        try (MockedStatic<DevoxxGenieStateService> ignored = mockState()) {
            assertThatThrownBy(() -> AgentTeamChatModelFactory.resolveBinding("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nonexistent")
                    .hasMessageContaining("Settings");
        }
    }
}
