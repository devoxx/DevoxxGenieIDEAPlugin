---
type: Groma Flow
title: Ask the LLM in agent mode
groma:
  id: ask-the-llm-in-agent-mode
---

A developer submits a prompt in the DevoxxGenie tool window with agent mode on. The prompt execution pipeline resolves the chat model, attaches the agent tool chain (built-in, PSI and MCP tools), streams the LLM response and tool results into the conversation view.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Developer](../actors/developer.md) | [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | Type a prompt and submit |
| [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | Build the chat message context and start execution |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [Model factory registry](../systems/devoxxgenieideaplugin/containers/plugin/components/model-registry.md) | Resolve the selected provider and model |
| [Model factory registry](../systems/devoxxgenieideaplugin/containers/plugin/components/model-registry.md) | [Cloud provider adapters](../systems/devoxxgenieideaplugin/containers/plugin/components/cloud-llm-adapters.md) | Create the streaming chat model |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [Chat memory](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-memory.md) | Load earlier turns |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [Agent runtime](../systems/devoxxgenieideaplugin/containers/plugin/components/agent-runtime.md) | Attach the agent tool provider |
| [Agent runtime](../systems/devoxxgenieideaplugin/containers/plugin/components/agent-runtime.md) | [Built-in agent tools](../systems/devoxxgenieideaplugin/containers/plugin/components/builtin-agent-tools.md) | Register built-in tools |
| [Agent runtime](../systems/devoxxgenieideaplugin/containers/plugin/components/agent-runtime.md) | [MCP client integration](../systems/devoxxgenieideaplugin/containers/plugin/components/mcp-client.md) | Add tools from MCP servers |
| [Cloud provider adapters](../systems/devoxxgenieideaplugin/containers/plugin/components/cloud-llm-adapters.md) | [Cloud LLM providers](../externals/cloud-llm-providers.md) | Send the request and stream tokens and tool calls |
| [MCP client integration](../systems/devoxxgenieideaplugin/containers/plugin/components/mcp-client.md) | [MCP servers](../externals/mcp-servers.md) | Execute an MCP tool call the LLM requested |
| [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | [Conversation view](../systems/devoxxgenieideaplugin/containers/plugin/components/conversation-view.md) | Stream the response into the chat |
