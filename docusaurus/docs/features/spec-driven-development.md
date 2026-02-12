---
sidebar_position: 3
title: Spec-driven Development
description: Use Spec-driven Development (SDD) with Backlog.md to define tasks as structured markdown specs, then let the LLM agent implement them autonomously within your IDE.
keywords: [devoxxgenie, spec-driven development, sdd, backlog.md, task specs, agent mode, acceptance criteria, milestones]
image: /img/devoxxgenie-social-card.jpg
---

# Spec-driven Development (SDD)

Spec-driven Development (SDD) is a workflow where you define **what** needs to be built as structured task specifications, and the LLM agent figures out **how** to build it. Instead of writing code instructions in chat, you write clear specs with acceptance criteria, and the agent implements them autonomously using its built-in tools.

<div style={{textAlign: 'center', margin: '2rem 0'}}>
<iframe
  width="100%"
  style={{aspectRatio: '16/9', maxWidth: '720px', borderRadius: '8px'}}
  src="https://www.youtube.com/embed/t1MOHCfsdvk"
  title="Spec-driven Development Demo"
  frameBorder="0"
  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
  allowFullScreen
/>
</div>

## Why Spec-driven Development?

Traditional AI-assisted coding relies on ad-hoc prompts: you describe what you want in natural language and hope the LLM understands. SDD takes a more disciplined approach:

- **Structured over ad-hoc**: Task specs have a defined format with title, description, acceptance criteria, priority, and dependencies, reducing ambiguity
- **Traceable progress**: The agent checks off acceptance criteria as it works, so you can see exactly what's been done
- **Reproducible**: Specs are version-controlled markdown files that live alongside your code
- **Team-friendly**: Multiple developers (and AI agents) can work from the same backlog
- **Autonomous execution**: Click "Implement with Agent" and the LLM reads the spec, plans the implementation, and executes it

## How It Works

Everything happens inside your IDE, from creating tasks to implementing them:

```
┌──────────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│   Create Tasks   │────▶│  DevoxxGenie Specs   │────▶│ Agent Implements │
│  (Prompt/Chat)   │     │ (Task List / Kanban) │     │   (SDD Tools)    │
└──────────────────┘     └──────────────────────┘     └──────────────────┘
        │                          │                          │
  "Create a task           Browse tasks in a tree     Agent reads files,
   for adding..."          or drag-and-drop on the    makes edits, checks
                           Kanban board               off acceptance criteria
```

1. **Create tasks from the prompt**: type "Create a task for..." in the DevoxxGenie chat and the agent creates structured spec files with acceptance criteria using the built-in SDD tools
2. **Browse tasks in the DevoxxGenie Specs**: a dedicated tool window with two views — a **Task List** grouped by status and a **Kanban Board** for visual drag-and-drop management
3. **Select a task** and click "Implement with Agent" to inject the full spec into the LLM prompt
4. **The agent implements autonomously**: reading files, making edits, checking off acceptance criteria, and recording implementation notes as it works

## Installation & Setup

### Prerequisites

- DevoxxGenie plugin installed in IntelliJ IDEA
- No external CLI tools required: backlog initialization is built into the plugin

### Step 1: Enable DevoxxGenie Specs

1. Open **Settings** > **Tools** > **DevoxxGenie** > **Spec Driven Dev**
2. Check **Enable Spec Browser**
3. Verify the **Spec directory** is set to `backlog` (the default)
4. Click **Apply**

The DevoxxGenie Specs tool window will now appear in your IDE.

### Step 2: Initialize Your Project Backlog

#### Option A: Init Backlog Button (Recommended)

The easiest way to set up your backlog is directly from the settings panel:

1. In the **Spec Driven Dev** settings panel, click the **Init Backlog** button
2. The plugin creates the full directory structure and a default `config.yml`, with no CLI tools or npm packages required
3. Once initialized, the button becomes disabled and shows "Backlog already initialized"

This creates the standard Backlog.md-compatible directory structure in pure Java, with no external dependencies:

```
your-project/
├── backlog/
│   ├── config.yml          # Backlog configuration (statuses, task prefix, milestones)
│   ├── tasks/              # Active task spec files
│   ├── drafts/             # Draft tasks not yet ready
│   ├── docs/               # Supporting documents
│   ├── decisions/          # Decision records
│   ├── milestones/         # Milestone definitions
│   ├── completed/          # Completed tasks (moved here automatically)
│   └── archive/            # Archived items
│       ├── tasks/
│       ├── drafts/
│       └── milestones/
└── ...
```

