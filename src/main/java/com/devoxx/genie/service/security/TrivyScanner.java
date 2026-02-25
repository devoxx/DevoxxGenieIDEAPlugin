package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Trivy scanner for software composition analysis (SCA).
 * Detects known vulnerabilities in project dependencies.
 */
@Slf4j
public class TrivyScanner extends AbstractScanner {

    @Override
    protected ScannerType getType() {
        return ScannerType.TRIVY;
    }

    @Override
    protected List<String> buildCommand(@NotNull String binaryPath,
                                         @NotNull String sourcePath,
                                         @NotNull String tempFile) {
        return List.of(
                binaryPath,
                "fs",
                "-f", "json",
                "--scanners", "vuln",
                "-o", tempFile,
                sourcePath
        );
    }

    @Override
    protected List<SecurityFinding> parseOutput(@NotNull String stdout, @NotNull String tempFile)
            throws SecurityScanException {
        List<SecurityFinding> findings = new ArrayList<>();
        try {
            String json = Files.readString(Path.of(tempFile));
            if (json.isBlank()) {
                return findings;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("Results");
            if (results == null) {
                return findings;
            }

            for (JsonElement resultEl : results) {
                JsonObject result = resultEl.getAsJsonObject();
                String target = getStr(result, "Target");
                JsonArray vulnerabilities = result.getAsJsonArray("Vulnerabilities");
                if (vulnerabilities == null) {
                    continue;
                }

                for (JsonElement vulnEl : vulnerabilities) {
                    JsonObject vuln = vulnEl.getAsJsonObject();

                    String vulnId = getStr(vuln, "VulnerabilityID");
                    String pkgName = getStr(vuln, "PkgName");
                    String installed = getStr(vuln, "InstalledVersion");
                    String fixed = getStr(vuln, "FixedVersion");
                    String severity = mapTrivySeverity(getStr(vuln, "Severity"));
                    String title = getStr(vuln, "Title");
                    String description = getStr(vuln, "Description");

                    findings.add(SecurityFinding.builder()
                            .scanner(ScannerType.TRIVY)
                            .ruleId(vulnId)
                            .title(title.isEmpty() ? vulnId + " in " + pkgName : title)
                            .description(buildDescription(vulnId, description, pkgName, installed, fixed, target))
                            .severity(severity)
                            .filePath(target)
                            .packageName(pkgName)
                            .installedVersion(installed)
                            .fixedVersion(fixed)
                            .fingerprint(vulnId + ":" + pkgName + ":" + installed)
                            .build());
                }
            }
        } catch (Exception e) {
            throw new SecurityScanException("Failed to parse trivy output: " + e.getMessage(), e);
        }
        return findings;
    }

    @Override
    protected int getTimeoutSeconds() {
        return 120;
    }

    private static String mapTrivySeverity(String severity) {
        if (severity == null) return "medium";
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "HIGH" -> "high";
            case "MEDIUM" -> "medium";
            case "LOW", "UNKNOWN" -> "low";
            default -> "medium";
        };
    }

    private @NonNull String buildDescription(String vulnId,
                                             @NonNull String description,
                                             String pkgName,
                                             String installed,
                                             String fixed,
                                             String target) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Vulnerability:** ").append(vulnId).append("\n");
        if (!description.isEmpty()) {
            sb.append("**Description:** ").append(description).append("\n");
        }
        sb.append("**Package:** ").append(pkgName).append("@").append(installed).append("\n");
        sb.append("**Target:** ").append(target).append("\n");
        if (!fixed.isEmpty()) {
            sb.append("**Fix available:** Upgrade to ").append(pkgName).append("@").append(fixed).append("\n");
        } else {
            sb.append("**Fix available:** No fix version available yet\n");
        }
        sb.append("\n**Remediation:** Update the dependency to the fixed version or evaluate if the vulnerability applies to your usage.");
        return sb.toString();
    }

    private static String getStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }
}
