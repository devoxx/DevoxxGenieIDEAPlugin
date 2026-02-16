---
id: TASK-27
title: Improve SpecContextBuilder test coverage (63% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 09:36'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/SpecContextBuilder.java
  - src/test/java/com/devoxx/genie/service/spec/SpecContextBuilderTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SpecContextBuilder has 63% instruction coverage and 50% branch coverage. It is a pure utility class with no IntelliJ Platform dependencies — all methods are static string builders.

Three methods to cover:
- buildContext(TaskSpec) — 70% instruction, 55% branch. Needs tests with various TaskSpec combinations: with/without dependencies, acceptance criteria, definition of done, implementation plan, notes, final summary, documentation, references, labels, milestone, assignee
- buildCliInstruction(TaskSpec) — 0% coverage. Needs tests for CLI workflow instruction generation
- buildAgentInstruction(TaskSpec) — 94% instruction, 50% branch. Needs branch coverage for null/empty field edge cases
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage reaches 80%+
- [x] #2 Branch coverage reaches 70%+
- [x] #3 Tests cover buildCliInstruction() (currently 0%)
- [x] #4 Tests cover all TaskSpec field combinations
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 27 new tests (30 total, up from 3) to SpecContextBuilderTest covering all three public methods:

**buildContext():** complete spec with all fields, minimal spec, all-null fields, milestone (present/empty/null), references (present/empty), documentation (present/empty), implementation plan (present/empty), implementation notes (present/empty), final summary (present/empty), empty lists omit sections, description (empty/null)

**buildCliInstruction():** id+title present, null id, null title, both null, workflow steps content, MCP tool naming style

**buildAgentInstruction():** id+title present, null id, null title, both null, workflow steps content, underscore tool naming style

**Cross-method comparison:** CLI vs agent instruction naming conventions (space-separated vs underscore)
<!-- SECTION:FINAL_SUMMARY:END -->
