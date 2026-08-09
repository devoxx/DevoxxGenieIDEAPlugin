package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.jetbrains.annotations.Nullable;

/**
 * Normalises model ids for the Cloudflare AI Gateway {@code /compat} endpoint.
 *
 * <p>Issue #1254: the compat endpoint addresses models as {@code provider/model}
 * (e.g. {@code openai/gpt-4o}, {@code workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast}).
 * Users naturally copy Workers AI model ids straight from the Cloudflare dashboard, where they
 * appear in their bare {@code @cf/...} form — the gateway then parses {@code @cf} as the provider
 * segment and rejects the request with {@code 400 AiGatewayError} code 2008 ("Invalid provider").
 * Since {@code @cf/} is the Workers AI model namespace, the missing {@code workers-ai/} provider
 * prefix can be added deterministically. Other ids pass through untouched: a bare id such as
 * {@code kimi-k2.6} could belong to any provider, so no prefix is guessed for it.</p>
 */
public final class CloudflareModelName {

    /** Cloudflare's model namespace for Workers AI models, as shown in their dashboard. */
    private static final String WORKERS_AI_NAMESPACE = "@cf/";

    /** The compat-endpoint provider prefix that routes to Workers AI. */
    public static final String WORKERS_AI_PROVIDER_PREFIX = "workers-ai/";

    private CloudflareModelName() {
    }

    /**
     * @param modelId the model id as configured or selected; may be {@code null}
     * @return the id trimmed and, for bare Workers AI ids ({@code @cf/...}), prefixed with
     *         {@code workers-ai/}; {@code null} stays {@code null}
     */
    public static @Nullable String normalize(@Nullable String modelId) {
        if (modelId == null) {
            return null;
        }
        String trimmed = modelId.trim();
        if (trimmed.startsWith(WORKERS_AI_NAMESPACE)) {
            return WORKERS_AI_PROVIDER_PREFIX + trimmed;
        }
        return trimmed;
    }
}
