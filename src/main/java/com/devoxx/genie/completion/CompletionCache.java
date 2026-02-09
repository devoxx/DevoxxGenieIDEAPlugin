package com.devoxx.genie.completion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache for inline completion results.
 * Keyed on a hash of the prefix tail and suffix head to provide fast
 * cache lookups for repeated typing patterns.
 */
public class CompletionCache {

    private static final int MAX_ENTRIES = 100;
    private static final int KEY_PREFIX_TAIL_LENGTH = 128;
    private static final int KEY_SUFFIX_HEAD_LENGTH = 64;

    private final LinkedHashMap<String, String> cache;

    public CompletionCache() {
        this.cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    /**
     * Look up a cached completion for the given prefix/suffix context.
     *
     * @return cached completion text, or null if not found
     */
    public @Nullable String get(@NotNull String prefix, @NotNull String suffix) {
        String key = computeKey(prefix, suffix);
        synchronized (cache) {
            return cache.get(key);
        }
    }

    /**
     * Store a completion result in the cache.
     */
    public void put(@NotNull String prefix, @NotNull String suffix, @NotNull String completion) {
        String key = computeKey(prefix, suffix);
        synchronized (cache) {
            cache.put(key, completion);
        }
    }

    /**
     * Clear all cached entries.
     */
    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private static @NotNull String computeKey(@NotNull String prefix, @NotNull String suffix) {
        String prefixTail = prefix.length() > KEY_PREFIX_TAIL_LENGTH
                ? prefix.substring(prefix.length() - KEY_PREFIX_TAIL_LENGTH)
                : prefix;
        String suffixHead = suffix.length() > KEY_SUFFIX_HEAD_LENGTH
                ? suffix.substring(0, KEY_SUFFIX_HEAD_LENGTH)
                : suffix;
        return prefixTail + "|" + suffixHead;
    }
}
