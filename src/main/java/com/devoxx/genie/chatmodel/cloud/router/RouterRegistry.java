package com.devoxx.genie.chatmodel.cloud.router;

import com.devoxx.genie.model.enumarations.ModelProvider;

import java.util.List;
import java.util.Optional;

/** The routers served by the generic router provider. */
public final class RouterRegistry {

    private static final RouterDefinition OPEN_ROUTER = new RouterDefinition(
            ModelProvider.OpenRouter,
            "OpenRouter",
            "https://openrouter.ai/api/v1/",
            List.of("/models"),
            "openRouterKey",
            "https://openrouter.ai/settings/keys",
            null,
            new CatalogFields("name",
                    List.of("context_length", "top_provider.context_length"),
                    "pricing.prompt", "pricing.completion", 1_000_000,
                    null, null),
            4000);

    private static final List<RouterDefinition> DEFINITIONS = List.of(OPEN_ROUTER);

    private RouterRegistry() {
    }

    public static List<RouterDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<RouterDefinition> find(ModelProvider provider) {
        return DEFINITIONS.stream().filter(d -> d.provider() == provider).findFirst();
    }

    public static boolean isGeneric(ModelProvider provider) {
        return find(provider).isPresent();
    }
}
