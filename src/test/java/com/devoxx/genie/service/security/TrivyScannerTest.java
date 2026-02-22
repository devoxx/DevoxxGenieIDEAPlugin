package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrivyScannerTest {

    private final TrivyScanner scanner = new TrivyScanner();

    @Test
    void parseOutput_withVulnerabilities_returnsSecurityFindings(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "SchemaVersion": 2,
                  "Results": [
                    {
                      "Target": "build.gradle.kts",
                      "Vulnerabilities": [
                        {
                          "VulnerabilityID": "CVE-2023-44487",
                          "PkgName": "io.netty:netty-codec-http2",
                          "InstalledVersion": "4.1.93.Final",
                          "FixedVersion": "4.1.100.Final",
                          "Severity": "HIGH",
                          "Title": "HTTP/2 Rapid Reset Attack",
                          "Description": "The HTTP/2 protocol allows denial of service..."
                        }
                      ]
                    }
                  ]
                }
                """;

        Path reportFile = tempDir.resolve("trivy-report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());

        assertThat(findings).hasSize(1);
        SecurityFinding finding = findings.get(0);
        assertThat(finding.getScanner()).isEqualTo(ScannerType.TRIVY);
        assertThat(finding.getRuleId()).isEqualTo("CVE-2023-44487");
        assertThat(finding.getTitle()).isEqualTo("HTTP/2 Rapid Reset Attack");
        assertThat(finding.getSeverity()).isEqualTo("high");
        assertThat(finding.getPackageName()).isEqualTo("io.netty:netty-codec-http2");
        assertThat(finding.getInstalledVersion()).isEqualTo("4.1.93.Final");
        assertThat(finding.getFixedVersion()).isEqualTo("4.1.100.Final");
        assertThat(finding.getFilePath()).isEqualTo("build.gradle.kts");
    }

    @Test
    void parseOutput_criticalSeverity_mapsToHigh(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "Results": [
                    {
                      "Target": "pom.xml",
                      "Vulnerabilities": [
                        {
                          "VulnerabilityID": "CVE-2021-44228",
                          "PkgName": "org.apache.logging.log4j:log4j-core",
                          "InstalledVersion": "2.14.1",
                          "FixedVersion": "2.17.0",
                          "Severity": "CRITICAL",
                          "Title": "Log4Shell"
                        }
                      ]
                    }
                  ]
                }
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo("high");
    }

    @Test
    void parseOutput_lowSeverity_mapsToLow(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "Results": [
                    {
                      "Target": "package.json",
                      "Vulnerabilities": [
                        {
                          "VulnerabilityID": "CVE-2023-0001",
                          "PkgName": "lodash",
                          "InstalledVersion": "4.17.20",
                          "Severity": "LOW",
                          "Title": "Minor issue"
                        }
                      ]
                    }
                  ]
                }
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo("low");
        assertThat(findings.get(0).getFixedVersion()).isEmpty();
    }

    @Test
    void parseOutput_noVulnerabilities_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "Results": [
                    {
                      "Target": "build.gradle.kts"
                    }
                  ]
                }
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).isEmpty();
    }

    @Test
    void parseOutput_emptyReport_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, "");

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).isEmpty();
    }

    @Test
    void parseOutput_multipleTargets_aggregatesFindings(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "Results": [
                    {
                      "Target": "build.gradle.kts",
                      "Vulnerabilities": [
                        {
                          "VulnerabilityID": "CVE-2023-0001",
                          "PkgName": "lib-a",
                          "InstalledVersion": "1.0",
                          "Severity": "MEDIUM",
                          "Title": "Issue A"
                        }
                      ]
                    },
                    {
                      "Target": "package.json",
                      "Vulnerabilities": [
                        {
                          "VulnerabilityID": "CVE-2023-0002",
                          "PkgName": "lib-b",
                          "InstalledVersion": "2.0",
                          "Severity": "HIGH",
                          "Title": "Issue B"
                        }
                      ]
                    }
                  ]
                }
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getFilePath()).isEqualTo("build.gradle.kts");
        assertThat(findings.get(1).getFilePath()).isEqualTo("package.json");
    }
}
