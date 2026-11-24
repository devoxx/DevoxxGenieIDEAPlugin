---
id: TASK-278
title: 'Spike: pluggable execution backend for sandboxing agent commands'
status: To Do
assignee: []
created_date: '2026-11-24T10:00:00Z'
labels:
  - spike
  - security
  - agent
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/agent/tool/RunCommandToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/RunTestsToolExecutor.java
  - src/main/java/com/devoxx/genie/service/cli/CliTaskExecutorService.java
  - src/main/java/com/devoxx/genie/service/acp/protocol/AcpTransport.java
  - src/main/java/com/devoxx/genie/service/agent/CommandBlacklist.java
  - src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/transport/StdioTransportPanel.java
  - https://www.brig.sh/docs/
  - https://github.com/brig-sh/brig
priority: medium
type: task
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Every process the agent starts runs on the host with the user's full rights: `run_command` and `run_tests` (`/bin/bash -c` or `cmd.exe /c` in the project directory), the external CLI runners (Claude Code, Codex, Gemini, ...), ACP agents and MCP stdio servers. The only guard on `run_command` is `CommandBlacklist`, which matches words in the command text. It catches mistakes but not a hostile or prompt-injected model (`bash -c "$(echo ...)"`, variables, or a script written with `write_file` and then run). Autonomous and auto-approve modes make this worse.

This spike decides whether to add a pluggable execution backend so these processes can run inside a sandbox, and which backends are worth building. Candidates:
- **Host**: today's behaviour, and always the default.
- **OS-level sandbox**: Seatbelt (`sandbox-exec`) on macOS, bubblewrap or Landlock on Linux. Starts in milliseconds and keeps the host toolchain. This is roughly what Claude Code and Codex CLI do.
- **MicroVM via Brig** (https://www.brig.sh, Apache-2.0): `hvi` VMM on Hypervisor.framework or KVM, network allow-list enforced outside the guest, credential broker, built-in profiles for claude, codex, gemini and opencode. It is pre-1.0, unaudited and CLI-only, and supports only Apple-silicon macOS 14+ and Linux with KVM (no Windows, no Intel Mac).
- **Container**: Docker or Podman, or a devcontainer. The fallback that also works on Windows.

Known limit for every backend: the project folder (`.git` included) stays writable, so a sandbox protects the rest of the machine, not the repo.

The output is a written recommendation with measurements, not production code. Throwaway prototype code stays on a spike branch.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A proposed `CommandExecutionBackend` interface (or equivalent) is sketched, with the call sites it would replace in RunCommandToolExecutor, RunTestsToolExecutor, CliTaskExecutorService, AcpTransport and MCP stdio, and with the timeout, output-capture, process-tree-kill and shell-env-file behaviour each depends on today
- [ ] #2 It is confirmed whether Brig can run a single command inside an already running sandbox (needed for per-command `run_command`); if not, the workaround or blocker is stated
- [ ] #3 Brig cold-start and warm-start times, and per-command overhead in a long-lived sandbox, are measured on Apple-silicon macOS and on Linux with KVM
- [ ] #4 A CLI runner (Claude Code or Codex) is run through `brig run <profile> <projectDir>` from the plugin's CLI runner path, confirming that prompt delivery, streamed output, MCP config and task completion still work
- [ ] #5 `./gradlew test` (or the project's run_tests command) is tried inside a Brig guest on macOS, with findings on JDK/Gradle availability, cold caches and native-binary mismatches (e.g. node_modules built on macOS)
- [ ] #6 An OS-level sandbox prototype for `run_command` (Seatbelt on macOS and/or bubblewrap on Linux) is tried, with its startup overhead and what it blocks (writes outside the project, network)
- [ ] #7 The Windows story is stated: which backend, if any, covers Windows users
- [ ] #8 A recommendation is recorded in the Final Summary: which backends to build, in which order, which spawn points come first, the settings/UX shape (per-project toggle, unavailable-platform state), and follow-up tasks are created for the chosen work
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Background (2026-11-24): initial pros/cons and SWOT discussed in chat. Working recommendation going into the spike: build the backend interface, ship OS-level sandboxing first for run_command/run_tests, offer Brig as an opt-in microVM backend starting with CLI runners in autonomous/skip-permissions modes, and keep Docker/devcontainers as the cross-platform fallback. Brig pages not yet read: install, runtimes, policies, broker, stability, changelog.
<!-- SECTION:NOTES:END -->
