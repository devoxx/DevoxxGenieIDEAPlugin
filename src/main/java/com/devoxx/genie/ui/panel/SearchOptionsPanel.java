package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.component.InputSwitch;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SearchOptionsPanel extends JPanel {
    private static final int DEFAULT_HEIGHT = JBUI.scale(30);
    private final List<InputSwitch> switches = new ArrayList<>();

    public SearchOptionsPanel(Project project) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0));
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        // RAG no longer has a per-session toggle here — it follows the master "Enable feature"
        // checkbox in RAG settings, and (in agent mode) the semantic_search tool can be turned
        // off individually under Agent Mode → Built-in Tools. Two toggles for the same thing
        // was a UX trap: people would enable RAG in settings, see nothing, and not realize
        // a second switch needed flipping. See task-222.

        InputSwitch webSearchSwitch = new InputSwitch(
                "Web",
                "Search the web for additional information"
        );

        switches.add(webSearchSwitch);

        updateInitialVisibility(stateService);

        webSearchSwitch.setSelected(stateService.getWebSearchActivated());

        webSearchSwitch.addEventSelected(selected -> {
            stateService.setWebSearchActivated(selected);
            updatePanelVisibility();
        });

        add(webSearchSwitch);

        setBorder(JBUI.Borders.empty(5, 10));
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
        switches.get(0).setVisible(stateService.getIsWebSearchEnabled());
        updatePanelVisibility();
    }
}
