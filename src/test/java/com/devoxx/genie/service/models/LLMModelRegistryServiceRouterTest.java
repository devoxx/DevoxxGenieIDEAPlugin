package com.devoxx.genie.service.models;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.router.RouterChatModelFactory;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLMModelRegistryServiceRouterTest {

    private MockedStatic<ApplicationManager> appManager;
    private MockedStatic<DevoxxGenieStateService> stateStatic;
    private MockedStatic<ChatModelFactoryProvider> providerStatic;
    private DevoxxGenieStateService state;
    private RouterChatModelFactory routerFactory;

    @BeforeEach
    void setUp() {
        appManager = Mockito.mockStatic(ApplicationManager.class);
        appManager.when(ApplicationManager::getApplication).thenReturn(mock(Application.class));

        state = mock(DevoxxGenieStateService.class);
        stateStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateStatic.when(DevoxxGenieStateService::getInstance).thenReturn(state);

        routerFactory = mock(RouterChatModelFactory.class);
        providerStatic = Mockito.mockStatic(ChatModelFactoryProvider.class);
        providerStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider("OpenRouter"))
                .thenReturn(Optional.of(routerFactory));
    }

    @AfterEach
    void tearDown() {
        providerStatic.close();
        stateStatic.close();
        appManager.close();
    }

    private static LanguageModel model(String name) {
        return LanguageModel.builder().provider(ModelProvider.OpenRouter).modelName(name).apiKeyUsed(true).build();
    }

    @Test
    void registry_mergesCachedOnly_noFetch() {
        when(state.isRouterEnabled(ModelProvider.OpenRouter)).thenReturn(true);
        when(routerFactory.getCachedModels()).thenReturn(List.of(model("vendor/alpha")));

        List<LanguageModel> models = new LLMModelRegistryService().getModels();

        assertThat(models).anyMatch(m -> m.getProvider() == ModelProvider.OpenRouter
                && m.getModelName().equals("vendor/alpha"));
        verify(routerFactory, never()).getModels();
        verify(state, never()).getRouterKey(Mockito.anyString());
    }

    @Test
    void registry_emptyBeforeAnyCatalogIsCached() {
        when(state.isRouterEnabled(ModelProvider.OpenRouter)).thenReturn(true);
        when(routerFactory.getCachedModels()).thenReturn(List.of());

        assertThat(new LLMModelRegistryService().getModels())
                .noneMatch(m -> m.getProvider() == ModelProvider.OpenRouter);
    }

    @Test
    void registry_disabledRouterCachedModelsDoNotAppear() {
        when(state.isRouterEnabled(ModelProvider.OpenRouter)).thenReturn(false);
        when(routerFactory.getCachedModels()).thenReturn(List.of(model("vendor/alpha")));

        assertThat(new LLMModelRegistryService().getModels())
                .noneMatch(m -> m.getProvider() == ModelProvider.OpenRouter);
    }
}
