package com.devoxx.genie.completion;

import com.intellij.openapi.editor.Document;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Extracts prefix and suffix text from an editor document at the cursor position
 * for use in Fill-in-the-Middle (FIM) completion requests.
 */
@Getter
public class EditorContextExtractor {

    private static final int MAX_PREFIX_CHARS = 4096;
    private static final int MAX_SUFFIX_CHARS = 1024;

    private final String prefix;
    private final String suffix;

    private EditorContextExtractor(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * Extract context from a document at the given cursor offset.
     *
     * @param document the editor document
     * @param offset   the cursor offset (caret position)
     * @return an EditorContextExtractor with prefix and suffix
     */
    public static @NotNull EditorContextExtractor extract(@NotNull Document document, int offset) {
        String text = document.getText();
        int textLength = text.length();

        // Clamp offset to valid range
        offset = Math.max(0, Math.min(offset, textLength));

        // Extract prefix: text before cursor, up to MAX_PREFIX_CHARS
        int prefixStart = Math.max(0, offset - MAX_PREFIX_CHARS);
        String prefix = text.substring(prefixStart, offset);

        // Extract suffix: text after cursor, up to MAX_SUFFIX_CHARS
        int suffixEnd = Math.min(textLength, offset + MAX_SUFFIX_CHARS);
        String suffix = text.substring(offset, suffixEnd);

        return new EditorContextExtractor(prefix, suffix);
    }

}
