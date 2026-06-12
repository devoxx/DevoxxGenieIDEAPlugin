---
id: TASK-237
title: Chat output font zoom via CMD/Ctrl +/- keyboard shortcuts
status: Done
assignee: []
created_date: '2026-06-12 14:21'
updated_date: '2026-06-12 14:32'
labels:
  - enhancement
  - ui
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1113'
modified_files:
  - src/main/java/com/devoxx/genie/ui/util/ChatFontSizeService.java
  - src/main/java/com/devoxx/genie/action/AbstractChatFontSizeAction.java
  - src/main/java/com/devoxx/genie/action/IncreaseChatFontSizeAction.java
  - src/main/java/com/devoxx/genie/action/DecreaseChatFontSizeAction.java
  - src/main/resources/META-INF/plugin.xml
  - src/test/java/com/devoxx/genie/ui/util/ChatFontSizeServiceTest.java
  - docusaurus/static/api/tips.json
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add keyboard shortcuts to increase/decrease the chat output panel font size, like editor zoom.

- CMD +/- on macOS, Ctrl +/- on Windows/Linux.
- Scales both prose (customFontSize) and code (customCodeFontSize) together.
- Persists across IDE restarts (DevoxxGenieStateService is @State-persisted).
- Reuses existing appearance refresh plumbing: mutate state -> publish APPEARANCE_SETTINGS_TOPIC -> ConversationPanel recomposes the Compose chat view.
- Scoped to the DevoxxGenie tool window (action disabled when tool window not active, so the shortcut passes through to the editor elsewhere).

Trackpad pinch / Cmd+scroll deferred to a follow-up (needs JBR/Compose gesture spike).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 CMD+ / CMD- (Mac) and Ctrl+ / Ctrl- (Win/Linux) increase/decrease chat output font size
- [x] #2 Both prose and code font sizes scale together, clamped to 8-24
- [x] #3 Font size persists across IDE restarts
- [x] #4 Shortcut only active when DevoxxGenie tool window is focused
- [x] #5 Unit test covers the clamp/next-size logic
- [x] #6 Project builds with JDK 21
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented via IDE AnActions + shortcuts (not Swing/Compose key handling) because the chat is Compose for Desktop and IDE actions take precedence regardless of Compose focus consumption.

- ChatFontSizeService.changeBy(delta): mutates DevoxxGenieStateService customFontSize + customCodeFontSize (clamped 8-24), sets useCustomFontSize/useCustomCodeFontSize=true, publishes APPEARANCE_SETTINGS_TOPIC. Persistence is free via @State.
- AbstractChatFontSizeAction.update() enables the action only when the DevoxxGenie tool window isActive(), so CMD/Ctrl +/- pass through to the editor elsewhere.
- plugin.xml: Increase = control/meta EQUALS + ADD; Decrease = control/meta MINUS + SUBTRACT (Mac OS X 10.5+ keymap for meta).

Verified: JDK 21 compileJava OK, 5/5 unit tests pass, buildPlugin SUCCESSFUL (plugin.xml valid). AC #4 (focus scoping) needs manual runIde confirmation.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-06-12 14:31
---
Manually verified working in runIde (user confirmed). Added a related tip to docusaurus/static/api/tips.json.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Cmd/Ctrl +/- shortcuts to zoom the Compose chat output font (prose + code together, clamped 8-24, persisted via @State). Actions are scoped to the DevoxxGenie tool window so the shortcut passes through to the editor elsewhere. Reuses the existing APPEARANCE_SETTINGS_TOPIC refresh path. Verified: 5/5 unit tests, buildPlugin, and manual runIde. Shipped in PR #1113. Trackpad pinch/Cmd+scroll deferred (needs JBR/Compose gesture spike).
<!-- SECTION:FINAL_SUMMARY:END -->
