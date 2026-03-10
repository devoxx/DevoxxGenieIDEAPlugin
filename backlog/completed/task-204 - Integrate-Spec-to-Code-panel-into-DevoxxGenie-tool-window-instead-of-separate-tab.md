---
id: TASK-204
title: >-
  Integrate Spec-to-Code panel into DevoxxGenie tool window instead of separate
  tab
status: Done
assignee: []
created_date: '2026-03-10 16:11'
updated_date: '2026-03-10 17:12'
labels:
  - ui
  - ux
  - spec-to-code
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowFactory.java
  - src/main/java/com/devoxx/genie/ui/window/ConversationTabRegistry.java
  - src/main/java/com/devoxx/genie/ui/panel/spec/SpecBrowserPanel.java
  - src/main/java/com/devoxx/genie/ui/window/SpecBrowserToolWindowFactory.java
  - 'src/main/resources/META-INF/plugin.xml:509'
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

The Spec-to-Code feature currently registers as a separate tool window (`DevoxxGenieSpecs`) with its own icon in the IDE sidebar (defined in `plugin.xml` line 509). When users have many plugins installed, the Specs icon can be hidden or pushed out of view, making the feature inaccessible.

## Solution

Move the Spec-to-Code panel from being a standalone tool window into the main DevoxxGenie tool window — e.g., as an internal tab, a sub-panel toggle, or a mode switch within the existing DevoxxGenie UI. This ensures users can always access it as long as they can see the DevoxxGenie icon.

## Key Files
- `src/main/resources/META-INF/plugin.xml` — `DevoxxGenieSpecs` tool window registration (line 509)
- `com.devoxx.genie.ui.window` — tool window factory classes
- `DevoxxGenieToolWindowContent` — main plugin window content

## Implementation Notes
- Remove or deprecate the separate `DevoxxGenieSpecs` tool window registration from `plugin.xml`
- Add the spec-to-code panel as a tab or section within `DevoxxGenieToolWindowContent`
- Preserve all existing spec-to-code functionality (spec editing, code generation, etc.)
- Consider using a `ContentManager` with tabs inside the existing tool window, or a toggle/button to switch views
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Spec-to-Code panel is accessible from within the main DevoxxGenie tool window (not as a separate IDE tool window)
- [ ] #2 The separate DevoxxGenieSpecs tool window registration is removed from plugin.xml
- [ ] #3 All existing Spec-to-Code functionality works correctly in the new location
- [ ] #4 Users can switch between the chat view and spec-to-code view within DevoxxGenie
- [ ] #5 No regression in the main DevoxxGenie chat/prompt functionality
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add `Key<Boolean> IS_SPEC_CONTENT` marker and helper `getChatTabCount(ContentManager)` in `DevoxxGenieToolWindowFactory`.

2. In `createToolWindowContent()`, after chat tabs, call `addSpecTabsIfEnabled()` — creates `SpecBrowserPanel` ("Task List") and `SpecKanbanPanel` ("Kanban Board") as non-closeable pinned tabs, marked with `IS_SPEC_CONTENT`.

3. Fix creation guard in `createNewTabWithId()`: use `getChatTabCount(cm) >= MAX_TABS` (not `getContentCount()`). Update `NewChatTabAction.update()` similarly. Fix "create new if empty" to use `getChatTabCount(cm) == 0`.

4. In `SpecBrowserPanel.implementCurrentSpec()`, switch to first non-spec chat tab after submitting prompt.

5. In `ConversationTabRegistry.getActiveTabId()`, if selected content is a spec tab, walk `ContentManager.getContents()` in tab order and return first non-spec chat tab's ID (deterministic fallback).

6. In `ExternalPromptService.setPromptText()`, when selected content yields null from registry (spec tab), find and select first non-spec chat tab before routing prompt.

7. Add `dispose()` to `SpecBrowserPanel` that calls `SpecService.removeChangeListener()` and `SpecTaskRunnerService.removeListener()`. Fix `SpecKanbanPanel.dispose()` to also call `SpecService.removeChangeListener()` (currently leaked).

8. Subscribe to `SETTINGS_CHANGED_TOPIC` for dynamic add/remove of spec tabs. On removal: dispose panels, remove content. Track spec Content references in fields.

