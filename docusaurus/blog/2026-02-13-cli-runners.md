---
slug: cli-runners
title: "CLI Runners: Use Your AI Subscriptions Directly Inside Your JetBrains IDE"
authors: [stephanj]
tags: [CLI runners, Claude Code, GitHub Copilot, OpenAI Codex, Google Gemini, Kimi, spec-driven development, agent mode, IntelliJ IDEA, LLM, open source]
date: 2026-02-13
description: "DevoxxGenie CLI Runners let you use Claude Code, GitHub Copilot, OpenAI Codex, Google Gemini, and Kimi directly inside IntelliJ — leveraging the AI subscriptions you already pay for."
keywords: [devoxxgenie, cli runners, claude code, github copilot, openai codex, google gemini, kimi, ai subscriptions, jetbrains, intellij idea, spec-driven development]
image: /img/devoxxgenie-social-card.jpg
---

# CLI Runners: Use Your AI Subscriptions Directly Inside Your JetBrains IDE

You're paying for Claude Pro. You've got a GitHub Copilot seat. Maybe you're subscribed to Google Gemini or Kimi too. But when you want to use these tools for serious coding work, you're jumping between terminal windows, browser tabs, and your IDE — context-switching constantly.

What if you could route all of that through a single interface inside IntelliJ, using the subscriptions you already pay for?

That's exactly what **CLI Runners** in DevoxxGenie do.

<!-- truncate -->

## The Subscription Problem

Most developers today are paying for at least one AI coding subscription. Claude Pro, GitHub Copilot, OpenAI's Codex — these are flat-rate monthly plans that give you access to powerful models. But using them typically means leaving your IDE: opening a terminal for Claude Code, switching to a browser for Copilot Chat, or running a separate CLI session for Codex.

Meanwhile, if you want AI inside your IDE through a plugin, you usually need to set up API keys and pay per-token on top of your existing subscription. You end up paying twice for the same models.

CLI Runners eliminate this problem entirely. DevoxxGenie launches your CLI tools as external processes, pipes task prompts to them, and streams the output back into IntelliJ's console — all while using **your existing subscription**, not a separate API billing account.

## How It Works

CLI Runners integrate directly with DevoxxGenie's [Spec-driven Development](/docs/features/spec-driven-development) workflow. When you select a task to implement, instead of routing it through a built-in LLM provider, DevoxxGenie can hand it off to an external CLI tool of your choice.

The system:

1. **Launches the CLI tool** as an external process
2. **Pipes the task prompt** via stdin (or as a trailing argument, depending on the tool)
3. **Auto-generates an MCP configuration** so the CLI tool can read and update your backlog tasks
4. **Streams output** into IntelliJ's Run tool window in real time
5. **Reports exit code and elapsed time** when the task completes

This means Claude Code, GitHub Copilot, and others can work on your spec-driven tasks with full awareness of your backlog — checking off acceptance criteria as they go.

## Supported CLI Tools

DevoxxGenie ships with built-in presets for five major CLI tools, plus a fully customizable option:

| Tool | Prompt Delivery | MCP Support | Notes |
|------|-----------------|-------------|-------|
| **Claude Code** | stdin with `-p` flag | Yes (auto-configured) | Full backlog integration |
| **GitHub Copilot** | stdin | Yes (auto-configured) | Uses `@` prefix |
| **OpenAI Codex** | Trailing argument | No | Cannot update task status directly |
| **Google Gemini** | stdin | Yes (auto-configured) | Full backlog integration |
| **Kimi** | `--prompt` flag | Yes (auto-configured) | Full backlog integration |
| **Custom** | User-defined | Configurable | Bring your own CLI tool |

For tools with MCP support, DevoxxGenie automatically generates and injects the MCP server configuration. This gives the CLI tool access to the 17 backlog management tools — so it can create, edit, complete, and archive tasks just like the built-in agent does.

## Setting It Up

Configuration takes about a minute:

