package com.devoxx.genie.service.agent.loop;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallCacheTest {

    private final ToolCallCache cache = new ToolCallCache(Set.of("read_file", "search_files"), Set.of("search_tools"));

    @Test
    void identicalReadOnlyCall_isServedFromCache() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 3);

        ToolCallCache.Lookup lookup = cache.lookup("read_file", "{\"path\":\"A.java\"}");

        assertThat(lookup.kind()).isEqualTo(ToolCallCache.Lookup.Kind.HIT);
        assertThat(lookup.text()).contains("tool call #3").endsWith("content");
    }

    @Test
    void recentRepeat_returnsShortReference_olderRepeatReturnsFullResult() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 3);

        ToolCallCache.Lookup recent = cache.lookup("read_file", "{\"path\":\"A.java\"}", 5, 3);
        ToolCallCache.Lookup older = cache.lookup("read_file", "{\"path\":\"A.java\"}", 9, 3);

        assertThat(recent.text()).startsWith("[Identical to tool call #3").doesNotContain("content");
        assertThat(older.text()).contains("tool call #3").endsWith("content");
    }

    @Test
    void argumentKeyOrderAndWhitespace_doNotMatter() {
        cache.store("search_files", "{\"pattern\":\"foo\",\"path\":\"src\"}", "hits", 1);

        assertThat(cache.lookup("search_files", "{ \"path\" : \"src\", \"pattern\" : \"foo\" }").kind())
                .isEqualTo(ToolCallCache.Lookup.Kind.HIT);
    }

    @Test
    void differentArguments_miss() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 1);

        assertThat(cache.lookup("read_file", "{\"path\":\"B.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.MISS);
    }

    @Test
    void mutatingTool_invalidatesCache() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 1);

        assertThat(cache.lookup("edit_file", "{\"path\":\"A.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.MISS);

        assertThat(cache.lookup("read_file", "{\"path\":\"A.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.MISS);
    }

    @Test
    void neutralTool_doesNotInvalidateCache() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 1);

        cache.lookup("search_tools", "{\"query\":\"jira\"}");

        assertThat(cache.lookup("read_file", "{\"path\":\"A.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.HIT);
    }

    @Test
    void errorResults_areNotCached() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "Error: File not found", 1);

        assertThat(cache.lookup("read_file", "{\"path\":\"A.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.MISS);
    }

    @Test
    void endlesslyRepeatedCall_isBlockedAfterAllowedRepeats() {
        cache.store("read_file", "{\"path\":\"A.java\"}", "content", 1);

        for (int i = 0; i < ToolCallCache.MAX_CACHED_REPEATS; i++) {
            assertThat(cache.lookup("read_file", "{\"path\":\"A.java\"}").kind()).isEqualTo(ToolCallCache.Lookup.Kind.HIT);
        }
        ToolCallCache.Lookup blocked = cache.lookup("read_file", "{\"path\":\"A.java\"}");

        assertThat(blocked.kind()).isEqualTo(ToolCallCache.Lookup.Kind.REPEAT_BLOCKED);
        assertThat(blocked.text()).startsWith("Error:").contains("exact arguments");
    }

    @Test
    void canonicalArguments_handlesInvalidJsonAndBlank() {
        assertThat(ToolCallCache.canonicalArguments(null)).isEqualTo("{}");
        assertThat(ToolCallCache.canonicalArguments("  ")).isEqualTo("{}");
        assertThat(ToolCallCache.canonicalArguments(" not json ")).isEqualTo("not json");
        assertThat(ToolCallCache.canonicalArguments("{\"b\":{\"d\":1,\"c\":2},\"a\":[{\"y\":1,\"x\":2}]}"))
                .isEqualTo("{\"a\":[{\"x\":2,\"y\":1}],\"b\":{\"c\":2,\"d\":1}}");
    }
}
