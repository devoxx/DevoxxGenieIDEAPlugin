---
id: TASK-219
title: Make RAG mutually exclusive with MCP and Agent mode in settings UI
status: To Do
assignee: []
created_date: '2026-05-26 20:16'
labels:
  - rag
  - mcp
  - agent
  - ui
  - bug
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/PromptExecutionService.java
  - src/main/java/com/devoxx/genie/controller/PromptExecutionController.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

When **RAG** is enabled simultaneously with **MCP** and/or **Agent mode**, the three pipelines conflict with each other. RAG injects retrieved context into the prompt, while MCP/Agent dynamically inject tools and reshape the request — leading to inconsistent behavior, wasted LLM calls, and unpredictable output.

These features were not designed to be combined and the current UI permits all three to be enabled at the same time.

## Expected Behavior

RAG, MCP, and Agent mode should be mutually exclusive at the settings/UI layer:

- When **RAG** is enabled → MCP and Agent mode controls should be disabled (greyed out) with a tooltip explaining the conflict.
- When **MCP** or **Agent mode** is enabled → the RAG enable checkbox should be disabled with a similar tooltip.
- Switching one on should automatically and visibly switch the conflicting ones off (or block toggling with an explanatory message — decision needed).

## Suggested Implementation

1. Centralize the mutual-exclusion rule in a small helper (e.g. `FeatureExclusivityCoordinator`) rather than duplicating wiring in each settings component.
2. Update:
   - `RAGSettingsComponent` / `RAGSettingsHandler`
   - `MCPSettingsComponent`
   - Agent mode settings (wherever the toggle lives in `DevoxxGenieStateService`)
3. On settings load: if multiple are already true in persisted state (legacy users), keep the highest-priority one enabled (suggested priority: Agent > MCP > RAG, but confirm) and disable the others with a one-time notification.
4. At prompt-submission time, add a defensive guard so that even if state somehow has multiple enabled, only one pipeline runs.

## Open Questions

- Should switching one feature on **auto-disable** the others, or **block** the toggle until the user disables the others manually? (Auto-disable is friendlier; blocking is more explicit.)
- Confirm the priority order for the legacy-state migration case.

## Files (Likely Touched)

- `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java`
- `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java`
- `src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java`
- Agent mode settings component (location to confirm)
- `DevoxxGenieStateService` (for the persisted toggles)
- `PromptExecutionController` / `PromptExecutionService` (defensive runtime guard)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Enabling RAG disables and visually greys out the MCP and Agent mode toggles, with a tooltip explaining the conflict
- [ ] #2 Enabling MCP or Agent mode disables and visually greys out the RAG enable checkbox, with an equivalent tooltip
- [ ] #3 Persisted settings that already have multiple of {RAG, MCP, Agent} enabled are reconciled on load to a single active feature using a documented priority order
- [ ] #4 A defensive guard in the prompt execution pipeline ensures only one of {RAG, MCP, Agent} actually runs even if state is inconsistent
- [ ] #5 Toggling behavior (auto-disable vs block-with-message) is decided, documented in the task, and applied consistently across all three settings panels
<!-- AC:END -->
