package com.devoxx.genie.model.security;

import lombok.Getter;

@Getter
public enum ScannerType {
    GITLEAKS("gitleaks", "Gitleaks", "Detects hardcoded secrets like passwords, API keys, and tokens"),
    OPENGREP("opengrep", "OpenGrep", "Static application security testing (SAST) scanner"),
    TRIVY("trivy", "Trivy", "Software composition analysis (SCA) for known vulnerabilities");

    private final String id;
    private final String displayName;
    private final String description;

    ScannerType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }
}
