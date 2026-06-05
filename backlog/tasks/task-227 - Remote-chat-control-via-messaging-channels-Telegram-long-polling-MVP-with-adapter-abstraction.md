---
id: TASK-227
title: Remote chat control via messaging channels — Telegram long-polling MVP with adapter abstraction
status: To Do
assignee: []
created_date: '2026-06-05 10:29'
updated_date: '2026-06-05 10:29'
labels:
  - agent-mode
  - messaging
  - channels
  - feature
  - hermes
  - security
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/ExternalPromptService.java
  - src/main/java/com/devoxx/genie/ui/topic/AppTopics.java
  - src/main/java/com/devoxx/genie/ui/listener/ConversationEventListener.java
  - src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - src/main/java/com/devoxx/genie/ui/window/ConversationTabRegistry.java
  - src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - 'https://github.com/NousResearch/hermes-agent'
  - 'https://hermes-agent.nousresearch.com/docs/user-guide/messaging/'
  - 'https://core.telegram.org/bots/api#getupdates'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Let a user **drive the DevoxxGenie chat from another device** (e.g. their phone) over a messaging channel. You send a message from Telegram, it runs as a prompt in the IDE's AI chat, and the response is delivered back to your phone. This mirrors the **Messaging Gateway** box of Nous Research's Hermes Agent ("chat with Hermes from Telegram, Discord, Slack, …"), scoped realistically to an IntelliJ plugin.

This task delivers the **MVP**: a single **Telegram** adapter using **long-polling**, behind a `ChannelAdapter` abstraction so additional platforms can be added later (see follow-up task-228), with the **security model built in from day one** (this is non-negotiable — see below).

### Why this is feasible (from codebase exploration)

DevoxxGenie already exposes clean in/out seams (an external plugin, SonarLint, already injects prompts via reflection):

- **Inbound (drive the chat):** `ExternalPromptService.setPromptText(String)` injects a prompt into the active tab and routes it to execution; alternatively publish `AppTopics.PROMPT_SUBMISSION_TOPIC` → `onPromptSubmitted(project, prompt, tabId)` on the IntelliJ MessageBus.
- **Outbound (capture the reply):** subscribe to `AppTopics.CONVERSATION_TOPIC` → `onNewConversation(ChatMessageContext)`; the final reply is `ChatMessageContext.getAiMessage().text()`. Streaming tokens are available via `StreamingResponseHandler` if live updates are wanted.
- **Routing:** `ConversationTabRegistry` + the optional `tabId` parameter already support targeting a specific conversation tab; chat memory is keyed by `projectHash-tabId`.

The bridge is essentially: *inbound message → `invokeLater` → publish to `PROMPT_SUBMISSION_TOPIC` → await `CONVERSATION_TOPIC` → send the text back out.*

### The central architectural constraint

DevoxxGenie is a **plugin, not a daemon**: it only runs while the IDE is open, on a dev machine that is behind NAT/firewall with no public IP. This dictates the connection style — the plugin must make **outbound** connections only:

- **Telegram Bot API long-polling (`getUpdates`)** needs only a bot token, makes outbound calls only, and works behind any firewall → **chosen for the MVP**.
- Webhook-based channels (e.g. WhatsApp Cloud API, Slack Events API) require a public endpoint and are explicitly **out of scope** here.

Headline use case this enables: *kick off a long agent task at your desk, walk away, and receive the result + send follow-ups from your phone.*

## Security model (REQUIRED in this task — not a follow-up)

A messaging bridge turns a chat app into **remote control of an agent that can run shell commands and edit files** on the developer's machine (agent mode has `run_command` / `write_file` / `edit_file`). The following are mandatory and must ship with the MVP:

