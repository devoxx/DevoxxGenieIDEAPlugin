---
id: TASK-230
title: Prototype find_callees PSI tool (Java-only spike to validate call-graph approach)
status: To Do
assignee: []
created_date: '2026-06-06 12:15'
updated_date: '2026-06-06 12:15'
labels:
  - spike
  - agent-tools
  - psi
  - code-navigation
dependencies:
  - TASK-229
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/FindReferencesToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
  - src/test/java/com/devoxx/genie/service/agent/tool/psi/
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A minimal, time-boxed spike that builds **only** the `find_callees` tool for **Java only**, to validate the PSI-based call-graph approach before committing to the full set of call-graph tools in task-229.

`find_callees` is the inverse of the existing `find_references`: given a method definition (`file` + `line`), it lists the methods that method *calls* (outgoing edges), with each callee's resolved definition location. It is the simplest of the four tools proposed in task-229 (no `CallHierarchy` traversal, no scoring, no exclusion heuristics) and therefore the right one to prove the pattern.

**Goal of the spike — answer these questions concretely, in code:**
- Can a `ToolExecutor` resolve a method body's call expressions to their definitions reliably inside a `ReadAction`, reusing `PsiToolUtils` for input resolution? (Walk the `PsiMethod` body for `PsiMethodCallExpression`, call `.resolveMethod()`, dedupe, format with `PsiToolUtils.formatLocation`.)
- What is the right output shape for the LLM (callee name, declaring class, location; how to render unresolved/library calls)?
- How should calls into JDK / third-party libraries (no project source) be presented vs. project-local callees?
- Is the `findNamedElementOnLine` helper sufficient to target the enclosing method, or is a small "find enclosing `PsiMethod`" helper needed (and should that land in `PsiToolUtils` for reuse by task-229)?

Keep it deliberately small: **Java only**, no Kotlin/UAST, no other tools. If the spike works cleanly, task-229 absorbs it and generalises (Kotlin/UAST, the other three tools); if PSI resolution proves fiddly, the findings reshape task-229's scope. Either way this should be a few hours, not days.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `FindCalleesToolExecutor` exists under `service/agent/tool/psi/`, registered as `find_callees` in `BuiltInToolProvider.registerPsiTools`, gated behind `psiToolsEnabled` and honouring `disabledAgentTools` like the other PSI tools
- [ ] #2 Given `file` + `line` (with optional `symbol` disambiguator) pointing at a Java method, it returns the resolved outgoing calls — callee name, declaring class, and definition location for project-local targets — bounded by a `MAX_RESULTS` cap
- [ ] #3 Calls resolving to JDK / library code (no project source) are listed but clearly distinguished from project-local callees; unresolvable calls are reported without failing the tool
- [ ] #4 Runs inside a `ReadAction`, reuses `PsiToolUtils`, and returns the same friendly `Error: ...` strings on bad input (missing file, no method on line) as `FindReferencesToolExecutor` — never throws to the LLM
- [ ] #5 Non-Java files return a clear "find_callees currently supports Java only" message rather than failing
- [ ] #6 A platform test (extending `AbstractLightPlatformTestCase`, mirroring the existing PSI tool tests) covers a Java fixture where method A calls B and C: the tool returns B and C, and reports a no-callees method correctly
- [ ] #7 A short findings note is added to task-229's Implementation Notes (or as a comment on this task) recording: whether an enclosing-method helper was needed, the chosen output shape, and any PSI gotchas — so task-229 can generalise with eyes open
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Copy the skeleton of `FindReferencesToolExecutor` almost verbatim — same constructor, `MAX_RESULTS` cap, `ReadAction.compute(...)` wrapper, `PsiToolUtils` usage, and error-string style. The only new logic is: from the resolved target element, get the enclosing `PsiMethod`, then `PsiTreeUtil.findChildrenOfType(method, PsiMethodCallExpression.class)`, `.resolveMethod()` each, dedupe by resolved method, and format.

If `PsiToolUtils.findNamedElementOnLine` lands you on a name identifier rather than the `PsiMethod`, walk up with `PsiTreeUtil.getParentOfType(el, PsiMethod.class)`. If that helper turns out generally useful, promote it into `PsiToolUtils` so task-229's `calculate_complexity`/`trace_call_chains` reuse it.

Explicitly out of scope for this spike: Kotlin/UAST support, `trace_call_chains`, `calculate_complexity`, `find_dead_code`, per-tool settings toggles. Those belong to task-229. Resist generalising here — the point is a fast, throwaway-able validation.
<!-- SECTION:NOTES:END -->
