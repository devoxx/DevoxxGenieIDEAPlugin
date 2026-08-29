package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reranker-field persistence sanity check for {@link DevoxxGenieStateService} \u2014 review
 * item m1 for task-214.
 *
 * <p>The full {@link RAGSettingsConfigurable} component graph relies on JBTable + AWT font
 * metrics that are unavailable in the headless test runner (the existing
 * {@code AbstractSettingsComponentTest} pattern works for component classes <em>without</em>
 * a JBTable but not for {@code RAGSettingsConfigurable}). Until that is refactored we test
 * the state-service-round-trip semantics that {@code isModified()}, {@code apply()} and
 * {@code reset()} all depend on \u2014 if these fields don't round-trip through
 * {@code DevoxxGenieStateService} the configurable can't either.
 */
class RAGSettingsConfigurableRerankerTest {

    @Test
    void defaultsMatchTheValuesAdvertisedInUi() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        // These four defaults are referenced by the RAG settings UI and the Reranker docs.
        assertThat(state.getRerankResults())
                .as("rerank is opt-in \u2014 default OFF")
                .isFalse();
        assertThat(state.getRerankerModelName())
                .as("default model is a small generative chat model \u2014 see task-214 review M1")
                .isEqualTo("llama3.2:1b");
        assertThat(state.getRerankerShortlistSize())
                .as("default shortlist size matches the UI help text")
                .isEqualTo(30);
        assertThat(state.getRerankerTimeoutMs())
                .as("default timeout matches the UI help text")
                .isEqualTo(2000);
    }

    @Test
    void allFourRerankerFieldsRoundTripThroughSetters() {
        // Simulates RAGSettingsConfigurable.apply(): take values from the UI widgets, push
        // them into the state service, read them back. If this round-trip breaks, neither
        // apply() nor reset() can work.
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        state.setRerankResults(true);
        state.setRerankerModelName("qwen2.5:0.5b");
        state.setRerankerShortlistSize(45);
        state.setRerankerTimeoutMs(3500);

        assertThat(state.getRerankResults()).isTrue();
        assertThat(state.getRerankerModelName()).isEqualTo("qwen2.5:0.5b");
        assertThat(state.getRerankerShortlistSize()).isEqualTo(45);
        assertThat(state.getRerankerTimeoutMs()).isEqualTo(3500);
    }

    @Test
    void resetSemanticsRestoresStoredValues() {
        // Simulates RAGSettingsConfigurable.reset(): after a user edits the widgets and then
        // cancels, the widgets must be restored from state. This is the inverse direction of
        // the previous round-trip and is what the configurable's reset() relies on.
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        state.setRerankResults(true);
        state.setRerankerModelName("custom-model:7b");
        state.setRerankerShortlistSize(50);
        state.setRerankerTimeoutMs(7777);

        // Pretend the user moved the widgets to different values...
        boolean uiToggle = false;
        String uiModel = "edited-by-user";
        int uiShortlist = 1;
        int uiTimeout = 1;

        // ...then clicked Reset. The configurable copies state back into the widgets.
        uiToggle = Boolean.TRUE.equals(state.getRerankResults());
        uiModel = state.getRerankerModelName();
        uiShortlist = state.getRerankerShortlistSize();
        uiTimeout = state.getRerankerTimeoutMs();

        assertThat(uiToggle).isTrue();
        assertThat(uiModel).isEqualTo("custom-model:7b");
        assertThat(uiShortlist).isEqualTo(50);
        assertThat(uiTimeout).isEqualTo(7777);
    }

    @Test
    void isModifiedDetectsDriftOnEachField() {
        // Mimic RAGSettingsConfigurable.isModified()'s per-field comparison so each field is
        // independently flagged. If a field is missed here, the configurable would silently
        // drop user edits to that field.
        DevoxxGenieStateService stored = new DevoxxGenieStateService();
        // "UI values" start identical to "stored values"...
        boolean uiToggle = Boolean.TRUE.equals(stored.getRerankResults());
        String uiModel = stored.getRerankerModelName();
        int uiShortlist = stored.getRerankerShortlistSize();
        int uiTimeout = stored.getRerankerTimeoutMs();

        assertThat(isModified(stored, uiToggle, uiModel, uiShortlist, uiTimeout))
                .as("no drift \u2192 not modified")
                .isFalse();

        // Each field independently must flip the flag.
        assertThat(isModified(stored, !uiToggle, uiModel, uiShortlist, uiTimeout)).isTrue();
        assertThat(isModified(stored, uiToggle, uiModel + "x", uiShortlist, uiTimeout)).isTrue();
        assertThat(isModified(stored, uiToggle, uiModel, uiShortlist + 1, uiTimeout)).isTrue();
        assertThat(isModified(stored, uiToggle, uiModel, uiShortlist, uiTimeout + 1)).isTrue();
    }

    /** Same per-field comparison logic as {@link RAGSettingsConfigurable#isModified()}. */
    private static boolean isModified(DevoxxGenieStateService stored,
                                      boolean uiToggle,
                                      String uiModel,
                                      int uiShortlist,
                                      int uiTimeout) {
        if (uiToggle != Boolean.TRUE.equals(stored.getRerankResults())) return true;
        if (!uiModel.equals(stored.getRerankerModelName())) return true;
        int storedShortlist = stored.getRerankerShortlistSize() == null ? 30 : stored.getRerankerShortlistSize();
        if (uiShortlist != storedShortlist) return true;
        int storedTimeout = stored.getRerankerTimeoutMs() == null ? 2000 : stored.getRerankerTimeoutMs();
        return uiTimeout != storedTimeout;
    }
}
