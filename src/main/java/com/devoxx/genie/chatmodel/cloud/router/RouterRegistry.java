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

    private static final RouterDefinition ORCA_ROUTER = new RouterDefinition(
            ModelProvider.OrcaRouter,
            "OrcaRouter",
            "https://api.orcarouter.ai/v1",
            List.of("/models"),
            "orcaRouterKey",
            "https://www.orcarouter.ai/console/token",
            null,
            new CatalogFields("name",
                    List.of("context_length", "top_provider.context_length"),
                    "pricing.prompt", "pricing.completion", 1_000_000,
                    "endpoint_capabilities", "chat_completions"),
            null);

    private static final RouterDefinition REQUESTY = new RouterDefinition(
            ModelProvider.Requesty,
            "Requesty",
            "https://router.requesty.ai/v1",
            List.of("/models/managed", "/models"),
            "requestyKey",
            "https://app.requesty.ai/api-keys",
            "Model ids are a managed policy name or the vendor/model form. "
                    + "An EU endpoint (router.eu.requesty.ai) exists but isn't used.",
            new CatalogFields(null,
                    List.of("context_window"),
                    "input_price", "output_price", 1_000_000,
                    "api", "chat"),
            4000);

    private static final RouterDefinition DEMON_ROUTE = new RouterDefinition(
            ModelProvider.DemonRoute,
            "DemonRoute",
            "https://api.demonroute.com/v1",
            List.of("/models"),
            "demonRouteKey",
            "https://demonroute.com",
            null,
            null,
            null);

    private static final List<RouterDefinition> DEFINITIONS = List.of(OPEN_ROUTER, ORCA_ROUTER, REQUESTY, DEMON_ROUTE);

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
