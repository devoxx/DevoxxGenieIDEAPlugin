package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.chatmodel.catalog.CatalogDefaults;
import com.devoxx.genie.chatmodel.catalog.ModelCatalogParser;
import com.devoxx.genie.model.LanguageModel;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Reads a router's model catalog with the router defaults (keyed, 128k window, full ids). */
public final class RouterCatalogParser {

    static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    private static final CatalogDefaults DEFAULTS = new CatalogDefaults(DEFAULT_CONTEXT_WINDOW, true, false);

    private RouterCatalogParser() {
    }

    /**
     * @throws JsonParseException when the body is not a JSON object with a {@code data} array
     */
    public static @NotNull List<LanguageModel> parse(@NotNull RouterDefinition def, @NotNull String json) {
        return ModelCatalogParser.parse(def.provider(), def.catalogFields(), DEFAULTS, json);
    }
}
