package com.devoxx.genie.chatmodel.local.clirunners;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CliRunnersChatModelFactoryTest {

    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private CliRunnersChatModelFactory factory;

    @BeforeEach
    void setUp() {
        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        factory = new CliRunnersChatModelFactory();
    }

    @AfterEach
    void tearDown() {
        mockedStateService.close();
    }

    @Test
    void createChatModel_returnsNull() {
        ChatModel result = factory.createChatModel(new CustomChatModel());
        assertThat(result).isNull();
    }

    @Test
    void getModels_returnsEnabledToolsOnly() {
        List<CliToolConfig> tools = List.of(
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.CLAUDE)
                        .name("Claude")
                        .executablePath("claude")
                        .enabled(true)
                        .build(),
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.COPILOT)
                        .name("Copilot")
                        .executablePath("copilot")
                        .enabled(false)
                        .build(),
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.CODEX)
                        .name("Codex")
                        .executablePath("codex")
                        .enabled(true)
                        .build()
        );
        when(stateService.getCliTools()).thenReturn(tools);

        List<LanguageModel> models = factory.getModels();

        assertThat(models).hasSize(2);
        assertThat(models.get(0).getModelName()).isEqualTo("Claude");
        assertThat(models.get(0).getProvider()).isEqualTo(ModelProvider.CLIRunners);
        assertThat(models.get(0).isApiKeyUsed()).isFalse();
        assertThat(models.get(0).getInputCost()).isEqualTo(0);
        assertThat(models.get(0).getOutputCost()).isEqualTo(0);
        assertThat(models.get(1).getModelName()).isEqualTo("Codex");
    }

    @Test
    void getModels_emptyWhenNoToolsEnabled() {
        List<CliToolConfig> tools = List.of(
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.CLAUDE)
                        .name("Claude")
                        .executablePath("claude")
                        .enabled(false)
                        .build()
        );
        when(stateService.getCliTools()).thenReturn(tools);

        List<LanguageModel> models = factory.getModels();
        assertThat(models).isEmpty();
    }

    @Test
    void getModels_emptyWhenNoToolsConfigured() {
        when(stateService.getCliTools()).thenReturn(List.of());

        List<LanguageModel> models = factory.getModels();
        assertThat(models).isEmpty();
    }

    @Test
    void getModels_setsCorrectDisplayName() {
        List<CliToolConfig> tools = List.of(
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.GEMINI)
                        .name("Gemini CLI")
                        .executablePath("gemini")
                        .enabled(true)
                        .build()
        );
        when(stateService.getCliTools()).thenReturn(tools);

        List<LanguageModel> models = factory.getModels();

        assertThat(models).hasSize(1);
        assertThat(models.get(0).getDisplayName()).isEqualTo("Gemini CLI");
        assertThat(models.get(0).getModelName()).isEqualTo("Gemini CLI");
    }

    @Test
    void getModels_setsZeroTokenLimits() {
        List<CliToolConfig> tools = List.of(
                CliToolConfig.builder()
                        .type(CliToolConfig.CliType.CUSTOM)
                        .name("Custom Tool")
                        .executablePath("custom")
                        .enabled(true)
                        .build()
        );
        when(stateService.getCliTools()).thenReturn(tools);

        List<LanguageModel> models = factory.getModels();

        assertThat(models).hasSize(1);
        assertThat(models.get(0).getInputMaxTokens()).isEqualTo(0);
        assertThat(models.get(0).getOutputMaxTokens()).isEqualTo(0);
    }

    @Test
    void getModels_multipleEnabledTools_allReturned() {
        List<CliToolConfig> tools = List.of(
                CliToolConfig.builder().name("Tool1").enabled(true).build(),
                CliToolConfig.builder().name("Tool2").enabled(true).build(),
                CliToolConfig.builder().name("Tool3").enabled(true).build(),
                CliToolConfig.builder().name("Tool4").enabled(true).build(),
                CliToolConfig.builder().name("Tool5").enabled(true).build()
        );
        when(stateService.getCliTools()).thenReturn(tools);

        List<LanguageModel> models = factory.getModels();
        assertThat(models).hasSize(5);
    }
}
