---
type: C4 Component
title: CLI runners
status: stable
groma:
  id: cli-runners
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/chatmodel/local/clirunners/CliRunnersChatModelFactory.java
      symbol: CliRunnersChatModelFactory
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/spec/CliToolConfig.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/ClaudeStreamJsonParser.java
      symbol: ClaudeStreamJsonParser
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/CliConsoleManager.java
      symbol: CliConsoleManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/AbstractCliCommand.java
      symbol: AbstractCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/ClaudeCliCommand.java
      symbol: ClaudeCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/CliCommand.java
      symbol: CliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/CodexCliCommand.java
      symbol: CodexCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/CopilotCliCommand.java
      symbol: CopilotCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/CustomCliCommand.java
      symbol: CustomCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/GeminiCliCommand.java
      symbol: GeminiCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/cli/command/KimiCliCommand.java
      symbol: KimiCliCommand
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/strategy/CliPromptStrategy.java
  group: External agent runners
  technology: Process execution
description: Runs prompts through external coding-agent CLIs
---

Builds the command line for Claude Code, Codex, Copilot, Gemini, Kimi or a custom CLI, runs it as a process per task, parses Claude stream-json output and shows it in an IDE console.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java](../../../../../../src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java) | [coding-agent-clis](../../../../../externals/coding-agent-clis.md) | Launches CLI agents | Child process |
