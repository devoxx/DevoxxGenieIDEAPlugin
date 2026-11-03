package com.devoxx.genie.service.rag.chunking;

import com.devoxx.genie.service.rag.ChunkQualityFilter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns one file's content into the chunks the RAG index stores: picks a splitter for the
 * file type, splits, and drops low-content chunks via {@link ChunkQualityFilter}.
 *
 * <p>Deliberately free of IntelliJ dependencies so the plugin's indexer and the standalone
 * retrieval benchmark ({@code RagCli benchmark}) chunk files with the same code. Returned
 * segments carry no metadata; attaching file path, timestamps and schema version is the
 * indexer's job.
 *
 * <p>Thread-safe: the plugin indexes several files in parallel with one shared instance.
 */
public final class RagChunker {

    /** Chunk size for the splitters (langchain4j counts characters when no tokenizer is given). */
    public static final int CHUNK_SIZE = 500;
    /** Chunk overlap; non-zero so a symbol straddling a boundary isn't lost. */
    public static final int CHUNK_OVERLAP = 50;

    private DocumentSplitter defaultSplitter;

    /**
     * Split {@code content} (the text of {@code path}) into indexable chunks.
     * Returns an empty list for blank content or when every chunk is low-content noise.
     */
    public @NotNull List<TextSegment> chunk(@NotNull Path path, @NotNull String content) {
        if (content.isBlank()) {
            return List.of();
        }
        List<TextSegment> raw = splitterFor(path).split(Document.from(content));
        List<TextSegment> kept = new ArrayList<>(raw.size());
        for (TextSegment segment : raw) {
            if (!ChunkQualityFilter.isLowContent(segment.text())) {
                kept.add(TextSegment.from(segment.text()));
            }
        }
        return kept;
    }

    /**
     * Pick a splitter appropriate to the file's content type. Real wins come from honoring
     * obvious natural boundaries:
     * <ul>
     *   <li>Markdown: split on paragraphs first so headers, code blocks, and bullet lists stay
     *       together when they fit. Falls back to line/word for any single paragraph over the
     *       chunk-size budget.</li>
     *   <li>Source code: line splitting respects statement boundaries better than the generic
     *       recursive separator order does for code (which prefers paragraph breaks, then
     *       newlines, then sentence punctuation — the last of which is wrong for code).</li>
     *   <li>Everything else: the default recursive splitter.</li>
     * </ul>
     * Per-language AST chunking is Phase 3; this is a cheap, library-only first pass.
     */
    @NotNull DocumentSplitter splitterFor(@NotNull Path path) {
        String ext = extensionOf(path);
        return switch (ext) {
            case "md", "mdx", "markdown" ->
                    new DocumentByParagraphSplitter(CHUNK_SIZE, CHUNK_OVERLAP, defaultSplitter());
            case "java", "kt", "kts", "py", "js", "mjs", "cjs", "ts", "tsx", "jsx",
                 "go", "rs", "cpp", "cc", "cxx", "hpp", "h", "c", "php", "rb", "scala" ->
                    new DocumentByLineSplitter(CHUNK_SIZE, CHUNK_OVERLAP, defaultSplitter());
            default -> defaultSplitter();
        };
    }

    /** Cached default (recursive) splitter — used for unknown file types and as the sub-splitter. */
    private synchronized DocumentSplitter defaultSplitter() {
        if (defaultSplitter == null) {
            defaultSplitter = DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP);
        }
        return defaultSplitter;
    }

    private static String extensionOf(@NotNull Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
