package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchProviderException;
import com.devoxx.genie.service.websearch.WebSearchReadiness;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.settings.common.SearchTestOutcome;
import com.devoxx.genie.ui.settings.common.SearchTestPanel;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Settings → Web search "Test search": runs the selected provider with the unsaved form and shows
 * each hit as a title + domain row, with the cleaned snippet and full URL in the preview.
 */
final class WebSearchTestSearch implements SearchTestPanel.Adapter<WebSearchTestSearch.Hit> {

    /** One result as shown in the test list. {@code snippet} is already cleaned for display. */
    record Hit(@NotNull String title, @Nullable String url, @NotNull String domain, @NotNull String snippet) {
    }

    private final Supplier<WebSearchConfig> formConfig;

    /** @param formConfig reads the settings as typed in the form (not yet applied); called on the EDT */
    WebSearchTestSearch(@NotNull Supplier<WebSearchConfig> formConfig) {
        this.formConfig = formConfig;
    }

    @Override
    public @NotNull String placeholder() {
        return "e.g. latest IntelliJ Platform release";
    }

    @Override
    public @NotNull String busyMessage() {
        WebSearchConfig config = formConfig.get();
        return config.provider() == null ? "Searching…"
                : "Searching with " + WebSearchService.definition(config.provider()).displayName() + "…";
    }

    @Override
    public @NotNull Supplier<SearchTestOutcome<Hit>> prepare(@NotNull String query) {
        // Snapshot the form on the EDT so newly entered credentials can be tested before Apply.
        WebSearchConfig config = formConfig.get();
        return () -> search(config, query);
    }

    /**
     * Runs the test search for {@code config}. Never throws: failures become an error outcome.
     * Provider exceptions may contain request URLs or credentials, so only the exception type is
     * shown for unexpected failures.
     */
    static @NotNull SearchTestOutcome<Hit> search(@NotNull WebSearchConfig config, @NotNull String query) {
        WebSearchReadiness readiness = WebSearchService.readiness(config);
        if (!readiness.isReady()) {
            return SearchTestOutcome.error(readiness.message());
        }
        String providerName = WebSearchService.definition(config.provider()).displayName();
        List<WebSearchOrganicResult> found;
        try {
            found = WebSearchService.search(config, query).results();
        } catch (WebSearchProviderException e) {
            return SearchTestOutcome.error(e.getMessage()); // written for the user; never contains credentials
        } catch (Exception e) {
            return SearchTestOutcome.error("Search with " + providerName + " failed (" + e.getClass().getSimpleName() +
                    "). Check the " + providerName + " settings and network connection.");
        }
        if (found == null || found.isEmpty()) {
            return SearchTestOutcome.empty(providerName + " found no results for \"" + query + "\". Try a broader query.");
        }
        List<Hit> hits = found.subList(0, Math.min(config.maxResults(), found.size())).stream()
                .map(WebSearchTestSearch::toHit)
                .toList();
        return SearchTestOutcome.results(
                providerName + " returned " + hits.size() + (hits.size() == 1 ? " result" : " results"), hits);
    }

    private static @NotNull Hit toHit(@NotNull WebSearchOrganicResult result) {
        String url = result.url() == null ? null : result.url().toString();
        String title = result.title() == null || result.title().isBlank()
                ? WebSearchSnippetFormatter.domain(url) : result.title().strip();
        return new Hit(title.isEmpty() ? "(no title)" : title, url,
                WebSearchSnippetFormatter.domain(url), WebSearchSnippetFormatter.clean(result.snippet()));
    }

    @Override
    public void renderRow(@NotNull SimpleColoredComponent row, @NotNull Hit hit) {
        row.append(hit.title(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        if (!hit.domain().isEmpty()) {
            row.append("   " + hit.domain(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @Override
    public @Nullable String rowTooltip(@NotNull Hit hit) {
        return hit.url();
    }

    @Override
    public @NotNull String previewTitle(@NotNull Hit hit) {
        return hit.url() == null ? hit.title() : hit.url();
    }

    @Override
    public @NotNull String previewText(@NotNull Hit hit) {
        return hit.snippet().isEmpty() ? "(The provider returned no snippet for this result.)" : hit.snippet();
    }

    @Override
    public void open(@NotNull Hit hit) {
        if (WebSearchSnippetFormatter.isOpenable(hit.url())) {
            BrowserUtil.browse(hit.url().strip());
        }
    }

    @Override
    public @NotNull String openHint() {
        return "Double-click to open in your browser";
    }
}
