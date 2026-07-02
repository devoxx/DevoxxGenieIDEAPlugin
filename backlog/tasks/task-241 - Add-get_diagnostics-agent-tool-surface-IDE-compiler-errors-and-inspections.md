---
id: TASK-241
title: Add get_diagnostics agent tool — surface IDE compiler errors and inspections
status: To Do
assignee: []
created_date: '2026-07-02 19:32'
labels:
  - agent-mode
  - tools
  - psi
  - feature
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
  - src/main/java/com/devoxx/genie/service/agent/tool/RunTestsToolExecutor.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add a `get_diagnostics` built-in agent tool that returns the IDE's compiler errors and
inspection warnings for a file (or a set of files). This closes the **write → verify → fix**
loop cheaply: today, after the agent edits a file with `edit_file`/`write_file`, its only way
to learn whether the code even compiles is `run_tests` or `run_command` — a full Gradle/Maven
cycle that takes minutes (and `run_command` defaults to a 30-second timeout). Meanwhile the
IDE's highlighting daemon already has this information in memory.

This is the single highest-impact missing agent tool: it is the core feedback loop that makes
CLI agents (Claude Code, Cursor) effective, and being *inside* the IDE means DevoxxGenie can
answer it in seconds without spawning a build. It improves **every** agent run that writes code.

### Current state

- All nine PSI tools (`find_symbols`, `find_references`, `find_callees`, …) are read-only
  *navigation* tools; none reports code health.
- `RunTestsToolExecutor` exists but requires a full build-system invocation.
- No tool exposes `DaemonCodeAnalyzer` / highlighting results.

### Design

- New `GetDiagnosticsToolExecutor` under `service/agent/tool/psi/` (it is PSI/daemon-based),
  registered in `BuiltInToolProvider` alongside the other PSI tools (gated by the existing
  `psiToolsEnabled` flag, or a dedicated flag if we want it available without full PSI tools).
- Tool signature: `get_diagnostics(file, severity?)` where `severity` filters to
  `error` (default) or `warning`+`error`. Consider an optional variant that accepts no file
  and reports diagnostics for all files modified during the current agent run (the executors
  know which paths were written).
- Implementation options, in preference order:
  1. Run highlighting passes programmatically for the target `PsiFile` and collect
     `HighlightInfo`s with severity ≥ requested (`DaemonCodeAnalyzerEx.processHighlights` if
     the document has been analyzed, otherwise trigger analysis via
     `com.intellij.codeInsight.daemon.impl` machinery / `MainPassesRunner`).
  2. Fallback: run selected local inspections via `InspectionEngine` on the file's PSI.
- Output format (LLM-friendly, mirrors other PSI tools): one line per finding —
  `severity, path:line, message`, capped (e.g. 100 findings) with a truncation note.
- **Threading:** must run under a read action off the EDT; highlighting requires the file to
  be analyzable (indexes ready). Return a clear "indexing in progress, retry" message during
  dumb mode instead of blocking.
- Read-only → add `get_diagnostics` to `AgentApprovalProvider.READ_ONLY_TOOLS` so it can be
  auto-approved.
- Description text should instruct the model: "Call this after editing files to check the
  code compiles and passes inspections before running tests."

### Acceptance criteria

- Agent can call `get_diagnostics` on a Java file with a syntax error and receive the error
  with line number, without any Gradle/Maven invocation.
- Warnings are only included when requested via `severity`.
- Tool is auto-approved when "auto-approve read-only tools" is on.
- Dumb-mode returns a graceful retry message; no EDT blocking (no freezes).
- Unit tests for argument parsing/formatting; platform test (extends
  AbstractLightPlatformTestCase) asserting a known-bad file yields an ERROR diagnostic.
- Tool can be disabled via the existing disabled-agent-tools mechanism and is listed in
  `AgentSettingsComponent`.
<!-- SECTION:DESCRIPTION:END -->
