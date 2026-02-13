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
        KIMI("Kimi", "kimi"),
        GEMINI("Gemini", "gemini"),
        KILOCODE("Kilocode", "kilocode"),
        CUSTOM("Custom", "");

        private final String displayName;
        private final String defaultExecutablePath;

        AcpType(String displayName, String defaultExecutablePath) {
            this.displayName = displayName;
            this.defaultExecutablePath = defaultExecutablePath;
        }
    }

    @Builder.Default
    private AcpType type = AcpType.CUSTOM;
    @Builder.Default
    private String name = "";
    @Builder.Default
    private String executablePath = "";
    @Builder.Default
    private boolean enabled = true;
}
