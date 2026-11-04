---
id: TASK-271
title: >-
  Offer model downloads for local embedding providers other than Ollama
  (optional)
status: To Do
assignee: []
created_date: '2026-11-04T13:42:00Z'
labels:
  - enhancement
  - RAG
milestone: m-0
dependencies:
  - TASK-265
references:
  - https://lmstudio.ai/docs/developer/rest/quickstart
  - https://www.simplified.guide/_export/xhtml/llama-cpp/server-load-model
  - https://www.mintlify.com/janhq/jan/features/api-server
priority: low
type: enhancement
ordinal: 15000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Optional future enhancement. TASK-265 offers to download a missing embedding model only for Ollama; other providers get a clear error when the model is missing or unusable. This task extends the download prompt to other local providers where their server supports it.

Findings on 2026-11-04 (recheck before starting):
- LM Studio documents `POST /api/v1/models/download` and a status endpoint (LM Studio 0.4+). It needs a bearer token, which would be a new credential, and job polling. Start here.
- llama.cpp router mode can load models already downloaded; a download through the API is unconfirmed.
- Jan: no download endpoint found, and sources disagree on whether it supports embeddings. Jan isn't an embedding provider yet.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 When a provider's server supports model downloads through an API, a missing embedding model prompts the user before downloading, as it does for Ollama
- [ ] #2 Declining or a failed download stops indexing and leaves the existing index untouched
- [ ] #3 Providers without download support keep TASK-265's clear error
<!-- AC:END -->
