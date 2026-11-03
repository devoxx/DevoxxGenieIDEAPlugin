---
sidebar_position: 3
title: Token Cost & Context Window
description: Read model token limits and per-million-token cost estimates in DevoxxGenie, with current API examples and guidance for local and custom providers.
keywords: [devoxxgenie, token cost, context window, token calculator, llm pricing, openai cost, anthropic cost, gemini cost]
image: /img/devoxxgenie-social-card.jpg
---

# Token Cost & Context Window

DevoxxGenie displays model limits and estimates token usage and cost to help you choose appropriate context. These figures are planning aids, not a provider billing statement or a guarantee that a request will fit.

## Understanding Tokens and Costs

### What are Tokens?

Models process text as tokens rather than characters or words. Tokenization varies by model, language, code, and other input types. Local text counts may differ from provider-reported usage, especially for images, reasoning, tool definitions, and message formatting.

### Why Token Management Matters

- **Cost**: Input and output can have different rates, and agent loops can make multiple billable requests.
- **Capacity**: System instructions, conversation history, attached code, tool results, and generated output all need room within the applicable limits.
- **Focus**: A larger supported window is not a reason to include every file in every request.

## Token Cost Configuration

Open **Settings → Tools → DevoxxGenie → Token Cost & Context Window**.

![Token Cost Settings](/img/token-cost-settings.png)

### Viewing Provider Costs {#configuring-provider-costs}

The settings page contains a **read-only model catalogue**, with columns for **Provider**, **Model**, **Input Cost**, **Output Cost**, and **Context Window**. Catalogue costs are in **US dollars per 1,000,000 tokens**, not per 1,000 tokens. Values apply to individual models, not to an entire provider.

Opening this page triggers a background refresh of the model configuration. The plugin uses remotely maintained model metadata with cached and built-in fallback values if the configuration is unavailable. This catalogue is not a live billing feed, and it can lag provider changes.

You cannot edit the catalogue's cost or context cells on this page. It does let you show or hide the **Calc Tokens** and **Add File** footer buttons.

### Provider-Specific Settings

Some providers have their own context or cost overrides on the main **Settings → Tools → DevoxxGenie** page:

- **Custom OpenAI**: Enable **Context Window** to enter the endpoint's real capacity; without an override the plugin assumes **4096 tokens**. **Input Cost** and **Output Cost** use USD per million tokens; zero hides the cost estimate rather than establishing that the service is free. See [Custom provider controls](../llm-providers/custom-providers.md#gateway-compatibility-and-usage-estimates).
- **Local models**: The usable context depends on the exact model, server metadata, and server configuration. A model's advertised maximum does not mean your local server has allocated that much context.

For actual charges, use your provider's pricing and usage dashboard. Caching, long-context rates, batch processing, reasoning, tool fees, and account-specific terms can make a simple input/output estimate differ from the invoice.

## Context Window Management

A **context window** is not the same as a **maximum output length**. Some providers publish a combined context budget; others list input and output limits separately. Reserve space for the answer and any model-specific reasoning rather than filling the entire advertised capacity with source code.

### Window Size by Provider

The following are **representative API specifications checked on November 2, 2026**, not an exhaustive list or a promise that every model is available in your installed plugin/account. The model links lead directly to official specifications so you can check for changes.

