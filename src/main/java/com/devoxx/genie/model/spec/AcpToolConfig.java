package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcpToolConfig {

    @Getter
    public enum AcpType {
        CLAUDE("Claude", "claude-code-acp", "acp"),
        COPILOT("Copilot", "copilot", "--acp"),
        KIMI("Kimi", "kimi", "acp"),
        GEMINI("Gemini", "gemini", "acp"),
        KILOCODE("Kilocode", "kilocode", "acp"),
        CUSTOM("Custom", "", "acp");

        private final String displayName;
        private final String defaultExecutablePath;
        private final String defaultAcpFlag;

        AcpType(String displayName, String defaultExecutablePath, String defaultAcpFlag) {
            this.displayName = displayName;
            this.defaultExecutablePath = defaultExecutablePath;
            this.defaultAcpFlag = defaultAcpFlag;
        }
    }

    @Builder.Default
    private AcpType type = AcpType.CUSTOM;
    @Builder.Default
    private String name = "";
    @Builder.Default
    private String executablePath = "";
    @Builder.Default
    private String acpFlag = "acp";
    @Builder.Default
    private boolean enabled = true;
}
