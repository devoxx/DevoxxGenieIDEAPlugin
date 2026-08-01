package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.jetbrains.annotations.Nullable;

/**
 * Assembles Cloudflare AI Gateway OpenAI-compatible base URLs from an account id and gateway name:
 * {@code https://gateway.ai.cloudflare.com/v1/{accountId}/{gatewayName}/compat} for BYOK provider
 * models, and {@code .../{accountId}/{gatewayName}/workers-ai/v1} for Workers AI models.
 *
 * <p>langchain4j's OpenAI client then appends {@code /chat/completions}; the model probe appends
 * {@code /models}. Both resolve correctly against the {@code /compat} base.</p>
 */
public final class CloudflareGatewayUrl {

    private static final String DEFAULT_ROOT = "https://gateway.ai.cloudflare.com/v1/";
    /** Cloudflare auto-creates a gateway named "default" on first authenticated request. */
    public static final String DEFAULT_GATEWAY = "default";

    /**
     * The gateway root. Only tests change it, so wire-level tests can point the whole factory
     * (URL assembly included) at a local mock server instead of the real Cloudflare host.
     */
    private static volatile String root = DEFAULT_ROOT;

    private CloudflareGatewayUrl() {
    }

    /**
     * @param testRoot the root to assemble URLs against (a trailing slash is added when missing),
     *                 or {@code null} to restore the real Cloudflare gateway root
     */
    @org.jetbrains.annotations.TestOnly
    static void setRootForTests(@Nullable String testRoot) {
        if (testRoot == null) {
            root = DEFAULT_ROOT;
        } else {
            root = testRoot.endsWith("/") ? testRoot : testRoot + "/";
        }
    }

    /**
     * @param accountId   the Cloudflare account id (required)
     * @param gatewayName the gateway name; blank falls back to {@link #DEFAULT_GATEWAY}
     * @return the {@code .../compat} base URL (no trailing slash), or {@code null} when the account id is blank
     */
    public static @Nullable String compatBaseUrl(String accountId, String gatewayName) {
        return baseUrl(accountId, gatewayName, "compat");
    }

    /**
     * The Workers AI provider path of the gateway, {@code .../{account}/{gateway}/workers-ai/v1}.
     *
     * <p>Issue #1256: the gateway-level {@code /compat} endpoint forwards chat bodies to the Workers
     * AI <em>native</em> model endpoint, whose input schema only accepts {@code messages} with plain
     * string content — the tool-calling shapes of an agent round trip (assistant messages carrying
     * {@code tool_calls}, {@code role:"tool"} results) fail its validation with
     * {@code 400 AiError 5006 "Bad input"}. This provider path is served by Workers AI's own
     * OpenAI-compatibility layer, which translates those shapes correctly, so Workers AI models are
     * routed here instead.</p>
     *
     * @param accountId   the Cloudflare account id (required)
     * @param gatewayName the gateway name; blank falls back to {@link #DEFAULT_GATEWAY}
     * @return the {@code .../workers-ai/v1} base URL (no trailing slash), or {@code null} when the
     *         account id is blank
     */
    public static @Nullable String workersAiBaseUrl(String accountId, String gatewayName) {
        return baseUrl(accountId, gatewayName, "workers-ai/v1");
    }

    private static @Nullable String baseUrl(String accountId, String gatewayName, String endpoint) {
        String account = accountId == null ? "" : stripSlashes(accountId.trim());
        if (account.isEmpty()) {
            return null;
        }
        String gateway = gatewayName == null ? "" : stripSlashes(gatewayName.trim());
        if (gateway.isEmpty()) {
            gateway = DEFAULT_GATEWAY;
        }
        return root + account + "/" + gateway + "/" + endpoint;
    }

    private static String stripSlashes(String value) {
        String v = value;
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
