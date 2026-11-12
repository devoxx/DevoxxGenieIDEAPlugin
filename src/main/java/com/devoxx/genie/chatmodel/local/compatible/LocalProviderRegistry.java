package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.chatmodel.catalog.CatalogFields;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;

import java.util.List;
import java.util.Optional;

/** The local providers served by the generic local provider. */
public final class LocalProviderRegistry {

    private static final LocalProviderDefinition NATIV = new LocalProviderDefinition(
            ModelProvider.Nativ,
            Constant.NATIV_MODEL_URL,
            "https://neywa-labs.github.io/nativ/",
            "Run MLX models locally on Apple Silicon. Nativ defaults to port 8080 — the same port as Llama.c++ — so change one of the two if you want both enabled.",
            null,
            8000,
            "Nativ's <code>/v1/models</code> does not report a context length; DevoxxGenie assumes 8000 tokens unless you set it here.");

    private static final LocalProviderDefinition RAPID_MLX = new LocalProviderDefinition(
            ModelProvider.RapidMLX,
            "http://localhost:8000/v1/",
            "https://github.com/raullenchai/Rapid-MLX",
            "Run MLX models locally on Apple Silicon, for example with <code>rapid-mlx serve qwen3.5-4b-4bit --port 8000</code>. Without <code>--port</code> it uses the first free port from 8000 to 8009, so check that this URL matches.",
            new CatalogFields(null, List.of("max_model_len", "context_window"), null, null, 1, "capabilities", "text"),
            8000,
            null);

    private static final List<LocalProviderDefinition> DEFINITIONS = List.of(NATIV, RAPID_MLX);

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
