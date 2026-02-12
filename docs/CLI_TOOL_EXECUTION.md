# CLI Tool Execution for Spec Task Runner

## Overview

The Spec Task Runner can now execute tasks via external CLI tools (GitHub Copilot CLI, Claude Code, Gemini CLI, Codex) instead of exclusively through the built-in LLM provider. CLI tools run as external processes, work directly on the filesystem, and use the Backlog MCP server to manage task status — the same completion detection mechanism used by the LLM path.

## Architecture

### Strategy Pattern

`SpecTaskRunnerService.submitNextTask()` delegates to one of two execution paths based on the user's selection:

```
submitNextTask()
  ├── submitTaskViaLlm()   →  message bus  →  langchain4j agent loop  →  backlog_task_edit(Done)
  └── submitTaskViaCli()   →  ProcessBuilder  →  CLI tool (with Backlog MCP)  →  task_edit(Done)
```

Both paths converge at `notifyPromptExecutionCompleted()` — the only difference is what triggers it.

### Completion Detection (Both Paths)

```
LLM Path:   prompt → langchain4j → backlog_task_edit(Done) → spec file changes
                                                                    ↓
                                                      onSpecsChanged() sets flag
                                                                    ↓
             ActionButtonsPanel.enableButtons() → notifyPromptExecutionCompleted()
                                                                    ↓
                                                         advance immediately

CLI Path:   prompt → ProcessBuilder → CLI tool → task_edit(Done) → spec file changes
                                                                    ↓
                                                      onSpecsChanged() sets flag
                                                                    ↓
             process.waitFor() exits → notifyPromptExecutionCompleted()
                                                                    ↓
                                                         advance immediately
```

**Important**: `ActionButtonsPanel.enableButtons()` guards its call to `notifyPromptExecutionCompleted()` with `!runner.isCliMode()` so it only fires for the LLM path. Without this guard, the LLM prompt lifecycle (which still runs in the background) would trigger a spurious completion notification immediately after the CLI process starts, causing every task to be skipped via the grace timer.

If the CLI tool exits without marking the task Done, the existing grace timer fires after 3 seconds and skips to the next task.

### Prompt Construction

CLI tools receive the same structured prompt as the LLM provider:

1. **Instruction block** (`SpecContextBuilder.buildCliInstruction()`) — task identification, step-by-step workflow using Backlog MCP tools (set In Progress, check criteria, append notes, write summary, set Done)
2. **Context block** (`SpecContextBuilder.buildContext()`) — full `<TaskSpec>` XML with metadata, description, acceptance criteria, definition of done, dependencies, references, implementation plan/notes
3. **Implementation request** suffix

### Process Execution

`CliTaskExecutorService` runs the CLI tool as an external process:

- **Command format**: `[executablePath, promptFlag, prompt, ...extraArgs]`
- **Working directory**: project base path
- **Thread model**: pooled thread via `ApplicationManager.executeOnPooledThread()`
- **Output streaming**: stdout/stderr streamed line-by-line to an IntelliJ `ConsoleView` in the Run tool window
- **Debug logging**: command details, process PID, exit code, elapsed time, and first 5 lines of output are logged to the IDE log for troubleshooting
- **Cancellation**: `Process.destroyForcibly()` on cancel

## Configuration

### CLI Tool Settings

Settings > Spec Driven Development > CLI Runners

Each CLI tool entry has:

| Field | Description | Example |
|-------|-------------|---------|
| Name | Display name used in the mode selector | `copilot` |
| Executable Path | Absolute path to the CLI binary | `/opt/homebrew/bin/copilot` |
| Prompt Flag | Flag that precedes the prompt argument | `-p` |
| Extra Args | Additional arguments appended after the prompt | `--allow-all` |
| Enabled | Whether the tool appears in the mode selector | `true` |

A default **copilot** entry is pre-populated: `/opt/homebrew/bin/copilot -p "..." --allow-all`

### Execution Mode Selector

The Spec Browser toolbar includes a ComboBox with:
- **LLM Provider** — uses the built-in langchain4j agent loop (default)
- **CLI: {name}** — one entry per enabled CLI tool

Selection is persisted to `DevoxxGenieStateService` (`specRunnerMode` and `specSelectedCliTool`). The combo refreshes automatically when settings are applied (via `SETTINGS_CHANGED_TOPIC` subscription).

## Prerequisites

