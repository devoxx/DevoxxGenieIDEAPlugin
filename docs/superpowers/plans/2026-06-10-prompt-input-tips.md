# Prompt Input Tips Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a rotating, remotely-updatable, weight-prioritised "Tip:" line below the prompt input placeholder, refreshed each time the input becomes empty.

**Architecture:** A `TipService` (`@Service`) mirrors the existing `ModelConfigService` — it fetches `tips.json` from `genie.devoxx.com/api/`, caches it in-memory and in `DevoxxGenieStateService`, and falls back to a baked-in tip list when offline. Tip selection is a pure, weight-aware function. `CommandAutoCompleteTextField` draws the tip on a second placeholder line and picks a fresh tip (via `TipService`) on each non-empty→empty document transition.

**Tech Stack:** Java 21, IntelliJ Platform SDK, OkHttp (`HttpClientProvider`), Gson, Lombok, JUnit 5 + AssertJ.

**Build note:** all Gradle commands require JDK 21:
```bash
export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home
```

---

### Task 1: Tip and TipConfig model classes

**Files:**
- Create: `src/main/java/com/devoxx/genie/model/tips/Tip.java`
- Create: `src/main/java/com/devoxx/genie/model/tips/TipConfig.java`
- Test: `src/test/java/com/devoxx/genie/model/tips/TipConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.devoxx.genie.model.tips;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipConfigTest {

    private final Gson gson = new Gson();

    @Test
    void parsesTipsWithAndWithoutWeight() {
        String json = """
            {
              "schemaVersion": 1,
              "lastUpdated": "2026-06-10",
              "tips": [
                { "text": "With weight", "weight": 3 },
                { "text": "Without weight" }
              ]
            }
            """;

        TipConfig config = gson.fromJson(json, TipConfig.class);

        assertThat(config.getSchemaVersion()).isEqualTo(1);
        assertThat(config.getLastUpdated()).isEqualTo("2026-06-10");
        assertThat(config.getTips()).hasSize(2);
        assertThat(config.getTips().get(0).getText()).isEqualTo("With weight");
        assertThat(config.getTips().get(0).getWeight()).isEqualTo(3);
        assertThat(config.getTips().get(1).getText()).isEqualTo("Without weight");
        // Gson leaves the absent int field at its default of 0; coercion happens in TipService.
        assertThat(config.getTips().get(1).getWeight()).isZero();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.devoxx.genie.model.tips.TipConfigTest 2>&1 | grep -E "FAILED|failed|error:"`
Expected: compile failure — `Tip` / `TipConfig` do not exist.

- [ ] **Step 3: Write minimal implementation**

`Tip.java`:
```java
package com.devoxx.genie.model.tips;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tip {
    private String text;
    private int weight;
}
```

`TipConfig.java`:
```java
package com.devoxx.genie.model.tips;

import lombok.Data;

import java.util.List;

@Data
public class TipConfig {
    private int schemaVersion;
    private String lastUpdated;
    private List<Tip> tips;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.devoxx.genie.model.tips.TipConfigTest 2>&1 | grep -E "FAILED|failed|BUILD"`
Expected: `BUILD SUCCESSFUL`, no `FAILED`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/devoxx/genie/model/tips/ src/test/java/com/devoxx/genie/model/tips/
git commit -m "feat(tips): add Tip and TipConfig model classes

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: TipService selection logic + fallback

This task creates `TipService` with the **pure, testable** selection helpers
(`effectiveWeight`, `selectWeighted`, `getFallbackTips`). The remote-fetch/cache
wiring is added in Task 3. Splitting this way lets the weight logic be unit-tested
without the IntelliJ platform.

