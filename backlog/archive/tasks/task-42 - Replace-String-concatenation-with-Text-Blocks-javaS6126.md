---
id: TASK-42
title: "Replace String concatenation with Text Blocks (java:S6126)"
status: To Do
priority: medium
assignee: []
created_date: '2026-02-14 18:05'
labels:
  - code-quality
  - java
  - text-blocks
  - "rule-java:S6126"
dependencies: []
references:
  - /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
documentation: []
ordinal: 1000
---

Replace String concatenation with Text Blocks according to rule java:S6126. The issue is in ChatMemoryManager.java at line 310 where multiline strings are concatenated using + operators instead of using Text Blocks.

