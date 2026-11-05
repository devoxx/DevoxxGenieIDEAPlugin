package com.devoxx.genie.chatmodel.catalog;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
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
 * Reads an OpenAI-style {@code {"data":[{"id":...}]}} model catalog as a JSON tree, using the
 * optional dotted paths of {@link CatalogFields}. No per-provider DTOs.
 */
public final class ModelCatalogParser {

    private ModelCatalogParser() {
    }

    /**
     * @throws JsonParseException when the body is not a JSON object with a {@code data} array
     */
    public static @NotNull List<LanguageModel> parse(@NotNull ModelProvider provider, @Nullable CatalogFields fields,
                                                  @NotNull CatalogDefaults defaults, @NotNull String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("data")
                || !root.getAsJsonObject().get("data").isJsonArray()) {
            throw new JsonParseException("Catalog has no \"data\" array");
        }

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
            models.add(toModel(provider, entry, id, fields, defaults));
        }
        return models;
    }

    private static LanguageModel toModel(ModelProvider provider, JsonObject entry, String id,
                                         @Nullable CatalogFields fields, CatalogDefaults defaults) {
        String displayName = null;
        int window = defaults.contextWindow();
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
                .provider(provider)
                .modelName(id)
                .displayName(displayName == null || displayName.isBlank() ? fallbackName(id, defaults) : displayName)
                .inputCost(inputCost)
                .outputCost(outputCost)
                .inputMaxTokens(window)
                .apiKeyUsed(defaults.apiKeyUsed())
                .build();
    }

    private static String fallbackName(String id, CatalogDefaults defaults) {
        if (defaults.shortDisplayNames()) {
            int slash = id.lastIndexOf('/');
            if (slash >= 0 && slash < id.length() - 1) {
                return id.substring(slash + 1);
            }
        }
        return id;
    }

    private static boolean matchesFilter(JsonObject entry, @Nullable CatalogFields fields) {
        if (fields == null || fields.filterField() == null) {
            return true;
        }
        JsonElement actual = walk(entry, fields.filterField());
        if (actual != null && actual.isJsonArray()) {
            if (actual.getAsJsonArray().isEmpty()) {
                return true;
            }
            for (JsonElement item : actual.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsString().equals(fields.filterValue())) {
                    return true;
                }
            }
            return false;
        }
        return actual == null || !actual.isJsonPrimitive() || actual.getAsString().equals(fields.filterValue());
    }

    private static double scaledPrice(JsonObject entry, @Nullable String path, double scale) {
        if (path == null) {
            return 0;
        }
        Double price = numberAt(entry, path);
        // Dynamic entries (e.g. openrouter/auto) report -1 for "no fixed price"
        if (price == null || price < 0) {
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