**Files:**
- Create: `src/main/java/com/devoxx/genie/service/tips/TipService.java`
- Test: `src/test/java/com/devoxx/genie/service/tips/TipServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TipServiceTest {

    private Tip tip(String text, int weight) {
        return new Tip(text, weight);
    }

    @Test
    void effectiveWeightCoercesNonPositiveToOne() {
        assertThat(TipService.effectiveWeight(tip("a", 0))).isEqualTo(1);
        assertThat(TipService.effectiveWeight(tip("a", -5))).isEqualTo(1);
        assertThat(TipService.effectiveWeight(tip("a", 4))).isEqualTo(4);
    }

    @Test
    void selectWeightedRespectsWeightDistribution() {
        List<Tip> tips = List.of(tip("low", 1), tip("high", 3));

        int low = 0;
        int high = 0;
        int draws = 10_000;
        for (int i = 0; i < draws; i++) {
            // sweep r deterministically across [0, 1)
            double r = (double) i / draws;
            Tip selected = TipService.selectWeighted(tips, null, r);
            if ("low".equals(selected.getText())) {
                low++;
            } else {
                high++;
            }
        }

        // weight 3 vs weight 1 -> high should appear ~3x as often as low
        double ratio = (double) high / low;
        assertThat(ratio).isBetween(2.7, 3.3);
    }

    @Test
    void selectWeightedNeverReturnsPreviousWhenAlternativesExist() {
        List<Tip> tips = List.of(tip("a", 1), tip("b", 1));
        for (int i = 0; i < 1000; i++) {
            double r = (double) i / 1000;
            assertThat(TipService.selectWeighted(tips, "a", r).getText()).isEqualTo("b");
        }
    }

    @Test
    void selectWeightedReturnsSingleTipEvenWhenItIsPrevious() {
        List<Tip> tips = List.of(tip("only", 1));
        Tip selected = TipService.selectWeighted(tips, "only", 0.5);
        assertThat(selected).isNotNull();
        assertThat(selected.getText()).isEqualTo("only");
    }

    @Test
    void selectWeightedReturnsNullForEmptyOrBlank() {
        assertThat(TipService.selectWeighted(List.of(), null, 0.5)).isNull();
        assertThat(TipService.selectWeighted(null, null, 0.5)).isNull();
        assertThat(TipService.selectWeighted(List.of(tip("  ", 1)), null, 0.5)).isNull();
    }

    @Test
    void fallbackTipsAreNonEmptyAndValid() {
        List<Tip> fallback = TipService.getFallbackTips();
        assertThat(fallback).isNotEmpty();
        assertThat(fallback).allSatisfy(t -> {
            assertThat(t.getText()).isNotBlank();
            assertThat(t.getWeight()).isPositive();
        });
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.devoxx.genie.service.tips.TipServiceTest 2>&1 | grep -E "FAILED|failed|error:"`
Expected: compile failure — `TipService` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `TipService.java` with the static selection logic and fallback list only
(fetch/cache wiring is added in Task 3). The class is an `@Service` so Task 3 can add
instance state and `getInstance()`.

```java
package com.devoxx.genie.service.tips;

import com.devoxx.genie.model.tips.Tip;
import com.devoxx.genie.model.tips.TipConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public final class TipService {

    @NotNull
    public static TipService getInstance() {
        return ApplicationManager.getApplication().getService(TipService.class);
    }

    /**
     * Returns the next tip text to display, weighted-random and excluding the
     * immediately-previous tip when alternatives exist.
     *
     * @param previousTipText the tip currently shown (may be null)
     * @return the next tip text, or null when no tips are available
     */
    @Nullable
    public String nextTip(@Nullable String previousTipText) {
        Tip selected = selectWeighted(getTips(), previousTipText, ThreadLocalRandom.current().nextDouble());
        return selected == null ? null : selected.getText();
    }

    /**
     * Returns the current tip list. Replaced in Task 3 with cache-backed lookup;
     * for now always returns the hardcoded fallback.
     */
    @NotNull
    public List<Tip> getTips() {
        return getFallbackTips();
    }

    static int effectiveWeight(@Nullable Tip tip) {
        if (tip == null || tip.getWeight() <= 0) {
            return 1;
        }
        return tip.getWeight();
    }

    /**
     * Weighted-random selection over {@code tips}, excluding any tip whose text equals
     * {@code previousTipText}. Falls back to the full set if exclusion empties the
     * candidate list (e.g. a single-tip list). Blank/null tips are ignored.
     *
     * @param r a value in [0, 1) used to pick within the cumulative weight range
     */
    @Nullable
    static Tip selectWeighted(@Nullable List<Tip> tips, @Nullable String previousTipText, double r) {
        if (tips == null || tips.isEmpty()) {
            return null;
        }

        List<Tip> valid = new ArrayList<>();
        for (Tip tip : tips) {
            if (tip != null && tip.getText() != null && !tip.getText().isBlank()) {
                valid.add(tip);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }

        List<Tip> candidates = new ArrayList<>();
        for (Tip tip : valid) {
            if (previousTipText == null || !previousTipText.equals(tip.getText())) {
                candidates.add(tip);
            }
        }
        if (candidates.isEmpty()) {
            candidates = valid;
        }

        int total = 0;
        for (Tip tip : candidates) {
            total += effectiveWeight(tip);
        }

        double target = r * total;
        double accumulated = 0;
        for (Tip tip : candidates) {
            accumulated += effectiveWeight(tip);
            if (target < accumulated) {
                return tip;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    @NotNull
    static List<Tip> getFallbackTips() {
        List<Tip> tips = new ArrayList<>();
        tips.add(new Tip("Type @ to add a file to the context.", 2));
        tips.add(new Tip("Generate a DEVOXXGENIE.md with /init for project-aware answers.", 3));
        tips.add(new Tip("Try /test, /explain or /review on selected code.", 2));
        tips.add(new Tip("Discover and install MCP servers from the MCP Marketplace in Settings.", 1));
        tips.add(new Tip("Enable Agent Mode to let DevoxxGenie read, search and edit files for you.", 1));
        tips.add(new Tip("Turn on RAG in Settings to add semantic project context automatically.", 1));
        tips.add(new Tip("Drag & drop images into the prompt when using a multimodal model.", 1));
        tips.add(new Tip("Press Ctrl+Space to autocomplete a slash command.", 1));
        tips.add(new Tip("Add the full project to context with large-context models like Gemini.", 1));
        tips.add(new Tip("DevoxxGenie is open source — star it on GitHub and join us at Devoxx!", 1));
        return tips;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.devoxx.genie.service.tips.TipServiceTest 2>&1 | grep -E "FAILED|failed|BUILD"`
