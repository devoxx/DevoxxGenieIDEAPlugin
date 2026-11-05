package com.devoxx.genie.chatmodel.local.compatible;

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

    private static final List<LocalProviderDefinition> DEFINITIONS = List.of(NATIV);

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