CLI tools **must have the Backlog MCP server installed** and configured. The tool needs access to the same backlog directory so it can:

1. Set task status to "In Progress"
2. Check off acceptance criteria as it works
3. Append implementation notes
4. Write a final summary
5. Set task status to "Done"

Without Backlog MCP, the CLI tool cannot mark tasks as complete, and the grace timer will skip tasks after 3 seconds.

## Files

### New Files

| File | Type | Description |
|------|------|-------------|
| `model/spec/CliToolConfig.java` | Model | CLI tool configuration (name, path, flag, args, enabled) |
| `service/spec/CliTaskExecutorService.java` | Service | Runs CLI processes, streams output, notifies on exit |
| `service/spec/CliConsoleManager.java` | Service | Manages ConsoleView in the Run tool window |

### Modified Files

| File | Changes |
|------|---------|
| `service/spec/SpecTaskRunnerService.java` | Added `cliMode` flag (with getter), `submitTaskViaCli()`, `findCliTool()`, CLI cancel support, debug logging |
| `service/spec/SpecContextBuilder.java` | Added `buildCliInstruction()` for CLI-specific prompt instructions |
| `ui/settings/DevoxxGenieStateService.java` | Added `cliTools`, `specRunnerMode`, `specSelectedCliTool` fields |
| `ui/settings/spec/SpecSettingsComponent.java` | Added CLI Runners section with table, Add/Edit/Remove dialog |
| `ui/panel/spec/SpecBrowserPanel.java` | Added execution mode ComboBox in toolbar, subscribes to `SETTINGS_CHANGED_TOPIC` to refresh combo |
| `ui/panel/ActionButtonsPanel.java` | Guarded `notifyPromptExecutionCompleted()` with `!runner.isCliMode()` to prevent LLM path from interfering with CLI execution |

## Testing

1. **Settings**: Open Settings > Spec Driven Development > verify CLI Runners section with pre-populated copilot entry
2. **Mode selector**: Open Spec Browser > verify ComboBox shows "LLM Provider" and "CLI: copilot"
3. **Single task CLI run**: Select "CLI: copilot", check one To Do task, click Run Selected:
   - Run tool window opens with console showing CLI output
   - CLI tool receives prompt with full task spec and backlog instructions
   - CLI tool uses Backlog MCP to mark task In Progress then Done
   - `onSpecsChanged()` detects Done, process exits, runner advances
4. **Batch run**: Run multiple tasks to verify sequential execution with dependency ordering
5. **Cancel**: Click Cancel during CLI execution to verify process is killed
6. **Grace timer**: If CLI exits without marking Done, grace timer fires and task is skipped
7. **Error handling**: Configure an invalid executable path to verify graceful error notification

## Troubleshooting

### Debug Logging

`CliTaskExecutorService` and `SpecTaskRunnerService` emit detailed logs to the IDE log (Help > Show Log in Finder). Key log lines to look for:

| Log Pattern | What It Tells You |
|-------------|-------------------|
| `CLI execute: task=..., promptLength=...` | Command details and prompt size |
| `CLI process started successfully (pid=...)` | Process launched; if missing, `ProcessBuilder.start()` failed |
| `CLI process exited: ... exitCode=..., elapsed=...ms` | How long the process ran and whether it succeeded |
| `CLI stdout [...] line N: ...` | First 5 lines of stdout (look for error messages from the CLI tool) |
| `CLI stderr [...] line N: ...` | First 5 lines of stderr |
| `CLI stdout-reader finished: 0 lines total` | CLI produced no output — likely crashed or rejected the prompt |
| `CLI mode: specRunnerMode=..., specSelectedCliTool=...` | Confirms the correct mode and tool are selected |

### Common Issues

| Symptom | Likely Cause |
|---------|-------------|
| Process exits in < 1 second with no output | Prompt exceeds OS argument length limit (~256KB on macOS). Consider shortening task descriptions. |
| All tasks skipped as "not marked Done" | CLI tool doesn't have Backlog MCP installed, or MCP server is not pointing to the correct backlog directory. |
| ComboBox only shows "LLM Provider" | CLI tools not saved — open Settings > Spec Driven Development > CLI Runners, verify entries, click Apply. |
| Grace timer skips tasks immediately | `ActionButtonsPanel.enableButtons()` firing in CLI mode — verify the `!runner.isCliMode()` guard is present. |
