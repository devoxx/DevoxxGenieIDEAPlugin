# Generic OpenAI-compatible Local Providers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve Nativ and Rapid-MLX from one definition-driven local provider, so a new OpenAI-compatible local server takes one enum line and one definition entry.

**Architecture:** `LocalProviderRegistry` holds `LocalProviderDefinition`s. One `OpenAiCompatibleChatModelFactory` extends `LocalChatModelFactory` and reads models through a catalog parser shared with routers. State moves to maps keyed by enum name, seeded from Nativ's legacy fields on load.

**Tech Stack:** Java 21 (Gradle toolchain), IntelliJ Platform SDK, langchain4j, Gson, OkHttp, JUnit 5, AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-05-generic-local-providers-design.md`

## Global Constraints

- Branch `feat/rapid-mlx-provider`. Stage by explicit path. Never stage `groma/` or `docs/superpowers/plans/2026-10-30-code-mode-experimental.md`, and never run `git reset --hard`, `git clean` or `git stash`.
- Run Gradle in the foreground with `export JAVA_HOME=~/.sdkman/candidates/java/21-zulu`, and write output to a file instead of piping into `head`.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- The enum constants are `ModelProvider.Nativ` (unchanged) and `ModelProvider.RapidMLX`, displayed as `Rapid-MLX`.
- Default URLs are `http://localhost:8080/v1/` (Nativ) and `http://localhost:8000/v1/` (Rapid-MLX). Both providers are disabled by default.
- Long-running work stays off the EDT.
- Rapid-MLX is always the last row on the Local tab.
- The settings tabs are, in order, `Local`, `Cloud Direct`, `Routers`, `Sharding`, `Custom OpenAI`. Exo's row moves to `Sharding`, and nothing else about Exo changes.
- Planned sharding solutions are display only: no `ModelProvider` entry, saved state, factory or request. The list, in order, is `vLLM`, `SGLang`, `llama.cpp RPC`, `LocalAI`, `GPUStack`.

## Review Focus

1. **Another server on the port:** an HTML or non-JSON reply on port 8000 is treated as "not running" and doesn't throw (Task 3).
2. **Upgrading a Nativ user:** their URL, enabled flag and fallback context survive, and a default URL isn't pinned (Task 2).
3. **Older Rapid-MLX:** `capabilities: []` on every model still lists the models (Task 1).
4. **Disabled provider:** no request and no notification (Task 3).
5. **Port changed in settings:** Rapid-MLX can land on 8000-8009, so after Apply the new URL is used with no restart and no stale list (Task 4).

## Starting state

The branch has uncommitted work from the abandoned per-provider approach. The controller clears it before Task 1:

- Delete `src/main/java/com/devoxx/genie/chatmodel/local/rapidmlx/`, `src/main/java/com/devoxx/genie/model/rapidmlx/` and `src/test/java/com/devoxx/genie/chatmodel/local/rapidmlx/`.
- `git restore` only the modified paths under `src/`. Keep `README.md` and the `docusaurus/` edits, which Task 5 finishes and commits.

---

### Task 1: Shared catalog parser

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/catalog/ModelCatalogParser.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/catalog/CatalogDefaults.java`
- Move: `chatmodel/cloud/router/CatalogFields.java` to `chatmodel/catalog/CatalogFields.java` (make public members usable across packages; update imports in `RouterDefinition`, `RouterRegistry` and router tests)
- Modify: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParser.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/catalog/ModelCatalogParserTest.java`

**Interfaces:**
- Produces: `record CatalogDefaults(int contextWindow, boolean apiKeyUsed, boolean shortDisplayNames)`
- Produces: `static List<LanguageModel> ModelCatalogParser.parse(ModelProvider provider, @Nullable CatalogFields fields, CatalogDefaults defaults, String json)`, throwing `JsonParseException` as today

- [ ] **Step 1: Write the failing tests** in `ModelCatalogParserTest`

