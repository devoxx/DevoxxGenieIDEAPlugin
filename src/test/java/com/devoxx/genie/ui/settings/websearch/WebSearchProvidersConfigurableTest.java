package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import com.devoxx.genie.service.websearch.TavilyStubServer;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.service.websearch.WebSearchServiceTestSupport;
import com.devoxx.genie.ui.listener.WebSearchStateListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.XmlSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Settings → Web search (TASK-277.1): one provider choice, registry-driven credential rows, Test search
 * against the unsaved form, and persistence across a restart.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSearchProvidersConfigurableTest {

    /** PasswordSafe stand-in shared by every state instance, as the real application service is. */
    static final class InMemoryCredentialService implements CredentialService {
        private final Map<String, String> store = new ConcurrentHashMap<>();

        @Override public @NotNull String getCredential(@NotNull CredentialKey key) { return getCredential(key.getSubKey()); }
        @Override public void setCredential(@NotNull CredentialKey key, @Nullable String value) { setCredential(key.getSubKey(), value); }
        @Override public void removeCredential(@NotNull CredentialKey key) { removeCredential(key.getSubKey()); }
        @Override public @NotNull String getCredential(@NotNull String subKey) { return store.getOrDefault(subKey, ""); }
        @Override public void setCredential(@NotNull String subKey, @Nullable String value) {
            if (value == null || value.isEmpty()) store.remove(subKey); else store.put(subKey, value);
        }
        @Override public void removeCredential(@NotNull String subKey) { store.remove(subKey); }
        @Override public boolean isAvailable() { return false; }
    }

    @Mock
    private Application application;

    @Mock
    private Project project;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private final InMemoryCredentialService credentials = new InMemoryCredentialService();
    private DevoxxGenieStateService stateService;
    private WebSearchProvidersConfigurable configurable;
    private WebSearchProvidersComponent component;

    @BeforeEach
    void setUp() {
        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(CredentialService.class)).thenReturn(credentials);

        stateService = new DevoxxGenieStateService();
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        MessageBus messageBus = mock(MessageBus.class);
        WebSearchStateListener noopListener = enabled -> {};
        lenient().when(project.getMessageBus()).thenReturn(messageBus);
        lenient().when(messageBus.syncPublisher(AppTopics.WEB_SEARCH_STATE_TOPIC)).thenReturn(noopListener);

        newConfigurable();
    }

    private void newConfigurable() {
        configurable = new WebSearchProvidersConfigurable(project);
        configurable.createComponent();
        component = getComponent();
    }

    @AfterEach
    void tearDown() {
        WebSearchServiceTestSupport.reset();
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(configurable.getDisplayName()).isEqualTo("Web Search");
    }

    @Test
    void providerChoices_areNonePlusEveryRegisteredProvider() {
        var combo = component.getProviderComboBox();
        assertThat(combo.getItemCount()).isEqualTo(1 + WebSearchProvider.values().length);
        assertThat(combo.getItemAt(0).provider()).isNull();
        assertThat(combo.getItemAt(0).label()).isEqualTo("None");
        assertThat(combo.getItemAt(1).label()).isEqualTo("Tavily");
        assertThat(combo.getItemAt(2).label()).isEqualTo("Keenable");
        assertThat(combo.getItemAt(3).label()).isEqualTo("Google Custom Search (deprecated)");
    }

    @Nested
    class Visibility {

        @Test
        void onlyTheSelectedProvidersCredentialRowsAreShown() {
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            assertThat(component.isShowingRowsOf(WebSearchProvider.GOOGLE)).isTrue();
            assertThat(component.isShowingRowsOf(WebSearchProvider.TAVILY)).isFalse();

            component.setSelectedProvider(WebSearchProvider.TAVILY);
            assertThat(component.isShowingRowsOf(WebSearchProvider.TAVILY)).isTrue();
            assertThat(component.isShowingRowsOf(WebSearchProvider.GOOGLE)).isFalse();
        }

        @Test
        void keenableSelected_showsOnlyItsOptionalKeyRow() {
            component.setSelectedProvider(WebSearchProvider.KEENABLE);

            assertThat(component.isShowingRowsOf(WebSearchProvider.KEENABLE)).isTrue();
            assertThat(component.isShowingRowsOf(WebSearchProvider.TAVILY)).isFalse();
            assertThat(component.isShowingRowsOf(WebSearchProvider.GOOGLE)).isFalse();
        }

        @Test
        void keenableKey_isAppliedAndPersisted() {
            component.setSelectedProvider(WebSearchProvider.KEENABLE);
            component.getCredentialField(CredentialKey.KEENABLE_SEARCH_KEY).setText("kn-key");
            configurable.apply();

            assertThat(stateService.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.KEENABLE);
            assertThat(stateService.getCredential(CredentialKey.KEENABLE_SEARCH_KEY)).isEqualTo("kn-key");
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void keenableWithoutKey_testSearchRunsInsteadOfAskingForAKey() {
            component.setSelectedProvider(WebSearchProvider.KEENABLE);

            assertThat(com.devoxx.genie.service.websearch.WebSearchService.readiness(component.formConfig()).isReady())
                    .isTrue();
        }

        @Test
        void googleSelected_showsTheDeprecationWarning_withDateAndAlternatives() {
            component.setSelectedProvider(WebSearchProvider.GOOGLE);

            assertThat(component.getDeprecationWarning().isVisible()).isTrue();
            assertThat(component.getDeprecationWarning().getText())
                    .contains("January 1, 2027").contains("Keenable").contains("Tavily");
        }

        @Test
        void deprecationWarning_isHidden_forOtherProvidersAndNone() {
            for (WebSearchProvider provider : new WebSearchProvider[]{WebSearchProvider.TAVILY, WebSearchProvider.KEENABLE, null}) {
                component.setSelectedProvider(provider);
                assertThat(component.getDeprecationWarning().isVisible()).as("warning for %s", provider).isFalse();
            }
        }

        @Test
        void freshSettings_defaultToNoProvider_notGoogle() {
            assertThat(new DevoxxGenieStateService().getSelectedWebSearchProvider()).isNull();
            assertThat(component.getSelectedProvider()).isNull();
        }

        @Test
        void noneSelected_showsNoCredentialRows() {
            component.setSelectedProvider(null);
            for (WebSearchProvider provider : WebSearchProvider.values()) {
                assertThat(component.isShowingRowsOf(provider)).as("%s rows", provider).isFalse();
            }
        }

        @Test
        void changingTheProviderInTheForm_doesNotWriteSettingsBeforeApply() {
            stateService.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("typed-not-applied");

            assertThat(stateService.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.TAVILY);
            assertThat(stateService.getGoogleSearchKey()).isEmpty();
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void switchingWebSearchOff_keepsTheProviderChoice() {
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getEnableWebSearchCheckbox().setSelected(true);
            component.getEnableWebSearchCheckbox().setSelected(false);

            assertThat(component.getSelectedProvider()).isEqualTo(WebSearchProvider.GOOGLE);
        }
    }

    @Nested
    class Apply {

        @Test
        void appliesEnableSwitch() {
            component.getEnableWebSearchCheckbox().setSelected(true);
            configurable.apply();
            assertThat(stateService.getIsWebSearchEnabled()).isTrue();

            component.getEnableWebSearchCheckbox().setSelected(false);
            configurable.apply();
            assertThat(stateService.getIsWebSearchEnabled()).isFalse();
        }

        @Test
        void appliesSelectedProvider_andKeepsLegacyFlagsInSync() {
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            configurable.apply();

            assertThat(stateService.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.GOOGLE);
            assertThat(stateService.isGoogleSearchEnabled()).isTrue();
            assertThat(stateService.isTavilySearchEnabled()).isFalse();
        }

        @Test
        void appliesNone() {
            stateService.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            component.setSelectedProvider(null);
            configurable.apply();

            assertThat(stateService.getSelectedWebSearchProvider()).isNull();
        }

        @Test
        void appliesEveryProvidersCredentials_soSwitchingBackNeedsNoReentry() {
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("tavily-key-123");
            component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("google-key-456");
            component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).setText("csi-key-789");
            configurable.apply();

            assertThat(stateService.getTavilySearchKey()).isEqualTo("tavily-key-123");
            assertThat(stateService.getGoogleSearchKey()).isEqualTo("google-key-456");
            assertThat(stateService.getGoogleCSIKey()).isEqualTo("csi-key-789");
        }

        @Test
        void clearingAKey_removesIt() {
            stateService.setTavilySearchKey("old-key");
            configurable.reset();
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("");
            configurable.apply();

            assertThat(stateService.getTavilySearchKey()).isEmpty();
        }

        @Test
        void appliesMaxSearchResults() {
            component.getMaxSearchResults().setNumber(7);
            configurable.apply();
            assertThat(stateService.getMaxSearchResults()).isEqualTo(7);
        }

        @Test
        void afterApply_nothingIsModified() {
            component.setSelectedProvider(WebSearchProvider.TAVILY);
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("k");
            component.getMaxSearchResults().setNumber(4);
            configurable.apply();

            assertThat(configurable.isModified()).isFalse();
        }
    }

    @Nested
    class Reset {

        @Test
        void resetsFormFromSettings() {
            stateService.setIsWebSearchEnabled(true);
            stateService.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE);
            stateService.setGoogleSearchKey("g-key");
            stateService.setGoogleCSIKey("cse-id");
            stateService.setTavilySearchKey("t-key");
            stateService.setMaxSearchResults(8);

            configurable.reset();

            assertThat(component.getEnableWebSearchCheckbox().isSelected()).isTrue();
            assertThat(component.getSelectedProvider()).isEqualTo(WebSearchProvider.GOOGLE);
            assertThat(new String(component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).getPassword())).isEqualTo("g-key");
            assertThat(new String(component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).getPassword())).isEqualTo("cse-id");
            assertThat(new String(component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).getPassword())).isEqualTo("t-key");
            assertThat(component.getMaxSearchResults().getNumber()).isEqualTo(8);
            assertThat(configurable.isModified()).isFalse();
        }

        @Test
        void resetDiscardsUnappliedEdits() {
            stateService.setSelectedWebSearchProvider(WebSearchProvider.TAVILY);
            configurable.reset();
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("unsaved");

            configurable.reset();

            assertThat(component.getSelectedProvider()).isEqualTo(WebSearchProvider.TAVILY);
            assertThat(component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).getPassword()).isEmpty();
        }
    }

    @Nested
    class IsModified {

        @Test
        void detectsEnableSwitchChange() {
            configurable.reset();
            component.getEnableWebSearchCheckbox().setSelected(!component.getEnableWebSearchCheckbox().isSelected());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void detectsProviderChange() {
            configurable.reset();
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void detectsCredentialChange_forEveryRegisteredField() {
            for (CredentialKey key : new CredentialKey[]{CredentialKey.TAVILY_SEARCH_KEY,
                    CredentialKey.GOOGLE_SEARCH_KEY, CredentialKey.GOOGLE_CSI_KEY, CredentialKey.KEENABLE_SEARCH_KEY}) {
                configurable.reset();
                component.getCredentialField(key).setText("changed-" + key);
                assertThat(configurable.isModified()).as("%s change detected", key).isTrue();
            }
        }

        @Test
        void detectsMaxSearchResultsChange() {
            configurable.reset();
            component.getMaxSearchResults().setNumber(component.getMaxSearchResults().getNumber() == 3 ? 4 : 3);
            assertThat(configurable.isModified()).isTrue();
        }
    }

    @Nested
    class TestSearch {

        @Test
        void formConfig_reflectsTheUnsavedForm() {
            component.setSelectedProvider(WebSearchProvider.TAVILY);
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("unsaved-key");
            component.getMaxSearchResults().setNumber(2);

            var config = component.formConfig();

            assertThat(config.provider()).isEqualTo(WebSearchProvider.TAVILY);
            assertThat(config.credential(CredentialKey.TAVILY_SEARCH_KEY)).isEqualTo("unsaved-key");
            assertThat(config.credentials()).doesNotContainKey(CredentialKey.GOOGLE_SEARCH_KEY);
            assertThat(config.maxResults()).isEqualTo(2);
        }

        @Test
        void e2e_testSearchUsesTheUnsavedProviderAndKey_againstTheStub() throws Exception {
            stateService.setSelectedWebSearchProvider(WebSearchProvider.GOOGLE); // saved choice differs
            try (TavilyStubServer tavily = new TavilyStubServer()) {
                tavily.respondWithHit("Stub hit", "https://example.com/stub", "Snippet from the stub.");
                component.setSelectedProvider(WebSearchProvider.TAVILY);
                component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("unsaved-tavily-key");

                String output = WebSearchProvidersComponent.performTestSearch(component.formConfig(), "devoxx genie");

                assertThat(output).contains("Stub hit").contains("https://example.com/stub").contains("Snippet from the stub.");
                assertThat(tavily.takeRequest().getBody().readUtf8()).contains("unsaved-tavily-key").contains("devoxx genie");
            }
        }

        @Test
        void e2e_testSearchWithGoogleSelected_neverContactsTavily() throws Exception {
            try (TavilyStubServer tavily = new TavilyStubServer()) {
                WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE,
                        request -> WebSearchServiceTestSupport.results("Google hit", "https://example.com/g"));
                component.setSelectedProvider(WebSearchProvider.GOOGLE);
                component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("g");
                component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).setText("c");
                component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("tavily-present-but-unselected");

                String output = WebSearchProvidersComponent.performTestSearch(component.formConfig(), "q");

                assertThat(output).contains("Google hit");
                assertThat(tavily.requestCount()).isZero();
            }
        }

        @Test
        void e2e_providerError_isShownWithoutLeakingTheResponse() throws Exception {
            try (TavilyStubServer tavily = new TavilyStubServer()) {
                tavily.respondWithStatus(401, "{\"detail\":\"secret-echo tvly-leaky-key\"}");
                component.setSelectedProvider(WebSearchProvider.TAVILY);
                component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("tvly-leaky-key");

                String output = WebSearchProvidersComponent.performTestSearch(component.formConfig(), "q");

                assertThat(output).startsWith("Search with Tavily failed").doesNotContain("tvly-leaky-key");
            }
        }

        @Test
        void testSearchWithMissingKey_explainsWhatIsMissing_andSendsNothing() throws Exception {
            try (TavilyStubServer tavily = new TavilyStubServer()) {
                component.setSelectedProvider(WebSearchProvider.GOOGLE);
                component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("g");

                String output = WebSearchProvidersComponent.performTestSearch(component.formConfig(), "q");

                assertThat(output).contains("Google").contains("Search Engine ID");
                assertThat(tavily.requestCount()).isZero();
            }
        }

        @Test
        void e2e_failingGoogleTestSearch_pointsToTheDeprecation() {
            WebSearchServiceTestSupport.replaceEngine(WebSearchProvider.GOOGLE, request -> {
                throw new IllegalStateException("API not enabled");
            });
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("g");
            component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).setText("c");

            String output = WebSearchProvidersComponent.performTestSearch(component.formConfig(), "q");

            assertThat(output).contains("Google Custom Search failed").contains("January 1, 2027")
                    .doesNotContain("API not enabled");
        }

        @Test
        void testSearchWithNoProvider_asksToChooseOne() {
            component.setSelectedProvider(null);

            assertThat(WebSearchProvidersComponent.performTestSearch(component.formConfig(), "q"))
                    .contains("No web search provider is selected");
        }
    }

    @Nested
    class RoundTrip {

        @Test
        void applyThenRestart_restoresProviderAndCredentials() {
            component.getEnableWebSearchCheckbox().setSelected(true);
            component.setSelectedProvider(WebSearchProvider.GOOGLE);
            component.getCredentialField(CredentialKey.GOOGLE_SEARCH_KEY).setText("g-key");
            component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).setText("cse-id");
            component.getCredentialField(CredentialKey.TAVILY_SEARCH_KEY).setText("t-key");
            component.getMaxSearchResults().setNumber(6);
            configurable.apply();

            // "Restart": serialize the saved settings, load them into a fresh state, reopen the page.
            DevoxxGenieStateService deserialized = XmlSerializer.deserialize(
                    XmlSerializer.serialize(stateService.getState()), DevoxxGenieStateService.class);
            DevoxxGenieStateService restarted = new DevoxxGenieStateService();
            restarted.loadState(deserialized);
            stateService = restarted;
            stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(restarted);
            newConfigurable();
            configurable.reset();

            assertThat(restarted.getSelectedWebSearchProvider()).isEqualTo(WebSearchProvider.GOOGLE);
            assertThat(restarted.getGoogleSearchKey()).isEqualTo("g-key");
            assertThat(restarted.getGoogleCSIKey()).isEqualTo("cse-id");
            assertThat(restarted.getTavilySearchKey()).isEqualTo("t-key");
            assertThat(restarted.getMaxSearchResults()).isEqualTo(6);
            assertThat(component.getSelectedProvider()).isEqualTo(WebSearchProvider.GOOGLE);
            assertThat(new String(component.getCredentialField(CredentialKey.GOOGLE_CSI_KEY).getPassword())).isEqualTo("cse-id");
            assertThat(configurable.isModified()).isFalse();
        }
    }

    private WebSearchProvidersComponent getComponent() {
        try {
            java.lang.reflect.Field field = WebSearchProvidersConfigurable.class.getDeclaredField("webSearchProvidersComponent");
            field.setAccessible(true);
            return (WebSearchProvidersComponent) field.get(configurable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
