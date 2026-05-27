package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SearchOptionsPanel logic.
 * <p>
 * After task-222 the panel hosts a single Web Search toggle — the RAG per-session toggle
 * was removed in favour of the master "Enable feature" checkbox in RAG settings.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchOptionsPanelTest {

    @Mock
    private Project project;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<ApplicationManager> appManagerMockedStatic;
    private SearchOptionsPanel createdPanel;

    @BeforeEach
    void setUp() {
        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        // Prevent NPE when IntelliJ UI infrastructure calls getService() during component construction
        lenient().when(application.getService(any(Class.class))).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        if (createdPanel != null) {
            createdPanel.getSwitches().forEach(InputSwitch::dispose);
            createdPanel = null;
        }
        stateServiceMockedStatic.close();
        appManagerMockedStatic.close();
    }

    private SearchOptionsPanel createPanel() {
        createdPanel = new SearchOptionsPanel(project);
        return createdPanel;
    }

    @Test
    void testConstructor_WebSearchDisabled_PanelNotVisible() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        assertThat(panel.isVisible()).isFalse();
    }

    @Test
    void testConstructor_WebSearchEnabled_PanelVisible() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        assertThat(panel.isVisible()).isTrue();
    }

    @Test
    void testConstructor_HasOnlyWebSwitch() {
        // RAG toggle was removed in task-222 — the panel hosts the Web switch only.
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches).hasSize(1);
    }

    @Test
    void testConstructor_WebSearchActivatedFromState_SwitchIsSelected() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(true);

        SearchOptionsPanel panel = createPanel();

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches.get(0).isSelected()).isTrue();
    }

    @Test
    void testGetPreferredSize_WhenNotVisible_ReturnsZero() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        assertThat(panel.getPreferredSize().height).isEqualTo(0);
        assertThat(panel.getPreferredSize().width).isEqualTo(0);
    }

    @Test
    void testGetMinimumSize_WhenNotVisible_ReturnsZeroHeight() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        assertThat(panel.getMinimumSize().height).isEqualTo(0);
    }

    @Test
    void testGetMinimumSize_WhenVisible_ReturnsDefaultHeight() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        assertThat(panel.getMinimumSize().height).isGreaterThan(0);
    }

    @Test
    void testUpdatePanelVisibility_AllSwitchesHidden_PanelHidden() {
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = createPanel();

        panel.getSwitches().forEach(sw -> sw.setVisible(false));
        panel.updatePanelVisibility();

        assertThat(panel.isVisible()).isFalse();
    }
}