```java
private static final CatalogFields RAPID = new CatalogFields(null,
        List.of("max_model_len", "context_window"), null, null, 1, "capabilities", "text");
private static final CatalogDefaults LOCAL = new CatalogDefaults(8000, false, true);

@Test void filter_emptyArrayMatches_likeAnAbsentField() {
    var models = ModelCatalogParser.parse(ModelProvider.Nativ, RAPID, LOCAL,
            "{\"data\":[{\"id\":\"a\",\"capabilities\":[]},{\"id\":\"b\"},"
            + "{\"id\":\"c\",\"capabilities\":[\"text\"]},{\"id\":\"d\",\"capabilities\":[\"embedding\"]}]}");
    assertThat(models).extracting(LanguageModel::getModelName).containsExactly("a", "b", "c");
}

@Test void contextWindow_usesFirstPositivePath_thenDefault() {
    var models = ModelCatalogParser.parse(ModelProvider.Nativ, RAPID, LOCAL,
            "{\"data\":[{\"id\":\"a\",\"max_model_len\":65536,\"context_window\":262144},"
            + "{\"id\":\"b\",\"max_model_len\":null,\"context_window\":262144},{\"id\":\"c\"}]}");
    assertThat(models).extracting(LanguageModel::getInputMaxTokens).containsExactly(65_536, 262_144, 8000);
}

@Test void shortDisplayNames_dropOrgPrefix_andLocalModelsUseNoKey() {
    var model = ModelCatalogParser.parse(ModelProvider.Nativ, null, LOCAL,
            "{\"data\":[{\"id\":\"mlx-community/x-4bit\"},{\"id\":\"mlx-community/\"}]}");
    assertThat(model).extracting(LanguageModel::getDisplayName).containsExactly("x-4bit", "mlx-community/");
    assertThat(model.get(0).getModelName()).isEqualTo("mlx-community/x-4bit");
    assertThat(model.get(0).isApiKeyUsed()).isFalse();
}
```

- [ ] **Step 2: Run them and confirm they fail to compile**

Run: `./gradlew test --tests '*ModelCatalogParserTest' > build/t1.log 2>&1; grep -E 'error:|^BUILD' build/t1.log`
Expected: `error:` lines for the missing classes.

- [ ] **Step 3: Implement** by moving the body of `RouterCatalogParser` into `ModelCatalogParser`. Use `defaults.contextWindow()` and `defaults.apiKeyUsed()`, apply the short-name rule when no display name is mapped, and treat an empty filter array as a match. `RouterCatalogParser.parse(def, json)` becomes a delegate with `new CatalogDefaults(128_000, true, false)`, keeping `DEFAULT_CONTEXT_WINDOW`.

- [ ] **Step 4: Confirm routers behave the same**

Run: `./gradlew test --tests '*ModelCatalogParserTest' --tests '*RouterCatalogParserTest' --tests '*RouterChatModelFactoryTest' --tests '*RouterRegistryTest' > build/t1.log 2>&1; grep -E ' FAILED$|^BUILD' build/t1.log`
Expected: `BUILD SUCCESSFUL`, with no assertion changes in router tests.

- [ ] **Step 5: Commit** as `refactor(catalog): share the model catalog parser with local providers`.

---

