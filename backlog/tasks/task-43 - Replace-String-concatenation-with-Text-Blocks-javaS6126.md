---
id: TASK-43
title: "Replace String concatenation with Text Blocks (java:S6126)"
status: To Do
priority: medium
assignee: []
created_date: '2026-02-14 18:06'
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

Replace String concatenation with Text Blocks according to rule java:S6126. The issue is in ChatMemoryManager.java in the buildSystemPrompt method where multiline strings are concatenated using + operators instead of using Text Blocks. This violates the code quality rule java:S6126 which recommends using Text Blocks for better readability and to avoid escaping issues.

