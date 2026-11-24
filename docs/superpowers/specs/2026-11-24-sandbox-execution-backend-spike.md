# Sandboxed execution backend — Spike findings (TASK-278)

**Date:** 2026-11-24
**Status:** Linux and desk research done; Brig hands-on checks (#3, #4, #5) waiting on a run of
[`sandbox-spike/brig-check.sh`](sandbox-spike/brig-check.sh) on an Apple-silicon Mac or a KVM Linux host.

## Why

Every process the agent starts runs on the host with the user's full rights. The only guard on
`run_command` is `CommandBlacklist`, a word matcher that a prompt-injected model can step around
(`bash -c "$(echo …)"`, variables, or `write_file` a script and run it). The default Claude Code CLI
runner even starts with `--dangerously-skip-permissions`.

## 1. Spawn points and what each relies on (AC #1)

| Call site | How it starts the process | Behaviour a backend must keep |
|---|---|---|
| `RunCommandToolExecutor` | `ProcessBuilder([shell, "-c", cmd])`, `cmd.exe /c` on Windows. Already has a `ProcessStarter` seam. | Configurable shell; shell env file sourced with `.` (a host path, so it must be readable in the sandbox); stdin from the null device; stderr merged; 30 s timeout; `destroyWithDescendants`; output capped and drained with a 1 s grace. |
| `RunTestsToolExecutor` | `ProcessBuilder(["/bin/bash", "-c", cmd])`; ignores the shell setting. | Timeout from settings; stderr merged; writable build caches (`~/.gradle`, `~/.m2`, `node_modules`); network for dependency resolution. **Existing bugs:** see "Side findings". |
| `CliTaskExecutorService` | `ProcessBuilder(cliCommand.buildProcessCommand(...))` | Full `EnvironmentUtil` env plus per-tool env overrides (API tokens); prompt written to stdin; stdout and stderr read separately (stream-json parsing); MCP config passed as a **host temp file path outside the project**; `taskCompletedKill`. |
| `AcpTransport` | `ProcessBuilder(command)` | JSON-RPC over stdin/stdout; stderr inherited; full `EnvironmentUtil` env. |
| `MCPExecutionService.initStdioClient` | langchain4j `StdioMcpTransport` builds the process from a `List<String>` | **Argv only**: the plugin never sees a `ProcessBuilder`, so a backend must be able to rewrite argv. Env is the full `System.getenv()` plus a PATH prefix and custom env. |

### Proposed interface

```java
/** Where and how a process the agent asked for is run. */
public interface CommandExecutionBackend {
    @NotNull String id();                        // "host", "bubblewrap", "seatbelt", "container", "brig"
    @NotNull Availability availability();        // Available, or Unavailable("Brig does not run on Windows")

    /** Argv that runs the request inside the sandbox. Needed where we don't own process creation (MCP stdio). */
    @NotNull List<String> wrap(@NotNull ExecutionRequest request);

    /** Start the request. Default: ProcessBuilder(wrap(request)) with the request's env, dir and redirects. */
    @NotNull SandboxedProcess start(@NotNull ExecutionRequest request) throws IOException;
}

public record ExecutionRequest(
        @NotNull Purpose purpose,                 // RUN_COMMAND, RUN_TESTS, CLI_RUNNER, ACP_AGENT, MCP_STDIO
        @NotNull List<String> command,            // argv; shell commands arrive as [shell, "-c", cmd]
        @NotNull Path projectRoot,                // the one writable project mount
        @NotNull Path workingDir,                 // inside projectRoot
        @NotNull Map<String, String> env,         // complete env; a backend never adds host env on its own
        @NotNull NetworkPolicy network,           // NONE | ALLOW(hosts) | HOST
        @NotNull List<Path> writableCaches,       // ~/.gradle, ~/.m2, ...
        @NotNull List<Path> readOnlyExtras,       // shell env file, CA bundle, MCP config file
        boolean mergeStderr,
        boolean stdinFromNull) {}

/** A started process plus a way to stop everything it spawned (in a VM or container, not just the client CLI). */
public interface SandboxedProcess {
    @NotNull Process process();
    void destroyTree();
}
```

`HostBackend` (today's behaviour) is always available and is the default. Selection is per project,
with a per-`Purpose` override so a user can sandbox CLI runners but not `run_command`, for example.
`readOnlyExtras` exists because three callers depend on host files outside the project: the shell
env file, the CLI runner MCP config, and (in some setups) a proxy CA bundle.

## 2. Brig (AC #2, #7, desk research)

Read: install, runtimes, policies, broker, stability, changelog, agents, cli, guest-image, quickstart,
sessions, brig-vs-docker, compare, and the GitHub README. The latest version is **v0.6.0, released 2026-11-20**.

- **Running one command in a running sandbox (#2): not supported for our use yet.** `brig sh <ref> -c '<script>'`
  "opens a login shell inside the sandbox"; the stability page keeps the retired `brig exec` spelling
  "until brig sh can pipe a command's output". The docs don't say whether the guest's exit code comes back.
  So `run_command` and `run_tests` can't go through Brig one command at a time today. **Workaround:** run the
  whole agent inside Brig (the CLI runner and ACP paths). `brig-check.sh` checks the piping and exit-code
  behaviour by hand.
- **Machine-readable surface:** `--json` on read verbs and on `run`/`sh`, in a versioned envelope
  (`apiVersion: brig.sh/v1alpha1`). Exit codes are documented (e.g. 4 = no usable runtime, 7 = isolation or
  policy refusal, 8 = Brigfile target not approved). The command grammar, `BRIG_*` settings, profile schema,
  JSON envelope and exit codes are declared stable surfaces. Pre-1.0 minor releases can still break.
- **Boot time:** no numbers anywhere. "A sandbox boots a kernel, so a cold start takes longer than a
  process sandbox." `BRIG_READY_TIMEOUT` defaults to 30 s. Measured by `brig-check.sh` (#3).
- **Toolchains:** custom OCI images through `--image` or a profile (`genericBoot: true`, `mem`, `cpus`). Only
  the guest home (`~/.brig/homes/<sandbox>`) persists; other root-filesystem writes are lost on
  `brig stop`, so `~/.gradle` must live under the guest home to stay warm. Nothing about JDK images is documented.
- **Network:** `Policy` resources with `egress.default: deny` and `allow: [{host: …}, {cidr: …}]`, enforced
  outside the guest (hvi on macOS; hull or nerdctl on Linux; other runtimes refuse with exit 7). The docs
  don't say whether a **host Ollama/LM Studio** (localhost:11434) is reachable, which matters for local-LLM users.
- **Credentials:** a broker keeps keys on the host (`brig secret import claude-code`); the guest gets
  `ANTHROPIC_BASE_URL=https://brig.internal/...` and a 12 h session token. That is better than our CLI runner
  passing tokens through env, but it is a different wiring from `CliToolConfig.envVars`.
- **CLI runner fit (#4, from the code):** `brig run <ref> <project> -- <args>` passes args to the agent
  unchanged, and the built-in `claude`/`codex`/`gemini`/`opencode` profiles match our runners. Two
  integration gaps: our MCP config is a host temp file outside the project (only the project and guest home
  are mounted), and it points at host binaries (e.g. `backlog`) that don't exist in the guest. Fix: write the
  config under the project (git-ignored) or the guest home, and only list MCP servers that exist in the guest
  image (or reach host MCP servers over HTTP).
- **Windows (#7):** "Brig does not run on Windows"; WSL2 isn't mentioned. Intel Macs are not supported.

## 3. OS-level sandbox prototype (AC #6, Linux)

Prototype: [`sandbox-spike/bwrap-run.sh`](sandbox-spike/bwrap-run.sh) (bubblewrap 0.9.0, unprivileged user
namespaces, x86-64 container without KVM). The policy: read-only host, writable project and `/tmp`, empty
`$HOME`, cleared environment with an allow-list, no network unless asked, own PID namespace.

| Check | Result |
|---|---|
| Write inside the project | works |
| Write to `/etc`, `/opt` | `Read-only file system` |
| Script written by the agent, then run, writing outside the project | `Read-only file system` |
| Read `~/.ssh/*` | `No such file or directory` (empty `$HOME`) |
| Network, default | connection refused |
| Network with `SANDBOX_NET=1` | works (needed a read-only bind of the proxy CA bundle under `$HOME`) |
| Secret in the host env (`FAKE_API_KEY`) | **leaked** until `--clearenv` plus an allow-list was added; then `unset` |
| `kill -9` of bwrap with two background children | all children gone |
| Background process (e.g. a Gradle daemon) after the command ends | killed (PID namespace) |
| stdin | passed through |
| `./gradlew --version` with `~/.gradle` writable and network on | works (Gradle 9.9.2, JDK 21) |
| **Overhead**, average of 50 runs of `true` | host `bash -c` **2.2 ms**, bwrap **10.0 ms** (about **+8 ms**) |

What this shows:
- An OS-level sandbox costs milliseconds and keeps the host toolchain, so it fits `run_command` and `run_tests`.
- **Environment filtering matters as much as the filesystem.** All five spawn points pass the full host env
  today. Any backend must start from an empty env plus an allow-list.
- Hiding `$HOME` breaks tools that read config there (CA bundles, `~/.gitconfig`, `~/.npmrc`). The policy
  needs a small, user-editable list of read-only extras.
- The PID namespace makes killing the tree reliable, but it also kills the **Gradle daemon** after each
  command, so every sandboxed `run_tests` pays a cold Gradle start. Sharing the daemon would need a shared
  PID and network namespace, which weakens isolation. Accept the cold start, or keep the build outside the
  sandbox.
- **Not tried here:** macOS Seatbelt (`sandbox-exec`, needs a Mac; Apple marks it deprecated but it is what
  Claude Code and Codex CLI use), Landlock, and the container backend (Docker daemon not running in this
  environment).

## 4. Side findings (existing bugs, not sandbox-specific)

1. `RunTestsToolExecutor.execute` calls `readProcessOutput` (reads until EOF) **before**
   `waitFor(timeout)`, so the timeout never fires while tests run: a hanging test hangs the tool. On timeout
   it calls `destroyForcibly()` on bash only, leaving the Gradle and test JVMs running. It also doesn't redirect
   stdin from the null device, unlike `run_command`.
2. `MCPExecutionService.initStdioClient` logs the full environment (`"MCP environment : " + env`) to
   the MCP log panel when debug logging is on, which can expose API keys and tokens.

## 5. Checklist for the Brig hands-on run (AC #3, #4, #5)

On an Apple-silicon Mac (macOS 15+) or a Linux host with KVM:

1. Install Brig (`brew tap brig-sh/brig && brew trust brig-sh/brig && brew install --cask brig`).
2. For the Claude runner step: `brig secret import claude-code` (or run with `SKIP_CLAUDE=1`).
3. `docs/superpowers/specs/sandbox-spike/brig-check.sh "$PWD" 2>&1 | tee brig-check.log`
4. Share `brig-check.log`. It records cold and warm start times, per-command overhead, whether
   `brig sh` passes stdout, stderr, exit codes and stdin when piped, file ownership of guest writes, default
   egress, whether a host Ollama is reachable, the guest toolchain, a Gradle test subset in the guest, and the
   Claude Code runner over stdin with stream-json output.

## 6. Draft recommendation (AC #8, to confirm after the Brig run)

1. **Build `CommandExecutionBackend` with `HostBackend` first** (a refactor with no behaviour change).
   Start at `RunCommandToolExecutor` (it already has the `ProcessStarter` seam), then `RunTestsToolExecutor`.
   Fix side finding 1 along the way.
2. **Environment allow-list for all spawn points, whatever the backend.** It is cheap and closes the
   biggest gap found.
3. **OS-level backend next:** bubblewrap on Linux, Seatbelt on macOS, for `run_command` and `run_tests`,
   opt-in per project. Writable project and caches, read-only host, empty home, network off by default
   with a toggle.
4. **Brig as an opt-in backend for CLI runners and ACP agents only**, mapped to `brig run <profile>
   <project> -- <args>`, with credentials through the broker and the MCP config written under the project.
   Shown as unavailable on Windows and Intel Macs. Go or no-go depends on the `brig-check.sh` results.
5. **Windows:** no microVM option. Use the container backend (Docker or Podman) as the Windows fallback;
   it is out of scope until there is demand.
6. **UX:** Settings → Agent → "Sandbox" with a backend dropdown, a per-purpose override, the network
   toggle and an extra-paths list. Show an unavailable state with the reason (`Availability`). Mention the
   active backend in the tool approval dialog.
