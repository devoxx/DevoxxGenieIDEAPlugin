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
