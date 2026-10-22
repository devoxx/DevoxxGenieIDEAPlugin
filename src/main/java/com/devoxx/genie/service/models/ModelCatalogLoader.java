package com.devoxx.genie.service.models;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.models.ModelConfig;
import com.devoxx.genie.model.models.ModelConfigEntry;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.*;
import com.intellij.openapi.application.PathManager;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Reads bundled and remote catalogs, isolating malformed entries from valid models. */
@Slf4j
public final class ModelCatalogLoader {
    private final Gson gson = new Gson();

    public ModelConfig loadBundled() {
        try (InputStream stream = ModelCatalogLoader.class.getResourceAsStream("/models.json")) {
            if (stream == null) throw new IOException("Bundled models.json is missing");
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot load bundled model catalog", e);
            ModelConfig empty = new ModelConfig();
            empty.setProviders(Collections.emptyMap());
            return empty;
        }
    }

    public ModelConfig parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        ModelConfig config = new ModelConfig();
        config.setSchemaVersion(1);
        config.setProviders(new LinkedHashMap<>());
        if (root.isJsonArray()) {
            for (JsonElement entry : root.getAsJsonArray()) readEntry(config, null, entry);
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("schemaVersion") && object.get("schemaVersion").getAsInt() > 1) {
                throw new IllegalArgumentException("Unsupported model catalog schema");
            }
            if (object.has("lastUpdated")) config.setLastUpdated(object.get("lastUpdated").getAsString());
            JsonObject providers = object.getAsJsonObject("providers");
            if (providers == null) throw new IllegalArgumentException("Missing catalog providers");
            for (Map.Entry<String, JsonElement> provider : providers.entrySet()) {
                if (!provider.getValue().isJsonArray()) {
                    log.warn("Skipping invalid model catalog provider {}", provider.getKey());
                    continue;
                }
                for (JsonElement entry : provider.getValue().getAsJsonArray()) {
                    readEntry(config, provider.getKey(), entry);
                }
            }
        } else {
            throw new IllegalArgumentException("Model catalog must be an array or object");
        }
        if (config.getProviders().isEmpty()) throw new IllegalArgumentException("No valid models in catalog");
        return config;
    }

    private void readEntry(ModelConfig config, String providerName, JsonElement json) {
        try {
            JsonObject object = json.getAsJsonObject();
            String name = providerName != null ? providerName : object.get("provider").getAsString();
            ModelProvider provider = ModelProvider.fromString(name);
            ModelConfigEntry entry = gson.fromJson(object, ModelConfigEntry.class);
            if (entry.getModelName() == null || entry.getModelName().isBlank() || entry.getInputMaxTokens() <= 0) {
                log.warn("Skipping model catalog entry for {}: missing name or positive context window", name);
                return;
            }
            if (entry.getDisplayName() == null || entry.getDisplayName().isBlank()) {
                entry.setDisplayName(entry.getModelName());
            }
            config.getProviders().computeIfAbsent(provider.getName(), ignored -> new ArrayList<>()).add(entry);
        } catch (RuntimeException e) {
            log.warn("Skipping invalid model catalog entry: {}", e.getMessage());
        }
    }

    public Map<String, LanguageModel> toModels(ModelConfig config) {
        Map<String, LanguageModel> result = new HashMap<>();
        if (config == null || config.getProviders() == null) return result;
        // Validate objects passed through the existing public registry API as well.
        ModelConfig validated;
        try {
            validated = parse(gson.toJson(config));
        } catch (RuntimeException e) {
            log.warn("Ignoring invalid model catalog: {}", e.getMessage());
            return result;
        }
        validated.getProviders().forEach((name, entries) -> {
            ModelProvider provider = ModelProvider.fromString(name);
            for (ModelConfigEntry entry : entries) {
                LanguageModel model = LanguageModel.builder()
                        .provider(provider).modelName(entry.getModelName()).displayName(entry.getDisplayName())
                        .inputCost(entry.getInputCost()).outputCost(entry.getOutputCost())
                        .inputMaxTokens(entry.getInputMaxTokens()).outputMaxTokens(entry.getOutputMaxTokens())
                        .apiKeyUsed(entry.isApiKeyUsed()).build();
                result.put(provider.getName() + (provider == ModelProvider.Anthropic ? "-" : ":") + model.getModelName(), model);
            }
        });
        return result;
    }

    private Path cachePath(String url) {
        String key = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString();
        return Path.of(PathManager.getSystemPath(), "devoxxgenie", "model-catalog", key + ".json");
    }

    public ModelConfig loadCached(String url) {
        try {
            Path path = cachePath(url);
            return Files.exists(path) ? parse(Files.readString(path)) : null;
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot restore model catalog cache: {}", e.getMessage());
            return null;
        }
    }

    public boolean isCacheFresh(String url, long ttlMillis) {
        try {
            return System.currentTimeMillis() - Files.getLastModifiedTime(cachePath(url)).toMillis() < ttlMillis;
        } catch (IOException e) {
            return false;
        }
    }

    public ModelConfig fetch(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Model catalog HTTP " + response.code());
            }
            String json = response.body().string();
            ModelConfig config = parse(json);
            Path cache = cachePath(url);
            try {
                Files.createDirectories(cache.getParent());
                Path temporary = Files.createTempFile(cache.getParent(), "models-", ".tmp");
                try {
                    Files.writeString(temporary, json);
                    Files.move(temporary, cache, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            } catch (IOException e) {
                log.warn("Cannot persist model catalog cache: {}", e.getMessage());
            }
            return config;
        }
    }
}
