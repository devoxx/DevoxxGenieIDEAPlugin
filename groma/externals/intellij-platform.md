---
type: C4 System
title: IntelliJ Platform
status: stable
groma:
  id: intellij-platform
  technology: IntelliJ Platform SDK
---

The host IDE. Supplies the tool-window, editor, PSI code model, VCS, build/test/run events, inline completion API, PasswordSafe credential storage, persistent settings and message bus the plugin builds on.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [intellij-platform](intellij-platform.md) | [src/main/java/com/devoxx/genie/service/automation/listeners/VcsCommitListener.java](../../src/main/java/com/devoxx/genie/service/automation/listeners/VcsCommitListener.java) | Notifies VCS events | IntelliJ listener |
