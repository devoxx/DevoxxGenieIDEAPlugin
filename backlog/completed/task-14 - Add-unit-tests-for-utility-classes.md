---
id: TASK-14
title: Add unit tests for utility classes
status: Done
assignee: []
created_date: '2026-02-13 19:22'
updated_date: '2026-02-13 20:37'
labels:
  - testing
  - util
dependencies: []
references:
  - src/main/java/com/devoxx/genie/util/
  - src/main/java/com/devoxx/genie/ui/util/
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create unit tests for utility/helper classes with 0% coverage. These are often pure functions that are easy to test.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Unit tests for ImageUtil (17 lines)
- [ ] #2 Unit tests for DockerUtil (11 lines)
- [ ] #3 Unit tests for FileUtil (12 lines)
- [ ] #4 Unit tests for ThreadUtils (13 lines)
- [ ] #5 Unit tests for DefaultLLMSettingsUtil (7 lines)
- [ ] #6 Unit tests for ClipboardUtil (3 lines)
- [ ] #7 Unit tests for MessageBusUtil (6 lines)
- [ ] #8 Unit tests for LocalDateTimeConverter (4 lines)
- [ ] #9 Unit tests for CodeLanguageUtil (29 lines)
- [ ] #10 Unit tests for LanguageGuesser (14 lines)
- [ ] #11 Unit tests for WindowContextFormatterUtil (9 lines)
- [ ] #12 Unit tests for FileTypeIconUtil (35 lines)
- [ ] #13 All tests pass with JaCoCo coverage > 70% for these classes
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Created 7 test files: ImageUtilTest, LocalDateTimeConverterTest, CodeLanguageUtilTest, WindowContextFormatterUtilTest, DefaultLLMSettingsUtilTest, FileUtilTest, ThreadUtilsTest. All pass.
<!-- SECTION:FINAL_SUMMARY:END -->
