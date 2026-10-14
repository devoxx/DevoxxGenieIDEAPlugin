---
type: Groma Flow
title: Trigger a prompt from an IDE event
groma:
  id: trigger-a-prompt-from-an-ide-event
---

A configured IDE event (here a VCS commit) renders a prompt template and submits it to the chat tool window, which runs it like a typed prompt.

## Steps

| From | To | Action |
| --- | --- | --- |
| [IntelliJ Platform](../externals/intellij-platform.md) | [Event automations](../systems/devoxxgenieideaplugin/containers/plugin/components/event-automations.md) | Notify a commit |
| [Event automations](../systems/devoxxgenieideaplugin/containers/plugin/components/event-automations.md) | [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | Submit the rendered prompt |
| [Chat tool window and prompt input](../systems/devoxxgenieideaplugin/containers/plugin/components/chat-tool-window.md) | [Prompt execution](../systems/devoxxgenieideaplugin/containers/plugin/components/prompt-execution.md) | Execute the prompt |
