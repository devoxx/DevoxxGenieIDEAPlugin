package com.devoxx.genie.completion;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Post-processes raw FIM completion text before it is shown as inline ghost text.
 * Handles:
 * <ul>
 *   <li>Stripping leading newlines from model output</li>
 *   <li>Detecting suffix overlap on the first line (so we don't duplicate
 *       text that already exists after the cursor)</li>
 *   <li>Splitting multi-line completions into first-line + remaining-lines elements</li>
 * </ul>
 */
public final class CompletionPostProcessor {

    private CompletionPostProcessor() {
    }

    // ── Element types ──────────────────────────────────────────────

    /** Sealed interface for processed completion elements. */
    public sealed interface Element permits GrayText, SkipText {
    }

    /** New text to render as gray ghost text in the editor. */
    public record GrayText(@NotNull String text) implements Element {
    }

    /** Text that already exists in the document and should be skipped over (not inserted). */
    public record SkipText(@NotNull String text) implements Element {
    }

    /** Result of post-processing a completion. */
    public record ProcessedCompletion(@NotNull List<Element> elements) {
        public boolean isEmpty() {
            return elements.isEmpty();
        }
    }

    // ── Public API ─────────────────────────────────────────────────

    /**
     * Process a raw completion string against the text that follows the cursor
     * on the current line.
     *
     * @param completion raw completion from the FIM model (may be multi-line)
     * @param lineSuffix text after the cursor on the current line (never null, may be empty)
     * @return processed completion with GrayText / SkipText elements
     */
    public static @NotNull ProcessedCompletion process(@NotNull String completion,
                                                       @NotNull String lineSuffix) {
        // Strip leading newlines that FIM models sometimes prepend
        String stripped = stripLeadingNewlines(completion);

        if (stripped.isEmpty()) {
            return new ProcessedCompletion(Collections.emptyList());
        }

        List<Element> elements = new ArrayList<>();

        // Split into first line and remaining lines
        int newlineIdx = stripped.indexOf('\n');
        String firstLine;
        String remaining;

        if (newlineIdx < 0) {
            firstLine = stripped;
            remaining = "";
        } else {
            firstLine = stripped.substring(0, newlineIdx);
            remaining = stripped.substring(newlineIdx); // includes the '\n'
        }

        // Handle suffix overlap on the first line
        String trimmedSuffix = lineSuffix.stripTrailing();
        if (!firstLine.isEmpty() && !trimmedSuffix.isEmpty()) {
            int overlapLen = findSuffixOverlap(firstLine, trimmedSuffix);
            if (overlapLen > 0) {
                String newText = firstLine.substring(0, firstLine.length() - overlapLen);
                String skipText = firstLine.substring(firstLine.length() - overlapLen);
                if (!newText.isEmpty()) {
                    elements.add(new GrayText(newText));
                }
                elements.add(new SkipText(skipText));
            } else {
                elements.add(new GrayText(firstLine));
            }
        } else if (!firstLine.isEmpty()) {
            elements.add(new GrayText(firstLine));
        }

        // Remaining lines as a single GrayText element
        if (!remaining.isEmpty()) {
            elements.add(new GrayText(remaining));
        }

        return new ProcessedCompletion(elements);
    }

    // ── Internal helpers ───────────────────────────────────────────

    /**
     * Find the longest suffix of {@code completion} that is a prefix of {@code suffix}.
     * For example: completion = "foo bar)", suffix = ")" → overlap = 1.
     */
    static int findSuffixOverlap(@NotNull String completion, @NotNull String suffix) {
        int maxOverlap = Math.min(completion.length(), suffix.length());
        for (int len = maxOverlap; len > 0; len--) {
            if (completion.endsWith(suffix.substring(0, len))) {
                return len;
            }
        }
        return 0;
    }

    static @NotNull String stripLeadingNewlines(@NotNull String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == '\n' || text.charAt(i) == '\r')) {
            i++;
        }
        return i == 0 ? text : text.substring(i);
    }
}
