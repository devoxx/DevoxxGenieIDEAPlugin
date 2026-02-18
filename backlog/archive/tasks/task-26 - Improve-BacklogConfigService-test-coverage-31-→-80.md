---
id: TASK-26
title: Improve BacklogConfigService test coverage (31% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:27'
updated_date: '2026-02-14 09:33'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/BacklogConfigService.java
  - src/test/java/com/devoxx/genie/service/spec/BacklogConfigServiceTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
BacklogConfigService has 31% instruction coverage and 15% branch coverage. It has low IntelliJ Platform dependencies (
only Project.getBasePath() and DevoxxGenieStateService.getInstance()). Core logic is file I/O and YAML-like config
parsing/serialization.

Need to add tests covering:

- parseConfig() — full YAML-like config parsing with scalars, lists, inline arrays
- applyConfigScalar() / applyConfigList() — field mapping
- parseInlineArray() — bracket-delimited array parsing
- serializeConfig() / serializeInlineList() — round-trip serialization
- loadConfig() / saveConfig() — file I/O with @TempDir
- getNextTaskId() / getNextDocumentId() — ID generation by scanning filesystem
- ensureInitialized() — lazy initialization
- getConfig() / invalidateCache() — caching behavior
- Directory path accessors: getTasksDir(), getDocsDir(), getCompletedDir(), getArchiveTasksDir(),
  getArchiveMilestonesDir()
- initBacklog() — directory creation and default config
- parseIntOrDefault() — edge cases
- scanMaxId() — filesystem scanning for max task/doc ID
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage reaches 80%+
- [x] #2 Branch coverage reaches 65%+
- [x] #3 Tests cover config parsing and serialization round-trip
- [x] #4 Tests cover filesystem-based ID generation
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 36 new tests (43 total, up from 7) to BacklogConfigServiceTest covering:

**Config parsing (parseConfig):** scalar fields, inline arrays, empty arrays, multiline list items, milestones with name/description, simple milestone names, comments/blank lines, unknown fields, camelCase key variants

**Serialization (serializeConfig/serializeInlineList):** all fields, null project name, null milestones, empty lists

**Round-trip (save → invalidate → load):** all scalar fields, statuses/labels, milestones, empty lists

**Caching (getConfig/invalidateCache):** cache between calls, invalidate forces reload, saveConfig updates cache immediately

**ensureInitialized:** auto-initializes when not initialized, no-op when already initialized

**Directory path accessors:** correct paths, null base path returns null, null/empty/custom spec directory

**ID generation (getNextTaskId/getNextDocumentId):** no tasks → returns 1, scans tasks/completed/archive dirs, takes max across all dirs, custom prefix, empty prefix fallback, doc ID scanning

**scanMaxId edge cases:** ignores non-.md files, non-numeric suffix, ID without dash, quoted IDs, files without id frontmatter

**Error cases:** initBacklog/saveConfig throw IOException with null base path, isBacklogInitialized returns false with null base path
<!-- SECTION:FINAL_SUMMARY:END -->
