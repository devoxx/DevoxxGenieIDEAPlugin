---
id: TASK-289
title: Improve parallel sub-agents based on Codex multi-agent architecture
status: To Do
assignee: []
created_date: '2026-11-28T10:33:00Z'
updated_date: '2026-11-28T10:07:00Z'
labels:
  - agent
  - sub-agents
  - architecture
  - spike
  - codex
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/SubAgentRunner.java
  - src/main/java/com/devoxx/genie/service/agent/tool/ReadOnlyToolProvider.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolDescriptions.java
  - src/main/java/com/devoxx/genie/service/agent/AgentLoopTracker.java
  - src/main/java/com/devoxx/genie/service/agent/AgentToolProviderFactory.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/threading/ThreadPoolManager.java
  - src/main/java/com/devoxx/genie/chatmodel/ChatModelProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/nonstreaming/NonStreamingPromptExecutionService.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - src/main/java/com/devoxx/genie/model/agent/SubAgentConfig.java
  - src/main/java/com/devoxx/genie/service/spec/SpecTaskRunnerService.java
  - >-
    src/main/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModel.kt
  - src/main/kotlin/com/devoxx/genie/ui/compose/components/AgentProgressLine.kt
  - >-
    backlog/tasks/task-225 -
    Agent-task-planner-with-explicit-decomposition-and-writable-sub-agent-delegation.md
  - >-
    backlog/tasks/task-285 -
    Agent-search_files-holds-the-read-lock-for-the-whole-search-and-freezes-the-IDE.md
  - >-
    backlog/tasks/task-288 -
    Show-live-progress-while-an-agent-run-is-in-flight.md
  - >-
    src/main/java/com/devoxx/genie/service/prompt/memory/TaskPreservingMessageWindowChatMemory.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/service/MessageCreationService.java
  - src/main/java/com/devoxx/genie/service/trace/AgentTraceRecorder.java
  - src/main/java/com/devoxx/genie/service/rag/SemanticSearchService.java
  - >-
    src/main/java/com/devoxx/genie/service/rag/embedding/EmbeddingModelFactory.java
  - >-
    src/main/java/com/devoxx/genie/chatmodel/local/ollama/OllamaChatModelFactory.java
  - src/test/java/com/devoxx/genie/service/agent/SubAgentMemoryTest.java
priority: medium
type: spike
ordinal: 40000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Context & Purpose

This task compares OpenAI Codex's multi-agent system (`../codex/codex-rs`) with DevoxxGenie's parallel sub-agent implementation (`parallel_explore` via `ParallelExploreToolExecutor` + `SubAgentRunner`) and records defects, gaps and a prioritised roadmap, so the agreed work can be split into implementation tasks.

**Evidence (2026-11-28):** The initial review read this repo on `local/test-283-285`, Codex at `065e18b0`, and the LangChain4j 1.24.0 sources. A follow-up ran 30 existing focused tests and a separate offline probe of the compiled chat-memory class. B14 records the reproduced memory states; timing, cancellation and provider behavior remain source-traced unless otherwise noted. No fixes were implemented.

Labels used below: **B** = defect, **R** = roadmap item, **Q** = open question.

## Findings and required follow-up tests

`parallel_explore` is a blocking fan-out/fan-in tool. Address these issues before adding writable delegation or an async agent registry. Detailed source references and the Codex comparison are retained in this task’s review comments; see Implementation Notes for the proposed split.

- **B1/B4 — Deadlines and pool policy.** Children share five application-wide threads, but the collector starts a fresh timeout for each indexed `get`. Decide whether the setting limits the whole batch from submission or each child from execution start (with a separate bound on waiting). A queued child may still finish before the deadline. Preserve completed results; at expiry cancel unfinished work and distinguish never-started from running. Test both queued outcomes. The non-streaming parent cap is configurable, **300 s by default**; there is no equivalent streaming cap. Choose pool isolation and a cross-tab concurrency limit *after* settling deadline semantics.
- **B2 — Query loss.** The tool advertises 2–5 queries but silently truncates to configured slots (default 3). Put the effective limit in the parameter schema and report skipped queries instead of silently dropping them.
- **B3/B7 — Two kinds of cancellation.** Parent Stop must cancel the batch’s registered futures and unblock the collector. A single-child timeout during model resolution is different: the tracker may not exist yet, and setting the shared parent flag would stop siblings. Give each child a sticky cancellation flag, safely publish its tracker, and honor either parent or child cancellation after setup and before chat. Latch-test a resolver that returns despite interruption: the expired child must not call the model, and a sibling must finish. Clean up runners/futures in `finally`.
- **B5/B11/B12 — Progress and event identity.** Debug-gated events are not a reliable progress feed. Start/finish events currently reflect submission/collection order; some terminal events and child/conversation identifiers are missing. Deliver progress to the owning turn, identify each child, and test two active tabs with debug off. Simply removing the debug gate would create orphan rows.
- **B6 — Failures appear successful.** Return typed per-child outcomes. Derive child events, per-query text, the all-failed `Error:` prefix **and** `ToolExecutionResult.isError` from the same batch result. The trace recorder consumes the structured flag, not the prefix. Exercise all-failed and mixed batches via `executeWithContext`; assert that chat and trace agree, and that mixed results retain the successful answer.
- **B8 — Model resolution.** With no configured default, an empty Ollama model list can yield a model with no name instead of falling back. Reproduce with a model-resolution test seam. Prefer explicit per-slot model → configured default → parent turn’s model, with clear failure and no silent provider probe.
- **B9 — Disabled tools.** `ReadOnlyToolProvider` does not honor `disabledAgentTools`. Fix this **independently of PSI expansion**. Test disabling each of the existing read/list/search tools without hiding enabled siblings.
- **B10 — Unbounded output.** Apply an aggregate cap where the combined report is handed back to the parent. Include headings, errors and a visible truncation notice in the bound. Test both a large single result and multiple smaller ones. “4,000 tokens × children” is not a bound: the Ollama factory does not pass through that output setting. Raw `read_file` content is also uncapped.
- **B13 — A cancelled turn can still launch work.** Stop during non-streaming `prepareMemory()` can precede query/tracker registration. Establish per-turn cancellation state before scheduling preparation and honor it at submission and tracker registration. Latch-test Stop during preparation and assert zero model calls. Separately cover non-streaming wall-clock expiry (which removes the tracker without cancelling it), streaming prompt-task cancellation and project disposal. These are source-traced scenarios, not reproduced failures.
- **B14 — Memory integrity and limits.** An offline probe of the compiled 10-message memory used system + assignment + one assistant message with N tool requests, then N results. At 7, the assignment survives; at 8, it disappears; at 9, only SYSTEM remains; at 10, SYSTEM is followed by an orphan tool result. Preserve the assignment and the active request/result batch together. Define oversized-batch behavior and add regression cases for 7–10 requests in **one** assistant message; sequential calls are not equivalent. Clamp tool budgets at use and revisit the child default of 200 vs. 50 for the parent.
- **B15 — Analytics and docs.** Early model-creation failures miss the child analytics event. The agent-mode docs disagree on whether child agents have PSI tools.

