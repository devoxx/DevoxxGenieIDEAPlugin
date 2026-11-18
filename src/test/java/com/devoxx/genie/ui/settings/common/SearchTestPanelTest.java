package com.devoxx.genie.ui.settings.common;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** TASK-277.7: the shared Settings test-search block (Web search and RAG). */
class SearchTestPanelTest {

    /** Echoes the query back as results; records what was opened. */
    private static class FakeAdapter implements SearchTestPanel.Adapter<String> {
        final List<String> opened = new ArrayList<>();
        volatile SearchTestOutcome<String> next = SearchTestOutcome.results("2 results", List.of("first", "second"));
        volatile CountDownLatch release = new CountDownLatch(0);
        int prepared;

        @Override public @NotNull String placeholder() { return "type here"; }
        @Override public @NotNull String busyMessage() { return "Searching fake…"; }

        @Override
        public @NotNull Supplier<SearchTestOutcome<String>> prepare(@NotNull String query) {
            prepared++;
            return () -> {
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return next;
            };
        }

        @Override public void renderRow(@NotNull SimpleColoredComponent row, @NotNull String item) { row.append(item); }
        @Override public @Nullable String rowTooltip(@NotNull String item) { return null; }
        @Override public @NotNull String previewTitle(@NotNull String item) { return "title of " + item; }
        @Override public @NotNull String previewText(@NotNull String item) { return "body of " + item; }
        @Override public void open(@NotNull String item) { opened.add(item); }
        @Override public @NotNull String openHint() { return "open it"; }
    }

    private final FakeAdapter adapter = new FakeAdapter();

    private SearchTestPanel<String> panel() throws Exception {
        List<SearchTestPanel<String>> created = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> created.add(new SearchTestPanel<>("Test", adapter)));
        return created.get(0);
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    @Test
    void outputIsHiddenUntilTheFirstSearch() throws Exception {
        SearchTestPanel<String> panel = panel();

        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.getQueryField().getEmptyText().getText()).isEqualTo("type here");
    }

    @Test
    void emptyQueryCannotBeRun() throws Exception {
        SearchTestPanel<String> panel = panel();

        onEdt(() -> panel.getQueryField().setText("   "));
        assertThat(panel.searchButton().isEnabled()).isFalse();
        onEdt(panel::run);
        assertThat(adapter.prepared).isZero();

        onEdt(() -> panel.getQueryField().setText("devoxx"));
        assertThat(panel.searchButton().isEnabled()).isTrue();
    }

    @Test
    void runningSearchShowsBusyAndBlocksASecondSearch() throws Exception {
        SearchTestPanel<String> panel = panel();
        adapter.release = new CountDownLatch(1);
        onEdt(() -> panel.getQueryField().setText("devoxx"));

        onEdt(panel::run);
        onEdt(() -> {
            assertThat(panel.statusText()).isEqualTo("Searching fake…");
            assertThat(panel.searchButton().isEnabled()).isFalse();
            panel.run(); // Enter pressed again while busy
        });
        assertThat(adapter.prepared).isEqualTo(1);

        adapter.release.countDown();
        waitUntil(() -> panel.searchButton().isEnabled());
        onEdt(() -> assertThat(panel.statusText()).startsWith("2 results · "));
    }

    @Test
    void resultsShowSummaryWithElapsedTimeAndPreviewTheFirstResult() throws Exception {
        SearchTestPanel<String> panel = panel();

        onEdt(() -> panel.showOutcome(adapter.next, Duration.ofMillis(830)));

        assertThat(panel.isVisible()).isTrue();
        assertThat(panel.isShowingResults()).isTrue();
        assertThat(panel.resultCount()).isEqualTo(2);
        assertThat(panel.statusText()).isEqualTo("2 results · 0.8 s");
        assertThat(panel.statusIcon()).isEqualTo(AllIcons.General.InspectionsOK);
        assertThat(panel.previewTitleText()).isEqualTo("title of first");
        assertThat(panel.previewBodyText()).isEqualTo("body of first");
    }

    @Test
    void openingUsesTheSelectedResult() throws Exception {
        SearchTestPanel<String> panel = panel();

        onEdt(() -> {
            panel.showOutcome(adapter.next, Duration.ZERO);
            panel.openSelectedForTest();
        });

        assertThat(adapter.opened).containsExactly("first");
    }

    @Test
    void errorsAreShownAsErrorsWithoutResults() throws Exception {
        SearchTestPanel<String> panel = panel();
        onEdt(() -> panel.showOutcome(adapter.next, Duration.ZERO));

        onEdt(() -> panel.showOutcome(SearchTestOutcome.error("Tavily rejected the API key (HTTP 401)."), Duration.ZERO));

        assertThat(panel.isShowingResults()).isFalse();
        assertThat(panel.resultCount()).isZero();
        assertThat(panel.statusText()).isEqualTo("Tavily rejected the API key (HTTP 401).");
        assertThat(panel.statusIcon()).isEqualTo(AllIcons.General.Error);
    }

    @Test
    void noResultsIsInformationNotAnError() throws Exception {
        SearchTestPanel<String> panel = panel();

        onEdt(() -> panel.showOutcome(SearchTestOutcome.empty("Nothing found."), Duration.ZERO));

        assertThat(panel.isShowingResults()).isFalse();
        assertThat(panel.statusText()).isEqualTo("Nothing found.");
        assertThat(panel.statusIcon()).isEqualTo(AllIcons.General.Information);
    }

    @Test
    void failingSearchBecomesAnErrorWithoutItsMessage() throws Exception {
        SearchTestPanel.Adapter<String> throwing = new FakeAdapter() {
            @Override
            public @NotNull Supplier<SearchTestOutcome<String>> prepare(@NotNull String query) {
                return () -> {
                    throw new IllegalStateException("https://api.example.com/?key=secret");
                };
            }
        };
        List<SearchTestPanel<String>> created = new ArrayList<>();
        onEdt(() -> created.add(new SearchTestPanel<>("Test", throwing)));
        SearchTestPanel<String> panel = created.get(0);

        onEdt(() -> {
            panel.getQueryField().setText("q");
            panel.run();
        });
        waitUntil(() -> panel.searchButton().isEnabled());

        onEdt(() -> assertThat(panel.statusText())
                .isEqualTo("Search failed (IllegalStateException).").doesNotContain("secret"));
    }

    @Test
    void providerTextIsNeverRenderedAsHtml() throws Exception {
        SearchTestPanel<String> panel = panel();

        onEdt(() -> panel.showOutcome(SearchTestOutcome.error("<html><img src=x>"), Duration.ZERO));

        assertThat(panel.statusText()).doesNotStartWith("<html");
    }

    @Test
    void elapsedTimeUsesOneDecimalInEveryLocale() {
        assertThat(SearchTestPanel.formatElapsed(Duration.ofMillis(1234))).isEqualTo("1.2 s");
        assertThat(SearchTestPanel.formatElapsed(Duration.ZERO)).isEqualTo("0.0 s");
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        boolean[] done = {false};
        while (System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> done[0] = condition.getAsBoolean());
            if (done[0]) return;
            Thread.onSpinWait();
        }
        throw new AssertionError("condition not met within 5 s");
    }
}
