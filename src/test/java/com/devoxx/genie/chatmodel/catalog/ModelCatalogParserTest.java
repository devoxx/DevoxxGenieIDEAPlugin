package com.devoxx.genie.chatmodel.catalog;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelCatalogParserTest {

    private static final CatalogFields RAPID = new CatalogFields(null,
            List.of("max_model_len", "context_window"), null, null, 1, "capabilities", "text");
    private static final CatalogDefaults LOCAL = new CatalogDefaults(8000, false, true);

    @Test
    void filter_emptyArrayMatches_likeAnAbsentField() {
        var models = ModelCatalogParser.parse(ModelProvider.Nativ, RAPID, LOCAL,
                "{\"data\":[{\"id\":\"a\",\"capabilities\":[]},{\"id\":\"b\"},"
                + "{\"id\":\"c\",\"capabilities\":[\"text\"]},{\"id\":\"d\",\"capabilities\":[\"embedding\"]}]}");
        assertThat(models).extracting(LanguageModel::getModelName).containsExactly("a", "b", "c");
    }

    @Test
    void contextWindow_usesFirstPositivePath_thenDefault() {
        var models = ModelCatalogParser.parse(ModelProvider.Nativ, RAPID, LOCAL,
                "{\"data\":[{\"id\":\"a\",\"max_model_len\":65536,\"context_window\":262144},"
                + "{\"id\":\"b\",\"max_model_len\":null,\"context_window\":262144},{\"id\":\"c\"}]}");
        assertThat(models).extracting(LanguageModel::getInputMaxTokens).containsExactly(65_536, 262_144, 8000);
    }

    @Test
    void shortDisplayNames_dropOrgPrefix_andLocalModelsUseNoKey() {
        var model = ModelCatalogParser.parse(ModelProvider.Nativ, null, LOCAL,
                "{\"data\":[{\"id\":\"mlx-community/x-4bit\"},{\"id\":\"mlx-community/\"}]}");
        assertThat(model).extracting(LanguageModel::getDisplayName).containsExactly("x-4bit", "mlx-community/");
        assertThat(model.get(0).getModelName()).isEqualTo("mlx-community/x-4bit");
        assertThat(model.get(0).isApiKeyUsed()).isFalse();
    }
}
