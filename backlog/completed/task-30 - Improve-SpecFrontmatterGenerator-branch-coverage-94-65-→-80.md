---
id: TASK-30
title: Improve SpecFrontmatterGenerator branch coverage (94%/65% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 09:55'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterGenerator.java
  - >-
    src/test/java/com/devoxx/genie/service/spec/SpecFrontmatterGeneratorTest.java
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SpecFrontmatterGenerator has excellent instruction coverage (94%) but branch coverage is only 65%. It is a pure utility class with no IntelliJ dependencies — all methods are static.

Branch gaps to address:
- quoteIfNeeded() — 54% branch (11 of 24 branches missed). Needs tests for YAML special chars: colons, hashes, brackets, quotes within strings, empty strings, multiline values
- generate() — 77% branch (8 missed). Needs tests with null/empty optional fields in TaskSpec
- generateDocument() — 50% branch (4 missed). Needs tests with null/empty BacklogDocument fields
- appendScalar() — 62% branch. Test with null value edge case
- appendScalarQuoted() — 62% branch. Test with null value edge case
- appendList() — 70% branch. Test with null and empty lists
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage stays at 90%+
- [x] #2 Branch coverage reaches 80%+
- [x] #3 Tests cover quoteIfNeeded() edge cases (YAML special characters)
- [x] #4 Tests cover null/empty field handling
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 22 new tests to SpecFrontmatterGeneratorTest covering:\n- quoteIfNeeded() edge cases: #, single quotes, double quotes (with escaping), newlines, {, [, *, &, backslash escaping, plain values, special chars in lists\n- generate() null/empty field handling: empty description, empty AC/DoD lists, empty implementation plan/notes/summary, null scalar fields, empty string scalar fields, null lists\n- generateDocument() branches: null content, empty content, null id/title\n\nResults: Branch coverage 65% → 84%, instruction coverage stable at 94%, line coverage 100%."
<!-- SECTION:FINAL_SUMMARY:END -->
