---
id: TASK-261.1
title: Document crucial gaps in recent plugin feature coverage
status: Done
assignee:
  - '@codex'
created_date: '2026-11-02T13:41:00Z'
updated_date: '2026-11-02T13:51:00Z'
labels: []
dependencies: []
modified_files:
  - docusaurus/docs/configuration/privacy-and-analytics.md
  - docusaurus/docs/configuration/debugging.md
  - docusaurus/docs/features/docker-agentic-platform.md
  - docusaurus/docs/features/chat-interface.md
  - docusaurus/docs/llm-providers/cloud-models.md
  - docusaurus/docs/llm-providers/custom-providers.md
  - docusaurus/docs/features/overview.md
  - docusaurus/docs/configuration/settings.md
  - docusaurus/docs/features/mcp_expanded.md
  - docusaurus/docs/getting-started/faq.md
  - docusaurus/sidebars.js
  - docusaurus/docs/configuration/prompts.md
  - docusaurus/docs/features/commands.md
  - docusaurus/docs/features/inline-completion.md
  - docusaurus/docs/features/sdd-agent-loop.md
  - docusaurus/docs/features/skills.md
  - docusaurus/docs/features/dnd-images.md
  - docusaurus/docs/features/cli-runners.md
  - docusaurus/docs/features/acp-runners.md
  - docusaurus/docs/getting-started/introduction.md
  - docusaurus/docs/features/agent-mode.md
parent_task_id: TASK-261
priority: medium
type: docs
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A second release-note and settings inventory pass found user-facing features absent from the reference docs: prompt queue/steering, privacy and analytics controls, debug/activity logs, Docker Agentic Platform preview, Bedrock authentication and Custom OpenAI compatibility controls. Users need discoverable operating instructions and safety caveats rather than release notes alone.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Document source-verified usage and limitations for crucial missing features found in the release/settings inventory.
- [x] #2 Add discoverable navigation and cross-links for new reference pages, and correct related overview omissions.
- [x] #3 Docusaurus builds and new/changed rendered pages and internal documentation links validate.
- [x] #4 Preserve existing documentation work and unrelated files; leave changes uncommitted.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Compare v1.8–v1.15 release notes, plugin.xml settings and current source against documentation; prioritize user workflows, authentication and privacy.
2. Add reference pages for privacy/analytics, debug/activity logs, and Docker Agentic Platform preview. Add queue/steer and attachment persistence guidance, Bedrock auth and Custom OpenAI controls; update settings names and feature overview/navigation.
3. Build Docusaurus, validate source-derived content and rendered internal links/anchors, preserve parent-task edits and finalize without commit/deployment.

Also cover the recent CLI/ACP image-path forwarding and persistent chat attachments, and normalize documented settings navigation to plugin.xml labels System Prompt & Personas, Agent Mode, and Inline Completion. These are part of the same source-verified feature-coverage pass.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Coverage inventory found existing guides already cover editable agent tool descriptions, write-approval diffs, changed-file review, command blacklist, semantic/web retrieval and per-project scanning; retain those rather than duplicate them. Added three new reference pages and linked missing workflow/authentication guidance.

Added source-verified fetch_page URL/redirect/download/output/timeout limits. Corrected the parallel exploration guide and diagrams: ReadOnlyToolProvider has only read_file/list_files/search_files, not fetch_page or PSI. Overview and introduction now describe main Agent Mode write/command/test capabilities accurately.

Verification: Docusaurus production build passed; all 31 combined changed reference pages exist in rendered output and 824 article-local documentation links/anchors resolve. Source-derived coverage checks validated sidebar entries, current settings labels, analytics gates, log retention, AP limitations, sub-agent tool subset, Bedrock auth and Custom OpenAI controls. Original audit regression checks and git diff --check passed.

Only documentation, sidebar and task records changed. Parent-task corrections, existing blog/PR commit and unrelated experimental plan preserved. No commit, push, deployment or live external CLI/cloud authentication tests performed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Closed crucial reference-documentation gaps from recent release notes and current settings. Added Privacy/Analytics, Debug/Activity Log and Docker Agentic Platform preview guides with sidebar navigation. Added Queue/Steer, persistent attachments, CLI/ACP image paths, Bedrock authentication, Custom OpenAI overrides and fetch_page limits. Corrected overview omissions, old settings labels and incorrect sub-agent capability diagrams. Production build and rendered-page/link/source-derived validation passed (31 combined pages, 824 internal links/anchors). Changes remain uncommitted and not deployed.
<!-- SECTION:FINAL_SUMMARY:END -->
