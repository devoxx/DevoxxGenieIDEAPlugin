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

    public SearchOptionsPanel() {
        super(new FlowLayout(FlowLayout.CENTER, JBUI.scale(10), 0));
        setOpaque(false);

        // Create switches
        InputSwitch semanticSearchSwitch = new InputSwitch(
                "Semantic",
                "Enable semantic code search in responses"
        );

        InputSwitch gitDiffSwitch = new InputSwitch(
                "GitDiff",
                "Show Git diff window to compare and merge code suggestions"
        );

        InputSwitch webSearchSwitch = new InputSwitch(
                "Web",
                "Search the web for additional information"
        );
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Add switches to our list for tracking
        switches.add(semanticSearchSwitch);
        switches.add(gitDiffSwitch);
        switches.add(webSearchSwitch);

        // Load saved state
        semanticSearchSwitch.setSelected(stateService.getSemanticSearchEnabled());
        gitDiffSwitch.setSelected(stateService.getGitDiffEnabled());
        webSearchSwitch.setSelected(stateService.getEnableWebSearch());

        // Ensure only one switch is initially active
        enforceInitialSingleSelection();

        // Add state change listeners with mutual exclusion
        semanticSearchSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(semanticSearchSwitch);
            }

            stateService.setSemanticSearchActivated(selected);
        });

        gitDiffSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(gitDiffSwitch);
            }
            stateService.setGitDiffActivated(selected);
        });

        webSearchSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(webSearchSwitch);
            }
            stateService.setWebSearchActivated(selected);
        });

        addMessageBusListener();

        // Add components
        add(semanticSearchSwitch);
        add(gitDiffSwitch);
        add(webSearchSwitch);

        // Add some padding
        setBorder(JBUI.Borders.empty(5, 10));

        // Set minimum size
        int minHeight = JBUI.scale(30);  // adjust this value as needed
        setMinimumSize(new Dimension(0, minHeight));
        setPreferredSize(new Dimension(getPreferredSize().width, minHeight));
    }

    private void addMessageBusListener() {
        Application application = ApplicationManager.getApplication();
        MessageBusConnection connect = application.getMessageBus().connect();

        connect.subscribe(AppTopics.SEMANTIC_SEARCH_STATE_TOPIC,
               (SemanticSearchStateListener) enabled -> switches.get(0).setSelected(enabled));

        connect.subscribe(AppTopics.GITDIFF_STATE_TOPIC,
                (GitDiffStateListener) enabled -> switches.get(1).setSelected(enabled));

        connect.subscribe(AppTopics.WEB_SEARCH_STATE_TOPIC,
                (WebSearchStateListener) enabled -> switches.get(2).setSelected(enabled));
    }

    private void deactivateOtherSwitches(InputSwitch activeSwitch) {
        switches.stream()
                .filter(sw -> sw != activeSwitch)
                .forEach(sw -> sw.setSelected(false));
    }

    private void enforceInitialSingleSelection() {
        // Find the first active switch
        switches.stream()
                .filter(InputSwitch::isSelected)
                .findFirst().ifPresent(this::deactivateOtherSwitches);

    }
}