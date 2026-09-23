package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Trims large, older tool results from the messages of an outgoing LLM request.
 *
 * <p>Every agent round trip re-sends the whole conversation, so a 20 KB file read early in a
 * run is paid for again on every later round trip. This keeps the {@code keepRecent} most
 * recent tool results verbatim and shortens older ones larger than {@code minChars} to their
 * first {@code headChars} characters plus a note telling the model how to get the full text
 * back (call the tool again — the per-run cache serves identical read-only calls instantly).
 *
 * <p>Only the outgoing request is changed; chat memory keeps the full text. Compaction is a
 * pure function of message position, so once a result is compacted it stays compacted in
 * every later request, keeping the request prefix stable for provider-side prompt caching.
 */
public class ToolResultCompactor {

    static final String MARKER_PREFIX = "[DevoxxGenie compacted this earlier tool result: ";

    private final int keepRecent;
    private final int minChars;
    private final int headChars;

    /** Outcome of compacting one request. */
    public record Result(@NotNull List<ChatMessage> messages, int compacted, long charsSaved) {
    }

    public ToolResultCompactor(int keepRecent, int minChars, int headChars) {
        this.keepRecent = Math.max(0, keepRecent);
        this.minChars = Math.max(1, minChars);
        this.headChars = Math.max(0, Math.min(headChars, this.minChars));
    }

    public @NotNull Result compact(@NotNull List<ChatMessage> messages) {
        int totalToolResults = 0;
        for (ChatMessage m : messages) {
            if (m instanceof ToolExecutionResultMessage) totalToolResults++;
        }
        int compactableCount = totalToolResults - keepRecent;
        if (compactableCount <= 0) {
            return new Result(messages, 0, 0);
        }

        List<ChatMessage> out = new ArrayList<>(messages.size());
        int seen = 0;
        int compacted = 0;
        long saved = 0;
        for (ChatMessage m : messages) {
            if (m instanceof ToolExecutionResultMessage tr && seen++ < compactableCount && tr.hasSingleText()) {
                String shortened = shorten(tr.toolName(), tr.text());
                if (shortened != null) {
                    out.add(ToolExecutionResultMessage.builder()
                            .id(tr.id())
                            .toolName(tr.toolName())
                            .text(shortened)
                            .isError(tr.isError())
                            .attributes(tr.attributes())
                            .build());
                    compacted++;
                    saved += tr.text().length() - shortened.length();
                    continue;
                }
            }
            out.add(m);
        }
        return compacted == 0 ? new Result(messages, 0, 0) : new Result(out, compacted, saved);
    }

    /** Returns the shortened text, or {@code null} when the text should be kept as is. */
    @Nullable String shorten(@Nullable String toolName, @Nullable String text) {
        if (text == null || text.length() < minChars || text.contains(MARKER_PREFIX)) {
            return null;
        }
        int omitted = text.length() - headChars;
        String head = text.substring(0, headChars);
        String note = "\n…\n" + MARKER_PREFIX + omitted + " of " + text.length()
                + " characters omitted to save context. Call " + (toolName != null ? toolName : "the tool")
                + " again with the same arguments if you need the full output.]";
        if (UntrustedContent.isWrapped(text)) {
            // Keep the untrusted block balanced so later messages are not read as part of it.
            note += "\n</" + UntrustedContent.TAG + ">";
        }
        // Never return something longer than the original.
        return head.length() + note.length() < text.length() ? head + note : null;
    }
}
