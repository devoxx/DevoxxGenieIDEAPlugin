package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelExploreToolExecutorTest {

    @Test
    void getStringArray_validJsonArray_returnsList() {
        String json = "{\"queries\": [\"find error handling\", \"find logging patterns\", \"find test utilities\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("find error handling");
        assertThat(result.get(1)).isEqualTo("find logging patterns");
        assertThat(result.get(2)).isEqualTo("find test utilities");
    }

    @Test
    void getStringArray_emptyArray_returnsEmptyList() {
        String json = "{\"queries\": []}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_missingKey_returnsEmptyList() {
        String json = "{\"other\": \"value\"}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_nullValue_returnsEmptyList() {
        String json = "{\"queries\": null}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_invalidJson_returnsEmptyList() {
        List<String> result = ToolArgumentParser.getStringArray("not json", "queries");
        assertThat(result).isEmpty();
    }

    @Test
    void getStringArray_arrayWithNulls_skipsNulls() {
        String json = "{\"queries\": [\"first\", null, \"third\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("first", "third");
    }

    @Test
    void getStringArray_singleElement_returnsSingletonList() {
        String json = "{\"queries\": [\"single query\"]}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("single query");
    }

    @Test
    void getStringArray_notAnArray_returnsEmptyList() {
        String json = "{\"queries\": \"not an array\"}";
        List<String> result = ToolArgumentParser.getStringArray(json, "queries");
        assertThat(result).isEmpty();
    }
}
