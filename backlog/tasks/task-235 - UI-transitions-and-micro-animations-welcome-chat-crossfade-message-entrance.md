---
id: TASK-235
title: >-
  UI transitions and micro-animations: Welcome↔Chat crossfade, message entrance,
  animation consolidation
status: Done
assignee: []
created_date: '2026-06-10 12:00'
updated_date: '2026-06-10 20:11'
labels:
  - enhancement
  - UX
  - polish
dependencies: []
references:
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ConversationScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/WelcomeScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/screen/ChatScreen.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/MessagePair.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/model/ConversationState.kt
  - >-
    src/main/java/com/devoxx/genie/ui/component/border/AnimatedGlowingBorder.java
  - src/main/java/com/devoxx/genie/ui/component/border/GlowingBorder.java
  - src/main/java/com/devoxx/genie/ui/panel/ActionButtonsPanel.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
State changes in the conversation UI are functionally correct but visually abrupt, and the animation machinery is split across two technologies.

1. **Welcome ↔ Chat crossfade.** `ConversationScreen.kt` switches between `WelcomeScreen` and `ChatScreen` on `ConversationState` with a hard swap. Wrap the branch in Compose's `Crossfade` (or `AnimatedContent`) with a short tween (~150-200ms). This covers: first prompt submitted (welcome → chat), New Conversation / clear (chat → welcome), and conversation restored from history. Must respect the existing `isRestoringConversation` guard in `ConversationViewModel` — the restoration flag exists precisely because intermediate states flashed during restore; the crossfade must not reintroduce a welcome flash mid-restore.
2. **Message entrance animation.** New user/AI bubbles in `ChatScreen`'s `LazyColumn` pop in instantly. Add a subtle entrance (fade-in + slight vertical slide, ≤150ms) via `Modifier.animateItemPlacement()` on items with stable keys, or `AnimatedVisibility` inside `MessagePair`. Streaming text updates within an existing bubble must NOT re-trigger the animation — animate on item insertion only (stable message-id keys are the mechanism).
3. **Consolidate the submit glow.** `AnimatedGlowingBorder` drives a breathing alpha with a 50ms `javax.swing.Timer` on the Swing side (`ActionButtonsPanel`), while every other animation is Compose `infiniteTransition`. Keep behavior identical but: lower the timer cadence if CPU shows up in profiling, ensure the timer is stopped (not just hidden) whenever execution ends including error/stop paths, and document it as the only remaining Swing animation so it gets migrated when the input panel moves to Compose. (Full migration of `ActionButtonsPanel` to Compose is out of scope here.)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Welcome → Chat and Chat → Welcome transitions crossfade smoothly (~150-200ms) instead of hard-swapping
- [x] #2 Restoring a conversation from history never flashes the Welcome screen mid-restore (existing isRestoringConversation behavior preserved)
- [x] #3 Newly added messages fade/slide in; streaming updates to an existing bubble do not replay the entrance animation; LazyColumn items use stable keys
- [x] #4 Auto-scroll and the task-232 scroll-to-bottom behavior (if merged) still work correctly with entrance animations enabled — no scroll-position jumps
- [x] #5 The glow border timer is verifiably stopped on all execution-end paths (complete, error, stop) — no orphaned 50ms timer ticking while idle
- [x] #6 Animations respect theme switching (no hardcoded colors introduced; use DevoxxGenieThemeAccessor)
- [ ] #7 No measurable EDT jank introduced: scrolling a long conversation with animations enabled stays smooth (manual check on a 50+ message conversation)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `Crossfade(targetState = conversationState)` at the top of `ConversationScreen` is likely a ~10-line change; the care is entirely in the restore path and in not recomposing `ChatScreen` needlessly (key the crossfade on the state's type, not its full payload).
- For entrance animations prefer `animateItemPlacement` only if the Compose Multiplatform version in use supports it well inside the IDE's Compose panel; otherwise a `remember { MutableTransitionState(false) }`-driven `AnimatedVisibility` per message keyed by message id is the safe fallback.
- Keep durations short and subtle — this is an IDE tool window, not a consumer app. Anything ≥250ms will feel sluggish during rapid agent turns.
- Check whether the IDE's "disable animations" / remote-dev mode should gate these (IntelliJ exposes `UISettings` animation flags); if a global flag exists, honor it.
- CopyButton already has transient "✓ Copied" feedback — nothing to do there (verified 2026-06-10).
- Out of scope: migrating ActionButtonsPanel/UserPromptPanel to Compose, conversation-history popup styling (task-236), sound effects.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented short Compose-based UI transitions for TASK-235: ConversationScreen now uses AnimatedContent keyed by state type for Welcome <-> Chat fades, ChatScreen uses stable message-id LazyColumn keys plus one-shot fade/slide entrance animations, and restore-state handling prevents Welcome flashes during history restore, including restored messages added through addChatMessage/addSystemMessage. Added IdeAnimations to centralize animation durations and disable animations in power-save or remote-desktop sessions. Tightened the remaining Swing submit glow so stopGlowing always stops the 50ms timer and documented it as the only remaining Swing animation. Verification: `./gradlew test --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest` passed 12 tests. Manual long-conversation jank check was not run in this session.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1105
<!-- SECTION:FINAL_SUMMARY:END -->