### Task 2: Definitions and saved state

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderDefinition.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistry.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`
- Test: `src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceLocalProviderTest.java`

**Interfaces:**
- Produces: `record LocalProviderDefinition(ModelProvider provider, String defaultUrl, String downloadUrl, String hint, @Nullable CatalogFields catalogFields, int defaultContextLength, @Nullable String fallbackContextHint)`
- Produces: `LocalProviderRegistry.definitions()` (Nativ only until Task 3 adds RapidMLX), `find(ModelProvider)` returning `Optional`, `isGeneric(ModelProvider)`
- Produces on the state service: `getLocalProviderUrl(ModelProvider)` (trimmed; blank or missing returns the definition's default), `setLocalProviderUrl(ModelProvider, String)`, `isLocalProviderEnabled(ModelProvider)`, `setLocalProviderEnabled(ModelProvider, boolean)`, `@Nullable Integer getLocalProviderFallbackContext(ModelProvider)`, `setLocalProviderFallbackContext(ModelProvider, @Nullable Integer)` (null removes), plus the three bean-accessor maps from the spec

Definition values:
- **Nativ:** download `https://neywa-labs.github.io/nativ/`, `catalogFields` null, length 8000. Hint and fallback hint are the two strings currently in `LLMProvidersComponent` (use the literal `8000` through the definition's length).
- **RapidMLX (added in Task 3):** download `https://github.com/raullenchai/Rapid-MLX`, `new CatalogFields(null, List.of("max_model_len", "context_window"), null, null, 1, "capabilities", "text")`, length 8000, no fallback hint. Hint: `Run MLX models locally on Apple Silicon, for example with <code>rapid-mlx serve qwen3.5-4b-4bit --port 8000</code>. Without <code>--port</code> it uses the first free port from 8000 to 8009, so check that this URL matches.`

- [ ] **Step 1: Write the failing tests**, following `DevoxxGenieStateServiceRouterTest`'s XML round-trip style

```java
private static DevoxxGenieStateService upgrade(DevoxxGenieStateService saved) {
    Element xml = XmlSerializer.serialize(saved);
    DevoxxGenieStateService loaded = new DevoxxGenieStateService();
    loaded.loadState(XmlSerializer.deserialize(xml, DevoxxGenieStateService.class));
    return loaded;
}

@Test void upgrade_carriesNativLegacySettings() {
    DevoxxGenieStateService saved = new DevoxxGenieStateService();
    saved.setNativModelUrl("http://localhost:9090/v1/");
    saved.setNativEnabled(true);
    saved.setNativFallbackContextLength(131_072);
    DevoxxGenieStateService loaded = upgrade(saved);
    assertThat(loaded.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:9090/v1/");
    assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isTrue();
    assertThat(loaded.getLocalProviderFallbackContext(ModelProvider.Nativ)).isEqualTo(131_072);
}

@Test void upgrade_doesNotPinDefaultUrl_orFallback() {
    DevoxxGenieStateService loaded = upgrade(new DevoxxGenieStateService());
    assertThat(loaded.getLocalProviderUrls()).doesNotContainKey("Nativ");
    assertThat(loaded.getLocalProviderFallbackContext(ModelProvider.Nativ)).isNull();
    assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
}

@Test void savedMapValues_winOverLegacyFields() {
    DevoxxGenieStateService saved = new DevoxxGenieStateService();
    saved.setNativModelUrl("http://old:1/v1/");
    saved.setNativEnabled(true);
    saved.setLocalProviderUrl(ModelProvider.Nativ, "http://new:2/v1/");
    saved.setLocalProviderEnabled(ModelProvider.Nativ, false);
    DevoxxGenieStateService loaded = upgrade(saved);
    assertThat(loaded.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://new:2/v1/");
    assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
}

@Test void defaults_disabledOnDefinitionUrl_andBlankUrlFallsBack() {
    DevoxxGenieStateService state = new DevoxxGenieStateService();
    assertThat(state.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
    assertThat(state.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:8080/v1/");
    state.setLocalProviderUrl(ModelProvider.Nativ, "   ");
    assertThat(state.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:8080/v1/");
}

@Test void maps_areThreadSafe_andDropNullEntries() {
    DevoxxGenieStateService state = new DevoxxGenieStateService();
    Map<String, String> urls = new HashMap<>();
    urls.put("Nativ", null);
    state.setLocalProviderUrls(urls);
    assertThat(state.getLocalProviderUrls()).isInstanceOf(ConcurrentMap.class).isEmpty();
}
```