1. Go to **Settings > Tools > DevoxxGenie > Spec Driven Dev**
2. In the **CLI Runners** section, click **+**
3. Select a tool **Type** from the dropdown — defaults are populated automatically
4. Adjust the **executable path** if needed
5. Add any **environment variables** for authentication (e.g., API keys)
6. Click **Test Connection** to verify everything works
7. **Apply** and you're done

![CLI Runners Setup](/img/CLI-Runners-Setup.png)

## Switching Between Tools

The DevoxxGenie Specs toolbar includes an execution mode dropdown where you can switch between the built-in LLM provider and any configured CLI runner. The selection persists across IDE restarts.

![CLI Runners Selection](/img/CLI-Runners-Selection.png)

This makes it easy to keep multiple tools configured and switch depending on the task. Use Claude Code for complex refactoring, Copilot for quick fixes, or a local model via the built-in provider when you want to stay offline.

## Interactive Chat Mode

CLI Runners aren't limited to spec-driven task execution. You can also chat interactively with any configured CLI tool directly from the DevoxxGenie chat window. Messages are sent to the external tool, and responses stream back in real time.

![CLI Runner Chat](/img/CLI-Runner-Chat.jpg)

This is where the subscription value really shines. Instead of burning API tokens at per-request rates, your conversations go through your existing monthly plan — whether that's Claude Pro, Copilot Business, or any other subscription. **Same models, same quality, no extra cost.**

## Why This Matters

The AI tooling landscape is fragmented. Every provider has their own CLI, their own interface, their own way of doing things. As a developer, you shouldn't have to choose one ecosystem and lock yourself in.

With CLI Runners, DevoxxGenie becomes a **unified interface** for all your AI coding tools:

- **Use what you already pay for.** No duplicate API costs. Your Claude Pro, Copilot, or Gemini subscription works directly inside IntelliJ.
- **Stay in your IDE.** No terminal hopping, no browser tabs. Everything streams into IntelliJ's console and chat windows.
- **Keep your workflow.** CLI Runners plug into the same spec-driven development and agent loop workflows. Your tasks, acceptance criteria, and backlog stay consistent regardless of which tool executes them.
- **Switch freely.** Different tools for different jobs. Configure them all, pick the right one for each task from a dropdown.

## Bring Your Own Tool

If your favourite CLI tool isn't in the preset list, the **Custom** type lets you configure any executable that can read prompts from stdin. Set the path, arguments, and optionally an MCP config flag — and it works like the rest.

## Next Up: ACP Runners

CLI Runners use stdin and MCP to communicate with external tools — and that works well. But the next evolution is already on the horizon: **Agent Communication Protocol (ACP)**.

ACP is an emerging standard for structured, bidirectional communication between AI agents. Several CLI tools are beginning to adopt it, and once they do, the integration gets significantly richer. Instead of piping text prompts and parsing stdout, ACP enables proper agent-to-agent dialogue — task delegation, progress callbacks, structured results, and multi-turn coordination between DevoxxGenie's built-in agent and external CLI agents.

We're actively exploring **ACP Runners** as the next step. Same idea as CLI Runners — leverage the tools and subscriptions you already have — but with a protocol designed specifically for agents talking to agents. Stay tuned.

## Getting Started

1. Update DevoxxGenie to the latest version
2. Go to **Settings > DevoxxGenie > Spec Driven Dev > CLI Runners**
3. Add your preferred CLI tools
4. Select a CLI runner from the toolbar dropdown
5. Start implementing tasks — or just chat

Your AI subscriptions are already paid for. Now put them to work where you actually write code.

**Links:**
- [Install from JetBrains Marketplace](https://plugins.jetbrains.com/plugin/24169-devoxxgenie)
- [CLI Runners Documentation](https://genie.devoxx.com/docs/features/cli-runners)
- [Spec-driven Development Docs](https://genie.devoxx.com/docs/features/spec-driven-development)
- [GitHub Repository](https://github.com/devoxx/DevoxxGenieIDEAPlugin)
