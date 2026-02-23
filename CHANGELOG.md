so # Changelog

All notable changes to this project will be documented in this file.

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
