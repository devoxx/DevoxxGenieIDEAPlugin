package com.devoxx.genie.chatmodel.local.nativ;

import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.nativ.NativModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NativChatModelFactoryTest {

    private static NativModelEntryDTO modelEntry(String id) {
        NativModelEntryDTO entry = new NativModelEntryDTO();
        entry.setId(id);
        entry.setObject("model");
        return entry;
    }

    @Test
    void createChatModel_usesConfiguredNativUrl() {
        try (MockedStatic<DevoxxGenieStateService> ignored = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(state);
            when(state.getNativModelUrl()).thenReturn("http://localhost:8080/v1/");

            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("mlx-community/Qwen2.5-Coder-7B-Instruct-4bit");

            ChatModel result = new NativChatModelFactory().createChatModel(customChatModel);

            assertThat(result).isNotNull();
        }
    }

    @Test
    void createStreamingChatModel_usesConfiguredNativUrl() {
        try (MockedStatic<DevoxxGenieStateService> ignored = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(state);
            when(state.getNativModelUrl()).thenReturn("http://localhost:8080/v1/");

            CustomChatModel customChatModel = new CustomChatModel();
            customChatModel.setModelName("mlx-community/gemma-3-4b-it-4bit");

            StreamingChatModel result = new NativChatModelFactory().createStreamingChatModel(customChatModel);

            assertThat(result).isNotNull();
        }
    }

    /**
     * Nativ never reports a context length, so an unconfigured fallback must land on the
     * factory's documented default rather than 0 (which would break the token/usage bar).
     */
    @Test
    void buildLanguageModel_withoutConfiguredFallback_usesDefaultContextLength() {
        try (MockedStatic<DevoxxGenieStateService> ignored = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(state);
            when(state.getNativFallbackContextLength()).thenReturn(null);

            LanguageModel model = new NativChatModelFactory()
                    .buildLanguageModel(modelEntry("mlx-community/Qwen2.5-Coder-7B-Instruct-4bit"));

            assertThat(model.getProvider()).isEqualTo(ModelProvider.Nativ);
            assertThat(model.getModelName()).isEqualTo("mlx-community/Qwen2.5-Coder-7B-Instruct-4bit");
            assertThat(model.getDisplayName()).isEqualTo("Qwen2.5-Coder-7B-Instruct-4bit");
            assertThat(model.getInputMaxTokens()).isEqualTo(NativChatModelFactory.DEFAULT_CONTEXT_LENGTH);
            assertThat(model.isApiKeyUsed()).isFalse();
            assertThat(model.getInputCost()).isZero();
            assertThat(model.getOutputCost()).isZero();
        }
    }

    @Test
    void buildLanguageModel_withConfiguredFallback_usesConfiguredContextLength() {
        try (MockedStatic<DevoxxGenieStateService> ignored = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
            when(DevoxxGenieStateService.getInstance()).thenReturn(state);
            when(state.getNativFallbackContextLength()).thenReturn(131_072);

            LanguageModel model = new NativChatModelFactory()
                    .buildLanguageModel(modelEntry("mlx-community/gemma-3-4b-it-4bit"));

            assertThat(model.getInputMaxTokens()).isEqualTo(131_072);
        }
    }

    @Test
    void resolveDisplayName_withoutOrgPrefix_keepsRawId() {
        assertThat(modelEntry("my-local-model").resolveDisplayName()).isEqualTo("my-local-model");
    }

    @Test
    void resolveDisplayName_withTrailingSlash_keepsRawId() {
        assertThat(modelEntry("mlx-community/").resolveDisplayName()).isEqualTo("mlx-community/");
    }

    @Test
    void resolveDisplayName_withNullId_returnsEmptyString() {
        assertThat(new NativModelEntryDTO().resolveDisplayName()).isEmpty();
    }
}
