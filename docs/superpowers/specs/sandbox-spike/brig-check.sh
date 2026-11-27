#!/usr/bin/env bash
# TASK-278 spike: hands-on Brig checks for acceptance criteria #2-#5.
# Run on an Apple-silicon Mac (macOS 15+) or Linux with KVM, with brig installed
# (https://www.brig.sh/docs/install/). Not tested by the author: no KVM was available.
#
#   ./brig-check.sh /path/to/DevoxxGenieIDEAPlugin 2>&1 | tee brig-check.log
#
# Optional:
#   SKIP_CLAUDE=1    skip the Claude Code runner check (needs `brig secret import claude-code` first)
#   REF=<sandbox>    use this sandbox ref if auto-detection from `brig ls --json` fails
#
# Every step prints its command, output, exit code and elapsed time.
# Paste brig-check.log into TASK-278's Implementation Notes (or back into the chat).
set -uo pipefail

PROJ=$(cd "${1:?usage: brig-check.sh <projectDir>}" && pwd)
now() { perl -MTime::HiRes=time -e 'printf "%.3f\n", time'; }
step() {
  local name=$1; shift
  echo; echo "=== $name"; echo "\$ $*"
  local s; s=$(now)
  "$@"; local rc=$?
  echo "--- rc=$rc elapsed=$(perl -e "printf '%.2f', $(now) - $s")s"
  return 0
}

step "environment" uname -a
step "brig version" brig version --json
step "brig doctor" brig doctor --json

# AC #3: cold start (first run downloads image + boot assets) vs warm start.
step "cold start: brig run -d ubuntu" brig run -d ubuntu "$PROJ"
step "sandbox list" brig ls --json
if [[ -z "${REF:-}" ]] && command -v jq >/dev/null; then
  REF=$(brig ls --json | jq -r '.. | objects | select(has("name")) | .name' | head -1)
fi
echo "Using REF=${REF:-<unset; rerun with REF=...>}"
[[ -n "${REF:-}" ]] || exit 1

# AC #2: one non-interactive command in the running sandbox, piped (no TTY).
step "exec: stdout/stderr/exit code, piped" bash -c "brig sh '$REF' -c 'echo to-stdout; echo to-stderr >&2; exit 3' </dev/null | cat; echo pipe-status=\${PIPESTATUS[0]}"
step "exec: --json outcome" bash -c "brig sh --json '$REF' -c 'echo hi; exit 4' </dev/null"
step "exec: stdin delivery" bash -c "echo from-host-stdin | brig sh '$REF' -c 'cat'"
step "exec: retired brig exec spelling" bash -c "brig exec '$REF' -- sh -c 'echo via-exec; exit 5' </dev/null | cat; echo pipe-status=\${PIPESTATUS[0]}"

# AC #3: per-command overhead in a long-lived sandbox (10 runs of `true`).
step "exec overhead x10" bash -c "for i in 1 2 3 4 5 6 7 8 9 10; do brig sh '$REF' -c true </dev/null >/dev/null; done"

# Isolation sanity checks.
step "guest: project mount" brig sh "$REF" -c 'echo BRIG_PROJECT=$BRIG_PROJECT; ls "$BRIG_PROJECT" | head -5; touch "$BRIG_PROJECT/.brig-write-test" && echo wrote'
step "host: file ownership of guest write" ls -l "$PROJ/.brig-write-test"
rm -f "$PROJ/.brig-write-test"
step "guest: default egress (expect blocked)" brig sh "$REF" -c 'curl -sS -m5 -o /dev/null -w "%{http_code}\n" https://repo.maven.apache.org/maven2/ || echo blocked'
step "guest: host Ollama reachable?" brig sh "$REF" -c 'for h in host.docker.internal brig.internal 10.0.2.2 192.168.64.1; do printf "%s: " $h; curl -sS -m3 http://$h:11434/api/version || echo no; done'

# AC #5: toolchain inside the guest.
step "guest: toolchain" brig sh "$REF" -c 'java -version 2>&1 | head -1; node --version; git --version; uname -m'
step "guest: gradle wrapper (needs JDK + egress to services.gradle.org)" brig sh "$REF" -c 'cd "$BRIG_PROJECT" && ./gradlew --version 2>&1 | tail -5'
step "guest: gradle test subset" brig sh "$REF" -c 'cd "$BRIG_PROJECT" && ./gradlew test --tests "*CommandBlacklistTest*" 2>&1 | tail -15'
step "guest: do caches persist? (guest home)" brig sh "$REF" -c 'echo HOME=$HOME; du -sh ~/.gradle 2>/dev/null || echo "no ~/.gradle"'

step "stop" brig stop "$REF"
step "warm start: brig run -d ubuntu (again)" brig run -d ubuntu "$PROJ"
step "warm: first exec" brig sh "$REF" -c true </dev/null

# AC #4: the plugin's CLI runner path, done by hand: prompt on stdin, stream-json on stdout.
if [[ "${SKIP_CLAUDE:-0}" != 1 ]]; then
  step "claude runner via brig (stdin prompt, stream-json)" bash -c \
    "echo 'Reply with the single word OK and nothing else.' | brig run claude '$PROJ' -- -p --verbose --output-format stream-json | head -c 4000; echo; echo pipe-status=\${PIPESTATUS[1]}"
  step "claude runner: is a host MCP config path visible in the guest?" bash -c \
    "tmp=\$(mktemp -t devoxxgenie-mcp); echo '{\"mcpServers\":{}}' > \"\$tmp\"; brig sh '$REF' -c \"test -r '\$tmp' && echo visible || echo not-visible: \$tmp\"; rm -f \"\$tmp\""
fi

step "cleanup" brig rm "$REF"
echo; echo "Done. Share brig-check.log."
