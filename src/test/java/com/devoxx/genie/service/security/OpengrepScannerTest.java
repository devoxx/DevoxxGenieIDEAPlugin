package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpengrepScannerTest {

    private final OpengrepScanner scanner = new OpengrepScanner();

    @Test
    void parseOutput_sarifWithFindings_returnsSecurityFindings() throws Exception {
        String sarif = """
                {
                  "version": "2.1.0",
                  "runs": [
                    {
                      "results": [
                        {
                          "ruleId": "java.lang.security.audit.sqli.tainted-sql-string",
                          "level": "error",
                          "message": {
                            "text": "SQL injection vulnerability detected"
                          },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": {
                                  "uri": "src/main/java/UserDao.java"
                                },
                                "region": {
                                  "startLine": 42,
                                  "endLine": 42
                                }
                              }
                            }
                          ],
                          "fingerprints": {
                            "matchBasedId/v1": "abc123"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        List<SecurityFinding> findings = scanner.parseOutput(sarif, "");

        assertThat(findings).hasSize(1);
        SecurityFinding finding = findings.get(0);
        assertThat(finding.getScanner()).isEqualTo(ScannerType.OPENGREP);
        assertThat(finding.getRuleId()).isEqualTo("java.lang.security.audit.sqli.tainted-sql-string");
        assertThat(finding.getTitle()).isEqualTo("SQL injection vulnerability detected");
        assertThat(finding.getSeverity()).isEqualTo("high");
        assertThat(finding.getFilePath()).isEqualTo("src/main/java/UserDao.java");
        assertThat(finding.getStartLine()).isEqualTo(42);
        assertThat(finding.getFingerprint()).isEqualTo("abc123");
    }

    @Test
    void parseOutput_emptyResults_returnsEmptyList() throws Exception {
        String sarif = """
                {
                  "version": "2.1.0",
                  "runs": [{ "results": [] }]
                }
                """;

        List<SecurityFinding> findings = scanner.parseOutput(sarif, "");
        assertThat(findings).isEmpty();
    }

    @Test
    void parseOutput_warningLevel_mapsMediumSeverity() throws Exception {
        String sarif = """
                {
                  "version": "2.1.0",
                  "runs": [
                    {
                      "results": [
                        {
                          "ruleId": "some-warning-rule",
                          "level": "warning",
                          "message": { "text": "Potential issue" },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": { "uri": "file.java" },
                                "region": { "startLine": 10 }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<SecurityFinding> findings = scanner.parseOutput(sarif, "");
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo("medium");
    }

    @Test
    void parseOutput_noteLevel_mapsLowSeverity() throws Exception {
        String sarif = """
                {
                  "version": "2.1.0",
                  "runs": [
                    {
                      "results": [
                        {
                          "ruleId": "info-rule",
                          "level": "note",
                          "message": { "text": "Informational" },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": { "uri": "file.java" },
                                "region": { "startLine": 1 }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<SecurityFinding> findings = scanner.parseOutput(sarif, "");
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getSeverity()).isEqualTo("low");
    }

    @Test
    void parseOutput_blankInput_returnsEmptyList() throws Exception {
        List<SecurityFinding> findings = scanner.parseOutput("", "");
        assertThat(findings).isEmpty();
    }
}
