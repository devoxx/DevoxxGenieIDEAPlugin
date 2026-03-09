---
id: TASK-199
title: Fix Copilot CLI runner unreadable error on test connection failure
status: Done
assignee: []
created_date: '2026-03-08 17:43'
labels:
  - bug
  - cli-runners
  - ui
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/971'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
**GitHub Issue:** #971 — [BUG] Copilot CLI runner fails due to authentication failure

**Problem:** When clicking "Test Connection" for a Copilot CLI runner in settings, failures produce an unreadable error message. The error was truncated to 80 characters and could still contain ANSI escape codes and control characters that rendered as garbage in the UI label.

**Root causes:**
1. Error output not fully sanitized — ANSI escapes stripped but control chars (bell, backspace, etc.) remained
2. Only the last line of stderr/stdout shown — multi-line errors lost context
3. Truncated to 80 chars in a non-wrapping JBLabel — important details cut off
4. No actionable guidance for common failures like authentication errors

**Fix implemented:**
- Extracted `CliTestErrorResolver` utility class with full sanitization (ANSI + control chars)
- Joins all meaningful output lines with `|` separator instead of only last line
- Detects auth errors (auth/token/login/credential/401/403) and permission errors, prefixes with actionable guidance
- Logs full error to IDE log for debugging
- Increased display limit to 200 chars with HTML wrapping (width:400px)
- Added tooltip with full error message when truncated
- Updated both CLI and ACP dialog `showTestResult` methods consistently

**Branch:** `fix/issue-971-copilot-cli-auth`
<!-- SECTION:DESCRIPTION:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Changes

**New files:**
- `CliTestErrorResolver.java` — Testable utility for resolving/formatting CLI test error messages with ANSI+control char stripping, auth/permission error detection, and full IDE logging
- `CliTestErrorResolverTest.java` — 10 unit tests covering sanitization, error detection, and fallback behavior

**Modified files:**
- `RunnerSettingsComponent.java` — Updated both CLI and ACP `showTestResult()` to use HTML rendering with word wrap (400px), increased display limit to 200 chars, added tooltip for full error on hover, delegated error resolution to `CliTestErrorResolver`

All tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
