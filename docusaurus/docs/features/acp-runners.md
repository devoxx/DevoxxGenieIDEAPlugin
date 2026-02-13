---
sidebar_position: 4
title: ACP Runners
description: Execute spec tasks via external ACP (Agent Communication Protocol) tools. DevoxxGenie communicates with ACP-compatible CLIs using JSON-RPC 2.0 over stdin/stdout for structured agent interactions.
keywords: [devoxxgenie, acp runners, agent communication protocol, json-rpc, claude, kimi, gemini, kilocode, agent loop, spec-driven development]
image: /img/devoxxgenie-social-card.jpg
---

# ACP Runners

ACP (Agent Communication Protocol) Runners let you execute spec tasks via **external CLI tools** that speak the ACP protocol — a structured, bidirectional communication layer built on **JSON-RPC 2.0 over stdin/stdout**. Unlike [CLI Runners](cli-runners.md) which pipe plain-text prompts and read stdout, ACP provides typed messages, capability negotiation, and structured streaming.

ACP Runners integrate with both [Spec-driven Development](spec-driven-development.md) (single task execution) and the [Agent Loop](sdd-agent-loop.md) (batch task execution).

## How It Works

DevoxxGenie spawns the ACP tool as a child process and communicates over stdin/stdout using JSON-RPC 2.0. The protocol begins with a capability handshake, then DevoxxGenie sends task prompts as structured messages and receives streaming responses.

```
┌──────────────────┐  JSON-RPC 2.0   ┌──────────────────────┐
│  DevoxxGenie     │◀═══════════════▶│  ACP Tool            │
│  (ACP Client)    │  stdin/stdout    │  (Claude/Kimi/...)   │
└──────────────────┘                  └──────────────────────┘
        │                                      │
  1. Spawns process                   2. Handshake (initialize)
  3. Sends prompt via                 4. Streams structured
     ACP message                        response chunks back
  5. Receives completion              6. Process stays alive
     with metadata                      for next message
```

### Protocol Flow

1. **Initialize** — Client sends `initialize` with supported capabilities; the tool responds with its capabilities
2. **Send Prompt** — Client sends the task prompt as a structured ACP message
3. **Receive Response** — Tool streams response chunks back, including text, file operations, and terminal commands
4. **Completion** — Tool signals completion; client processes the final result

## Supported ACP Tools

| ACP Tool | Executable | Description |
|----------|-----------|-------------|
| **Claude** | `claude-code-acp` | Claude Code via the [claude-code-acp](https://github.com/zed-industries/claude-code-acp) bridge by Zed Industries |
| **Copilot** | `copilot --acp` | GitHub Copilot CLI in ACP mode |
| **Kimi** | `kimi` | Moonshot AI's coding assistant with ACP support |
| **Gemini CLI** | `gemini` | Google's Gemini CLI with ACP protocol mode |
| **Kilocode** | `kilocode` | Kilocode's AI coding agent |
| **Custom** | *(user-defined)* | Any ACP-compatible tool |

## Setup

1. Open **Settings** > **Tools** > **DevoxxGenie** > **CLI/ACP Runners**
2. Scroll to the **ACP Runners** section
3. Click **+** to add a new ACP tool
4. Select the **Type** from the dropdown — the executable path is pre-filled with sensible defaults
5. Adjust the **Executable path** if your CLI is installed in a different location
6. Click **Test Connection** to verify the ACP handshake succeeds
7. Click **OK**, then **Apply**

### Claude via claude-code-acp

Claude Code doesn't natively speak ACP, but Zed Industries maintains the [claude-code-acp](https://github.com/zed-industries/claude-code-acp) bridge that wraps Claude Code in an ACP-compatible interface.

To set it up:

1. Install the bridge: `npm install -g @anthropic-ai/claude-code-acp` (or follow the repository instructions)
2. In DevoxxGenie, add a new ACP tool with **Type: Claude** — the executable path auto-fills to `claude-code-acp`
3. Ensure Claude Code is installed and authenticated (`claude --version` should work)
4. Click **Test Connection** to verify the ACP handshake

## Configuration Reference

| Field | Description |
|-------|-------------|
| **Type** | Preset type (Claude, Copilot, Kimi, Gemini, Kilocode, Custom). Selecting a type auto-fills the executable path. |
| **Executable path** | Path to the ACP-compatible CLI binary (e.g., `claude-code-acp`, `copilot`, `kimi`, `gemini`) |
| **Enabled** | Toggle to enable/disable a tool without deleting its configuration |

## ACP vs CLI Runners

| Feature | CLI Runners | ACP Runners |
|---------|-------------|-------------|
| **Communication** | Plain text over stdin/stdout | JSON-RPC 2.0 over stdin/stdout |
| **Protocol** | One-shot process per task | Persistent process with structured messages |
| **Streaming** | Raw stdout stream | Typed response chunks |
| **Capability negotiation** | None | Handshake with capability exchange |
| **MCP integration** | Auto-generated MCP config file | Not required (protocol handles context) |
| **Process lifecycle** | Spawned and terminated per task | Stays alive for multiple interactions |

:::tip
Claude Code is available both as a **CLI Runner** (direct stdin/stdout) and as an **ACP Runner** (via the claude-code-acp bridge). The ACP mode provides structured communication and richer streaming, while the CLI mode is simpler to set up.
:::

## Selecting the Execution Mode

The **DevoxxGenie Specs** toolbar contains an execution mode dropdown:

- **LLM Provider** — uses the built-in LLM agent (default)
- **CLI: Claude** / **CLI: Copilot** / etc. — uses CLI runners
- **ACP: Claude** / **ACP: Copilot** / **ACP: Kimi** / **ACP: Gemini** / etc. — uses ACP runners

The selection is persisted across IDE restarts.

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| ACP handshake fails | Tool not installed or wrong path | Check the executable path in **Settings > CLI/ACP Runners**, use **Test Connection** to verify |
| "Connection refused" error | Tool doesn't support ACP protocol | Verify the tool version supports ACP. Some tools require a specific flag or version for ACP mode |
| No response after prompt | Process started but not responding | Check that the tool is authenticated. Try running the executable manually with `--version` to verify installation |
| Test Connection times out | Tool is slow to initialize | Some tools need to download models on first run. Try running the tool manually first to complete initial setup |
| Claude ACP bridge not found | `claude-code-acp` not installed globally | Install via `npm install -g @anthropic-ai/claude-code-acp` and ensure it's on your PATH |
