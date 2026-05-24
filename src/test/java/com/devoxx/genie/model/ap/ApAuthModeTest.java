package com.devoxx.genie.model.ap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApAuthModeTest {

    @Test
    void getEnvValue_cachedLogin_isNull() {
        assertThat(ApAuthMode.CACHED_LOGIN.getEnvValue()).isNull();
    }

    @Test
    void getEnvValue_dockerDesktop_isDockerDesktop() {
        assertThat(ApAuthMode.DOCKER_DESKTOP.getEnvValue()).isEqualTo("docker-desktop");
    }

    @Test
    void getEnvValue_manualTokens_isManual() {
        assertThat(ApAuthMode.MANUAL_TOKENS.getEnvValue()).isEqualTo("manual");
    }

    @Test
    void fromName_null_defaultsToCachedLogin() {
        assertThat(ApAuthMode.fromName(null)).isEqualTo(ApAuthMode.CACHED_LOGIN);
    }

    @Test
    void fromName_unknown_defaultsToCachedLogin() {
        assertThat(ApAuthMode.fromName("NOT_A_REAL_MODE")).isEqualTo(ApAuthMode.CACHED_LOGIN);
    }

    @Test
    void fromName_emptyString_defaultsToCachedLogin() {
        assertThat(ApAuthMode.fromName("")).isEqualTo(ApAuthMode.CACHED_LOGIN);
    }

    @Test
    void fromName_caseSensitive_lowercaseUnknown() {
        // valueOf is case-sensitive; lowercase should fall through to the default.
        assertThat(ApAuthMode.fromName("manual_tokens")).isEqualTo(ApAuthMode.CACHED_LOGIN);
    }

    @Test
    void fromName_cachedLogin_roundTrips() {
        assertThat(ApAuthMode.fromName("CACHED_LOGIN")).isEqualTo(ApAuthMode.CACHED_LOGIN);
    }

    @Test
    void fromName_dockerDesktop_roundTrips() {
        assertThat(ApAuthMode.fromName("DOCKER_DESKTOP")).isEqualTo(ApAuthMode.DOCKER_DESKTOP);
    }

    @Test
    void fromName_manualTokens_roundTrips() {
        assertThat(ApAuthMode.fromName("MANUAL_TOKENS")).isEqualTo(ApAuthMode.MANUAL_TOKENS);
    }
}
