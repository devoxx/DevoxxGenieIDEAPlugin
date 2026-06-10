---
id: TASK-223
title: Add web_search built-in tool to Agent Mode
status: Done
assignee: []
created_date: '2026-06-04 12:37'
updated_date: '2026-06-04 12:40'
labels:
  - agent-mode
  - web-search
  - feature
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/agent/tool/SemanticSearchToolExecutor.java
  - src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/websearch/WebSearchPromptExecutionService.java
  - src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java
  - src/main/resources/META-INF/plugin.xml
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Overview

Add a `web_search` built-in agent tool so the LLM can issue web queries during an agentic session. Currently web search is only available as a prompt-level execution strategy (`WebSearchPromptStrategy`) and is completely unavailable inside Agent Mode. This feature bridges the gap by exposing the existing Tavily/Google search integration as a callable agent tool.

The tool is gated behind a new "Enable web_search tool" toggle in the Agent Mode settings panel, which requires a valid Tavily or Google Custom Search API key to be effective. When no key is configured, a warning and deep-link to the Web Search settings panel are shown.

## Implementation Plan

### Files to Modify / Create

| File | Change |
|---|---|
| `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java` | Add `webSearchAgentToolEnabled: Boolean` field (default false) |
| `src/main/java/com/devoxx/genie/service/agent/tool/WebSearchToolExecutor.java` | **New file** — tool executor calling LangChain4J WebSearchEngine directly |
| `src/main/java/com/devoxx/genie/service/agent/tool/BuiltInToolProvider.java` | Register `web_search` conditionally when flag is true |
| `src/main/java/com/devoxx/genie/service/agent/AgentApprovalProvider.java` | Add `web_search` to `READ_ONLY_TOOLS` set |
| `src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java` | Add "Web Search Tool" section with checkbox + warning panel |

### Step 1 — DevoxxGenieStateService

After the Security scanning settings block (~line 342), add:
```java
// Web search agent tool
private Boolean webSearchAgentToolEnabled = false;
```
Lombok `@Getter`/`@Setter` auto-generate the accessor methods.

### Step 2 — WebSearchToolExecutor (new file)

- Implements `ToolExecutor`
- Does NOT use `WebSearchPromptExecutionService` (which has an extra LLM summarisation step)
- Calls `TavilyWebSearchEngine` or `GoogleCustomWebSearchEngine` directly via LangChain4J
- Returns raw numbered results: title + URL + snippet (truncated to 1000 chars)
- `createWebSearchEngine()` is package-private for test injection (same pattern as `SemanticSearchToolExecutor.searchService()`)
- Errors returned as strings (not thrown) for graceful agent loop recovery
- Precedence: Tavily wins when both providers are configured (matches existing `WebSearchPromptExecutionService` logic)

### Step 3 — BuiltInToolProvider

After the `semantic_search` block (lines 208–233), add:
```java
if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getWebSearchAgentToolEnabled())) {
    tools.put(
        ToolSpecification.builder()
            .name("web_search")
            .description("Search the web for current information, documentation, news, or any topic. " +
                "Returns ranked results with titles, URLs, and content snippets. " +
                "Use when the answer requires information beyond the project codebase — " +
                "e.g. library docs, API references, recent releases, or general knowledge. " +
                "Requires a Tavily or Google Custom Search API key configured in " +
                "Settings → DevoxxGenie → Web search.")
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("query", "Search query as a natural-language question or keyword phrase.")
                .required("query")
                .build())
            .build(),
        new WebSearchToolExecutor()
    );
}
```

### Step 4 — AgentApprovalProvider

Add `"web_search"` to the `READ_ONLY_TOOLS` `Set.of(...)` at line 22. Web search is read-only (no side effects).

### Step 5 — AgentSettingsComponent

**Field declaration** (alongside other checkbox fields):
```java
private final JBCheckBox enableWebSearchToolCheckbox =
    new JBCheckBox("Enable web_search tool",
        Boolean.TRUE.equals(stateService.getWebSearchAgentToolEnabled()));
```

**New UI section** — insert before `// --- Debug ---` (line 245):
```java
addSection(contentPanel, gbc, "Web Search Tool");
addFullWidthRow(contentPanel, gbc, enableWebSearchToolCheckbox);
addHelpText(contentPanel, gbc, "Gives the agent a 'web_search' tool using your configured Tavily or Google Custom Search key.");
if (!isAnyWebSearchKeyConfigured()) {
    addFullWidthRow(contentPanel, gbc, buildWebSearchWarningPanel());
}
```

**Helper `isAnyWebSearchKeyConfigured()`** — checks both Tavily and Google keys are non-blank when enabled.

**Helper `buildWebSearchWarningPanel()`** — shows a red "No web search API key configured." label + `HyperlinkLabel` "Open Web search settings" that calls:
```java
ShowSettingsUtil.getInstance().showSettingsDialog(project, "Web search");
```
The display name `"Web search"` matches `displayName="Web search"` in `plugin.xml` line 580.

**Wire `isModified` / `apply` / `reset`** with the standard one-liner pattern used by all other checkboxes in the component.

New imports needed in `AgentSettingsComponent`:
- `com.intellij.openapi.options.ShowSettingsUtil`
- `com.intellij.openapi.project.ProjectManager`
- `com.intellij.ui.HyperlinkLabel`
- `com.intellij.ui.JBColor`
- `java.awt.FlowLayout`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Agent Mode settings panel shows a 'Web Search Tool' section with an 'Enable web_search tool' checkbox
- [ ] #2 When no Tavily or Google API key is configured, a red warning label and 'Open Web search settings' hyperlink are shown
- [ ] #3 The hyperlink navigates directly to the Web Search settings panel
- [ ] #4 When the checkbox is enabled and a valid Tavily key is configured, the agent has a web_search tool available
- [ ] #5 When the checkbox is enabled and a valid Google Custom Search key+CSI is configured, the agent has a web_search tool available
- [ ] #6 web_search returns numbered results with title, URL, and snippet (truncated to 1000 chars)
- [ ] #7 web_search is auto-approved (read-only) when agentAutoApproveReadOnly is enabled
- [ ] #8 When no API key is configured at call time, the tool returns a descriptive error string (not an exception)
- [ ] #9 When the API call fails, the tool returns an error string with a hint to verify the key in settings
- [ ] #10 The toggle persists across IDE restarts via DevoxxGenieSettingsPlugin.xml
- [ ] #11 Plugin builds without errors: ./gradlew buildPlugin
- [ ] #12 All existing tests pass: ./gradlew test
<!-- AC:END -->
