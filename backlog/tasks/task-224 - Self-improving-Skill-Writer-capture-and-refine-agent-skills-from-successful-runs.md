---
id: TASK-224
title: Self-improving Skill Writer — capture and refine agent skills from successful runs
status: To Do
assignee: []
created_date: '2026-06-05 10:17'
updated_date: '2026-06-05 10:17'
labels:
  - agent-mode
  - skills
  - feature
  - hermes
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/skill/SkillRegistry.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - 'https://github.com/NousResearch/hermes-agent'
  - 'https://agentskills.io/'
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

This task introduces a **Skill Writer**: a "learning loop" that lets the agent capture a successful multi-step run as a reusable **skill document**, and later refine that skill when it is used again. This is the single defining capability of Nous Research's Hermes Agent ("the only agent with a built-in learning loop — it creates skills from experience, improves them during use"), and it is the main agentic gap in DevoxxGenie today.

### Current state (from codebase exploration)

DevoxxGenie already **reads** skills but cannot **write** them:

- `SkillRegistry` loads **static** skill markdown files (LangChain4J experimental `@Skill` + `FileSystemSkillLoader`) from a priority-ordered set of directories: `~/.agents/skills/`, `~/.claude/skills/`, `~/.devoxxgenie/skills/`, and project-local `.agents/.claude/.devoxxgenie/skills/`. It exposes `activate_skill` (and optional `read_skill_resource`) to the agent.
- `AgentToolProviderFactory` wires `SkillRegistry.buildSkills().toolProvider()` into the agent tool chain.
- There is **no mechanism** that captures a successful trajectory and persists it as a new skill, and no mechanism to refine an existing skill after use.

This task adds the write/refine half of the loop, reusing the existing on-disk skill convention so captured skills are immediately discoverable by `SkillRegistry` on the next agent run.

### Design principles

- **User-curated, not silent.** Hermes nudges itself to persist knowledge; for an IDE we keep the human in the loop. Capture should be **proposed** (a confirmation dialog showing the draft skill), consistent with DevoxxGenie's existing approval-centric agent UX (`AgentApprovalProvider`). This avoids polluting the skill library with low-quality entries.
- **Reuse the existing storage convention.** Write captured skills as markdown to the **project-local** `<project>/.devoxxgenie/skills/<skill-name>/SKILL.md` (highest-precedence project location that `SkillRegistry` already scans), so no new loader or registry is needed. A global-vs-project destination choice can be offered in the dialog.
- **Progressive disclosure.** Follow the agentskills.io shape that `SkillRegistry` already consumes (name + description front-matter + body), so captured skills are compatible with the same `activate_skill` path and stay token-cheap until activated.
- **Off the EDT.** Skill drafting calls the selected LLM provider; it must run off the Event Dispatch Thread (see CLAUDE.md EDT constraints), like other prompt execution.

## Implementation Plan

### 1. `capture_skill` agent tool (new)

- New `CaptureSkillToolExecutor` under `service/agent/tool/`, registered in `BuiltInToolProvider` behind a `skillCaptureEnabled` flag (default false).
- Tool signature: `capture_skill(name, description, steps)` — the agent supplies a short name, a one-line description, and the ordered procedure (the distilled "how I solved it"). The executor renders these into the agentskills.io `SKILL.md` format and writes the file.
- Because it writes to disk, it must pass through `AgentApprovalProvider` as a **write** tool (NOT added to `READ_ONLY_TOOLS`), surfacing the draft skill in the approval dialog for the user to accept/edit/reject.
- After a successful write, trigger an async `SkillRegistry` reload so the new skill is available without an IDE restart (the registry already supports async reload).

### 2. End-of-run capture heuristic (the "nudge")

