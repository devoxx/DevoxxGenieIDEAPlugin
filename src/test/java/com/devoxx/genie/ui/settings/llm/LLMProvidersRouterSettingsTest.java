package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.router.RouterDefinition;
import com.devoxx.genie.chatmodel.cloud.router.RouterRegistry;
import com.devoxx.genie.chatmodel.cloud.router.RouterChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.credentials.CredentialService;
import com.sun.net.httpserver.HttpServer;
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        assertThat(routers).anyMatch(text -> text.contains(LLMProvidersComponent.ROUTERS_WARNING));
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
    void routersTab_listsDemonRouteLast_withRequestyAboveIt() {
        configurable();
        List<String> apiKeyRows = tabText("Routers").stream()
                .filter(text -> text.endsWith(" API Key"))
                .toList();
        assertThat(apiKeyRows).containsExactly(
                "OpenRouter API Key", "Cloudflare API Key", "OrcaRouter API Key", "Requesty API Key",
                "DemonRoute API Key");
    }

    @Test
    void everyRouterRow_startsDisabledWithEmptyKey() {
        configurable();
        for (RouterDefinition def : RouterRegistry.definitions()) {
            LLMProvidersComponent.RouterRow row = component().getRouterRows().get(def.provider());
            assertThat(row).as("row for %s", def.provider()).isNotNull();
            assertThat(row.enabled().isSelected()).isFalse();
            assertThat(row.key().getPassword()).isEmpty();
        }
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
        stateService.setRouterKey(definition.credentialSubKey(), "sk-stored");
        stateService.setRouterEnabled(ROUTER, true);
        configurable();
        assertThat(configurable.isModified()).as("not dirty straight after opening").isFalse();

        LLMProvidersComponent.RouterRow row = component().getRouterRows().get(ROUTER);
        row.key().setText("sk-new");
        assertThat(configurable.isModified()).isTrue();
        row.key().setText("sk-stored");
        assertThat(configurable.isModified()).isFalse();

        row.enabled().setSelected(false);
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
        RouterDefinition second = new RouterDefinition(ModelProvider.Groq, "Second", "https://second.test/v1/",
                List.of("/models"), "router.second", "https://second.test/keys", null, null, null);
        ChatModelFactory secondFactory = mock(ChatModelFactory.class);
        factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(ModelProvider.Groq.name()))
                .thenReturn(Optional.of(secondFactory));

        try (MockedStatic<RouterRegistry> registry = Mockito.mockStatic(RouterRegistry.class)) {
            registry.when(RouterRegistry::definitions).thenReturn(List.of(definition, second));
            configurable();
            assertThat(component().getRouterRows()).containsOnlyKeys(ROUTER, ModelProvider.Groq);

            configurable.apply();
            verify(routerFactory, never()).resetModels();
            verify(secondFactory, never()).resetModels();

            component().getRouterRows().get(ModelProvider.Groq).key().setText("sk-second");
            configurable.apply();
            verify(secondFactory, times(1)).resetModels();
            verify(routerFactory, never()).resetModels();
            assertThat(stateService.getRouterKey("router.second")).isEqualTo("sk-second");

            component().getRouterRows().get(ROUTER).enabled().setSelected(false);
            component().getRouterRows().get(ROUTER).key().setText("sk-first");
            configurable.apply();
            verify(routerFactory, times(1)).resetModels();
            verify(secondFactory, times(1)).resetModels();
        }
        factoryProviderMockedStatic.verify(
                () -> ChatModelFactoryProvider.getFactoryByProvider(ModelProvider.Cloudflare.name()), never());
    }

    /** A real factory against a loopback server: only a key containing "good" gets a catalog. */
    private final class RealRouter {
        final AtomicInteger requests = new AtomicInteger();
        final HttpServer server;
        final RouterChatModelFactory factory;
        private final MockedStatic<LLMProviderService> providerService;

        RealRouter() throws Exception {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/models", exchange -> {
                requests.incrementAndGet();
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                boolean good = auth != null && auth.contains("good");
                byte[] body = (good ? "{\"data\":[{\"id\":\"model-a\"}]}" : "{}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(good ? 200 : 500, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            LLMProviderService service = mock(LLMProviderService.class);
            when(service.getApiKey(ROUTER)).thenAnswer(inv -> stateService.getRouterKey(definition.credentialSubKey()));
            providerService = Mockito.mockStatic(LLMProviderService.class);
            providerService.when(LLMProviderService::getInstance).thenReturn(service);

            RouterDefinition local = new RouterDefinition(ROUTER, definition.label(),
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/", List.of("/models"),
                    definition.credentialSubKey(), definition.keyUrl(), null, definition.catalogFields(), null);
            factory = new RouterChatModelFactory(local) {
                @Override
                protected void notifyFetchFailure() {
                    // No project in a unit test.
                }
            };
            factoryProviderMockedStatic.when(() -> ChatModelFactoryProvider.getFactoryByProvider(ROUTER.name()))
                    .thenReturn(Optional.of(factory));
        }

        void close() {
            providerService.close();
            server.stop(0);
        }
    }

    @Test
    void apply_keyRemoved_clearsCatalogAndStopsRequests() throws Exception {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "good-key");
        RealRouter router = new RealRouter();
        try {
            assertThat(router.factory.getModels()).hasSize(1);
            assertThat(router.requests).hasValue(1);

            configurable();
            component().getRouterRows().get(ROUTER).enabled().setSelected(false);
            component().getRouterRows().get(ROUTER).key().setText("");
            configurable.apply();

            assertThat(router.factory.getCachedModels()).as("catalog dropped").isEmpty();
            stateService.setRouterEnabled(ROUTER, true);
            assertThat(router.factory.getModels()).isEmpty();
            assertThat(router.requests).as("no request without a key").hasValue(1);
        } finally {
            router.close();
        }
    }

    @Test
    void apply_correctedKey_clearsBackoffAndRequestsAgain() throws Exception {
        stateService.setRouterEnabled(ROUTER, true);
        stateService.setRouterKey(definition.credentialSubKey(), "bad-key");
        RealRouter router = new RealRouter();
        try {
            assertThat(router.factory.getModels()).isEmpty();
            assertThat(router.factory.getModels()).as("inside the failure backoff").isEmpty();
            assertThat(router.requests).hasValue(1);

            configurable();
            component().getRouterRows().get(ROUTER).key().setText("good-key");
            configurable.apply();

            assertThat(router.factory.getModels()).hasSize(1);
            assertThat(router.requests).hasValue(2);
        } finally {
            router.close();
        }
    }

    /** A keychain that holds {@code routerKey} for the router and nothing for any other entry. */
    private CredentialService credentialServiceReading(String routerKey) {
        CredentialService credentials = mock(CredentialService.class,
                inv -> inv.getMethod().getReturnType() == String.class ? "" : null);
        doReturn(routerKey).when(credentials).getCredential(definition.credentialSubKey());
        return credentials;
    }

    @Test
    void apply_unchangedRouterWithEmptyField_neverWritesCredential() throws ConfigurationException {
        // A keychain read that fails or finds nothing gives an empty field. Applying another edit must not
        // write that empty value back, because an empty write deletes the stored entry.
        CredentialService credentials = credentialServiceReading("");
        when(application.getService(CredentialService.class)).thenReturn(credentials);
        configurable();

        component().getOllamaEnabledCheckBox().setSelected(true);
        configurable.apply();

        verify(credentials, never()).setCredential(eq(definition.credentialSubKey()), any());
        verify(credentials, never()).removeCredential(anyString());
        verify(routerFactory, never()).resetModels();
    }

    @Test
    void apply_unchangedRouterWithStoredKey_neverWritesCredential() throws ConfigurationException {
        CredentialService credentials = credentialServiceReading("sk-keep");
        when(application.getService(CredentialService.class)).thenReturn(credentials);
        stateService.setRouterEnabled(ROUTER, true);
        configurable();

        component().getOllamaEnabledCheckBox().setSelected(true);
        configurable.apply();

        verify(credentials, never()).setCredential(eq(definition.credentialSubKey()), any());
    }

    @Test
    void apply_keyRemovedOnPurpose_deletesIt() throws ConfigurationException {
        CredentialService credentials = credentialServiceReading("sk-old");
        when(application.getService(CredentialService.class)).thenReturn(credentials);
        configurable();

        component().getRouterRows().get(ROUTER).key().setText("");
        configurable.apply();

        verify(credentials).setCredential(definition.credentialSubKey(), "");
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
