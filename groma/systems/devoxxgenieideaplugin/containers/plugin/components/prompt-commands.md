---
type: C4 Component
title: Prompt slash commands
status: stable
groma:
  id: prompt-commands
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/Command.java
      symbol: Command
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/FindCommand.java
      symbol: FindCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/HelpCommand.java
      symbol: HelpCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/PromptCommand.java
      symbol: PromptCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/PromptCommandProcessor.java
      symbol: PromptCommandProcessor
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/SearchCommand.java
      symbol: SearchCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/command/UserCommand.java
      symbol: UserCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/processor/CommandProcessor.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/commands/CommandsSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/commands/CommandsSettingsConfigurable.java
      symbol: CommandsSettingsConfigurable
  group: Chat experience
description: Handles /help, /find, /search, /init and user-defined commands
---

Parses a prompt that starts with `/`, runs built-in commands (help, RAG find, web search, DEVOXXGENIE.md init) or expands custom user prompt templates before execution. Includes the settings page for custom commands.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/processor/CommandProcessor.java](../../../../../../src/main/java/com/devoxx/genie/ui/processor/CommandProcessor.java) | [src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java](../../../../../../src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java) | Generates DEVOXXGENIE.md | Java call |
