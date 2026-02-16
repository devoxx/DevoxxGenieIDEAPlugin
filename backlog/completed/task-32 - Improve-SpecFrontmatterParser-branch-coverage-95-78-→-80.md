---
id: TASK-32
title: Improve SpecFrontmatterParser branch coverage (95%/78% → 80%+)
status: Done
assignee: []
created_date: '2026-02-14 09:28'
updated_date: '2026-02-14 10:21'
labels:
  - testing
  - spec-service
  - coverage
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/spec/SpecFrontmatterParser.java
  - src/test/java/com/devoxx/genie/service/spec/SpecFrontmatterParserTest.java
  - >-
    src/test/java/com/devoxx/genie/service/spec/SpecFrontmatterParserNewFieldsTest.java
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
SpecFrontmatterParser has excellent instruction coverage (95%) but branch coverage is 78%, just below the 80% target. It is a pure utility class with no IntelliJ dependencies.

Branch gaps to address:
- stripQuotes() — 62% branch (6 of 16 missed). Needs tests for single-quoted strings, escaped quotes, strings with only whitespace, empty quoted strings
- applyScalarField() — 85% branch (2 missed). Missing field name handling
- parseFrontmatter() — 76% branch (6 missed). Edge cases in YAML parsing: multi-line values, special characters in keys
- parse() — 66% branch (2 missed). Content without frontmatter delimiter, empty content
- applyListField() — 80% branch (2 missed). Edge cases for list field types
- parseDefinitionOfDone() — 75% branch (2 missed). Edge cases for checkbox parsing
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Instruction coverage stays at 95%+
- [x] #2 Branch coverage reaches 80%+
- [x] #3 Tests cover stripQuotes() edge cases
- [x] #4 Tests cover frontmatter parsing edge cases
- [x] #5 All tests pass
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 20 new tests to SpecFrontmatterParserTest covering:\n\nstripQuotes() edge cases:\n- empty string, single char, mismatched double/single quotes, unquoted value, double-quoted, single-quoted\n\napplyScalarField() alternate keys:\n- created_date, updated_date, parent (alternate key names)\n- ordinal with invalid number (NumberFormatException branch)\n\napplyListField() alternate keys:\n- label, dependency, reference, docs (singular/alternate forms)\n\nparseFrontmatter() edge cases:\n- line without colon, list item before any key, flush list on next scalar, trailing list flush\n\nparseSections() edge cases:\n- DoD section with no checkboxes, AC section with no checkboxes\n- DoD alternate header name, Plan/Notes/Summary alternate names\n- Explicit ## Description section, empty pre-section text\n\nResults: Branch coverage 78% → 84%, instruction coverage stable at 95%, line coverage 99%."
<!-- SECTION:FINAL_SUMMARY:END -->
