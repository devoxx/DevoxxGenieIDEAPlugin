package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolArgumentParserTest {

    @Test
    void getString_validJson_returnsValue() {
        assertThat(ToolArgumentParser.getString("{\"path\": \"src/main\"}", "path"))
                .isEqualTo("src/main");
    }

    @Test
    void getString_missingKey_returnsNull() {
        assertThat(ToolArgumentParser.getString("{\"other\": \"val\"}", "path"))
                .isNull();
    }

    @Test
    void getString_nullValue_returnsNull() {
        assertThat(ToolArgumentParser.getString("{\"path\": null}", "path"))
                .isNull();
    }

    @Test
    void getString_invalidJson_returnsNull() {
        assertThat(ToolArgumentParser.getString("not json", "path"))
                .isNull();
    }

    @Test
    void getBoolean_validTrue_returnsTrue() {
        assertThat(ToolArgumentParser.getBoolean("{\"recursive\": true}", "recursive", false))
                .isTrue();
    }

    @Test
    void getBoolean_missingKey_returnsDefault() {
        assertThat(ToolArgumentParser.getBoolean("{}", "recursive", true))
                .isTrue();
    }

    @Test
    void getBoolean_invalidJson_returnsDefault() {
        assertThat(ToolArgumentParser.getBoolean("bad", "recursive", false))
                .isFalse();
    }

    @Test
    void getInt_validValue_returnsInt() {
        assertThat(ToolArgumentParser.getInt("{\"count\": 42}", "count", 0))
                .isEqualTo(42);
    }

    @Test
    void getInt_missingKey_returnsDefault() {
        assertThat(ToolArgumentParser.getInt("{}", "count", 10))
                .isEqualTo(10);
    }
}