Expected: `BUILD SUCCESSFUL`, no `FAILED`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/devoxx/genie/service/tips/TipService.java src/test/java/com/devoxx/genie/service/tips/TipServiceTest.java
git commit -m "feat(tips): add TipService weighted selection and fallback tips

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Remote fetch + persistent cache wiring

Add the two cache fields to `DevoxxGenieStateService` and replace `TipService.getTips()`
with cache-backed lookup that mirrors `ModelConfigService` (in-memory cache → persistent
cache → background fetch → fallback). No new unit test — this is platform-bound; verified
by a passing build and the existing `TipServiceTest` still passing.

**Files:**
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java:461-463`
- Modify: `src/main/java/com/devoxx/genie/service/tips/TipService.java`

- [ ] **Step 1: Add cache fields to DevoxxGenieStateService**

Locate the existing model-config cache fields (around line 461):
```java
    private String modelConfigCachedJson = "";
    private long modelConfigLastFetchTimestamp = 0L;
    private String modelConfigPluginVersion = "";
```
Add directly below them:
```java
    private String tipsCachedJson = "";
    private long tipsLastFetchTimestamp = 0L;
```
(These get Lombok `@Data`/state accessors `getTipsCachedJson()`, `setTipsCachedJson(...)`,
`getTipsLastFetchTimestamp()`, `setTipsLastFetchTimestamp(...)` like the surrounding fields.)

- [ ] **Step 2: Replace TipService.getTips() with cache-backed lookup**

In `TipService.java`, add the imports:
```java
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;
```

Add these fields and constants to the class body (below `getInstance()`):
```java
    private static final String TIPS_JSON_URL = "https://genie.devoxx.com/api/tips.json";
    private static final long CACHE_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new GsonBuilder().create();

    private volatile TipConfig cachedConfig;
