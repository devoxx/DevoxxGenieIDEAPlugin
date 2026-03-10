---
id: TASK-203
title: Force skiko.renderApi=SOFTWARE on Windows before library load
status: Done
priority: high
assignee:
  - codex
created_date: '2026-03-10 16:09'
updated_date: '2026-03-10 16:50'
labels:
  - bug
  - windows
  - rendering
  - skiko
dependencies: []
references:
  - src/main/resources/META-INF/plugin.xml
documentation: []
ordinal: 1000
---

<!-- SECTION:DESCRIPTION:BEGIN -->
On Windows, the Skiko rendering library (used by Compose/JCEF) can fail or produce rendering artifacts when using the default GPU-accelerated render API. The plugin should force `skiko.renderApi=SOFTWARE` as a system property on Windows **before** the Skiko library is loaded, ensuring stable software rendering.

This is typically done by setting `System.setProperty("skiko.renderApi", "SOFTWARE")` early in the plugin initialization lifecycle (e.g., in a `preloadActivity` or application-level service initialization), before any Compose or Skiko-dependent UI component is instantiated.

**Why this matters:**
- Some Windows machines with older or incompatible GPU drivers experience crashes or blank panels when Skiko uses hardware rendering
- Setting the property after the library is loaded has no effect — it must be set before first use
- This only affects Windows; macOS and Linux do not exhibit the same issues
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

- [ ] #1 System property `skiko.renderApi` is set to `SOFTWARE` on Windows before any Skiko library is loaded
- [ ] #2 The property is only set on Windows (not macOS or Linux)
- [ ] #3 The property is set early enough in plugin initialization (preloadActivity or equivalent) to take effect before any Compose/JCEF/Skiko UI component is created
- [ ] #4 No regression on macOS or Linux rendering

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add an application-level startup initializer that sets `skiko.renderApi=SOFTWARE` on Windows before any DevoxxGenie Compose UI is created.
2. Keep existing project startup work in `PostStartupActivity`, but remove Skiko property handling from that project-level hook because it is too late and should no longer be conditional.
3. Remove or update the existing user-facing opt-in software-rendering setting so the UI no longer implies this behavior is optional on Windows.
4. Add focused tests for the startup initializer behavior and run the relevant test/compile verification.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Investigation: the existing project-level `PostStartupActivity` sets `skiko.renderApi` too late, after application startup. That timing explains why Windows backend/linking failures can occur before the SafeComposeContainer fallback path logs anything.

Implementation update: moved Windows render API forcing to an application-level `applicationInitializedListener`, removed the obsolete appearance toggle, and updated docs/runtime messaging to reflect unconditional Windows software rendering.

Verification update: focused unit test for the new initializer passes; `buildSearchableOptions --no-daemon` succeeds after fixing the listener to override `execute` on this IntelliJ platform line.

Verification refinement: `verifyPlugin --no-daemon` on the updated build shows the new Windows startup hook is compatible across IC 2025.1, IC 2025.2, and IU 2025.3. Switching from `applicationInitializedListener` to a public `AppLifecycleListener` registration removed the internal API warnings introduced by the first implementation attempt.

Follow-up fix: moved `WindowsSkikoRenderApiInitializer` registration out of `<extensions>` and into a top-level `<applicationListeners>` block after confirming platform listeners are declared there in the IDE's own bundled plugin.xml.

Post-fix verification: `buildSearchableOptions --no-daemon` still succeeds, and `verifyPlugin --no-daemon` reports the plugin as compatible on IC 2025.1 / IC 2025.2 / IU 2025.3 with no internal-API findings from the Windows startup hook. Remaining verifier findings are the pre-existing deprecated `StartupUiUtil.isUnderDarcula()` usages.
<!-- SECTION:NOTES:END -->

