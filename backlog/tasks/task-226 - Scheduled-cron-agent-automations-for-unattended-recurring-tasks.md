---
id: TASK-226
title: Scheduled / cron agent automations for unattended recurring tasks
status: To Do
assignee: []
created_date: '2026-06-05 10:17'
updated_date: '2026-06-05 10:17'
labels:
  - agent-mode
  - automation
  - scheduling
  - feature
  - hermes
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - 'https://github.com/NousResearch/hermes-agent'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add **time-based (scheduled / cron) automations** so DevoxxGenie can run agent prompts unattended on a recurring schedule. Hermes Agent provides "scheduled tasks using natural language or cron expressions ... with result delivery ... and pause/resume/edit". DevoxxGenie currently only triggers automations on IDE events, not on a clock.

### Current state (from codebase exploration)

- `EventAutomationService` maps IDE events (file save, VCS commit, etc.) to prompt templates with variable interpolation, and can auto-submit a prompt or show a confirmation balloon.
- There is **no time-based trigger** — nothing runs "every morning", "hourly", or on a cron expression. The infrastructure for *defining* an automation (prompt template + variable interpolation + submit/confirm flow) already exists; only the **scheduler** is missing.

This task adds a scheduling layer on top of the existing automation infrastructure rather than building a parallel system.

### Design principles

- **Reuse `EventAutomationService`'s execution path.** A scheduled automation is the same "template → interpolate → submit/confirm" flow, triggered by a timer instead of an IDE event. Refactor the trigger/registration so both event-based and time-based automations share the submit path.
- **IDE-appropriate scheduling.** Use IntelliJ's `AppExecutorUtil.getAppScheduledExecutorService()` (or `Alarm`) for scheduling — NOT raw threads. Schedules only fire while the IDE is open (document this clearly; we are not a daemon like Hermes).
- **Off the EDT, with safe submission.** Timer callbacks run off the EDT; prompt submission must hop onto the correct thread via `ApplicationManager.getApplication().invokeLater()` as the existing automation flow does.
- **Visible and controllable.** Scheduled automations must be listable with pause / resume / edit / delete and last-run / next-run status, so unattended runs are never opaque.

## Implementation Plan

### 1. Schedule model & persistence

- Define a `ScheduledAutomation` model: id, name, schedule expression (cron and/or a small natural-language vocabulary like "every day at 09:00", "hourly"), the prompt template, enabled flag, and last/next-run timestamps.
- Persist the list via `DevoxxGenieStateService` (consistent with how other settings persist). Per-project or application-level — match how `EventAutomationService` automations are currently scoped.

### 2. Scheduler service

- New `ScheduledAutomationService` that, on project open, registers each enabled automation with `AppExecutorUtil.getAppScheduledExecutorService()` (compute next-fire from the cron/NL expression). On fire, it interpolates the template and routes into the **shared** automation submit path extracted from `EventAutomationService`.
- Parse cron via a small dependency-free parser or a vetted cron library already on the classpath; support a minimal natural-language subset mapped onto cron. Validate expressions at save time with a clear error.
- Update last-run/next-run on each fire; reschedule the next occurrence. Cleanly dispose all schedules on project close (no leaked timers).

### 3. Optional: agent-callable scheduling tool

- Optionally expose a `schedule_task(name, schedule, prompt)` built-in tool (gated by a flag) so the agent itself can create a scheduled automation — matching Hermes' model where the agent schedules its own follow-ups. Creating a schedule is a side-effecting action → approval-gated via `AgentApprovalProvider`. This can be a follow-up if it widens scope too far.

### 4. Settings / management UI

- Add a "Scheduled Automations" management UI (in `AgentSettingsComponent` or a dedicated panel): list automations with next-run/last-run, and add / edit / pause / resume / delete actions. Editing reuses the prompt-template editor pattern from the event-automation UI.

### 5. Tests

- Unit test schedule-expression parsing and next-fire computation (cron + the NL subset; invalid expressions return a clear validation error).
- Test that firing routes through the shared automation submit path with correct variable interpolation (submission mocked).
- Test pause/resume/edit/delete update both persistence and the live scheduler registration.
- Test that disposing the service cancels all scheduled futures (no leaked timers).

## Out of scope (capture as follow-ups)

- Running schedules while the IDE is closed (true daemon behaviour) — out of scope for an IDE plugin; document the "only while IDE is open" limitation.
- Delivering results to external platforms (Telegram/Slack/email) as Hermes does — DevoxxGenie surfaces results in the tool window; external delivery is a separate integration.
- Distributed/remote execution backends.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `ScheduledAutomation` model (id, name, schedule expression, prompt template, enabled, last/next run) is defined and persisted via `DevoxxGenieStateService`
- [ ] #2 Cron expressions and a minimal natural-language subset (e.g. "every day at 09:00", "hourly") are supported and validated at save time with a clear error on invalid input
- [ ] #3 A `ScheduledAutomationService` schedules enabled automations using IntelliJ's application scheduled executor (not raw threads) and computes/refreshes the next-fire time after each run
- [ ] #4 On fire, the automation interpolates its template and submits via a submit path shared with `EventAutomationService` (event-based and time-based automations reuse the same execution flow)
- [ ] #5 Timer callbacks run off the EDT and submission hops to the correct thread via `invokeLater`, consistent with the existing automation flow
- [ ] #6 A management UI lists scheduled automations with next-run/last-run and supports add / edit / pause / resume / delete
- [ ] #7 Disposing the service (project close) cancels all scheduled futures with no leaked timers
- [ ] #8 The "only runs while the IDE is open" limitation is clearly documented in the UI/help text
- [ ] #9 Unit tests cover expression parsing & next-fire computation, validation of invalid expressions, the shared submit path with interpolation (submission mocked), pause/resume/edit/delete, and disposal cancelling all futures
- [ ] #10 Plugin builds (`./gradlew buildPlugin`) and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Originated from research comparing DevoxxGenie's agent mode to Nous Research's Hermes Agent (https://github.com/NousResearch/hermes-agent), which offers scheduled tasks via natural-language or cron expressions with pause/resume/edit. DevoxxGenie's `EventAutomationService` already provides the "template → interpolate → submit/confirm" flow for IDE-event triggers; this task adds the missing time-based scheduler on top of that same flow rather than a parallel system. Key constraint vs Hermes: an IDE plugin is not a daemon, so schedules only fire while the IDE is open — document this rather than trying to emulate always-on behaviour. Use `AppExecutorUtil.getAppScheduledExecutorService()` / `Alarm`, keep timer work off the EDT, and dispose all futures on project close. Per CLAUDE.md: feature branch before code changes; behavioural tests first where practical.
<!-- SECTION:NOTES:END -->
