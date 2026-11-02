---
sidebar_position: 7
title: Debug and Activity Log - DevoxxGenie
description: Capture agent, MCP, and raw model traffic in the Activity Log, inspect full payloads, and prepare safe diagnostic information for bug reports.
keywords: [devoxxgenie, debugging, activity log, raw request, mcp logs, agent logs]
image: /img/devoxxgenie-social-card.jpg
---

# Debug and Activity Log

Use **Settings → Tools → DevoxxGenie → Debug** to enable diagnostic logging. The three switches are independent:

| Setting | Captures | Activity Log filter |
|---|---|---|
| **Enable Raw Request/Response Logging** | Model request messages, tools, parameters, and response data | **Show Raw Only** |
| **Enable MCP Logging** | MCP traffic while MCP support is enabled | **Show MCP Only** |
| **Enable agent debug logs** | Tool arguments/results, intermediate responses, approvals, and sub-agent activity | **Show Agents Only** |

The MCP and agent switches also appear on their respective settings pages. They edit the same settings, not separate log streams.

## Inspect a failed request

1. Enable the relevant logging option and Apply.
2. Reproduce the issue.
3. Open the **Activity Log** tool window (registered as **DevoxxGenieActivityLogs**) from the IDE tool windows.
4. Choose a filter to narrow the entries.
5. Double-click an entry to open its full payload in an editor. Use **Copy All Logs** when you need to copy the log contents.
6. Disable unnecessary logging after troubleshooting.

By default, the panel retains **250 entries** and shows up to **500 characters** in each single-line preview rather than the full payload. A shortened row does not mean the underlying entry has lost its content. The log follows new entries only while you are already at the bottom, so you can scroll up to inspect earlier activity without being pulled back down.

:::warning Logs can contain sensitive content
Likely API keys and tokens are masked automatically, but raw prompts, responses, code, tool arguments, and results may still contain secrets or proprietary information. Redaction is not a guarantee. Inspect and sanitize copied logs before sharing them; do not attach a raw conversation blindly to a public issue.
:::

This diagnostic content is distinct from [anonymous usage analytics](privacy-and-analytics.md).

## Reporting an issue

Include the plugin and IDE versions, provider/model, the failing action, expected behavior, and a minimal sanitized log excerpt. Say whether Agent Mode, MCP, streaming, or an external runner was involved. Avoid sharing API keys or an entire project when a small reproduction is sufficient.
