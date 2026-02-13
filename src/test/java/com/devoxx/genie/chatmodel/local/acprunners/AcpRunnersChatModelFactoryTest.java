package com.devoxx.genie.chatmodel.local.acprunners;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcpRunnersChatModelFactoryTest {

    @Test
    void testCreateChatModel_returnsNull() {
        AcpRunnersChatModelFactory factory = new AcpRunnersChatModelFactory();
        ChatModel result = factory.createChatModel(new CustomChatModel());
        assertThat(result).isNull();
    }

    @Test
    void testGetModels_returnsEnabledToolsOnly() {
        try (MockedStatic<DevoxxGenieStateService> mockedState = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockState);

            List<AcpToolConfig> tools = List.of(
                    AcpToolConfig.builder()
                            .type(AcpToolConfig.AcpType.KIMI)
                            .name("Kimi")
                            .executablePath("kimi")
                            .enabled(true)
                            .build(),
                    AcpToolConfig.builder()
                            .type(AcpToolConfig.AcpType.GEMINI)
                            .name("Gemini")
                            .executablePath("gemini")
                            .enabled(false)
                            .build(),
                    AcpToolConfig.builder()
                            .type(AcpToolConfig.AcpType.KILOCODE)
                            .name("Kilocode")
                            .executablePath("kilocode")
                            .enabled(true)
                            .build()
            );
            when(mockState.getAcpTools()).thenReturn(tools);

            AcpRunnersChatModelFactory factory = new AcpRunnersChatModelFactory();
            List<LanguageModel> models = factory.getModels();

            assertThat(models).hasSize(2);
            assertThat(models.get(0).getModelName()).isEqualTo("Kimi");
            assertThat(models.get(0).getProvider()).isEqualTo(ModelProvider.ACPRunners);
            assertThat(models.get(0).isApiKeyUsed()).isFalse();
            assertThat(models.get(1).getModelName()).isEqualTo("Kilocode");
        }
    }

    @Test
    void testGetModels_emptyWhenNoToolsEnabled() {
        try (MockedStatic<DevoxxGenieStateService> mockedState = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockState);

            List<AcpToolConfig> tools = List.of(
                    AcpToolConfig.builder()
                            .type(AcpToolConfig.AcpType.KIMI)
                            .name("Kimi")
                            .executablePath("kimi")
                            .enabled(false)
                            .build()
            );
            when(mockState.getAcpTools()).thenReturn(tools);

            AcpRunnersChatModelFactory factory = new AcpRunnersChatModelFactory();
            List<LanguageModel> models = factory.getModels();

            assertThat(models).isEmpty();
        }
    }

    @Test
    void testGetModels_emptyWhenNoToolsConfigured() {
        try (MockedStatic<DevoxxGenieStateService> mockedState = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(mockState);
            when(mockState.getAcpTools()).thenReturn(List.of());

            AcpRunnersChatModelFactory factory = new AcpRunnersChatModelFactory();
            List<LanguageModel> models = factory.getModels();

            assertThat(models).isEmpty();
        }
    }
}