## Roadmap boundaries

- **R1/R2:** Land correctness first. Carry the parent model per turn, reuse its model configuration path, relabel auto-detect, and disclose that inheritance may switch an existing user to a paid provider.
- **R3/R4:** Expand only enabled read-only tools; resolve PSI read-lock and approval issues before adding unbounded searches. Share spec builders and update parent/child prompts alongside the new tools. Keep `semantic_search` out of the default explorer: Chroma collection isolation and authorization to send child queries to the configured embedding endpoint are **separate prerequisites**.
- **R5:** Coordinate scoped, approval-gated writable roles with TASK-225; keep the explorer read-only. Child roles cannot grant tools the parent disabled.
- **R6:** Send the delegated query plus explicit bounded context. Do **not** automatically forward `InvocationContext.userMessage()`: it can include editor/project content and system instructions, and the child may use another provider. If needed, pass `ChatMessageContext.getUserPrompt()` separately. Cap the **entire assembled child input**. Test that a marker present only in the parent editor context never reaches a different-provider child.
- **R7:** Measure usage throughout the run and price children using their own models, separately from the parent.
- **R8/R9:** Defer async lifecycle tools; first deliver correctly scoped queued/running/terminal progress for the existing synchronous tool.

## Relationship to Existing Backlog Tasks

- **TASK-285** (In Progress, not on master): on master `search_files` and `list_files`, which sub-agents already have, hold a read action for the whole walk, so up to five can be in flight at once. Merge before Tier 1 work is cut from master. It does not cover the PSI tools.
- **TASK-288** (In Progress, not on master): `AgentProgressLine.kt` exists only on that branch. It names the main loop's gate as the limitation and put un-gating out of scope; B5 is the smaller half.
- **TASK-225** (To Do, priority high, no dependencies): nothing stops it being built first on the unfixed cancellation, timeout and pool code. Gate its writable delegation on memory integrity, cancellation and truthful outcomes (B3/B6/B7/B13/B14); its planner is independent.
- **TASK-283 / TASK-284** (run view; AC "sub-agent steps nested under the parallel_explore call" is unit-test-only so far), **TASK-279** (trace key mapping used by the executor), **TASK-280** (export leaves sub-agent rows out), **TASK-257** (editable tool descriptions; overrides hide new defaults).
- **TASK-287** (context size for local models): agree whether its output caps apply inside sub-agents and how B10's truncation is signalled.

The Q1–Q5 review answers and proposed follow-up sequence are in Implementation Notes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Second-opinion review validates or challenges each Tier 1 bug fix and quick win (shared deadline, query truncation handling, cancellation race, pool isolation, lifecycle event gating, parent model inheritance, PSI/semantic tool exposure, prompt upgrade)
- [ ] #2 Second-opinion review evaluates Tier 2 proposals (SubAgentRole abstraction with monotonic privilege reduction, parent context injection/forking, token/cost aggregation) and clarifies overlap/sequencing with TASK-225
- [ ] #3 Second-opinion review assesses whether Tier 3 async primitives (spawn_agent, followup_task, wait_agent, interrupt_agent) are justified within LangChain4j/IntelliJ vs. evolving parallel_explore + delegate_task
- [ ] #4 All 5 open questions are answered in Implementation Notes or Final Summary, including PSI concurrency/read-lock safety checks
- [ ] #5 Agreed work is broken down into concrete follow-up implementation tasks
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Created 2026-11-28 following a comparative code review of `../codex/codex-rs` and DevoxxGenie's `ParallelExploreToolExecutor` / `SubAgentRunner` / `ReadOnlyToolProvider`.

### Second-opinion review, 2026-11-28