| Provider | Model | Published context/input limit | Maximum output |
|---|---|---|---|
| OpenAI | [GPT-5.5 (`gpt-5.5`)](https://developers.openai.com/api/docs/models/gpt-5.5) | 1,050,000-token context window | 128,000 tokens |
| OpenAI | [GPT-5.4 (`gpt-5.4`)](https://developers.openai.com/api/docs/models/gpt-5.4) | 1,050,000-token context window | 128,000 tokens |
| Anthropic | [Claude Sonnet 5.5 (`claude-sonnet-5-5`)](https://platform.claude.com/docs/en/models/sonnet-5-5/overview) | 1,000,000-token context window | 128,000 tokens |
| Google | [Gemini 3.8 Flash (`gemini-3.8-flash`)](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash) | 1,048,576-token **input limit** | 65,536 tokens |
| Google | [Gemini 3.1 Pro Preview (`gemini-3.1-pro-preview`)](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-pro-preview) | 1,048,576-token **input limit** | 65,536 tokens |
| Ollama / other local servers | Exact installed model and server configuration | Inspect the model/server configuration; no single provider-wide limit | Model/server-dependent |

These are API limits, not ChatGPT, Claude chat subscription, or IDE-agent product limits. Model aliases, preview versions, hosting platforms, and account access can differ. A provider may publish a newer model before the plugin catalogue has its metadata; use the model list and displayed limits in your installation rather than assuming this table changes the available models.

The plugin's **Context Window** column is model metadata used for estimates. It does not reconfigure your server, increase an API limit, or account for every provider-specific budgeting rule.

### Optimizing Context Window Usage

1. Select a model and inspect the displayed context capacity.
2. Add only the relevant files or directories and check the token estimate.
3. Leave headroom for instructions, history, tool calls/results, and output.
4. Use [RAG](../features/rag.md) or Agent Mode's targeted file-reading tools when a whole project is too large.

## Token Calculation Features

### Token Calculator

To inspect directory context:

1. Right-click a directory in the project view.
2. Select **Calc Tokens for Directory**.
3. Review the token count and scan information. Cost/compatible-model information depends on the calculation path and available metadata.

![Token Calculator](/img/token-calculator.png)

### Calculating Tokens for Current Prompt

Use the **Calc Tokens** footer action to estimate context before sending. If it is hidden, enable **Show Calc Tokens button in footer** on the Token Cost & Context Window settings page.

Treat the count as an estimate of the included text, not a complete reproduction of the provider's tokenizer and request accounting. Increasing the configured [chat memory size](../features/chat-memory.md) or attaching more files can increase subsequent request usage.

### Token Usage in Responses

Response metrics can show input/output token usage and an estimated cost when the execution path supplies that information. Missing usage or pricing metadata is not evidence of a free request.

The basic catalogue estimate uses:

```text
estimated cost (USD) =
    input tokens  × input rate per million  / 1,000,000
  + output tokens × output rate per million / 1,000,000
```

For example, with **hypothetical** rates of $2/M input and $10/M output, 10,000 input tokens plus 1,000 output tokens cost approximately **$0.03**. These illustrative rates are not a current model price quotation.

## Smart Model Selection

The model selector displays context metadata and, when available, cost information. Use it as a comparison aid; do not rely on every model being automatically disabled whenever your full conversation would exceed its true API limit. The server/provider remains authoritative, and different execution paths may have different information available.

### Cost-Saving Strategies

- Keep requests focused and include only the code needed for the task.
- Remove unneeded attachments and start a fresh conversation when old context is no longer useful.
- Use retrieval or targeted file reads instead of repeatedly sending the entire project.
- Monitor provider-side usage, particularly for agent loops and large contexts.
- Use local inference where appropriate; it avoids provider API token charges but still uses your hardware and resources.

## Project Scanning and Token Management

Use [Scan & Copy Project](../features/project-scanner.md) to select extensions, exclude files/directories, respect `.gitignore`, and optionally omit JavaDoc. These preferences are per project.

There is no maximum-file-size, scan-depth, line-count, or token-limit control on that settings page. Narrow the selection and exclusions rather than looking for those settings.

### "Add Project" Feature

Adding full-project context can consume a large portion of the selected model's budget. Check the scan/token information, include only relevant source, and leave room for the rest of the request. A large context-window label is not a guarantee that the complete project plus conversation will fit.

The project is sent once per request in the system prompt, so follow-up prompts don't accumulate extra copies of it in chat memory. The whole snapshot is still sent (and billed as input tokens) on **every** request while it is active, so remove it once you no longer need it.

## Best Practices

1. Begin with a small, relevant context and expand only when necessary.
2. Check the exact model/version and hosting platform rather than applying one limit to every model from that provider.
3. Keep estimates separate from provider-reported usage and actual billing.
4. Configure provider-side spend controls if you need a hard budget; a cost display is not a spending cap.

## Troubleshooting

- **Token count differs from the provider**: Local tokenization and provider accounting can differ. There is no general tokenizer-adjustment setting on this page; use provider usage for billing and leave context headroom.
- **Context limit errors**: Reduce files/history and output length, or choose a model/server configuration with sufficient capacity. Check Custom OpenAI's context override if its usage bar incorrectly shows a 4K window.
- **Costs or limits look stale**: Reopen Token Cost & Context Window to request a catalogue refresh. Cached/fallback values may remain if the fetch fails. Verify the exact model against the official specification and pricing page.
- **Unexpected charges**: Review repeated agent requests and provider-specific billing categories. The simple estimate does not guarantee the final amount billed.
