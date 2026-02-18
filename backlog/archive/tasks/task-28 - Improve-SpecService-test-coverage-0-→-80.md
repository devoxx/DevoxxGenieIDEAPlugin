---
id: TASK-28
title: Improve SpecService test coverage (0% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 09:40'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/SpecService.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SpecService has 0% instruction and 0% branch coverage. It is the largest class in the package (627 lines, 56 methods) and manages task/document CRUD operations backed by markdown files on disk.

IntelliJ dependencies: Project, ApplicationManager.executeOnPooledThread(), VirtualFileManager/MessageBus (file watching), LocalFileSystem (VFS refresh). Will need refactoring to extract file I/O into overridable methods.

Key areas to test:
- Task CRUD: createTask(), updateTask(), completeTask(), archiveTask()
- Task queries: getAllSpecs(), getSpec(), getSpecsByStatus(), getStatuses(), getSpecsByFilters(), searchSpecs()
- Document CRUD: createDocument(), updateDocument(), getAllDocuments(), getDocument(), searchDocuments(), listDocuments()
- File management: refresh(), discoverAndParseSpecs(), discoverAndParseDocs(), scanTasksIn(), parseAndCacheSpec(), parseAndCacheDocument()
- Utility methods: buildTaskFileName(), parseDocument(), isSpecFileEvent(), getSpecDirectoryPath()
- Lifecycle: addChangeListener(), removeChangeListener(), dispose(), initFileWatcher()

Refactoring approach: Extract file watcher setup, VFS refresh, and static service lookups into overridable methods. Use @TempDir for filesystem tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage reaches 80%+
- [x] #2 Branch coverage reaches 65%+
- [x] #3 Tests cover task CRUD operations
- [x] #4 Tests cover document CRUD operations
- [x] #5 Tests cover query/filter/search methods
- [x] #6 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created SpecServiceTest with 51 tests covering SpecService (previously 0% coverage).

**Test infrastructure:** Created `MockContext` helper class managing MockedStatic for DevoxxGenieStateService, LocalFileSystem, plus Project mock with MessageBus/MessageBusConnection for the constructor's initFileWatcher(). BacklogConfigService wired via project.getService() mock.

**Task read operations (14 tests):** getAllSpecs (populated + empty), getSpec (by id, case-insensitive, not found), getSpecsByStatus (filter, case-insensitive), getStatuses (distinct, empty)

**getSpecsByFilters (8 tests):** filter by status, assignee, labels, search (title/id/description matches), limit, no filters, empty strings ignored

**searchSpecs (5 tests):** title+description matching, filter by status, filter by priority, limit, empty filters

**Task write operations (10 tests):** createTask (id generation, existing id, createdAt preservation, retrievable after creation, clean filename, blank title, null title), updateTask (writes changes, throws on null filepath), completeTask (sets Done, throws on not found), archiveTask (moves file, throws on not found)

**Document operations (10 tests):** getAllDocuments, getDocument (by id, case-insensitive, not found), createDocument (id generation, retrievable), updateDocument (content+title, keep title on null/empty, throws on not found), searchDocuments (title+content, limit), listDocuments (no search, with search)

**Refresh/cache (4 tests):** refresh reloads, null base path handling, change listeners (add/remove/notify), completed dir scanning, root-level md scanning (backward compat)

**Edge cases (4 tests):** specs without id skipped, specs without frontmatter skipped, docs without frontmatter skipped, docs without id skipped, dispose clears caches, hasSpecDirectory, acceptance criteria round-trip
<!-- SECTION:FINAL_SUMMARY:END -->
