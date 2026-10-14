---
type: C4 Component
title: Security scanning
status: stable
groma:
  id: security-scanning
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/security/ScannerType.java
      symbol: ScannerType
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/security/SecurityFinding.java
      symbol: SecurityFinding
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/security/SecurityScanResult.java
      symbol: SecurityScanResult
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/AbstractScanner.java
      symbol: AbstractScanner
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/GitleaksScanner.java
      symbol: GitleaksScanner
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/OpengrepScanner.java
      symbol: OpengrepScanner
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/SecurityBinaryManager.java
      symbol: SecurityBinaryManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/SecurityScanException.java
      symbol: SecurityScanException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/SecurityScanListener.java
      symbol: SecurityScanListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/SecurityScannerService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/security/TrivyScanner.java
      symbol: TrivyScanner
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/security/SecurityScanSettingsComponent.java
      symbol: SecurityScanSettingsComponent
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/security/SecurityScanSettingsConfigurable.java
      symbol: SecurityScanSettingsConfigurable
  group: Agent mode
description: Runs Gitleaks, OpenGrep and Trivy and reports findings
---

Resolves the scanner binaries, runs them on the project, parses their output into findings and can file them as Backlog tasks. Available from settings and as an agent tool.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/security/SecurityScannerService.java](../../../../../../src/main/java/com/devoxx/genie/service/security/SecurityScannerService.java) | [src/main/java/com/devoxx/genie/service/spec/SpecService.java](../../../../../../src/main/java/com/devoxx/genie/service/spec/SpecService.java) | Files findings as tasks | Java call |
| [src/main/java/com/devoxx/genie/service/security/AbstractScanner.java](../../../../../../src/main/java/com/devoxx/genie/service/security/AbstractScanner.java) | [security-scanner-tools](../../../../../externals/security-scanner-tools.md) | Runs scanners | Child process |
