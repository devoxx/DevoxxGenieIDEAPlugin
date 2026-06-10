package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import com.devoxx.genie.model.tips.TipConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that docusaurus/static/api/tips.json is well-formed and
 * can be deserialized by the same Gson path used in production.
 */
class TipsJsonTest {

    private static final Path TIPS_JSON_PATH =
            Path.of("docusaurus/static/api/tips.json");

    @Test
    void realTipsJsonParsesAndIsValid() throws Exception {
        assertThat(TIPS_JSON_PATH).exists();

        String json = Files.readString(TIPS_JSON_PATH);
        TipConfig config = new GsonBuilder().create().fromJson(json, TipConfig.class);

        assertThat(config.getSchemaVersion()).isEqualTo(1);
        assertThat(config.getLastUpdated()).isNotBlank();
        assertThat(config.getTips()).isNotEmpty();
        assertThat(config.getTips()).allSatisfy((Tip tip) -> {
            assertThat(tip.getText()).isNotBlank();
            assertThat(tip.getWeight()).isGreaterThanOrEqualTo(1);
        });
    }
}
