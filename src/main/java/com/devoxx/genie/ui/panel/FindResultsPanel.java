package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.ui.component.FileEntryComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
public class FindResultsPanel extends JPanel {
    private final JPanel resultsContainer;
    private final transient Project project;

    public FindResultsPanel(Project project, List<SemanticFile> results) {
        super(new BorderLayout());
        this.project = project;

        // Create header with search stats
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel header = new JLabel(String.format("Found %d relevant files", results.size()));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        headerPanel.add(header, BorderLayout.NORTH);

        // Add summary
        JLabel summary = new JLabel(String.format(
                "Showing matches with relevance scores above %.0f%%",
                DevoxxGenieStateService.getInstance().getIndexerMinScore() * 100));
        summary.setForeground(JBColor.GRAY);
        headerPanel.add(summary, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Create scrollable results container
        resultsContainer = new JPanel();
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));

        // Add results
        addResults(results);

        // Add scroll pane
        JBScrollPane scrollPane = new JBScrollPane(resultsContainer);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addResults(@NotNull List<SemanticFile> results) {
        // Sort results by score descending
        List<SemanticFile> sortedResults = results.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        for (SemanticFile result : sortedResults) {
            // Create result panel
            JPanel resultPanel = new JPanel(new BorderLayout());
            resultPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            // Add file component with click handling
            FileEntryComponent fileComponent = new FileEntryComponent(project, result);
            resultPanel.add(fileComponent, BorderLayout.CENTER);

            resultsContainer.add(resultPanel);
        }
    }
}