package com.devoxx.genie.service.spec;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Targeted, text-level edits of a task file. Unlike {@link SpecFrontmatterGenerator},
 * which re-serialises the reduced {@link com.devoxx.genie.model.spec.TaskSpec} model,
 * these edits leave every other byte of the file untouched, so frontmatter fields and
 * markup this plugin does not model (Backlog.md's {@code type}, {@code modified_files},
 * {@code <!-- SECTION:... -->} markers, ...) survive.
 */
final class SpecTaskFileEditor {

    private static final String NOTES_END_MARKER = "<!-- SECTION:NOTES:END -->";
    private static final String NOTES_BEGIN_MARKER = "<!-- SECTION:NOTES:BEGIN -->";
    private static final Pattern STATUS_LINE = Pattern.compile("(?m)^status:.*$");
    private static final Pattern NOTES_HEADING =
            Pattern.compile("(?mi)^##\\s+(implementation notes|notes)\\s*$");
    private static final Pattern NEXT_HEADING = Pattern.compile("(?m)^##\\s+");

    private SpecTaskFileEditor() {
    }

    /**
     * Sets the frontmatter {@code status} and appends {@code note} as a new paragraph at
     * the end of the Implementation Notes section, creating that section if needed.
     * Line endings (LF or CRLF) are preserved.
     */
    static @NotNull String setStatusAndAppendNote(@NotNull String content,
                                                  @NotNull String status,
                                                  @NotNull String note) {
        boolean crlf = content.contains("\r\n");
        String text = crlf ? content.replace("\r\n", "\n") : content;

        text = appendNote(setStatus(text, status), note);

        return crlf ? text.replace("\n", "\r\n") : text;
    }

    private static @NotNull String setStatus(@NotNull String text, @NotNull String status) {
        if (!text.startsWith("---\n")) {
            return text;
        }
        int close = text.indexOf("\n---", 3);
        if (close < 0) {
            return text;
        }
        String frontmatter = text.substring(0, close + 1);
        String rest = text.substring(close + 1);
        Matcher m = STATUS_LINE.matcher(frontmatter);
        String replacement = "status: " + status;
        if (m.find()) {
            frontmatter = frontmatter.substring(0, m.start()) + replacement + frontmatter.substring(m.end());
        } else {
            frontmatter = frontmatter + replacement + "\n";
        }
        return frontmatter + rest;
    }

    private static @NotNull String appendNote(@NotNull String text, @NotNull String note) {
        // 1. Backlog.md marker style: insert just before the END marker.
        int end = text.indexOf(NOTES_END_MARKER);
        if (end >= 0) {
            String before = stripTrailingBlankLines(text.substring(0, end));
            String separator = before.endsWith(NOTES_BEGIN_MARKER) ? "\n" : "\n\n";
            return before + separator + note + "\n" + text.substring(end);
        }

        // 2. Plain "## Implementation Notes" heading: append at the end of that section.
        Matcher heading = NOTES_HEADING.matcher(text);
        if (heading.find()) {
            Matcher next = NEXT_HEADING.matcher(text);
            int sectionEnd = next.find(heading.end()) ? next.start() : text.length();
            String before = stripTrailingBlankLines(text.substring(0, sectionEnd));
            String after = text.substring(sectionEnd);
            return before + "\n\n" + note + "\n" + (after.isEmpty() ? "" : "\n" + after);
        }

        // 3. No notes section yet: add one, matching the file's marker style.
        String section = text.contains("<!-- SECTION:")
                ? "## Implementation Notes\n\n" + NOTES_BEGIN_MARKER + "\n" + note + "\n" + NOTES_END_MARKER + "\n"
                : "## Implementation Notes\n\n" + note + "\n";
        return stripTrailingBlankLines(text) + "\n\n" + section;
    }

    /** Removes trailing whitespace-only lines and the final newline. */
    private static @NotNull String stripTrailingBlankLines(@NotNull String s) {
        int i = s.length();
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) {
            i--;
        }
        return s.substring(0, i);
    }
}
