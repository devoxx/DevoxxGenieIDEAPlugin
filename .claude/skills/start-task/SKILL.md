---
name: start-task
description: Create a feature branch for a backlog task, switch to it, and start implementation. Use when the user says "start task-123", "work on task-123", "implement task-123", or invokes /start-task with a task ID.
allowed-tools: Bash(git checkout:*), Bash(git branch:*), Bash(git status:*), Bash(git pull:*), Bash(git stash:*), Bash(npm run lint:*), Bash(npm run typecheck:*), Bash(npm test:*), Bash(npm run build*), Bash(npx playwright test:*), mcp__backlog__task_view, mcp__backlog__task_edit, mcp__backlog__task_search, mcp__backlog__task_list
---

# Start Task

## Context

- Current branch: !`git branch --show-current`
- Working tree status: !`git status --short`
- Existing branches: !`git branch --list 'feature/task-*'`

## Your task

Create a feature branch for a backlog task and begin implementation.

The user will provide a task ID (e.g. `task-123` or `123`). If no task ID is provided, check the argument `$ARGUMENTS` for the task reference.

### Step 1 — Load the task

- Normalize the input: if the user gave just a number like `123`, treat it as `task-123`.
- Use `mcp__backlog__task_view` to read the full task details: title, description, acceptance criteria, and any referenced files.
- If the task is not found, use `mcp__backlog__task_search` to locate it.
- Print a brief summary of the task for the user.

### Step 2 — Ensure a clean working tree

- Check `git status`. If there are uncommitted changes, warn the user and ask whether to stash them before proceeding.
- Ensure we are on the main branch. If not, ask the user if they want to switch.

### Step 3 — Create and switch to a feature branch

- Pull latest changes on main: `git pull --rebase`.
- Derive a branch name from the task: `feature/task-{id}-{slugified-title}` (lowercase, hyphens, max ~60 chars).
  - Example: task 113 "Split app.css into native CSS modules" → `feature/task-113-split-css-modules`
- Create and switch to the branch: `git checkout -b <branch-name>`.

### Step 4 — Mark the task as in-progress

- Use `mcp__backlog__task_edit` to set the task status to `In Progress`.

### Step 5 — Plan and implement

- Analyze the task description and acceptance criteria carefully.
- Read all files referenced in the task, plus any related code you need to understand.
- Create a plan and present it to the user for approval before writing code.
- Once approved, implement the changes following the project's patterns and conventions.
- After implementation, run the full verification suite:
  ```bash
  npm run lint && npm run typecheck && npm test && npm run build:app && npx playwright test
  ```
- Fix any issues found by the verification suite.

### Step 6 — Report

- Summarize what was implemented and which acceptance criteria were addressed.
- Remind the user they can use `/close-task-commit-push-pr` when ready to ship.

## Rules

- Always create the branch from an up-to-date main branch.
- Never start implementation without showing the plan to the user first.
- Follow existing code patterns and conventions in the project.
- Do not use interactive git flags (`-i`).
- If the working tree is dirty, never silently discard changes.
