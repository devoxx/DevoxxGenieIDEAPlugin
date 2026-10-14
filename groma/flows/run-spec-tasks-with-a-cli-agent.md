---
type: Groma Flow
title: Run spec tasks with a CLI agent
groma:
  id: run-spec-tasks-with-a-cli-agent
---

A developer runs Backlog.md spec tasks from the spec browser. The runner orders them by dependencies, reads each spec and hands it to an external coding-agent CLI.

## Steps

| From | To | Action |
| --- | --- | --- |
| [Developer](../actors/developer.md) | [Spec browser](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-browser-ui.md) | Start a spec task run |
| [Spec browser](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-browser-ui.md) | [Spec task runner](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-task-runner.md) | Run selected tasks |
| [Spec task runner](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-task-runner.md) | [Spec and Backlog service](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-backlog-service.md) | Read task specs and dependencies |
| [Spec task runner](../systems/devoxxgenieideaplugin/containers/plugin/components/spec-task-runner.md) | [CLI runners](../systems/devoxxgenieideaplugin/containers/plugin/components/cli-runners.md) | Execute each task |
| [CLI runners](../systems/devoxxgenieideaplugin/containers/plugin/components/cli-runners.md) | [Coding agent CLIs](../externals/coding-agent-clis.md) | Launch the agent process |