```

Replace the placeholder `getTips()` from Task 2 with:
```java
    @NotNull
    public List<Tip> getTips() {
        TipConfig config = cachedConfig;

        if (config == null) {
            config = restoreFromPersistentCache();
            if (config != null) {
                cachedConfig = config;
            }
        }

        if (config == null) {
            // Cold start: kick off a background fetch and use the fallback this time.
            triggerBackgroundFetch();
            return getFallbackTips();
        }

        triggerBackgroundRefreshIfStale();

        List<Tip> tips = config.getTips();
        return (tips == null || tips.isEmpty()) ? getFallbackTips() : tips;
    }

    private void triggerBackgroundRefreshIfStale() {
        long lastFetch = DevoxxGenieStateService.getInstance().getTipsLastFetchTimestamp();
        if ((System.currentTimeMillis() - lastFetch) > CACHE_TTL_MS) {
            triggerBackgroundFetch();
        }
    }

    private void triggerBackgroundFetch() {
        ApplicationManager.getApplication().executeOnPooledThread(this::fetchAndCache);
    }

    private void fetchAndCache() {
        try {
            Request request = new Request.Builder().url(TIPS_JSON_URL).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Failed to fetch tips: HTTP {}", response.code());
                    return;
                }

                String json = response.body().string();
                TipConfig config = gson.fromJson(json, TipConfig.class);

                if (config == null) {
                    log.warn("Failed to parse tips JSON");
                    return;
                }
                if (config.getSchemaVersion() > SUPPORTED_SCHEMA_VERSION) {
                    log.info("Remote tips have unsupported schema version {}, using fallback",
                            config.getSchemaVersion());
                    return;
                }

                cachedConfig = config;

                DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
                state.setTipsCachedJson(json);
                state.setTipsLastFetchTimestamp(System.currentTimeMillis());

                log.info("Successfully fetched and cached {} tips (schema v{}, updated {})",
                        config.getTips() == null ? 0 : config.getTips().size(),
                        config.getSchemaVersion(), config.getLastUpdated());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch tips: {}", e.getMessage());
        }
    }

    @Nullable
    private TipConfig restoreFromPersistentCache() {
        try {
            String cachedJson = DevoxxGenieStateService.getInstance().getTipsCachedJson();
            if (cachedJson == null || cachedJson.isEmpty()) {
                return null;
            }
            TipConfig config = gson.fromJson(cachedJson, TipConfig.class);
            if (config != null && config.getSchemaVersion() <= SUPPORTED_SCHEMA_VERSION) {
                return config;
            }
        } catch (Exception e) {
            log.warn("Failed to restore tips from persistent cache: {}", e.getMessage());
        }
        return null;
    }
```

- [ ] **Step 3: Run tests + compile to verify nothing broke**

Run: `./gradlew test --tests com.devoxx.genie.service.tips.TipServiceTest 2>&1 | grep -E "FAILED|failed|BUILD"`
Expected: `BUILD SUCCESSFUL`, no `FAILED` (selection/fallback tests still green; `getTips()` returns fallback when no cache, so distribution behaviour is unchanged in tests).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/devoxx/genie/service/tips/TipService.java src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
git commit -m "feat(tips): fetch tips.json with 24h cache and fallback

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Draw the tip line in the input field

Render a second gray line below the existing placeholder and pick a fresh tip on each
non-empty→empty transition. Verified by build + manual `runIde` (Swing painting is not
unit-tested in this codebase).

**Files:**
- Modify: `src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java`

- [ ] **Step 1: Add imports and the currentTip field**

In `CommandAutoCompleteTextField.java`, add imports:
```java
import com.devoxx.genie.service.tips.TipService;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.jetbrains.annotations.Nullable;
```

Add a field next to the existing `placeholder` field (around line 36):
```java
    @Nullable
    private String currentTip;
```

- [ ] **Step 2: Seed the first tip and register the empty-transition listener**

In the constructor (after `addKeyListener(new CommandKeyListener());`, around line 44), add:
```java
        // Seed the first tip and refresh it whenever the field transitions to empty.
        currentTip = TipService.getInstance().nextTip(null);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // typing makes the field non-empty; nothing to do
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshTipIfEmpty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // attribute changes only; ignore
            }
        });
```

Note: `setText("")` after a submit fires `removeUpdate`, so clearing the field rotates
the tip. The `CommandDocument` set via `setDocument(...)` is the document this listener
attaches to.

Add the helper method to the class body:
```java
    private void refreshTipIfEmpty() {
        if (getText().isEmpty()) {
            currentTip = TipService.getInstance().nextTip(currentTip);
            repaint();
        }
    }
```

- [ ] **Step 3: Draw the tip on a second line in paintComponent**

Replace the existing `paintComponent` (lines 82-89):
```java
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !placeholder.isEmpty()) {
            g.setColor(JBColor.GRAY);
            g.drawString(placeholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
        }
    }
```
with:
```java
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (!getText().isEmpty()) {
            return;
        }
        int x = getInsets().left;
        int baseline = g.getFontMetrics().getMaxAscent() + getInsets().top;
        if (!placeholder.isEmpty()) {
            g.setColor(JBColor.GRAY);
            g.drawString(placeholder, x, baseline);
        }
        if (currentTip != null && !currentTip.isBlank()) {
            g.setColor(JBColor.GRAY);
            g.drawString("Tip: " + currentTip, x, baseline + g.getFontMetrics().getHeight());
        }
    }
