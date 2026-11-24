#!/usr/bin/env bash
# TASK-278 spike prototype: run one agent command in a bubblewrap sandbox on Linux.
#
#   bwrap-run.sh <projectDir> <command...>
#
# Policy:
#   - the host filesystem is read-only; the project and a fresh /tmp are writable
#   - $HOME is an empty tmpfs, so ~/.ssh, ~/.aws, tokens etc. are hidden
#   - the environment is cleared; only an allow-list of variables is passed in
#   - no network unless SANDBOX_NET=1
#   - own PID namespace: killing bwrap kills the whole process tree, and nothing
#     (e.g. a Gradle daemon) outlives the command
#
# Knobs:
#   SANDBOX_NET=1                 share the host network
#   SANDBOX_CACHES="dir1 dir2"    extra writable dirs (e.g. ~/.gradle, ~/.m2, ~/.npm)
#   SANDBOX_RO="dir1 dir2"        extra read-only dirs under $HOME (e.g. a CA bundle)
#   SANDBOX_ENV="VAR1 VAR2"       extra variables to pass through
set -euo pipefail

PROJ=$(realpath "$1"); shift

args=(--ro-bind / / --dev /dev --proc /proc --tmpfs /tmp
      --tmpfs "$HOME"
      --unshare-user --unshare-pid --unshare-ipc --unshare-uts --unshare-cgroup-try
      --die-with-parent --new-session
      --clearenv)

for v in PATH HOME USER LANG LC_ALL TERM JAVA_HOME JAVA_TOOL_OPTIONS ${SANDBOX_ENV:-}; do
  [[ -n "${!v:-}" ]] && args+=(--setenv "$v" "${!v}")
done
for d in ${SANDBOX_RO:-}; do args+=(--ro-bind "$d" "$d"); done
for d in ${SANDBOX_CACHES:-}; do mkdir -p "$d"; args+=(--bind "$d" "$d"); done
args+=(--bind "$PROJ" "$PROJ" --chdir "$PROJ")
[[ "${SANDBOX_NET:-0}" == 1 ]] || args+=(--unshare-net)

exec bwrap "${args[@]}" -- /bin/bash -c "$*"