1. **Deny-by-default allowlist** of Telegram user IDs (mirrors Hermes' default: deny anyone not allowlisted). Empty allowlist ⇒ the bot replies to no one. Messages from non-allowlisted IDs are ignored/refused and logged.
2. **Bot token stored in IntelliJ `PasswordSafe`** (CredentialAttributes), NOT in the plaintext `DevoxxGenieSettingsPlugin.xml` state. (Codebase currently serializes secrets as plaintext XML — acceptable for a local API key, NOT for a remote-access credential.)
3. **Remote capability gating.** Remotely-submitted prompts default to a restricted mode (no agent writes / no shell). An explicit, separate opt-in is required to allow remote-triggered writes; when enabled, remote-triggered write/run tools must still pass through `AgentApprovalProvider` so the developer approves at the IDE. The default must be safe.
4. **Off by default**, clearly labelled, with a visible "remote control active" indicator in the IDE.
5. **Project disambiguation.** When multiple IDE windows/projects are open, the adapter must target a single explicitly-bound project (a `/project` selector command + a stored binding); never broadcast a remote prompt to all projects.

## Implementation Plan

### 1. ChannelAdapter abstraction

- `ChannelAdapter` interface: lifecycle (`start`/`stop`/`pause`/`resume`), inbound callback (delivers `(senderId, text)`), and `sendReply(chatId, text)`. Keep it platform-agnostic so task-228 can add Slack/Discord without touching the core.
- `ChannelGatewayService` (application- or project-level service) that owns the active adapters, the allowlist, the project binding, and the inbound→submit / response→outbound wiring. Reuse the EDT-marshalling + submit path already used by `ExternalPromptService` / `EventAutomationService`.

### 2. Telegram adapter (long-polling)

- `TelegramChannelAdapter` runs a background long-poll loop calling Telegram `getUpdates` (outbound HTTPS; reuse the project's existing HTTP client stack). No inbound server, no public endpoint.
- On an allowlisted message: marshal to EDT via `ApplicationManager.getApplication().invokeLater(...)`, publish to `PROMPT_SUBMISSION_TOPIC` for the bound project/tab.
- Subscribe to `CONVERSATION_TOPIC`; on completion, format `getAiMessage().text()` (handle Telegram's 4096-char limit by chunking; render code blocks reasonably) and `sendMessage` back to the originating chat.
- A circuit breaker (à la Hermes) auto-pauses the adapter after repeated upstream failures and surfaces an IDE notification; resumable from settings or a command.

### 3. Slash-style control commands (in-channel)

- Minimal admin commands over the channel: `/project list|use <name>` (bind target project), `/status`, `/pause`, `/resume`, `/stop`. Only allowlisted admins may use them.

### 4. Settings UI

- New "Messaging Channels" settings section: enable toggle (default off), Telegram bot token field (persisted via PasswordSafe), allowlist editor (user IDs), the "allow remote writes/shell" opt-in (default off, with a clear warning), and the bound-project selector. Live status (running/paused/circuit-broken) + last-error display.

### 5. Tests

- Unit test the inbound path: an allowlisted message results in a `PROMPT_SUBMISSION_TOPIC` publish on the EDT for the bound project; a non-allowlisted message does NOT (and is logged/refused).
- Test the outbound path: a `CONVERSATION_TOPIC` completion produces a correctly chunked reply sent via a mocked Telegram client.
- Test capability gating: with remote-writes off, remote prompts cannot trigger write/shell tools; with it on, such tools still route through `AgentApprovalProvider`.
- Test PasswordSafe round-trip for the token (token never written to the XML state).
- Test the long-poll loop offset handling and circuit-breaker pause/resume (Telegram client mocked); graceful disposal cancels the loop with no leaked threads.
- Verify feature-off default: no network activity, no adapter started.

## Out of scope (covered elsewhere / future)

- Additional channel adapters (Slack Socket Mode, Discord gateway) → **task-228** (depends on this task's `ChannelAdapter` abstraction).
- Webhook-based channels (WhatsApp Cloud API, Slack Events API) and any companion relay server for always-on / public-endpoint delivery → future task; explicitly not in the MVP.
- True always-on operation while the IDE is closed (daemon behaviour) — document the "only runs while the IDE is open" limitation, same constraint as task-226.
- Voice transcription / media attachments / cron delivery (Hermes extras).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `ChannelAdapter` abstraction and a `ChannelGatewayService` exist; the gateway wires inbound messages to `PROMPT_SUBMISSION_TOPIC` and outbound replies from `CONVERSATION_TOPIC`, reusing the existing EDT-marshalling submit path
- [ ] #2 A `TelegramChannelAdapter` connects via Bot API long-polling (`getUpdates`, outbound only — no inbound server / public endpoint) and works behind a NAT/firewall
- [ ] #3 An allowlisted Telegram message runs as a prompt in the bound project's chat and the response is delivered back to the originating Telegram chat (chunked to respect the 4096-char limit)
- [ ] #4 Authorization is deny-by-default: messages from user IDs not in the configured allowlist are refused and logged; an empty allowlist replies to no one
- [ ] #5 The Telegram bot token is stored via IntelliJ `PasswordSafe` and never written to `DevoxxGenieSettingsPlugin.xml`
- [ ] #6 Remote-submitted prompts default to a restricted mode (no agent writes/shell); enabling remote writes is a separate explicit opt-in, and remote-triggered write/run tools still pass through `AgentApprovalProvider`
- [ ] #7 When multiple projects are open, a remote message targets a single explicitly-bound project (via a `/project` selector + stored binding), never all projects
- [ ] #8 In-channel admin commands (`/project`, `/status`, `/pause`, `/resume`, `/stop`) work and are restricted to allowlisted admins; a circuit breaker auto-pauses the adapter on repeated upstream failures with an IDE notification
- [ ] #9 A "Messaging Channels" settings section provides enable toggle (default off), token (PasswordSafe), allowlist editor, the remote-writes opt-in with a warning, the bound-project selector, and live status
- [ ] #10 Unit tests cover allowlist enforcement, inbound→submit and response→outbound paths, capability gating, PasswordSafe token round-trip, and circuit-breaker/disposal (no leaked threads); with the feature off there is no network activity. Plugin builds (`./gradlew buildPlugin`) and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Originated from research comparing DevoxxGenie's agent mode to Nous Research's Hermes Agent (https://github.com/NousResearch/hermes-agent), whose Messaging Gateway is a single background process hosting many platform adapters with deny-by-default allowlist auth, per-chat sessions, a `/platform` control command, and per-platform circuit breakers (https://hermes-agent.nousresearch.com/docs/user-guide/messaging/).

Key architectural finding: DevoxxGenie is a plugin, not a daemon, behind NAT — so adapters must be outbound-only. Telegram long-polling (`getUpdates`) is the only widely-used channel that is both firewall-friendly and trivial to set up, hence the MVP. The in/out seams already exist and are already used by a third party (SonarLint injects prompts via `ExternalPromptService`): inbound `ExternalPromptService.setPromptText` / `AppTopics.PROMPT_SUBMISSION_TOPIC`, outbound `AppTopics.CONVERSATION_TOPIC` (`ChatMessageContext.getAiMessage().text()`), tab routing via `ConversationTabRegistry` + `tabId`. Reuse the `EventAutomationService` submit/EDT pattern.

Security is the hard part, not the bridge: remote control of an agent with `run_command`/`write_file` is a real RCE surface, so allowlist + PasswordSafe token + remote-capability gating + safe defaults are mandatory in the MVP (not deferred). Per CLAUDE.md: feature branch before code changes; keep the poll loop off the EDT and submission marshalled onto it; behavioural tests first where practical. Document the "only runs while the IDE is open" limitation (shared with task-226).
<!-- SECTION:NOTES:END -->
