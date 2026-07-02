---
id: TASK-243
title: Add rename_symbol agent tool — semantic project-wide rename via IDE refactoring
status: To Do
assignee: []
created_date: '2026-07-02 19:32'
labels:
  - agent-mode
  - tools
  - psi
  - refactoring
  - feature
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/FindReferencesToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/EditFileToolExecutor.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add a `rename_symbol` agent tool that performs a **semantically correct, project-wide rename**
using the IDE's refactoring engine (`RenameProcessor`). This is the first PSI *write* tool —
all nine existing PSI tools are read-only navigation.

Renaming via `edit_file` is fundamentally unsafe: string replacement cannot distinguish a
method named `process` from an unrelated identifier, misses qualified usages, overloads,
Javadoc references, and property files, and corrupts code when the name is a substring of
another symbol. IntelliJ's `RenameProcessor` handles all of this — imports, inheritance
(renaming an interface method renames implementations), Javadoc, and (optionally) usages in
comments/strings. No CLI-based agent can match this; it is the flagship "being inside the
IDE" advantage on the write side.

### Proposed tool

`rename_symbol(file, line, symbol?, new_name, search_in_comments?)`

- `file` + `line` (+ optional `symbol` disambiguator) locate the declaration, using the same
  position-resolution conventions as `find_references`/`find_definition`
  (see `PsiToolUtils`).
- `new_name`: the new identifier. Validate with the language's naming rules
  (`PsiNameHelper`/`NamesValidator`) before running.
- `search_in_comments` (optional, default false): also rename occurrences in comments and
  string literals.
- Returns a summary: old name → new name, number of usages updated, list of affected files
  (capped), and any conflicts.

### Design

- New `RenameSymbolToolExecutor` under `service/agent/tool/psi/`, registered in
  `BuiltInToolProvider` behind the existing `psiToolsEnabled` flag **and** a dedicated
  `psiWriteToolsEnabled` flag (default false — opt-in, since this mutates many files).
- **Approval:** it mutates the workspace, so it must NOT be added to
  `AgentApprovalProvider.READ_ONLY_TOOLS` — every call goes through the standard approval
  dialog. The approval payload should show old name, new name, and usage count so the user
  can judge blast radius before accepting.
- **Conflict handling:** run `RenameProcessor` in non-interactive mode; detect conflicts
  (name collisions, shadowing) via the processor's conflict collection and, instead of
  showing modal UI, abort and return the conflict list to the model as the tool result.
  Never pop refactoring dialogs from an agent thread.
- **Threading:** resolve the target under a read action off the EDT; execute the rename as a
  write command action on the EDT (`WriteCommandAction`/`invokeAndWait`), keeping it a single
  undoable command so the user can Ctrl+Z the whole rename.
- Language support: works for any language whose plugin supports rename refactoring (Java,
  Kotlin, Python, JS/TS, …) — `RenameProcessor` is platform-level, so unlike
  `find_callees` this need not be Java-only. Guard with a clear "rename not supported for
  this element" message when the element is not a `PsiNamedElement`.
- Tool description should steer the model: "Prefer this over edit_file whenever renaming a
  class, method, field, or variable — it updates all references project-wide."

### Follow-up candidates (out of scope here)

- `move_symbol`, `inline_symbol`, `change_signature`, safe-delete — same pattern, higher
  complexity. Ship rename first and validate the approval/threading model.
- `format_code` / optimize-imports tool (trivial by comparison; could piggyback on the
  `psiWriteToolsEnabled` flag).

### Acceptance criteria

- Renaming a Java class via the tool updates the declaration, all usages, imports, and the
  file name; the change is undoable as a single action.
- Renaming an interface method renames its implementations.
- Invalid identifiers and conflicts abort with a descriptive message and change nothing.
- Every invocation shows the approval dialog (regardless of auto-approve-read-only).
- Platform tests (extends AbstractLightPlatformTestCase) covering class rename, method
  rename with implementations, and conflict abort.
<!-- SECTION:DESCRIPTION:END -->
