package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenGrep scanner for static application security testing (SAST).
 * Parses SARIF output format.
 */
@Slf4j
public class OpengrepScanner extends AbstractScanner {

    @Override
    protected ScannerType getType() {
        return ScannerType.OPENGREP;
    }

    @Override
    protected List<String> buildCommand(@NotNull String binaryPath,
                                         @NotNull String sourcePath,
                                         @NotNull String tempFile) {
        return List.of(
                binaryPath,
                "scan",
                "--sarif",
                "--experimental",
                "--timeout", "5",
                sourcePath
        );
    }

    @Override
    protected List<SecurityFinding> parseOutput(@NotNull String stdout, @NotNull String tempFile)
            throws SecurityScanException {
        List<SecurityFinding> findings = new ArrayList<>();
        try {
            if (stdout.isBlank()) {
                return findings;
            }

            JsonObject sarif = JsonParser.parseString(stdout).getAsJsonObject();
            JsonArray runs = sarif.getAsJsonArray("runs");
            if (runs == null || runs.isEmpty()) {
                return findings;
            }

            JsonObject firstRun = runs.get(0).getAsJsonObject();
            JsonArray results = firstRun.getAsJsonArray("results");
            if (results == null) {
                return findings;
            }

            for (JsonElement resultEl : results) {
                JsonObject result = resultEl.getAsJsonObject();

                String ruleId = getStr(result, "ruleId");
                String message = "";
                JsonObject messageObj = result.getAsJsonObject("message");
                if (messageObj != null) {
                    message = getStr(messageObj, "text");
                }

                String level = getStr(result, "level");
                String severity = mapSarifLevel(level);

                // Extract location
                String filePath = "";
                int startLine = 0;
                int endLine = 0;
                JsonArray locations = result.getAsJsonArray("locations");
                if (locations != null && !locations.isEmpty()) {
                    JsonObject location = locations.get(0).getAsJsonObject();
                    JsonObject physicalLocation = location.getAsJsonObject("physicalLocation");
                    if (physicalLocation != null) {
                        JsonObject artifactLocation = physicalLocation.getAsJsonObject("artifactLocation");
                        if (artifactLocation != null) {
                            filePath = getStr(artifactLocation, "uri");
                        }
                        JsonObject region = physicalLocation.getAsJsonObject("region");
                        if (region != null) {
                            startLine = getInt(region, "startLine");
                            endLine = getInt(region, "endLine");
                            if (endLine == 0) endLine = startLine;
                        }
                    }
                }

                String fingerprint = "";
                JsonObject fingerprints = result.getAsJsonObject("fingerprints");
                if (fingerprints != null) {
                    JsonElement fp = fingerprints.entrySet().stream().findFirst()
                            .map(java.util.Map.Entry::getValue).orElse(null);
                    if (fp != null && !fp.isJsonNull()) {
                        fingerprint = fp.getAsString();
                    }
                }

                findings.add(SecurityFinding.builder()
                        .scanner(ScannerType.OPENGREP)
                        .ruleId(ruleId)
                        .title(message.isEmpty() ? ruleId : message)
                        .description(buildDescription(ruleId, message, filePath, startLine))
                        .severity(severity)
                        .filePath(filePath)
                        .startLine(startLine)
                        .endLine(endLine)
                        .fingerprint(fingerprint)
                        .build());
            }
        } catch (Exception e) {
            throw new SecurityScanException("Failed to parse opengrep SARIF output: " + e.getMessage(), e);
        }
        return findings;
    }

    @Override
    protected int getTimeoutSeconds() {
        return 60;
    }

    @Override
    protected boolean isAcceptableExitCode(int exitCode) {
        // opengrep returns 1 when findings are found
        return exitCode == 0 || exitCode == 1;
    }

    @Contract(pure = true)
    private static @NonNull String mapSarifLevel(String level) {
        if (level == null) return "medium";
        return switch (level.toLowerCase()) {
            case "error" -> "high";
            case "warning" -> "medium";
            case "note", "none" -> "low";
            default -> "medium";
        };
    }

    private @NonNull String buildDescription(String ruleId, @NonNull String message, String filePath, int startLine) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Rule:** ").append(ruleId).append("\n");
        if (!message.isEmpty()) {
            sb.append("**Finding:** ").append(message).append("\n");
        }
        sb.append("**Location:** ").append(filePath).append(":").append(startLine).append("\n");
        sb.append("\n**Remediation:** Review the flagged code pattern and apply the suggested fix from the rule documentation.");
        return sb.toString();
    }

    private static String getStr(@NonNull JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : "";
    }

    private static int getInt(@NonNull JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && !el.isJsonNull() ? el.getAsInt() : 0;
    }
}
