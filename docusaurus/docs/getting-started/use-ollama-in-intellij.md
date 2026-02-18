---
sidebar_position: 6
title: How to Use Ollama in IntelliJ IDEA with DevoxxGenie
description: Step-by-step guide to running local AI models in IntelliJ IDEA using Ollama and DevoxxGenie. No API keys, no cloud, no cost — run models like Llama, Mistral, and DeepSeek entirely on your machine.
keywords: [ollama intellij idea, ollama intellij plugin, local llm intellij, run ollama intellij, devoxxgenie ollama setup, offline ai coding intellij, free ai coding intellij]
image: /img/devoxxgenie-social-card.jpg
slug: /getting-started/use-ollama-in-intellij-idea
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# How to Use Ollama in IntelliJ IDEA with DevoxxGenie

[Ollama](https://ollama.com) lets you run large language models locally — on your own CPU or GPU — with no API keys, no cloud dependency, and no per-token cost. DevoxxGenie integrates with Ollama out of the box, so you can use powerful models like **Llama 3**, **Mistral**, **DeepSeek Coder**, and **Qwen** directly inside IntelliJ IDEA.

This guide walks you through the full setup in under five minutes.

## Why Use Ollama?

- **Free**: No API costs. Run as many prompts as you want.
- **Private**: Your code never leaves your machine.
- **Offline**: Works without an internet connection after the initial model download.
- **Flexible**: Switch models instantly as new ones are released.

## Step 1: Install Ollama

Download and install Ollama from [ollama.com](https://ollama.com/download) for macOS, Windows, or Linux.

After installation, verify it's running:

```bash
ollama --version
```

Ollama runs as a local HTTP server on `http://localhost:11434` by default.

## Step 2: Pull a Model

Download the model you want to use. For general coding assistance, these are good starting points:

<Tabs>
  <TabItem value="general" label="General Purpose" default>

```bash
ollama pull llama3.2          # Meta's Llama 3.2 (3B) — fast, good for chat
ollama pull llama3.1:8b       # Llama 3.1 8B — better quality, needs ~8GB RAM
ollama pull mistral           # Mistral 7B — strong reasoning
ollama pull qwen3:8b          # Alibaba Qwen3 — excellent coding ability
```

  </TabItem>
  <TabItem value="coding" label="Coding Focused">

```bash
ollama pull deepseek-coder-v2  # DeepSeek Coder V2 — top-tier code model
ollama pull qwen3:8b           # Qwen3 — strong at Java/Kotlin
ollama pull codellama:13b      # Meta Code Llama 13B
```

  </TabItem>
  <TabItem value="agent" label="Agent Mode">

```bash
ollama pull glm-4.7-flash      # GLM-4.7 Flash — excellent tool-use, fast, great for Agent Mode
ollama pull qwen3:14b          # Qwen3 14B — strong reasoning for agents
ollama pull llama3.1:70b       # Llama 3.1 70B — best quality if your GPU can handle it
```

  </TabItem>
  <TabItem value="completion" label="Inline Completion (FIM)">

```bash
ollama pull qwen3:0.6b           # Tiny and fast for inline completion
ollama pull starcoder2:3b         # StarCoder2 3B — excellent FIM model
ollama pull deepseek-coder:1.3b   # DeepSeek Coder 1.3B — lightweight FIM
```

  </TabItem>
</Tabs>

The first pull downloads the model weights (1–20 GB depending on model size). Subsequent runs use the cached version.

## Step 3: Install DevoxxGenie

If you haven't already, install DevoxxGenie from the JetBrains Marketplace:

1. Open IntelliJ IDEA
2. Go to **Settings** → **Plugins** → **Marketplace**
3. Search for **DevoxxGenie**
4. Click **Install** and restart the IDE

## Step 4: Configure Ollama in DevoxxGenie

1. Open **Settings** → **Tools** → **DevoxxGenie**
2. In the **LLM Providers** section, find **Ollama**
3. The base URL defaults to `http://localhost:11434` — leave this as-is unless you're running Ollama on a different host or port
4. Click **Refresh Models** — DevoxxGenie queries the Ollama API and populates the model list automatically
5. Select your model from the **Model Name** dropdown
6. Click **Apply**

:::tip Running Ollama on a Different Machine?
If Ollama runs on a server or another computer on your network, change the base URL to `http://your-server-ip:11434`. Make sure the Ollama server is accessible from your development machine.
:::

## Step 5: Start Chatting

Open the DevoxxGenie tool window (the genie lamp icon in the right sidebar) and start asking questions. The response comes from your local Ollama instance — no data leaves your machine.

**Example prompts to try:**
- Select a Java class → ask "Explain this class and its responsibilities"
- Select a method → ask "Write a JUnit 5 test for this method"
- Ask "What are the design patterns used in this codebase?" with relevant files added to context

## Inline Code Completion with Ollama

DevoxxGenie supports Fill-in-the-Middle (FIM) inline completion powered by Ollama. This provides GitHub Copilot-style ghost-text suggestions as you type.

1. Go to **Settings** → **Tools** → **DevoxxGenie** → **Inline Completion**
2. Enable **Inline Completion**
3. Set the provider to **Ollama**
4. Select a FIM-capable model (e.g., `qwen3:0.6b`, `starcoder2:3b`)
5. Click **Apply**

Use a small, fast model (1–3B parameters) for inline completion — responsiveness matters more than raw quality here.

## Agent Mode with Ollama

[Agent Mode](../features/agent-mode.md) lets the LLM autonomously read, edit, and search your codebase. It works with local Ollama models — no cloud API key required.

For Agent Mode, use a model with strong tool-use (function-calling) support:
- `glm-4.7-flash` — excellent tool-use reliability, fast and efficient, great for Agent Mode
- `qwen3:14b` — strong reasoning and code understanding
- `llama3.1:8b` — good all-round choice for lighter setups

Enable Agent Mode in the DevoxxGenie toolbar, select your Ollama model, and the LLM will be able to use built-in tools to explore your project autonomously.

## Performance Tips

| Hardware | Recommended Model Size | Expected Speed |
|----------|----------------------|----------------|
| 8GB RAM, no dedicated GPU | 3–7B (quantized) | 5–15 tokens/sec |
| 16GB RAM / 8GB VRAM | 7–13B | 15–40 tokens/sec |
| 32GB RAM / 16GB VRAM | 13–30B | 20–60 tokens/sec |
| 64GB+ RAM / 24GB VRAM | 70B | 10–30 tokens/sec |

- **Apple Silicon Macs** (M1/M2/M3/M4) run Ollama models very efficiently using unified memory — a 16GB M2 Mac handles 13B models comfortably.
- Prefer **quantized models** (Q4_K_M or Q5_K_M) for the best speed/quality trade-off.
- For inline completion, always use the smallest model that gives acceptable results.

## Troubleshooting

**DevoxxGenie says "No models found"**
Ensure Ollama is running (`ollama serve`) and that you've pulled at least one model (`ollama list`).

**Slow responses**
Switch to a smaller/more quantized model. For chat, try `llama3.2:3b`. For completion, try `qwen3:0.6b`.

**Connection refused**
Check that Ollama is running on the correct port. Try opening `http://localhost:11434` in a browser — you should see `Ollama is running`.

**Model not showing in dropdown**
Click **Refresh Models** in DevoxxGenie settings after pulling a new model with `ollama pull`.

## Next Steps

- [Local LLM Providers](../llm-providers/local-models.md) — full reference for all local provider options
- [Agent Mode](../features/agent-mode.md) — autonomous codebase exploration with local models
- [Inline Completion](../features/inline-completion.md) — FIM-based ghost-text suggestions with Ollama
- [Why DevoxxGenie](why-devoxxgenie.md) — how DevoxxGenie compares to subscription-based tools
