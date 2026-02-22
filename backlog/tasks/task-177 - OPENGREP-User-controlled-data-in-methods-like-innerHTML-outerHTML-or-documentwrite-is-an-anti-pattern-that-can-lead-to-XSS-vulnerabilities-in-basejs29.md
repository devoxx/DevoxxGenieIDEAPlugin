---
id: task-177
title: "[OPENGREP] User controlled data in methods like `innerHTML`, `outerHTML` or `document.write` is an anti-pattern that can lead to XSS vulnerabilities in base.js:29"
status: To Do
priority: medium
assignee: []
created_date: '2026-02-22 09:12'
labels:
  - security
  - opengrep
  - medium
dependencies: []
references: []
documentation: []
ordinal: 1000
---

**Rule:** javascript.browser.security.insecure-document-method.insecure-document-method
**Finding:** User controlled data in methods like `innerHTML`, `outerHTML` or `document.write` is an anti-pattern that can lead to XSS vulnerabilities
**Location:** /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin/src/main/resources/webview/js/base.js:29

**Remediation:** Review the flagged code pattern and apply the suggested fix from the rule documentation.

