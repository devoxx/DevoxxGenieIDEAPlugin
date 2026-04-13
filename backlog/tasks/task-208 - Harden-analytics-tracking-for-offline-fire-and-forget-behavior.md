---
id: TASK-208
title: Harden analytics tracking for offline fire-and-forget behavior
status: Done
assignee:
  - codex
created_date: '2026-04-13 12:25'
updated_date: '2026-04-13 12:29'
labels:
  - analytics
  - stability
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java
  - src/test/java/com/devoxx/genie/service/analytics/AnalyticsServiceTest.java
  - src/main/java/com/devoxx/genie/service/prompt/PromptExecutionService.java
  - src/main/java/com/devoxx/genie/ui/window/DevoxxGenieToolWindowContent.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Ensure anonymous usage analytics can never crash or interrupt the IntelliJ plugin when the user is offline, the analytics endpoint is unreachable, or analytics setup fails. Tracking must remain non-critical, fire-and-forget behavior from every public analytics entry point.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Public analytics tracking methods never throw to callers when state access, payload creation, scheduling, URI creation, or network delivery fails
- [x] #2 Analytics HTTP delivery remains asynchronous in production and never blocks the EDT or prompt/model-selection flow
- [x] #3 Offline, DNS, timeout, and non-2xx endpoint failures are swallowed and logged only at debug level
- [x] #4 Regression tests cover silent failure before scheduling and during network delivery
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Harden `AnalyticsService` public tracking entry points so analytics preconditions, state lookup, payload construction, endpoint parsing, client creation, and dispatch failures cannot propagate to callers.
2. Use asynchronous `HttpClient.sendAsync` for production delivery so analytics remains fire-and-forget without occupying IntelliJ pooled threads while offline or timing out.
3. Keep test-only synchronous injection for deterministic existing tests, and add async test coverage for failed delivery.
4. Update analytics call sites where needed so service lookup/tracking remains non-critical.
5. Run the focused analytics test class and update acceptance criteria based on verified behavior.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented analytics hardening in the plugin: production delivery now uses HttpClient.sendAsync, public tracking methods catch pre-send failures, and call sites use safe static entry points so service lookup/tracking remains non-critical. Added regression coverage for state lookup failure, invalid endpoint URI, synchronous network failure, and async network failure. Verified with `./gradlew -q test --tests com.devoxx.genie.service.analytics.AnalyticsServiceTest`.

Added explicit regression coverage for non-2xx analytics responses remaining silent, and reran `./gradlew -q test --tests com.devoxx.genie.service.analytics.AnalyticsServiceTest` successfully.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Summary:
- Hardened `AnalyticsService` so analytics precondition checks, state lookup, endpoint parsing, request dispatch, and HTTP failures are swallowed and logged at debug level instead of propagating to plugin callers.
- Switched production analytics delivery from IntelliJ pooled-thread blocking sends to `HttpClient.sendAsync`, keeping prompt/model-selection flows fire-and-forget when offline or when the endpoint is unreachable.
- Updated prompt execution and model-selection call sites to use safe analytics entry points.
- Added regression tests for state lookup failure, invalid endpoint URI, synchronous network failure, async network failure, and non-2xx endpoint responses.

Tests:
- `./gradlew -q test --tests com.devoxx.genie.service.analytics.AnalyticsServiceTest`
<!-- SECTION:FINAL_SUMMARY:END -->
