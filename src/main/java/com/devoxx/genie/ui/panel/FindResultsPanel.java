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
public class FindResultsPanel extends BackgroundPanel {
    private final JPanel resultsContainer;
    private final transient Project project;

    public FindResultsPanel(Project project,
                            @NotNull List<SemanticFile> results) {
        super("FindResultsPanel");

        this.project = project;
        setLayout(new BorderLayout());

        // Create header with search stats
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel header = new JLabel(String.format("Found %d relevant files", results.size()));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        headerPanel.add(header, BorderLayout.NORTH);

        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        // Add summary
        JLabel summary = new JLabel(String.format(
                "Showing %d matches max. with relevance scores above %.0f%%",
                stateService.getIndexerMaxResults(),
                stateService.getIndexerMinScore() * 100));
        summary.setForeground(JBColor.GRAY);
        headerPanel.add(summary, BorderLayout.NORTH);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(headerPanel, BorderLayout.NORTH);

        JLabel footerLabel = new JLabel("Set max. results, min. relevance score in plugin RAG settings.");
        footerLabel.setForeground(JBColor.GRAY);
        footerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(footerLabel, BorderLayout.SOUTH);

        // Create scrollable results container
        resultsContainer = new JPanel();
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));

        // Add results
        addResults(results);
        setMaximumSize(new Dimension(Short.MAX_VALUE, results.size() * 20));

        // Add scroll pane
        JBScrollPane scrollPane = new JBScrollPane(resultsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
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

            // Add file component with click handling
            FileEntryComponent fileComponent = new FileEntryComponent(project, result);
            resultPanel.add(fileComponent, BorderLayout.CENTER);

            resultsContainer.add(resultPanel);
        }
    }
}