package com.devoxx.genie.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityFinding {
    private ScannerType scanner;
    private String ruleId;
    private String title;
    private String description;
    private String severity;
    private String filePath;
    private int startLine;
    private int endLine;
    private String packageName;
    private String installedVersion;
    private String fixedVersion;
    private String fingerprint;
}
