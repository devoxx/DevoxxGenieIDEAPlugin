---
id: TASK-192
title: Update Docusaurus static API model lists with latest Google and OpenAI models
status: To Do
assignee: []
created_date: '2026-03-07 15:14'
updated_date: '2026-03-07 15:19'
labels:
  - documentation
  - models
  - pricing
dependencies: []
references:
  - docusaurus/static/api/models.json
  - docusaurus/docs/llm-providers/cloud-models.md
  - 'https://developers.openai.com/api/docs/pricing'
  - 'https://ai.google.dev/gemini-api/docs/pricing'
  - 'https://ai.google.dev/gemini-api/docs/models'
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Summary
The `docusaurus/static/api/models.json` file and `docusaurus/docs/llm-providers/cloud-models.md` need updating with newly available LLMs from Google and OpenAI. Last updated: 2026-02-19.

## Research Findings (March 2026)

### Google Gemini - Models to ADD

| Model ID | Display Name | Input $/1M | Output $/1M | Context | Max Output |
|----------|-------------|-----------|-------------|---------|------------|
| `gemini-3.1-flash-lite-preview` | Gemini 3.1 Flash-Lite Preview | $0.25 | $1.50 | 1,048,576 | 65,536 |

### Google Gemini - Models to REMOVE/UPDATE
- **`gemini-3-flash-preview`** — Being deprecated, sunset March 9, 2026. Consider removing.
- **`gemini-3-pro-preview`** — Deprecated March 9, 2026, replaced by `gemini-3.1-pro-preview` (already in list). Remove.

### OpenAI - Models to ADD

| Model ID | Display Name | Input $/1M | Output $/1M | Context | Max Output |
|----------|-------------|-----------|-------------|---------|------------|
| `o3` | o3 | $2.00 | $8.00 | 200,000 | 100,000 |
| `o3-pro` | o3-pro | $20.00 | $80.00 | 200,000 | 100,000 |
| `o4-mini` | o4-mini | $1.10 | $4.40 | 200,000 | 100,000 |
| `gpt-5.4` | GPT 5.4 | $2.50 | $10.00 | 1,000,000 | 128,000 |
| `gpt-5.4-thinking` | GPT 5.4 Thinking | $2.50 | $15.00 | 1,000,000 | 128,000 |
| `gpt-5.4-pro` | GPT 5.4 Pro | $30.00 | $180.00 | 1,000,000 | 128,000 |

### OpenAI - Models to potentially REMOVE (deprecated)
- **`gpt-3.5-turbo`** — Long deprecated by OpenAI
- **`gpt-4`** — Deprecated, superseded by GPT-4o and GPT-4.1
- **`gpt-4-turbo-preview`** — Deprecated
- **`gpt-4o`** / **`gpt-4o-mini`** — Potentially deprecated in favor of GPT-4.1 family; verify before removing
- **`o1`** — Superseded by o3 family; verify availability

### Sources
- [OpenAI API Pricing](https://developers.openai.com/api/docs/pricing)
- [OpenAI Models](https://developers.openai.com/api/docs/models)
- [GPT-5.4 Model docs](https://developers.openai.com/api/docs/models/gpt-5.4)
- [GPT-5.4 Thinking announcement](https://9to5mac.com/2026/03/05/openai-upgrades-chatgpt-with-gpt-5-4-thinking-offering-six-key-improvements/)
- [o4-mini pricing](https://pricepertoken.com/pricing-page/model/openai-o4-mini)
- [Google Gemini API Pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Google Gemini Models](https://ai.google.dev/gemini-api/docs/models)
- [Gemini 3.1 Flash-Lite announcement](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-1-flash-lite/)
- [Gemini 3 Pro deprecation](https://ai.google.dev/gemini-api/docs/gemini-3)

## Files to Update
1. `docusaurus/static/api/models.json` — Add new models, remove deprecated ones, update `lastUpdated`
2. `docusaurus/docs/llm-providers/cloud-models.md` — Update model references in text if needed

## Notes
- Prices are per 1M tokens
- Verify exact model IDs against official API docs before committing
- GPT-5.4 has a 272K default context, 1M with experimental flag — document as 1M (matches OpenAI docs)
- GPT-5.4 Thinking has same input price as GPT-5.4 ($2.50/1M) but higher output ($15.00/1M vs $10.00/1M) due to reasoning tokens
- GPT-5.4 Pro is OpenAI's most expensive model at $30/$180 per 1M tokens
- Google Gemini 3 Pro Preview sunsets March 9, 2026 — time-sensitive removal
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All newly available Google and OpenAI models are added to models.json with correct model IDs, pricing, and token limits
- [ ] #2 Deprecated/sunset models are removed from models.json
- [ ] #3 cloud-models.md is updated to reflect the new model landscape
- [ ] #4 lastUpdated field in models.json is set to the date of the update
- [ ] #5 All prices verified against official provider pricing pages before committing
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Corrected OpenAI GPT-5.4 Models (verified from official docs)

**Source:** https://developers.openai.com/api/docs/models/gpt-5.4 and https://developers.openai.com/api/docs/pricing

### Models to ADD

#### OpenAI - GPT-5.4 Family
| Model ID | Context Window | Max Output | Input $/1M | Cached Input $/1M | Output $/1M |
|----------|---------------|------------|-----------|-------------------|-------------|
| `gpt-5.4` | 1,050,000 | 128,000 | $2.50 | $0.25 | $15.00 |
| `gpt-5.4-pro` | 1,050,000 | 128,000 | $30.00 | N/A | $180.00 |

**Extended context pricing (>272K input tokens):**
- `gpt-5.4`: Input $5.00, Cached $0.50, Output $22.50 (2x input, 1.5x output)
- `gpt-5.4-pro`: Input $60.00, Output $270.00

**Snapshot:** `gpt-5.4-2026-03-05`

**NOTE:** There is NO separate "gpt-5.4-thinking" model. The base `gpt-5.4` already includes reasoning/thinking capabilities.

#### OpenAI - Reasoning Models
| Model ID | Input $/1M | Cached Input $/1M | Output $/1M |
|----------|-----------|-------------------|-------------|
| `o3` | $2.00 | $0.50 | $8.00 |
| `o3-pro` | $20.00 | N/A | $80.00 |
| `o4-mini` | $1.10 | $0.275 | $4.40 |

#### Google
| Model ID | Input $/1M | Output $/1M |
|----------|-----------|-------------|
| `gemini-3.1-flash-lite-preview` | $0.25 | $1.50 |

### Models to REMOVE (deprecated)
- **Google:** `gemini-3-flash-preview`, `gemini-3-pro-preview` (sunset March 9, 2026)
- **OpenAI:** `gpt-3.5-turbo`, `gpt-4`, `gpt-4-turbo-preview` (deprecated)
- **OpenAI (verify first):** `o1`, `gpt-4o`, `gpt-4o-mini`

### Files to Update
- `docusaurus/docs/models/openai.md`
- `docusaurus/docs/models/google.md`
- Any related pricing/model list files in docusaurus/
<!-- SECTION:NOTES:END -->
