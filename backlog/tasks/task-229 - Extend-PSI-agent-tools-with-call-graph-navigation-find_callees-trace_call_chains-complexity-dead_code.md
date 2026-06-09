---
id: TASK-229
title: >-
  Extend PSI agent tools with call-graph navigation (find_callees,
  trace_call_chains, complexity, dead_code)
status: In Progress
assignee: []
created_date: '2026-06-06 12:00'
updated_date: '2026-06-08 14:32'
labels:
  - enhancement
  - agent-tools
  - psi
  - code-navigation
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindReferencesToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindImplementationsToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDefinitionToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindSymbolsToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/DocumentSymbolsToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
  - 'https://github.com/CodeGraphContext/CodeGraphContext'
modified_files:
  - src/main/java/com/devoxx/genie/service/agent/tool/psi/PsiToolUtils.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindCalleesToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/TraceCallChainsToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/CalculateComplexityToolExecutor.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/psi/FindDeadCodeToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/psi/FindCalleesToolExecutorTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/psi/TraceCallChainsToolExecutorTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/psi/CalculateComplexityToolExecutorTest.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/psi/FindDeadCodeToolExecutorTest.java
  - docusaurus/docs/features/agent-mode.md
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
This task comes out of a feasibility investigation into [CodeGraphContext](https://github.com/CodeGraphContext/CodeGraphContext) (CGC) — a Python/TypeScript tool that turns a repository into a queryable code graph (tree-sitter → Neo4j/KuzuDB) and exposes it to AI agents over MCP (`find_callers`, `find_callees`, `find_definitions`, `analyze_inheritance`, `trace_call_chains`, `find_dead_code`, `calculate_complexity`, ...).

**Conclusion of the investigation: do not port CGC to Java.** The DevoxxGenie plugin already provides the core of CGC's value through PSI-based agent tools that ride on IntelliJ's native semantic index — an always-live, incrementally-updated, language-aware code graph. Today's PSI tools (registered in `BuiltInToolProvider.registerPsiTools`) already cover most of CGC's surface:

| CGC capability | DevoxxGenie equivalent (exists) |
|---|---|
| `find_definitions` | `find_definition` |
| `find_callers` / usages | `find_references` |
| `analyze_inheritance` | `find_implementations` |
| symbol lookup | `find_symbols`, `document_symbols` |

Porting tree-sitter grammars + a graph DB + a file-watching indexer would rebuild — less accurately and with a heavy Docker/DB runtime dependency — what PSI already gives us inside the IDE. (We already feel the friction of an external-DB dependency with the ChromaDB-backed RAG feature; we should not add a second one.)

What CGC has that we **don't** yet, and that is cheap to add on top of PSI / the IntelliJ `CallHierarchy` API, is the **call-graph direction and analysis** primitives. This task closes those gaps by adding new tools to the existing `psi/` package, following the established `ToolExecutor` + `ProjectAnalyzerExtension`-style pattern and the `PsiToolUtils` helpers.

**New tools to add:**

- **`find_callees`** — given a method definition (file + line), list the methods it calls (outgoing edges). This is the inverse of the existing `find_references` and is the single most-requested missing primitive. Implementable by walking the method body's `PsiCallExpression` / `PsiMethodCallExpression` nodes (and language equivalents via the UAST/`PsiCall` abstraction where available) and resolving each call target.
- **`trace_call_chains`** — given a start symbol and an optional target symbol, traverse caller→callee (or callee→caller) edges up to a bounded depth and return the path(s). Build on IntelliJ's `CallHierarchy` (`com.intellij.ide.hierarchy.call`) APIs rather than re-implementing traversal. Must be depth- and result-bounded to stay responsive and avoid context blow-up.
- **`calculate_complexity`** — compute cyclomatic complexity for a method (or all methods in a file) by counting decision points (`if`/`for`/`while`/`case`/`catch`/`&&`/`||`/ternary) over the PSI tree. Returns per-method scores and flags methods over a configurable threshold.
- **`find_dead_code`** — report symbols (methods/fields/classes) with zero project-scope references via `ReferencesSearch`, with sensible exclusions (entry points, public API, `@Override`, framework-annotated members, test fixtures, reflection/serialization). This is heuristic — must be clearly labelled as "candidates", not certainties, in the tool description and output.

**Investigation areas (resolve during implementation):**
- **Language reach.** PSI reference/definition search is language-agnostic, but call-body traversal (`find_callees`) and complexity counting are easiest via Java PSI / UAST. Decide which languages are in-scope for v1 (recommend Java + Kotlin via UAST, with graceful "unsupported language" messages for others) and which degrade to "not available for this language".
- **`CallHierarchy` API surface.** Confirm the call-hierarchy provider APIs are usable headlessly from a `ToolExecutor` inside a `ReadAction`, and whether they require the language plugin to be loaded (mirror the `isJavaAvailable()` reflection guard used in `JavaProjectScannerExtension`).
- **Bounding.** All four tools must cap results (see `MAX_RESULTS = 50` in `FindReferencesToolExecutor`) and `trace_call_chains` must cap depth (suggest default 5, max 10) and total paths.
- **Settings gating.** These register only when `psiToolsEnabled` is true, exactly like the existing PSI tools, and respect the `disabledAgentTools` list in `provideTools`.

**Out of scope:** any graph-DB / tree-sitter / external-process architecture; multi-repo or non-indexed-file analysis (that is the one niche where standing up CGC as an external MCP server — which DevoxxGenie can already consume as an MCP client — is the right answer; capture that as a docs note, not code).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `find_callees` tool registered in `BuiltInToolProvider.registerPsiTools`: given `file` + `line` of a method definition (with optional `symbol` disambiguator), returns the resolved outgoing method calls with their definition locations, bounded to a `MAX_RESULTS` cap
- [x] #2 `trace_call_chains` tool registered: given a start symbol (file + line), an optional target symbol, a `direction` (`callers` | `callees`, default `callers`), and a bounded `depth` (default 5, hard max 10), returns the call path(s) found, capped in both depth and number of paths
- [x] #3 `calculate_complexity` tool registered: given a `file` (and optional `line` to target a single method), returns per-method cyclomatic complexity scores and flags methods exceeding a configurable threshold
- [x] #4 `find_dead_code` tool registered: returns symbols with zero project-scope references, clearly labelled as heuristic "candidates", with documented exclusions (entry points, `@Override`, public API, framework-annotated, reflection/serialization, tests)
- [x] #5 All four tools run inside a `ReadAction`, reuse `PsiToolUtils` helpers (`resolvePsiFile`, `findNamedElementOnLine`, `formatLocation`, `getProjectBase`), and return the same friendly `Error: ...` strings on bad input as the existing PSI tools — never throw to the LLM
- [x] #6 All four tools are gated behind `psiToolsEnabled` and honour the `disabledAgentTools` list, matching the existing PSI tool registration
- [x] #7 Languages without call-body/complexity support return a clear "not supported for <language>" message rather than failing; Java is supported in v1 (Kotlin via UAST if feasible within scope)
- [x] #8 Unit/platform tests (extending `AbstractLightPlatformTestCase`, mirroring existing PSI tool tests) cover, per supported language fixture: callees of a method are resolved; a known caller chain is traced within the depth bound; complexity of a method with known decision points matches the expected count; an unreferenced private method is reported as dead-code candidate while an `@Override`/entry-point method is not
- [x] #9 Tool descriptions explain when to prefer each tool over `search_files`/`find_references`, and the `find_dead_code` description states results are candidates requiring human confirmation
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented four new PSI ToolExecutors under service/agent/tool/psi/, registered in BuiltInToolProvider.registerPsiTools (gated by psiToolsEnabled; disabledAgentTools honoured automatically by provideTools).

Key decisions:
- Java only in v1 (per AC #7). Each tool guards with PsiToolUtils.isJavaAvailable() (reflection on JavaPsiFacade) + isJavaFile() (language ID == "JAVA"), returning a clear "not supported for <language>" message otherwise. Java is an OPTIONAL plugin dependency (<depends optional="true">com.intellij.modules.java</depends>), so Java-PSI calls are isolated in helper methods invoked only after the guard — lazy classloading keeps the executors loadable in non-Java IDEs. Kotlin-via-UAST deferred to a follow-up.
- trace_call_chains: implemented as a depth/path/node-bounded DFS over PSI primitives (ReferencesSearch for caller direction, method-body call resolution for callee direction) rather than the UI-bound com.intellij.ide.hierarchy.call API, which is fragile headlessly inside a ReadAction. Bounds: depth default 5 / hard max 10, MAX_PATHS=20, NODE_BUDGET=500. Optional target stops early.
- find_dead_code: scoped per-file (bounded, matches existing per-file PSI tools) rather than whole-project. Conservative exclusions (public, constructors/main, @Override + any annotation, serialization members) to minimise false positives; output explicitly labelled heuristic CANDIDATES.
- calculate_complexity: McCabe count via PsiTreeUtil collection of decision points (if/for/foreach/while/do/case/catch/ternary/&&/||); complexityOf(PsiMethod) is package-visible static for direct unit testing.

Testing: 27 fixture+validation tests (BasePlatformTestCase + Jupiter @Test, with @AfterEach tearDown() to avoid platform injector-leak failures). All pass; full suite green.

Added docs to docusaurus/docs/features/agent-mode.md (tool table + per-tool sections). NOTE: the requested docs note that power users wanting CGC's graph for polyglot/non-indexed repos can run codegraphcontext as an external MCP server was NOT yet added — capture in a follow-up docs PR.

Docs note for CGC-as-external-MCP (polyglot/non-indexed repos) WAS added — admonition in docusaurus/docs/features/agent-mode.md linking to mcp_expanded.md. (Supersedes the earlier 'NOT yet added' line above.)
<!-- SECTION:NOTES:END -->
