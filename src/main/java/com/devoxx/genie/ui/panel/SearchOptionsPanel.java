package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.listener.GitDiffStateListener;
import com.devoxx.genie.ui.listener.SemanticSearchStateListener;
import com.devoxx.genie.ui.listener.WebSearchStateListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SearchOptionsPanel extends JPanel {
    private final List<InputSwitch> switches = new ArrayList<>();
    private static final int DEFAULT_HEIGHT = JBUI.scale(30);

    public SearchOptionsPanel() {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
        setOpaque(false);

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Create switches
        InputSwitch semanticSearchSwitch = new InputSwitch(
                "RAG",
                "Enable RAG-enabled code search"
        );

        InputSwitch gitDiffSwitch = new InputSwitch(
                "Git Diff",
                "Show Git diff window to compare and merge code suggestions"
        );

        InputSwitch webSearchSwitch = new InputSwitch(
                "Web",
                "Search the web for additional information"
        );

        // Add switches to our list for tracking
        switches.add(semanticSearchSwitch);
        switches.add(gitDiffSwitch);
        switches.add(webSearchSwitch);

        // Initialize visibility based on state service
        updateInitialVisibility(stateService);

        // Load saved state for enabled switches
        semanticSearchSwitch.setSelected(stateService.getRagEnabled());
        gitDiffSwitch.setSelected(stateService.getGitDiffEnabled());
        webSearchSwitch.setSelected(stateService.getEnableWebSearch());

        // Ensure only one switch is initially active
        enforceInitialSingleSelection();

        // Add state change listeners with mutual exclusion
        semanticSearchSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(semanticSearchSwitch);
            }
            stateService.setRagActivated(selected);
            updatePanelVisibility();
        });

        gitDiffSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(gitDiffSwitch);
            }
            stateService.setGitDiffActivated(selected);
            updatePanelVisibility();
        });

        webSearchSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(webSearchSwitch);
            }
            stateService.setWebSearchActivated(selected);
            updatePanelVisibility();
        });

        // Set up message bus listeners for visibility changes
        setupMessageBusListeners();

        // Add components
        add(semanticSearchSwitch);
        add(gitDiffSwitch);
        add(webSearchSwitch);

        // Add some padding
        setBorder(JBUI.Borders.empty(5, 10));

        // Update panel visibility based on initial state
        updatePanelVisibility();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, shouldBeVisible() ? DEFAULT_HEIGHT : 0);
    }

    @Override
    public Dimension getPreferredSize() {
        if (!shouldBeVisible()) {
            return new Dimension(0, 0);
        }
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, DEFAULT_HEIGHT);
    }

    private boolean shouldBeVisible() {
        return switches.stream().anyMatch(sw -> sw.isVisible());
    }

    private void updatePanelVisibility() {
        setVisible(shouldBeVisible());
        revalidate();
        repaint();
    }

    private void updateInitialVisibility(DevoxxGenieStateService stateService) {
        // Set initial visibility based on state service
        switches.get(0).setVisible(stateService.getRagEnabled());
        switches.get(1).setVisible(stateService.getGitDiffEnabled());
        switches.get(2).setVisible(stateService.getEnableWebSearch());

        // Update panel visibility
        updatePanelVisibility();
    }

    private void setupMessageBusListeners() {
        Application application = ApplicationManager.getApplication();
        MessageBusConnection connect = application.getMessageBus().connect();

        // Subscribe to state changes and update both visibility and selection
        connect.subscribe(AppTopics.SEMANTIC_SEARCH_STATE_TOPIC,
                (SemanticSearchStateListener) enabled -> {
                    InputSwitch semanticSwitch = switches.get(0);
                    semanticSwitch.setVisible(enabled);
                    semanticSwitch.setSelected(enabled);
                    updatePanelVisibility();
                });

        connect.subscribe(AppTopics.GITDIFF_STATE_TOPIC,
                (GitDiffStateListener) enabled -> {
                    InputSwitch gitDiffSwitch = switches.get(1);
                    gitDiffSwitch.setVisible(enabled);
                    gitDiffSwitch.setSelected(enabled);
                    updatePanelVisibility();
                });

        connect.subscribe(AppTopics.WEB_SEARCH_STATE_TOPIC,
                (WebSearchStateListener) enabled -> {
                    InputSwitch webSearchSwitch = switches.get(2);
                    webSearchSwitch.setVisible(enabled);
                    webSearchSwitch.setSelected(enabled);
                    updatePanelVisibility();
                });
    }

    private void deactivateOtherSwitches(InputSwitch activeSwitch) {
        switches.stream()
                .filter(sw -> sw != activeSwitch && sw.isVisible())
                .forEach(sw -> sw.setSelected(false));
    }

    private void enforceInitialSingleSelection() {
        // Find the first active and visible switch
        switches.stream()
                .filter(sw -> sw.isSelected() && sw.isVisible())
                .findFirst()
                .ifPresent(this::deactivateOtherSwitches);
    }
}