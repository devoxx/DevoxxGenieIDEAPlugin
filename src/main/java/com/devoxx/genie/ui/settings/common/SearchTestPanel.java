package com.devoxx.genie.ui.settings.common;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.NamedColorUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * The "test a search from Settings" block shared by Web Search and RAG settings: a query row
 * (field + button) and, below it, a status line plus a single-line result list with a preview of
 * the selected result. What is searched and how a result looks comes from an {@link Adapter}.
 *
 * <p>Rows are deliberately single-line: variable-height multi-line list cells flicker on JBR, so
 * the detail of a result lives in the preview instead.
 *
 * <p>Add {@link #getQueryRow()} as the "Test query" setting row and this panel as a full-width row
 * below it. The output stays hidden until the first search.
 */
public final class SearchTestPanel<T> extends JPanel {

    /** Everything specific to one kind of test search. */
    public interface Adapter<T> {

        /** Placeholder shown in the empty query field. */
        @NotNull String placeholder();

        /** Status text while the search runs, e.g. "Searching with Tavily…". Called on the EDT. */
        @NotNull String busyMessage();

        /**
         * Called on the EDT when a search starts, so any unsaved form state can be read there. The
         * returned supplier runs off the EDT and should turn its own failures into an
         * {@link SearchTestOutcome#error error outcome}.
         */
        @NotNull Supplier<SearchTestOutcome<T>> prepare(@NotNull String query);

        /** Renders one single-line row. Text is appended verbatim (never interpreted as HTML). */
        void renderRow(@NotNull SimpleColoredComponent row, @NotNull T item);

        @Nullable String rowTooltip(@NotNull T item);

        /** The preview heading; clicking it calls {@link #open}. */
        @NotNull String previewTitle(@NotNull T item);

        @NotNull String previewText(@NotNull T item);

        /** Opens the result (browser, editor, ...). Called on the EDT. */
        void open(@NotNull T item);

        /** Tooltip of the preview heading and the hint for double-clicking a row. */
        @NotNull String openHint();

        default boolean monospacePreview() {
            return false;
        }

        default int previewRows() {
            return 4;
        }

        /**
         * Status text when {@link #prepare}'s supplier throws despite the contract. The default
         * shows only the exception type, since messages can carry request URLs or keys.
         */
        default @NotNull String failureMessage(@NotNull Throwable error) {
            return "Search failed (" + error.getClass().getSimpleName() + ").";
        }
    }

    private static final int MAX_VISIBLE_ROWS = 6;

    private final Adapter<T> adapter;

    private final JBTextField queryField = new JBTextField();
    private final JButton searchButton;
    private final JPanel queryRow = new JPanel(new BorderLayout(JBUI.scale(5), 0));

    private final JBLabel status = new JBLabel();
    private final DefaultListModel<T> resultsModel = new DefaultListModel<>();
    private final JBList<T> results = new JBList<>(resultsModel);
    private final ActionLink previewTitle = new ActionLink("", (java.awt.event.ActionListener) e -> openSelected());
    private final JBTextArea previewText;
    private final JPanel resultsView = new JPanel(new BorderLayout(0, JBUI.scale(6)));

    private boolean running;

    public SearchTestPanel(@NotNull String buttonText, @NotNull Adapter<T> adapter) {
        super(new BorderLayout(0, JBUI.scale(6)));
        this.adapter = adapter;
        this.searchButton = new JButton(buttonText);
        this.previewText = new JBTextArea(adapter.previewRows(), 40);

        queryField.getEmptyText().setText(adapter.placeholder());
        queryRow.add(queryField, BorderLayout.CENTER);
        queryRow.add(searchButton, BorderLayout.EAST);

        status.setIconTextGap(JBUI.scale(6));

        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer(new RowRenderer());
        results.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPreview(results.getSelectedValue());
        });

        previewTitle.setToolTipText(adapter.openHint());
        previewText.setEditable(false);
        previewText.setMargin(JBUI.insets(6));
        if (adapter.monospacePreview()) {
            previewText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().getSize()));
        } else {
            previewText.setLineWrap(true);
            previewText.setWrapStyleWord(true);
        }

        JPanel previewPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.add(previewTitle, BorderLayout.WEST);
        previewPanel.add(titleRow, BorderLayout.NORTH);
        JBScrollPane previewScroll = new JBScrollPane(previewText);
        if (!adapter.monospacePreview()) {
            // Prose reads as text under its link, not as an input box.
            previewText.setOpaque(false);
            previewText.setMargin(JBUI.insets(2, 0));
            previewScroll.setOpaque(false);
            previewScroll.getViewport().setOpaque(false);
            previewScroll.setBorder(JBUI.Borders.empty());
        }
        previewPanel.add(previewScroll, BorderLayout.CENTER);

        resultsView.add(new JBScrollPane(results), BorderLayout.NORTH);
        resultsView.add(previewPanel, BorderLayout.CENTER);
        resultsView.setVisible(false);

        add(status, BorderLayout.NORTH);
        add(resultsView, BorderLayout.CENTER);
        setVisible(false);

        installListeners();
        updateButton();
    }

    /** The query field, e.g. to prefill a query for docs screenshots. */
    public @NotNull JBTextField getQueryField() {
        return queryField;
    }

    /** The query field and search button, for the "Test query" setting row. */
    public @NotNull JComponent getQueryRow() {
        return queryRow;
    }

    private void installListeners() {
        searchButton.addActionListener(e -> run());
        queryField.addActionListener(e -> run());
        queryField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateButton();
            }
        });
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                openSelected();
                return true;
            }
        }.installOn(results);
        results.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelected();
                    e.consume();
                }
            }
        });
    }

    /** Starts a search for the typed query, unless it is empty or a search is already running. */
    void run() {
        String query = queryField.getText().strip();
        if (query.isEmpty() || running) {
            return;
        }
        Supplier<SearchTestOutcome<T>> search = adapter.prepare(query);
        setRunning(true);
        showBusy(adapter.busyMessage());
        long start = System.nanoTime();
        new SwingWorker<SearchTestOutcome<T>, Void>() {
            @Override
            protected SearchTestOutcome<T> doInBackground() {
                return search.get();
            }

            @Override
            protected void done() {
                Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
                try {
                    showOutcome(get(), elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showOutcome(SearchTestOutcome.error("Search interrupted."), elapsed);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showOutcome(SearchTestOutcome.error(adapter.failureMessage(cause)), elapsed);
                } finally {
                    setRunning(false);
                }
            }
        }.execute();
    }

    private void setRunning(boolean running) {
        this.running = running;
        updateButton();
    }

    private void updateButton() {
        searchButton.setEnabled(!running && !queryField.getText().isBlank());
    }

    private void showBusy(@NotNull String message) {
        resultsModel.clear();
        resultsView.setVisible(false);
        setStatus(AnimatedIcon.Default.INSTANCE, message, UIUtil.getLabelForeground());
    }

    /**
     * Shows {@code outcome} in place of any previous results. Must be called on the EDT; public so
     * docs screenshots can show a result state without a live search.
     */
    public void showOutcome(@NotNull SearchTestOutcome<T> outcome, @NotNull Duration elapsed) {
        resultsModel.clear();
        switch (outcome.kind()) {
            case RESULTS -> {
                outcome.items().forEach(resultsModel::addElement);
                results.setVisibleRowCount(Math.min(outcome.items().size(), MAX_VISIBLE_ROWS));
                setStatus(AllIcons.General.InspectionsOK,
                        outcome.message() + " · " + formatElapsed(elapsed), UIUtil.getLabelForeground());
                resultsView.setVisible(true);
                results.setSelectedIndex(0);
            }
            case EMPTY -> {
                resultsView.setVisible(false);
                setStatus(AllIcons.General.Information, outcome.message(), UIUtil.getLabelForeground());
            }
            case ERROR -> {
                resultsView.setVisible(false);
                setStatus(AllIcons.General.Error, outcome.message(), NamedColorUtil.getErrorForeground());
            }
        }
    }

    private void setStatus(@NotNull Icon icon, @NotNull String text, @NotNull Color foreground) {
        status.setIcon(icon);
        // Plain text only: a message may quote provider output, which must not be read as HTML.
        status.setText(plain(text));
        status.setForeground(foreground);
        setVisible(true);
        revalidate();
        repaint();
    }

    private void showPreview(@Nullable T item) {
        if (item == null) {
            previewTitle.setText("");
            previewText.setText("");
            return;
        }
        previewTitle.setText(plain(adapter.previewTitle(item)));
        previewText.setText(adapter.previewText(item));
        previewText.setCaretPosition(0);
    }

    private void openSelected() {
        T item = results.getSelectedValue();
        if (item != null) {
            adapter.open(item);
        }
    }

    /** Swing renders label/button text starting with {@code <html>} as HTML; results are untrusted. */
    private static @NotNull String plain(@NotNull String text) {
        return text.regionMatches(true, 0, "<html", 0, 5) ? " " + text : text;
    }

    static @NotNull String formatElapsed(@NotNull Duration elapsed) {
        return String.format(Locale.ROOT, "%.1f s", elapsed.toMillis() / 1000.0);
    }

    // --- for tests ------------------------------------------------------------------------------


    @NotNull JButton searchButton() {
        return searchButton;
    }

    @NotNull String statusText() {
        return status.getText();
    }

    @Nullable Icon statusIcon() {
        return status.getIcon();
    }

    boolean isShowingResults() {
        return resultsView.isVisible();
    }

    int resultCount() {
        return resultsModel.size();
    }

    @NotNull String previewTitleText() {
        return previewTitle.getText();
    }

    @NotNull String previewBodyText() {
        return previewText.getText();
    }

    void openSelectedForTest() {
        openSelected();
    }

    private final class RowRenderer extends ColoredListCellRenderer<T> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends T> list, T item,
                                             int index, boolean selected, boolean hasFocus) {
            if (item == null) return;
            adapter.renderRow(this, item);
            String tooltip = adapter.rowTooltip(item);
            setToolTipText(tooltip == null ? adapter.openHint() : tooltip + " — " + adapter.openHint());
        }
    }
}
