package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.model.LanguageModel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a router's {@code {"data":[{"id":...}]}} model catalog as a JSON tree, using the
 * optional dotted paths of {@link CatalogFields}. No per-router DTOs.
 */
public final class RouterCatalogParser {

    static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    private RouterCatalogParser() {
    }

    /**
     * @throws JsonParseException when the body is not a JSON object with a {@code data} array
     */
    public static @NotNull List<LanguageModel> parse(@NotNull RouterDefinition def, @NotNull String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("data")
                || !root.getAsJsonObject().get("data").isJsonArray()) {
            throw new JsonParseException("Catalog has no \"data\" array");
        }

        CatalogFields fields = def.catalogFields();
        List<LanguageModel> models = new ArrayList<>();
        for (JsonElement element : root.getAsJsonObject().getAsJsonArray("data")) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String id = stringAt(entry, "id");
            if (id == null || id.isBlank() || !matchesFilter(entry, fields)) {
                continue;
            }
            models.add(toModel(def, entry, id, fields));
        }
        return models;
    }

    private static LanguageModel toModel(RouterDefinition def, JsonObject entry, String id,
                                         @Nullable CatalogFields fields) {
        String displayName = null;
        int window = DEFAULT_CONTEXT_WINDOW;
        double inputCost = 0;
        double outputCost = 0;

        if (fields != null) {
            if (fields.displayName() != null) {
                displayName = stringAt(entry, fields.displayName());
            }
            for (String path : fields.contextWindow()) {
                Double value = numberAt(entry, path);
                if (value != null && value > 0) {
                    window = (int) Math.min(value, Integer.MAX_VALUE);
                    break;
                }
            }
            inputCost = scaledPrice(entry, fields.inputPrice(), fields.priceScale());
            outputCost = scaledPrice(entry, fields.outputPrice(), fields.priceScale());
        }

        return LanguageModel.builder()
                .provider(def.provider())
                .modelName(id)
                .displayName(displayName == null || displayName.isBlank() ? id : displayName)
                .inputCost(inputCost)
                .outputCost(outputCost)
                .inputMaxTokens(window)
                .apiKeyUsed(true)
                .build();
    }

    private static boolean matchesFilter(JsonObject entry, @Nullable CatalogFields fields) {
        if (fields == null || fields.filterField() == null) {
            return true;
        }
        String actual = stringAt(entry, fields.filterField());
        return actual == null || actual.equals(fields.filterValue());
    }

    private static double scaledPrice(JsonObject entry, @Nullable String path, double scale) {
        if (path == null) {
            return 0;
        }
        Double price = numberAt(entry, path);
        if (price == null) {
            return 0;
        }
        // Same rounding the OpenRouter factory used: 6 decimals, HALF_UP
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(scale))
                .setScale(6, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static @Nullable JsonElement walk(JsonObject entry, String path) {
        JsonElement current = entry;
        for (String part : path.split("\\.")) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }
        return current == null || current.isJsonNull() ? null : current;
    }

    private static @Nullable String stringAt(JsonObject entry, String path) {
        JsonElement value = walk(entry, path);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    /** Accepts JSON numbers and numeric strings (including scientific notation); anything else is null. */
    private static @Nullable Double numberAt(JsonObject entry, String path) {
        JsonElement value = walk(entry, path);
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        try {
            double number = Double.parseDouble(value.getAsString().trim());
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
