package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SearchOptionsPanel extends JPanel {
    private static final int DEFAULT_HEIGHT = JBUI.scale(30);
    @Getter
    private final List<InputSwitch> switches = new ArrayList<>();

    public SearchOptionsPanel(Project project) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
        setOpaque(false);

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // Create switches
        InputSwitch ragSwitch = new InputSwitch(
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
        switches.add(ragSwitch);
        switches.add(gitDiffSwitch);
        switches.add(webSearchSwitch);

        // Initialize visibility based on state service
        updateInitialVisibility(stateService);

        // Load saved states for enabled switches
        ragSwitch.setSelected(stateService.getRagActivated());
        gitDiffSwitch.setSelected(stateService.getGitDiffActivated());
        webSearchSwitch.setSelected(stateService.getWebSearchActivated());

        // Ensure only one switch is initially active
        enforceInitialSingleSelection();

        // Add state change listeners with mutual exclusion
        ragSwitch.addEventSelected(selected -> {
            if (selected) {
                deactivateOtherSwitches(ragSwitch);
            }

            // Change input field placeholder based on RAG state
            project.getMessageBus()
                    .syncPublisher(AppTopics.RAG_ACTIVATED_CHANGED_TOPIC)
                    .onRAGStateChanged(selected);

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

        // Add components
        add(ragSwitch);
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
        return switches.stream().anyMatch(Component::isVisible);
    }

    public void updatePanelVisibility() {
        setVisible(shouldBeVisible());
        revalidate();
        repaint();
    }

    private void updateInitialVisibility(@NotNull DevoxxGenieStateService stateService) {
        // Set initial visibility based on state service
        switches.get(0).setVisible(stateService.getRagEnabled());
        switches.get(1).setVisible(stateService.getGitDiffEnabled());
        switches.get(2).setVisible(stateService.getIsWebSearchEnabled());

        // Update panel visibility
        updatePanelVisibility();
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