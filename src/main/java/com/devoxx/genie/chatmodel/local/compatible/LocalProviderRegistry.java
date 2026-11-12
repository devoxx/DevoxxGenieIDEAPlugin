package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.chatmodel.catalog.CatalogFields;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;

import java.util.List;
import java.util.Optional;

/** The local providers served by the generic local provider. */
public final class LocalProviderRegistry {

    // llama-server reports meta.n_ctx (the context it runs with, set by -c) and meta.n_ctx_train (the model's
    // maximum); requests beyond n_ctx fail, so the running context wins. It answers on /models and /v1/models.
    private static final LocalProviderDefinition LLAMA_CPP = new LocalProviderDefinition(
            ModelProvider.LLaMA,
            Constant.LLAMA_CPP_MODEL_URL,
            "https://github.com/ggml-org/llama.cpp",
            "Run models with llama.cpp's <code>llama-server</code>; DevoxxGenie lists the models it serves. It listens on port 8080 by default, like Nativ, so change one of the two if you enable both.",
            new CatalogFields(null, List.of("meta.n_ctx", "meta.n_ctx_train"), null, null, 1, null, null),
            8000,
            null,
            true);

    private static final LocalProviderDefinition GPT4ALL = new LocalProviderDefinition(
            ModelProvider.GPT4All,
            Constant.GPT4ALL_MODEL_URL,
            "https://gpt4all.io/",
            "Run models in the GPT4All desktop app. Turn on its local API server in GPT4All's settings; it listens on port 4891 by default.",
            null,
            8000,
            "GPT4All's <code>/v1/models</code> does not report a context length; DevoxxGenie assumes 8000 tokens unless you set it here.",
            true);

    private static final LocalProviderDefinition NATIV = new LocalProviderDefinition(
            ModelProvider.Nativ,
            Constant.NATIV_MODEL_URL,
            "https://neywa-labs.github.io/nativ/",
            "Run MLX models locally on Apple Silicon. Nativ defaults to port 8080 — the same port as Llama.c++ — so change one of the two if you want both enabled.",
            null,
            8000,
            "Nativ's <code>/v1/models</code> does not report a context length; DevoxxGenie assumes 8000 tokens unless you set it here.",
            false);

    private static final LocalProviderDefinition RAPID_MLX = new LocalProviderDefinition(
            ModelProvider.RapidMLX,
            "http://localhost:8000/v1/",
            "https://github.com/raullenchai/Rapid-MLX",
            "Run MLX models locally on Apple Silicon, for example with <code>rapid-mlx serve qwen3.5-4b-4bit --port 8000</code>. Without <code>--port</code> it uses the first free port from 8000 to 8009, so check that this URL matches.",
            new CatalogFields(null, List.of("max_model_len", "context_window"), null, null, 1, "capabilities", "text"),
            8000,
            null,
            false);

    private static final List<LocalProviderDefinition> DEFINITIONS = List.of(LLAMA_CPP, GPT4ALL, NATIV, RAPID_MLX);

    private LocalProviderRegistry() {
    }

    public static List<LocalProviderDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<LocalProviderDefinition> find(ModelProvider provider) {
        return DEFINITIONS.stream().filter(d -> d.provider() == provider).findFirst();
    }

    public static boolean isGeneric(ModelProvider provider) {
        return find(provider).isPresent();
    }
}
