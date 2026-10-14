---
type: C4 Component
title: Agent skills
status: stable
groma:
  id: agent-skills
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/skill/SkillRegistry.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/skill/SkillsSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/skill/SkillsSettingsConfigurable.java
      symbol: SkillsSettingsConfigurable
  group: Agent mode
  technology: langchain4j skills
description: Loads langchain4j skills from skill directories
---

Scans user and project `.agents`, `.claude` and `.devoxxgenie` skill folders, with project folders taking priority, and exposes the skills to agent mode. Includes the skills settings page.
