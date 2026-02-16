---
id: TASK-17
title: Improve ParallelExploreToolExecutor test coverage (0% → 70%+)
status: Done
assignee: []
created_date: '2026-02-14 08:23'
updated_date: '2026-02-14 08:34'
labels:
  - testing
  - agent-tools
  - coverage
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutor.java
  - >-
    src/test/java/com/devoxx/genie/service/agent/tool/ParallelExploreToolExecutorTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ParallelExploreToolExecutor has 0% instruction and 0% branch coverage (91 missed lines, 24 missed branches). The existing test only tests ToolArgumentParser.getStringArray(), not the executor itself.

Need to add tests covering:
- execute() method with valid parallel explore requests (multiple paths/patterns)
- Argument parsing and validation (missing/invalid arguments)
- Parallel file exploration logic
- Error handling for inaccessible directories
- Result aggregation from parallel explorations
- Edge cases: empty path lists, single path, large result sets
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage reaches 70%+
- [x] #2 Branch coverage reaches 50%+
- [x] #3 Tests cover execute() method directly
- [x] #4 Tests cover error/edge cases
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Results

**ParallelExploreToolExecutor coverage improved from 0% to 89% instruction / 75% branch coverage.**

### Tests Added (17 new tests, 8 existing kept = 25 total)

**Argument validation (4 tests):**
- Empty queries, missing key, invalid JSON, null queries

**Successful execution (2 tests):**
- Single query launches one sub-agent
- Multiple queries launch multiple sub-agents with correct results

**Parallelism cap (2 tests):**
- Queries exceeding max parallelism are capped
- Null parallelism setting falls back to default constant

**Timeout handling (2 tests):**
- Sub-agent timeout returns appropriate message
- Null timeout setting uses default

**Error handling (2 tests):**
- Sub-agent exception returns error message
- Mixed success/failure across multiple agents

**Cancellation (1 test):**
- Cancel propagates to active runners

**Result formatting (1 test):**
- Output has correct markdown headers and structure

**Event publishing (3 tests):**
- Debug logs enabled → events published
- Debug logs disabled → no publish attempt
- Project disposed → no publish attempt

### Approach
Used `MockedConstruction<SubAgentRunner>` to intercept sub-agent creation, `MockedStatic` for `DevoxxGenieStateService`, `ThreadPoolManager`, and `ApplicationManager` singletons. Real `ExecutorService` instances used for threading behavior.
<!-- SECTION:FINAL_SUMMARY:END -->