- [ ] **Step 2: Run them and confirm they fail to compile**

Run: `./gradlew test --tests '*DevoxxGenieStateServiceLocalProviderTest' > build/t2.log 2>&1; grep -E 'error:|^BUILD' build/t2.log`

- [ ] **Step 3: Implement.** Model the map setters on `setRouterEnabled(Map)`. In `loadState`, after the router lines: copy each map through its setter, seed the Nativ URL only if it differs from `NATIV_MODEL_URL` (`putIfAbsent`), seed enabled with `putIfAbsent`, and seed the fallback only if non-null. Keep the three legacy fields and their generated accessors, with a comment saying they exist for old settings files. Leave every existing caller alone until Task 4.

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests '*DevoxxGenieStateServiceLocalProviderTest' --tests '*DevoxxGenieStateServiceRouterTest' --tests '*DevoxxGenieStateServiceTest' --tests '*ProviderEnabledResolverTest' > build/t2.log 2>&1; grep -E ' FAILED$|^BUILD' build/t2.log`


- [ ] **Step 5: Commit** as `feat(local): add local provider definitions and saved state`.

---

### Task 3: Generic factory

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactory.java`
- Modify: `src/main/java/com/devoxx/genie/chatmodel/local/LocalLLMProviderUtil.java`
- Modify: `src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java` (add `RapidMLX("Rapid-MLX", Type.LOCAL)` after `Exo`)
- Modify: `chatmodel/local/compatible/LocalProviderRegistry.java` (add the RapidMLX definition from Task 2's values, after Nativ)
- Modify: `chatmodel/ChatModelFactoryProvider.java` (one `FACTORY_SUPPLIERS.put` for RapidMLX only; Task 4 replaces it with a registry loop), `ui/settings/ProviderEnabledResolver.java` (the default branch also checks `LocalProviderRegistry.isGeneric`, and `case Nativ` stays until Task 4)
- Test: `src/test/java/com/devoxx/genie/chatmodel/local/compatible/OpenAiCompatibleChatModelFactoryTest.java`

**Interfaces:**
- Consumes: `LocalProviderDefinition`, `ModelCatalogParser`, `CatalogDefaults`, the Task 2 state accessors
- Produces: public constructor `OpenAiCompatibleChatModelFactory(LocalProviderDefinition def)`
- Produces: package-private test constructor `(LocalProviderDefinition def, BooleanSupplier enabled, Supplier<String> url, Supplier<Integer> fallbackContext, CatalogBodyFetcher fetcher)`, where `CatalogBodyFetcher` is a nested `interface { String fetch(String url) throws IOException; }`
- Produces: `LocalLLMProviderUtil.getBodyFromUrl(String url)` and `getBodyFromUrl(String url, OkHttpClient client)`, sharing the existing success and null-body checks with `fetchModels`

- [ ] **Step 1: Write the failing tests.** Use a test subclass that records `handleProviderNotRunning()` and `handleGeneralFetchError(...)` instead of notifying.

```java
@Test void disabled_sendsNoRequest_andDoesNotNotify()
// fetcher fails the test if called; getModels() is empty; notRunning count is 0

@Test void nonJsonReply_isTreatedAsNotRunning()
// fetcher returns "<html>other server</html>"; getModels() is empty without throwing; notRunning count is 1

@Test void connectionFailure_isTreatedAsNotRunning()
// fetcher throws IOException; getModels() is empty; notRunning count is 1

@Test void listsModels_fromUrlJoinedWithModels()
// for URLs "http://localhost:8000/v1/" and "http://localhost:8000/v1", the fetcher sees "http://localhost:8000/v1/models"

@Test void rapidMlx_payload_leavesOutEmbeddingModel()
// real shape: {"data":[{"id":"qwen3.5-4b-4bit","context_window":262144,"max_model_len":65536,"capabilities":["text","tools"]},
//   {"id":"embed","capabilities":["embedding"]}]} gives one model, id qwen3.5-4b-4bit, context 65536, provider RapidMLX

@Test void nativ_payload_usesConfiguredFallback_thenDefault()
// {"data":[{"id":"mlx-community/gemma-3-4b-it-4bit","object":"model"}]}: fallback 131072 gives 131072;
// null gives 8000; display name "gemma-3-4b-it-4bit"; apiKeyUsed false

@Test void resetModels_refetches_fromCurrentUrl()
// url supplier changes between calls; after resetModels() the fetcher sees the new URL

@Test void rapidMlx_defaultsToDisabledOnPort8000()
// new DevoxxGenieStateService(): isLocalProviderEnabled(RapidMLX) is false; getLocalProviderUrl(RapidMLX) is "http://localhost:8000/v1/"
```

Also add a loopback test in `LocalLLMProviderUtilTest` (create it if absent): a JDK `HttpServer` returns a body on 200, and `getBodyFromUrl(url, new OkHttpClient())` returns it; a 500 throws `UnsuccessfulRequestException`.

- [ ] **Step 2: Run them and confirm they fail to compile**

Run: `./gradlew test --tests '*OpenAiCompatibleChatModelFactoryTest' --tests '*LocalLLMProviderUtilTest' > build/t3.log 2>&1; grep -E 'error:|^BUILD' build/t3.log`

- [ ] **Step 3: Implement.**
- `getModelUrl()` returns the URL supplier's value. Chat and streaming use `createOpenAiChatModel` and `createOpenAiStreamingChatModel`.
- `getModels()` returns `List.of()` when disabled, otherwise `super.getModels()`.
- `fetchModels()` returns the parsed `LanguageModel[]`, with defaults `new CatalogDefaults(fallback != null ? fallback : def.defaultContextLength(), false, true)`.
- Wrap `JsonParseException` in `IOException`, because `LocalChatModelFactory` catches only `IOException` when probing.
- `buildLanguageModel(Object)` casts and returns the model.
- Override `handleProviderNotRunning()` so the message names `def.provider().getName()`.

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests '*OpenAiCompatibleChatModelFactoryTest' --tests '*LocalLLMProviderUtilTest' --tests '*ChatModelFactoryProviderTest' --tests '*ProviderEnabledResolverTest' --tests '*ProviderTypeTest' --tests '*DefaultLLMSettingsUtilTest' > build/t3.log 2>&1; grep -E ' FAILED$|^BUILD' build/t3.log`

- [ ] **Step 5: Commit** as `feat(local): add the generic OpenAI-compatible local factory`.

---

### Task 4: Switch Nativ and Rapid-MLX over

**Files:**
- Delete: `chatmodel/local/nativ/`, `model/nativ/`, and their tests under `src/test/java/com/devoxx/genie/chatmodel/local/nativ/`
- Modify: `chatmodel/ChatModelFactoryProvider.java`, `chatmodel/ChatModelProvider.java`, `controller/ProjectContextController.java`, `service/DevoxxGenieSettingsService.java`, `service/agent/SubAgentRunner.java`, `ui/panel/LlmProviderPanel.java`, `ui/settings/ProviderEnabledResolver.java`, `ui/settings/agent/ModelComboLoader.java`, `ui/settings/DevoxxGenieStateService.java`, `ui/settings/llm/LLMProvidersComponent.java`, `ui/settings/llm/LLMProvidersConfigurable.java`, `src/main/resources/META-INF/plugin.xml`
- Test: `src/test/java/com/devoxx/genie/chatmodel/local/compatible/LocalProviderRegistryTest.java`; `LLMProvidersRouterSettingsTest`, `ModelComboLoaderTest`, `ChatModelFactoryProviderTest`, `ProjectContextControllerTest`, `DevoxxGenieStateServiceTest`

**Interfaces:**
- Consumes: everything from Tasks 1-3
- Produces: `LLMProvidersComponent.LocalProviderRow(JCheckBox enabled, JTextField url, @Nullable JCheckBox fallbackEnabled, @Nullable JBIntSpinner fallback)` and `getLocalProviderRows()`, a `Map<ModelProvider, LocalProviderRow>` in registry order

- [ ] **Step 1: Write the failing tests**

`LocalProviderRegistryTest`, the guard for the one-enum-line-one-definition goal:

```java
@Test void registry_hasExpectedProviders() {
    assertThat(LocalProviderRegistry.definitions()).extracting(LocalProviderDefinition::provider)
            .containsExactly(ModelProvider.Nativ, ModelProvider.RapidMLX);
}

@Test void everyDefinition_isFullyWired() throws Exception {
    // for each definition, following RouterRegistryTest.everyDefinition_isFullyWired:
    // provider type is LOCAL; factory is an OpenAiCompatibleChatModelFactory; ProjectContextController.isSupportedProvider is true;
    // ProviderEnabledResolver follows state.setLocalProviderEnabled; new state is disabled; default URL starts with "http://localhost:";
    // ChatModelProvider sets the base URL to state.getLocalProviderUrl(provider); URLs are unique
}
```

Settings tests in `LLMProvidersRouterSettingsTest`, using its `configurable()` and `tabText(...)` helpers. Also change `tabs_inOrder` to expect `"Local", "Cloud Direct", "Routers", "Sharding", "Custom OpenAI"`:

```java
@Test void localTab_endsWithLlamaThenNativThenRapidMlx() {
    configurable();
    assertThat(tabText("Local").stream().filter(t -> t.endsWith(" URL")).toList())
            .endsWith("LLaMA.c++ URL", "Nativ URL", "Rapid-MLX URL")
            .doesNotContain("Exo URL");
}

@Test void shardingTab_holdsExo() {
    configurable();
    assertThat(tabText("Sharding")).contains("Exo URL");
}

@Test void shardingTab_listsPlannedSolutions_grayedOutBelowExo() {
    configurable();
    // Collect the JLabels of the Sharding tab, as collectText does for text.
    // Each of "vLLM", "SGLang", "llama.cpp RPC", "LocalAI", "GPUStack" has a label with isEnabled() == false,
    // and the labels come after "Exo URL" in that order.
    // "Coming soon" appears once per solution. The tab has no enabled JCheckBox or JTextField for any of them:
    // its only checkbox and URL field are Exo's.
}

@Test void plannedSolutions_areNotProviders() {
    assertThat(ModelProvider.values()).extracting(ModelProvider::getName)
            .doesNotContain("vLLM", "SGLang", "llama.cpp RPC", "LocalAI", "GPUStack");
}

@Test void onlyNativHasFallbackRow()
// "Nativ Fallback Context" is on the Local tab; "Rapid-MLX Fallback Context" is not

@Test void apply_savesUrl_andResetsThatProvidersFactory_whenUrlChanges()
// set the Rapid-MLX row URL to http://localhost:8003/v1/, apply: state holds it, isModified() is false afterwards,
// and the factory's resetModels ran; applying again unchanged doesn't reset it

@Test void reset_restoresRowsFromState()
```

In `ModelComboLoaderTest`: add `genericLocalProvider_loadsOffTheCallingThread`, following its router test.

- [ ] **Step 2: Run them and confirm they fail**

Run: `./gradlew test --tests '*LocalProviderRegistryTest' --tests '*LLMProvidersRouterSettingsTest' --tests '*ModelComboLoaderTest' > build/t4.log 2>&1; grep -E 'error:| FAILED$|^BUILD' build/t4.log`

- [ ] **Step 3: Rewire the non-UI sites.**
- `ChatModelFactoryProvider`: register from the registry, next to the router loop, replacing Task 3's single RapidMLX entry and the Nativ entry.
- `ChatModelProvider.setLocalBaseUrl`: before the switch, use the registry's URL when `isGeneric`; remove `case Nativ`.
- `ProjectContextController`, `LlmProviderPanel`: replace the Nativ check with `LocalProviderRegistry.isGeneric(...)`.
- `ProviderEnabledResolver`: remove `case Nativ`, which the default branch from Task 3 now covers.
- `SubAgentRunner.setBaseUrlIfLocal`: resolve with `ModelProvider.fromString(resolvedProviderName)` inside the existing guard style, use the registry when generic, and remove `case "Nativ"`.
- `ModelComboLoader`: load off the calling thread for routers and generic local providers.
- `DevoxxGenieSettingsService`: remove `getNativModelUrl` and `setNativModelUrl`.
- `DevoxxGenieStateService.getConfigValue`: remove the `nativModelUrl` case and any test assertion on it.
- `plugin.xml`: remove the `NativModelService` registration.

- [ ] **Step 4: Rewire settings.** In `LLMProvidersComponent`, build rows from the registry at the end of the Local panel, using `createTextWithDownloadButton`, `addProviderSettingRow` and `addHintText`. Keep the current spinner bounds and enable-state listeners for the fallback row. Remove the Nativ fields. Add `createShardingPanel()` with Exo's existing row and hint, and add the `Sharding` tab after `Routers` in `createProviderTabs()`. Below Exo, add one row per entry of a private `PLANNED_SHARDING_SOLUTIONS` list: a disabled `JLabel` with the name and a disabled `JLabel("Coming soon")`, no link, listener or state. Finish with `addHintText` using: `OpenAI-compatible servers such as vLLM can already be used from the <b>Custom OpenAI</b> tab.` In `LLMProvidersConfigurable`, cover the rows in `isModified`, in a new `applyLocalProviders(settings)` modelled on `applyRouters` (reset the factory when URL or enabled changed), and in `reset`.

- [ ] **Step 5: Delete the Nativ classes and tests, then run the affected tests**

Run: `./gradlew test --tests '*LocalProviderRegistryTest' --tests '*LLMProvidersRouterSettingsTest' --tests '*ModelComboLoaderTest' --tests '*ChatModelFactoryProviderTest' --tests '*ProjectContextControllerTest' --tests '*ProviderEnabledResolverTest' --tests '*LlmProviderPanelTest' --tests '*DevoxxGenieStateServiceTest' > build/t4.log 2>&1; grep -E 'error:| FAILED$|^BUILD' build/t4.log`
Expected: `BUILD SUCCESSFUL`. Add `"RapidMLX"` to the supported list in `ProjectContextControllerTest` and an `Arguments.of("RapidMLX", OpenAiCompatibleChatModelFactory.class)` to `ChatModelFactoryProviderTest`; change the Nativ row there to the same class if one exists.

- [ ] **Step 6: Confirm no leftovers**

Run: `git grep -n -E 'NativChatModelFactory|NativModelService|NativModelEntryDTO|getNativModelUrl|rapidmlx\.' -- src`
Expected: only the legacy accessors' use in `DevoxxGenieStateService`, if any.

- [ ] **Step 7: Commit** as `feat(local): serve Nativ and Rapid-MLX from the generic provider`.

---

### Task 5: Docs and final verification

**Files:**
- Modify: `CLAUDE.md`, `AGENTS.md`, `src/main/resources/META-INF/plugin.xml`, `docusaurus/docs/llm-providers/local-models.md`, `docusaurus/docs/llm-providers/exo.md`, `docusaurus/docs/configuration/settings.md`
- Commit the kept edits: `README.md`, `docusaurus/docs/features/overview.md`, `docusaurus/docs/getting-started/faq.md`, `docusaurus/docs/getting-started/quick-start-local.md`, `docusaurus/docs/getting-started/why-devoxxgenie.md`, `docusaurus/docs/llm-providers/overview.md`, `docusaurus/docusaurus.config.js`
- Commit: this plan and its spec

- [ ] **Step 1: Update docs.**
- `CLAUDE.md`, under "Adding a New LLM Provider": add that OpenAI-compatible local servers skip the factory and need a `Type.LOCAL` enum entry plus one `LocalProviderDefinition` in `LocalProviderRegistry`, that `LocalProviderRegistry.isGeneric(provider)` is the check for shared code, and that `LocalProviderRegistryTest.everyDefinition_isFullyWired` enforces the wiring.
- `AGENTS.md`: list `nativ` and `rapidmlx` under a generic local entry.
- `plugin.xml` change notes, Unreleased: `feat: add Rapid-MLX as a local LLM provider for Apple Silicon, off by default (#1342). Thanks to Raullen Chai for the proposal and reference implementation. Not tested against a live server`. Also add: `feat: Exo moves to a new <b>Sharding</b> tab in the LLM provider settings`.
- `local-models.md`: keep the Rapid-MLX section's content, and confirm its context-window and embedding text match Tasks 1-3. Note in the provider list that Exo is configured on the **Sharding** tab.
- `README.md` ("grouped in four tabs") and `configuration/settings.md` ("grouped in four tabs" and its table): say five tabs and add **Sharding**: `Exo, which splits a model across several Apple Silicon devices. Other solutions are listed as coming soon`. Don't add the planned solutions to any supported-provider list. Remove Exo from anything describing the Local tab.
- `llm-providers/exo.md:96`: replace "in the Local LLM Providers section" with "on the **Sharding** tab". `llm-providers/exo.md:206`: change the path to `Settings > DevoxxGenie > Sharding > Exo URL`.
- `llm-providers/overview.md`: mark Exo in the local list as `(Distributed AI cluster, settings tab **Sharding**)`.
- Leave `docusaurus/blog/` posts alone, because they describe the plugin at the time they were written.

- [ ] **Step 2: Build the docs**

Run: `(cd docusaurus && npm run build 2>&1 | grep -E 'SUCCESS|ERROR|Broken')`
Expected: `[SUCCESS]`.

- [ ] **Step 3: Run the full suite and plugin verification**

Run: `./gradlew test > build/full.log 2>&1; echo $?; grep -E ' FAILED$|^BUILD ' build/full.log`, then total `build/test-results/test/*.xml`.
Run: `./gradlew verifyPlugin > build/verify.log 2>&1; echo $?; grep -E 'Compatible|^BUILD ' build/verify.log`
Expected: both exit 0, with 0 failures and every IDE `Compatible`.

- [ ] **Step 4: Commit** as `docs(local): document generic local providers and Rapid-MLX`, with `Closes #1342` in the body and `Co-authored-by: raullenchai <raullenchai@gmail.com>` for the docs text adapted from the fork, followed by the standard Claude line.

## Manual check for the maintainer

With `./gradlew runIde`:

1. An existing Nativ setup keeps its URL, checkbox, fallback context and selected model after upgrade.
2. With `rapid-mlx serve qwen3.5-4b-4bit --port 8000` running, tick **Rapid-MLX URL**, pick the provider, and chat in both streaming and non-streaming modes.
3. Change the port in settings, Apply, and confirm the model list comes from the new port.
4. Confirm Exo works from the **Sharding** tab: its checkbox, URL and cluster panel behave as before. Check the planned rows read as clearly grayed out in both light and dark themes.
5. Retake the four `docusaurus/static/img/settings-*.png` screenshots, whose tab bar won't show **Sharding**, and take a new one of the Sharding tab. `settings-local.png` also needs the new rows. The controller then blanks private values such as the Cloudflare Account ID, saves them under the same names plus `settings-sharding.png`, and adds the new image to `README.md`, `configuration/settings.md` and `llm-providers/exo.md`.
