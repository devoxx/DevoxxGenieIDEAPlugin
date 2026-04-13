---
name: git-commit-push-pr
description: Commit all changes, push to remote, and open a pull request in one go. Use when the user says "commit push pr", "ship it", "open a pr", or invokes /git-commit-push-pr.
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git commit:*), Bash(git push:*), Bash(git branch:*), Bash(git checkout:*), Bash(gh pr create:*)
---

# Git Commit, Push & PR

## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`
- Main branch: !`git rev-parse --verify main 2>/dev/null && echo main || echo master`
- Recent commits: !`git log --oneline -10`

## Your task

Review the changes in the repo and ship them as a pull request in one go.

### Step 1 — Analyze changes and plan commits

Before committing, analyze `git status` and `git diff HEAD` to identify logically distinct groups of changes. Group by feature or concern — for example:

- New files that form a self-contained module → one commit
- Modifications to an existing file that depend on the new module → separate commit
- Config or tooling changes → separate commit

Print a short commit plan (list of planned commits with the files in each) so the grouping is visible.

### Step 2 — Commit

- Create one commit per logical group identified above. Stage only the files for that group using explicit file names (never `git add -A`; never stage `.env` or credential files).
- Write a clean, descriptive commit message for each commit using conventional commits style.
- End every commit message with:
  `Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>`
- Use a HEREDOC to pass the commit message for correct formatting.

### Step 3 — Push

- If the current branch is `main` or `master`, create a new feature branch first (use a descriptive name based on the changes).
- Push the branch to origin with `-u` to set upstream tracking.

### Step 4 — Pull Request

- Create a PR using `gh pr create` targeting the main branch.
- Keep the PR title short (under 70 characters), using conventional commit style.
- Use a HEREDOC for the PR body with this format:

```
## Summary
<1-3 bullet points describing the changes>

## Test plan
- [ ] <testing checklist items>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### Step 5 — Report

- Print the PR URL so the user can see it.

## Rules

- Do all steps in as few messages as possible. Parallelize independent tool calls.
- Do not read or explore code beyond what git provides in the context above.
- Do not use interactive git flags (`-i`).
- Never force-push or amend existing commits.
- If a pre-commit hook fails, fix the issue and create a NEW commit (do not amend).
