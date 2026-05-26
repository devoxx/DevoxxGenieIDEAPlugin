---
id: TASK-218
title: 'RAG query expansion: fix misleading "one extra LLM call" help text'
status: To Do
assignee: []
created_date: '2026-05-26 20:15'
labels:
  - rag
  - ui
  - docs
dependencies:
  - TASK-217
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

The help text under the **Query expansion** checkbox in `Settings → RAG` says:

> "Paraphrase the query into multiple variants and fuse the per-variant results (Reciprocal Rank Fusion). Improves retrieval on meta-style questions such as 'where do we discuss X?' at the cost of **one extra LLM call per RAG search**."

The checkbox label itself also says: *"Enable LLM query expansion (one extra LLM call per RAG search)"*.

This is misleading. The "Number of variants" spinner directly below controls how many additional queries are issued. With the default value of 3, each RAG search performs **N additional LLM calls**, not one.

## Expected Behavior

Help text and checkbox label should accurately reflect that the cost scales with the number of variants.

## Suggested Wording

- Checkbox label: `Enable LLM query expansion (extra LLM calls scale with the number of variants)`
- Help text trailing sentence: `... at the cost of one extra LLM call per variant per RAG search.`

(Exact wording is up to the maintainer — the requirement is that "one extra LLM call" is no longer stated as a fixed cost.)

## Files

- `src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java`
- Any associated resource bundles / message files used for the label and help text.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Checkbox label no longer claims a fixed 'one extra LLM call' cost when the variants spinner controls additional calls
- [ ] #2 Help text accurately describes that the number of extra LLM calls equals the number of variants
- [ ] #3 Wording reviewed for clarity and consistency with the rest of the RAG settings panel
<!-- AC:END -->
