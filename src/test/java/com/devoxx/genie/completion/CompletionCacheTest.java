package com.devoxx.genie.completion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionCacheTest {

    private CompletionCache cache;

    @BeforeEach
    void setUp() {
        cache = new CompletionCache();
    }

    @Test
    void shouldReturnNullForCacheMiss() {
        assertThat(cache.get("prefix", "suffix")).isNull();
    }

    @Test
    void shouldReturnCachedValue() {
        cache.put("prefix", "suffix", "completion");

        assertThat(cache.get("prefix", "suffix")).isEqualTo("completion");
    }

    @Test
    void shouldReturnDifferentValuesForDifferentKeys() {
        cache.put("prefix1", "suffix1", "completion1");
        cache.put("prefix2", "suffix2", "completion2");

        assertThat(cache.get("prefix1", "suffix1")).isEqualTo("completion1");
        assertThat(cache.get("prefix2", "suffix2")).isEqualTo("completion2");
    }

    @Test
    void shouldOverwriteExistingEntry() {
        cache.put("prefix", "suffix", "old");
        cache.put("prefix", "suffix", "new");

        assertThat(cache.get("prefix", "suffix")).isEqualTo("new");
    }

    @Test
    void shouldClearAllEntries() {
        cache.put("prefix1", "suffix1", "completion1");
        cache.put("prefix2", "suffix2", "completion2");

        cache.clear();

        assertThat(cache.get("prefix1", "suffix1")).isNull();
        assertThat(cache.get("prefix2", "suffix2")).isNull();
    }

    @Test
    void shouldEvictOldestEntriesWhenFull() {
        // Fill cache beyond MAX_ENTRIES (100)
        for (int i = 0; i < 110; i++) {
            cache.put("prefix" + i, "suffix" + i, "completion" + i);
        }

        // First entries should have been evicted
        assertThat(cache.get("prefix0", "suffix0")).isNull();
        assertThat(cache.get("prefix5", "suffix5")).isNull();

        // Recent entries should still be present
        assertThat(cache.get("prefix109", "suffix109")).isEqualTo("completion109");
        assertThat(cache.get("prefix100", "suffix100")).isEqualTo("completion100");
    }

    @Test
    void shouldUsePrefixTailAndSuffixHeadForKey() {
        // Two different prefixes with the same tail should produce the same cache key
        String commonTail = "x".repeat(128);
        String prefix1 = "aaa" + commonTail;
        String prefix2 = "bbb" + commonTail;

        String commonHead = "y".repeat(64);
        String suffix1 = commonHead + "ccc";
        String suffix2 = commonHead + "ddd";

        cache.put(prefix1, suffix1, "completion");

        // Same tail+head = same key, so this should hit
        assertThat(cache.get(prefix2, suffix2)).isEqualTo("completion");
    }
}
