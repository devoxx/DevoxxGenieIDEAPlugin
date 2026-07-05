# Spec: Agent Team Orchestration (multi-agent, hybrid local/cloud providers)

Status: **Draft — approved for implementation on branch `claude/devoxxgenie-multi-agent-setup-iahcby`**
Backlog: TASK-241 … TASK-248 (this spec is the umbrella document)
Prior art: the `DockerAgents` POC repo (container-per-agent orchestration) and backlog TASK-225
(planner + writable delegation).

---

## 1. Problem statement

DevoxxGenie today runs **one conversation bound to one LLM provider + model**. Agent mode gives
that single conversation agentic tools (`read_file`, `write_file`, `run_command`, …), and
`parallel_explore` can fan out anonymous **read-only** research sub-agents — but there is:

- no notion of a *named agent* with its own persona, tool policy, and provider/model binding;
- no *orchestration loop* where a coordinator agent delegates work to specialists
  (orchestrator, architect, implementer, reviewer, documentalist);
- no way to run a **hybrid team** — e.g. a local Ollama model coordinating and reviewing while a
  cloud model implements, which is exactly the economics users want (cheap local tokens for
  high-volume roles, premium cloud tokens only where quality is critical).

The sibling `DockerAgents` POC validated the target architecture with containers: per-agent Genie
YAML specs carrying `{provider, model}`, one-shot delegation over HTTP, and a strict
summary-only result contract. This spec ports that architecture **in-process** into the plugin,
reusing the existing sub-agent machinery instead of Docker.

## 2. Goals

1. Users can define an **agent team**: named agents, each with a persona (system instruction), a
   tool allowlist, an optional provider+model override, and budgets (tool calls, timeout).
2. A conversation can run in **Agent Team mode**: the main agent loop acts as *orchestrator* and
   delegates to specialists via a new `delegate_task` tool (single task or parallel fan-out).
3. **Hybrid providers**: each agent resolves its own `ChatModel` through the existing
   `ChatModelFactoryProvider` — any mix of local (Ollama, LMStudio, llama.cpp, Jan, GPT4All,
   CustomOpenAI) and cloud (Anthropic, OpenAI, Gemini, Groq, Bedrock, …) providers runs
   concurrently.
4. Delegations are **observable** (chat progress blocks + activity log), **cancellable** (Stop
   propagates), **budgeted**, and **approval-gated** for writes.
5. Ship **built-in default personas** matching the DockerAgents team so the feature works out of
   the box: `orchestrator`, `architect`, `implementer`, `reviewer` (read-only), `documentalist`
   (fetch-only).

## 3. Non-goals (this iteration)

- No Docker/container execution, no Redis, no HTTP session API. Agents are in-process
  `AiServices` loops on the existing sub-agent thread pool. (A remote DockerAgents backend is a
  phase-4 option behind the same tool seam — TASK-248.)
- No nested delegation: specialists cannot call `delegate_task` (depth 1, enforced structurally).
- No cross-conversation persistent agents; every delegation is one-shot, like DockerAgents.
- No changes to non-agent chat, RAG, MCP, or CLI/ACP runner flows.

## 4. Architecture

### 4.1 Concept mapping (DockerAgents → DevoxxGenie)

