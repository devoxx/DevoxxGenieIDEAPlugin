---
id: TASK-228
title: Additional messaging channel adapters — Slack (Socket Mode) and Discord (gateway)
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
dependencies:
  - task-227
references:
  - src/main/java/com/devoxx/genie/service/automation/EventAutomationService.java
  - src/main/java/com/devoxx/genie/ui/topic/AppTopics.java
  - src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  - 'https://github.com/NousResearch/hermes-agent'
  - 'https://hermes-agent.nousresearch.com/docs/user-guide/messaging/'
  - 'https://api.slack.com/apis/socket-mode'
  - 'https://discord.com/developers/docs/topics/gateway'
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add **Slack** and **Discord** channel adapters on top of the `ChannelAdapter` abstraction delivered in **task-227** (Telegram MVP). This extends remote chat control to two more popular team platforms while reusing all of the MVP's core wiring (inbound → `PROMPT_SUBMISSION_TOPIC`, outbound ← `CONVERSATION_TOPIC`, allowlist, PasswordSafe credentials, remote-capability gating, project binding, circuit breaker).

**This task depends on task-227** and must not start until the `ChannelAdapter` abstraction and `ChannelGatewayService` exist. If implementing this reveals leaks in that abstraction, fix the abstraction in task-227's code rather than special-casing here.

### Why Slack Socket Mode and Discord gateway (and not webhooks)

Same constraint as the MVP: DevoxxGenie is a plugin behind NAT with no public IP, so adapters must connect **outbound only**.

- **Slack — Socket Mode**: an outbound WebSocket from the app to Slack; no public webhook endpoint required. Firewall-friendly. (Slack's Events API webhook variant requires a public URL and is therefore out of scope.)
- **Discord — Gateway**: bots maintain an outbound persistent WebSocket to Discord's gateway. Firewall-friendly by design.

Both fit the outbound-only model cleanly, so they are the right second and third channels. WhatsApp (webhook + Meta business verification + ToS issues on unofficial libraries) remains explicitly out of scope.

## Implementation Plan

### 1. SlackChannelAdapter (Socket Mode)

- Implement `ChannelAdapter` using Slack Socket Mode: obtain a WebSocket URL via `apps.connections.open` (app-level token), maintain the outbound socket, ack events, and handle reconnects.
- Map Slack identities (user IDs) onto the shared allowlist model; threads/channels map onto the gateway's per-conversation routing and the bound project.
- Send replies via `chat.postMessage`; respect Slack formatting (mrkdwn, code blocks) and message-size limits.
- Credentials (bot token + app-level token) stored via PasswordSafe, consistent with task-227.

### 2. DiscordChannelAdapter (gateway)

- Implement `ChannelAdapter` against the Discord gateway WebSocket (identify, heartbeat, resume, message-create events). Either a minimal client or a vetted JVM Discord library — evaluate dependency weight before adding one; prefer reusing the existing HTTP/WebSocket stack if practical.
- Map Discord user IDs onto the allowlist; channels/DMs onto conversation routing and the bound project.
- Send replies via the channel message endpoint; handle the 2000-char limit by chunking; render code blocks.
- Token via PasswordSafe.

### 3. Shared concerns (reuse, do not re-implement)

- Allowlist, deny-by-default auth, remote-capability gating (restricted-by-default; remote writes opt-in and still `AgentApprovalProvider`-gated), project binding, circuit breaker, and the inbound/outbound MessageBus wiring all come from task-227's `ChannelGatewayService`. These adapters only translate platform-specific transport/identity/formatting.
- Extend the "Messaging Channels" settings section with per-platform enable toggles, credential fields (PasswordSafe), and per-platform allowlists. Each platform shows independent live status (running/paused/circuit-broken).
- Optionally add per-platform tool scoping (Hermes assigns different toolsets per platform) — keep simple: each platform inherits the same remote-capability gating unless explicitly overridden.

### 4. Tests

- For each adapter: allowlist enforcement (allowlisted → submit; others refused/logged), outbound reply formatting + chunking against the platform limit (transport mocked), reconnect/resume handling, and circuit-breaker pause/resume.
- Confirm both adapters run concurrently with the Telegram adapter through a single `ChannelGatewayService` without cross-talk (a message on one platform replies only on that platform/conversation).
- Disposal cancels all sockets/loops with no leaked threads.

## Out of scope

- Webhook-only channels (WhatsApp Cloud API, Slack Events API), any companion relay server, and always-on/daemon operation — future tasks.
- Voice/media/transcription and cron delivery.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A `SlackChannelAdapter` implements the task-227 `ChannelAdapter` using Slack Socket Mode (outbound WebSocket, no public endpoint) and works behind a NAT/firewall
- [ ] #2 A `DiscordChannelAdapter` implements `ChannelAdapter` using the Discord gateway WebSocket (outbound, no public endpoint) and works behind a NAT/firewall
- [ ] #3 Both adapters reuse the shared allowlist (deny-by-default), remote-capability gating, project binding, and circuit-breaker from `ChannelGatewayService` — these are not re-implemented per adapter
- [ ] #4 Inbound messages route to `PROMPT_SUBMISSION_TOPIC` for the bound project and responses from `CONVERSATION_TOPIC` are sent back to the originating Slack/Discord conversation, chunked to each platform's message-size limit with reasonable code-block rendering
- [ ] #5 All platform credentials (Slack bot + app-level tokens, Discord bot token) are stored via IntelliJ `PasswordSafe`, never in the XML state
- [ ] #6 The "Messaging Channels" settings section gains per-platform enable toggles, credential fields, per-platform allowlists, and independent live status; all default off
- [ ] #7 Telegram, Slack, and Discord adapters can run concurrently through one `ChannelGatewayService` with no cross-talk (a message replies only on its own platform/conversation)
- [ ] #8 Reconnect/resume is handled for both WebSocket adapters, and disposal cancels all sockets/loops with no leaked threads
- [ ] #9 Any new third-party dependency (e.g. a Discord JVM library) is justified vs reusing the existing HTTP/WebSocket stack and documented
- [ ] #10 Unit tests cover allowlist enforcement, outbound formatting/chunking, reconnect handling, and concurrent multi-platform operation; plugin builds (`./gradlew buildPlugin`) and all tests pass (`./gradlew test`)
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Follow-up to task-227 (Telegram long-polling MVP + `ChannelAdapter` abstraction), itself from research comparing DevoxxGenie to Nous Research's Hermes Agent messaging gateway (https://hermes-agent.nousresearch.com/docs/user-guide/messaging/), which hosts many platform adapters in one process. Same outbound-only constraint (plugin behind NAT) drives the choice of Slack Socket Mode and the Discord gateway — both are outbound WebSockets needing no public endpoint, unlike webhook variants. All security/routing/gating logic is inherited from task-227's `ChannelGatewayService`; these adapters only handle platform-specific transport, identity mapping, and message formatting/chunking (Slack mrkdwn; Discord 2000-char limit). Evaluate dependency weight before pulling in a Discord library. Per CLAUDE.md: feature branch before code changes; sockets off the EDT, submission marshalled onto it; behavioural tests first where practical.
<!-- SECTION:NOTES:END -->
