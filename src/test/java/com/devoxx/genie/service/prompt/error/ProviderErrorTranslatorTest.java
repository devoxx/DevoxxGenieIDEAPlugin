package com.devoxx.genie.service.prompt.error;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderErrorTranslatorTest {

    /** The exact body Cloudflare AI Gateway returns for an unroutable model (issue reproduction). */
    private static final String CLOUDFLARE_2005_BODY =
            "{\"success\":false,\"result\":[],\"messages\":[],\"error\":[{\"code\":2005,"
            + "\"message\":\"Failed to get response from provider\"}],\"name\":\"AiGatewayError\","
            + "\"httpCode\":400,\"internalCode\":2005,\"message\":\"Failed to get response from provider\","
            + "\"description\":\"Failed to get response from provider\"}";

    @Test
    void translatesCloudflareErrorWithModelNameCodeAndActionableGuidance() {
        Throwable error = new RuntimeException("Provider unavailable: " + CLOUDFLARE_2005_BODY);

        Optional<String> result = ProviderErrorTranslator.translate(error, "azure-openai/kimi-k2.6");

        assertThat(result).isPresent();
        String msg = result.get();
        assertThat(msg)
                .contains("Cloudflare AI Gateway")
                .contains("azure-openai/kimi-k2.6")
                .contains("2005")
                .contains("Failed to get response from provider")
                .contains("dropdown")
                .contains("Cloudflare AI Gateway dashboard");
        // The raw JSON blob must not leak into the user-facing message.
        assertThat(msg).doesNotContain("{").doesNotContain("httpCode").doesNotContain("\"success\"");
    }

    @Test
    void findsCloudflareBodyDeepInTheCauseChain() {
        Throwable root = new RuntimeException("status code: 400; body: " + CLOUDFLARE_2005_BODY);
        Throwable wrapped = new IllegalStateException("Provider unavailable", root);

        assertThat(ProviderErrorTranslator.translate(wrapped, "moonshot/moonshotai/kimi-k3"))
                .get()
                .satisfies(m -> assertThat(m).contains("moonshot/moonshotai/kimi-k3").contains("2005"));
    }

    @Test
    void fallsBackToGenericPhrasingWhenModelNameUnknown() {
        Throwable error = new RuntimeException(CLOUDFLARE_2005_BODY);

        assertThat(ProviderErrorTranslator.translate(error, null))
                .get()
                .satisfies(m -> assertThat(m).contains("the selected model").doesNotContain("model ''"));
    }

    /** The body from issue #1254: '@cf' (or a stray URL segment) parsed as an unknown provider. */
    private static final String CLOUDFLARE_2008_BODY =
            "{\"success\":false,\"result\":[],\"messages\":[],\"error\":[{\"code\":2008,"
            + "\"message\":\"Invalid provider\"}],\"name\":\"AiGatewayError\","
            + "\"httpCode\":400,\"internalCode\":2008,\"message\":\"Invalid provider\","
            + "\"description\":\"Invalid provider\"}";

    @Test
    void explainsProviderModelNamingForInvalidProviderErrors() {
        // Issue #1254: a bare Workers AI id makes /compat parse '@cf' as the provider -> code 2008.
        Throwable error = new RuntimeException("Provider unavailable: " + CLOUDFLARE_2008_BODY);

        Optional<String> result = ProviderErrorTranslator.translate(error, "@cf/openai/gpt-oss-20b");

        assertThat(result).isPresent();
        assertThat(result.get())
                .contains("2008")
                .contains("Invalid provider")
                .contains("provider/model")
                .contains("workers-ai/@cf/openai/gpt-oss-20b")
                .contains("/compat")
                .doesNotContain("{").doesNotContain("httpCode");
    }

    @Test
    void explainsMissingProviderPrefixForUnroutableBareModelIds() {
        // Issue #1254: 'kimi-k2.6' carries no provider prefix, so the gateway cannot route it.
        Throwable error = new RuntimeException("status code: 400; body: " + CLOUDFLARE_2005_BODY);

        Optional<String> result = ProviderErrorTranslator.translate(error, "kimi-k2.6");

        assertThat(result).isPresent();
        assertThat(result.get())
                .contains("kimi-k2.6")
                .contains("no provider prefix")
                .contains("provider/model")
                .contains("dropdown")
                .doesNotContain("{").doesNotContain("httpCode");
    }

    @Test
    void suggestsWorkersAiPrefixForBareCfIdsFailingWith2005() {
        // Issue #1254: the reporter's exact models, failing with "Failed to get response from
        // provider" — the fix is the 'workers-ai/' prefix, and the message must say so.
        for (String model : new String[]{"@cf/openai/gpt-oss-20b", "@cf/zai-org/glm-4.7-flash"}) {
            Throwable error = new RuntimeException("Provider unavailable: " + CLOUDFLARE_2005_BODY);

            Optional<String> result = ProviderErrorTranslator.translate(error, model);

            assertThat(result).isPresent();
            assertThat(result.get())
                    .contains("2005")
                    .contains("workers-ai/" + model)
                    .doesNotContain("{").doesNotContain("httpCode");
        }
    }

    @Test
    void explainsWorkersAiPermissionsWhenAPrefixedWorkersAiModelFailsWith2005() {
        // Correctly-prefixed Workers AI model still failing 2005: the request routed, so the
        // remaining causes are gateway-side — token permission or model availability.
        Throwable error = new RuntimeException("status code: 400; body: " + CLOUDFLARE_2005_BODY);

        Optional<String> result =
                ProviderErrorTranslator.translate(error, "workers-ai/@cf/openai/gpt-oss-20b");

        assertThat(result).isPresent();
        assertThat(result.get())
                .contains("Workers AI")
                .contains("permission")
                .doesNotContain("{").doesNotContain("httpCode");
    }

    @Test
    void keepsGenericGuidanceForPrefixedModelsOnOtherErrorCodes() {
        // A correctly-prefixed model failing with 2005 is a gateway-side problem (provider key
        // missing, model unavailable) — the pre-#1254 guidance remains the right one.
        Throwable error = new RuntimeException(CLOUDFLARE_2005_BODY);

        assertThat(ProviderErrorTranslator.translate(error, "openai/gpt-4o"))
                .get()
                .satisfies(m -> assertThat(m)
                        .contains("dropdown")
                        .contains("Cloudflare AI Gateway dashboard")
                        .doesNotContain("no provider prefix"));
    }

    @Test
    void returnsEmptyForNonCloudflareErrors() {
        assertThat(ProviderErrorTranslator.translate(
                new RuntimeException("Connection refused"), "openai/gpt-4o")).isEmpty();
        assertThat(ProviderErrorTranslator.translate(null, "x")).isEmpty();
    }

    @Test
    void toleratesSelfReferentialCauseChainWithoutLooping() {
        // A malformed cause chain (t.getCause() == t) must not spin forever.
        RuntimeException selfRef = new RuntimeException("Connection refused") {
            @Override public synchronized Throwable getCause() { return this; }
        };
        assertThat(ProviderErrorTranslator.translate(selfRef, "x")).isEmpty();
    }
}