```

- [ ] **Step 4: Compile and run the full test suite**

Run: `./gradlew test 2>&1 | grep -E "FAILED|failed|BUILD"`
Expected: `BUILD SUCCESSFUL`, no `FAILED`.

- [ ] **Step 5: Manual verification in a sandbox IDE**

Run: `./gradlew runIde`
Expected: the prompt input shows the shortcut placeholder on line 1 and a `Tip: …` line
on line 2 while empty. Type text → both lines disappear. Submit/clear → the field empties
and a (usually different) tip appears.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/devoxx/genie/ui/component/input/CommandAutoCompleteTextField.java
git commit -m "feat(tips): render rotating tip line under the prompt placeholder

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Publish tips.json to docusaurus static API

Create the remote source file with content matching the in-plugin fallback so the
deployed endpoint and the fallback stay in parity.

**Files:**
- Create: `docusaurus/static/api/tips.json`

- [ ] **Step 1: Create tips.json**

```json
{
  "schemaVersion": 1,
  "lastUpdated": "2026-06-10",
  "tips": [
    { "text": "Type @ to add a file to the context.", "weight": 2 },
    { "text": "Generate a DEVOXXGENIE.md with /init for project-aware answers.", "weight": 3 },
    { "text": "Try /test, /explain or /review on selected code.", "weight": 2 },
    { "text": "Discover and install MCP servers from the MCP Marketplace in Settings.", "weight": 1 },
    { "text": "Enable Agent Mode to let DevoxxGenie read, search and edit files for you.", "weight": 1 },
    { "text": "Turn on RAG in Settings to add semantic project context automatically.", "weight": 1 },
    { "text": "Drag & drop images into the prompt when using a multimodal model.", "weight": 1 },
    { "text": "Press Ctrl+Space to autocomplete a slash command.", "weight": 1 },
    { "text": "Add the full project to context with large-context models like Gemini.", "weight": 1 },
    { "text": "DevoxxGenie is open source — star it on GitHub and join us at Devoxx!", "weight": 1 }
  ]
}
```

- [ ] **Step 2: Validate JSON**

Run: `python3 -m json.tool docusaurus/static/api/tips.json > /dev/null && echo VALID`
Expected: `VALID`.

- [ ] **Step 3: Commit**

```bash
git add docusaurus/static/api/tips.json
git commit -m "feat(tips): publish tips.json to docusaurus static API

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Remote `tips.json` at `genie.devoxx.com/api/` → Task 5 (file) + Task 3 (`TIPS_JSON_URL`). ✓
- `schemaVersion` / `lastUpdated` / `tips[]` shape + `weight` → Task 1. ✓
- Weighted-random, never-repeat-previous selection → Task 2 (`selectWeighted`). ✓
- `weight` defaults to 1 when absent/≤0 → Task 2 (`effectiveWeight`). ✓
- TipService mirrors ModelConfigService (in-memory + persistent cache, 24h TTL, schema guard, EDT-safe background fetch, hardcoded fallback) → Task 3. ✓
- Two new `DevoxxGenieStateService` fields → Task 3. ✓
- Second placeholder line drawn only when empty; tip chosen on non-empty→empty transition, never in `paintComponent`; first tip seeded in constructor → Task 4. ✓
- Tests: weighted distribution, never-previous, single/empty edge cases, malformed/missing-weight fallback, Gson round-trip + schema → Tasks 1 & 2. ✓
- No settings toggle, no per-locale translation (YAGNI) → respected (no such tasks). ✓

**Deviation from spec (intentional):** the spec listed `PromptInputArea` as kicking off the
initial fetch. Task 4 instead seeds the first tip in `CommandAutoCompleteTextField`'s
constructor via `TipService.getInstance().nextTip(null)`, which already triggers the
cold-start background fetch through `getTips()`. This keeps the change in one component and
leaves `PromptInputArea` untouched. Net behaviour matches the spec.

**Placeholder scan:** no TBD/TODO/"add error handling"/"similar to Task N" — all code is concrete. ✓

**Type consistency:** `Tip(text, weight)` constructor (Lombok `@AllArgsConstructor`), `getText()`/`getWeight()`, `effectiveWeight(Tip)`, `selectWeighted(List<Tip>, String, double)`, `getFallbackTips()`, `nextTip(String)`, `getTips()`, state accessors `getTipsCachedJson()/setTipsCachedJson()/getTipsLastFetchTimestamp()/setTipsLastFetchTimestamp()` — names used identically across Tasks 1–4. ✓
