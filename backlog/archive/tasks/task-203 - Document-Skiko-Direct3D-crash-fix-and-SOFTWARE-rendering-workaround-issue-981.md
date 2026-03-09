---
id: TASK-203
title: >-
  Document Skiko/Direct3D crash fix and SOFTWARE rendering workaround (issue
  #981)
status: Done
assignee: []
created_date: '2026-03-09 11:34'
updated_date: '2026-03-09 11:58'
labels:
  - documentation
  - bug-fix
  - windows
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/981'
  - src/main/kotlin/com/devoxx/genie/ui/compose/SafeComposeContainer.kt
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/ComposeConversationViewController.kt
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Document the fix for GitHub issue #981 where the plugin crashes with `UnsatisfiedLinkError` on certain Windows GPU/driver configurations when Skiko tries to initialize Direct3D rendering.

**What was the problem:**
- `ComposePanel.addNotify()` triggers Skiko's `Direct3DSwingRedrawer` which throws `UnsatisfiedLinkError`
- Skiko's `RedrawerManager.findNextWorkingRenderApi()` only catches `RenderException`, not `Error`, so the error propagates and crashes the tool window

**What was fixed (branch `fix/issue-981`):**
- Added `SafeComposeContainer.kt` — a JPanel wrapper that catches `Throwable` during `addNotify()` and shows a fallback Swing panel with recovery instructions
- Modified `ComposeConversationViewController.kt` to use SafeComposeContainer instead of creating ComposePanel directly
- Tracks `composeInitFailed` flag to skip theme/appearance updates when in fallback mode

**User workaround:**
Add `-Dskiko.renderApi=SOFTWARE` to IDE VM options (Help > Edit Custom VM Options) to force software rendering.

**Documentation needed:**
- Add a troubleshooting entry to the Docusaurus docs covering this error and the workaround
- Consider adding a FAQ entry about GPU driver compatibility
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Troubleshooting entry added to Docusaurus docs covering `UnsatisfiedLinkError` and the `-Dskiko.renderApi=SOFTWARE` workaround
- [x] #2 FAQ entry added about Windows GPU driver/rendering compatibility
- [x] #3 User can force software rendering before Compose initialization via Settings > Appearance checkbox and restart
- [x] #4 Fallback error/help panel explains recovery steps, including the startup VM option and settings-based workaround, when Compose initialization fails
- [x] #5 Task notes document that plugin-local runtime retry is not a robust fix and that startup/early-init renderer selection is the supported workaround
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Corrected completed plan

1. Add and expose a persistent software-rendering preference.
2. Apply `skiko.renderApi=SOFTWARE` before any Compose UI is initialized when that preference is enabled.
3. Document the Windows rendering failure, the startup VM option workaround, and the settings-based workaround.
4. Ensure the fallback/help panel provides recovery guidance instead of allowing the tool window to fail without instructions.
5. Record the investigation outcome that plugin-local runtime retry is not the supported recovery path and that upstream/library-level fixes remain the stronger long-term option.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Corrected implementation record

### Final understanding
This task started with a runtime SOFTWARE-retry narrative, but follow-up source inspection showed that the important supported workaround is **pre-start renderer selection**, not plugin-local retry after initialization has already started.

### What remains valid
1. **User-facing workaround remains valid**
   - `-Dskiko.renderApi=SOFTWARE` as a JVM startup option can bypass the Direct3D path on affected Windows systems.

2. **Persistent plugin setting remains valid**
   - The plugin exposes a "Force software rendering" setting that applies the renderer choice before Compose UI creation on next startup.

3. **Fallback/help UI remains useful**
   - If Compose initialization fails, the plugin can still catch the failure at the container boundary and show recovery instructions instead of hard-crashing the tool window.

### What was corrected
1. **Runtime retry is not a robust fix in this plugin**
   - `ComposePanel` performs real Compose/Skiko initialization in `addNotify()`, not at simple construction time.
   - A retry wrapped around panel creation is therefore not aligned with the actual failure boundary.

2. **True native library load failures are one-shot**
   - In Skiko `0.8.18`, `Library.load()` marks the library as loaded before `findAndLoad()` completes.
   - If the native library load itself fails, later attempts are skipped.
   - Changing `skiko.renderApi` afterward cannot recover that class of failure.

3. **Runtime retry should not be treated as the supported recovery path**
   - It is still theoretically possible for a fresh instance to observe a new renderer value in some post-load failure scenarios.
   - But for this plugin, the current retry design should be considered unreliable and superseded by startup/early-init renderer selection.

### Recommended supported behavior
- Prefer early-start renderer selection via settings or JVM option.
- Show a fallback/help panel with recovery instructions if Compose initialization still fails.
- Treat upstream/local Skiko fixes or architectural renderer-path control as the stronger long-term direction.

### Files relevant to the supported solution
- `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`
- `src/main/java/com/devoxx/genie/service/PostStartupActivity.java`
- `src/main/java/com/devoxx/genie/ui/settings/appearance/AppearanceSettingsComponent.java`
- `src/main/kotlin/com/devoxx/genie/ui/compose/SafeComposeContainer.kt`
- `docusaurus/docs/getting-started/troubleshooting.md`
- `docusaurus/docs/getting-started/faq.md`

### Superseded notes
Any earlier notes in this task that describe plugin-local automatic SOFTWARE retry as the definitive working fix should be treated as superseded by this corrected record.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Corrected summary

Issue #981 is a Windows-specific Compose/Skiko initialization failure involving the Direct3D path. The durable, supported workaround is to select SOFTWARE rendering **before** Compose initialization, either through `-Dskiko.renderApi=SOFTWARE` at JVM startup or through the plugin's persisted "Force software rendering" setting applied on startup.

The task now reflects that plugin-local runtime retry should not be treated as the robust fix. The reliable user-facing outcome is:
- documented troubleshooting guidance
- a settings-based startup workaround
- a fallback/help panel with recovery steps when initialization fails

Longer term, stronger fixes remain an upstream/local Skiko patch or a renderer-path architecture change in newer Compose/Skiko.
<!-- SECTION:FINAL_SUMMARY:END -->