| DockerAgents                                | DevoxxGenie equivalent                                        |
|---------------------------------------------|---------------------------------------------------------------|
| `agents/<name>.yml` Genie spec              | `AgentDefinition` POJO persisted in `DevoxxGenieStateService` |
| `models.default.{provider,model}`           | per-agent `modelProvider` + `modelName` via `ChatModelFactoryProvider` |
| container per session                       | `AgentRunner` per delegation on `ThreadPoolManager.getSubAgentPool()` |
| `POST /sessions` + `GET /wait`              | `delegate_task` tool executor (synchronous from the LLM's view) |
| `GET /sessions/wait_all?ids=…` fan-out      | `delegate_task` with a `tasks[]` array (all collected, per-task failures structured) |
| `result.json` `{status, summary, intent}`   | `AgentResult` rendered into the tool result string            |
| YAML `toolsets` → `--allowedTools`          | per-agent allowlist filtering the built-in `ToolProvider`     |
| Redis event stream / SSE                    | `AppTopics.ACTIVITY_LOG_MSG` message bus (existing)           |
| `MAX_SESSION_SECONDS` / `LOCAL_MAX_TURNS`   | per-agent `timeoutSeconds` / `maxToolCalls`                   |
| DELEGATION TRANSPORT addendum + live catalog| orchestrator system-prompt fragment generated from the registry |

### 4.2 Components

```
ui/settings/agent (Agent Team tab)          model/agent/AgentDefinition
        │  edits                                    ▲ reads
        ▼                                           │
DevoxxGenieStateService.agentDefinitions ──► AgentRegistry (service/agent/team/)
                                                    │ resolve(name)
main conversation loop (AiServices)                 ▼
  orchestrator persona + catalog ──tool──► DelegateTaskToolExecutor
                                                    │ 1..N parallel
                                                    ▼
                                       AgentRunner (generalized SubAgentRunner)
                                         • own ChatModel (any provider)
                                         • own MessageWindowChatMemory
                                         • per-agent ToolProvider (allowlist)
                                         •   wrapped in AgentApprovalProvider
                                         •   wrapped in AgentLoopTracker (child)
                                                    │
                                                    ▼
                                             AgentResult{status, summary, …}
```

### 4.3 Data model — `AgentDefinition` (new, `model/agent/`)

```java
public class AgentDefinition {
    private String name;                 // unique, flat token: "reviewer"
    private String description;          // one-liner shown in the orchestrator catalog
    private String instruction;          // persona / system prompt body
    private String modelProvider = "";   // "" = inherit the conversation's provider
    private String modelName = "";
    private List<String> allowedTools = new ArrayList<>(); // concrete tool names
    private boolean readOnly;            // convenience: strips write/run tools at build time
    private Integer maxToolCalls;        // null = SUB_AGENT_MAX_TOOL_CALLS
    private Integer timeoutSeconds;      // null = SUB_AGENT_TIMEOUT_SECONDS
    private Double temperature;          // null = global setting
    private boolean builtIn;             // built-ins are reset-able, not deletable
    private boolean enabled = true;
}
```

Persistence: `List<AgentDefinition> agentDefinitions` on `DevoxxGenieStateService`
(same XML-serialized pattern as `subAgentConfigs` / `customPrompts`). `SubAgentConfig` remains
for `parallel_explore` backwards compatibility (see §9 migration).

**Toolset presets** (UI convenience, mirrors DockerAgents `toolsets`):

| Preset            | Tools                                                            |
|-------------------|------------------------------------------------------------------|
| `filesystem-ro`   | `read_file`, `list_files`, `search_files`                        |
| `filesystem`      | + `write_file`, `edit_file`                                      |
| `shell`           | `run_command`, `run_tests`                                       |
| `fetch`           | `fetch_page`, `web_search`                                       |
| `analysis`        | PSI tools + `semantic_search` (when available)                   |

An agent's effective tool list is the union of its presets minus globally disabled tools, and is
**never broader than the parent conversation's** allowed set.

### 4.4 `AgentRegistry` (new, `service/agent/team/`)

- Application service exposing `List<AgentDefinition> getAll()`, `Optional<AgentDefinition>
  byName(String)`, `String buildCatalogPrompt()` (the markdown table of name/description/model
  used in the orchestrator system fragment — the in-process analog of DockerAgents' live
  `GET /agents` delegation table).
- Seeds built-in defaults on first access (§7). Validates unique names (`^[a-z][a-z0-9-]{1,31}$`).

### 4.5 `AgentRunner` (generalize `service/agent/SubAgentRunner`)

Keep the proven structure — own model, own memory, own tracker, non-streaming `AiServices` —
and parameterize what is currently hardcoded:

- **Persona**: system prompt = `definition.instruction` + the project-context fragments already
  assembled by `ChatMemoryManager.buildAugmentedSystemPrompt()` (PROJECT_ROOT, DEVOXXGENIE.md /
  CLAUDE.md, testing/MCP/RAG fragments as applicable).
- **Model**: `resolveModel()` reads `definition.modelProvider/modelName` (fallback: the
  conversation's active provider/model, *not* Ollama/OpenAI auto-detect). Per-agent
  `temperature` threads through `buildModelConfig()` instead of the global setting.
- **Tools**: a `TeamAgentToolProvider` filters `BuiltInToolProvider` (+ MCP if the agent opts in
  later) down to `definition.allowedTools`, wrapped in `AgentApprovalProvider` (approval dialogs
  labeled with the agent name) then a per-agent `AgentLoopTracker` with
  `definition.maxToolCalls`.
- **Result contract** (mirrors `result.json`): `AgentRunner.execute()` **always** returns an
  `AgentResult` on every exit path — success, tool-budget exhaustion, timeout, cancellation,
  model/creation errors:

```java
public record AgentResult(String agent, String intent, Status status, // OK, ERROR, TIMEOUT, CANCELLED
                          String summary, int toolCalls, long durationMs,
                          String provider, String model) {}
```

- **Local-model resilience** (ported from DockerAgents `runner.py`): if a local OpenAI-compatible
  server rejects a tools-enabled request, surface a readable summary ("model does not support
  tool calling — configure a tool-capable model for agent '<name>'") rather than a stack trace;
  tool/timeout errors become actionable strings, never silent failures.

`SubAgentRunner` becomes a thin wrapper (or is replaced) so `parallel_explore` keeps working
unchanged.

### 4.6 `delegate_task` tool (new, `service/agent/tool/DelegateTaskToolExecutor`)

Registered in `BuiltInToolProvider` **only** for the orchestrating conversation (gated by the
Agent Team toggle — specialists never receive it: depth-1 enforced structurally, matching
DockerAgents' "no self-delegation" rule but at the tool layer instead of the prompt layer).

Tool schema:

```json
{
  "name": "delegate_task",
  "parameters": {
    "tasks": [{
      "agent":  "string  — a name from the agent catalog",
      "task":   "string  — complete, self-contained prompt (children get NO conversation history)",
      "intent": "string  — short lineage label, e.g. 'review changes for #123'"
    }]
  }
}
```

Behavior (ports DockerAgents' handoff guarantees):

1. **Fail fast on unknown agent** — return an error listing available agent names (analog of the
   API's 404-on-unknown-agent) instead of spawning anything.
2. One `AgentRunner` per task on the sub-agent pool; `tasks.length > 1` = parallel fan-out
   bounded by `subAgentParallelism`. The slowest child bounds the call, not the sum.
3. **Structured partial results** — a failed/timed-out/cancelled child becomes an entry with its
   status + readable message; one dead child never fails the batch (`wait_all` semantics).
4. The merged tool result contains, per task, only: agent name, intent, status, provider:model
   label, tool-call count, and the child's `summary`. **Never child transcripts** — the
   summary-only contract is what keeps orchestrator context lean and local-model-viable.
5. Implements `AgentLoopTracker.Cancellable` and registers on the parent tracker: the Stop
   button cancels every running child (same wiring as `parallel_explore`).
6. Publishes `ActivityMessage`s (`SUB_AGENT_STARTED/COMPLETED/ERROR`) with agent name +
   provider:model label for the chat progress UI and the Agent/MCP log panel.

### 4.7 Agent Team mode (orchestration loop)

No new loop is written. When the user enables **Agent Team** for a conversation:

- `AgentToolProviderFactory` adds `delegate_task` to the tool chain and (configurable,
  default ON) **strips direct write/run tools from the orchestrator** so it stays a pure
  coordinator — the structural version of the DockerAgents orchestrator's "STRICT FORBIDDEN
  ACTIONS" prompt block.
- `ChatMemoryManager.buildAugmentedSystemPrompt()` appends an `<AGENT_TEAM_INSTRUCTION>`
  fragment: the coordinator mandate (understand → break down → delegate → track → synthesize;
  one-shot children; self-contained task prompts; read only summaries) + the live catalog from
  `AgentRegistry.buildCatalogPrompt()` + a delegation decision guide.
- The orchestrator runs on the conversation's selected provider/model — which may itself be a
  local model. Lessons from DockerAgents applied: the orchestrator gets a **higher tool-call
  budget** (delegations are cheap round-trips; default `agentMaxToolCalls` is raised for team
  mode), and children are instructed to return terse summaries.
- Streaming, chat memory, token/cost display, and Stop behave exactly as today.

### 4.8 Hybrid provider execution

Already proven by `SubAgentRunner` + thread-safe cached factories in
`ChatModelFactoryProvider`; base URLs for local providers come from settings (existing
`setBaseUrlIfLocal` logic). Requirements carried into `AgentRunner`:

- N agents on N different providers run concurrently on the sub-agent pool.
- Children are **non-streaming** (only the summary matters) so slow local models never degrade
  the main chat; liveness comes from the activity feed.
- Per-agent cost/token usage is attributed per delegation and surfaced in the result block.

## 5. UI

1. **Settings → Agent → "Agent Team" tab** (`ui/settings/agent/`): list of definitions with
   Add/Copy/Edit/Delete (built-ins: Edit/Reset only). Editor fields: name, description, persona
   textarea, provider combo + model combo (reuse the `AgentConfigRow` pattern), toolset preset
   checkboxes + resolved tool list preview, budgets, temperature, enabled. Import/Export Genie
   YAML buttons land in TASK-247.
2. **Conversation toggle**: an "Agent Team" switch next to the existing agent-mode control
   (enabled only when agent mode is on and ≥1 non-orchestrator agent is enabled).
3. **Chat progress rendering**: delegation events render as a collapsible block per child —
   `🤖 reviewer (Ollama · qwen3.6) — running… → ✓ done (first summary line)` — fed by the
   existing `ACTIVITY_LOG_MSG` subscription in `ConversationPanel` → Compose view. The full
   summary appears in the orchestrator's final answer; the block is progress, not transcript.
4. **Approval dialogs** from writable children are labeled with the agent name and serialized
   through the existing `AgentApprovalService` queue (N parallel children must not stack modal
   dialogs).

## 6. Safety & limits

- Writes/commands from any child remain gated by `AgentApprovalProvider`
  (auto-approve-read-only honored per existing setting).
- Child tool allowlists are clamped to the parent conversation's effective toolset (the
  in-process analog of DockerAgents' `PROFILE_CAPS` clamping of UI-writable specs).
- Depth-1 delegation only; `delegate_task` is never in a child's tool list.
- Per-child budgets: `maxToolCalls` (default `SUB_AGENT_MAX_TOOL_CALLS`=200) and
  `timeoutSeconds` (default `SUB_AGENT_TIMEOUT_SECONDS`, raised for implementer-class agents).
- Cancellation: parent tracker → `DelegateTaskToolExecutor.cancel()` → every child runner;
  each child memory is repaired with `sanitizeOrphanedToolMessages` semantics on abort.
- All delegation work runs on the sub-agent pool — never the EDT; only message-bus publishes
  touch UI topics.

## 7. Built-in default personas (seeded, user-editable)

Ported/condensed from `DockerAgents/agents/*.yml`; provider defaults chosen for the hybrid
story ("" = inherit conversation model):

| name           | tools                          | provider default        | persona core |
|----------------|--------------------------------|-------------------------|--------------|
| `orchestrator` | *(none — coordinator persona only; used as the team-mode system fragment, not a spawnable agent)* | conversation model | pure coordinator mandate, decision guide, synthesis rules |
| `architect`    | filesystem-ro, analysis        | inherit                 | design, ADRs, implementation plans; never writes code |
| `implementer`  | filesystem, shell              | inherit                 | writes/fixes code + tests; follows project conventions; terse result summary |
| `reviewer`     | filesystem-ro, analysis        | first enabled local provider, else inherit | structured review: bugs/style/security/tests; blocking vs suggestion |
| `documentalist`| fetch                          | first enabled local provider, else inherit | web research; returns cited, condensed summaries |

## 8. Delivery plan (backlog tasks)

| Task     | Slice | Depends on |
|----------|-------|------------|
| TASK-241 | `AgentDefinition` + `AgentRegistry` + persistence + built-in seeds + unit tests | — |
| TASK-242 | `AgentRunner` generalization + per-agent tool provider + `AgentResult` contract; `parallel_explore` regression-safe | 241 |
| TASK-243 | `delegate_task` executor: validation, fan-out, structured results, cancellation, activity events | 242 |
| TASK-244 | Agent Team mode: toggle, orchestrator system fragment + live catalog, tool-chain gating | 243 |
| TASK-245 | Settings UI: Agent Team tab (list + editor) | 241 |
| TASK-246 | Chat UI: delegation progress blocks + per-agent labels in log panel | 243 |
| TASK-247 | Genie YAML import/export (`agents/*.yml` interop with DockerAgents) | 241, 245 |
| TASK-248 | Optional remote backend: `delegate_task` targeting a DockerAgents `orchestrator-api` | 244 |
| TASK-249 | "Agent Team" pseudo-provider in the LLM dropdown: agents as selectable models — orchestrator = team mode, specialist = chat directly as that agent (persona + scoped tools) | 244, 245 |
| TASK-250 | Per-agent execution target (IN_PROCESS / DOCKER_AGENTS): mixed fan-outs, remote-side failures as structured per-task errors; replaces the global remote toggle | 248 |
| TASK-251 | Phase B isolation: LOCAL_CONTAINER target — docker-java-spawned container with the project bind-mounted (ro/rw per agent), workspace-preserving semantics | 250 |

Phases: **1** = 241+242 (foundations, no behavior change) → **2** = 243 (delegation) →
**3** = 244+245+246 (orchestration + UX) → **4** = 247+248 (interop, optional).

## 9. Migration & compatibility

- `parallel_explore` keeps its exact behavior and settings; existing `List<SubAgentConfig>`
  rows are migrated on first run into read-only explorer `AgentDefinition`s only if the user
  opens the Agent Team tab (no silent behavior change).
- Everything is additive and gated behind the Agent Team toggle (default OFF).
- No plugin.xml permission changes; no new dependencies (langchain4j 1.14.0 suffices).

## 10. Testing

- Unit: registry validation/seeding; allowlist clamping; `AgentResult` on every exit path
  (success, unknown agent, budget exhaustion, timeout, cancel, model-creation failure);
  fan-out partial-failure batching; depth-1 enforcement.
- Integration (`*IT` / platform tests): `delegate_task` end-to-end with a fake `ChatModel`;
  team-mode system-prompt assembly; settings round-trip persistence.
- Manual smoke (mirrors DockerAgents' smoke ladder): single delegation → parallel fan-out →
  hybrid (local reviewer + cloud implementer) → cancellation mid-fan-out → approval flow from
  a writable child.

## 11. Risks / watch-outs

- **Local-model tool-calling fragility** — mitigated by readable error summaries and per-agent
  model choice; recommend tool-capable local models in docs.
- **Concurrent approval dialogs** — serialized queue, agent-labeled.
- **Context bloat in the orchestrator** — summary-only contract; children get self-contained
  prompts, never history.
- **Global-settings leakage** — per-agent params must thread through `buildModelConfig`, never
  read `DevoxxGenieStateService` inside child loops (pattern already established).
- **EDT violations** — all child work on the sub-agent pool; UI only via message bus.
