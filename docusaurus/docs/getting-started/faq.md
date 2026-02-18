---
sidebar_position: 7
title: Frequently Asked Questions - DevoxxGenie
description: Answers to common questions about DevoxxGenie — the free, open-source AI code assistant plugin for IntelliJ IDEA. Covers pricing, privacy, LLM support, Ollama, MCP, Agent Mode, and more.
keywords: [devoxxgenie faq, intellij ai plugin questions, is devoxxgenie free, devoxxgenie ollama, devoxxgenie privacy, devoxxgenie agent mode, devoxxgenie mcp, devoxxgenie vs copilot]
image: /img/devoxxgenie-social-card.jpg
---

import Head from '@docusaurus/Head';

<Head>
  <script type="application/ld+json">{`
    {
      "@context": "https://schema.org",
      "@type": "FAQPage",
      "mainEntity": [
        {
          "@type": "Question",
          "name": "Is DevoxxGenie free?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Yes, DevoxxGenie itself is completely free and open source. It uses a BYOK (Bring Your Own Keys) model — you supply your own API keys for cloud LLMs, or run local models with Ollama or LM Studio at no API cost. There is no DevoxxGenie subscription fee."
          }
        },
        {
          "@type": "Question",
          "name": "Does DevoxxGenie send my code to the cloud?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Only to the LLM provider you explicitly configure. If you use a cloud provider like OpenAI or Anthropic, your prompts (which may include code) are sent to that provider's API under their privacy policy. If you use a local model via Ollama or LM Studio, nothing leaves your machine. DevoxxGenie itself does not collect or transmit your code."
          }
        },
        {
          "@type": "Question",
          "name": "Which LLMs does DevoxxGenie support?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "DevoxxGenie supports a wide range of LLMs. Cloud providers include OpenAI (GPT-4o, o3, o4-mini), Anthropic (Claude 3.5/4), Google (Gemini 1.5/2.x), Grok (xAI), Mistral, Groq, DeepInfra, DeepSeek (R1, Coder), Kimi (Moonshot AI), GLM (Zhipu AI), OpenRouter, Azure OpenAI, and Amazon Bedrock. Local providers include Ollama, LM Studio, GPT4All, Llama.cpp, and Jan. It also supports any OpenAI-compatible custom endpoint."
          }
        },
        {
          "@type": "Question",
          "name": "How do I use Ollama with DevoxxGenie?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Install Ollama from ollama.com, pull a model (e.g., 'ollama pull llama3.2'), then in DevoxxGenie settings go to Tools > DevoxxGenie > LLM Providers > Ollama, leave the base URL as http://localhost:11434, click Refresh Models, and select your model. See the full guide at https://genie.devoxx.com/docs/getting-started/use-ollama-in-intellij-idea"
          }
        },
        {
          "@type": "Question",
          "name": "What is Agent Mode?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Agent Mode enables the LLM to autonomously explore and modify your codebase using built-in tools — reading files, listing directories, searching for patterns, running tests, and making targeted edits. Instead of you manually providing code context, the agent investigates your project on-demand. It works with both local Ollama models and cloud providers."
          }
        },
        {
          "@type": "Question",
          "name": "What is MCP in DevoxxGenie?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "MCP stands for Model Context Protocol — an open standard that lets LLMs connect to external tools and services. DevoxxGenie includes a built-in MCP Marketplace where you can browse and install MCP servers for filesystem access, web browsing, databases, APIs, and more. Once installed, the LLM can use these tools automatically during conversations."
          }
        },
        {
          "@type": "Question",
          "name": "What is Spec-driven Development (SDD)?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Spec-driven Development is a workflow where you define what needs to be built as structured task specs with acceptance criteria (stored as markdown files), and the LLM agent figures out how to build it. The DevoxxGenie Specs tool window shows tasks in a Kanban board and task list. Clicking 'Implement with Agent' injects the full spec into the LLM prompt, and the agent checks off acceptance criteria as it works."
          }
        },
        {
          "@type": "Question",
          "name": "What is RAG in DevoxxGenie?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "RAG stands for Retrieval-Augmented Generation. DevoxxGenie can index your project's source code into a local ChromaDB vector database (running in Docker) using Ollama embeddings. When you ask a question, it retrieves the most semantically relevant code snippets and includes them in the prompt automatically — giving the LLM accurate project-specific context without you having to manually select files."
          }
        },
        {
          "@type": "Question",
          "name": "Does DevoxxGenie work offline?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "Yes, if you use a local model provider like Ollama. Once you've downloaded a model, DevoxxGenie can run entirely offline — no internet connection is needed. Cloud providers (OpenAI, Anthropic, etc.) require an internet connection."
          }
        },
        {
          "@type": "Question",
          "name": "Which IntelliJ versions does DevoxxGenie support?",
          "acceptedAnswer": {
            "@type": "Answer",
            "text": "DevoxxGenie requires IntelliJ IDEA 2023.3.4 or later. It works with IntelliJ IDEA Community and Ultimate editions, as well as other JetBrains IDEs built on the IntelliJ platform (like PyCharm, GoLand, WebStorm)."
          }
        }
      ]
    }
  `}</script>
</Head>

# Frequently Asked Questions

## General

