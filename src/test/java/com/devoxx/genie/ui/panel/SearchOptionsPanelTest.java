package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
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
import static org.mockito.Mockito.*;

/**
 * Tests for SearchOptionsPanel logic.
 * <p>
 * SearchOptionsPanel manages RAG and Web Search toggle switches.
 * Key logic: mutual exclusion (only one switch active at a time),
 * visibility based on state service, and panel visibility based on switch visibility.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchOptionsPanelTest {

    @Mock
    private Project project;

    @Mock
    private DevoxxGenieStateService stateService;

    @Mock
    private MessageBus messageBus;

    @Mock
    private MessageBusConnection messageBusConnection;

    @Mock
    private Application application;

    private MockedStatic<DevoxxGenieStateService> stateServiceMockedStatic;
    private MockedStatic<ApplicationManager> appManagerMockedStatic;

    @BeforeEach
    void setUp() {
        stateServiceMockedStatic = Mockito.mockStatic(DevoxxGenieStateService.class);
        stateServiceMockedStatic.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        appManagerMockedStatic = Mockito.mockStatic(ApplicationManager.class);
        appManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(application);
        // Prevent NPE when IntelliJ UI infrastructure calls getService() during component construction
        lenient().when(application.getService(any(Class.class))).thenReturn(null);

        when(project.getMessageBus()).thenReturn(messageBus);
        when(messageBus.syncPublisher(any())).thenReturn((com.devoxx.genie.ui.listener.RAGStateListener) selected -> {});
    }

    @AfterEach
    void tearDown() {
        stateServiceMockedStatic.close();
        appManagerMockedStatic.close();
    }

    @Test
    void testConstructor_BothDisabled_PanelNotVisible() {
        when(stateService.getRagEnabled()).thenReturn(false);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.isVisible()).isFalse();
    }

    @Test
    void testConstructor_RagEnabled_PanelVisible() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.isVisible()).isTrue();
    }

    @Test
    void testConstructor_WebSearchEnabled_PanelVisible() {
        when(stateService.getRagEnabled()).thenReturn(false);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.isVisible()).isTrue();
    }

    @Test
    void testConstructor_BothEnabled_PanelVisible() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.isVisible()).isTrue();
    }

    @Test
    void testConstructor_HasTwoSwitches() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches).hasSize(2);
    }

    @Test
    void testConstructor_RagActivatedFromState_SwitchIsSelected() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches.get(0).isSelected()).isTrue(); // RAG switch
        assertThat(switches.get(1).isSelected()).isFalse(); // Web switch
    }

    @Test
    void testConstructor_WebSearchActivatedFromState_SwitchIsSelected() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(true);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches.get(0).isSelected()).isFalse(); // RAG switch
        assertThat(switches.get(1).isSelected()).isTrue(); // Web switch
    }

    @Test
    void testConstructor_BothActivated_OnlyFirstRemains() {
        // When both are activated, enforceInitialSingleSelection keeps first one
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(true);
        when(stateService.getWebSearchActivated()).thenReturn(true);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        // enforceInitialSingleSelection finds first selected+visible, deactivates others
        assertThat(switches.get(0).isSelected()).isTrue(); // RAG stays
        assertThat(switches.get(1).isSelected()).isFalse(); // Web deactivated
    }

    @Test
    void testGetPreferredSize_WhenNotVisible_ReturnsZero() {
        when(stateService.getRagEnabled()).thenReturn(false);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.getPreferredSize().height).isEqualTo(0);
        assertThat(panel.getPreferredSize().width).isEqualTo(0);
    }

    @Test
    void testGetMinimumSize_WhenNotVisible_ReturnsZeroHeight() {
        when(stateService.getRagEnabled()).thenReturn(false);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.getMinimumSize().height).isEqualTo(0);
    }

    @Test
    void testGetMinimumSize_WhenVisible_ReturnsDefaultHeight() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        assertThat(panel.getMinimumSize().height).isGreaterThan(0);
    }

    @Test
    void testUpdatePanelVisibility_AllSwitchesHidden_PanelHidden() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        // Manually hide both switches
        panel.getSwitches().forEach(sw -> sw.setVisible(false));
        panel.updatePanelVisibility();

        assertThat(panel.isVisible()).isFalse();
    }

    @Test
    void testSwitchVisibility_OnlyRagEnabled() {
        when(stateService.getRagEnabled()).thenReturn(true);
        when(stateService.getIsWebSearchEnabled()).thenReturn(false);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches.get(0).isVisible()).isTrue(); // RAG visible
        assertThat(switches.get(1).isVisible()).isFalse(); // Web not visible
    }

    @Test
    void testSwitchVisibility_OnlyWebSearchEnabled() {
        when(stateService.getRagEnabled()).thenReturn(false);
        when(stateService.getIsWebSearchEnabled()).thenReturn(true);
        when(stateService.getRagActivated()).thenReturn(false);
        when(stateService.getWebSearchActivated()).thenReturn(false);

        SearchOptionsPanel panel = new SearchOptionsPanel(project);

        List<InputSwitch> switches = panel.getSwitches();
        assertThat(switches.get(0).isVisible()).isFalse(); // RAG not visible
        assertThat(switches.get(1).isVisible()).isTrue(); // Web visible
    }
}
