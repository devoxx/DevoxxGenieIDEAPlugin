# Generic OpenAI-compatible local providers

## Problem

Adding a local server that speaks the OpenAI API means copying a factory, a model service and two DTOs, then editing about 15 existing files. Nativ and Rapid-MLX (#1342) differ only in data. Routers solved the same problem with `RouterDefinition` and `RouterRegistry`.

## Goal

A typical OpenAI-compatible local server takes **one `ModelProvider` enum line and one `LocalProviderDefinition` entry**, enforced by a guard test. The first definitions are Nativ (migrated) and Rapid-MLX (new).

Jan, GPT4All, LLaMA.c++, LMStudio, Ollama and Exo are out of scope. They can move later if their code allows it.

## Design

### Definition

`LocalProviderDefinition` holds the provider, default URL, download URL, hint text, catalog field mapping, default context length, and an optional fallback-context hint. A non-null fallback hint means the provider gets a "Fallback Context" settings row.

| | Nativ | Rapid-MLX |
|---|---|---|
| Default URL | `http://localhost:8080/v1/` | `http://localhost:8000/v1/` |
| Context window | Not reported | `max_model_len`, then `context_window` |
| Chat filter | None | `capabilities` contains `text` |
| Fallback Context row | Yes | No |
| Default context length | 8000 | 8000 |

### Factory

One `OpenAiCompatibleChatModelFactory`, extending `LocalChatModelFactory`, so the existing "not running" handling and 15-second re-probe stay. It lists models from `<url>/models`. There is no per-provider model service or DTO.

Display names drop the organisation prefix (`mlx-community/x` shows as `x`), as Nativ does today.

### Shared catalog parser

The parser and `CatalogFields` move out of the routers package into `chatmodel.catalog`, so local providers don't depend on cloud routers. Routers keep their behaviour through a thin `RouterCatalogParser`.

An **empty** filter array now counts as a match, like an absent field. Older Rapid-MLX versions return `capabilities: []` for every chat model.

### Settings and state

Saved state uses three maps keyed by enum name: `localProviderUrls`, `localProviderEnabled` and `localProviderFallbackContext`. A blank or missing URL resolves to the definition's default, so an unedited URL isn't pinned to today's default.

On `loadState`, Nativ's legacy `nativModelUrl`, `isNativEnabled` and `nativFallbackContextLength` seed the maps when the map has no value. The legacy fields stay so old settings files still load. The enum constant `Nativ` keeps its name, so a saved provider and model selection carries over.

### Settings tabs

The tabs become `Local | Cloud Direct | Routers | Sharding | Custom OpenAI`.

- **Local** ends with: LLaMA.c++, Nativ, Rapid-MLX. Generic rows follow the registry and always come last, so Rapid-MLX stays at the bottom.
- **Sharding** is new and holds Exo's existing row and hint. This only moves the settings row: Exo keeps its `Type.LOCAL` provider type, saved settings, factory, cluster panel and place in the chat provider list.
- **Planned solutions** are listed below Exo as grayed-out, non-interactive rows marked "Coming soon": vLLM, SGLang, llama.cpp RPC, LocalAI and GPUStack. They are display only: no enum entry, saved state, factory or chat-list entry. A hint under them says OpenAI-compatible servers such as vLLM can already be used from the **Custom OpenAI** tab.

## Behaviour

- A disabled provider sends no requests and shows no "not running" notification.
- A response that isn't the expected JSON, for example another server on port 8000, is treated as "not running" and doesn't throw.
- Apply drops the provider's cached models when its URL or enabled state changes.
- Agent settings load these providers' models off the EDT through `ModelComboLoader`.

## Not tested

No one on this work has run a chat request against live Nativ or Rapid-MLX servers, so the maintainer's manual check covers that. Docs and release notes state it for Rapid-MLX.
