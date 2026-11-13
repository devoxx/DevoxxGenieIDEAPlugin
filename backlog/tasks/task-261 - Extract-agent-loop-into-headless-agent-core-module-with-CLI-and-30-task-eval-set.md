---
id: TASK-261
title: Extract agent loop into headless agent-core module with CLI and 30-task eval set
status: To Do
assignee: []
created_date: '2026-09-29 12:00'
labels:
  - agent
  - architecture
  - evaluation
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/agent/ToolErrorRecovery.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - settings.gradle.kts
  - build.gradle.kts
  - 'https://github.com/google-research/rrsi'
  - 'https://arxiv.org/abs/2609.24972'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
We cannot measure the agent harness (system prompt, tool descriptions, loop control, error recovery, context handling) today, because the agent loop only runs inside IntelliJ and there is no eval set. Harness changes ship on intuition, and regressions across models or providers go unnoticed.

Goal: move the agent loop into a plain-Java `agent-core` Gradle subproject that the plugin and a new `agent-cli` both use, and add an eval set of about 30 realistic tasks with hidden pass/fail checks. The CLI must run the **same** harness code that ships in the plugin (extracted, not copied), so eval scores reflect what users get.

This is the prerequisite for eventually applying harness search in the style of RRSI (Regularized Recursive Self-Improvement, google-research/rrsi, arXiv 2609.24972). RRSI evolves an agent harness against a scored task suite, accepts a change only when its gain exceeds measurement noise and pays for its extra tokens, and screens out changes that overfit the task suite. Its `domains/coding/PATTERNS.md` is a useful checklist of harness mechanisms to evaluate manually once this infrastructure exists.

### Current coupling (assessed 2026-09-29)
- The loop is langchain4j `AiServices` plus a `ToolProvider` wrapped by `AgentLoopTracker` (see `NonStreamingPromptExecutionService` around lines 215-237). That part is portable.
- Files with no or minimal IntelliJ use: `ToolErrorRecovery`, `CommandBlacklist`, `BuiltInToolDescriptions`, `ToolArgumentParser`, `TestResultParser`, `BuildSystemDetector`, `CompositeToolProvider`, `FetchPageToolExecutor`, `WebSearchToolExecutor`.
- IntelliJ dependencies are concentrated in: `Project` (22 files, mostly used to find the project root), VFS plus `WriteCommandAction` in the read/write/edit/list/search executors, approval dialogs (`AgentApprovalService`), diff preview (`AgentDiffPreviewFactory`), `AgentFileChangeTracker`, and `DevoxxGenieStateService` for the prompt, limits and enabled tools.

### Proposed layout
```
agent-core/      plain Java 21 + langchain4j, no IntelliJ dependency
  AgentRunner, AgentLoopTracker, ToolErrorRecovery, tool descriptions/specs
  interfaces: Workspace (read/write/list/search), CommandRunner,
              ApprovalPolicy, AgentConfig (system prompt, limits, enabled tools)
  file-based tool executors written against those interfaces
agent-cli/       picocli entry point, java.nio Workspace, auto-approve policy
  genie-agent run  --task evals/tasks/017 --provider ... --model ...
  genie-agent eval --suite evals/tasks --trials 2 --out results.jsonl
evals/tasks/NNN/ repo snapshot + prompt.md + hidden check script + metadata
root plugin      implements the interfaces with VFS / WriteCommandAction / dialogs
```

### Out of scope (initially)
PSI tools (`service/agent/tool/psi`), MCP tools, RAG `semantic_search`, and backlog tools stay plugin-only. The CLI toolset is therefore smaller, and eval reports must state which toolset was used. Running RRSI itself is also out of scope; that becomes a follow-up task once a noise baseline exists.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new `agent-core` Gradle subproject (a plain `java-library` with no `com.intellij` imports) contains the agent loop, loop tracking, tool error recovery, tool descriptions/specifications and the file/command tool executors, written against Workspace, CommandRunner, ApprovalPolicy and AgentConfig interfaces
- [ ] #2 The plugin consumes `agent-core` (bundled into the plugin distribution) through IntelliJ-backed implementations of those interfaces, with no duplicated harness logic; `./gradlew test` and `./gradlew verifyPlugin` pass
- [ ] #3 Plugin agent behaviour is unchanged: edits still go through WriteCommandAction/VFS (undo, open editors), approvals and diff previews still work, and the changed-files tracking still fires
- [ ] #4 A new `agent-cli` subproject can run one task end to end against a local directory using any provider configured via CLI flags or environment variables (at least Anthropic, OpenAI and Ollama)
- [ ] #5 An `evals/tasks` suite of about 30 realistic tasks exists, each with a repo snapshot, task prompt, a hidden check script the agent cannot see, and metadata (language, category, expected difficulty); the suite covers Java, Kotlin, TypeScript and Python and a mix of bug fixes, small features, refactors and make-this-test-pass tasks
- [ ] #6 Each trial runs in an isolated sandbox (Docker or at least a throwaway copy of the repo with the command blacklist enforced) so the agent cannot modify the host or read the check scripts
- [ ] #7 `genie-agent eval` writes one JSONL record per trial (task id, model, pass/fail, tokens in/out, tool calls, wall time, termination reason) and prints a summary with pass rate and mean cost
- [ ] #8 A baseline is recorded for 2-3 models with at least 2 trials per task, including the run-to-run spread, and documented under `docs/` or `evals/README.md` so later harness changes can be judged against the noise level
- [ ] #9 Unit tests cover the new interfaces' plugin and CLI implementations and the eval result aggregation
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Spike the multi-module Gradle setup first: add an empty `agent-core` java-library and confirm the IntelliJ Platform Gradle plugin bundles it and `verifyPlugin` passes. Stop and reassess if this fights the build.
2. Define the Workspace, CommandRunner, ApprovalPolicy and AgentConfig interfaces. Move the IntelliJ-free classes into `agent-core` unchanged.
3. Port the file and command tool executors to the interfaces one at a time. Provide plugin implementations (VFS/WriteCommandAction/approval dialog) and keep plugin tests green after each move.
4. Extract the loop assembly (AiServices builder plus tool provider plus AgentLoopTracker) from the prompt execution services into an `AgentRunner` in core, and have both the streaming and non-streaming services use it.
5. Add `agent-cli` (picocli): a java.nio Workspace, a ProcessBuilder CommandRunner, an auto-approve policy with the blacklist enforced, and provider/model flags.
6. Build 3 pilot tasks plus the runner and JSONL output end to end, then grow to about 30 tasks. Check that each check script fails on the untouched snapshot and passes on a reference solution.
7. Add sandboxing (Docker image per language family, or a temp-copy fallback).
8. Run the baseline (2-3 models × ≥2 trials), record the results and spread, and write `evals/README.md`.
9. Create a follow-up task: triage the harness against the RRSI pattern checklist, and optionally prototype an RRSI domain adapter that calls `genie-agent eval`.
<!-- SECTION:PLAN:END -->
