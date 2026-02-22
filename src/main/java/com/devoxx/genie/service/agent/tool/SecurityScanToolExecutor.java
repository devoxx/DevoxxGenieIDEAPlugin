package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityScanResult;
import com.devoxx.genie.service.security.SecurityScannerService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Agent tool executor for running security scans.
 * Runs synchronously and returns a JSON summary.
 */
@Slf4j
public class SecurityScanToolExecutor implements ToolExecutor {

    private final Project project;

    public SecurityScanToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            Set<ScannerType> scannerTypes = switch (request.name()) {
                case "run_gitleaks_scan" -> EnumSet.of(ScannerType.GITLEAKS);
                case "run_opengrep_scan" -> EnumSet.of(ScannerType.OPENGREP);
                case "run_trivy_scan"    -> EnumSet.of(ScannerType.TRIVY);
                default -> {
                    // run_security_scan: honour optional scanners param
                    List<String> names = ToolArgumentParser.getStringArray(request.arguments(), "scanners");
                    yield parseScannerTypes(names);
                }
            };

            SecurityScannerService service = SecurityScannerService.getInstance(project);
            SecurityScanResult result = service.runScanSync(scannerTypes);

            return formatResult(result);
        } catch (Exception e) {
            log.error("Security scan tool failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private Set<ScannerType> parseScannerTypes(List<String> names) {
        if (names.isEmpty()) return null;

        Set<ScannerType> types = EnumSet.noneOf(ScannerType.class);
        for (String name : names) {
            switch (name.toLowerCase().trim()) {
                case "gitleaks" -> types.add(ScannerType.GITLEAKS);
                case "opengrep" -> types.add(ScannerType.OPENGREP);
                case "trivy"    -> types.add(ScannerType.TRIVY);
                default -> log.warn("Unknown scanner type: {}", name);
            }
        }
        return types.isEmpty() ? null : types;
    }

    private String formatResult(SecurityScanResult result) {
        JsonObject json = new JsonObject();
        json.addProperty("totalFindings", result.getFindings().size());
        json.addProperty("gitleaksFindings", result.getGitleaksCount());
        json.addProperty("opengrepFindings", result.getOpengrepCount());
        json.addProperty("trivyFindings", result.getTrivyCount());
        json.addProperty("durationMs", result.getDurationMs());

        if (!result.getErrors().isEmpty()) {
            JsonArray errorsArray = new JsonArray();
            result.getErrors().forEach(errorsArray::add);
            json.add("errors", errorsArray);
        }

        // Summary for LLM
        StringBuilder summary = new StringBuilder();
        summary.append("Security scan completed in ").append(result.getDurationMs()).append("ms.\n");
        summary.append("Total findings: ").append(result.getFindings().size()).append("\n");

        if (result.getGitleaksCount() > 0) {
            summary.append("- Gitleaks (secrets): ").append(result.getGitleaksCount()).append("\n");
        }
        if (result.getOpengrepCount() > 0) {
            summary.append("- OpenGrep (SAST): ").append(result.getOpengrepCount()).append("\n");
        }
        if (result.getTrivyCount() > 0) {
            summary.append("- Trivy (SCA): ").append(result.getTrivyCount()).append("\n");
        }
        if (!result.getErrors().isEmpty()) {
            summary.append("Errors: ").append(String.join("; ", result.getErrors())).append("\n");
        }
        if (!Boolean.FALSE.equals(DevoxxGenieStateService.getInstance().getSecurityScanCreateSpecTasks())) {
            summary.append("\nFindings have been created as backlog tasks with [GITLEAKS], [OPENGREP], or [TRIVY] prefixes.");
        } else {
            summary.append("\nSpec task creation is disabled — findings were NOT added to the backlog.");
        }

        json.addProperty("summary", summary.toString());
        return json.toString();
    }
}
