package com.devoxx.genie.model.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityScanResult {
    @Builder.Default
    private List<SecurityFinding> findings = new ArrayList<>();
    private int gitleaksCount;
    private int opengrepCount;
    private int trivyCount;
    private long durationMs;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
