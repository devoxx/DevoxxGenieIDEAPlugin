---
id: TASK-261
title: Align Docusaurus reference documentation with plugin 1.15.1
status: Done
assignee:
  - '@codex'
created_date: '2026-11-02T13:33:00Z'
updated_date: '2026-11-02T13:40:00Z'
labels: []
dependencies: []
references:
  - build.gradle.kts
  - src/main/java/com/devoxx/genie/service/skill/SkillRegistry.java
  - src/main/java/com/devoxx/genie/service/spec/SpecService.java
  - src/main/java/com/devoxx/genie/service/security/GitleaksScanner.java
modified_files:
  - docusaurus/docs/getting-started/installation.md
  - docusaurus/docs/getting-started/faq.md
  - docusaurus/docs/getting-started/troubleshooting.md
  - docusaurus/docs/features/inline-completion.md
  - docusaurus/docs/features/security-scanning.md
  - docusaurus/docs/features/spec-driven-development.md
  - docusaurus/docs/features/chat-memory.md
  - docusaurus/docs/features/project-scanner.md
  - docusaurus/docs/features/skills.md
  - docusaurus/docs/features/web-search.md
  - docusaurus/docs/features/rag.md
  - docusaurus/docs/features/commands.md
  - docusaurus/docs/features/mcp_expanded.md
  - docusaurus/docs/configuration/appearance.md
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/contributing/development.md
  - docusaurus/docs/configuration/prompts.md
  - docusaurus/docs/features/chat-interface.md
  - docusaurus/docs/llm-providers/overview.md
  - docusaurus/docs/llm-providers/cloud-models.md
priority: medium
type: docs
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The approved documentation audit found incorrect compatibility, security coverage, task lifecycle, settings and Skills guidance plus missing feature documentation. Users need instructions that match the current plugin rather than obsolete behavior.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Correct all confirmed discrepancies in the approved audit across affected reference pages.
- [x] #2 Document Personas, Favorites, Show Thinking, NVIDIA provider, and curated Skills discovery links with navigable internal links.
- [x] #3 Docusaurus production build succeeds and regression checks find no audited obsolete claims in current reference docs.
- [x] #4 Changes remain documentation-only and preserve unrelated working tree files.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Apply the approved audit corrections to compatibility, security, SDD lifecycle, memory, scanning, Skills, web search, RAG, reset semantics and developer API examples.
2. Add source-verified reference guidance for Personas, Favorites, Show Thinking and NVIDIA, and link the Skills discovery sources.
3. Check affected pages and source-derived assertions; build Docusaurus and inspect generated pages; review diff and finalize task. Documentation only, leave uncommitted.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Existing implementation audit approved by user. Keep historical blog posts unchanged; update current reference pages. Preserve unrelated untracked plan and existing PR branch.

Verified all 20 modified reference pages against the approved audit and source. Corrected the second occurrence of the old Backlog tool count and clarified that initialization still creates completed/ but completion does not move files there.

Docusaurus production build passed. Source-backed regression assertions passed for IDE build, memory/RAG defaults, all 20 Backlog tools and all 7 curated skill URLs. Parsed rendered HTML: all 20 pages exist, 553 article-local documentation links/anchors resolve, and Personas/Favorites/Thinking/NVIDIA/Skills discovery anchors are generated. git diff --check passed.

Only docusaurus/docs files plus this task record changed; no plugin source, historical blog, sidebar or unrelated plan edits. Build used baked-in stats after remote fetch failures and emitted a non-blocking update-check permissions warning. No commit, push or deployment.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Updated 20 Docusaurus reference pages to match plugin 1.15.1: corrected IDE compatibility, Gitleaks scope, task lifecycle/tool inventory, memory, scanner settings, Skills caching/resource access, web search, RAG, reset semantics, MCP transport and provider API examples. Added linked reference guidance for Personas, Favorites, Show Thinking, NVIDIA and Skills discovery. Production build, source-derived regression assertions, rendered-page/link checks and whitespace validation passed. Changes remain uncommitted and are not yet deployed.
<!-- SECTION:FINAL_SUMMARY:END -->
