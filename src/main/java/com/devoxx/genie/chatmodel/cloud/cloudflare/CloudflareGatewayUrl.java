package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.jetbrains.annotations.Nullable;

/**
 * Assembles the Cloudflare AI Gateway OpenAI-compatible base URL from an account id and gateway name:
 * {@code https://gateway.ai.cloudflare.com/v1/{accountId}/{gatewayName}/compat}.
 *
 * <p>langchain4j's OpenAI client then appends {@code /chat/completions}; the model probe appends
 * {@code /models}. Both resolve correctly against this {@code /compat} base.</p>
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
        String account = accountId == null ? "" : stripSlashes(accountId.trim());
        if (account.isEmpty()) {
            return null;
        }
        String gateway = gatewayName == null ? "" : stripSlashes(gatewayName.trim());
        if (gateway.isEmpty()) {
            gateway = DEFAULT_GATEWAY;
        }
        return root + account + "/" + gateway + "/compat";
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
