---
id: TASK-244
title: read_file — add line-range params and truncation cap to protect context window
status: To Do
assignee: []
created_date: '2026-07-02 19:40'
labels:
  - agent-mode
  - tools
  - enhancement
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadFileToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/DocumentSymbolsToolExecutor.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

`ReadFileToolExecutor` currently returns the **entire file** with no offset/limit parameters
and no size cap (`ReadFileToolExecutor.readFile()` does `file.contentsToByteArray()` →
`new String(...)` and returns it verbatim). A single 5,000-line file can blow the context
window and the token budget of the whole agent run — and on real codebases the agent reads
many files per run. Every other mature agent toolset (Claude Code's Read, Cursor, aider)
reads windowed with an explicit continuation hint.

### Proposed change

Extend the `read_file` tool spec in `BuiltInToolProvider` with optional parameters:

- `start_line` (1-based, default 1)
- `limit` (number of lines, default e.g. 1000)

And add a hard safety cap (e.g. ~50K chars) that applies even when the caller asks for more.

Output contract:

- When truncated (by `limit` or the cap), append a clear marker the model can act on, e.g.
  `[Truncated: showing lines 1-1000 of 4813. Call read_file with start_line=1001 to continue.]`
- Prefix each line with its line number (`cat -n` style). This is what makes the PSI tools'
  line-based parameters (`find_references(file, line)`, `rename_symbol`) usable — today the
  model has to count lines itself.
- Reading a range that starts past EOF returns a friendly message with the actual line count.

### Notes

- Update the tool description to steer the model: "For large files, use document_symbols
  first to find the relevant region, then read that range."
- Keep the read under `ReadAccess.compute()` as today; binary files (probe for NUL bytes /
  use `FileTypeManager` binary check) should return "binary file, N bytes" instead of
  garbage.
- Backward compatible: with no params the behavior only changes for files exceeding the
  default limit/cap.

### Acceptance criteria

- `read_file` on a huge file returns at most the default window plus a truncation marker
  containing the total line count and the next `start_line`.
- `start_line`/`limit` window correctly (off-by-one covered by tests).
- Line numbers are present and match editor line numbers (1-based).
- Existing tests updated; new unit tests for windowing, cap, EOF, and binary files.
<!-- SECTION:DESCRIPTION:END -->
