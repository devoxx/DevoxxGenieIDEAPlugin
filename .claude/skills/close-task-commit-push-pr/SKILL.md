---
name: close-task-commit-push-pr
description: Close the active backlog task (detected from branch name), commit all changes, push to remote, and open a pull request. Use when the user says "close task and ship it", "close task commit push pr", or invokes /close-task-commit-push-pr.
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git commit:*), Bash(git push:*), Bash(git branch:*), Bash(git checkout:*), Bash(gh pr create:*), mcp__backlog__task_edit, mcp__backlog__task_complete, mcp__backlog__task_view
---

# Close Task, Commit, Push & PR

## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`
- Main branch: !`git rev-parse --verify main 2>/dev/null && echo main || echo master`
- Recent commits: !`git log --oneline -10`

## Your task

Close the active backlog task, commit all changes, push, and open a pull request.

### Step 0 — Identify the task

- Extract the task ID from the current branch name (e.g. `feature/task-113-split-css-modules` → `task-113`).
- If no task ID is found in the branch name, ask the user which task to close.

### Step 1 — Close the backlog task

Close the task **before** committing so the task file changes are included in the commit and PR.

- Use `mcp__backlog__task_view` to read the task details (title, acceptance criteria).
- Use `mcp__backlog__task_edit` to:
  - Set status to `Done`
  - Check off all acceptance criteria that were completed (review the diff to determine which ones)
  - Write a `finalSummary` that concisely describes what was implemented
- Use `mcp__backlog__task_complete` to move the task to the completed folder.

### Step 2 — Analyze changes and plan commits

Before committing, analyze `git status` and `git diff HEAD` to identify logically distinct groups of changes. Group by feature or concern — for example:

- New files that form a self-contained module → one commit
- Modifications to an existing file that depend on the new module → separate commit
- Task/backlog file changes → include in the final commit (or a dedicated chore commit)

Print a short commit plan (list of planned commits with the files in each) so the grouping is visible.

### Step 3 — Commit

- Create one commit per logical group identified above. Stage only the files for that group using explicit file names (never `git add -A`; never stage `.env` or credential files).
- Write a clean, descriptive commit message for each commit using conventional commits style.
- End every commit message with:
  `Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>`
- Use a HEREDOC to pass the commit message for correct formatting.
- **Include the closed task file** in the final commit.

### Step 4 — Push

- If the current branch is `main` or `master`, create a new feature branch first (use a descriptive name based on the changes).
- Push the branch to origin with `-u` to set upstream tracking.

### Step 5 — Pull Request

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

### Step 6 — Update task with PR reference

- Now that you have the PR number, use `mcp__backlog__task_edit` to append the PR URL/number to the `finalSummary`.

### Step 7 — Amend commit with updated task file

- Stage the updated task file and amend the last commit to include the PR reference: `git commit --amend --no-edit` then `git push --force-with-lease`.

### Step 8 — Report

- Print the PR URL and confirm the task was closed.

## Rules

- Do all steps in as few messages as possible. Parallelize independent tool calls.
- Do not read or explore code beyond what git provides in the context above.
- Do not use interactive git flags (`-i`).
- Never force-push or amend existing commits.
- If a pre-commit hook fails, fix the issue and create a NEW commit (do not amend).
