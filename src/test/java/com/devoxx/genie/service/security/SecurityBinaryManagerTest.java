package com.devoxx.genie.service.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityBinaryManagerTest {

    @Test
    void detectPlatform_returnsNonNullPlatformString() {
        String platform = SecurityBinaryManager.detectPlatform();
        assertThat(platform).isNotNull();
        assertThat(platform).matches("(mac_arm64|mac_x64|linux_arm64|linux_x64|win_x64)");
    }
}
