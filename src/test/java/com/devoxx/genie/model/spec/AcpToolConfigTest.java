package com.devoxx.genie.model.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcpToolConfigTest {

    @Test
    void testBuilder_defaultValues() {
        AcpToolConfig config = AcpToolConfig.builder().build();

        assertThat(config.getType()).isEqualTo(AcpToolConfig.AcpType.CUSTOM);
        assertThat(config.getName()).isEmpty();
        assertThat(config.getExecutablePath()).isEmpty();
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    void testBuilder_customValues() {
        AcpToolConfig config = AcpToolConfig.builder()
                .type(AcpToolConfig.AcpType.KIMI)
                .name("Kimi")
                .executablePath("/usr/local/bin/kimi")
                .enabled(false)
                .build();

        assertThat(config.getType()).isEqualTo(AcpToolConfig.AcpType.KIMI);
        assertThat(config.getName()).isEqualTo("Kimi");
        assertThat(config.getExecutablePath()).isEqualTo("/usr/local/bin/kimi");
        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    void testAcpType_displayNames() {
        assertThat(AcpToolConfig.AcpType.CLAUDE.getDisplayName()).isEqualTo("Claude");
        assertThat(AcpToolConfig.AcpType.COPILOT.getDisplayName()).isEqualTo("Copilot");
        assertThat(AcpToolConfig.AcpType.KIMI.getDisplayName()).isEqualTo("Kimi");
        assertThat(AcpToolConfig.AcpType.GEMINI.getDisplayName()).isEqualTo("Gemini");
        assertThat(AcpToolConfig.AcpType.KILOCODE.getDisplayName()).isEqualTo("Kilocode");
        assertThat(AcpToolConfig.AcpType.CUSTOM.getDisplayName()).isEqualTo("Custom");
    }

    @Test
    void testAcpType_defaultExecutablePaths() {
        assertThat(AcpToolConfig.AcpType.CLAUDE.getDefaultExecutablePath()).isEqualTo("claude-code-acp");
        assertThat(AcpToolConfig.AcpType.COPILOT.getDefaultExecutablePath()).isEqualTo("copilot");
        assertThat(AcpToolConfig.AcpType.KIMI.getDefaultExecutablePath()).isEqualTo("kimi");
        assertThat(AcpToolConfig.AcpType.GEMINI.getDefaultExecutablePath()).isEqualTo("gemini");
        assertThat(AcpToolConfig.AcpType.KILOCODE.getDefaultExecutablePath()).isEqualTo("kilocode");
        assertThat(AcpToolConfig.AcpType.CUSTOM.getDefaultExecutablePath()).isEmpty();
    }

    @Test
    void testAcpType_defaultAcpFlags() {
        assertThat(AcpToolConfig.AcpType.CLAUDE.getDefaultAcpFlag()).isEqualTo("acp");
        assertThat(AcpToolConfig.AcpType.COPILOT.getDefaultAcpFlag()).isEqualTo("--acp");
        assertThat(AcpToolConfig.AcpType.KIMI.getDefaultAcpFlag()).isEqualTo("acp");
        assertThat(AcpToolConfig.AcpType.GEMINI.getDefaultAcpFlag()).isEqualTo("acp");
        assertThat(AcpToolConfig.AcpType.KILOCODE.getDefaultAcpFlag()).isEqualTo("acp");
        assertThat(AcpToolConfig.AcpType.CUSTOM.getDefaultAcpFlag()).isEqualTo("acp");
    }

    @Test
    void testAcpType_allValues() {
        AcpToolConfig.AcpType[] values = AcpToolConfig.AcpType.values();
        assertThat(values).hasSize(6);
        assertThat(values).containsExactly(
                AcpToolConfig.AcpType.CLAUDE,
                AcpToolConfig.AcpType.COPILOT,
                AcpToolConfig.AcpType.KIMI,
                AcpToolConfig.AcpType.GEMINI,
                AcpToolConfig.AcpType.KILOCODE,
                AcpToolConfig.AcpType.CUSTOM
        );
    }

    @Test
    void testEquality() {
        AcpToolConfig config1 = AcpToolConfig.builder()
                .type(AcpToolConfig.AcpType.KIMI)
                .name("Kimi")
                .executablePath("kimi")
                .enabled(true)
                .build();

        AcpToolConfig config2 = AcpToolConfig.builder()
                .type(AcpToolConfig.AcpType.KIMI)
                .name("Kimi")
                .executablePath("kimi")
                .enabled(true)
                .build();

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void testInequality_differentEnabled() {
        AcpToolConfig config1 = AcpToolConfig.builder()
                .type(AcpToolConfig.AcpType.KIMI)
                .name("Kimi")
                .enabled(true)
                .build();

        AcpToolConfig config2 = AcpToolConfig.builder()
                .type(AcpToolConfig.AcpType.KIMI)
                .name("Kimi")
                .enabled(false)
                .build();

        assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    void testNoArgConstructor() {
        AcpToolConfig config = new AcpToolConfig();
        assertThat(config.getType()).isEqualTo(AcpToolConfig.AcpType.CUSTOM);
        assertThat(config.getName()).isEmpty();
        assertThat(config.getExecutablePath()).isEmpty();
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    void testSetters() {
        AcpToolConfig config = new AcpToolConfig();
        config.setType(AcpToolConfig.AcpType.GEMINI);
        config.setName("Gemini");
        config.setExecutablePath("/usr/bin/gemini");
        config.setEnabled(false);

        assertThat(config.getType()).isEqualTo(AcpToolConfig.AcpType.GEMINI);
        assertThat(config.getName()).isEqualTo("Gemini");
        assertThat(config.getExecutablePath()).isEqualTo("/usr/bin/gemini");
        assertThat(config.isEnabled()).isFalse();
    }
}
