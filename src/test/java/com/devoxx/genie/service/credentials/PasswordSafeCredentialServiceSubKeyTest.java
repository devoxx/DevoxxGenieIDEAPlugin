package com.devoxx.genie.service.credentials;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Sub-key addressed credentials; PasswordSafe is skipped in headless runs so the in-memory path is exercised. */
class PasswordSafeCredentialServiceSubKeyTest {

    private final PasswordSafeCredentialService service = new PasswordSafeCredentialService();

    @Test
    void subKey_roundTrips() {
        service.setCredential("router.acme", "secret");

        assertThat(service.getCredential("router.acme")).isEqualTo("secret");
        assertThat(service.getCredential("router.other")).isEmpty();
    }

    @Test
    void enumAndSubKey_shareOneEntry() {
        service.setCredential(CredentialKey.OPEN_ROUTER_KEY, "k1");
        assertThat(service.getCredential("openRouterKey")).isEqualTo("k1");

        service.setCredential("openRouterKey", "k2");
        assertThat(service.getCredential(CredentialKey.OPEN_ROUTER_KEY)).isEqualTo("k2");
    }

    @Test
    void emptyString_removesEntry() {
        service.setCredential("router.acme", "secret");
        service.setCredential("router.acme", "");

        assertThat(service.getCredential("router.acme")).isEmpty();
    }
}
