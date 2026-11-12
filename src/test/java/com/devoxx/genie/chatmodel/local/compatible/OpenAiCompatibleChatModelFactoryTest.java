package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleChatModelFactoryTest {

    /** Records the notifications instead of sending them. */
    private static class RecordingFactory extends OpenAiCompatibleChatModelFactory {
        int notRunning;
        final List<IOException> fetchErrors = new ArrayList<>();

        RecordingFactory(LocalProviderDefinition def, BooleanSupplier enabled, Supplier<String> url,
                         Supplier<Integer> fallback, CatalogBodyFetcher fetcher) {
            super(def, enabled, url, fallback, fetcher);
        }

        @Override
        protected void handleProviderNotRunning() {
            notRunning++;
        }

        @Override
        protected void handleGeneralFetchError(IOException e) {
            fetchErrors.add(e);
        }
    }

    private static LocalProviderDefinition rapidMlx() {
        return LocalProviderRegistry.find(ModelProvider.RapidMLX).orElseThrow();
    }

    private static LocalProviderDefinition llamaCpp() {
        return LocalProviderRegistry.find(ModelProvider.LLaMA).orElseThrow();
    }

    private static LocalProviderDefinition gpt4All() {
        return LocalProviderRegistry.find(ModelProvider.GPT4All).orElseThrow();
    }

    private static LocalProviderDefinition nativ() {
        return LocalProviderRegistry.find(ModelProvider.Nativ).orElseThrow();
    }

    private static RecordingFactory factory(LocalProviderDefinition def, boolean enabled, String url,
                                            Integer fallback, OpenAiCompatibleChatModelFactory.CatalogBodyFetcher fetcher) {
        return new RecordingFactory(def, () -> enabled, () -> url, () -> fallback, fetcher);
    }

    @Test
    void disabled_sendsNoRequest_andDoesNotNotify() {
        RecordingFactory f = factory(rapidMlx(), false, "http://localhost:8000/v1/", null,
                url -> {
                    throw new AssertionError("must not fetch while disabled");
                });

        assertThat(f.getModels()).isEmpty();
        assertThat(f.notRunning).isZero();
    }

    @Test
    void nonJsonReply_isTreatedAsNotRunning() {
        RecordingFactory f = factory(rapidMlx(), true, "http://localhost:8000/v1/", null,
                url -> "<html>other server</html>");

        assertThat(f.getModels()).isEmpty();
        assertThat(f.notRunning).isEqualTo(1);
    }

    @Test
    void connectionFailure_isTreatedAsNotRunning() {
        RecordingFactory f = factory(rapidMlx(), true, "http://localhost:8000/v1/", null,
                url -> {
                    throw new IOException("refused");
                });

        assertThat(f.getModels()).isEmpty();
        assertThat(f.notRunning).isEqualTo(1);
    }

    @Test
    void listsModels_fromUrlJoinedWithModels() {
        for (String base : List.of("http://localhost:8000/v1/", "http://localhost:8000/v1")) {
            AtomicReference<String> seen = new AtomicReference<>();
            RecordingFactory f = factory(rapidMlx(), true, base, null, url -> {
                seen.set(url);
                return "{\"data\":[]}";
            });

            f.getModels();

            assertThat(seen.get()).isEqualTo("http://localhost:8000/v1/models");
        }
    }

    @Test
    void rapidMlx_payload_leavesOutEmbeddingModel() {
        String body = "{\"data\":[{\"id\":\"qwen3.5-4b-4bit\",\"context_window\":262144,\"max_model_len\":65536,"
                + "\"capabilities\":[\"text\",\"tools\"]},{\"id\":\"embed\",\"capabilities\":[\"embedding\"]}]}";
        RecordingFactory f = factory(rapidMlx(), true, "http://localhost:8000/v1/", null, url -> body);

        List<LanguageModel> models = f.getModels();

        assertThat(models).hasSize(1);
        assertThat(models.get(0).getModelName()).isEqualTo("qwen3.5-4b-4bit");
        assertThat(models.get(0).getInputMaxTokens()).isEqualTo(65536);
        assertThat(models.get(0).getProvider()).isEqualTo(ModelProvider.RapidMLX);
    }

    @Test
    void nativ_payload_usesConfiguredFallback_thenDefault() {
        String body = "{\"data\":[{\"id\":\"mlx-community/gemma-3-4b-it-4bit\",\"object\":\"model\"}]}";

        List<LanguageModel> withFallback = factory(nativ(), true, "http://localhost:8080/v1/", 131072, url -> body).getModels();
        assertThat(withFallback).hasSize(1);
        assertThat(withFallback.get(0).getInputMaxTokens()).isEqualTo(131072);

        List<LanguageModel> withoutFallback = factory(nativ(), true, "http://localhost:8080/v1/", null, url -> body).getModels();
        assertThat(withoutFallback.get(0).getInputMaxTokens()).isEqualTo(8000);
        assertThat(withoutFallback.get(0).getDisplayName()).isEqualTo("gemma-3-4b-it-4bit");
        assertThat(withoutFallback.get(0).isApiKeyUsed()).isFalse();
    }

    @Test
    void resetModels_refetches_fromCurrentUrl() {
        AtomicReference<String> url = new AtomicReference<>("http://one:8000/v1/");
        List<String> seen = new ArrayList<>();
        RecordingFactory f = new RecordingFactory(rapidMlx(), () -> true, url::get, () -> null, u -> {
            seen.add(u);
            return "{\"data\":[]}";
        });

        f.getModels();
        url.set("http://two:8000/v1/");
        f.resetModels();
        f.getModels();

        assertThat(seen).containsExactly("http://one:8000/v1/models", "http://two:8000/v1/models");
    }

    @Test
    void gpt4All_payload_usesProviderDefaultContext_thenConfiguredFallback() {
        // GPT4All's /v1/models reports id, object, owned_by and permissions, but no context length.
        String body = "{\"object\":\"list\",\"data\":[{\"id\":\"Llama 3 8B Instruct\",\"object\":\"model\","
                + "\"owned_by\":\"humanity\",\"permissions\":[]}]}";
        AtomicReference<String> seen = new AtomicReference<>();

        List<LanguageModel> models = factory(gpt4All(), true, "http://localhost:4891/v1/", null, url -> {
            seen.set(url);
            return body;
        }).getModels();

        assertThat(seen.get()).isEqualTo("http://localhost:4891/v1/models");
        assertThat(models).hasSize(1);
        assertThat(models.get(0).getProvider()).isEqualTo(ModelProvider.GPT4All);
        assertThat(models.get(0).getModelName()).isEqualTo("Llama 3 8B Instruct");
        assertThat(models.get(0).getDisplayName()).isEqualTo("Llama 3 8B Instruct");
        assertThat(models.get(0).getInputMaxTokens()).isEqualTo(8000);

        List<LanguageModel> withFallback = factory(gpt4All(), true, "http://localhost:4891/v1/", 32768, url -> body).getModels();
        assertThat(withFallback.get(0).getInputMaxTokens()).isEqualTo(32768);
    }

    @Test
    void llamaCpp_payload_usesRunningContext_thenTrainingContext_thenDefault() {
        // Shape captured from llama-server 0.4.1: without --alias the id is the GGUF path, and meta.n_ctx is the
        // context the server runs with (-c) while meta.n_ctx_train is the model's maximum.
        String running = "{\"id\":\"/models/gemma-4-E2B-it-Q4_K_M.gguf\",\"object\":\"model\",\"owned_by\":\"llamacpp\","
                + "\"meta\":{\"n_ctx\":4096,\"n_ctx_train\":131072,\"n_vocab\":262144}}";
        String trainingOnly = "{\"id\":\"older.gguf\",\"object\":\"model\",\"meta\":{\"n_ctx_train\":32768}}";
        String noMeta = "{\"id\":\"bare\",\"object\":\"model\"}";
        String body = "{\"object\":\"list\",\"data\":[" + running + "," + trainingOnly + "," + noMeta + "]}";
        AtomicReference<String> seen = new AtomicReference<>();

        List<LanguageModel> models = factory(llamaCpp(), true, "http://localhost:8080", null, url -> {
            seen.set(url);
            return body;
        }).getModels();

        assertThat(seen.get()).isEqualTo("http://localhost:8080/models");
        assertThat(models).extracting(LanguageModel::getModelName)
                .containsExactlyInAnyOrder("/models/gemma-4-E2B-it-Q4_K_M.gguf", "older.gguf", "bare");
        LanguageModel gemma = models.stream().filter(m -> m.getModelName().endsWith("gemma-4-E2B-it-Q4_K_M.gguf"))
                .findFirst().orElseThrow();
        assertThat(gemma.getDisplayName()).isEqualTo("gemma-4-E2B-it-Q4_K_M.gguf");
        assertThat(gemma.getInputMaxTokens()).isEqualTo(4096);
        assertThat(models).filteredOn(m -> m.getModelName().equals("older.gguf"))
                .extracting(LanguageModel::getInputMaxTokens).containsExactly(32768);
        assertThat(models).filteredOn(m -> m.getModelName().equals("bare"))
                .extracting(LanguageModel::getInputMaxTokens).containsExactly(8000);
        assertThat(models).allMatch(m -> m.getProvider() == ModelProvider.LLaMA);
    }

    @Test
    void llamaCpp_defaultsToEnabledOnItsExistingUrl() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        assertThat(state.isLocalProviderEnabled(ModelProvider.LLaMA)).isTrue();
        assertThat(state.getLocalProviderUrl(ModelProvider.LLaMA)).isEqualTo("http://localhost:8080");
    }

    @Test
    void gpt4All_defaultsToEnabledOnPort4891() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        assertThat(state.isLocalProviderEnabled(ModelProvider.GPT4All)).isTrue();
        assertThat(state.getLocalProviderUrl(ModelProvider.GPT4All)).isEqualTo("http://localhost:4891/v1/");
    }

    @Test
    void rapidMlx_defaultsToDisabledOnPort8000() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();

        assertThat(state.isLocalProviderEnabled(ModelProvider.RapidMLX)).isFalse();
        assertThat(state.getLocalProviderUrl(ModelProvider.RapidMLX)).isEqualTo("http://localhost:8000/v1/");
    }
}
