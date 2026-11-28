---
id: TASK-285
title: >-
  Agent search_files holds the read lock for the whole search and freezes the
  IDE
status: In Progress
assignee: []
created_date: '2026-11-26T15:32:00Z'
updated_date: '2026-11-26T15:39:00Z'
labels:
  - agent
  - bug
  - edt
  - performance
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutor.java
  - src/main/java/com/devoxx/genie/util/ReadAccess.java
modified_files:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/SearchFilesToolExecutorTest.java
  - src/main/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/ListFilesToolExecutorTest.java
priority: high
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On 2026-11-26 the IDE froze for 18 s (freeze report threadDumps-freeze-20261126-163009, IU-263.7307.353) during an agent run. The EDT was waiting for the write lock (DefaultProjectTimed.dispose → runWriteAction). In all three thread dumps (5 s apart), a JobScheduler thread was inside SearchFilesToolExecutor.searchInFile, reading file contents.

Cause: SearchFilesToolExecutor.execute wraps the entire search in one ReadAccess.compute(...) read action: the recursive directory walk (searchInDirectory) and reading and regex-matching every file line by line (searchInFile). A write action can't start until that finishes, so any write (typing, saving, refactoring, platform housekeeping) blocks the EDT for the whole search.

Why it was slow: the search skips only a fixed list of folder names (.git, node_modules, build, out, target, .idea, bin, .gradle). It ignores folders the IDE excludes, and here it walked .intellijPlatform/, which holds downloaded IntelliJ distributions and plugin sandboxes.

ListFilesToolExecutor walks the tree in the same way and should be checked. The other agent tools that use ReadAccess are out of scope unless they show the same pattern.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 search_files never holds a read action while reading or matching file contents; directory listings use short read actions, so a write action can run while a search is in progress
- [x] #2 search_files skips folders the IDE treats as excluded or ignored (project file index), and the fixed skip list also covers .intellijPlatform
- [x] #3 list_files is checked for the same pattern and fixed the same way if it holds one read action for a whole tree walk
- [x] #4 Search results are unchanged for normal project files (same matches, order, limits and path-outside-project checks)
- [x] #5 Tests written first: a write action can start while a search is running, and excluded folders are not searched
- [ ] #6 Verified manually: an agent search over this repository no longer freezes the IDE
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fix 2026-11-26:
- SearchFilesToolExecutor: no read action around the whole search. Each VFS lookup (project root, search folder, each directory listing, the exclusion check) takes its own short read action through an overridable readAction(...) seam, which runs directly without an IntelliJ application (plain unit tests). File contents are read without any lock: openContent() reads local files with java.nio (toNioPath), other files through VirtualFile.getInputStream().
- Folders the IDE excludes or ignores are skipped (ProjectFileIndex.isExcluded, best effort: on error the folder is searched as before). SKIP_DIRS also covers .intellijPlatform.
- ListFilesToolExecutor had the same single read action around a recursive walk (bounded by MAX_ENTRIES = 500, so less severe), and a recursive listing here filled the limit with .intellijPlatform entries. Same fix: a short read action per lookup, IDE exclusions and .intellijPlatform skipped.
- Tests written first, failing before the fix: SearchFilesToolExecutorTest (contents read with no read action held, listings inside one; IDE-excluded folders and .intellijPlatform skipped) and ListFilesToolExecutorTest (no nested or whole-walk read action; excluded folders not listed). The SKIP_DIRS constant tests are updated. Full ./gradlew test --rerun passes (JDK 21).
- Open: AC #6, a manual check that an agent search over this repository no longer freezes the IDE.
<!-- SECTION:NOTES:END -->