### Is DevoxxGenie free?

Yes, DevoxxGenie itself is completely free and open source. It uses a **BYOK (Bring Your Own Keys)** model — you supply your own API keys for cloud LLMs, or run local models with Ollama or LM Studio at no API cost. There is no DevoxxGenie subscription fee.

### Does DevoxxGenie send my code to the cloud?

Only to the LLM provider **you** explicitly configure:

- **Cloud providers** (OpenAI, Anthropic, etc.): your prompts, which may include code, are sent to that provider's API under their own privacy policy.
- **Local providers** (Ollama, LM Studio, etc.): nothing leaves your machine. The model runs locally.

DevoxxGenie itself does not collect, store, or transmit your code.

### Which IntelliJ versions are supported?

DevoxxGenie requires **IntelliJ IDEA 2023.3.4 or later**. It works with the Community and Ultimate editions, and with other JetBrains IDEs on the IntelliJ platform (PyCharm, GoLand, WebStorm, etc.).

### Does DevoxxGenie work offline?

Yes — if you use a local model provider like **Ollama**. Once you've downloaded a model, DevoxxGenie runs entirely offline. Cloud providers (OpenAI, Anthropic, Gemini, etc.) require an internet connection.

---

## LLM Providers

### Which LLMs does DevoxxGenie support?

**Cloud providers**: OpenAI (GPT-4o, o3, o4-mini), Anthropic (Claude 3.5/4), Google (Gemini 1.5/2.x), Grok (xAI), Mistral, Groq, DeepInfra, DeepSeek (R1, Coder), Kimi (Moonshot AI), GLM (Zhipu AI), OpenRouter, Azure OpenAI, Amazon Bedrock

**Local providers**: Ollama, LM Studio, GPT4All, Llama.cpp, Jan, any OpenAI-compatible endpoint

### How do I use Ollama with DevoxxGenie?

See the [full Ollama setup guide](/docs/getting-started/use-ollama-in-intellij-idea). The short version:

1. Install Ollama and pull a model: `ollama pull llama3.2` or `ollama pull llama4`
2. In DevoxxGenie settings → **LLM Providers** → **Ollama**
3. Leave the base URL as `http://localhost:11434`
4. Click **Refresh Models** and select your model

### Can I use my own API endpoint (OpenAI-compatible)?

Yes. DevoxxGenie supports [custom providers](../llm-providers/custom-providers.md) — any endpoint that speaks the OpenAI chat completions API, including self-hosted models, DeepSeek R1, Grok, JLama, and enterprise AI platforms.

---

## Features

### What is Agent Mode?

[Agent Mode](../features/agent-mode.md) enables the LLM to **autonomously explore and modify your codebase** using built-in tools — reading files, listing directories, searching for patterns, running tests, and making targeted edits. Instead of manually providing code context, the agent investigates your project on-demand.

It works with both local Ollama models (e.g. Qwen2.5, Llama 4, Mistral Small) and cloud providers.

### What is Spec-driven Development (SDD)?

[Spec-driven Development](../features/spec-driven-development.md) is a workflow where you define **what** needs to be built as structured task specs with acceptance criteria (stored as markdown files), and the LLM agent figures out **how** to build it.

The DevoxxGenie Specs tool window shows tasks in a Kanban board. Click **"Implement with Agent"** and the agent checks off acceptance criteria as it works.

### What is MCP?

[MCP (Model Context Protocol)](../features/mcp_expanded.md) is an open standard that lets LLMs connect to external tools and services. DevoxxGenie includes a built-in **MCP Marketplace** where you can install servers for filesystem access, web browsing, databases, APIs, and more. The LLM uses these tools automatically during conversations.

### What is RAG?

[RAG (Retrieval-Augmented Generation)](../features/rag.md) indexes your project's source code into a local vector database (ChromaDB via Docker) using Ollama embeddings. When you ask a question, the most semantically relevant code snippets are retrieved and included in the prompt automatically — giving the LLM accurate project context without manual file selection.

### What are Skills?

[Skills](../features/skills.md) are reusable slash commands you define in settings. Type `/explain`, `/test`, `/review`, or any custom command in the prompt input to trigger a predefined prompt template. Built-in skills include `/test`, `/explain`, `/review`, `/find` (RAG search), `/tdg`, and `/init`.

### What is inline code completion?

[Inline completion](../features/inline-completion.md) provides GitHub Copilot-style ghost-text suggestions as you type, powered by Fill-in-the-Middle (FIM) models via Ollama or LM Studio. Enable it in **Settings** → **DevoxxGenie** → **Inline Completion**.

---

## Troubleshooting

### Ollama models aren't showing up in the dropdown

Click **Refresh Models** in DevoxxGenie settings after pulling a new model. Make sure Ollama is running (`ollama serve` or verify `http://localhost:11434` is reachable in a browser).

### Responses are very slow with local models

Switch to a smaller quantized model. For chat, try `llama3.2:3b` or `llama4:scout`. For inline completion, try `qwen2.5-coder:0.5b`. See the [Ollama performance tips](use-ollama-in-intellij.md#performance-tips).

### Where do I report bugs or request features?

Open an issue on [GitHub](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) or start a discussion in [GitHub Discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions).
