package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.SearchResult;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

/**
 * Results of the "Test semantic search" section: one line per match, with the selected match's
 * chunk in a preview below. Hidden until the first search, so it takes no space on the page.
 */
final class RagSearchResultsPanel extends JPanel {

    private static final int MAX_VISIBLE_MATCHES = 6;
    private static final int PREVIEW_ROWS = 12;

    private final JBLabel message = new JBLabel();
    private final DefaultListModel<SearchResult> matchesModel = new DefaultListModel<>();
    private final JBList<SearchResult> matches = new JBList<>(matchesModel);
    private final JBLabel previewTitle = new JBLabel();
    private final JBTextArea preview = new JBTextArea(PREVIEW_ROWS, 40);
    private final JPanel resultsView = new JPanel(new BorderLayout(0, JBUI.scale(6)));

    private @Nullable String basePath;

    RagSearchResultsPanel() {
        super(new BorderLayout(0, JBUI.scale(6)));
        setVisible(false);

        matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matches.setCellRenderer(new MatchRenderer());
        matches.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPreview(matches.getSelectedValue());
        });

        preview.setEditable(false);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().getSize()));
        preview.setMargin(JBUI.insets(6));
        previewTitle.setForeground(JBUI.CurrentTheme.ContextHelp.FOREGROUND);

        JPanel previewPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        previewPanel.add(previewTitle, BorderLayout.NORTH);
        previewPanel.add(new JBScrollPane(preview), BorderLayout.CENTER);

        resultsView.add(new JBScrollPane(matches), BorderLayout.NORTH);
        resultsView.add(previewPanel, BorderLayout.CENTER);

        add(message, BorderLayout.NORTH);
        add(resultsView, BorderLayout.CENTER);
    }

    /** Shows a status or error message in place of any previous results. */
    void showMessage(@NotNull String text) {
        matchesModel.clear();
        message.setText(text);
        resultsView.setVisible(false);
        setVisible(true);
        revalidate();
        repaint();
    }

    void showResults(@NotNull String query, @NotNull List<SearchResult> results, @Nullable String projectBasePath) {
        if (results.isEmpty()) {
            showMessage(noMatchesMessage(query));
            return;
        }
        basePath = projectBasePath;
        matchesModel.clear();
        results.forEach(matchesModel::addElement);
        matches.setVisibleRowCount(Math.min(results.size(), MAX_VISIBLE_MATCHES));
        message.setText(summary(query, results.size()));
        resultsView.setVisible(true);
        setVisible(true);
        matches.setSelectedIndex(0);
        revalidate();
        repaint();
    }

    private void showPreview(@Nullable SearchResult match) {
        if (match == null) {
            previewTitle.setText("");
            preview.setText("");
            return;
        }
        previewTitle.setText(relativePath(basePath, match.filePath()));
        preview.setText(match.content() == null ? "" : match.content());
        preview.setCaretPosition(0);
    }

    static @NotNull String summary(@NotNull String query, int count) {
        return count + (count == 1 ? " match" : " matches") + " for \"" + query + "\", best first";
    }

    static @NotNull String noMatchesMessage(@NotNull String query) {
        return "No matches for \"" + query + "\". Check that this project is indexed, or lower the minimum score.";
    }

    static @NotNull String formatScore(@Nullable Double score) {
        return score == null ? "-" : String.format(Locale.ROOT, "%.2f", score);
    }

    /** The path below the project root, or the whole path for a file outside the project. */
    static @NotNull String relativePath(@Nullable String basePath, @Nullable String filePath) {
        if (filePath == null) return "";
        String file = filePath.replace('\\', '/');
        if (basePath == null) return file;
        String base = basePath.replace('\\', '/');
        String prefix = base.endsWith("/") ? base : base + "/";
        return file.startsWith(prefix) ? file.substring(prefix.length()) : file;
    }

    private final class MatchRenderer extends ColoredListCellRenderer<SearchResult> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends SearchResult> list, SearchResult match,
                                             int index, boolean selected, boolean hasFocus) {
            String path = relativePath(basePath, match.filePath());
            int slash = path.lastIndexOf('/');
            append(formatScore(match.score()) + "   ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            append(path.substring(slash + 1), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            if (slash > 0) {
                append("   " + path.substring(0, slash), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            setToolTipText(match.filePath());
        }
    }
}
