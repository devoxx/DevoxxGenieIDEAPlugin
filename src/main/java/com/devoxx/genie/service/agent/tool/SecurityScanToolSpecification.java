package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Tool specification for the run_security_scan agent tool.
 */
public final class SecurityScanToolSpecification {

    private SecurityScanToolSpecification() {}

    public static ToolSpecification securityScan() {
        return ToolSpecification.builder()
                .name("run_security_scan")
                .description("Run all available security scanners (gitleaks for secrets, opengrep for SAST, trivy for SCA) " +
                        "against the current project. Findings are automatically created as backlog tasks. " +
                        "Returns a JSON summary of the scan results including finding counts and any errors.")
                .parameters(JsonObjectSchema.builder()
                        .addProperty("scanners", JsonArraySchema.builder()
                                .items(JsonStringSchema.builder()
                                        .description("Scanner name: 'gitleaks', 'opengrep', or 'trivy'")
                                        .build())
                                .description("Optional list of scanners to run. If omitted, all available scanners are used.")
                                .build())
                        .build())
                .build();
    }

    public static ToolSpecification gitleaksScan() {
        return ToolSpecification.builder()
                .name("run_gitleaks_scan")
                .description("Run gitleaks to detect hardcoded secrets (passwords, API keys, tokens, certificates) " +
                        "in the current project. Findings are automatically created as high-priority backlog tasks. " +
                        "Returns a JSON summary with the number of secrets found.")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    public static ToolSpecification opengrepScan() {
        return ToolSpecification.builder()
                .name("run_opengrep_scan")
                .description("Run opengrep (SAST) to detect security vulnerabilities in source code such as " +
                        "SQL injection, XSS, path traversal, insecure deserialization, and other code-level issues. " +
                        "Findings are automatically created as backlog tasks. " +
                        "Returns a JSON summary with the number of issues found.")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    public static ToolSpecification trivyScan() {
        return ToolSpecification.builder()
                .name("run_trivy_scan")
                .description("Run trivy (SCA) to detect known CVE vulnerabilities in project dependencies " +
                        "(npm, Maven, Gradle, pip, Go modules, etc.). " +
                        "Findings are automatically created as backlog tasks with fix version information. " +
                        "Returns a JSON summary with vulnerability counts by severity.")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }
}
