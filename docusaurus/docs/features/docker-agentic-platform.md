---
sidebar_position: 15
title: Docker Agentic Platform Preview - DevoxxGenie
description: Configure the Docker ap CLI integration, browse agents and sessions, start runs, and understand the early-preview integration's limitations.
keywords: [devoxxgenie, docker, agentic platform, ap cli, agents, sessions, preview]
image: /img/devoxxgenie-social-card.jpg
---

# Docker Agentic Platform preview

The **Agentic Platform** tab integrates Docker's `ap` CLI with DevoxxGenie. It is separate from the built-in chat Agent Mode and the CLI/ACP runner providers.

:::warning Early preview
APIs, storage, and behavior may change. The plugin delegates execution and authentication to the external `ap` CLI; this page does not imply that every run is offline or sandboxed by DevoxxGenie's built-in tool approvals. Review the selected agent, project, and external platform permissions before submitting sensitive work.
:::

## Setup

1. Obtain the Docker Agentic Platform `ap` binary separately; the plugin does not install it.
2. Open **Settings → Tools → DevoxxGenie → Docker Agentic Platform (PREVIEW)**.
3. Check **Enable Docker Agentic Platform tab in the DevoxxGenie tool window**.
4. Set **ap CLI binary** to the executable on disk.
5. Select an authentication mode and click **Test connection**.
6. Apply your settings and open the **Agentic Platform** tab in the DevoxxGenie tool window.

The connection test runs `ap version` and `ap agent ls --limit 1`. It applies the values currently entered on the settings page, so the test uses those values even before you click Apply.

## Authentication modes

| Mode | Behavior |
|---|---|
| **Cached login** | Recommended starting point: authenticate by running `ap` in a terminal first. The plugin supplies no authentication environment overrides and lets the CLI use its cached credentials. |
| **Docker Desktop login** | Forces `AP_AGENTIC_PLATFORM_AUTH_MODE=docker-desktop`. |
| **Manual tokens** | Supplies the configured access and refresh tokens through `AP_AGENTIC_PLATFORM_ACCESS_TOKEN` and `AP_AGENTIC_PLATFORM_REFRESH_TOKEN`. |

If `ap agent ls` fails in your terminal, resolve the CLI authentication or access problem before relying on the IDE integration.

## Use the tab

- **New Run**: Refresh the available agents/projects, select both, enter a prompt, and submit. Output is displayed as it streams. The CLI is launched with the current IDE project's base directory as its working directory.
- **Sessions**: Browse past sessions with agent/project filters and open a selected session in the platform's browser UI.
- **Agents**: Browse agents and inspect their details.

The file-picker button in **New Run** inserts a file's path into the prompt. It is not the normal chat attachment pipeline and does not itself upload that file's contents.

The run button becomes **Stop** during execution. When the integration is watching spawned sessions after a stream ends, it becomes a polling control; stopping that watch leaves the displayed results visible. Stopping local polling should not be interpreted as confirmation that every remote session has been cancelled.

## Limitations and troubleshooting

- Continuing an existing session inside the IDE is not exposed by the current CLI integration; use **Open in browser** instead.
- To diagnose a connection failure, check the binary path and executable permissions, then run `ap version` and `ap agent ls` with the same authentication mode outside the IDE.
- Disabling the settings checkbox removes the integration tab; it is not an instruction to terminate work already running on an external platform.

See [Privacy and analytics](../configuration/privacy-and-analytics.md) before enabling external integrations, or use [Agent Mode](agent-mode.md) for the plugin's built-in tools.