9. Remove `DevoxxGenieSpecs` registration from plugin.xml (lines 509-514). Delete `SpecBrowserToolWindowFactory.java`.

10. Tab ordering: insert new chat tabs at `getChatTabCount()` index so spec tabs stay at the end.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Explored the codebase. The main DevoxxGenie tool window uses IntelliJ's ContentManager with closeable tabs for chat conversations (max 8). The Spec tool window currently has its own factory (`SpecBrowserToolWindowFactory`) creating two tabs: Task List (`SpecBrowserPanel`) and Kanban Board (`SpecKanbanPanel`).

The chosen approach adds spec panels as non-closeable pinned Content tabs within the existing ContentManager, using a `DataKey` marker to distinguish them from chat tabs. This avoids nested tab UIs and doesn't require rewriting the chat tab management system.

Key concern: `getActiveTabId()` in `ConversationTabRegistry` returns null when a spec tab is selected (since spec tabs aren't registered in the contentMap). Added fallback logic to route prompts to the first available chat tab.

Critical files:
- `ui/window/DevoxxGenieToolWindowFactory.java` — main changes
- `ui/window/ConversationTabRegistry.java` — fallback routing
- `ui/panel/spec/SpecBrowserPanel.java` — switch to chat after implement
- `META-INF/plugin.xml` — remove DevoxxGenieSpecs registration
- `ui/window/SpecBrowserToolWindowFactory.java` — to be removed

Revised plan after review: (1) ExternalPromptService.setPromptText() needs fallback when spec tab is selected — external integrations like SonarLint break otherwise. (2) SpecBrowserPanel and SpecKanbanPanel need proper disposal with listener cleanup for dynamic add/remove. SpecKanbanPanel has a leaked SpecService change listener. (3) MAX_TABS guard in createNewTabWithId() must use chat-tab count, not total content count. (4) getActiveTabId() fallback must walk ContentManager.getContents() in tab order for determinism, not iterate ConcurrentHashMap.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary

Integrated the Spec-to-Code panels (Task List + Kanban Board) into the main DevoxxGenie tool window as non-closeable, pinned tabs instead of a separate `DevoxxGenieSpecs` sidebar tool window.

## Changes

### `DevoxxGenieToolWindowFactory.java` (major rewrite)
- Added `IS_SPEC_CONTENT` DataKey marker to distinguish spec tabs from chat tabs
- Added `getChatTabCount()` utility to count only chat tabs (excludes spec tabs)
- Added `addSpecTabsIfEnabled()` to create Task List and Kanban Board as pinned, non-closeable Content tabs
- Added `updateSpecTabs()` for dynamic add/remove when settings change (listens to `SETTINGS_CHANGED_TOPIC`)
- Added `findFirstChatTab()` helper (public, used by ExternalPromptService)
- Creation guard uses `getChatTabCount()` so spec tabs don't eat into the 8-tab chat limit
- `contentRemoved` listener skips cleanup for spec tabs and checks `getChatTabCount() == 0` for auto-create
- New chat tabs inserted before spec tabs (spec tabs always at end of tab bar)
- Made class `public` for cross-package access

### `ConversationTabRegistry.java`
- `getActiveTabId()` now falls back to the first chat tab (in tab order) when a spec tab is selected

### `ExternalPromptService.java`
- When selected content is a spec tab (twc is null), finds and selects the first chat tab before routing the prompt

### `SpecBrowserPanel.java`
- Now implements `Disposable` with proper `dispose()` that removes spec change listener and runner listener
- Stored `specChangeListener` Runnable reference for cleanup
- `implementCurrentSpec()` now switches to the first chat tab before submitting the prompt
- Splitter always vertical (tree on top, details below) since it now has the full tool window height
- Removed dynamic orientation switching (`updateSplitterOrientation`, `HORIZONTAL_LAYOUT_MIN_WIDTH`)

### `SpecKanbanPanel.java`
- Fixed leaked `SpecService` change listener: stored `specChangeListener` reference, removed in `dispose()`

### `plugin.xml`
- Removed `DevoxxGenieSpecs` tool window registration (lines 509-514)

### Deleted
- `SpecBrowserToolWindowFactory.java` — logic moved into `DevoxxGenieToolWindowFactory`
<!-- SECTION:FINAL_SUMMARY:END -->
