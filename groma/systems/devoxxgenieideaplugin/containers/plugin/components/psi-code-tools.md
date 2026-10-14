---
type: C4 Component
title: PSI code navigation tools
status: stable
groma:
  id: psi-code-tools
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/CalculateComplexityToolExecutor.java
      symbol: CalculateComplexityToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/DocumentSymbolsToolExecutor.java
      symbol: DocumentSymbolsToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindCalleesToolExecutor.java
      symbol: FindCalleesToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDeadCodeToolExecutor.java
      symbol: FindDeadCodeToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java
      symbol: FindDefinitionToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindImplementationsToolExecutor.java
      symbol: FindImplementationsToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindReferencesToolExecutor.java
      symbol: FindReferencesToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java
      symbol: FindSymbolsToolExecutor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolCatalog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
      symbol: PsiToolUtils
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/agent/tool/psi/TraceCallChainsToolExecutor.java
  group: Agent mode
  technology: IntelliJ PSI
description: Agent tools backed by the IDE code model
---

Find symbols, definitions, references, implementations, callees, call chains, document symbols, dead code and complexity, using IntelliJ PSI under read actions.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java](../../../../../../src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java) | [intellij-platform](../../../../../externals/intellij-platform.md) | Queries code model | IntelliJ PSI |
