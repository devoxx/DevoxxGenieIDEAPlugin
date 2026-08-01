package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudflareGatewayUrlTest {

    @Test
    void assemblesCompatBaseUrlFromAccountAndGateway() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("acct123", "default"))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/compat");
    }

    @Test
    void trimsWhitespaceAndStraySlashesInInputs() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("  acct123 ", " my-gw/ "))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/my-gw/compat");
    }

    @Test
    void returnsNullWhenAccountIdBlank() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("   ", "default")).isNull();
    }

    @Test
    void fallsBackToDefaultGatewayWhenBlank() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("acct123", "  "))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/compat");
    }

    // Issue #1256: Workers AI models are routed through the gateway's workers-ai/v1 provider path
    // (Workers AI's own OpenAI-compatibility layer) because /compat cannot express tool calling
    // against the Workers AI native schema.

    @Test
    void assemblesWorkersAiBaseUrlFromAccountAndGateway() {
        assertThat(CloudflareGatewayUrl.workersAiBaseUrl("acct123", "default"))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/workers-ai/v1");
    }

    @Test
    void workersAiBaseUrlFallsBackToDefaultGatewayAndRejectsBlankAccount() {
        assertThat(CloudflareGatewayUrl.workersAiBaseUrl("acct123", " "))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/workers-ai/v1");
        assertThat(CloudflareGatewayUrl.workersAiBaseUrl("  ", "default")).isNull();
    }
}
