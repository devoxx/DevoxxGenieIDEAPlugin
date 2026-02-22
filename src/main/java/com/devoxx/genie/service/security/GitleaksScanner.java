package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gitleaks scanner for detecting hardcoded secrets.
 */
@Slf4j
public class GitleaksScanner extends AbstractScanner {

    @Override
    protected ScannerType getType() {
        return ScannerType.GITLEAKS;
    }

    @Override
    protected List<String> buildCommand(@NotNull String binaryPath,
                                         @NotNull String sourcePath,
                                         @NotNull String tempFile) {
        return List.of(
                binaryPath,
                "detect",
                "--no-git",
                "--exit-code=0",
                "--max-target-megabytes=1",
                "--report-format=json",
                "--report-path=" + tempFile,
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

            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                return findings;
            }

            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                findings.add(SecurityFinding.builder()
                        .scanner(ScannerType.GITLEAKS)
                        .ruleId(getStr(obj, "RuleID"))
                        .title(getStr(obj, "Description"))
                        .description(buildDescription(obj))
                        .severity("high")
                        .filePath(getStr(obj, "File"))
                        .startLine(getInt(obj, "StartLine"))
                        .endLine(getInt(obj, "EndLine"))
                        .fingerprint(getStr(obj, "Fingerprint"))
                        .build());
            }
        } catch (Exception e) {
            throw new SecurityScanException("Failed to parse gitleaks output: " + e.getMessage(), e);
        }
        return findings;
    }

    @Override
    protected int getTimeoutSeconds() {
        return 30;
    }

    private String buildDescription(JsonObject obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Rule:** ").append(getStr(obj, "RuleID")).append("\n");
        sb.append("**Match:** `").append(truncateMatch(getStr(obj, "Match"))).append("`\n");
        sb.append("**Secret:** `").append(maskSecret(getStr(obj, "Secret"))).append("`\n");
        String author = getStr(obj, "Author");
        if (!author.isEmpty()) {
            sb.append("**Author:** ").append(author).append("\n");
        }
        sb.append("\n**Remediation:** Rotate the exposed secret immediately and remove it from source code. ");
        sb.append("Use environment variables or a secrets manager instead.");
        return sb.toString();
    }

    private static String maskSecret(String secret) {
        if (secret.length() <= 8) return "****";
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    private static String truncateMatch(String match) {
        return match.length() > 100 ? match.substring(0, 100) + "..." : match;
    }

    private static String getStr(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    private static int getInt(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsInt() : 0;
    }
}
