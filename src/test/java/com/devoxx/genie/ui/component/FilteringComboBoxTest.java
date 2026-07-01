package com.devoxx.genie.ui.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FilteringComboBox#filter(List, String, Function)} — the case-insensitive
 * substring matcher behind the type-to-filter model dropdown. Kept free of Swing so it runs headless.
 */
class FilteringComboBoxTest {

    private static final Function<String, String> IDENTITY = s -> s;

    private static final List<String> NVIDIA_SAMPLE = List.of(
            "moonshotai/kimi-k2.6",
            "meta/llama-3.3-70b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "qwen/qwen3.5-397b-a17b",
            "deepseek-ai/deepseek-v4-pro",
            "baai/bge-m3");

    @Test
    void matchesSubstringCaseInsensitively() {
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "kim", IDENTITY))
                .containsExactly("moonshotai/kimi-k2.6");
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "KIM", IDENTITY))
                .containsExactly("moonshotai/kimi-k2.6");
    }

    @Test
    void matchesAnywhereInTheText() {
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "llama", IDENTITY))
                .containsExactly("meta/llama-3.3-70b-instruct", "nvidia/llama-3.1-nemotron-70b-instruct");
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "nemotron", IDENTITY))
                .containsExactly("nvidia/llama-3.1-nemotron-70b-instruct");
    }

    @Test
    void blankOrNullQueryReturnsEverything() {
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "", IDENTITY)).isEqualTo(NVIDIA_SAMPLE);
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "   ", IDENTITY)).isEqualTo(NVIDIA_SAMPLE);
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, null, IDENTITY)).isEqualTo(NVIDIA_SAMPLE);
    }

    @Test
    void queryIsTrimmedBeforeMatching() {
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "  qwen  ", IDENTITY))
                .containsExactly("qwen/qwen3.5-397b-a17b");
    }

    @Test
    void noMatchReturnsEmpty() {
        assertThat(FilteringComboBox.filter(NVIDIA_SAMPLE, "does-not-exist", IDENTITY)).isEmpty();
    }

    @Test
    void searchesAgainstTheExtractedTextNotToString() {
        record Model(String display, String id) { }
        List<Model> models = List.of(
                new Model("Moonshot v1 8K", "moonshot-v1-8k"),
                new Model("Kimi K2 Turbo", "kimi-k2-turbo-preview"));
        Function<Model, String> search = m -> m.display() + " " + m.id();

        // Matches on the human display name...
        assertThat(FilteringComboBox.filter(models, "moonshot", search)).hasSize(1);
        // ...and on the raw model id.
        assertThat(FilteringComboBox.filter(models, "turbo-preview", search))
                .singleElement().extracting(Model::id).isEqualTo("kimi-k2-turbo-preview");
    }

    @Test
    void filterReturnsANewListAndDoesNotMutateInput() {
        List<String> original = List.copyOf(NVIDIA_SAMPLE);
        FilteringComboBox.filter(NVIDIA_SAMPLE, "llama", IDENTITY);
        assertThat(NVIDIA_SAMPLE).isEqualTo(original);
    }

    /**
     * Reproduces the reported bug: typing successive characters must keep narrowing the list. The
     * item-count is reduced synchronously by the editor's document listener, so this is observable
     * without pumping the EDT (only the popup toggle is deferred, and is irrelevant here).
     */
    @Test
    void typingSuccessiveCharactersKeepsNarrowingTheModel() {
        FilteringComboBox<String> combo = new FilteringComboBox<>(Function.identity(), Function.identity());
        for (String id : NVIDIA_SAMPLE) {
            combo.addItem(id);
        }
        javax.swing.text.JTextComponent editor =
                (javax.swing.text.JTextComponent) combo.getEditor().getEditorComponent();

        assertThat(combo.getItemCount()).isEqualTo(NVIDIA_SAMPLE.size());

        editor.setText("k");                       // first char — matches "kimi" and "deepseek"
        assertThat(combo.getItemCount()).isEqualTo(2);

        editor.setText("ki");                      // second char — used to reset to the full list
        assertThat(combo.getItemCount()).isEqualTo(1);
        assertThat(combo.getItemAt(0)).isEqualTo("moonshotai/kimi-k2.6");

        editor.setText("kix");                     // no match
        assertThat(combo.getItemCount()).isZero();

        editor.setText("");                        // cleared — full list returns
        assertThat(combo.getItemCount()).isEqualTo(NVIDIA_SAMPLE.size());
    }

    /** getSelectedItem() must never return the raw typed String, even mid-filter. */
    @Test
    void selectedItemIsNeverTheTypedString() {
        FilteringComboBox<String> combo = new FilteringComboBox<>(Function.identity(), Function.identity());
        NVIDIA_SAMPLE.forEach(combo::addItem);
        javax.swing.text.JTextComponent editor =
                (javax.swing.text.JTextComponent) combo.getEditor().getEditorComponent();

        editor.setText("llama");                   // "llama" is a query, not one of the items
        Object selected = combo.getSelectedItem();
        // Never the raw typed query; either no committed selection (null) or a genuine item.
        assertThat(selected).isNotEqualTo("llama");
        if (selected != null) {
            assertThat(NVIDIA_SAMPLE).contains((String) selected);
        }
    }
}
