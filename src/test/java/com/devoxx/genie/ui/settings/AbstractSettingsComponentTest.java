package com.devoxx.genie.ui.settings;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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

import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractSettingsComponentTest {

    @Mock
    private Application application;

    private MockedStatic<ApplicationManager> applicationManagerMockedStatic;
    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;

    private DevoxxGenieStateService stateService;
    private TestSettingsComponent component;

    @BeforeEach
    void setUp() {
        stateService = new DevoxxGenieStateService();

        applicationManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        lenient().when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        component = new TestSettingsComponent();
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        applicationManagerMockedStatic.close();
    }

    @Test
    void shouldReturnPanelFromCreatePanel() {
        JPanel panel = component.createPanel();
        assertThat(panel).isNotNull();
        assertThat(panel.getLayout()).isInstanceOf(BorderLayout.class);
    }

    @Test
    void shouldHaveDefaultEmptyAddListeners() {
        // addListeners is a no-op by default - should not throw
        component.addListeners();
    }

    @Test
    void shouldHaveConstants() {
        assertThat(AbstractSettingsComponent.LINK_EMOJI).isNotEmpty();
        assertThat(AbstractSettingsComponent.PASSWORD_EMOJI).isNotEmpty();
    }

    @Nested
    class AddSection {

        @Test
        void shouldAddSectionToPanel() {
            JPanel testPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            component.callAddSection(testPanel, gbc, "Test Section");

            // After adding a section, gridy should be incremented
            assertThat(gbc.gridy).isGreaterThan(0);
            // Panel should have at least one component
            assertThat(testPanel.getComponentCount()).isGreaterThan(0);
        }
    }

    @Nested
    class AddSettingRow {

        @Test
        void shouldAddLabelAndComponentRow() {
            JPanel testPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JTextField field = new JTextField();
            component.callAddSettingRow(testPanel, gbc, "Setting Label", field);

            // Should add label + component = 2 components
            assertThat(testPanel.getComponentCount()).isEqualTo(2);
            assertThat(gbc.gridy).isEqualTo(1);
        }

        @Test
        void shouldAddLabelOnlyRow() {
            JPanel testPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            component.callAddSettingRowLabelOnly(testPanel, gbc, "Info text");

            assertThat(testPanel.getComponentCount()).isEqualTo(1);
            assertThat(gbc.gridy).isEqualTo(1);
        }
    }

    @Nested
    class AddProviderSettingRow {

        @Test
        void shouldAddCheckboxProviderRow() {
            JPanel testPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JCheckBox checkbox = new JCheckBox("Enable");
            component.callAddProviderSettingRow(testPanel, gbc, "Provider", checkbox);

            // Should add label + provider panel (which contains checkbox) = 2 components
            assertThat(testPanel.getComponentCount()).isEqualTo(2);
        }

        @Test
        void shouldAddCheckboxWithUrlProviderRow() {
            JPanel testPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JCheckBox checkbox = new JCheckBox("Enable");
            JTextField urlField = new JTextField("http://localhost:8080");
            component.callAddProviderSettingRowWithUrl(testPanel, gbc, "Provider", checkbox, urlField);

            // Should add label + provider panel (which contains checkbox + url) = 2 components
            assertThat(testPanel.getComponentCount()).isEqualTo(2);
        }
    }

    @Nested
    class CreateTextWithButtons {

        @Test
        void shouldCreatePasswordButtonPanel() {
            JTextField field = new JTextField();
            JComponent result = component.callCreateTextWithPasswordButton(field, "https://example.com");

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(JPanel.class);
        }

        @Test
        void shouldCreateLinkButtonPanel() {
            JTextField field = new JTextField();
            JComponent result = component.callCreateTextWithLinkButton(field, "https://example.com");

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(JPanel.class);
        }
    }

    /**
     * Concrete implementation of AbstractSettingsComponent for testing.
     * Exposes protected methods for testing.
     */
    private static class TestSettingsComponent extends AbstractSettingsComponent {

        void callAddSection(JPanel panel, GridBagConstraints gbc, String title) {
            addSection(panel, gbc, title);
        }

        void callAddSettingRow(JPanel panel, GridBagConstraints gbc, String label, JComponent component) {
            addSettingRow(panel, gbc, label, component);
        }

        void callAddSettingRowLabelOnly(JPanel panel, GridBagConstraints gbc, String label) {
            addSettingRow(panel, gbc, label);
        }

        void callAddProviderSettingRow(JPanel panel, GridBagConstraints gbc, String label, JCheckBox checkbox) {
            addProviderSettingRow(panel, gbc, label, checkbox);
        }

        void callAddProviderSettingRowWithUrl(JPanel panel, GridBagConstraints gbc, String label, JCheckBox checkbox, JComponent urlComponent) {
            addProviderSettingRow(panel, gbc, label, checkbox, urlComponent);
        }

        JComponent callCreateTextWithPasswordButton(JComponent component, String url) {
            return createTextWithPasswordButton(component, url);
        }

        JComponent callCreateTextWithLinkButton(JComponent component, String url) {
            return createTextWithLinkButton(component, url);
        }
    }
}
