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
}
