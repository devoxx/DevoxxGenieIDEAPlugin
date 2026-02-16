package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSearchProvidersConfigurableTest {

    @Mock
    private Application application;

    @Mock
    private Project project;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private WebSearchProvidersConfigurable configurable;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        configurable = new WebSearchProvidersConfigurable(project);
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldHaveCorrectDisplayName() {
        assertThat(configurable.getDisplayName()).isEqualTo("Web Search");
    }

    @Test
    void shouldCreateComponent() {
        assertThat(configurable.createComponent()).isNotNull();
    }

    @Nested
    class Apply {

        @Test
        void shouldApplyWebSearchEnabled() {
            WebSearchProvidersComponent component = getComponent();
            component.getEnableWebSearchCheckbox().setSelected(true);
            configurable.apply();
            assertThat(stateService.getIsWebSearchEnabled()).isTrue();
        }

        @Test
        void shouldApplyWebSearchDisabled() {
            stateService.setIsWebSearchEnabled(true);
            WebSearchProvidersComponent component = getComponent();
            component.getEnableWebSearchCheckbox().setSelected(false);
            configurable.apply();
            assertThat(stateService.getIsWebSearchEnabled()).isFalse();
        }

        @Test
        void shouldApplyTavilyEnabled() {
            WebSearchProvidersComponent component = getComponent();
            component.getTavilySearchEnabledCheckBox().setSelected(true);
            configurable.apply();
            assertThat(stateService.isTavilySearchEnabled()).isTrue();
        }

        @Test
        void shouldApplyTavilyApiKey() {
            WebSearchProvidersComponent component = getComponent();
            component.getTavilySearchApiKeyField().setText("tavily-key-123");
            configurable.apply();
            assertThat(stateService.getTavilySearchKey()).isEqualTo("tavily-key-123");
        }

        @Test
        void shouldApplyGoogleSearchEnabled() {
            WebSearchProvidersComponent component = getComponent();
            component.getGoogleSearchEnabledCheckBox().setSelected(true);
            configurable.apply();
            assertThat(stateService.isGoogleSearchEnabled()).isTrue();
        }

        @Test
        void shouldApplyGoogleSearchApiKey() {
            WebSearchProvidersComponent component = getComponent();
            component.getGoogleSearchApiKeyField().setText("google-key-456");
            configurable.apply();
            assertThat(stateService.getGoogleSearchKey()).isEqualTo("google-key-456");
        }

        @Test
        void shouldApplyGoogleCSIKey() {
            WebSearchProvidersComponent component = getComponent();
            component.getGoogleCSIApiKeyField().setText("csi-key-789");
            configurable.apply();
            assertThat(stateService.getGoogleCSIKey()).isEqualTo("csi-key-789");
        }

        @Test
        void shouldApplyMaxSearchResults() {
            WebSearchProvidersComponent component = getComponent();
            component.getMaxSearchResults().setNumber(5);
            configurable.apply();
            assertThat(stateService.getMaxSearchResults()).isEqualTo(5);
        }
    }

    @Nested
    class Reset {

        @Test
        void shouldResetWebSearchEnabled() {
            stateService.setIsWebSearchEnabled(true);
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(component.getEnableWebSearchCheckbox().isSelected()).isTrue();
        }

        @Test
        void shouldResetTavilyEnabled() {
            stateService.setTavilySearchEnabled(true);
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(component.getTavilySearchEnabledCheckBox().isSelected()).isTrue();
        }

        @Test
        void shouldResetTavilyKey() {
            stateService.setTavilySearchKey("my-tavily-key");
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(new String(component.getTavilySearchApiKeyField().getPassword())).isEqualTo("my-tavily-key");
        }

        @Test
        void shouldResetGoogleSearchEnabled() {
            stateService.setGoogleSearchEnabled(true);
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(component.getGoogleSearchEnabledCheckBox().isSelected()).isTrue();
        }

        @Test
        void shouldResetGoogleSearchKey() {
            stateService.setGoogleSearchKey("google-key");
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(new String(component.getGoogleSearchApiKeyField().getPassword())).isEqualTo("google-key");
        }

        @Test
        void shouldResetGoogleCSIKey() {
            stateService.setGoogleCSIKey("csi-key");
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(new String(component.getGoogleCSIApiKeyField().getPassword())).isEqualTo("csi-key");
        }

        @Test
        void shouldResetMaxSearchResults() {
            stateService.setMaxSearchResults(7);
            configurable.reset();
            WebSearchProvidersComponent component = getComponent();
            assertThat(component.getMaxSearchResults().getNumber()).isEqualTo(7);
        }
    }

    @Nested
    class IsModified {

        @Test
        void shouldDetectWebSearchEnabledChange() {
            // Note: The WebSearchProvidersComponent listeners update the stateService
            // directly on checkbox change (setIsWebSearchEnabled), so the checkbox change
            // propagates to the stateService. To test isModified, we need to change the
            // state service value after the component was constructed.
            WebSearchProvidersComponent component = getComponent();
            // Change the state after construction, so component still shows old value
            stateService.setIsWebSearchEnabled(!component.getEnableWebSearchCheckbox().isSelected());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectTavilyEnabledChange() {
            WebSearchProvidersComponent component = getComponent();
            // Change the state directly, not the checkbox (to avoid listener side effects)
            stateService.setTavilySearchEnabled(!component.getTavilySearchEnabledCheckBox().isSelected());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectGoogleEnabledChange() {
            WebSearchProvidersComponent component = getComponent();
            // Change state directly to avoid listener side effects
            stateService.setGoogleSearchEnabled(!component.getGoogleSearchEnabledCheckBox().isSelected());
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectMaxSearchResultsChange() {
            stateService.setMaxSearchResults(3);
            WebSearchProvidersComponent component = getComponent();
            component.getMaxSearchResults().setNumber(8);
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectTavilyKeyChange() {
            stateService.setTavilySearchKey("old-key");
            WebSearchProvidersComponent component = getComponent();
            component.getTavilySearchApiKeyField().setText("new-key");
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectGoogleKeyChange() {
            stateService.setGoogleSearchKey("old-key");
            WebSearchProvidersComponent component = getComponent();
            component.getGoogleSearchApiKeyField().setText("new-key");
            assertThat(configurable.isModified()).isTrue();
        }

        @Test
        void shouldDetectGoogleCSIKeyChange() {
            stateService.setGoogleCSIKey("old-csi");
            WebSearchProvidersComponent component = getComponent();
            component.getGoogleCSIApiKeyField().setText("new-csi");
            assertThat(configurable.isModified()).isTrue();
        }
    }

    @Nested
    class WebSearchComponentListeners {

        @Test
        void shouldDisableTavilyWhenGoogleSelected() {
            WebSearchProvidersComponent component = getComponent();
            component.getEnableWebSearchCheckbox().setSelected(true);
            component.getTavilySearchEnabledCheckBox().setSelected(true);

            // Now select Google - should disable Tavily
            component.getGoogleSearchEnabledCheckBox().setSelected(true);
            assertThat(component.getTavilySearchEnabledCheckBox().isSelected()).isFalse();
        }

        @Test
        void shouldDisableGoogleWhenTavilySelected() {
            WebSearchProvidersComponent component = getComponent();
            component.getEnableWebSearchCheckbox().setSelected(true);
            component.getGoogleSearchEnabledCheckBox().setSelected(true);

            // Now select Tavily - should disable Google
            component.getTavilySearchEnabledCheckBox().setSelected(true);
            assertThat(component.getGoogleSearchEnabledCheckBox().isSelected()).isFalse();
        }

        @Test
        void shouldDisableBothProvidersWhenWebSearchDisabled() {
            WebSearchProvidersComponent component = getComponent();
            component.getEnableWebSearchCheckbox().setSelected(true);
            component.getTavilySearchEnabledCheckBox().setSelected(true);

            // Disable web search entirely
            component.getEnableWebSearchCheckbox().setSelected(false);
            assertThat(component.getTavilySearchEnabledCheckBox().isSelected()).isFalse();
            assertThat(component.getGoogleSearchEnabledCheckBox().isSelected()).isFalse();
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
