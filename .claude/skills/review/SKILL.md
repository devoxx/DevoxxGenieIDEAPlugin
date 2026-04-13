---
name: review
description: Review local code changes for bugs, regressions, missing tests, and pragmatic improvements. Use when the user asks to review the current changes from `git status`, or to review a specific backlog task by id such as `task-65` or `TASK-65`. Also trigger when the user says things like "review", "review current changes", "check my changes", "look over the diff", or "review task-42".
---

# Review

## Overview

Review the current local changes or the implementation tied to a backlog task id and report concrete findings, not a changelog. Treat this as a code review unless the user explicitly asks for edits.

## Workflow

1. Resolve the review target.
If the prompt includes a backlog task id, locate the task markdown in `backlog/tasks/` first, then `backlog/completed/` if needed. Read the description, acceptance criteria, and referenced files. If no task id is given, start from `git status --short` and review the current local changes directly.

2. Build review scope from local context.
Check `git status --short`, inspect the diff for files related to the task, and read surrounding code where behavior is affected. Prefer `rg` and targeted `git diff -- <path>` over broad scans.

3. Review for defects and regressions.
Prioritize:
- broken behavior versus the task intent
- stale callers or UI paths left behind after refactors
- contract mismatches across main/preload/renderer/shared code
- silent failure handling and misleading fallback behavior
- missing or weak tests for the changed behavior
- unnecessary performance regressions or duplicate work

4. Verify when useful.
Run focused tests for the touched area if they are available and cheap. Mention clearly when verification is blocked or when broader typecheck/test failures are unrelated.

## Output

Report findings first, ordered by severity. For each finding:
- state the severity
- describe the bug/risk or improvement
- include clickable file references with line numbers when available
- explain the user-visible or maintenance impact briefly

After findings, include:
- open questions or assumptions if any
- a brief note on verification performed

If no findings are discovered, say that explicitly and mention residual risk or missing coverage.

## Prompt Shape

Interpret prompts like `review`, `review current changes`, or `review the changes from git status` as a request to review the current working tree. Interpret prompts like `task-56`, `review task-56`, or `review task-56 and report any issues/bugs or possible improvements` as a review of the local changes associated with that task.