- After an agent run completes in `StreamingPromptStrategy` (and the non-streaming path), evaluate a lightweight heuristic: the run **succeeded** (no error / user did not cancel) AND it used a non-trivial number of tool calls (read `AgentLoopTracker` for the executed-call count; threshold configurable, e.g. >= 4).
- When the heuristic fires, post a non-blocking suggestion (balloon/notification, mirroring `EventAutomationService`'s confirmation balloon) offering "Save this as a reusable skill?". Accepting asks the model to draft the skill (off the EDT) and opens the same approval/edit dialog as the `capture_skill` tool.
- The heuristic must be cheap and must NOT block the response; it only proposes.

### 3. Skill refinement (`refine_skill`)

- When an existing skill is activated during a run (observable via the `activate_skill` path), and that run subsequently succeeds or fails, allow the agent to propose an edit to the activated skill via a `refine_skill(name, updated_steps, reason)` tool.
- Refinement is also a **write** operation → approval dialog + on-disk update. Keep a simple version trail (e.g. append-only `CHANGELOG` section in `SKILL.md`, or a `.bak` of the prior version) so refinements are reversible. No external versioning system.

### 4. Settings & gating

- Add `skillCaptureEnabled: Boolean` (default false) to `DevoxxGenieStateService`.
- Add a "Skill Capture" section to `AgentSettingsComponent`: enable checkbox, capture-threshold (min tool calls) field, and a destination default (project vs `~/.devoxxgenie/skills`). Help text explaining the learning loop.

### 5. Tests

- Unit test `CaptureSkillToolExecutor`: renders valid agentskills.io `SKILL.md`, writes to the correct project-local directory, sanitizes the skill name into a safe folder name, and returns an error string (not an exception) on IO failure.
- Test the end-of-run heuristic in isolation (given a mock `AgentLoopTracker` call count + success flag, it proposes vs stays silent at the threshold boundary).
- Test that a captured skill is subsequently discoverable by `SkillRegistry` (round-trip), and that refinement updates the file and preserves a recoverable prior version.
- Verify graceful degradation when the feature flag is off (no tool registered, no end-of-run prompt).

## Out of scope (capture as follow-ups)

- Fully autonomous (no-confirmation) capture and periodic self-reinforcement nudges à la Hermes — keep human-in-the-loop for v1.
- Agent-authored *executable* tools (Hermes' Python `skill_manage`). DevoxxGenie skills are knowledge documents, not new code-level tools; generating runnable tools is a separate, larger effort.
- Sharing/syncing skills to a remote registry (agentskills.io publishing).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `capture_skill(name, description, steps)` built-in agent tool exists, gated behind a `skillCaptureEnabled` setting (default false), and writes an agentskills.io-compatible `SKILL.md` into a skills directory already scanned by `SkillRegistry`
- [ ] #2 `capture_skill` is treated as a write tool: it routes through `AgentApprovalProvider` and presents the drafted skill to the user for accept/edit/reject before anything is written to disk
- [ ] #3 After a captured skill is written, `SkillRegistry` reloads so the skill is usable via `activate_skill` without an IDE restart
- [ ] #4 When an agent run completes successfully and used at least the configured number of tool calls (read from `AgentLoopTracker`), the user is offered a non-blocking suggestion to save the run as a skill; the suggestion never blocks or delays the response
- [ ] #5 Skill drafting (LLM summarisation of the trajectory) runs off the EDT using the currently selected LLM provider
- [ ] #6 A `refine_skill(name, updated_steps, reason)` tool lets the agent propose an edit to a previously activated skill; refinement is approval-gated and preserves a recoverable previous version of the skill
- [ ] #7 An Agent Mode "Skill Capture" settings section exposes the enable toggle, the capture threshold, and the default destination (project vs user home), persisted via `DevoxxGenieStateService`
- [ ] #8 Skill name input is sanitised into a safe directory name and IO failures are returned as error strings (not thrown) so the agent loop recovers gracefully
- [ ] #9 Unit tests cover skill rendering/writing, the end-of-run threshold boundary, the SkillRegistry round-trip (capture → discover → activate), and refinement version preservation
- [ ] #10 With the feature flag off, no capture tool is registered and no end-of-run prompt appears; plugin builds (`./gradlew buildPlugin`) and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Originated from research comparing DevoxxGenie's agent mode to Nous Research's Hermes Agent (https://github.com/NousResearch/hermes-agent), whose signature feature is a closed learning loop: it writes a reusable skill document after solving a hard problem and self-improves skills during use. DevoxxGenie already has the *read* side (`SkillRegistry` + `activate_skill`, agentskills.io-shaped files in `~/.agents`, `~/.claude`, `~/.devoxxgenie` and project-local equivalents) but no *write/refine* side. This task adds capture + refine while keeping a human-in-the-loop confirmation step (consistent with the existing `AgentApprovalProvider` UX) instead of Hermes' fully autonomous persistence.

Key reuse points: `SkillRegistry` (loader + async reload), `AgentToolProviderFactory` (tool-chain assembly), `BuiltInToolProvider` (flag-gated tool registration pattern — see the `web_search`/`semantic_search` blocks), `AgentLoopTracker` (executed tool-call count for the heuristic), `AgentApprovalProvider` (write-tool approval), `EventAutomationService` (confirmation-balloon pattern for the non-blocking suggestion). Per CLAUDE.md: create a feature branch before code changes, keep summary/drafting off the EDT, and write behavioural tests first where practical.
<!-- SECTION:NOTES:END -->
