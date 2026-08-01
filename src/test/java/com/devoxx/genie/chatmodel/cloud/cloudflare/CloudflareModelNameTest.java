package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1254: Cloudflare AI Gateway's {@code /compat} endpoint addresses models as
 * {@code provider/model}. Users copy Workers AI ids from the Cloudflare dashboard in their bare
 * {@code @cf/...} form, which the gateway rejects with 400 code 2008 ("Invalid provider") because
 * {@code @cf} is parsed as the provider segment. {@code @cf/} is the Workers AI namespace, so the
 * {@code workers-ai/} prefix is added deterministically.
 */
class CloudflareModelNameTest {

    @Test
    void prefixesBareWorkersAiIdsWithWorkersAiProvider() {
        // The exact models from issue #1254.
        assertThat(CloudflareModelName.normalize("@cf/openai/gpt-oss-20b"))
                .isEqualTo("workers-ai/@cf/openai/gpt-oss-20b");
        assertThat(CloudflareModelName.normalize("@cf/zai-org/glm-4.7-flash"))
                .isEqualTo("workers-ai/@cf/zai-org/glm-4.7-flash");
    }

    @Test
    void leavesAlreadyPrefixedWorkersAiIdsUntouched() {
        assertThat(CloudflareModelName.normalize("workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast"))
                .isEqualTo("workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast");
    }

    @Test
    void leavesRegularProviderPrefixedIdsUntouched() {
        assertThat(CloudflareModelName.normalize("openai/gpt-4o")).isEqualTo("openai/gpt-4o");
        assertThat(CloudflareModelName.normalize("anthropic/claude-4-5-sonnet"))
                .isEqualTo("anthropic/claude-4-5-sonnet");
    }

    @Test
    void doesNotGuessAProviderForBareModelIds() {
        // 'kimi-k2.6' from the issue could belong to any provider — no prefix is invented for it;
        // the ProviderErrorTranslator guidance covers this case instead.
        assertThat(CloudflareModelName.normalize("kimi-k2.6")).isEqualTo("kimi-k2.6");
    }

    @Test
    void trimsWhitespaceBeforeInspectingTheId() {
        assertThat(CloudflareModelName.normalize("  @cf/openai/gpt-oss-20b  "))
                .isEqualTo("workers-ai/@cf/openai/gpt-oss-20b");
        assertThat(CloudflareModelName.normalize(" openai/gpt-4o ")).isEqualTo("openai/gpt-4o");
    }

    @Test
    void passesNullThrough() {
        assertThat(CloudflareModelName.normalize(null)).isNull();
    }
}