The default `config.yml` includes three statuses (To Do, In Progress, Done), `task` as the ID prefix, and `To Do` as the default status.

#### Option B: From the Terminal

You can also initialize using the [Backlog.md](https://backlog.md) CLI:

```bash
npm install -g backlog.md
cd your-project
backlog init
```

### Step 3: Create Your First Task

When DevoxxGenie Specs is enabled, 17 backlog tools are automatically available. Just type what you need in natural language in the DevoxxGenie prompt:

> "Create a task to add user authentication with JWT for the REST API, high priority, with acceptance criteria for login endpoint, token generation, and password hashing"

> "Create a task for adding input validation to the registration form with acceptance criteria for email format, password strength, and error messages"

> "Break down the user authentication feature into 3-4 tasks with acceptance criteria"

The agent will use the `backlog_task_create` tool to create properly structured spec files with all the metadata, acceptance criteria, and descriptions filled in automatically. The new tasks appear instantly in the DevoxxGenie Specs.

#### Manually (Markdown)

You can also create a `.md` file directly in `backlog/tasks/` with YAML frontmatter:

```markdown
---
id: TASK-1
title: Add user authentication
status: To Do
priority: high
labels:
  - security
  - api
created: 2025-01-15 10:00
---

## Description

Implement JWT-based authentication for the REST API endpoints.

## Acceptance Criteria

- [ ] POST /auth/login endpoint accepts email and password
- [ ] Successful login returns a JWT token with 24h expiry
- [ ] Protected endpoints return 401 without valid token
- [ ] Password is hashed with bcrypt before storage
- [ ] Rate limiting on login endpoint (5 attempts per minute)
```

## Using the DevoxxGenie Specs

The **DevoxxGenie Specs** tool window provides two views for managing your task specs: a **Task List** and a **Kanban Board**. Switch between them using the tabs at the top of the tool window.

### Task List

The Task List view displays tasks in a tree structure grouped by status:

- **To Do**: Tasks ready for implementation
- **In Progress**: Tasks currently being worked on
- **Done**: Completed tasks

Click any task to see its full details in the preview panel and open the markdown file in the editor. The layout adapts automatically — when the tool window is narrow (e.g., docked to the side), the tree and preview stack vertically; when wide (e.g., docked at the bottom), they sit side by side.

![SDD Task List](/img/SDD-TaskList.png)

### Kanban Board

The Kanban Board provides a visual drag-and-drop interface for managing task status. Each status column shows task cards with priority badges, labels, and acceptance criteria progress.

![SDD Kanban Board](/img/SDD-Kanban.png)

Key features of the Kanban Board:

- **Drag and drop**: Move task cards between status columns to update their status instantly — changes are written back to the spec files automatically
- **Drag to bin**: Drag a task card to the trash zone at the bottom of the board to archive it
- **Click to open**: Click any task card to open the corresponding markdown file in the editor
- **Live updates**: The board refreshes automatically when specs change on disk or via agent actions
- **Theme-aware**: The board follows your IDE's light/dark theme

The Kanban Board uses JCEF (the embedded Chromium browser in IntelliJ) for a rich HTML5 experience. If JCEF is not available, a read-only Swing fallback is displayed instead.

### Preview Panel

The preview panel (in the Task List view) shows:
- Task ID, title, status, and priority
- Full description
- Acceptance criteria with check/uncheck status
- Dependencies and references
- Implementation plan and notes

### Implement with Agent

The **"Implement with Agent"** button is the core of SDD. When you click it:

1. The full task spec is serialized into a structured `<TaskSpec>` context block
2. An agent instruction is prepended, telling the LLM to:
   - Set the task status to "In Progress"
   - Follow the acceptance criteria exactly
   - Check off each criterion as it's completed
   - Record implementation notes as it works
   - Write a final summary when done
   - Mark the task as complete
3. The combined prompt is submitted to the active LLM in agent mode

The agent then autonomously implements the task, updating the spec file in real-time as it progresses.

## Backlog Tools (Agent Mode)

When the DevoxxGenie Specs is enabled, DevoxxGenie automatically registers **17 built-in backlog tools** that the LLM agent can use. These tools allow the agent to manage the entire task lifecycle programmatically.

### Task Tools (7)

| Tool | Description |
|------|-------------|
| `backlog_task_create` | Create a new task with title, description, acceptance criteria, labels, priority, etc. |
| `backlog_task_list` | List tasks with optional filters (status, assignee, labels, search text) |
| `backlog_task_search` | Search tasks by title and description with status/priority filters |
| `backlog_task_view` | View full details of a specific task by ID |
| `backlog_task_edit` | Edit task metadata, plan, notes, acceptance criteria (check/uncheck), and final summary |
| `backlog_task_complete` | Mark a task as Done and move it to the completed directory |
| `backlog_task_archive` | Move a task to the archive directory |

### Document Tools (5)

| Tool | Description |
|------|-------------|
| `backlog_document_list` | List documents with optional search filter |
| `backlog_document_view` | View full document contents by ID |
| `backlog_document_create` | Create a new supporting document |
| `backlog_document_update` | Update document content and title |
| `backlog_document_search` | Search documents by title and content |

### Milestone Tools (5)

| Tool | Description |
|------|-------------|
| `backlog_milestone_list` | List all milestones from configuration |
| `backlog_milestone_add` | Add a new milestone |
| `backlog_milestone_rename` | Rename a milestone (optionally updating all referencing tasks) |
| `backlog_milestone_remove` | Remove a milestone (clear, keep, or reassign tasks) |
| `backlog_milestone_archive` | Archive a milestone |

## Task Spec Format

Task specs follow the [Backlog.md](https://backlog.md) format: markdown files with YAML frontmatter. Here's a complete example showing all supported fields:

```markdown
---
id: TASK-42
title: Implement caching layer for API responses
status: In Progress
priority: high
milestone: v2.0
assignee:
  - alice
  - bob
labels:
  - performance
  - api
dependencies:
  - TASK-40
  - TASK-41
references:
  - https://redis.io/docs/
  - src/main/java/com/example/api/
documentation:
  - docs/caching-strategy.md
created: 2025-03-01 09:00
updated: 2025-03-15 14:30
---

## Description

Add a Redis-based caching layer for frequently accessed API endpoints
to reduce database load and improve response times.

## Acceptance Criteria

- [x] Redis client configured with connection pooling
- [x] GET /api/products cached with 5-minute TTL
- [ ] Cache invalidation on POST/PUT/DELETE operations
- [ ] Cache hit/miss metrics exposed via /actuator/metrics
- [ ] Graceful degradation when Redis is unavailable

## Definition of Done

- [ ] All acceptance criteria checked
- [ ] Unit tests written and passing
- [ ] Integration tests with embedded Redis
- [ ] Documentation updated

## Implementation Plan

1. Add Redis dependencies to build.gradle
2. Create CacheService abstraction
3. Implement Redis-backed cache with TTL support
4. Add cache interceptor for API controllers
5. Write tests with embedded Redis

## Implementation Notes

Started with Spring Cache abstraction using `@Cacheable` annotations.
Chose Lettuce client over Jedis for async support.

## Final Summary

(Written by agent upon completion)
```

## Workflow Example

Here's a typical SDD workflow:

### 1. Plan Your Work

Create task specs by typing directly into the DevoxxGenie prompt:

> "Create 4 tasks for implementing user authentication: one for the login endpoint, one for JWT token management, one for password hashing, and one for rate limiting. Set them all to high priority with the 'security' label."

The agent will create all four task specs with proper acceptance criteria, and they'll immediately appear in the DevoxxGenie Specs.

### 2. Prioritize and Organize

Organize your backlog using the DevoxxGenie prompt:

> "Add a milestone called 'v2.0' and assign all security-labeled tasks to it"

> "Set TASK-3 to high priority and add the 'api' label"

You can also manage tasks visually: use the **Task List** to review details, or switch to the **Kanban Board** to drag tasks between status columns. Drag a card to the trash zone to archive tasks you no longer need.

### 3. Implement with Agent

Open the DevoxxGenie Specs, select a task, and click **"Implement with Agent"**. The agent will:

- Read the spec and understand the requirements
- Use `read_file`, `list_files`, and `search_files` to explore the codebase
- Use `write_file` and `edit_file` to make changes
- Use `backlog_task_edit` to check off acceptance criteria as it goes
- Use `backlog_task_edit` with `notesAppend` to record what it changed
- Use `backlog_task_complete` to mark the task as done

### 4. Review and Iterate

Review the agent's work. The spec file now contains:
- All acceptance criteria checked off
- Implementation notes documenting what was changed and why
- A final summary of the completed work

If something needs adjustment, uncheck the relevant acceptance criteria and click "Implement with Agent" again.

## Configuration

### Spec Directory

By default, DevoxxGenie looks for specs in the `backlog/` directory relative to your project root. You can change this in **Settings** > **Tools** > **DevoxxGenie** > **Spec Driven Dev**.

### Backlog Configuration

The `backlog/config.yml` file is created automatically when you click the **Init Backlog** button or initialize via the chat prompt. It controls ID generation, statuses, and milestone definitions. The file is managed automatically when you create tasks and milestones through the DevoxxGenie prompt:

```yaml
version: 1
id_counters:
  task: 42
  document: 5
milestones:
  - name: v2.0
    description: Next major release
  - name: v2.1
    description: Bug fixes and improvements
```

## Adding References and Documentation to Tasks

Tasks support two fields for linking external information: **references** and **documentation**. When the agent implements a task, these are included in the prompt context so the LLM knows where to look for relevant code, APIs, or design docs.

- **`references`**: Links to code paths, issues, or external resources related to the task (e.g., source files, GitHub issues, API endpoints)
- **`documentation`**: Links to design specs, architecture docs, or guides that provide context for the implementation

### From the DevoxxGenie Prompt

Ask the agent to add references or documentation when creating or editing a task:

> "Create a task to refactor the payment service, with references to src/main/java/com/example/payment/ and https://github.com/myproject/issues/42"

> "Add documentation links to TASK-7: docs/payment-architecture.md and https://wiki.example.com/payment-flow"

The agent uses `backlog_task_create` (with `references` and `documentation` fields) or `backlog_task_edit` (with `addReferences` and `addDocumentation`) to update the task file.

### By Editing the Markdown File

Click any task in the DevoxxGenie Specs to open its markdown file in the editor, then add the fields to the YAML frontmatter:

```markdown
---
id: TASK-7
title: Refactor payment service
status: To Do
priority: high
references:
  - src/main/java/com/example/payment/
  - https://github.com/myproject/issues/42
documentation:
  - docs/payment-architecture.md
  - https://wiki.example.com/payment-flow
---
```

Save the file and the DevoxxGenie Specs will pick up the changes automatically via the file watcher.

### How the Agent Uses Them

When you click **"Implement with Agent"** or run a batch of tasks, the full task context sent to the LLM includes dedicated sections:

```
## References
- src/main/java/com/example/payment/
- https://github.com/myproject/issues/42

## Documentation
- docs/payment-architecture.md
- https://wiki.example.com/payment-flow
```

This gives the agent clear pointers to the relevant code and design documents before it starts working.

## Viewing Your Backlog Outside the IDE

Since task specs are standard [Backlog.md](https://backlog.md)-compatible markdown files, you can also view and manage your backlog from the terminal using the Backlog.md CLI:

```bash
# Interactive Kanban board in the terminal
backlog board

# Web-based Kanban board in your browser (default: http://localhost:6420)
backlog browser
```

The terminal board (`backlog board`) gives you a quick overview directly in your shell, while `backlog browser` launches a full web UI with drag-and-drop, task editing, and real-time updates. Both read from the same `backlog/` directory, so any changes made in DevoxxGenie, the CLI, or the web UI are reflected everywhere.

To install the CLI:

```bash
npm i -g backlog.md
```

## Batch Task Execution (Agent Loop)

For running multiple tasks sequentially with dependency ordering, progress tracking, and automatic task advancement, see [Agent Loop — Batch Task Execution](sdd-agent-loop.md).

Both the built-in LLM provider and external [CLI Runners](cli-runners.md) support batch execution.

## CLI Runners

Instead of using the built-in LLM provider, you can execute spec tasks via **external CLI tools** — such as Claude Code, GitHub Copilot CLI, OpenAI Codex CLI, or Google Gemini CLI. See the dedicated [CLI Runners](cli-runners.md) page for setup instructions, supported tools, and configuration details.

## Tips and Best Practices

- **Write clear acceptance criteria**: The more specific your criteria, the better the agent can implement and verify its work
- **Use dependencies**: If Task B depends on Task A, add the dependency so the agent knows the order
- **Break down large tasks**: Smaller, focused tasks with 3-5 acceptance criteria work better than large monolithic specs
- **Add references and docs**: Link relevant source files, issues, and design documents so the agent has full context when implementing
- **Review agent notes**: The implementation notes the agent writes help you understand what was changed and why
- **Version control your specs**: Task specs are plain markdown files, so commit them alongside your code
- **Use milestones for releases**: Group tasks by milestone to track progress toward releases
- **Leverage documents**: Use backlog documents to store architectural decisions, API specs, or design notes that the agent can reference
