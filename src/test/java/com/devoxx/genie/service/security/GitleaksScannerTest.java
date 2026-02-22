package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitleaksScannerTest {

    private final GitleaksScanner scanner = new GitleaksScanner();

    @Test
    void parseOutput_withFindings_returnsSecurityFindings(@TempDir Path tempDir) throws Exception {
        String json = """
                [
                  {
                    "Description": "AWS Access Key",
                    "StartLine": 10,
                    "EndLine": 10,
                    "StartColumn": 5,
                    "EndColumn": 25,
                    "Match": "AKIAIOSFODNN7EXAMPLE",
                    "Secret": "AKIAIOSFODNN7EXAMPLE",
                    "File": "config/secrets.yaml",
                    "SymlinkFile": "",
                    "Commit": "",
                    "Entropy": 3.5,
                    "Author": "developer",
                    "Email": "",
                    "Date": "",
                    "Message": "",
                    "Tags": [],
                    "RuleID": "aws-access-key-id",
                    "Fingerprint": "config/secrets.yaml:aws-access-key-id:10"
                  }
                ]
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());

        assertThat(findings).hasSize(1);
        SecurityFinding finding = findings.get(0);
        assertThat(finding.getScanner()).isEqualTo(ScannerType.GITLEAKS);
        assertThat(finding.getRuleId()).isEqualTo("aws-access-key-id");
        assertThat(finding.getTitle()).isEqualTo("AWS Access Key");
        assertThat(finding.getSeverity()).isEqualTo("high");
        assertThat(finding.getFilePath()).isEqualTo("config/secrets.yaml");
        assertThat(finding.getStartLine()).isEqualTo(10);
        assertThat(finding.getFingerprint()).isEqualTo("config/secrets.yaml:aws-access-key-id:10");
    }

    @Test
    void parseOutput_emptyReport_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, "");

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).isEmpty();
    }

    @Test
    void parseOutput_emptyArray_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, "[]");

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).isEmpty();
    }

    @Test
    void parseOutput_multipleFindings_returnsAll(@TempDir Path tempDir) throws Exception {
        String json = """
                [
                  {
                    "Description": "Generic API Key",
                    "StartLine": 5,
                    "EndLine": 5,
                    "Match": "api_key=abc123",
                    "Secret": "abc123",
                    "File": "app.properties",
                    "RuleID": "generic-api-key",
                    "Fingerprint": "fp1"
                  },
                  {
                    "Description": "Private Key",
                    "StartLine": 20,
                    "EndLine": 25,
                    "Match": "-----BEGIN RSA PRIVATE KEY-----",
                    "Secret": "MIIEpAIBAAKCAQEA...",
                    "File": "keys/server.pem",
                    "RuleID": "private-key",
                    "Fingerprint": "fp2"
                  }
                ]
                """;

        Path reportFile = tempDir.resolve("report.json");
        Files.writeString(reportFile, json);

        List<SecurityFinding> findings = scanner.parseOutput("", reportFile.toString());
        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getRuleId()).isEqualTo("generic-api-key");
        assertThat(findings.get(1).getRuleId()).isEqualTo("private-key");
        assertThat(findings).allSatisfy(f -> assertThat(f.getSeverity()).isEqualTo("high"));
    }
}
