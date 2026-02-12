---
id: TASK-2
title: Refactor executeStrategySpecific into focused private methods
status: Done
assignee: []
created_date: '2026-02-12 18:36'
updated_date: '2026-02-12 18:38'
labels:
  - refactor
  - code-quality
  - cli-runner
dependencies: []
priority: medium
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Refactor the large `executeStrategySpecific` method in `CliPromptStrategy` into focused, well-named private methods. The current method is ~170 lines with deeply nested logic handling CLI tool lookup, process building, stderr/stdout reading, WebView streaming, and result finalization all in one place.
<!-- SECTION:DESCRIPTION:END -->

Break down the executeStrategySpecific method (~170 lines, complexity ~50) into 6 focused private methods:

1. **buildAndConfigureProcess** — ProcessBuilder setup, env vars, start
2. **startStderrReader** — daemon thread for stderr
3. **streamStdoutToUI** — stdout loop with filtering and WebView/console streaming
4. **handleProcessCompletion** — wait for exit, join stderr, dispatch to finalize
5. **finalizeSuccess** — EDT success path (set AI message, update WebView, chat memory)
6. **finalizeFailure** — EDT failure path

The main method becomes a thin orchestrator with no deep nesting. Pure structural refactor, no behavior change.

**File:** `CliPromptStrategy.java`
**Location:** `src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java`

**Acceptance Criteria:**
- [ ] Extract buildAndConfigureProcess method
- [ ] Extract startStderrReader method
- [ ] Extract streamStdoutToUI method
- [ ] Extract handleProcessCompletion method
- [ ] Extract finalizeSuccess method
- [ ] Extract finalizeFailure method
- [ ] Main method becomes thin orchestrator
- [ ] No behavior changes
- [ ] All tests pass

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 executeStrategySpecific is refactored into focused private methods (each under ~30 lines)
- [x] #2 Private methods have clear single-responsibility names (e.g. buildProcess, readStdout, readStderr, finalizeSuccess, finalizeError)
- [x] #3 No behavioral changes — same process execution, streaming, console output, and error handling
- [x] #4 Code compiles successfully with `./gradlew compileJava`
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Changes Made

**File modified:** `src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java`

The monolithic `executeStrategySpecific` method (~170 lines, deeply nested) was broken into 9 focused private methods:

1. **`executeStrategySpecific`** (~30 lines) — validates CLI tool, builds command, dispatches to thread pool
2. **`runCliProcess`** (~30 lines) — orchestrates the process lifecycle: start, read streams, wait, finalize
3. **`startProcess`** (~15 lines) — configures and starts the OS process (working dir, env vars)
4. **`startStderrReader`** (~17 lines) — spawns daemon thread reading stderr → console
5. **`streamStdoutToViews`** (~28 lines) — reads stdout, filters via CliCommand, streams to console + WebView
6. **`finalizeProcessResult`** (~10 lines) — dispatches to EDT and delegates to success/error handler
7. **`finalizeSuccess`** (~16 lines) — sets AI message, updates WebView, adds to chat memory, completes task
8. **`finalizeError`** (~7 lines) — prints error, builds error message from stderr, fails task
9. **`handleProcessStartFailure`** / **`handleProcessInterrupted`** (~7 lines each) — error handlers for IOException and InterruptedException

Also removed unused import `java.io.OutputStreamWriter` that was in the original file.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Refactored `CliPromptStrategy.executeStrategySpecific` into focused private methods\n\n### Problem\nThe `executeStrategySpecific` method was ~170 lines with 3 levels of nesting (lambda → try/catch → while loop → another lambda). It handled CLI tool resolution, process building, environment configuration, stderr reading, stdout streaming with filtering, WebView updates, success finalization, error finalization, and two different exception handlers — all inline.\n\n### Solution\nExtracted 9 focused private methods, each with a single responsibility and under ~30 lines:\n\n| Method | Responsibility |\n|--------|---------------|\n| `executeStrategySpecific` | Validates CLI tool, builds command, dispatches to pool |\n| `runCliProcess` | Orchestrates process lifecycle (start → read → wait → finalize) |\n| `startProcess` | Configures ProcessBuilder (working dir, env vars) and starts it |\n| `startStderrReader` | Spawns daemon thread that reads stderr → console |\n| `streamStdoutToViews` | Reads stdout, filters via CliCommand, streams to console + WebView |\n| `finalizeProcessResult` | EDT dispatcher — routes to success or error handler |\n| `finalizeSuccess` | Sets AI message, updates WebView, adds to chat memory |\n| `finalizeError` | Prints error, constructs error from stderr lines |\n| `handleProcessStartFailure` | IOException handler for process start |\n| `handleProcessInterrupted` | InterruptedException handler |\n\n### Behavioral Changes\nNone — this is a pure structural refactoring. All process execution, streaming, console output, error handling, and cancellation behavior is preserved exactly.\n\n### Files Modified\n- `src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java`\n\n### Cleanup\n- Removed unused import `java.io.OutputStreamWriter`"}
</invoke>
<!-- SECTION:FINAL_SUMMARY:END -->
