package com.devoxx.genie.ui.component;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link ComboBox} that lets the user type to reduce its items to those whose search text
 * (case-insensitive substring) matches the typed query. Built for long lists — e.g. the 100+
 * NVIDIA models — where a plain dropdown is impractical.
 *
 * <p>The combo is editable so the query can be typed inline, but its editor is wired so that
 * {@link #getSelectedItem()} ALWAYS returns an item of type {@code T} (or {@code null}) — never the
 * raw typed {@code String}. Existing {@code (T) combo.getSelectedItem()} call sites therefore keep
 * working unchanged. Selection {@link ActionListener}s fire only on genuine user commits (clicking a
 * row / pressing Enter on a match), never while the list is being re-filtered.</p>
 *
 * <p>The popup rows still use whatever {@code ListCellRenderer} is installed; only the collapsed
 * field shows the plain {@code displayTextExtractor} text (the editor component).</p>
 *
 * @param <T> the element type
 */
public class FilteringComboBox<T> extends ComboBox<T> {

    private final transient Function<? super T, String> displayTextExtractor;
    private final transient Function<? super T, String> searchTextExtractor;

    /** The complete, unfiltered set of items; the model shown may be a filtered subset of this. */
    private final transient List<T> allItems = new ArrayList<>();

    private final JTextField editorField = new JTextField();

    /** Re-entrancy guard: {@code true} while we mutate the model/editor programmatically. */
    private boolean syncing = false;

    /** Last committed selection (an item of {@code T}); what {@link #getSelectedItem()} reflects. */
    private transient T committed = null;

    /**
     * @param displayTextExtractor produces the text shown in the collapsed field for a selected item
     * @param searchTextExtractor  produces the text a typed query is matched against (substring)
     */
    public FilteringComboBox(@NotNull Function<? super T, String> displayTextExtractor,
                             @NotNull Function<? super T, String> searchTextExtractor) {
        this.displayTextExtractor = displayTextExtractor;
        this.searchTextExtractor = searchTextExtractor;

        setEditable(true);
        setEditor(new FilterEditor());
        editorField.setBorder(BorderFactory.createEmptyBorder());

        editorField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onQueryChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQueryChanged(); }
        });

        // When the popup opens while the field still shows the committed selection (i.e. the user
        // clicked the arrow rather than typing a query), show the full list. During typing the field
        // holds the query — not the committed item — so this leaves the filtered subset intact.
        addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                if (!syncing && showsCommittedSelection() && getItemCount() != allItems.size()) {
                    rebuildModel(allItems);
                }
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { /* no-op */ }
            @Override public void popupMenuCanceled(PopupMenuEvent e) { /* no-op */ }
        });
    }

    /** True when the editor field shows the committed selection rather than an in-progress query. */
    private boolean showsCommittedSelection() {
        String text = editorField.getText();
        return committed == null ? text.isEmpty() : text.equals(displayTextExtractor.apply(committed));
    }

    /**
     * Case-insensitive substring filter. Package-private and static so the matching logic can be
     * unit-tested without instantiating Swing components.
     */
    static <T> List<T> filter(@NotNull List<T> items,
                              @Nullable String query,
                              @NotNull Function<? super T, String> searchTextExtractor) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(items);
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> {
                    String text = searchTextExtractor.apply(item);
                    return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
                })
                .collect(Collectors.toList());
    }

    private void onQueryChanged() {
        if (syncing) {
            return;
        }
        List<T> matches = filter(allItems, editorField.getText(), searchTextExtractor);
        rebuildModel(matches);
        // Reflect the new match set in the popup on the next EDT cycle (mutating popup visibility
        // directly inside a document event is unsafe). We only ever open/close it — never hide-then-
        // show — so no spurious popup events are generated while the user is typing. Guarded on
        // isShowing() since the popup can only be shown for a displayed combo.
        SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                setPopupVisible(getItemCount() > 0);
            }
        });
    }

    /**
     * Swap the displayed model to {@code items} without firing selection events to external listeners
     * and without disturbing the committed selection or the query text the user is typing.
     */
    private void rebuildModel(@NotNull List<T> items) {
        ActionListener[] listeners = getActionListeners();
        for (ActionListener listener : listeners) {
            removeActionListener(listener);
        }
        syncing = true;
        try {
            DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
            for (T item : items) {
                model.addElement(item);
            }
            setModel(model);
            // Keep the committed item as the selected object even when it is filtered out of view, so
            // getSelectedItem() stays a valid T (never null mid-typing, never a String).
            setSelectedItem(committed);
        } finally {
            syncing = false;
            for (ActionListener listener : listeners) {
                addActionListener(listener);
            }
        }
    }

    private void setEditorTextSilently(String text) {
        syncing = true;
        try {
            editorField.setText(text);
            editorField.setCaretPosition(editorField.getDocument().getLength());
        } finally {
            syncing = false;
        }
    }

    // --- Keep the master list in sync with the population calls existing code already uses ---

    @Override
    public void addItem(T item) {
        super.addItem(item);
        if (!syncing) {
            allItems.add(item);
        }
    }

    @Override
    public void removeAllItems() {
        super.removeAllItems();
        if (!syncing) {
            allItems.clear();
            committed = null;
            setEditorTextSilently("");
        }
    }

    /**
     * Editable-combo editor that renders a {@code T} as its display text but always returns the
     * underlying {@code T} (or {@code null}) from {@link #getItem()} — so committing the editor never
     * turns the combo's selected item into a raw {@code String}.
     */
    private final class FilterEditor implements ComboBoxEditor {

        @Override
        public Component getEditorComponent() {
            return editorField;
        }

        @Override
        public void setItem(Object anObject) {
            if (anObject == null) {
                committed = null;
                if (!syncing) {
                    setEditorTextSilently("");
                }
            } else if (!(anObject instanceof String)) {
                @SuppressWarnings("unchecked")
                T item = (T) anObject;
                committed = item;
                if (!syncing) {
                    setEditorTextSilently(displayTextExtractor.apply(item));
                }
            }
            // A raw String (shouldn't occur) is ignored so the committed T is never overwritten.
        }

        @Override
        public Object getItem() {
            return committed;
        }

        @Override
        public void selectAll() {
            editorField.selectAll();
            editorField.requestFocus();
        }

        @Override
        public void addActionListener(ActionListener l) {
            editorField.addActionListener(l);
        }

        @Override
        public void removeActionListener(ActionListener l) {
            editorField.removeActionListener(l);
        }
    }
}
