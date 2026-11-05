package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.router.RouterDefinition;
import com.devoxx.genie.chatmodel.cloud.router.RouterRegistry;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
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

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LLMProvidersRouterSettingsTest {

    private static final ModelProvider ROUTER = ModelProvider.OpenRouter;

    @Mock private Application application;
    @Mock private Project project;
    @Mock private MessageBus messageBus;
    @Mock private SettingsChangeListener settingsChangeListener;
    @Mock private PropertiesService propertiesService;
    @Mock private ChatModelFactory routerFactory;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<PropertiesService> propertiesServiceMockedStatic;
    private MockedStatic<ChatModelFactoryProvider> factoryProviderMockedStatic;

    private DevoxxGenieStateService stateService;
    private LLMProvidersConfigurable configurable;
    private JComponent root;
    private final RouterDefinition definition = RouterRegistry.find(ROUTER).orElseThrow();

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        propertiesServiceMockedStatic = Mockito.mockStatic(PropertiesService.class);
        propertiesServiceMockedStatic.when(PropertiesService::getInstance).thenReturn(propertiesService);
        when(propertiesService.getVersion()).thenReturn("1.0.0-test");
        lenient().when(project.getMessageBus()).thenReturn(messageBus);
        lenient().when(messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)).thenReturn(settingsChangeListener);

        factoryProviderMockedStatic = Mockito.mockStatic(ChatModelFactoryProvider.class);
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(anyString()))
                .thenReturn(Optional.empty());
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(ROUTER.name()))
                .thenReturn(Optional.of(routerFactory));
    }

    @AfterEach
    void tearDown() {
        factoryProviderMockedStatic.close();
        propertiesServiceMockedStatic.close();
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    private LLMProvidersConfigurable configurable() {
        configurable = new LLMProvidersConfigurable(project);
        root = configurable.createComponent();
        return configurable;
    }

    private LLMProvidersComponent component() {
        try {
            Field field = LLMProvidersConfigurable.class.getDeclaredField("llmSettingsComponent");
            field.setAccessible(true);
            return (LLMProvidersComponent) field.get(configurable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JTabbedPane find(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane tabs && tabs.getTabCount() > 0) {
                return tabs;
            }
            if (c instanceof Container nested) {
                JTabbedPane found = find(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectText(Component c, List<String> out) {
        if (c instanceof JLabel label && label.getText() != null) {
            out.add(label.getText());
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectText(child, out);
            }
        }
    }

    private JTabbedPane providerTabs() {
        return find(root);
    }

    private List<String> tabText(String title) {
        JTabbedPane tabs = providerTabs();
        List<String> texts = new ArrayList<>();
        collectText(tabs.getComponentAt(tabs.indexOfTab(title)), texts);
        return texts;
    }

    // ---- component -----------------------------------------------------------------------------

    @Test
    void tabs_inOrder() {
        configurable();
        JTabbedPane tabs = providerTabs();
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            titles.add(tabs.getTitleAt(i));
        }
        assertThat(titles).containsExactly("Local", "Cloud Direct", "Routers", "Custom OpenAI");
    }

    @Test
    void routersTab_showsWarningAndAllRouters() {
        configurable();
        List<String> routers = tabText("Routers");
        assertThat(String.join("\n", routers)).contains("Routers forward your prompts and code");
        for (RouterDefinition def : RouterRegistry.definitions()) {
            assertThat(routers).contains(def.label() + " API Key");
            assertThat(component().getRouterRows()).containsKey(def.provider());
        }
        assertThat(routers).contains("Cloudflare API Key", "Cloudflare Account ID",
                "Cloudflare Gateway Name", "Cloudflare Model");

        List<String> cloud = tabText("Cloud Direct");
        assertThat(cloud).doesNotContain("Open Router API Key", "OpenRouter API Key", "Cloudflare API Key",
                "Cloudflare Account ID", "Cloudflare Gateway Name", "Cloudflare Model");
        assertThat(cloud).contains("NVIDIA API Key", "Enable Azure OpenAI Provider", "Enable AWS Bedrock");
    }

    @Test
    void warning_matchesSpecText() {
        String plain = LLMProvidersComponent.ROUTERS_WARNING.replaceAll("<[^>]+>", "");
        assertThat(plain).isEqualTo("Routers forward your prompts and code to other providers. "
                + "Anything you send, including project files added to context, may pass through the router "
                + "and whichever provider it selects. Each has its own logging, retention and training policies, "
                + "and a provider may be in another country. DevoxxGenie doesn't verify these services or their "
                + "privacy claims, and listing one here isn't an endorsement. Check the router's terms before "
                + "enabling it, and avoid sending confidential code or secrets unless your organisation allows it.");
        assertThat(LLMProvidersComponent.ROUTERS_WARNING).startsWith("<b>Routers forward your prompts and code to other providers.</b>");
    }

    // ---- configurable --------------------------------------------------------------------------

    @Test
    void isModified_detectsKeyAndCheckbox() {
        configurable();
        assertThat(configurable.isModified()).isFalse();

        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.key().setText("sk-new");
        assertThat(configurable.isModified()).isTrue();
        row.key().setText("");
        assertThat(configurable.isModified()).isFalse();

        row.enabled().setSelected(true);
        assertThat(configurable.isModified()).isTrue();
    }

    @Test
    void apply_persistsKeyAndFlag() throws ConfigurationException {
        configurable();
        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.enabled().setSelected(true);
        row.key().setText("sk-new");

        configurable.apply();

        assertThat(stateService.isRouterEnabled(ROUTER)).isTrue();
        assertThat(stateService.getRouterKey(definition.credentialSubKey())).isEqualTo("sk-new");
    }

    @Test
    void reset_restoresBoth() {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "sk-stored");
        configurable();
        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.enabled().setSelected(false);
        row.key().setText("edited");

        configurable.reset();

        assertThat(row.enabled().isSelected()).isTrue();
        assertThat(new String(row.key().getPassword())).isEqualTo("sk-stored");
    }

    @Test
    void validation_flagsEnabledRouterWithoutKey() {
        configurable();
        component().getRouterRows().get(ROUTER).enabled().setSelected(true);

        assertThatThrownBy(() -> configurable.apply())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining(definition.label() + " is enabled but has no API key");
        verify(routerFactory, never()).resetModels();
    }

    @Test
    void apply_resetsCatalogOnlyForChangedRouter() throws ConfigurationException {
        configurable();
        configurable.apply();
        verify(routerFactory, never()).resetModels();

        component().getRouterRows().get(ROUTER).key().setText("sk-1");
        configurable.apply();
        verify(routerFactory, times(1)).resetModels();

        // Nothing changed since: no further reset.
        configurable.apply();
        verify(routerFactory, times(1)).resetModels();

        // Cloudflare is not a generic router and has no factory reset here.
        factoryProviderMockedStatic.verify(
                () -> ChatModelFactoryProvider.getFactoryByProvider(ModelProvider.Cloudflare.name()), never());
    }

    @Test
    void apply_keyRemoved_clearsCatalogAndStopsRequests() throws ConfigurationException {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "sk-old");
        configurable();
        List<String> keyAtReset = new ArrayList<>();
        doAnswer(inv -> {
            keyAtReset.add(stateService.getRouterKey(definition.credentialSubKey()));
            return null;
        }).when(routerFactory).resetModels();

        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.enabled().setSelected(false);
        row.key().setText("");
        configurable.apply();

        assertThat(keyAtReset).containsExactly("");
        assertThat(stateService.isRouterEnabled(ROUTER)).isFalse();
    }

    @Test
    void apply_writesKeyAndFlagBeforeReset() throws ConfigurationException {
        configurable();
        List<String> seen = new ArrayList<>();
        doAnswer(inv -> {
            seen.add(stateService.getRouterKey(definition.credentialSubKey()) + "|" + stateService.isRouterEnabled(ROUTER));
            return null;
        }).when(routerFactory).resetModels();

        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.enabled().setSelected(true);
        row.key().setText("sk-new");
        configurable.apply();

        assertThat(seen).containsExactly("sk-new|true");
    }

    @Test
    void apply_disableThenReenableWithNewKey_resetsEachTime() throws ConfigurationException {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "sk-old");
        configurable();
        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);

        row.enabled().setSelected(false);
        configurable.apply();
        verify(routerFactory, times(1)).resetModels();

        row.enabled().setSelected(true);
        row.key().setText("sk-corrected");
        configurable.apply();
        verify(routerFactory, times(2)).resetModels();
        assertThat(stateService.getRouterKey(definition.credentialSubKey())).isEqualTo("sk-corrected");
    }

    @Test
    void apply_correctedKeyWhileEnabled_resetsSoBackoffClears() throws ConfigurationException {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "sk-bad");
        configurable();

        component().getRouterRows().get(ROUTER).key().setText("sk-good");
        configurable.apply();

        verify(routerFactory, times(1)).resetModels();
    }

    @Test
    void apply_unchanged_doesNotDeleteKey() throws ConfigurationException {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "sk-keep");
        configurable();

        configurable.apply();

        assertThat(stateService.getRouterKey(definition.credentialSubKey())).isEqualTo("sk-keep");
        assertThat(stateService.isRouterEnabled(ROUTER)).isTrue();
        verify(routerFactory, never()).resetModels();
    }
}
