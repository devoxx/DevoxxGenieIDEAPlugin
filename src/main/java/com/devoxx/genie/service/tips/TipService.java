package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import com.devoxx.genie.model.tips.TipConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public final class TipService {

    private static final List<Tip> FALLBACK_TIPS = List.of(
            new Tip("Type @ to add a file to the context.", 2),
            new Tip("Generate a DEVOXXGENIE.md with /init for project-aware answers.", 3),
            new Tip("Try /test, /explain or /review on selected code.", 2),
            new Tip("Discover and install MCP servers from the MCP Marketplace in Settings.", 1),
            new Tip("Enable Agent Mode to let DevoxxGenie read, search and edit files for you.", 1),
            new Tip("Turn on RAG in Settings to add semantic project context automatically.", 1),
            new Tip("Drag & drop images into the prompt when using a multimodal model.", 1),
            new Tip("Press Ctrl+Space to autocomplete a slash command.", 1),
            new Tip("Add the full project to context with large-context models like Gemini.", 1),
            new Tip("DevoxxGenie is open source — star it on GitHub and join us at Devoxx!", 1));

    @NotNull
    public static TipService getInstance() {
        return ApplicationManager.getApplication().getService(TipService.class);
    }

    /**
     * Returns the next tip text to display, weighted-random and excluding the
     * immediately-previous tip when alternatives exist.
     *
     * @param previousTipText the tip currently shown (may be null)
     * @return the next tip text, or null when no tips are available
     */
    @Nullable
    public String nextTip(@Nullable String previousTipText) {
        Tip selected = selectWeighted(getTips(), previousTipText, ThreadLocalRandom.current().nextDouble());
        return selected == null ? null : selected.getText();
    }

    /**
     * Returns the current tip list. Replaced in a later task with cache-backed lookup;
     * for now always returns the hardcoded fallback.
     */
    @NotNull
    public List<Tip> getTips() {
        return getFallbackTips();
    }

    static int effectiveWeight(@Nullable Tip tip) {
        if (tip == null || tip.getWeight() <= 0) {
            return 1;
        }
        return tip.getWeight();
    }

    /**
     * Weighted-random selection over {@code tips}, excluding any tip whose text equals
     * {@code previousTipText}. Falls back to the full set if exclusion empties the
     * candidate list (e.g. a single-tip list). Blank/null tips are ignored.
     *
     * @param r a value in [0, 1) used to pick within the cumulative weight range
     */
    @Nullable
    static Tip selectWeighted(@Nullable List<Tip> tips, @Nullable String previousTipText, double r) {
        if (tips == null || tips.isEmpty()) {
            return null;
        }

        List<Tip> valid = new ArrayList<>();
        for (Tip tip : tips) {
            if (tip != null && tip.getText() != null && !tip.getText().isBlank()) {
                valid.add(tip);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }

        List<Tip> candidates = new ArrayList<>();
        for (Tip tip : valid) {
            if (previousTipText == null || !previousTipText.equals(tip.getText())) {
                candidates.add(tip);
            }
        }
        if (candidates.isEmpty()) {
            candidates = valid;
        }

        int total = 0;
        for (Tip tip : candidates) {
            total += effectiveWeight(tip);
        }

        double target = r * total;
        double accumulated = 0;
        for (Tip tip : candidates) {
            accumulated += effectiveWeight(tip);
            if (target < accumulated) {
                return tip;
            }
        }
        // Floating-point safety net: r is in [0,1), so this is only reached on rounding at the upper bound.
        return candidates.get(candidates.size() - 1);
    }

    @NotNull
    static List<Tip> getFallbackTips() {
        return FALLBACK_TIPS;
    }
}
