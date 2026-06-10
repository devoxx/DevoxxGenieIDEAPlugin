# Prompt Input Tips — Design

**Date:** 2026-06-10
**Branch:** `feature/prompt-input-tips`
**Status:** Approved

## Goal

Add a second "Tip:" line to the DevoxxGenie prompt input area, similar to how the
Claude CLI surfaces a rotating tip. The tip appears below the existing placeholder
line (submit/newline shortcuts + "Type prompt or /help") and only while the input is
empty. Tips rotate to a fresh one each time the input becomes empty, are sourced from
a remotely-updatable JSON file, and support weighting so certain tips can be shown
more frequently.

## Current behaviour

- `CommandAutoCompleteTextField.paintComponent()`
  (`src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java:83-89`)
  draws a single line of gray placeholder text, only when the field is empty.
- `PromptInputArea.updatePlaceHolder()`
  (`src/main/java/com/devoxx/genie/ui/component/input/PromptInputArea.java:211-223`)
  builds that string:
  `"<submit> to submit, <newline> for newline. " + <prompt.placeholder | rag.prompt.placeholder>`,
  where the suffix comes from `messages.properties`.

## Design

### 1. Remote tips source & weighting

New file `docusaurus/static/api/tips.json`, served at
`https://genie.devoxx.com/api/tips.json` (same host/pattern as the existing
`models.json` and `welcome.json`). Editing this file and redeploying docusaurus
updates tips without a plugin release.

```jsonc
{
  "schemaVersion": 1,
  "lastUpdated": "2026-06-10",
  "tips": [
    { "text": "Type @ to add a file to the context.",                    "weight": 1 },
    { "text": "Generate a DEVOXXGENIE.md with /init for better answers.", "weight": 3 },
    { "text": "Discover MCP servers in the MCP Marketplace in Settings.", "weight": 1 },
    { "text": "Try /test, /explain or /review on selected code.",         "weight": 1 }
  ]
}
```

**Priority via `weight`** (integer, default `1` when absent): selection is
weighted-random — a tip with `weight: 3` is three times as likely to be shown as a
`weight: 1` tip. To "force" a tip more frequently, bump its weight. This is simpler
and more flexible than a separate boolean priority flag and degrades gracefully when
`weight` is omitted.

**Selection rule:** weighted-random, never returning the immediately-previous tip
(so consecutive empty states feel fresh). When only one tip exists, that tip is
returned. When the list is empty, no tip line is drawn.

**Tip content** (curated, spanning feature discovery, shortcuts & commands, workflow,
and occasional Devoxx flavour) seeds both the docusaurus file and the in-plugin
hardcoded fallback.

### 2. Plugin components & wiring

**New model classes** (`model/tips/`):

- `TipConfig` — `int schemaVersion`, `String lastUpdated`, `List<Tip> tips`.
- `Tip` — `String text`, `int weight` (treated as `1` when `<= 0` or absent).

**New `TipService`** (`service/tips/TipService.java`), an `@Service` mirroring
`ModelConfigService` (`service/models/ModelConfigService.java`) line-for-line:

- `TIPS_JSON_URL = "https://genie.devoxx.com/api/tips.json"`, `CACHE_TTL_MS = 24h`,
  `SUPPORTED_SCHEMA_VERSION = 1`.
- OkHttp via `HttpClientProvider.getClient()`, Gson parse.
- In-memory `volatile TipConfig cachedConfig` plus persistent cache via **two new
  fields** on `DevoxxGenieStateService`: `tipsCachedJson` and
  `tipsLastFetchTimestamp` (same pattern as the existing `modelConfig*` fields).
- `getTips()` returns the cached list and triggers a background refresh if stale;
  on cold start (no cache) it triggers a background fetch and returns the **hardcoded
  fallback list** baked into `TipService`. The background fetch runs via
  `ApplicationManager.getApplication().executeOnPooledThread(...)` — never on the EDT.
- Schema-version guard: remote content with `schemaVersion > SUPPORTED_SCHEMA_VERSION`
  is ignored in favour of the fallback.
- `nextTip(String previous)` — weighted-random pick excluding `previous` when
  alternatives exist. Randomness via `java.util.concurrent.ThreadLocalRandom` (pure
  compute, no EDT concern).

**Wiring into the input field:**

- `CommandAutoCompleteTextField` gains a `currentTip` field and draws line 2 in
  `paintComponent` (gray, `y += fontMetrics.getHeight()`), only when the field is
  empty — mirroring the existing placeholder draw at
  `CommandAutoCompleteTextField.java:83-89`.
- A `DocumentListener` detects the **non-empty → empty transition** and sets
  `currentTip = TipService.getInstance().nextTip(currentTip)` followed by `repaint()`.
  The constructor seeds the first tip. Tip selection **never** happens inside
  `paintComponent` (which fires on every repaint and would flicker).
- `PromptInputArea` calls `TipService.getInstance().getTips()` on construction to kick
  off the initial background fetch.

### Error handling

- Network failure / malformed JSON / unsupported schema → fall back to the hardcoded
  tip list; log at `warn`, never surface an error to the user.
- Empty tip list → draw no second line (graceful no-op).
- Missing/invalid `weight` → coerce to `1`.

### Testing

- `TipServiceTest`:
  - Weighted distribution sanity — over N draws a high-weight tip is selected
    disproportionately more than a low-weight tip.
  - `nextTip` never returns `previous` when alternatives exist.
  - Single-tip and empty-list edge cases.
  - Malformed / missing-`weight` JSON falls back gracefully.
- `TipConfig` Gson round-trip + schema-version rejection.

### Out of scope (YAGNI)

- No settings toggle to disable tips — tips are always on. If wanted later, it is a
  one-field add on `DevoxxGenieStateService` plus a guard in `nextTip`/painting.
- No per-locale tip translations — tips live in the remote JSON / fallback in English;
  consistent with the lightweight remote-content approach.

## Files touched

- `docusaurus/static/api/tips.json` (new)
- `src/main/java/com/devoxx/genie/model/tips/TipConfig.java` (new)
- `src/main/java/com/devoxx/genie/model/tips/Tip.java` (new)
- `src/main/java/com/devoxx/genie/service/tips/TipService.java` (new)
- `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java` (+2 fields)
- `src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java`
- `src/main/java/com/devoxx/genie/ui/component/input/PromptInputArea.java`
- `src/test/java/com/devoxx/genie/service/tips/TipServiceTest.java` (new)
