package com.devoxx.genie.model.tips;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipConfigTest {

    private final Gson gson = new Gson();

    @Test
    void parsesTipsWithAndWithoutWeight() {
        String json = """
            {
              "schemaVersion": 1,
              "lastUpdated": "2026-06-10",
              "tips": [
                { "text": "With weight", "weight": 3 },
                { "text": "Without weight" }
              ]
            }
            """;

        TipConfig config = gson.fromJson(json, TipConfig.class);

        assertThat(config.getSchemaVersion()).isEqualTo(1);
        assertThat(config.getLastUpdated()).isEqualTo("2026-06-10");
        assertThat(config.getTips()).hasSize(2);
        assertThat(config.getTips().get(0).getText()).isEqualTo("With weight");
        assertThat(config.getTips().get(0).getWeight()).isEqualTo(3);
        assertThat(config.getTips().get(1).getText()).isEqualTo("Without weight");
        // Gson leaves the absent int field at its default of 0; coercion happens in TipService.
        assertThat(config.getTips().get(1).getWeight()).isZero();
    }
}
