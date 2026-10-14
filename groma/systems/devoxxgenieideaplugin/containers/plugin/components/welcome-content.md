---
type: C4 Component
title: Welcome screen content
status: stable
groma:
  id: welcome-content
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/tips/Tip.java
      symbol: Tip
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/tips/TipConfig.java
      symbol: TipConfig
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/blog/BlogFeedService.java
      symbol: BlogFeedService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/blog/BlogPost.java
      symbol: BlogPost
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/tips/TipService.java
      symbol: TipService
  group: Chat experience
description: Blog posts and tips shown on the welcome screen
---

Provides the bundled list of blog posts, refreshed from the documentation site's RSS feed and cached, and rotating usage tips.
