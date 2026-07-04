---
id: TASK-247
title: Genie YAML import/export for agent definitions (DockerAgents interop)
status: Done
assignee: []
created_date: '2026-07-04 10:30'
updated_date: '2026-07-04 13:15'
labels:
  - agent-mode
  - agent-team
  - interop
dependencies:
  - TASK-241
  - TASK-245
references:
  - docs/specs/agent-team-orchestration.md
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Phase 4a of the Agent Team orchestration spec (docs/specs/agent-team-orchestration.md, §8).

The DockerAgents POC's `agents/*.yml` Genie format is the authoritative agent-spec shape:

```yaml
models:
  default: { provider: ollama, model: qwen3.6:... }
agents:
  root:
    model: default
    description: ...
    instruction: |
      ...
    toolsets:
      - type: shell
      - type: filesystem
        readonly: true
```

Add Import/Export on the Agent Team settings tab:

- **Import**: parse a Genie YAML into an `AgentDefinition` — map `models.<key>.provider/model`
  to the closest DevoxxGenie `ModelProvider` (ollama→Ollama, anthropic→Anthropic,
  lmstudio→LMStudio, llamacpp→LLaMA, openai-compatible fallbacks), `toolsets` to toolset
  presets (`shell`→shell preset, `filesystem[+readonly]`→filesystem/-ro, `fetch`→fetch),
  `instruction`→persona. Unknown providers/toolsets import as warnings with sensible fallbacks.
- **Export**: serialize an `AgentDefinition` back to the same shape so personas edited in the
  IDE run unchanged in a DockerAgents deployment.
- Round-trip stability for the five shipped personas.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 Importing DockerAgents' reviewer.yml yields a read-only reviewer definition bound to Ollama.
- [x] #2 Export → import round-trips without loss for shipped personas.
- [x] #3 Unknown provider/toolset values degrade gracefully with a user-visible warning.
<!-- AC:END -->

## Implementation Notes

Implemented on branch claude/devoxxgenie-multi-agent-setup-iahcby.
- `service/agent/team/GenieAgentSpecMapper`: fromYaml(name, yaml) → ImportResult{definition, warnings}; toYaml(definition). Provider map (ollama/lmstudio/llamacpp/gpt4all/jan/exo/customopenai/anthropic/openai/google|gemini/mistral/groq/deepseek/openrouter/kimi); unknown providers (codex, copilot) degrade to inherit + warning. Toolsets: filesystem[+readonly]/shell/fetch map to presets; `analysis` accepted as a DevoxxGenie extension so shipped personas round-trip; todo/mcp/unknown → warnings. `readOnly` derived as "nothing writable granted". Missing models block = inherit, no warning.
- snakeyaml 2.3 added to build.gradle.kts (SafeConstructor for parsing).
- Settings UI: Import YAML…/Export YAML… buttons on the Agent Team page (FileChooser/FileSaver, name sanitized from filename + uniqued, warnings dialog).
- Tests: GenieAgentSpecMapperTest — real reviewer.yml shape import, readOnly derivation, unknown-provider/toolset warnings, malformed-doc rejection, round-trip of all four shipped personas and of an Ollama binding (green).
