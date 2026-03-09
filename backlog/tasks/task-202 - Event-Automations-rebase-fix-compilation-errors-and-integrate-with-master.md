---
id: TASK-202
title: 'Event Automations: rebase, fix compilation errors, and integrate with master'
status: In Progress
assignee: []
created_date: '2026-03-09 08:13'
updated_date: '2026-03-09 08:20'
labels:
  - feature
  - event-automations
  - rebase
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/PostStartupActivity.java
  - >-
    src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java
  - src/main/java/com/devoxx/genie/service/automation/listeners/
  - >-
    src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java:81
  - 'src/main/java/com/devoxx/genie/ui/DevoxxGenieToolWindowContent.java:190'
  - 'src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java:331'
  - 'src/main/java/com/devoxx/genie/model/enumarations/IdeEventType.java:12'
  - >-
    src/main/java/com/devoxx/genie/service/automation/EventAutomationSettings.java:66
  - >-
    src/main/java/com/devoxx/genie/service/automation/listeners/TestExecutionListener.java:37
  - >-
    src/main/java/com/devoxx/genie/service/automation/listeners/VcsCommitListener.java:43
  - 'src/main/java/com/devoxx/genie/model/enumarations/AgentType.java:12'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Branch `claude/research-cursor-automations-rhxHA` implements IDE Event Automations — a POC that couples JetBrains IDE events (file open, file save, build success/failure, process crash) with agent-triggered prompts.

After rebasing onto master, two compilation errors were found and fixed:

1. **`PostStartupActivity.java:85`** — `ExecutionListener.EXECUTION_TOPIC` does not exist; the topic constant lives on `ExecutionManager.EXECUTION_TOPIC`. Fixed import and reference.
2. **`EventAutomationService.java:88`** — `PromptSubmissionListener.onPromptSubmitted()` gained a third `tabId` parameter on master. Added `null` as the third argument.

### Key files in this branch
- `service/automation/EventAutomationService.java` — core automation engine
- `service/automation/listeners/` — IDE event listeners (BuildCompilation, FileEvent, FileSave, ProcessExit, TestExecution, VcsCommit)
- `service/PostStartupActivity.java` — registers event automation listeners on project open
- `ui/settings/` — Event Automations settings panel (POC)
- Docusaurus docs page for Event Automations feature
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Branch rebased onto master without conflicts
- [x] #2 All compilation errors from rebase resolved
- [ ] #3 Full test suite passes
- [ ] #4 Event automation listeners register correctly on project startup
- [ ] #5 [P1] Event automations work without requiring the tool window to be open — decouple prompt dispatch from DevoxxGenieToolWindowContent/ActionButtonsPanel UI wiring (EventAutomationService.java:81, DevoxxGenieToolWindowContent.java:190, ActionButtonsPanel.java:331)
- [ ] #6 [P2] Remove or disable unsupported event types — IdeEventType.java:12 and EventAutomationSettings.java:66 list FILE_CREATED, METHOD_ADDED, GRADLE_SYNC, PROJECT_OPENED but PostStartupActivity.java:68 and plugin.xml:543 never wire listeners for them
- [ ] #7 [P2] TestExecutionListener uses correct project — TestExecutionListener.java:37 picks the first open project instead of the project owning the test run, breaking multi-project sessions
- [ ] #8 [P2] Before-commit automation includes staged diff content, not just file names and change types — VcsCommitListener.java:43 and AgentType.java:12 default review prompt advertises line-specific review but only passes file-level metadata
<!-- AC:END -->
