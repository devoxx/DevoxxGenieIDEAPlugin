---
id: TASK-203.1
title: Design and implement a robust non-retry fix path for Skiko Windows
status: Done
priority: medium
parent_task_id: TASK-203
assignee: []
created_date: '2026-03-09 11:59'
updated_date: '2026-03-09 13:28'
labels:
  - bug-fix
  - windows
  - compose
  - skiko
dependencies:
  - TASK-203
references:
  - "https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/981"
  - src/main/kotlin/com/devoxx/genie/ui/compose/SafeComposeContainer.kt
  - >-
  - src/main/java/com/devoxx/genie/service/PostStartupActivity.java
  - >-
documentation:
  - "backlog://workflow/task-execution"
  - >-
ordinal: 1000
---

<!-- SECTION:DESCRIPTION:BEGIN -->
Follow up on issue #981 with an engineering fix that does not rely on plugin-local runtime retry after Compose initialization has already started. Task context: the corrected investigation on TASK-203 shows that the supported workaround is startup/early-init renderer selection (`-Dskiko.renderApi=SOFTWARE` or the persisted setting), while the current runtime retry narrative is not robust for this plugin. This follow-up should determine and implement the strongest viable long-term fix path, such as a local/upstream Skiko patch, a safer initialization boundary for recovery, or an explicit renderer-path architecture change in newer Compose/Skiko.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

- [ ] #1 A concrete long-term fix path for issue #981 is selected and documented, with rationale based on the corrected TASK-203 investigation
- [ ] #2 The implemented solution no longer treats plugin-local runtime retry as the supported recovery mechanism for Windows Direct3D initialization failures
- [ ] #3 Windows rendering failure handling remains user-recoverable through startup/early-init software rendering selection and fallback/help guidance if initialization still fails
- [ ] #4 Relevant tests, diagnostics, or validation steps are added or documented so the chosen fix path can be verified without relying on the old retry narrative
- [ ] #5 User-facing and task-facing documentation are consistent with the chosen fix path and do not describe plugin-local runtime retry as the definitive solution