Eight independent reviewers (one per claim cluster, one of them blind to this task), each followed by a skeptic that tried to refute the verdicts contradicting the task and the new findings rated medium or high. Claims the reviewers confirmed and findings rated low had a single reviewer only; of those, the docs contradiction (B15) and Codex's always-on lifecycle events were re-checked by hand. The description above was rewritten from the surviving findings; the first draft's R5 and R8 proposals are kept, with the review's notes added. No source files were changed and no follow-up tasks were created; acceptance criteria are left unchecked for the task owner to confirm.

**What was wrong in the first draft and is now corrected**
- B3: the described interleaving was impossible and half the proposed fix already existed; tracker publication is one issue; individual timeouts before publication are a separate issue (see revised B3). The "never reset" item was dropped.
- B4: the proposed fix (virtual threads plus per-call semaphore plus JVM bound) was internally contradictory.
- B5: "always publish the events" would produce orphan rows and would not make `AgentProgressLine` show sub-agent progress.
- B1 and B4 were listed as independent; the two affect each other, but queued work need not time out. The follow-up distinguishes deadline semantics from pool choice (B1).
- R2: the "global sub-agent default" step already exists; `InvocationContext` cannot supply the parent model as wired.
- R9: `SpecTaskRunnerService` has no pool or semaphore to share.
- Codex: depth limit and delegation heuristics are v1, not v2; `explorer` is not read-only and has no model of its own; roles have no tool allowlist; `wait_agent` returns no result; history forking strips nothing in its default mode; `FINAL_ANSWER` is plain text; the budget is an enforcement gate, not a cost readout; the worker warning is addressed to the parent.
- Cross-reference "Tier 1 #5" meant bug 5. AC #1 omitted bug 5 and the prompt upgrade.

**Confirmed as written:** B1 and B2 (line references and behaviour), the pool size and scope, the gate at `:184`, tool names and flags for R3, line references for R4, "sub-agent token usage is discarded" (R7), empty sub-agent memory (R6).

**Reviewer claims a skeptic refuted (not carried into the description)**
- "Nothing ties sub-agents to project disposal": non-streaming has a cancellation callback, but it can run before a query is registered; the streaming prompt-task callback misses tracker cancellation (B13).
- "CLI/ACP runners can be chosen as sub-agent provider": already filtered out of both combos.
- "The `InterruptedException` branch is reachable from code mode": no current caller interrupts the thread running `explore()`.
- "`InvocationContext` cannot carry per-turn data": `InvocationParameters` can (R2 carrier decision).

**Defects found outside this task's scope (candidates for their own tasks)**
1. `specTaskPool` is dead code and the user-visible spec "max concurrency" setting is never enforced: `SpecTaskRunnerService` launches every CLI task of a layer at once (`:797-814`).
2. Non-streaming agent/MCP path reports no token usage or cost for the main agent (R7 prerequisite).
3. Five PSI executors hold a whole-search read action; affects the main agent today (Q2).
4. **Moved into scope as B14:** the follow-up probe reproduced both assignment loss and an orphan tool result; see the correctness work below.
5. `ChromaEmbeddingService.init()` race across projects (R3).
6. `READ_ONLY_TOOLS` omits `find_callees`, `trace_call_chains`, `calculate_complexity`, `find_dead_code`.
7. `ThreadPoolManager` is not `Disposable`; its listener connection is unparented.

**Review answers (Q1–Q5)**

- **Q1. One PR or several?** Several; see "Proposed split". Decide B1 timeout semantics before selecting B4 pool policy. B9 ships independently; pair the later R3 tool expansion with R4 prompt changes.
- **Q2. PSI concurrency.** Safe against data races, not for IDE responsiveness. All nine executors use `ReadAccess.compute` (`runReadAction`); five hold it for an unbounded search and none waits for smart mode. Read actions do not block each other but a write (typing) waits for the longest one in flight. This already applies to `search_files` on master. Fix: `ReadAction.nonBlocking(...).inSmartMode(...)` for the single-search tools, one short read action per node for `trace_call_chains` and `find_dead_code`.
- **Q3. Inheritance vs. cost.** Reuse the existing dropdown with "Same as main chat" as the null entry; no new setting. State the multiplier in the help text, lower the 200 tool-call default, and do R7 with or right after R2 so the spend is visible.
- **Q4. Async tools.** Recommendation: not now. The first draft's reason was wrong: between-round steering exists. The real costs are extra round trips and parent tokens, pressure on the global loop counter, and sub-agents outliving the per-prompt tracker that owns cancellation and tracing. The one thing async adds that nothing else does is letting the parent keep working while sub-agents run, which only pays off once writable workers exist. `executeToolsConcurrently` is a cheaper partial alternative but makes all parent tools in a turn concurrent, so it needs a serialising wrapper for write and approval tools.
- **Q5. Context.** Explicit delegated context only by default; never auto-forward the enriched parent message. Bound the whole child input (R6).

