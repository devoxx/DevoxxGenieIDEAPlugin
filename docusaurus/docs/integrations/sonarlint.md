---
sidebar_position: 2
title: SonarQube with DevoxxGenie
description: Fix SonarQube issues with AI assistance and create backlog task specs directly from SonarLint findings in IntelliJ IDEA.
keywords: [devoxxgenie, sonarlint, sonarqube, intellij plugin, ai fix, code quality, backlog, task creation]
image: /img/devoxxgenie-social-card.jpg
---

# SonarQube with DevoxxGenie

The **SonarLint DevoxxGenie** plugin is a fork of SonarLint for IntelliJ (v11.13) that adds a DevoxxGenie AI layer on top of the standard SonarQube analysis. It lets you fix SonarLint findings with a single click and optionally create structured backlog task specs for later resolution.

<a href="https://www.youtube.com/watch?v=vWEK0jEIU3s" target="_blank" rel="noopener noreferrer" style={{display: 'block', position: 'relative', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 4px 8px rgba(0,0,0,0.1)'}}>
  <img src="/img/integrations/sonarlint-banner.png" alt="SonarLint DevoxxGenie Demo" style={{width: '100%', display: 'block', borderRadius: '8px'}} />
  <div style={{position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', width: '68px', height: '48px', background: '#ff0000', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
    <svg viewBox="0 0 68 48" width="68" height="48"><polygon points="27,17 27,31 41,24" fill="#fff"/></svg>
  </div>
</a>

:::info Requirements
- **DevoxxGenie** v0.9.12 or later
- **IntelliJ IDEA** 2024.2 or later
- Both plugins installed and enabled in the same IDE instance
:::

---

## Overview

When SonarLint detects a code quality issue, the SonarLint DevoxxGenie plugin surfaces three ways to act on it with AI:

1. **Intention action** (lightbulb / Alt+Enter) — fastest path for a single violation
2. **Rule panel button** — from the SonarLint tool window while reviewing a rule
3. **Batch task creation** — create `TASK-*.md` files in `backlog/tasks/` for later AI-assisted resolution

---

## Entry Point 1: Intention Action

Press **Alt+Enter** (or click the lightbulb) on any SonarLint-highlighted code to see a "Fix with DevoxxGenie" intention action. Selecting it sends an AI-crafted fix prompt to DevoxxGenie with full context: the rule ID, rule description, affected code, and surrounding lines.

![Lightbulb intention action for SonarLint fix](/img/integrations/sonarlint-intention-action.png)

The prompt is submitted automatically — DevoxxGenie will respond with a suggested fix in the chat panel.

---

## Entry Point 2: Rule Panel Button

When you open the **SonarLint tool window** and select an issue, the rule detail panel includes a **"Fix with DevoxxGenie"** button in the header area.

![Fix with DevoxxGenie button in rule panel](/img/integrations/sonarlint-rule-panel-button.png)

Clicking this button:
1. Assembles a prompt with the rule name, severity, description, and the violating code snippet
2. Sends it to the active DevoxxGenie conversation
3. Focuses the DevoxxGenie tool window so you can review the response

---

## Entry Point 3: Create DevoxxGenie Task(s)

The SonarLint toolbar includes a **"Create DevoxxGenie Task(s)"** action. This does not immediately invoke the LLM — instead, it writes one or more `TASK-*.md` files into `backlog/tasks/` for deferred AI-assisted resolution via the [Spec-Driven Development](../features/spec-driven-development.md) workflow.

![Task creation toolbar action](/img/integrations/sonarlint-task-creation.png)

### What gets written

Each task file follows the [standard backlog format](./overview.md#file-format):

```markdown
---
id: TASK-7
title: Fix SonarLint java:S2259 in UserService.java:87
status: todo
priority: high
created: 2026-02-18
source: sonarlint
rule: java:S2259
file: src/main/java/com/example/UserService.java
line: 87
---

## Description

SonarQube rule **java:S2259** (Null pointers should not be dereferenced) triggered at
`UserService.java:87`.

## Acceptance Criteria

- [ ] Resolve the SonarLint finding without introducing regressions
- [ ] All existing tests pass
```

Task IDs are allocated by scanning all existing files in `backlog/tasks/`, `backlog/completed/`, and `backlog/archive/tasks/` to find the current highest `id:` value, then incrementing by one. See the [Task ID Synchronisation](./overview.md#task-id-synchronisation) section for details.

---

## Prompt Context

All three entry points build the prompt from the same set of contextual fields:

| Field | Source |
|---|---|
| Rule ID | SonarLint finding metadata |
| Rule name & description | SonarLint rule database |
| Severity / type | SonarLint finding metadata |
| File path & line number | Editor selection |
| Violating code snippet | ±10 lines around the finding |
| Project language | IntelliJ project model |

---

## Installation

1. Download the plugin JAR from [GitHub Releases](https://github.com/devoxx/sonarlint-devoxxgenie-intellij/releases)
2. In IntelliJ: **Settings → Plugins → Install Plugin from Disk…**
3. Select the downloaded JAR and restart the IDE
4. Ensure DevoxxGenie v0.9.12+ is also installed

:::note
This plugin replaces the standard SonarLint plugin. Uninstall the official SonarLint before installing this fork to avoid conflicts.
:::

---

## GitHub Repository

[github.com/devoxx/sonarlint-devoxxgenie-intellij](https://github.com/devoxx/sonarlint-devoxxgenie-intellij)
