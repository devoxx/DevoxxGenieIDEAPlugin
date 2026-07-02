---
id: TASK-245
title: Add format_code agent tool — IDE code-style reformat and optimize imports
status: To Do
assignee: []
created_date: '2026-07-02 19:40'
labels:
  - agent-mode
  - tools
  - psi
  - feature
dependencies:
  - TASK-243
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/WriteFileToolExecutor.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add a `format_code(file, optimize_imports?)` tool that reformats a file with the project's
configured IDE code style (`CodeStyleManager.reformat`) and optionally runs
`OptimizeImportsProcessor`. Agent-written code that ignores the project's code style produces
noisy diffs that reviewers reject and follow-up `edit_file` calls that fail to match
(indentation drift breaks exact-string matching). The IDE already owns the authoritative
code-style settings — this tool is thin glue.

### Design

- New `FormatCodeToolExecutor` under `service/agent/tool/psi/`, registered in
  `BuiltInToolProvider` behind the `psiWriteToolsEnabled` flag introduced by TASK-243 (it
  mutates the workspace, so it shares the write-tools gate and is NOT in
  `READ_ONLY_TOOLS` — standard approval applies).
- Parameters: `file` (required), `optimize_imports` (optional boolean, default true),
  optional `start_line`/`end_line` to reformat only a region (keeps diffs minimal when the
  agent only touched part of a file).
- Execute as a single `WriteCommandAction` on the EDT (undoable in one step), PSI resolution
  under a read action off the EDT — same threading pattern as TASK-243.
- Return a short summary: whether the file changed, and lines affected.
- Consider (behind the same flag) auto-suggesting in `write_file`/`edit_file` tool results:
  "File written. Consider format_code to match project code style." — cheap prompt-side nudge
  with no behavior coupling.

### Acceptance criteria

- Formatting a misindented Java file matches what Code → Reformat Code produces; unused
  imports are removed when `optimize_imports` is true.
- Region formatting only touches the requested range.
- Files of unsupported types return a clear message instead of an exception.
- Undo reverts the whole format as one step.
- Platform test comparing tool output against `CodeStyleManager` reference behavior.
<!-- SECTION:DESCRIPTION:END -->