**Revised proposed split (draft for AC #5; each fix starts with a regression test)**
1. Memory integrity (B14): preserve valid multi-call exchanges at the 7–10 boundary; define the oversized-batch policy.
2. Cancellation and outcomes (B3/B6/B7/B13): test Stop during preparation, one timeout during model resolution with an unaffected sibling, and all-failed vs. mixed structured outcomes. Add a runner/model-resolution test seam. Gate writable delegation in TASK-225 on these fixes and B14.
3. Deadline and limits (B1/B2/B4/B10/B14): choose batch vs. execution-time semantics before selecting the executor. Exercise queued completion vs. expiry on a real small pool, report skipped queries, clamp settings and bound the aggregate report with a truncation notice.
4. Independently fix disabled-tool enforcement for the existing three tools (B9); do not wait for PSI or semantic search.
5. Per-turn carrier and model inheritance (R2/B8): settings relabel and changelog. Use the carrier for explicit bounded context (R6), not automatic forwarding. Follow with the non-streaming usage fix and per-child cost (R7).
6. Tool expansion (R3/R4/B15): build on TASK-285, resolve PSI read-lock issues, share spec builders, update prompts/docs and account for early-error analytics. Semantic search requires **both** collection isolation and authorization for the embedding endpoint; omit it by default.
7. Sub-agent progress (B5/B11/B12/R9) builds on TASK-288. Coordinate R5 worker roles with TASK-225 after the correctness work. Do not schedule R8 async lifecycle tools yet.

**Follow-up evidence, 2026-11-28:** `./gradlew test --offline --tests '*ParallelExploreToolExecutorTest' --tests '*ReadOnlyToolProviderTest'` passed: 27 + 3 tests, zero failures. The separate offline probe used the compiled memory class and reproduced the B14 states; it was not a live-provider test. Passing existing tests does not verify the proposed fixes. Cancellation interleavings, B12 cross-tab leakage, transport interruption on Bedrock/Azure, and read-lock stall duration remain unverified at runtime. No code changes or follow-up task creation in this review.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-28T10:07:00Z

---

Detailed review 1/8

## Reference Summary: Codex multi-agent patterns (v1 and v2)

Codex has two generations; the first draft attributed several v1 things to "MultiAgentV2".

- **Async lifecycle tools (v2; `core/src/tools/handlers/multi_agents_spec.rs`, `multi_agents_v2/`):** `spawn_agent`, `send_message`, `followup_task`, `wait_agent`, `interrupt_agent`, `list_agents`. v2 has no `close_agent`. `wait_agent` takes `timeout_ms` only, wakes on any agent's mailbox update or on new user input, and **does not return the child's result**: the result is injected into the parent's history as a separate message. A timed-out wait cancels nothing. `send_message` does not start a turn; `followup_task` does.
- **Roles (`core/src/agent/role.rs`; the `agent-roles` crate only parses user TOML roles):** built-ins are `default`, `explorer`, `worker`. A role can set instructions, model and reasoning effort, and can only *disable* five coarse feature switches and skills. There is **no per-tool allowlist/denylist**. Parent authority (approval policy, sandbox, CWD) is re-applied after the role (`child_config.rs`), so a role cannot escalate. It is not monotonic on cost: a role-pinned model overrides the caller's choice.
- **`explorer` is a prompting label only.** Its config file is empty: same tools (including shell and edits), same model, same sandbox as `default`. DevoxxGenie's `ReadOnlyToolProvider` is already stricter. The built-in role list is only shown to the model when user roles exist.
- **Model inheritance (`child_config.rs:249`):** explicit `model` argument, else config `agent_default_subagent_model`, else the parent's model. An unknown model is an explicit error listing the available models; there is no silent auto-detect.
- **History fork (`history.rs`):** `fork_turns` is `all` (the default) or `none`. Two exclusive modes exist. With the prompt-cache prefix preserved (v2 default) the whole parent history is kept, including tool calls and approvals, and a disclaimer is appended (`history.rs:543`). Only when the prefix is not preserved are tool traffic and approvals filtered out.
- **Limits:** the depth limit (default 1) is **v1 only** and ignored by v2, which allows nested spawning. Concurrency: v1 allows 6 threads; v2 allows 4 per session including the root, so 3 children. Slot rollback is `SpawnReservation` in `registry.rs`. Eviction (`residency.rs`) is demand-driven LRU over finished children when a slot is needed, not idle-time based.
- **Completion delivery:** plain text with a `FINAL_ANSWER` header (`context/inter_agent_completion_message.rs`), not a structured schema. Successful results are passed through uncapped; **error text is capped (about 900 tokens) and carries a next-action line**. Capacity overflow is an explicit error naming the limit, never a silent drop.
- **Budget (`control/budget.rs`):** an optional enforcement gate in budget units shared by all threads. It is precedent for a token budget guard, not for a cost readout.
- **Delegation prompting:** the "when to delegate" text at `multi_agents_spec.rs:753-780` is the **v1** `spawn_agent` description and is parent-facing only. Two lines above it (`:750-751`) Codex says not to spawn unless the user explicitly asks, and that requests for thoroughness or investigation do not count. Proactive spawning is enabled only at ultra reasoning effort. The "not alone in the codebase" rule (`role.rs:353-354`) tells the *parent* to say this to workers; it is not in the worker's own prompt.
- **Lifecycle events:** Codex always emits sub-agent started/completed activity, independent of any debug flag.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 2/8

## Current State & Defects in DevoxxGenie

`parallel_explore` is a synchronous, blocking fan-out/fan-in tool. One `ParallelExploreToolExecutor` is built per prompt (`AgentToolProviderFactory.java:63,75`) and registered as a child of the prompt's `AgentLoopTracker`. Tool calls in one assistant turn run sequentially (the project never calls `executeToolsConcurrently`). `SpecTaskRunnerService` is unrelated machinery: it runs OS processes or whole chat prompts and has no pool or semaphore.

### Defects from the first draft, corrected

**B1. Sequential waits can multiply the timeout** (`ParallelExploreToolExecutor.java:112-115`). Each indexed `Future.get(timeoutSeconds)` starts its own wait; the collector can spend up to N × timeout. Submission, execution start and collection are different times.
- Decide what the setting promises **before** changing B4: either a whole-batch deadline from submission, or an execution allowance per child from actual start with a separate bound on queue/batch waiting. A queued child may start and finish in time; queueing alone is not a timeout.
- With a batch deadline, keep completed results and cancel unfinished runners and futures at expiry; distinguish never-started from running children. Test both a queued child that finishes in time and one still queued at expiry with a controlled small pool.
- Account for the remaining non-streaming parent budget. Its wall-clock cap is configurable; 300 s is the fallback/default, not a fixed ceiling (`NonStreamingPromptExecutionService.resolveTimeoutSeconds`). The streaming path has no equivalent wall-clock cap.

**B2. Silent query truncation** (`:86-87`). Confirmed, and the cause was missed: the tool contract contradicts the setting. `BuiltInToolDescriptions.java:156` and the schema text at `BuiltInToolProvider.java:186` both say "2-5 queries", while the effective limit is `subAgentConfigs.size()` when rows exist, else 3 by default (`DevoxxGenieStateService.java:1030-1034`), range 1-10. On a default install a model that follows the description loses queries 4 and 5.
- Fix: put the real limit into the hardcoded parameter description at `:186` (the provider is rebuilt per prompt; the main description is user-overridable, so it cannot carry the number reliably), and append a per-query "skipped (limit N)" status to the result. Codex's precedent is an explicit limit error.
- The review recommends against the first draft's other option, running the excess in batches: it changes the setting from "number of sub-agents" to "concurrency", multiplies wall time against the B1 deadline, and breaks per-index model configs.
- Blank and duplicate queries are not filtered; one query is accepted although the text says 2-5.
- `ParallelExploreToolExecutorTest:189` pins the current behaviour (`doesNotContain("Sub-Agent #3")`).

---

created: 2026-11-28T10:07:00Z

---

Detailed review 3/8

**B3. Parent Stop and individual timeout need distinct cancellation state** (`SubAgentRunner.java:52,77-103,170-174`; `ParallelExploreToolExecutor.java:119-122,152-156`). A parent Stop sets the shared flag, but a single-child timeout calls `runner.cancel()` and interrupts its future without setting that flag. If the child is resolving its model, there is no tracker to cancel yet. A resolver that returns despite interruption can proceed to chat. Making the tracker volatile fixes visibility, not cancellation before assignment.
- Keep the parent flag sticky for the prompt. Give each runner its own sticky cancellation flag, safely publish its tracker and honor either flag after setup and before chat. Cancelling one runner must not cancel siblings.
- Hold model resolution on a latch, time out one child, then release it despite interruption. Assert it never calls chat and a sibling completes. Track runners and futures per call and clean up in `finally` (B7).

**B4. Thread pool** (`ThreadPoolManager.java:39`). Confirmed and understated: the pool is 5 threads application-wide while the settings allow 10 sub-agents, so a single call with 6-10 rows queues behind itself. Two default tabs (3 + 3) also exceed it. A child may still be queued when its wait expires; that is distinct from an execution timeout.
- The proposed fix was contradictory: a per-call `Semaphore(maxParallelism)` does nothing while B2 truncates, and any JVM-wide bound re-creates cross-tab queueing. Virtual threads are not justified for at most 10 blocking tasks and their interaction with read actions is unverified.
- After deciding the B1 deadline contract, evaluate a per-call `AppExecutorUtil.createBoundedApplicationPoolExecutor(name, maxParallelism)` (precedent: `AgentTraceService.java:91`). This avoids contention on the plugin’s shared five-thread sub-agent pool. It also drops the JVM-wide cap (N tabs × up to 10 model calls); accept or mitigate that explicitly.
- Related: `ThreadPoolManager` is not `Disposable`, forces non-daemon threads, and `shutdown()` is only called from a JVM shutdown hook.
- After a timeout, `future.cancel(true)` does release the thread for providers on langchain4j's JDK HTTP client. Not verified for Bedrock and Azure SDK transports or for an agent inside a tool call.

**B5. Lifecycle events gated behind debug logging** (`:184`). The gate exists, but it is deliberate and consistent with every other agent publisher, and TASK-288 put changing it out of scope. **"Just un-gate" is not a safe fix:**
- With debug off no `parallel_explore` parent row exists (the main loop's `TOOL_REQUEST` is gated too), so `ConversationViewModel.kt:601-631` drops the launch event and appends each `STARTED`/`COMPLETED` as separate orphan rows.
- `AgentProgressLine` does not read sub-agent state at all (`AgentProgressLine.kt:50` filters `subAgentId == null`); showing "3 sub-agents (1/3 done)" needs a code change there.
- Completion events are published in index order from the collecting loop, not when each agent finishes, and `STARTED` is published at submit.
- Preferred fix: a separate always-on progress state for sub-agents and leave `ACTIVITY_LOG_MSG` as the debug stream. The alternative (un-gate tool and sub-agent events and filter per event type on the display side) changes what users with debug off see and must include approval events.
- B6, B11 and B12 are prerequisites.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 4/8

### Defects the first draft missed

**B6. Failed sub-agents are reported as successes.** `SubAgentRunner.execute` catches `Exception` and returns a string (`SubAgentRunner.java:128-130`, also `:79`, `:85-86`, `:102`), so the executor publishes `SUB_AGENT_COMPLETED` and the chat shows a green row (`ConversationViewModel.kt:615`). The `ExecutionException` branch (`ParallelExploreToolExecutor.java:129-134`) is reachable only for `java.lang.Error`; the two tests that exercise it mock a throwing runner that production never produces. The combined result always starts with `# Parallel Exploration Results`, so the call is classified `TOOL_RESPONSE` even when every sub-agent failed. Fix: return a typed result (ok / error / cancelled / timed out, plus text) and use it for the event type, the per-query status line and an `Error:` result when all failed. Also set the structured `ToolExecutionResult.isError` flag from the same batch outcome: `executeWithContext()` currently sets only `resultText` (`ParallelExploreToolExecutor.java:64-67`), while `AgentTraceRecorder.java:119-125` records `event.isError()`. Test all-failed and mixed batches via `executeWithContext`: chat and trace must agree, and a mixed batch must retain the successful answer and identify the failed child.

**B7. User cancel does not unblock `parallel_explore`.** `cancel()` (`ParallelExploreToolExecutor.java:152-157`) sets the flag and calls `runner.cancel()` but never cancels the futures, which are locals. `tracker.cancel()` only short-circuits the *next* tool call, after which the model is called again to wrap up. Neither prompt path interrupts the tool thread. Fix: track the futures per call, `cancel(true)` them, and have the collecting loop return once cancelled.

**B8. Auto-detect model resolution is broken** *(derived)*. With both settings blank (the shipped default) `tryDefaultModel()` returns an Ollama model whenever creation does not throw. If Ollama is down or has no models, `getModels()` returns an empty list, the model is built with a **null name**, and every sub-agent fails at request time; the OpenAI branch is reached only when Ollama creation throws. Each sub-agent also probes Ollama (with retries) and raises its own "provider not running" notification. The help text (`AgentSettingsComponent.java:320-321`) promises "tries Ollama, then OpenAI". There is no `SubAgentRunnerTest`.

**B9. `ReadOnlyToolProvider` ignores `disabledAgentTools`** (`:72-78`): a tool the user unticked is still given to every sub-agent. Fix the current three tools independently of PSI expansion: verify disabling each one removes it from the child’s offered tools without hiding enabled siblings. Its three tool specs are hand copies of `BuiltInToolProvider`'s with no parity test.

**B10. No aggregate result cap.** `formatCombinedResults` concatenates queries, answers and error text without a bound. Do not rely on 4,000 configured output tokens × child count: for example, `OllamaChatModelFactory.java:30-48` does not pass that setting to the model. Limit the whole report at the handoff to the parent, including headings, errors and a visible truncation notice. Test an oversized single answer and several individually small results that exceed the cap together. Separately, `read_file` is uncapped in both main and child tools.

**B11. Event shape.** `SUB_AGENT_*` events never set `subAgentId`, so the activity log prints `[1/0] ⬇ Sub-agent started: null` (`AgentMcpLogPanel.java:555,580-587`). Cancelled and interrupted sub-agents publish no terminal event (`ParallelExploreToolExecutor.java:127-128`, `:135-138`), so their rows stay RUNNING.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 5/8

**B12. Activity events carry no conversation id** *(derived)*. `ActivityMessage` has only a project hash; two tabs of one project with runs in flight receive each other's tool and sub-agent events. The trace path already solves this with `AgentTraceBuffer.keyForInvocation`. Sub-agent models also get `MCPListenerService` attached, which publishes their intermediate text with no sub-agent or project attribution.

**B13. A stopped or expired turn can still launch work** *(source-traced)*.
- Non-streaming: Stop during `prepareMemory()` can run before `executeQuery()` has registered a future or tracker. Preparation then returns and submits the query without checking cancellation (`NonStreamingPromptStrategy.java:81-89,143-148`). Establish per-turn cancellation state before scheduling preparation; submission and late tracker registration must honor it. Regression: pause preparation on a latch, Stop, release, and assert zero model calls.
- Non-streaming timeout: `orTimeout` completes the future, but cleanup removes the tracker without cancelling it (`NonStreamingPromptExecutionService.java:104,123-133`). Streaming: prompt-task cancellation (including project close) stops the handler but its completion callback clears the tracker without cancelling it. Cover normal Stop, timeout and disposal separately.

**B14. Tool budgets and chat-memory integrity.** The child defaults to 200 tool calls vs. 50 for the parent; clamp settings at use, not only in UI spinners. One direct `parallel_explore` consumes one parent call.
- **Reproduced:** start with system + assignment in the 10-message `TaskPreservingMessageWindowChatMemory`, append one AI message with N tool requests, then its N results. At **7** the assignment survives; at **8** it disappears; at **9** only SYSTEM remains; at **10** SYSTEM is followed by a tool result without its request. This is a malformed history, not just lost findings. The probe did not test provider rejection.
- Preserve the assignment and the current request/result batch as a unit. Define how an oversized batch is handled without emitting orphan results. Add regression cases for 7–10 requests in **one** assistant message; `SubAgentMemoryTest` covers sequential single requests instead. This correctness fix applies to shared chat memory and precedes writable or async agents.

**B15. Smaller items.** The per-sub-agent `agent` analytics event is intentional (task-209 AC #23), but it is skipped when no model could be created (`SubAgentRunner.java:150`, `tracker == null`), although that criterion says it fires on error too. `docusaurus/docs/features/agent-mode.md:375` says PSI tools are available to sub-agents; `:392` and the code say they are not.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 6/8

## Roadmap

### Tier 1 — correctness and quick wins

**R1. Fix B1-B15** in the order given under "Proposed split" in the notes.

**R2. Inherit the parent conversation's model** (`SubAgentRunner.java:196-246`).
- Today's chain already has three steps: per-index `SubAgentConfig`, then the global default (`subAgentModelProvider`/`subAgentModelName`, "Default sub-agent provider/model"), then `tryDefaultModel()`. Only the third step changes.
- New order: per-index override, global default, **parent model**. Replace the auto-detect with a clear error; both production callers always have a `LanguageModel`. Never call `factory.getModels()` from `SubAgentRunner`.
- Pass the parent's `LanguageModel` object from `ChatMessageContext`, not provider and name strings, and not from global settings (the selection is per tab and can change mid-run). `InvocationContext` carries nothing usable as wired today: its `modelProvider()` is langchain4j's enum and `chatMemoryId()` is the constant `"default"`.
- Build the model through an extracted `ChatModelProvider.initChatModel(LanguageModel)` so sub-agents stop duplicating config code that has already drifted (no Ollama context-window override, no Exo case). `setBaseUrlIfLocal` is pinned by reflection in `LocalProviderRegistryTest:114-126`.
- Relabel "None (Auto-detect)" to "Same as main chat". Existing users on the implicit Ollama default will move to their chat model, which may be paid: changelog note required. Skip inheritance when the parent is a CLI/ACP runner.
- **Carrier decision first:** R2, R6 and R7 all need per-turn data (parent model, memory key, usage accumulator) inside the executor. Choose once between an explicit per-turn object through `createToolProvider` → `BuiltInToolProvider` → executor, or langchain4j `InvocationParameters` added to both `Assistant.chat(...)` methods.

**R3. More read-only tools for sub-agents** (`ReadOnlyToolProvider`).
- Tool names and flags in the first draft are correct. Gate registration in the constructor *and* filter `disabledAgentTools` in `provideTools()`. Never reference the Java-only executors outside the `isJavaAvailable()` guard (issue #1100).
- Use shared static spec builders for both providers plus a parity test, not ten more hand copies.
- **Blocked on read locks (Q2):** `find_symbols`, `find_references`, `find_implementations`, `trace_call_chains` and `find_dead_code` each hold one blocking read action for an unbounded search, the same pattern TASK-285 fixed for `search_files`. Start with `document_symbols`, `find_definition`, `find_callees`, `calculate_complexity`.
- Sub-agents have no approval layer. `READ_ONLY_TOOLS` (`AgentApprovalProvider.java:36-44`) omits the four Java PSI tools, so the main agent prompts for them; decide that first. Rule: sub-agents get only tools that are read-only and have no network or process side effects (so no `fetch_page`, web search, MCP or security scans without an approval design).
- `semantic_search`: `ChromaEmbeddingService.init()` is re-run on every search and reassigns an unsynchronised field on an application service; with two projects open one thread can search the other's collection. Fix collection isolation before exposing it. Independently, `SemanticSearchService.java:103-114` embeds the query, potentially through a remote endpoint (`EmbeddingModelFactory.java:26-40`). Keep `semantic_search` out of the default explorer unless the configured endpoint is explicitly authorized for child queries. A Chroma race fix is not network authorization.
- "Fewer round-trips, lower token usage" is a hypothesis; measure it on a fixed query set.

**R4. Prompt and description upgrade** (`BuiltInToolDescriptions.java:151-156`, `SubAgentRunner.java:40-45` and the user prompt at `:116-117`).
- Parent: concrete, non-overlapping, self-contained queries; do not redo delegated work. Sub-agent: report exact paths, symbols and line numbers; stay in scope. The sub-agent half is our own wording, not Codex's.
- Decide deliberately whether `parallel_explore` stays proactive ("Use this for broad exploration tasks") or becomes opt-in as in Codex; this is also the cheapest answer to Q3.
- Both strings name the three current tools, so R3 and R4 land together. A stored user override hides any new default text.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 7/8

### Tier 2 — architecture

**R5. `SubAgentRole` abstraction with monotonic privilege reduction.** Replace the single hardcoded `SYSTEM_PROMPT` + `ReadOnlyToolProvider` with typed roles:
- **`explorer`** (default for `parallel_explore`): enabled read-only tools + safe PSI; no `semantic_search` by default (see R3), optional faster/cheaper model override.
- **`worker`** (aligns with TASK-225 `delegate_task`): read + scoped write/test tools (`edit_file`, `write_file`, `run_tests`), **always wrapped in the parent's `AgentApprovalProvider`** so a sub-agent can never exceed the parent's permissions. Its system prompt carries explicit file/module ownership and "you are not alone in the codebase; do not revert edits made by others".
- **`reviewer`**: read-only + PSI + `calculate_complexity` / `find_dead_code` / security scan tools (when enabled), tuned for review and verification.
- **Custom project/user roles** (optional phase 2b): discoverable from `.devoxxgenie/agents/*.md` (frontmatter + system prompt, similar to skills) or Settings, with per-role instructions, model override and a tool allowlist/denylist that is strictly a subset of the parent-enabled tools.

Review notes on R5:
- The read-only `explorer`, the `reviewer` role and per-tool allowlists are DevoxxGenie designs; Codex has none of them (see the reference summary). Putting the "not alone" rule in the worker's own prompt is an adaptation; Codex tells the parent to say it.
- Only the `delegate_task` half of TASK-225 overlaps (`plan_task` is independent). Decide whether roles replace TASK-225's three flags or sit on top, and reconcile the tool lists (`run_command` there, `run_tests` here).
- For a writable `worker`: `SubAgentRunner.java:96` hardcodes `ReadOnlyToolProvider`. Approval dialogs would come from pool threads while the parent waits, and the approval wait (capped at 120 s, `AgentApprovalService.java:54`) equals the default sub-agent timeout, so it must be excluded from that timeout.
- The `reviewer`'s security scans spawn external processes, and sub-agents have no approval layer today; that needs the approval question from R3 answered first.

**R6. Parent context.** Send the delegated query plus an explicit, bounded `context` field by default. Do not automatically prepend `InvocationContext.userMessage()`: the rendered message can include editor content, project context or system instructions (`MessageCreationService.java:127-142,154-221`), and a child can use a different provider. If the original question is needed, pass `ChatMessageContext.getUserPrompt()` separately. Cap the **entire assembled child input**, not just the optional field. Test that an editor-only marker in the parent message is absent from a different-provider child's input and that the combined input respects the cap. Do not fork the parent history by default.

**R7. Token usage and cost.**
- Prerequisite bug: the non-streaming agent path shows **no** tokens or cost for the main agent (`Assistant` returns `AiMessage`; `NonStreamingPromptExecutionService.java:259-263,318-320`). Fix that first with `Result<AiMessage>`.
- `Result<T>.tokenUsage()` exists and is the aggregate, but it is lost on timeout, cancel and error, the most expensive runs. Use an `AiServiceListener<AiServiceResponseReceivedEvent>` on the sub-agent builder (as `AgentTraceRecorder.registerForSubAgent` does) feeding an accumulator the parent reads. A per-sub-agent `ChatModelListener` would touch about 40 factory call sites.
- Never add sub-agent tokens into the parent `TokenUsage`: cost is priced with the parent model only (`ChatMessageContext.java:95-98`). Compute cost per sub-agent with its own model's prices and show a separate figure.
- Traces have no usage field for any agent; adding one is a schema change that affects the export.

---

created: 2026-11-28T10:07:00Z

---

Detailed review 8/8

### Tier 3 — async lifecycle tools

**R8. Evaluate async lifecycle primitives (`spawn_agent`, `followup_task`, `wait_agent`, `interrupt_agent`, `list_agents`).**
- **Motivation:** blocking `parallel_explore` makes the parent wait until all sub-agents finish and discards sub-agent memory immediately, so follow-up questions start again.
- **Architecture sketch:**
  - A per-conversation `SubAgentRegistry` (scoped to a chat tab / conversation id).
  - A spawn depth limit (default `maxDepth = 1`), a per-conversation concurrency cap, and try/finally slot cleanup.
  - Each spawned agent gets a short `task_name` (e.g. `auth_explorer`), its own `ChatMemory` kept alive while the conversation is active, and a `CompletableFuture` + mailbox queue (`BlockingQueue<SubAgentResult>`) delivering completion notices to the parent.
  - `followup_task(target, message)` reuses an existing sub-agent's warm `ChatMemory`.
  - `wait_agent(timeout)` blocks on the mailbox and returns as soon as any sub-agent posts a completion or error, or the user cancels.
  - `interrupt_agent(target)` cancels one sub-agent without aborting its siblings or the parent turn.
- **Review recommendation: do not build this now** (reasons under Q4; the owner decides). What the sketch is missing if it is built later:
  - The registry must be its own cancellation root, registered with every new per-prompt `AgentLoopTracker`; today the tracker owns cancellation and tracing and is replaced each prompt.
  - An end-of-turn policy (join with a deadline, or cancel) so no agent crosses a turn boundary in a first version. "Disposed when the prompt stops" and "memory kept alive while the conversation is active" need one agreed lifetime, plus a cap on warm agents (Codex v1 uses `close_agent`, v2 uses LRU eviction).
  - Lifecycle tools must be exempt from, or cheap in, the global loop counter (precedent: `trackedNested`, `AgentLoopTracker.java:125-127`); `wait_agent` must poll the cancellation flag.
  - Completion notices can be delivered between rounds through the existing `SteeringMessageInjector`, so `wait_agent` need not carry the result.
  - `followup_task` assumes a memory that retains the investigation; the 10-message window (B14) does not. Agent names need a charset and a uniqueness check.
  - Borrowed limits: depth 1 is Codex v1; v2 has no depth limit and defaults to 3 concurrent children.
  - Suggested preconditions to revisit: writable long-running workers from TASK-225 exist (the case where the parent has real work to interleave).

**R9. Observability.** Show sub-agents by name and status in the chat and the run view. The status list needs Cancelled as well as Running / Completed / Errored / Timed out, and a Queued state unless `STARTED` is emitted from inside the task. The first draft's "share thread pool and cancellation primitives with `SpecTaskRunnerService`" is dropped: there is nothing to share.

---
<!-- COMMENTS:END -->
