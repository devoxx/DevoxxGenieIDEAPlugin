package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default query and document prefixes per model, and how settings store them. A stored
 * {@code null} means "use the model's default", while a stored {@code ""} means the user wants
 * no prefix, so the two stay distinct.
 */
public final class EmbeddingPrefixes {

    private static final String NOMIC_QUERY_PREFIX = "search_query: ";
    private static final String NOMIC_DOCUMENT_PREFIX = "search_document: ";

    private EmbeddingPrefixes() {}

    public static @NotNull String defaultQueryPrefix(@Nullable String modelName) {
        return isNomicEmbedText(modelName) ? NOMIC_QUERY_PREFIX : "";
    }

    public static @NotNull String defaultDocumentPrefix(@Nullable String modelName) {
        return isNomicEmbedText(modelName) ? NOMIC_DOCUMENT_PREFIX : "";
    }

    /** The prefix to use: what the user stored, or the model's default when nothing is stored. */
    public static @NotNull String resolve(@Nullable String stored, @NotNull String modelDefault) {
        return stored == null ? modelDefault : stored;
    }

    /** What to store for a field's text; {@code null} keeps following the model's default. */
    public static @Nullable String toStored(@NotNull String text, @NotNull String modelDefault) {
        return text.equals(modelDefault) ? null : text;
    }

    /**
     * A prefix field's text after the model changes: it follows the new model's default unless
     * the user had typed something of their own.
     */
    public static @NotNull String afterModelChange(@NotNull String currentText,
                                                   @NotNull String previousDefault,
                                                   @NotNull String newDefault) {
        return currentText.equals(previousDefault) ? newDefault : currentText;
    }

    private static boolean isNomicEmbedText(@Nullable String modelName) {
        if (modelName == null) {
            return false;
        }
        String trimmed = modelName.trim();
        int tag = trimmed.indexOf(':');
        String base = tag < 0 ? trimmed : trimmed.substring(0, tag);
        return base.equalsIgnoreCase(EmbeddingFingerprint.DEFAULT_MODEL_NAME);
    }
}
