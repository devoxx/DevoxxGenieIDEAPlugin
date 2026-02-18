---
sidebar_position: 1
title: Plugin Integration API
description: Learn how external IntelliJ plugins can integrate with DevoxxGenie at runtime to send prompts and create backlog task files.
keywords: [devoxxgenie, integration, api, intellij plugin, external plugin, prompt, backlog, task]
image: /img/devoxxgenie-social-card.jpg
---

# Plugin Integration API

DevoxxGenie exposes a lightweight runtime API that other IntelliJ plugins can use to interact with it. This page documents how to detect DevoxxGenie, send prompts programmatically, and create backlog task files that are picked up by the Spec-Driven Development workflow.

---

## Detecting DevoxxGenie at Runtime

Before calling any DevoxxGenie API, verify the plugin is installed and enabled using `PluginManagerCore`:

```java
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

public class DevoxxGenieDetector {
    private static final String PLUGIN_ID = "com.devoxx.genie";

    public static boolean isAvailable() {
        var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        return plugin != null && plugin.isEnabled();
    }
}
```

Always guard integration code with this check so your plugin degrades gracefully when DevoxxGenie is not present.

---

## Sending a Prompt via Reflection

DevoxxGenie exposes `ExternalPromptService` for receiving prompt text from other plugins. Access it via reflection to avoid a hard compile-time dependency:

```java
import com.intellij.openapi.project.Project;

public class DevoxxGeniePromptSender {

    public static void sendPrompt(Project project, String promptText) {
        if (!DevoxxGenieDetector.isAvailable()) return;

        try {
            Class<?> serviceClass = Class.forName(
                "com.devoxx.genie.service.ExternalPromptService"
            );
            Object instance = serviceClass
                .getMethod("getInstance", Project.class)
                .invoke(null, project);

            serviceClass
                .getMethod("setPromptText", String.class)
                .invoke(instance, promptText);

        } catch (Exception e) {
            // DevoxxGenie not available or API changed — fail silently
        }
    }
}
```

`setPromptText(String)` populates the DevoxxGenie prompt input field and submits it, triggering a full LLM query with the current conversation context.

---

## Creating Backlog Task Files

Integrations can create structured task files that are automatically recognised by the [Spec-Driven Development](../features/spec-driven-development.md) workflow. Files are written to `backlog/tasks/` inside the project root.

### File Format

Each task is a Markdown file with YAML frontmatter:

```markdown
---
id: TASK-42
title: Fix NullPointerException in UserService.getUser()
status: todo
priority: high
created: 2026-02-18
source: sonarlint
rule: java:S2259
file: src/main/java/com/example/UserService.java
line: 87
---

## Description

SonarQube rule **java:S2259** (Null pointers should not be dereferenced) was triggered at
`UserService.java:87`. The return value of `userRepository.findById(id)` is not checked for
`null` before being dereferenced.

## Acceptance Criteria

- [ ] Add a null-check or use `Optional` for the return value of `findById`
- [ ] Ensure no `NullPointerException` can occur in the affected code path
- [ ] All existing tests pass after the fix
```

### Frontmatter Fields

| Field | Required | Description |
|---|---|---|
| `id` | Yes | Unique task identifier (e.g. `TASK-42`) |
| `title` | Yes | Short human-readable task title |
| `status` | Yes | `todo`, `in-progress`, or `done` |
| `priority` | No | `low`, `medium`, `high`, or `critical` |
| `created` | No | ISO 8601 date |
| `source` | No | Originating tool (e.g. `sonarlint`, `spotbugs`) |
| `rule` | No | Tool-specific rule ID |
| `file` | No | Source file path relative to project root |
| `line` | No | Line number of the finding |

---

## Task ID Synchronisation

To avoid ID collisions, scan existing task files before assigning a new ID.

DevoxxGenie stores tasks in three locations:

| Location | Content |
|---|---|
| `backlog/tasks/` | Active tasks |
| `backlog/completed/` | Completed tasks |
| `backlog/archive/tasks/` | Archived tasks |

**Algorithm:**

```java
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class TaskIdAllocator {
    private static final Pattern ID_PATTERN = Pattern.compile("^id:\\s*TASK-(\\d+)", Pattern.MULTILINE);

    public static int nextTaskId(Path projectRoot) throws IOException {
        int max = 0;
        for (String dir : new String[]{"backlog/tasks", "backlog/completed", "backlog/archive/tasks"}) {
            Path folder = projectRoot.resolve(dir);
            if (!Files.isDirectory(folder)) continue;
            try (var stream = Files.walk(folder, 1)) {
                for (Path p : (Iterable<Path>) stream.filter(f -> f.toString().endsWith(".md"))::iterator) {
                    String content = Files.readString(p);
                    var matcher = ID_PATTERN.matcher(content);
                    if (matcher.find()) {
                        max = Math.max(max, Integer.parseInt(matcher.group(1)));
                    }
                }
            }
        }
        return max + 1;
    }
}
```

---

## Task Filename Convention

Name task files using this pattern to keep them sortable and identifiable:

```
TASK-{n}-{tool}-{rule}-{file}-l{line}.md
```

**Examples:**

```
TASK-42-sonarlint-java_S2259-UserService-l87.md
TASK-43-spotbugs-NP_NULL_ON_SOME_PATH-OrderProcessor-l124.md
```

**Rules:**
- Replace special characters (`:`, `/`, `.`) with `_` or `-`
- Keep the filename under ~100 characters
- The numeric prefix `TASK-{n}` ensures natural sort order matches creation order

---

## Real-World Integrations

| Plugin | Features | Source |
|---|---|---|
| [SonarLint DevoxxGenie](./sonarlint.md) | AI fix + backlog task creation | [GitHub](https://github.com/stephanj/sonarlint-devoxxgenie-intellij) |
| [SpotBugs DevoxxGenie](./spotbugs.md) | AI fix (prompt-only) | [GitHub](https://github.com/stephanj/spotbugs-devoxxgenie-plugin) |
