---
id: TASK-242
title: Add read-only git agent tools — git_status, git_diff, git_log
status: To Do
assignee: []
created_date: '2026-07-02 19:32'
labels:
  - agent-mode
  - tools
  - git
  - feature
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/RunCommandToolExecutor.java
  - src/main/java/com/devoxx/genie/service/git/GitMergeService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add dedicated **read-only git tools** to Agent Mode: `git_status`, `git_diff`, and `git_log`.
Today the agent has no visibility into version-control state — it cannot see what it (or the
user) has changed, so it cannot self-review its edits, scope verification to touched files,
or write accurate commit messages. The only workaround is `run_command("git diff")`, which
requires a per-call user approval and shares `run_command`'s 30-second timeout — friction
that makes models avoid it.

Because these operations are read-only, dedicated tools can join
`AgentApprovalProvider.READ_ONLY_TOOLS` and be **auto-approved**, removing that friction
entirely. The plugin already ships git plumbing (`GitMergeService` reads uncommitted changes
for prompt context), so this is mostly wiring, not new capability.

### Proposed tools

1. **`git_status`** — porcelain status: staged / unstaged / untracked files with paths
   relative to the project root. No parameters.
2. **`git_diff`** — unified diff. Parameters:
   - `path` (optional): limit to one file/directory.
   - `staged` (optional boolean): diff the index instead of the working tree.
   - `ref` (optional): diff against a ref (e.g. `HEAD~1`, a branch) instead of the index.
   - Output capped (e.g. 50K chars) with a truncation note listing remaining changed files.
3. **`git_log`** — recent history. Parameters:
   - `path` (optional): log for one file.
   - `max_count` (optional, default 10, hard cap 50): number of commits.
   - One-line-per-commit output: short hash, author date, subject.

### Design

- New executors under `service/agent/tool/git/` (`GitStatusToolExecutor`,
  `GitDiffToolExecutor`, `GitLogToolExecutor`), registered in `BuiltInToolProvider` behind a
  `gitToolsEnabled` state flag (default true — read-only and safe), individually
  disableable via the existing disabled-agent-tools list and shown in
  `AgentSettingsComponent`.
- Prefer the IntelliJ **git4idea** API (`GitRepositoryManager` / `Git` command runner) so the
  tools respect the IDE's configured git executable and work on all platforms; fall back to a
  plain `git` process (reusing `RunCommandToolExecutor`'s process/timeout machinery) when the
  git4idea plugin is unavailable. Follow the same optional-dependency pattern used for the
  Java-only PSI tools (see issue #1100 note in `BuiltInToolProvider`): never hard-link
  git4idea classes from always-loaded code.
- Graceful degradation: if the project has no git root, return a clear
  "not a git repository" message rather than an error/stacktrace.
- All three names go into `AgentApprovalProvider.READ_ONLY_TOOLS`.
- Tool descriptions should steer the model: "Use git_diff to review your own changes before
  declaring a task complete" — this enables a self-review step at the end of agent runs.

### Explicitly out of scope (follow-up candidate)

- Write operations (`git_commit`, `git_checkout`, branch creation) — useful for
  checkpoint/rollback of agent work, but approval-gated and with different safety
  considerations. Keep this task read-only so it can ship quickly.

### Acceptance criteria

- With agent mode on, the model can call `git_status`/`git_diff`/`git_log` without an
  approval prompt when auto-approve-read-only is enabled.
- `git_diff` with no arguments returns working-tree changes; `staged` and `ref` variants
  work; output is capped with an explicit truncation marker.
- Non-git projects get a friendly "not a git repository" message.
- Unit tests for argument parsing and output formatting (mocked runner); an IT against a
  temp git repo covering status/diff/log happy paths.
<!-- SECTION:DESCRIPTION:END -->
