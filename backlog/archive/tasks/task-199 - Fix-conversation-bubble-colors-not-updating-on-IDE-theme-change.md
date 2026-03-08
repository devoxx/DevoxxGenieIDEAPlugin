---
id: TASK-199
title: Fix conversation bubble colors not updating on IDE theme change
status: Done
assignee:
  - Codex
created_date: '2026-03-08 16:42'
updated_date: '2026-03-08 17:01'
labels:
  - bug
  - ui
  - theme
  - compose
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AiBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/UserBubble.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ConversationScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/theme/DevoxxGenieTheme.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java
  - src/main/java/com/devoxx/genie/ui/util/ThemeChangeListener.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When the IDE appearance is switched between dark and light themes, the DevoxxGenie conversation UI does not fully refresh its bubble foreground/background colors. In particular, AI bubbles can retain stale dark-theme styling after switching to a light theme, leaving the chat visually inconsistent with the active IDE appearance. Fix the Compose conversation rendering path so message bubble colors react correctly to IDE theme changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Switching the IDE between dark and light themes updates AI bubble background and text colors to match the active theme without requiring the tool window to be reopened.
- [x] #2 Switching the IDE between dark and light themes updates user bubble background and text colors to match the active theme without requiring the tool window to be reopened.
- [x] #3 Code block backgrounds and message text within chat bubbles also refresh consistently with the active theme.
- [x] #4 Existing conversations already visible in the chat repaint correctly after a theme change.
- [x] #5 Theme changes do not regress conversation rendering, streaming updates, or appearance-settings refresh behavior.
- [x] #6 Automated regression coverage verifies theme-change refresh behavior for Compose conversation bubbles.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Subscribe the conversation UI to project theme-change notifications and forward them into the Compose conversation controller so light/dark IDE switches update the active chat view without reopening the tool window.
2. Keep the Compose bubble/code-block rendering theme-aware by rebuilding from the current Devoxx theme state for both user and AI markdown content.
3. Add targeted regression coverage for theme-state propagation in the Compose conversation path and run the relevant tests before closing the task.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Subscribed ConversationPanel to ThemeChangeNotifier.THEME_CHANGED_TOPIC so active Compose chat views now receive IDE light/dark theme changes without reopening the tool window.

On theme change, ConversationPanel now refreshes its Swing/container backgrounds and forwards the new dark/light flag into Compose via viewController.themeChanged(...).

ComposeConversationViewController now revalidates and repaints the ComposePanel after both themeChanged() and appearanceSettingsChanged() to force visible chat content to refresh immediately.

Added ConversationViewModelTest to verify theme changes update theme state without dropping existing visible chat messages.

Follow-up investigation showed the previous repaint-only change was insufficient because ThemeChangeListener.register() was effectively never called for normal opened projects: PostStartupActivity gated it behind project.isDefault().

ThemeChangeListener registration is now invoked on startup for real projects, with an internal one-time guard to avoid duplicate listener registration across projects.

ThemeChangeListener now computes the current dark/light state directly from the active LAF via JBColor.isBright() when publishing ThemeChangeNotifier events, avoiding stale cached theme values during appearance switches.

Re-ran focused tests after the listener registration fix: ./gradlew -q test --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest --tests com.devoxx.genie.ui.compose.components.ExtractCodeTextTest passed.

Manual validation showed the previous fix was incomplete: theme refresh worked for one light/dark switch but not reliably for subsequent toggles. Reopening investigation to trace repeat theme-change delivery and Compose markdown recomposition behavior.

Second follow-up fix: the theme publisher could still emit a stale dark/light value on repeated appearance toggles because it read the theme too early during lookAndFeelChanged(). ThemeChangeListener now defers publication with invokeLater and derives the active theme from StartupUiUtil.isUnderDarcula() after the LAF has settled.

ThemeDetector was aligned to the same StartupUiUtil-based detection so theme-dependent code paths use a consistent source of truth across startup and runtime theme changes.

Re-ran focused tests after the repeated-toggle fix: ./gradlew -q test --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest --tests com.devoxx.genie.ui.compose.components.ExtractCodeTextTest passed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Fixed live Compose chat theme refresh so conversation bubbles now update immediately and reliably when the IDE appearance switches between dark and light themes, including repeated toggles, without requiring an IDE restart or reopening the tool window. The conversation panel now subscribes to the project theme-change topic, refreshes its Swing/container backgrounds, and forwards the new theme state into the Compose conversation controller. The Compose controller also revalidates and repaints after theme and appearance updates so existing visible messages refresh in place.

The work addressed two separate root causes. First, the theme-change listener was effectively never registered for normal opened projects because startup registration was gated behind project.isDefault(); registration now runs for real project startup with a one-time guard. Second, repeated appearance switches could still publish a stale dark/light value because the theme was read too early during lookAndFeelChanged(); the publisher now defers via invokeLater and derives the active theme from StartupUiUtil.isUnderDarcula() after the LAF has settled. ThemeDetector was aligned to the same source of truth. Focused regression coverage in ConversationViewModelTest verifies theme changes update Compose theme state without dropping visible chat content, and the targeted test run passed: ./gradlew -q test --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest --tests com.devoxx.genie.ui.compose.components.ExtractCodeTextTest. Manual validation confirmed that repeated IDE appearance switches now refresh visible chat bubbles correctly.
<!-- SECTION:FINAL_SUMMARY:END -->
