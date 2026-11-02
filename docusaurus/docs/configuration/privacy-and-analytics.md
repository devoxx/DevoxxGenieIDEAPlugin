---
sidebar_position: 6
title: Privacy and Analytics - DevoxxGenie
description: Understand what leaves your machine, control anonymous usage analytics, and distinguish telemetry from provider requests and sensitive debug logs.
keywords: [devoxxgenie, privacy, analytics, consent, telemetry, credentials, local models]
image: /img/devoxxgenie-social-card.jpg
---

# Privacy and analytics

Using a local model keeps model inference local, but does not make every optional integration offline. This page separates model requests, usage analytics, and diagnostic logs.

## Model requests and external tools

- **Local providers** receive prompts at the endpoint you configure. For local-only inference, use a service running on your own machine.
- **Cloud providers** receive prompts and any code or attachments included in them.
- **Web Search**, remote **MCP servers**, external **CLI/ACP runners**, and the **Docker Agentic Platform** can communicate with other services independently of the selected chat model. Review their configuration before sending sensitive information.
- Conversation history is stored locally. API keys managed by the plugin use IntelliJ's credential storage; this does not remove sensitive text from prompts, history, or diagnostic logs.

Disabling usage analytics does not disable requests to providers or tools you explicitly use.

## Analytics consent

A first-launch notification offers **Sure, help out** or **Disable**. No usage analytics is emitted before the consent notice has been acknowledged, or when analytics is disabled. Dismissing the notification without choosing does not grant consent, and the notice is not repeatedly shown.

You can change the choice under **Settings → Tools → DevoxxGenie → Analytics** using **Help improve DevoxxGenie by sending anonymous usage data**, then Apply. Applying that setting also acknowledges the notice.

## What analytics contains

When enabled and acknowledged, the plugin can send:

- An anonymous install ID and a per-launch session ID.
- Plugin and IDE versions, selected provider and model names.
- Identifiers for enabled or used features, such as Agent Mode, RAG, MCP, streaming, and Web Search.
- Coarse counts/buckets for configured MCP servers, custom commands, chat memory size, and tool use.

Analytics does **not** include prompt or response text, conversation history, source files or paths, project names, Git remotes, API keys, credentials, token usage, cost data, MCP server names/URLs/commands, or custom command names and bodies. The Analytics settings page links to the source implementation of the payload builder.

## Diagnostic logs are different

[Debug logging](debugging.md) is not anonymous usage telemetry. Raw request/response logging deliberately captures conversation content for troubleshooting. Likely credentials are masked, but prompts and responses are not generally redacted. Review logs before copying them or attaching them to an issue.

## Related guides

- [Debug and Activity Log](debugging.md)
- [Web Search](../features/web-search.md)
- [MCP Support](../features/mcp_expanded.md)
- [CLI Runners](../features/cli-runners.md) and [ACP Runners](../features/acp-runners.md)
- [Docker Agentic Platform preview](../features/docker-agentic-platform.md)
