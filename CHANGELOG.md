# Changelog

## v1.9.0 - 2026-07-09

### Added
- feat(debug): add an opt-in **Raw Request/Response viewer** for LLM traffic (Settings → DevoxxGenie → Debug, off by default) — captures the full request and response exchanged with the provider (messages, tool calls, token usage) and renders it in the Activity Log tool window, filterable via "Show Raw Only", with double-click-to-JSON and copy-all for pasting into bug reports. The capture listener hooks into `ChatModelFactory.getListener()`, the single choke point every provider factory already attaches listeners through, so it works across all providers at once. Provider API keys are never in scope (factories set `.apiKey(...)` on the model builder, so the key lives in the HTTP client, not in the serialized `ChatRequest`/`ChatResponse`), and `SecretRedactor` masks Bearer tokens, known key shapes (`sk-`, `sk-ant-`, `AKIA`, `AIza`, `gsk_`) and secret-looking JSON fields as defence in depth. Raw captures stay in the Activity Log tool window and are never appended to the in-chat activity timeline (#1197, #1198)

### Fixed
- fix(customopenai): honour the configured **context window** instead of falling back to the 4096 default — setting a large window (e.g. 262000) had no effect: the conversation footer kept reporting a 4K window and drove the usage bar red. Three defects on the Custom OpenAI path, all fixed: `CustomOpenAIChatModelFactory` cached fully-built `LanguageModel` objects whose `inputMaxTokens`/`inputCost`/`outputCost` are *settings* rather than properties of the `/models` response, so the first probe froze the fresh-install default and nothing invalidated it when settings changed (it now caches only the model ids and rebuilds the settings-derived fields on every `getModels()` call, keeping the network probe a single shot); re-selection after Apply silently switched the user's model, because `LanguageModel` is a Lombok `@Data` value type whose `equals()` covers `inputMaxTokens` and a non-editable `JComboBox` *rejects* a `setSelectedItem()` argument absent from its model (re-selection is now by model name); and the persisted-model restore path synthesised a model with `inputMaxTokens = 0`, hiding the context indicator entirely for providers whose `/models` endpoint need not enumerate the configured model (#1201)

### Documentation
- docs(blog): add "MTPLX: 2x Faster Local LLMs on Apple Silicon, Wired Into DevoxxGenie" — how MTPLX uses a model's built-in multi-token prediction heads for speculative decoding (no draft model, unchanged output distribution) and how to point DevoxxGenie at it through the Custom OpenAI provider (#1200)

### Dependencies
- chore(deps): upgrade Langchain4J 1.17.1 → 1.17.2 (and beta 1.17.1-beta27 → 1.17.2-beta27), AWS SDK 2.46.20 → 2.47.0, Netty 4.2.15.Final → 4.2.16.Final (#1191)

### Contributors
- @stephanj

## v1.8.14 - 2026-07-07

### Added
- feat(settings): block saving LLM settings when an enabled cloud provider has no credential — the Settings UI previously let you enable a cloud provider without entering its API key, persisting a half-configured provider that could never work and only failed later at prompt time. `LLMProvidersConfigurable.apply()` now validates before persisting and raises the standard IntelliJ `ConfigurationException` (dialog shows the error and stays open, nothing is saved), aggregating all violations into one message. Covers the 12 key-field providers (blank/whitespace-only keys), Azure OpenAI (key, endpoint, deployment) and AWS Bedrock (credential for the selected auth mode) (task-253, #1196)

### Fixed
- fix(agent): prevent dangling `tool_calls` from corrupting agent conversations — Langchain4j writes the `AiMessage(tool_calls)` to chat memory *before* tool results, so any throw in between (hallucinated/unavailable tool name, malformed tool arguments, round-trip limit) left an orphaned `tool_calls` tail that made OpenAI-compatible providers (DeepSeek, OpenAI, …) reject every subsequent request in the conversation. Two-layer fix: `ToolErrorRecovery` configures all tool-using `AiServices` builders to return hallucinated-tool-name and bad-argument errors as tool results (letting the model self-correct), and `ChatMemoryManager.prepareMemory()` now self-heals by sanitizing any orphaned tool tail before each new prompt instead of only on explicit cancellation (#1193, #1194)
- fix(providers): gracefully handle a missing API key at prompt submission — submitting with a cloud provider selected but no key configured crashed on the EDT with an unhandled `IllegalArgumentException: apiKey cannot be null or blank`, surfaced as an "IDE error occurred" balloon. A submit-time guard in `ActionButtonsPanelController` now shows a friendly notification pointing to Settings and aborts the submission, backed by a new `LLMProviderService.requiresApiKey()` covering all key-based providers including Bedrock, Grok, Kimi and GLM (task-252, #1195)

### Documentation
- docs(blog): add a growth milestone blog post — 75K JetBrains Marketplace downloads and the active-user growth curve, with dashboard screenshots (#1192)

### Contributors
- @stephanj

## v1.8.13 - 2026-07-03

### Added
- feat(customopenai): configurable **context window** and **input/output cost** for the Custom OpenAI provider (Settings → Large Language Models) — internal/OpenAI-compatible models no longer assume a hardcoded 4096-token window, so the token-usage bar reflects the real window instead of showing a false red "context exceeded" warning, and setting costs (dollars per 1M tokens) makes the estimated cost appear in each AI response bubble; leaving costs at 0 keeps them hidden as before (#1186, #1187)

### Fixed
- fix(agent): respect user-configured agent tool-call limits above 100 — Langchain4j's `ToolService` defaults `maxToolCallingRoundTrips` to 100 and throws when exceeded, so limits raised above 100 (allowed up to 500 since #1163) still died at tool call 100 with "exceeded 100 tool calling round trips". The plugin's own graceful `AgentLoopTracker` limit (which asks the LLM to wrap up instead of throwing) now always fires first: all three tool-using `AiServices` builders (non-streaming, streaming, and sub-agents) override the Langchain4j round-trip limit to the configured max plus a small grace margin, keeping it as a hard backstop against runaway loops (#1188, #1189)

### Contributors
- @stephanj

## v1.8.12 - 2026-07-02

### Added
- feat(thinking): add an opt-in **"Show Thinking"** setting (default off) that renders a reasoning model's chain of thought in a dedicated, theme-aware bubble above the final answer, in both streaming and non-streaming mode. Closes the non-streaming gap by having the AiServices `Assistant` return `AiMessage` instead of `String` (langchain4j drops `AiMessage.thinking()` for `String` returns) and calling `ChatModel.chat(...)` directly on the no-tool path. Applies `returnThinking` across all OpenAI-compatible factories (LMStudio, Jan, GPT4All, Exo, llama.cpp, CustomOpenAI, OpenAI, DeepSeek, DeepInfra, OpenRouter, Groq, Grok, GLM, Kimi, NVIDIA) and Mistral; Anthropic/Gemini/Bedrock are excluded for now (extended thinking there needs a token budget → cost impact) (task-240, #1181)
- feat(tips): add a new rotating tip to the prompt-input tip line

### Fixed
- fix(models): prevent a foreign provider's model being restored after an IDE restart — restarting could restore the **Ollama** provider yet show an **Anthropic** model as selected. The persisted provider/model pair could become cross-provider inconsistent (`processModelNameSelection` persisted the model on programmatic combo events, a transient auto-select of the first provider persisted its models when the async fetch landed, provider switches persisted only the provider, and concurrent fetches had no stale-response guard), and the not-in-list restore path then presented the foreign model under Ollama. Adds a programmatic-update guard, pair-writes, a fetch generation token, and a safer restore fallback (#1184)
- fix(providers): handle null-text AI responses from OpenAI-compatible providers so a final assistant message with `null` content (e.g. reasoning models, or empty replies after tool-call file edits) no longer silently drops in the Compose UI, persists `null` to history, or crashes `ConversationHistoryManager` with `IllegalArgumentException` on reopen — "Provider unavailable: text cannot be null" (#1176, #1182)
- fix(history): persist a conversation to history when a run fails **after** an answer has already streamed — previously an error late in the turn (seen intermittently with the NVIDIA provider in agent + streaming mode) left the correct answer unsaved and absent from conversation history (#1177)
- fix(shutdown): guard thread-pool shutdown against a null `Application` during JVM exit. `ThreadPoolShutdownManager`'s JVM shutdown hook could fire after the IntelliJ `Application` was torn down, so `ThreadPoolManager.getInstance()` NPE'd inside `getApplication().getService(...)` (#1178)

### Dependencies
- chore(deps): bump AWS SDK BOM 2.46.19 → 2.46.20 (#1180)

### Documentation
- docs(blog): add a blog post on the visible LLM thinking feature — the Show Thinking setting surfacing a reasoning model's chain of thought before the final answer, with screenshots of the live thinking block and the settings toggle (#1183)
- docs(web): add a Mailjet newsletter signup section to the landing page and a dedicated welcome page for confirmed subscribers (#1179)
- docs(readme): add NVIDIA to the cloud provider list in the README

### Contributors
- @stephanj

## v1.8.11 - 2026-07-01

### Added
- feat(nvidia): add **NVIDIA NIM** (build.nvidia.com) as a first-class, OpenAI-compatible cloud LLM provider — base URL `https://integrate.api.nvidia.com/v1`, a single free `nvapi` key unlocks 100+ models. `NvidiaChatModelFactory` fetches the full catalogue (~120 models) live from `/v1/models` rather than a hardcoded subset; since NVIDIA's model listing carries no context length, each model is assigned a best-effort context-window heuristic (default 128K). Adds a Settings row with an enable checkbox, API-key field, and a link to `https://build.nvidia.com` (#1171)
- feat(ui): make the model-name dropdown type-to-filter via a new reusable `FilteringComboBox<T>` — typing narrows the list to matching models, essential now that a provider like NVIDIA can list 100+ entries. The combo stays editable but a custom `ComboBoxEditor` guarantees `getSelectedItem()` always returns a `T` (or `null`), never the typed String, so existing `(LanguageModel) getSelectedItem()` call sites keep working unchanged (#1172)
- feat(skills): add **NVIDIA Build Skills** to the "Browse skills online" links on the Skills settings page (#1174)

### Fixed
- fix(customopenai): stop the IDE freezing when the **CustomOpenAI** provider is selected with a slow or unreachable URL. The provider-selection handler was calling `getModels()` on the EDT, doing a blocking `/models` GET (10s connect timeout × 3 retries). Model loading now runs off the EDT on a pooled thread and applies results via `invokeLater`, with a fast-fail probe; the previously-selected model is also correctly restored on restart (#1169)
- fix(providers): only show **ACP Runners** / **CLI Runners** in the LLM provider dropdown when a corresponding tool is actually enabled. Both were declared as `Type.LOCAL`, so `getLocalModelProviders()` added them unconditionally via `fromType(Type.LOCAL)`, bypassing the enabled-tool guards. They're now filtered out of `getLocalModelProviders()` so the guards are the single source of truth (#1173)

### Dependencies
- chore(deps): bump the gradle-dependencies group (17 updates) — Langchain4J 1.17.0 → 1.17.1 (beta27 for the chroma/mcp/web-search/reactor/skills modules) and AWS SDK BOM 2.46.18 → 2.46.19 (#1170)

### Documentation
- docs(blog): announce NVIDIA free model support — one free `nvapi` key → 100+ OpenAI-compatible models, 2-minute setup, the type-to-filter dropdown, and the context-window heuristic (#1175)

### Contributors
- @stephanj

## v1.8.10 - 2026-06-30

### Added
- feat(skills): add a "Browse skills online" section to the Skills settings page with curated, clickable links to where users can find and download `SKILL.md` skills — Anthropic Skills, the agentskills.io open standard, the Claude Code Marketplace and SkillsMP — each paired with a short description, plus a hint to drop a downloaded skill folder into a scanned directory and hit Reload. A lightweight first step toward in-plugin skill discovery (#1168)
- feat(agent): raise the agent's max tool-calls ceiling from 100 to 500 and bump the default from 25 to 50 so discovery + implementation on larger codebases no longer stalls mid-run. The limit was already a soft stop (`AgentLoopTracker` returns a wrap-up message to the LLM rather than aborting); existing users keep their stored spinner value, only the selectable ceiling and the new-install default change (#1163)

### Fixed
- fix(agent): dedupe agent tools by name in `CompositeToolProvider` so an explicitly enabled MCP tool now overrides a built-in tool of the same name instead of crashing. Enabling both the built-in `read_file` and the JetBrains MCP server's `read_file` blew up agent execution with `IllegalConfigurationException: Duplicated definition for tool: read_file`; the merge now keeps first-seen ordering, lets the later (MCP) provider win, and logs the clash at INFO (#1159)
- fix(agent): classify a non-zero `run_command` exit as a tool error so the Activity panel renders a red ✗ instead of a green ✓. The icon is decided by `AgentLoopTracker.isErrorResult()` (results starting with `"Error:"`); a non-zero command returned `"Exit code: N"` and slipped through as success. Non-zero results are now prefixed `"Error: Command exited with code N"`, preserving the exit code for the LLM (#1164)

### Changed
- refactor(models): drop the deprecated OpenAI models (GPT-4, GPT-4o, GPT-4o mini, GPT-4 Turbo, GPT-3.5 Turbo) from the model registry in favour of the newer GPT-4.1 family
- build: pin the Gradle wrapper to 9.6.0 (later bumped to 9.6.1 by Dependabot, see Dependencies)

### Dependencies
- chore(deps): bump the gradle-dependencies group — Langchain4J 1.16.3 → 1.17.0 (beta26 → beta27 for the chroma/mcp/web-search/reactor/skills modules), logback-classic 1.5.36 → 1.5.37, and JUnit 6.1.0 → 6.1.1 (#1162)
- chore(deps): bump logback-classic 1.5.35 → 1.5.36 (#1161)
- chore(deps): bump AWS SDK BOM 2.46.17 → 2.46.18 and the Gradle wrapper 9.6.0 → 9.6.1 (#1166)

### Documentation
- docs(blog): add a DevoxxGenie plugin analytics post (#1167)

### Contributors
- @stephanj

## v1.8.9 - 2026-06-24

### Fixed
- fix(chat): stop the Compose chat from crashing with `NoSuchMethodError: kotlinx.coroutines.Job.cancel$default` after MCP or streamed responses. A Dependabot group bump had pulled `multiplatform-markdown-renderer` up to 0.43.0, which links against a newer kotlinx-coroutines ABI than the IntelliJ platform ships, so syntax-highlighted code blocks blew up at render time. The renderer is now pinned to the `0.38.x` line (0.38.1 — the newest build that still runs on the bundled coroutines), with an explicit Gradle guard so Dependabot can't push it past 0.38.x again (#1158)
- fix(mcp): use the SSE transport for `HTTP_SSE` MCP servers so "Test Connection" and live tool calls work against SSE endpoints such as the JetBrains built-in MCP server (`http://127.0.0.1:64342/sse`). Both the test-connection and runtime paths were using the streamable-HTTP transport against an SSE endpoint, which the server rejected with `Unexpected status code: 405` (#1151)
- fix(cost): guard against null token counts when calculating cost so agent runs no longer abort with `NullPointerException` from `TokenUsage.inputTokenCount()`. Some providers (reported with Ollama + cloud models) return a `TokenUsage` whose input/output counts are null; the cost calculation now treats missing counts as zero instead of unboxing them (#1149)
- fix(ui): replace the deprecated `ComponentPanelBuilder` comment with a `JBLabel` hint, removing a deprecation warning while keeping the same inline help text (#1154)

### Dependencies
- chore(deps): bump the gradle-dependencies group — AWS SDK BOM 2.46.10 → 2.46.17, commonmark 0.28.0 → 0.29.0, logback-classic 1.5.34 → 1.5.35, Kotlin (jvm/lombok/compose) 2.2.20 → 2.4.0, and the Gradle wrapper 9.5.1 → 9.6.0 (#1153)
- chore(deps): bump docusaurus dev dependencies — webpack-dev-server 5.2.4 → 5.2.5 (#1155), launch-editor 2.12.0 → 2.14.1 (#1147), and `ws` (#1152)

### Documentation
- docs(tips): add rotating tips promoting the companion SpotBugs and SonarLint DevoxxGenie plugins, and remove obsolete docs (#1148)

### Contributors
- @stephanj

## v1.8.8 - 2026-06-19

### Fixed
- fix(edit): make the agent's edit tool match multiline `old_string` blocks on Windows files. `EditFileToolExecutor` compared `old_string` against the file's raw on-disk bytes with an exact `indexOf`, but Windows files use CRLF line endings while the LLM emits `old_string` with LF, so any multi-line block (e.g. several imports) could never match and surfaced "The specified old_string was not found". Single-line edits were unaffected, matching the reported symptom. Matching and replacement now normalize line endings to LF, and the file's detected original separator is restored on write (#1144, #1145)

### Changed
- chore(build): repoint the documented JDK 21 path to the available SDKMAN `21-zulu` JDK (the old `azul-21.0.5` path no longer existed, so Gradle silently fell back to JDK 25) and silence the `buildSearchableOptions` shutdown noise that made a passing `clean buildPlugin` look like it was failing (#1146)

### Contributors
- @stephanj

## v1.8.7 - 2026-06-19

### Added
- feat(logs): surface the exact system prompt each chat sends to the model so users can inspect what the LLM actually received. It is published once per conversation (when the system message is first added to memory) under a new `AgentType.SYSTEM_PROMPT`: the inline chat **Activity** panel shows it as `[SYSTEM PROMPT]` with the full, untruncated prompt (collapsed by default, click to expand) and the standalone **DevoxxGenie Logs** panel shows a `📋 System prompt` entry (double-click → open the full prompt in an editor tab). The Logs tool window is now always available, so the prompt is inspectable even in plain chat mode (no agent/MCP/CLI/RAG required) (#1138)

### Fixed
- fix(webserver): replace the embedded `WebServer`'s IntelliJ-bundled Netty implementation with the JDK built-in `com.sun.net.httpserver.HttpServer`, eliminating all 9 Plugin Verifier "internal API" flags (`ByteBuf`, `Unpooled`, `ByteBufHolder`, `readableBytes`, `copiedBuffer`). The public API is preserved exactly, so the only caller (`SpecKanbanPanel`) is unaffected; resource serving, `/health-check`, CORS, content-type detection and 404 handling all carry over (#1141)
- fix(storage): make conversation history safe under concurrent access so closing two chats at almost the same time no longer drops a conversation. `ConversationStorageService.getInstance()` was returning a new instance (and its own JDBC connection) on every call, and the SQLite connection had no busy timeout or WAL mode, so concurrent saves failed instantly with `SQLITE_BUSY` and lost that conversation. It is now a real double-checked singleton, the connection sets `PRAGMA busy_timeout = 5000` and `PRAGMA journal_mode = WAL`, and a shared `ReentrantLock` serializes all mutating operations (`addConversation`, `removeConversation`, `clearAllConversations`, `cleanupOldConversations`) (#1140)
- fix(chat): stop the Compose chat viewport from teleporting while scrolling up. Two distinct bugs caused jumps: every bubble's `MessageEntrance` always placed an `AnimatedVisibility` in each `LazyColumn` item's measurement path, so recycled items reporting a transient zero size reset `firstVisibleItemIndex` to `0`; and an assistant bubble scrolling in at the top edge was first measured short (lazy `Markdown`/syntax rendering) then grew, snapping the viewport to the start of the message. `MessageEntrance` now renders content directly unless a bubble is genuinely playing its one-shot entrance (`shouldPlayEntrance(...)` extracted and unit-tested), finished AI bubbles reserve their measured height via a per-message cache, and auto-follow is guarded against re-pinning the view while the user scrolls (#1139)

### Contributors
- @coradead
- @stephanj

## v1.8.6 - 2026-06-18

### Added
- feat(welcome): add setup nudges that guide new users to configure skills and/or MCP servers. When either capability is unconfigured, a "Get Started" `SetupCard` appears on the Welcome screen with contextual messaging and "Add Skill" / "Add MCP" buttons that open the matching settings panels directly. The `Welcome` state gains a `hasMcpServers` flag (`ConversationViewModel.loadHasMcpServers()`) threaded through to the Compose UI (#1130)

### Fixed
- fix(agent/skills): make tool-provider wrappers honor langchain4j's `executeWithContext` contract so skill-backed tools work. langchain4j 1.16.2 added `ToolExecutor.executeWithContext(...)`, and `AbstractSkillToolExecutor` throws `IllegalStateException("executeWithContext must be called instead")` from the legacy `execute(...)`. Our wrappers (`AgentLoopTracker`, `AgentApprovalProvider`, `ApprovalRequiredToolProvider`, `InstrumentedMcpToolProvider`) only overrode the legacy method, so `activate_skill` and other skill tools surfaced `"Error: executeWithContext must be called instead"`. They now implement both methods and thread the invocation context through the whole wrapper chain (#1135)
- fix(mcp): pass user-entered environment variables to the STDIO connection test in the *Add MCP Server* dialog. The test previously spawned the server process without the env-var table values (e.g. API tokens), so any MCP server needing them to authenticate failed the test and couldn't be created. `TransportPanel` gains a `createClient(headers, env)` method, `StdioTransportPanel` merges the user env on top of the inherited system environment (user values win, mirroring `MCPExecutionService#initStdioClient`), and `MCPServerDialog` captures the env-var table on the EDT and threads it into the test client (#1137)
- fix(mcp-marketplace): page the MCP Marketplace registry instead of loading the entire list up front. The dialog previously fetched every page into memory and filtered client-side, hanging on open as the registry grew to thousands of entries — and the Cancel button did nothing because the fetch loop never polled progress. It now lazily loads the first page with a "Load More" cursor button, moves search server-side (debounced, with a generation counter discarding stale responses), and runs fetches on a pooled thread updating the table via `invokeLater(ModalityState.any())`. Also fixes a latent bug where the search query was sent as `q` instead of the registry's `search` parameter (#1128, #1129)

### Dependencies
- build(deps): bump the gradle-dependencies group with 20 updates — langchain4j `1.16.2` → `1.16.3` (and beta artifacts `1.16.2-beta26` → `1.16.3-beta26`), Kotlin `2.2.20` → `2.4.0`, AWS SDK BOM `2.46.10` → `2.46.12` (#1125, #1133)

### Contributors
- @coradead
- @stephanj

## v1.8.5 - 2026-06-16

### Added
- feat(ui): show how much of the selected model's context window the ongoing conversation occupies. A persistent "Context window: used / max (pct%)" label with a thin colored bar sits near the prompt input (`ActionButtonsPanel`), updating after every completed response and resetting on a new conversation; each AI bubble also appends the running used/max context to its metadata line. The `TokenUsageBar` now colors by fill ratio (green/yellow/red) so it actually warns near overflow (#1126)

### Fixed
- fix(streaming): record `TokenUsage` on the streaming path. Streaming responses (the default) previously reported no token metrics because `onCompleteResponse` never called `setTokenUsageAndCost`; it now derives input/output tokens and cost from the final `ChatResponse`, guarded for local providers that return null usage (#1126)

### Changed
- refactor(ui): clean up the AI response bubble token display. The header now reads `1.8s ~ 10.5K in / 179 out` — input/output are labelled, large counts are abbreviated via a shared K/M/B `formatTokens` helper, the duplicated context-window readout is dropped (it's already shown under the prompt), and time/cost/token decimals render with `Locale.US` so the separator is always a period. The summary logic is extracted into a Compose-free, unit-tested `formatMetadataSummary()` (#1127)

### Documentation
- docs: README updates (7aa5189a)

### Contributors
- @stephanj

## v1.8.4 - 2026-06-15

### Fixed
- fix(cli-runner): refresh the IntelliJ VFS after a CLI Runner (Copilot, Claude, …) finishes so edits made by the external process appear in open editors immediately. CLI Runners write directly to disk while the IDE works against its in-memory snapshot, so changes previously only showed after closing/reopening the file or running "Reload from disk". `CliPromptStrategy` now calls `VfsUtil.markDirtyAndRefresh` on the project root on both success and error exit codes, forcing a re-stat so externally-changed files are actually detected (#1119)
- fix(settings): stop the "Tools > DevoxxGenie" LLM settings panel from overflowing the dialog width and clipping the input-field borders. A long stored API key (e.g. a ~150-char OpenAI `sk-proj-…` key) made an unbounded `JPasswordField`/`JTextField` size its preferred width to the full string, pushing the GridBag column and the whole auto-sized dialog past the screen edge. Fields are now capped to a fixed column count, the input column absorbs width via `weightx`, and hints word-wrap — dropping the form's minimum width from ~728px to ~625px (#1121)
- fix(devoxxgenie-md): DEVOXXGENIE.md generation no longer hangs. The "Tree depth" setting was never wired through, so generation always walked the entire project; depth is now honored with bounded recursion, the task is cancellable, and per-file progress is shown. "Analyzing project structure…" is also dramatically faster — `ProjectAnalyzer` collapsed ~11+ full project-tree walks into one and fixed a `.gitignore` skip that descended into `node_modules`/`.git`/`build` instead of skipping them (#1122)

### Changed
- refactor(quality): apply SonarQube code-quality fixes across LLM providers and services — extract duplicated string literals to constants (S1192), make `ModelVersionComparator` `Serializable`, add exhaustive `default` switch branches, replace a provider switch with an `EnumMap` registry, use pattern-matching `instanceof`, swap null-body checks for `Objects.requireNonNull`, and register `BlogFeedService` as an IntelliJ application service (#1120)
- build: pin the Kotlin and IntelliJ Platform Gradle plugins to known-working versions for reproducible builds (188441d1)

### Documentation
- docs(tips): add an agent debug-log tip to the rotating prompt tips (543089cd)

### Tests
- test(settings): stop the `LLMProvidersConfigurable` tests from building the real Swing panel, which failed under a mocked `Application` that can't resolve the platform `ExperimentalUI` service — fixing two long-standing pre-existing test failures (#1123)

### Dependencies
- build(deps): bump the gradle-dependencies group with 24 updates (#1112)
- build(deps): bump shell-quote 1.8.3 → 1.8.4 in /docusaurus (#1102)

### Contributors
- @stephanj
- @dependabot

## v1.8.3 - 2026-06-15

### Added
- feat(welcome): the welcome screen now leads with **Latest from the Blog** instead of burying it below a long Features scroll. The RSS-fed blog section moved up under the greeting/CFP banner so fresh posts are visible first, and **Explore Features** became a collapsible section (new `CollapsibleSectionHeader` with chevron + item count), collapsed by default so the 14-chip grid no longer dominates the page and Skills/Quick Commands stay reachable (#1118)
- feat(ui): chat output font zoom via **Cmd +/-** (macOS) / **Ctrl +/-** (Windows/Linux) — scales both prose and code font sizes together (clamped 8–24) and persists across IDE restarts via `DevoxxGenieStateService`. The actions are scoped to the DevoxxGenie tool window so the shortcuts pass through to the editor everywhere else (task-237, #1113)

### Fixed
- fix(customopenai): resolve the Custom OpenAI model name and populate its model list. When the "Custom OpenAI Model Name" override was disabled (the default), the factory sent an empty `model` field and servers rejected the request; `resolveModelName()` now falls through explicit override → selected dropdown model → `"default"` so a non-blank value is always sent. `getModels()` was a hardcoded empty list and now queries the server's `/models` endpoint to populate the picker, degrading gracefully to an empty list on a missing URL, unreachable server, or unparseable response (#1116)

### Documentation
- docs: add Algolia Experiences documentation search to the Docusaurus site, with clickable search results (#1114, #1115)
- docs: add the Engram persistent-memory blog post (9869d3c7)

### Contributors
- @stephanj

## v1.8.2 - 2026-06-11

### Added
- feat(ui): live agent activity timeline — `TOOL_REQUEST`/`TOOL_RESPONSE`/`TOOL_ERROR` events are paired into single Activity-section rows with per-row status (pulsing spinner while running, ✓ on success, ✗ on error, ⏸ while awaiting approval), click-to-expand arguments/result, and nested sub-agent rows under `parallel_explore`. Adds an always-on one-line live status in the AI bubble ("Running search_files… (step 4/25)" / "Waiting for your approval…") so a multi-minute agent run is never dead-silent, plus an "Open Logs" link to the Logs tool window. Approval lifecycle events (`APPROVAL_REQUESTED/GRANTED/DENIED`) are now published (task-233, #1109)
- feat(ui): explicit terminal states in chat — abnormal completions now leave durable in-chat feedback instead of failing silently. Stopped responses show a muted "⏹ Stopped by user" footer, errors render a red-tinted card with a one-shot Retry that re-submits the original prompt, and hitting the agent loop limit shows a "Reached max tool calls (N)" notice linking to Settings → Agent Mode (task-234, #1107)
- feat(ui): chat input/output text now scales with IntelliJ's Appearance → Zoom IDE factor and applies live (no tool-window reopen). Compose typography and Markdown heading offsets multiply by the sanitized IDE scale, and the Swing prompt input derives its editor font from the scaled size (task-237, #1106)
- feat(ui): UI transitions and micro-animations — Welcome ↔ Chat crossfade, message-entrance fade-and-slide, and submit-glow polish, with animations disabled in power-save mode and remote-desktop sessions (task-235, #1105)
- feat(ui): long-running feedback polish — Activity rows running over 2s show a live "running… Ns" elapsed ticker, RAG indexing now runs as a determinate, cancellable background task with per-file progress, and conversation-history deletion is undo-deferred with a 5s grace window and an Undo notification action (task-236, #1110)

### Changed
- feat(streaming): batched token updates — `StreamingResponseHandler` now flushes on a 75ms one-shot instead of one EDT post per token (immediate first paint, lossless final flush), fixing EDT flooding and jank with fast providers. Adds a blinking caret while streaming, true tail-follow via a bottom-anchor item, and a floating scroll-to-bottom button when scrolled up mid-stream; removes flickering hover popups in the Agent/MCP log panel (detail stays on double-click) and de-duplicates tool-call rows (task-232, #1104)

### Fixed
- fix(settings): open the settings dialog without blocking the EDT — fixes the intermittent PyCharm `IllegalStateException: This method is forbidden on EDT` crash when opening DevoxxGenie settings. `SettingsDialogUtil` now collects configurable groups on a pooled thread and shows the dialog on the EDT, mirroring the platform's own ShowSettingsAction; the "Open Web search settings" / "Open RAG settings" links route through the same utility (task-238, #1108)

### Dependencies
- chore(deps): bump software.amazon.awssdk:bom 2.46.6 → 2.46.7 (#1111)

### Contributors
- @stephanj

## v1.8.1 - 2026-06-10

### Fixed
- fix(agent): agent mode no longer crashes on non-Java IDEs (PyCharm, WebStorm, GoLand, …) — toggling agent mode threw `NoClassDefFoundError: com/intellij/psi/PsiModifierListOwner` because four PSI agent-tool executors reference Java-plugin PSI types in their signatures, and `BuiltInToolProvider` constructed them unconditionally even though `com.intellij.modules.java` is an optional dependency. The classes failed to link before the existing runtime guard could run. Registration of the four Java-only tools (`find_callees`, `trace_call_chains`, `calculate_complexity`, `find_dead_code`) is now gated on `PsiToolUtils.isJavaAvailable()`; non-Java IDEs keep the five language-agnostic PSI tools and agent mode works again (#1100, #1101)

### Contributors
- @stephanj

## v1.8.0 - 2026-06-10

### Added
- feat(tips): rotating "Tip:" line under the prompt input — shows a hint below the placeholder while the field is empty and rotates to a fresh tip after each submit/clear. Tips are fetched from `genie.devoxx.com/api/tips.json` (same mechanism as `models.json`), so the set of 28 tips can change without a plugin release; selection is weighted-random and never repeats the previous tip. Offline-safe via a 24h persistent cache, schema-version guard, and a hardcoded fallback list kept in parity with the published JSON (#1099)

### Changed
- refactor: replace deprecated future-removal APIs across agent tools, MCP tool providers, PSI executors, the project analyzer, and theme utilities — removes the compiler deprecation warnings with no behavioral change (4c9b5ac)

### Documentation
- docs(tips): add design and implementation plan for the rotating prompt-input tips (#1099)

### Tests
- test(tips): weighted-distribution, never-repeat-previous, and edge-case coverage for TipService; Gson round-trip for TipConfig; and a TipsJsonTest verifying the published tips.json parses and stays in parity with the fallback list (#1099)

### Contributors
- @stephanj

## v1.7.4 - 2026-06-10

### Fixed
- fix(credentials): don't read API keys from the OS keychain at IDE startup — populating the provider combo box previously called `PasswordSafe.get()` for every cloud provider on project open, triggering macOS keychain password dialogs at every startup. Provider availability is now derived solely from the non-secret per-provider enabled flags; API keys are read lazily via `getApiKey()` only when a prompt is executed. Behavior change: an enabled provider with no stored key now appears in the combo box and fails at prompt time instead of being silently hidden (#1097)

### Changed
- chore(models): refresh static models.json from provider sources (#1096)

### Contributors
- @stephanj

## v1.7.3 - 2026-06-09

### Added
- feat(agent): add PSI call-graph navigation agent tools — lets the LLM traverse caller/callee and type hierarchies for more accurate code navigation (task-229, #1093)
- feat(agent): fine-grained enable/disable per PSI tool in Settings, so each PSI navigation tool can be toggled individually (task-229, #1093)

### Fixed
- fix(agent): keep intermediate reasoning visible in streaming chat — agent thinking/tool steps were silently dropped from the chat view (#1095)
- fix(agent): gate tool-activity entries in chat output behind the "show tool activity" setting (#1095)

### Changed
- build: migrate to platform-bundled Compose and raise minimum supported IDE to 2025.3.3 (#1089)
- build: remove duplicate Netty version declaration (#1091)
- refactor(chatmodel): replace provider switch-statement with EnumMap registry for cleaner factory lookup (#1094)
- refactor(agent): extract system-prompt section builders and implementation formatting helper for readability

### Documentation
- docs(task-229): add design spec for fine-grained PSI tool control

### Contributors
- @stephanj

## v1.7.2 - 2026-06-09

### Fixed
- fix(rag): migrate ChromaDB REST client from v1 to v2 API — fixes collection/embedding endpoints for ChromaDB 0.6.x (#1085, #1086)
- fix(rag): convert Windows volume path for WSL Linux Docker daemon
- fix(credentials): skip PasswordSafe in headless mode during `buildSearchableOptions` to eliminate repeated OS keychain prompts; add empty-read cache so non-headless sessions don't re-query the keychain for unset keys

### Dependencies
- chore: upgrade langchain4j 1.15.0 → 1.16.1 (stable) and 1.15.0-beta25 → 1.16.1-beta26 (mcp, chroma, reactor, skills, web-search modules)
- chore: upgrade software.amazon.awssdk:bom 2.44.1 → 2.46.6
- chore: upgrade sqlite-jdbc 3.53.0.0 → 3.53.2.0
- chore: upgrade netty-all 4.2.13.Final → 4.2.15.Final
- chore: upgrade logback-classic 1.5.32 → 1.5.34
- chore: upgrade junit-jupiter 6.1.0-RC1 → 6.1.0 (release)
- chore: upgrade junit-platform 6.0.3 → 6.1.0
- chore: upgrade mockwebserver 5.3.2 → 5.4.0

### Tests
- test(rag): add e2e tests for ChromaDB v2 API and Docker volume path

### Contributors
- @stephanj

## v1.7.1 - 2026-06-08

### Added
- feat(agent): add `web_search` built-in tool to agent mode — the LLM can search the web via the configured search engine when agent mode is on; treated as read-only for approval (task-223, #1079)
- feat(commands): add `/search` slash command for ad-hoc web searches (requires Web Search enabled in settings) (#1079)

### Changed
- feat(commands): remove the obsolete `/tdg` command; removed defaults are pruned and new defaults merged on `loadState` so existing users see the change automatically (#1079)

### Security
- fix: migrate API keys from plaintext `DevoxxGenieSettingsPlugin.xml` to IntelliJ PasswordSafe (OS keychain) — keys are no longer stored in plaintext on disk (#1046)
- API key getters/setters are now `@Transient` so secrets are never re-serialized back into the settings XML

### Fixed
- fix: correct `@Transient` migration bug, add partial-failure guard and thread safety to the credential migration
- fix: address PR #1046 review findings — idempotent retry via startup activity and an all-or-nothing `credentialsMigratedV1` flag
- fix(websearch): correct the Web switch visibility in `SearchOptionsPanel` and fix `isModified()` always returning false for the Web Search enable toggle (#1078)
- fix(openai): correct GPT-5.5 temperature validation (#1074, #1075)

### Contributors
- @stephanj
- @mihaibuba

## v1.7.0 - 2026-05-29

### Added
- feat(rag): expose `semantic_search` as an agent tool, so the LLM retrieves semantically when agent mode is on (task-221)
- feat(rag): query expansion + reciprocal-rank fusion for meta queries (task-222)
- feat(rag): settings UI for query expansion (enable checkbox + variants spinner)
- feat(rag): user-configurable directory exclusion list (task-220)
- feat(rag): auto-reindex tracked files on save (debounced `BulkFileListener`)
- feat(rag): structured retrieval log + "Show RAG Only" filter in the Logs panel
- feat(rag): per-extension splitter selection
- feat(rag): drop low-content chunks at index time
- feat(rag): replace mtime/metadata-filter index check with a content-hash manifest
- feat(rag): add `RagCli` + `ragQuery` Gradle task for offline diagnosis
- feat: add support for IntelliJ 2026.2 EAP (build 262.*)
- Add Claude Opus 4.8 to models.json

### Changed
- refactor(rag): collapse RAG toggles into a single `ragEnabled` switch and polish `/find`
- perf(rag): cache the embedding model and batch embeddings via `embedAll`
- perf(rag): index files in a small bounded thread pool during bulk indexing
- perf(rag): place `SemanticContext` adjacent to the user prompt and skip RAG on short follow-ups
- Drop support for IntelliJ IDEA versions < 2026 and update dependencies (#1065)

### Fixed
- fix: restore IntelliJ 2025.x compatibility
- fix: enable debugging in IntelliJ 2026.1 (#1065)
- fix: stop bundling the platform coroutines jar; restore the plugin build (#1054)
- fix: update to `gemini-3.1-flash-lite` from the deprecated `gemini-3.1-flash-lite-preview` model (#1057)
- fix(prompt): cap agent/MCP wall-clock execution to prevent silent hangs
- fix(rag): embed chunk content (not file paths) and preserve per-chunk results
- fix(rag): unblock the loading indicator during query expansion
- fix(rag): unfreeze ChromaDB Settings actions and clean up the Logs panel
- fix(rag): wrap help text reliably in RAG settings
- fix(ui): switch the on-state color from orange to green for clarity
- fix: update jacoco version (#1065)

### Contributors
- @stephanj
- @mihaibuba

## v1.6.0 - 2026-05-24

### Added
- feat(ap): add Docker Agentic Platform CLI integration (preview)
- Add Gemini 3.5 Flash model to models.json
- feat: Add copy button to user prompt bubble (#1024)
- feat: add shell env file and shell selector for run_command tool

### Fixed
- fix(jan): support Jan v0.8.0 — model list crash and chat hang (#1051)
- fix: surface multi-line run_command output in agent log preview
- fix: support CJK characters in chat input field (#1034)

### Contributors
- @stephanj

## [1.5.0] - 2026-05-18

### Added
- feat: Introduce langchain4j Skills and rename Custom Prompts → Commands (#1040)
  - New `Skills` settings tab listing skills detected under `~/.devoxxgenie/skills/`, `<project>/.devoxxgenie/skills/`, `.claude/skills/` and `.agents/skills/`, with enable/disable, source, open-folder and reload actions
  - `SkillRegistry` project service loads `SKILL.md` definitions; project skills override user skills (with warning)
  - Skills are wired into the agent tool chain via `AgentToolProviderFactory` and the system prompt fragment is appended by `ChatMemoryManager` when agent mode is on
  - Legacy `customPrompts` XML state is migrated to `commands` on load (settings tab renamed from `Custom Prompts` to `Commands`)
- feat(welcome): show active Skills on the welcome page above Quick Commands (#1041)
- feat(settings): replace six per-source `Open folder` buttons with a single button + source dropdown (#1041)
- feat(skills): scan `.claude/skills` and `.agents/skills` alongside `.devoxxgenie/skills` (#1041)

### Fixed
- fix: fire `onCustomPromptsChanged` from `SkillsSettingsConfigurable` on apply so the welcome page refreshes immediately (#1040)

### Docs
- Add `Commands vs Skills` blog post and screenshot (#1040)
- Update Commands rename and add Skills documentation (#1040)
- Fix duplicate footnote markers in `overview.md` (#1040)

### CI
- ci(claude-review): skip job on fork PRs (OIDC unavailable from forks)
- ci(lychee): exclude `intel.com` from link check (403 on automated crawlers)

### Contributors
- @stephanj

## [1.4.5] - 2026-05-18

### Added
- feat: Add copy button to user prompt bubble (#1024) (#1037)
- feat: add shell env file and shell selector for run_command tool (#1036)
- Add task-211: persistent semantic conversation memory backed by ChromaDB (#1028)

### Fixed
- fix: surface multi-line run_command output in agent log preview (#1038)
- fix: support CJK characters in chat input field (#1034) (#1035)
- fix: destroy process tree in AcpTransport.close() to prevent thread leak (#1031)
- fix: revert Compose/Kotlin ecosystem bump that broke the UI (#1030)

### Dependencies

### Contributors
- @app/dependabot
- @knekrasov
- @stephanj

## [1.4.4] - 2026-05-14

### Added
- Persistent semantic conversation memory backed by ChromaDB (task-211) (#1028)

### Fixed
- Revert Compose/Kotlin/Skiko ecosystem bump to known-good versions and align JVM target to 21 (#1030)
- Restore last selected model state on restart (#1021) (#1022)
- Correct Devstral 2 model name (#1019)
- Apply IDE Editor font to prompt input field (#1013)
- Prevent ACP reader thread leak on transport close

### Dependencies
- Bump the gradle-dependencies group with 38 updates (#1023)
- Bump @babel/plugin-transform-modules-systemjs in /docusaurus (#1026)
- Bump fast-uri from 3.0.6 to 3.1.2 in /docusaurus (#1025)
- Bump follow-redirects from 1.15.11 to 1.16.0 in /docusaurus (#1011)

### Contributors
- @app/dependabot
- @flobo3
- @mihaibuba
- @stephanj

## [1.4.3] - $(date +%Y-%m-%d)

### Added
- feat(welcome): blog feed on welcome screen + friendlier analytics copy (#1010)
- feat(analytics): track feature enablement & usage (task-209) (#1008)
- feat(analytics): anonymous LLM provider/model usage analytics (#1005)
- feat: add Exo distributed AI cluster as LLM provider (#1002)

### Fixed
- fix(analytics): harden offline tracking delivery (#1007)

### Dependencies

### Contributors
- @app/dependabot
- @stephanj
## [1.4.2] - $(date +%Y-%m-%d)

### Added
- feat(analytics): track feature enablement & usage (task-209) (#1008)
- feat(analytics): anonymous LLM provider/model usage analytics (#1005)
- feat: add Exo distributed AI cluster as LLM provider (#1002)

### Fixed
- fix(analytics): harden offline tracking delivery (#1007)

### Dependencies

### Contributors
- @app/dependabot
- @stephanj

## [Unreleased]

### Added
- Anonymous LLM provider/model usage analytics (opt-out). Helps guide which providers and models receive engineering investment. The plugin sends a minimal payload — anonymous install ID (UUID), per-launch session ID, plugin version, IDE version, LLM provider name, LLM model name — when you run a prompt or change models. **Never sent**: prompt text, response text, file content, file paths, project names, conversation history, API keys, or anything that could identify you. A first-launch notification asks for consent; you can disable it any time in *Settings → DevoxxGenie → General*.

## [1.4.1]

### Updated
- Upgrade 40 Gradle dependencies including Langchain4J to 1.12.2
- Update LLM models.json with latest provider model names (Anthropic 1M context for 4.6 models, new Grok 4.20 lineup, Groq gpt-oss models, Bedrock Claude 4.6/4.5/4.1 and Llama 4)
- Add ModelConfigJsonTest to validate models.json schema and parsing

### Fixed
- Rollback Kotlin/Compose/Skiko versions to fix Compose UI rendering crash (UnsatisfiedLinkError in Metal renderer, NoSuchMethodError in Kotlin Duration stdlib)
- Fix SafeComposeContainer to call super.addNotify() before adding children so native init errors are caught properly
- Fix Kotlin compilation errors for multiplatform-markdown-renderer API changes
- Fix AnthropicChatModelTest for removed model name constant in Langchain4J 1.12.2

## [1.4.0]

### Added
- Exo distributed AI cluster as new local LLM provider — run large AI models across multiple Apple Silicon devices connected via Thunderbolt (#1002)
- Exo auto-discovery of downloaded models from /state API
- Automatic Exo instance creation with placement preview across cluster
- Background progress bar during Exo model loading with cancellation support
- Auto-recovery when Exo instances disconnect or get recycled
- Collapsible cluster status panel above chat showing nodes, memory, GPU usage, temperature, and active instance status
- Exo Docusaurus documentation page

### Fixed
- MarkdownConversationRenderer missing createConversationJEditorPane method

## [1.3.3]

### Fixed
- Catch NoClassDefFoundError for CompilerTopics in non-Java IDEs (PhpStorm, WebStorm, etc.) (#990)

## [1.3.2]

### Added
- Integrate Spec-to-Code panels into main DevoxxGenie tool window instead of separate sidebar (#987)
- Force Skiko software rendering on Windows via AppLifecycleListener (#986)
- Include archived tasks in backlog duplicate detection (#985)

### Fixed
- File attachment double-click not adding file to window context (#988)
- Remove flaky OpenRouterChatModelFactory tests
- Defensive copy in ProcessedCompletion record constructor
- Refactor Kotlin Compose compiler arg filter to use compilerOptions API

### Changed
- Centralize system prompt construction in ChatMemoryManager

### Documentation
- Update FAQ and troubleshooting for automatic Windows software rendering

## [1.3.1]

### Added
- Bedrock bearer token authentication support (#983)
- 'Force software rendering' setting for Skiko/GPU issues (#981)

### Fixed
- Separate Ollama context window override from discovered metadata (#979) — `num_ctx` is now only sent when explicitly enabled via new "Ollama Request Context Override" setting
- Catch Skiko/Direct3D init errors to prevent chat UI crash (#981)

### Documentation
- Add troubleshooting guide and FAQ entry for GPU rendering issues (#981)
- Add confirmation popup screenshot to Event Automations docs page

## [1.3.0]

### Added
- Event Automations (BETA) — AI agents triggered by IDE events (file open/save, build failure, test failure, commit, process crash)
- Built-in agents: Code Review, Build Fix, Debug, Test Generator, Code Explainer, plus Custom agents
- Event Automation settings panel with enable/disable, custom prompts, and auto-run per mapping
- Template engine for agent prompts with `{{context}}`, `{{content}}`, `{{files}}`, `{{event}}`, `{{timestamp}}`, `{{meta.KEY}}` variables
- Before-commit automation includes full staged diff (before/after file content) for line-level code review
- "More Info" help link in Event Automations settings panel linking to documentation
- Event Automations Docusaurus documentation page with settings screenshot

### Fixed
- Event automations ensure DevoxxGenie tool window is initialized before dispatching prompts (prevents silent prompt loss)
- `TestExecutionListener` uses project-aware `testSuiteFinished(root, project)` overload for correct project in multi-project sessions
- Removed 10 unsupported `IdeEventType` enum values and 3 `AgentType` entries that had no backing listeners
- CLI runner error message readability on test connection failure (#971)

### Testing
- `IdeEventTypeTest` validates enum values match wired listeners to catch future drift
- `EventAutomationSettingsTest` validates default mappings, agent/event type integrity, and boundary checks
- Compose theme state retention test coverage

## [1.2.0]

### Added
- Conversation tabs for parallel model usage across independent chat sessions
- DevoxxGenie Workshop link to welcome page banner

### Fixed
- Tab name not updating when restoring conversation from history (stayed "New Chat")
- Theme-aware syntax highlighting to prevent invisible mark characters in code blocks
- Theme change detection using `StartupUiUtil.isUnderDarcula()` with EDT-safe propagation
- Chat memory initialization before restoring conversation from history
- CLI runner conversations not persisting to conversation history
- Duplicate response text in Claude CLI runner
- ANSI escape codes in CLI test connection errors
- `top_p` parameter omitted for GPT-5 models in OpenAI provider
- Conversation DB moved to durable config directory with legacy migration

## [1.1.0]

### Changed
- Raise minimum IDE version to 2025.1, add support up to 2026.1.* (#972, #963)
- Update Compose Desktop to 1.10.1 and markdown-renderer to 0.39.2
- Bundle Compose runtime for 251+ IDEs and add configurable IDE version property
- Exclude bundled markdown plugin and add it as bundled dependency

### Fixed
- Adapt markdown renderer API to v0.28.0 breaking changes
- Correct markdown renderer exclude group coordinates
- Compose runtime packaging for 251+ IDEs

### Removed
- Unused logo rendering from WelcomeScreen

### Testing
- Mock `LLMModelRegistryService` for deterministic cloud factory tests

## [1.0.0]

### Added
- Native Compose Desktop conversation UI replacing JCEF WebView (#961)
- New Compose UI components: chat bubbles (AI & user), activity section, thinking indicator, copy button, file references section
- Dynamic fonts and markdown rendering in user message bubbles

### Fixed
- Hide loading indicator on successful prompt completion
- Restore correct `shouldIncludeSystemMessage` logic for o1 models
- Filter raw JSON from CLI Runner output — show only human-readable text

### Removed
- JCEF WebView implementation, handlers, templates, and all associated CSS/JS/HTML resources
- Dead code: `WelcomeContentService`, `CodeGeneratorService` (TDG), `NodeProcessorFactory`, `FillerPanel`, `HelpPanel`

### Refactored
- Improve thread safety, error handling, and code clarity across prompt strategies and conversation panels
- Apply Lombok annotations and enforce field modifiers for cleaner Java code

## [0.10.2]

### Fixed
- Agent intermediate reasoning text now reliably appears below "Thinking..." indicator in chat webview with fallback DOM lookup (#958)
- Added info-level logging throughout the activity message flow for easier debugging (#958)

### Testing
- Comprehensive test suite for `WebViewActivityHandler` covering lifecycle, intermediate response display, tool call filtering, MCP messages, and JS content validation (18 tests) (#958)

## [0.10.1]

### Changed
- Unified MCP and Agent activity logging into single `ActivityMessage` system, eliminating duplicated handler logic (#957)

### Fixed
- DevoxxGenie Logs panel now shows full log text with horizontal scrolling instead of truncating tool arguments and results (#957)

## [0.10.0]

### Added
- Show agent intermediate reasoning messages in the chat output panel below "Thinking..." with full markdown rendering
- New "Show tool activity in chat output" setting in Agent Mode settings to optionally display tool call details in the chat panel
- Preserve agent reasoning text when final AI response replaces the loading area
- Show actual LLM text in Agent Activity panel instead of hardcoded "LLM intermediate response" placeholder

### Documentation
- Security Scanning feature blog post
- Security Scanning announcement in README and welcome.json

## [0.9.17]

### Added
- Security Scanning feature — Gitleaks (secret detection), OpenGrep (SAST) and Trivy (SCA/CVE) exposed as LLM agent tools (`run_gitleaks_scan`, `run_opengrep_scan`, `run_trivy_scan`) (#954)
- Dedicated Security Scanning settings panel with per-scanner path browser, Test button and install guidance (#954)
- Security scan findings auto-created as prioritised Backlog.md tasks (gated by "Create Spec Tasks" toggle) (#954)
- Security Scan toolbar action in Spec Browser with live progress indicator (#954)
- Security Scanning documentation page and homepage feature block (#954)

### Fixed
- SonarLint: use explicit assertions instead of implicit no-throw checks (#953)
- Backlog task naming prefix normalisation (#952)
- WebView aggressive repaint after long idle in CEF rendering pipeline (#808)

### Documentation
- Add GEMINI.md with Backlog.md workflow instructions for Gemini AI

### Chore
- Add Lychee link checker CI workflow (#950, #951)

## [0.9.16]

### Fixed
- Conversation history popup now closes after selecting an old chat (#946)
- Remove EDT-blocking sleep calls during conversation restoration (#946)
- Prevent IDE freezes when searching/attaching large files (#942)

### Added
- Prefer value of num_ctx for context length of Ollama models (#936)

## [0.9.15]

### Added
- Externalize cloud model config (names, costs, context windows) to remote models.json with 24h TTL caching (#943)
- Refresh button now works for cloud providers (Anthropic, OpenAI, Google, etc.) with model diff notification (#943)
- Add Gemini 3.1 Pro Preview model

### Fixed
- Prevent IDE freezes when searching/attaching large files (#942)

### Changed
- Token Cost settings table is now read-only and refreshes from remote models.json on open (#944)

## [0.9.14]

### Added
- Compact icon-only toolbar footer: all buttons uniform 28x28 with tooltips, FlowLayout eliminates gaps when buttons are hidden (#941)
- Distinct folder-with-plus icon for Add Project button to differentiate from Add File (#941)
- Toggleable Add File button in footer via Settings > Token Cost & Context Window (#941)
- "More Info" help links added to all settings panels (#940)

### Changed
- Merge Agent and MCP log panels into unified Activity Logs panel (#938)
- Vertically centered Agent/MCP labels in footer right panel (#941)
- Replace unicode escapes with literal characters in AgentMcpLogPanel

## [0.9.13]

### Added
- Add claude-sonnet-4-6 to Anthropic model selection (#932)
- Parallel execution for independent tasks in SDD Spec Browser dependency graph (#927)
- Green parallel Run buttons added to Spec Browser toolbar (#927)
- Individual and bulk archive/unarchive for done tasks in SDD Spec Browser (#933)
- Project statistics/overview panel in SDD Spec Browser (#933)
- Project-wide Definition of Done defaults for SDD backlog (#933)
- Fuzzy search replaces simple text matching in SDD backlog (#933)

### Changed
- Make ChatModelFactoryProvider a final utility class (#934)

### Fixed
- ACP client Set iteration using for-each loop for JsonNode.properties() (#929)
- Replace deprecated JsonNode.fields() with properties() (#927)
- Implicit narrowing conversion in compound assignment (security alert)
- Improve AcpTransportTest assertions and remove unused import (#934)

### Dependencies
- Bump gradle dependencies (#930)

## [0.9.12]

### Added
- ExternalPromptService: new project service for programmatic prompt submission from external components
- JaCoCo code coverage plugin with proper IntelliJ plugin classloading support
- Comprehensive test suite: 2100+ unit tests across services, UI, chatmodel factories, and utilities

### Changed
- ACP Client rewritten with builder pattern, lifecycle management, configurable timeouts, and typed exception hierarchy
- Attached files context now included in prompt history for ACP/CLI runners
- MCP services refactored for testability with dependency injection (MCPExecutionService, MCPListenerService, MCPRegistryService, ApprovalRequiredToolProvider, FilteredMcpToolProvider)
- ACP protocol: JsonRpcMessage fields encapsulated with proper getters/setters, AgentRequestHandler uses typed exceptions
- ActionButtonsPanelController: extract createDefaultLanguageModel into focused helper methods

### Fixed
- Resolve jar duplicate entry for `META-INF/DevoxxGenie.kotlin_module`
- Replace deprecated `Query.forEach` with `findAll` iteration

## [0.9.11]

### Added
- PSI Tools: 5 new IDE-powered code intelligence tools for agent mode — `find_symbols`, `document_symbols`, `find_references`, `find_definition`, `find_implementations` (#921)
- Semantic code navigation: agent can jump to definitions, find usages, and discover implementations using IntelliJ's Program Structure Interface
- PSI tools work across all IDE-supported languages (Java, Kotlin, Python, JS/TS, Go, Rust, etc.)
- New PSI Tools toggle in Agent Settings (enabled by default)

### Documentation
- Add PSI Tools (Code Intelligence) section to Agent Mode documentation with tool descriptions, example prompts, and architecture diagrams

## [0.9.10]

### Added
- ACP Runners: communicate with external agents (Kimi, Gemini CLI, Kilocode, Claude Code, Copilot) via the Agent Communication Protocol (JSON-RPC 2.0 over stdin/stdout) with structured streaming and capability negotiation (#920)
- Conversation history for CLI and ACP runners: prior exchanges are formatted as a text preamble so external tools can recall earlier messages (#920)
- Per-tool enable/disable for MCP servers and agent mode built-in tools (#919)
- New `fetch_page` built-in agent tool for web page fetching (#919)
- Claude and Copilot as new ACP tool types with configurable ACP flags (#920)

### Changed
- Move CLAUDE.md/AGENTS.md and DEVOXXGENIE.md content injection from per-user-message to system prompt, set once per conversation (#920)
- Dedicated CLI/ACP Runners settings page, extracted from Spec Driven Development settings (#920)

### Dependencies
- Bump software.amazon.awssdk:bom (#918)

## [0.9.9]

### Added
- CLI Runners as a chat-mode provider: execute prompts via external CLI tools (Claude Code, GitHub Copilot, Codex, Gemini CLI, Kimi) directly from the chat interface (#916)
- CLI tool execution for spec tasks: run selected or all To Do tasks through CLI runners (Claude, Copilot, Codex, Gemini, Kimi) (#874, #875)
- Kimi CLI runner with `--prompt` flag support and `--quiet` mode for clean output (#875, #916)

### Changed
- Refactor CLI runners to dedicated `com.devoxx.genie.service.cli` package with Command pattern (GoF) (#874, #875)

### Fixed
- Defer `scrollTo` in JCEF WebView to fix auto-scroll after DOM updates (#916)

### Dependencies
- Bump the gradle-dependencies group with 3 updates (#873)

## [0.9.8]

### Added
- Automated test execution: new `run_tests` agent tool that auto-detects build systems (Gradle, Maven, npm, Cargo, Go, Make), runs tests, and returns structured results to the LLM (#863)
- Agent iterates on test failures: system prompt instructs the LLM to run tests after code changes and fix until they pass
- Configurable test execution settings: enable/disable toggle, timeout (default 5 min), custom test command with `{target}` placeholder
- Test output parsing for Gradle, Maven Surefire, and Jest with pass/fail counts and failed test names
- Agent Loop: batch task execution with dependency-aware topological sort, progress tracking, and automatic task advancement (#872)
- Spec task runner service with batch execution UI for running selected or all To Do tasks

### Fixed
- Prevent agent and MCP logs from leaking across projects (#867)
- Defer task advancement until prompt execution completes (#872)
- Downgrade Kotlin to 2.0.21 to match IDE runtime and remove dead activeTaskSpec code
- Prompts settings dialog text overflows viewport

### Documentation
- Add "Automated Test Execution" section to Agent Mode docs with build system table, configuration, and troubleshooting
- Add Agent Loop batch task execution page for SDD
- Add references, documentation, and CLI board sections to SDD docs
- Reorder docs sidebar: Agent Mode after Features Overview, then Spec-driven Development, then Agent Loop
- Add Agent Loop cross-references to Agent Mode and Spec-driven Development pages

## [0.9.7]

### Added
- Spec Driven Development (SDD) feature with Backlog.md integration, Kanban board, and enhanced Spec Browser (#862)
- 17 granular backlog tools replacing manage_spec for full CRUD operations on tasks, documents, and milestones
- Init Backlog button for quick project setup

### Changed
- Remove deprecated o1-mini model from OpenAI registry

### Dependencies
- Bump the gradle-dependencies group across 1 directory with 18 updates (#861)

## [0.9.6]

### Added
- Inline code completion using Fill-in-the-Middle (FIM) models with configurable provider/model settings (#857)

### Changed
- Migrate Gradle IntelliJ Plugin from 1.x to 2.x (#858)

### Documentation
- Update contributing guidelines
- Add CLAUDE.md/AGENTS.md feature to v0.9.5 changelog

## [0.9.5]

### Added
- Add CLAUDE.md and AGENTS.md support: automatically include project instructions in prompts with priority logic (CLAUDE.md first, then AGENTS.md) (#856, #854)
- Improve agent logging and MCP tool provider integration (#855)
- Replace parallelism spinner with dynamic add/remove sub-agent rows for more flexible agent configuration (#855)

### Fixed
- Add null-safety to welcome template rendering with fallback to local content
- Use correct announcement fields (type/message) in welcome.json

### Documentation
- Update Agent Mode docs to reflect add/remove sub-agent UI
- Add Agent Mode and Parallel Sub-Agents sections to homepage and README
- Update MCP documentation and fix GitHub issue template links

## [0.9.4]

### Added
- Agent mode with built-in tool execution: read_file, write_file, edit_file, list_files, search_files, run_command, parallel_explore (#849)
- Parallel sub-agents for concurrent codebase exploration, each configurable with a different LLM provider/model (#850)
- MCP configuration JSON import/export for easy sharing of MCP server setups (#852, #786)

### Fixed
- Clear selection metadata when removing files from context (#847, #783)

### Documentation
- Rename Sub-Agents documentation to Agent Mode

## [0.9.3]

### Added
- Externalize welcome text with remote fetching from `genie.devoxx.com/api/welcome.json` (#845)
- Support for announcements, dynamic feature lists, and social links in welcome screen without plugin release (#845)
- Analytics tracking pixel for welcome view metrics via Cloudflare Worker + GA4 Measurement Protocol (#845)
- Schema versioning for remote welcome content with graceful local fallback (#845)

### Fixed
- Replace deprecated `URL(String)` constructor with `java.net.URI` in `WebViewExternalLinkHandler` (#841)

### Documentation
- Refresh documentation for v0.9.2 and deploy to `genie.devoxx.com` (#842)

## [0.9.2]

### Added
- Add dedicated **Skills** settings dialog for custom user-defined skills
- Add `$ARGUMENT` placeholder support in custom skill templates (injects everything typed after `/skill-name`)

### Changed
- Keep **Prompts** settings focused on system instruction, DEVOXXGENIE.md options, and keyboard shortcuts
- Update welcome screen wording to Skills terminology and highlight MCP Marketplace first

### Fixed
- Show resolved custom skill prompt in the user message bubble instead of raw `/command`
- Migrate legacy HTTP/SSE MCP transport usage to `StreamableHttpMcpTransport` (removes deprecated-for-removal API usage)
- Issue #809: Fix LM Studio API URL compatibility - changed default from `http://localhost:1234/api/v1/` to `http://localhost:1234/v1/` to match OpenAI-compatible chat completions endpoint, while preserving rich metadata endpoint access for context length detection
- Issue #809: Fix LM Studio context length detection so model metadata no longer defaults to 8K when larger context values are available
- Issue #809: Add optional LM Studio fallback context setting in the GUI when model metadata does not expose context length
- Replace deprecated `Project.getBaseDir()` with `ProjectUtil.guessProjectDir()` across all scanner extensions (#838)
- Replace deprecated `NioEventLoopGroup` with `MultiThreadIoEventLoopGroup` + `NioIoHandler` for Netty 4.2.x compatibility (#839)
- Resolve plugin verifier warnings for experimental and deprecated API usage (#840)
- Add Kotlin K2 plugin mode declaration (`supportsKotlinPluginMode`) in plugin.xml, required since IntelliJ 2024.2.1 (#840)

## [0.9.1]

### Added
- Add MCP server marketplace for browsing and installing MCP servers (#835)
- Add location and type filters to the MCP marketplace dialog (#835)
- Add support for custom headers in HTTP MCP transports for authentication (#835)

### Changed
- Refactor MCP integration to remove the core sub-module and use the official `langchain4j-mcp` artifact (#835)
- Remove legacy GitHub MCP and FileSystem MCP quick-action buttons from settings in favor of marketplace-driven setup (#835)

### Fixed
- Issue #791: Escape triple braces in user content to prevent template variable exception (#834)

## [0.9.0]

### Added
- Add Claude Code GitHub Workflow (#827)

### Improved
- Reposition MCP Activity in conversation view: shown between user prompt and AI response in non-streaming mode, at the top of the response in streaming mode
- Filter MCP Activity to only show tool calls and log messages, excluding redundant full AI responses

### Fixed
- Fix image attachments not sent to LLM: images were stripped at multiple points (FileListManager cleared prematurely, multimodal content dropped by chat memory and AiServices text-only interface)
- Issue #698: Fix black screen after idle — WebView sleep/wake recovery with proper resource disposal (#824)
- Issue #804: Pass context window (numCtx) to Ollama to use model's full context instead of 4096 default
- MCP "Test Connection & Fetch Tools" no longer blocks the IDE; runs with cancellable progress dialog and fixes MCP client resource leak on error (#826)
- Fix MCP debug logging: show JSON-RPC traffic in MCP Logs panel (#829)
- Fix welcome text not cleared on first chat message (#825)

### Dependencies
- Bump gradle-dependencies group with 12 updates (#823)
  - dev.langchain4j beta dependencies from 1.10.0-beta18 to 1.11.0-beta19 (web-search-engine-google-custom, web-search-engine-tavily, chroma, mcp, reactor)
  - software.amazon.awssdk:bom from 2.41.0 to 2.41.23
  - org.commonmark:commonmark from 0.27.0 to 0.27.1
  - io.netty:netty-all from 4.2.9.Final to 4.2.10.Final
  - ch.qos.logback:logback-classic from 1.5.23 to 1.5.27
  - org.junit.platform:junit-platform-launcher from 6.0.1 to 6.0.2
  - org.junit.jupiter:junit-jupiter-params from 6.0.1 to 6.0.2
  - org.assertj:assertj-core from 3.27.6 to 3.27.7
  - gradle-wrapper from 8.11.1 to 8.13

## [0.8.0]

### Added
- Update models for Anthropic, OpenAI and Gemini
- Add support for MCP streamable HTTP

### Fixed
- Issue #796: Web search doesn't work (null error)

### Dependencies
- Issue #689: Upgrade to langchain4j 1.10.0(-beta18)

## [0.7.1]

### Added
- Issue #767: MCP approval timeout should be configurable
- Issue #771: Enable streaming responses for Bedrock Anthropic models
- Added Claude Sonnet 4 to the available Amazon Bedrock models

### Fixed
- Issue #806: Plugin 'DevoxxGenie' (version '0.7.0') is not compatible with the current version of the IDE

### Dependencies
- Bump the gradle-dependencies

## [0.7.0]

### Added
- Added Claude Sonnet 3.5 v2 to Amazon Bedrock models (#761)
- Added configuration setting to enable/disable regional inference for Amazon Bedrock models (i.e. prefixing model names with 'us', 'eu', or 'apac') (#759)
- Configure Dependabot for Gradle dependencies (#749)

### Fixed
- Fixed issue where the entire file was added in the context when adding code snippet (#761)
- Fixed fallback for empty submit and newline shortcuts on macOS 15.5 with IntelliJ IDEA 2025.1.3 (#753)
- Fixed duplicated user message issue (#748)

### Dependencies
- Bump the gradle-dependencies group with 22 updates (#765)
  - software.amazon.awssdk:sts from 2.25.6 to 2.32.16
  - software.amazon.awssdk:sso from 2.31.64 to 2.32.16
  - software.amazon.awssdk:ssooidc from 2.31.64 to 2.32.16
  - com.squareup.retrofit2:converter-gson from 2.11.0 to 3.0.0
  - org.xerial:sqlite-jdbc from 3.48.0.0 to 3.50.3.0
  - com.github.docker-java:docker-java from 3.4.0 to 3.5.3
  - com.github.docker-java:docker-java-transport-httpclient5 from 3.4.0 to 3.5.3
  - com.knuddels:jtokkit from 1.0.0 to 1.1.0
  - org.commonmark:commonmark from 0.22.0 to 0.25.1
  - io.netty:netty-all from 4.1.100.Final to 4.2.3.Final
  - ch.qos.logback:logback-classic from 1.4.12 to 1.5.18
  - nl.basjes.gitignore:gitignore-reader from 1.6.0 to 1.12.0
  - org.junit.jupiter:junit-jupiter-api from 5.11.0-M2 to 6.0.0-M2
  - org.junit.jupiter:junit-jupiter-engine from 5.11.0-M2 to 6.0.0-M2
  - org.junit.platform:junit-platform-launcher from 1.11.3 to 1.13.4
  - org.projectlombok:lombok from 1.18.34 to 1.18.38
  - org.junit.jupiter:junit-jupiter-params from 5.10.3 to 5.13.4
  - org.mockito:mockito-core from 5.11.0 to 5.18.0
  - org.mockito:mockito-junit-jupiter from 5.15.2 to 5.18.0
  - org.assertj:assertj-core from 3.26.0 to 3.27.3
  - org.jetbrains.intellij from 1.17.2 to 1.17.4
  - org.gradle.toolchains.foojay-resolver-convention from 0.5.0 to 1.0.0
- Bump brace-expansion from 1.1.11 to 1.1.12 in /docusaurus (#764)
- Bump on-headers and compression in /docusaurus (#752)

### Contributors
- @mydeveloperplanet
- @jaginn
- @ffeifel
- @aivantsov
- @teramawi
- @fchill
- @dependabot[bot]
