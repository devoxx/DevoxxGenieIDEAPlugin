# Generic LLM Routers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve OpenAI-compatible routers from one generic factory driven by data definitions, and group all routers on a new Routers settings tab with a privacy warning.

**Architecture:** `RouterRegistry` holds one `RouterDefinition` per router. A single `RouterChatModelFactory` builds models and reads catalogs from a definition, and shared code asks the registry instead of naming routers. Enabled flags live in one state map and keys in PasswordSafe by sub-key string.

**Tech Stack:** Java 21, IntelliJ Platform, langchain4j OpenAI client, OkHttp, Gson, JUnit 5, AssertJ, Mockito.

**Spec:** `docs/superpowers/specs/2026-11-05-generic-llm-routers-design.md`

**Backlog:** TASK-272. Read `get_backlog_instructions(task-execution)` and `groma agent-instructions backlog`, and record each changed file on the task as you go.

## Global Constraints

- Build with JDK 21: `export JAVA_HOME=~/.sdkman/candidates/java/21-zulu`.
- Test command: `./gradlew test --tests <Class> 2>&1 | grep -E "FAILED|failed"`. No output means no failures; confirm tests ran by checking the exit status or the XML report in `build/test-results/test`.
- Mass `NoClassDefFoundError` in tests means the sandbox jar is missing: run `./gradlew prepareTestSandbox --rerun`.
- A JUnit 5 test extending `BasePlatformTestCase` needs `@AfterEach` running tearDown on the EDT, or later tests fail with "File name too long". Prefer plain JUnit 5 with Mockito.
- No live network calls in tests. Use a JDK `HttpServer` on loopback or a fake `RouterCatalogClient`.
- UI on the EDT. Blocking work off it. Inside the modal settings dialog use `executeOnPooledThread` with `ModalityState.any()`, not `Task.Backgroundable`.
- No affiliate, referral or attribution headers or parameters.
- No vendor marketing or privacy claims in UI text or docs.
- Use `RouterRegistry.isGeneric(provider)`, not `Type.ROUTER`, at shared-code sites: Cloudflare is `Type.ROUTER` but its behaviour must not change.
- Never log or display API keys.

## Review Focus

1. **A router returns 200 with HTML, or 401, or invalid JSON:** the model list is empty, nothing throws, the user sees a short failure notification, and the key never appears in logs. Test in Task 3.
2. **A pasted key with surrounding whitespace, or only whitespace:** it is trimmed, and a blank key counts as no key and sends no request. Test in Task 3.
3. **The IDE restarts with a router model selected, before any catalog has loaded:** the saved provider and model are restored after the background load, not replaced by another provider. Test in Task 4.
4. **Catalog entries with null or blank ids, ids containing slashes or `@`, duplicates across endpoints, or non-numeric prices:** bad entries are skipped, ids stay verbatim, first entry wins, and a bad price becomes 0. Test in Task 2.
5. **A key is removed in settings while its catalog is cached:** Apply clears the catalog, so the router shows no models and sends no requests. Test in Task 5.

---

### Task 1: Router credentials and enabled state

**Files:**
- Modify: `src/main/java/com/devoxx/genie/service/credentials/CredentialService.java`
- Modify: `src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java`
- Modify: `src/main/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialService.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`
- Modify: `src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java`
- Test: `src/test/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialServiceSubKeyTest.java`
- Test: `src/test/java/com/devoxx/genie/ui/settings/DevoxxGenieStateServiceRouterTest.java`

**Interfaces:**
- Produces: `CredentialService.getCredential(String subKey): String`, `setCredential(String subKey, String value)`, `removeCredential(String subKey)`
- Produces: `static CredentialAttributes CredentialKey.attributesFor(String subKey)`
- Produces on state service and settings interface: `String getRouterKey(String subKey)`, `void setRouterKey(String subKey, String value)`
- Produces on state service: `boolean isRouterEnabled(ModelProvider p)`, `void setRouterEnabled(ModelProvider p, boolean enabled)`, persisted `Map<String, Boolean> routerEnabled`, keyed by `p.name()`

- [ ] **Step 1: Write failing tests.** `subKey_roundTrips`; `enumAndSubKey_shareOneEntry` (set via `OPEN_ROUTER_KEY`, read via `"openRouterKey"`, and the reverse); `emptyString_removesEntry`. For state: `isRouterEnabled_defaultsFalse`; `loadState_seedsOpenRouterFromLegacyFlag` (legacy true, no map entry, so `isRouterEnabled(OpenRouter)` is true); `loadState_keepsExistingMapValue` (legacy true, map false stays false); `routerEnabled_survivesXmlRoundTrip` using `XmlSerializer.serialize` and `deserialize`.

- [ ] **Step 2: Run and confirm failure.** Expected: compile errors for the missing methods.

- [ ] **Step 3: Implement.** Change `PasswordSafeCredentialService`'s `memoryFallback` and `emptyKeys` to `String`-keyed collections, with enum methods delegating via `key.getSubKey()`. `CredentialKey.attributes()` delegates to `attributesFor`. Seed in `loadState` after `copyBean`, only when the map has no `OpenRouter` entry. Keep the legacy `isOpenRouterEnabled` field, the `openRouterKey` landing-pad field and `CredentialKey.OPEN_ROUTER_KEY`, because the plaintext migration needs them.

- [ ] **Step 4: Run the two test classes and `DevoxxGenieStateServiceTest`.** Expected: no failures.

- [ ] **Step 5: Commit** with message `feat(router): add sub-key credentials and router enabled state`.

### Task 2: Definitions and catalog reader

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterDefinition.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/CatalogFields.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParser.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistry.java`
- Modify: `src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java`
- Modify: `src/main/java/com/devoxx/genie/service/analytics/ProviderType.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogParserTest.java`
- Test: `src/test/resources/router/openrouter-models.json`, `src/test/resources/router/requesty-models.json`

**Interfaces:**
- Produces:

```java
record RouterDefinition(ModelProvider provider, String label, String baseUrl,
    List<String> modelEndpoints, String credentialSubKey, String keyUrl,
    @Nullable String hint, @Nullable CatalogFields catalogFields, @Nullable Integer maxTokens) {}

record CatalogFields(@Nullable String displayName, List<String> contextWindow,
    @Nullable String inputPrice, @Nullable String outputPrice, double priceScale,
    @Nullable String filterField, @Nullable String filterValue) {}

final class RouterCatalogParser {
    static final int DEFAULT_CONTEXT_WINDOW = 128_000;
    static List<LanguageModel> parse(RouterDefinition def, String json); // throws JsonParseException when not {"data":[...]}
}

final class RouterRegistry {
    static List<RouterDefinition> definitions();
    static Optional<RouterDefinition> find(ModelProvider provider);
    static boolean isGeneric(ModelProvider provider);
}
```

- Produces: `ModelProvider.Type.ROUTER`, mapped to `CLOUD` by `ProviderType`.

- [ ] **Step 1: Write failing tests.** Use realistic samples containing these values:
  - `openRouter_mapsNestedFields`: `pricing.prompt` as the string `"0.0000015"` gives `inputCost` 1.5; `context_length` null falls back to `top_provider.context_length`; `displayName` comes from `name`.
  - `requesty_keepsChatEntriesOnly`: a `api: "embedding"` entry is dropped, a missing `api` is kept. The field filter treats an absent field as a match.
  - `noMapping_usesDefaults`: id only gives displayName = id, window 128000, cost 0, `apiKeyUsed` true.
  - `skipsBadEntries_keepsIdsVerbatim` (Review Focus 4): null entry and blank id skipped; `vendor/model`, `@cf/x` unchanged; a non-numeric price gives 0.
  - `invalidJson_throws`: HTML and `{}` throw `JsonParseException`.
  - `providerType_routerMapsToCloud`.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement.** Use Gson's `JsonParser`, walk dotted paths, and accept numbers or numeric strings. Scale prices with the existing `BigDecimal` rounding from `OpenRouterChatModelFactory.convertAndScalePrice` (6 decimals, `HALF_UP`). Add `ROUTER` to `Type`, and move `OpenRouter` and `Cloudflare` to it. Add the OpenRouter definition: base URL `https://openrouter.ai/api/v1/`, endpoint `/models`, sub-key `openRouterKey`, key URL `https://openrouter.ai/settings/keys`, `maxTokens` 4000, mapping per the spec table with scale 1,000,000.

- [ ] **Step 4: Run the test class.** Expected: no failures.

- [ ] **Step 5: Commit** with message `feat(router): add router definitions and catalog parser`.

### Task 3: RouterChatModelFactory

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterChatModelFactory.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterCatalogClient.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterChatModelFactoryTest.java`

**Interfaces:**
- Consumes: Task 2 types.
- Produces:

```java
interface RouterCatalogClient { String fetch(String url, String apiKey) throws IOException; }

public class RouterChatModelFactory implements ChatModelFactory {
    public RouterChatModelFactory(RouterDefinition def);
    RouterChatModelFactory(RouterDefinition def, BooleanSupplier enabled, Supplier<String> key, RouterCatalogClient client, LongSupplier clockMillis); // tests
    public List<LanguageModel> getModels();        // may fetch; never on the EDT
    public List<LanguageModel> getCachedModels();  // never fetches or reads credentials
    public void resetModels();
    protected void notifyFetchFailure();           // overridable for tests
}
```

- [ ] **Step 1: Write failing tests**, using a fake client and clock:
  - `getCachedModels_neverFetches`: zero client calls and zero key reads before a load.
  - `success_isCached`: a second `getModels()` makes no call.
  - `partialFailure_cachesSuccessfulEndpoint`, and `merge_firstEndpointWinsById`.
  - `totalFailure_backsOffFor60Seconds`: no call at 59,999 ms, a call at 60,000 ms. An empty catalog behaves the same way.
  - `resetModels_clearsBackoffAndCache`.
  - `noKey_sendsNoRequest` and `whitespaceKey_sendsNoRequest`; `key_isTrimmed` (Review Focus 2).
  - `disabled_readsNoKeyAndSendsNoRequest`: returns an empty list with zero key reads and zero client calls, and records no failure or backoff, so re-enabling can fetch at once.
  - `queuedLoad_executedAfterDisabling_sendsNoRequest`: submit `getModels()` to a held executor while the router is enabled, disable the router, run the queued task, and assert zero key reads and zero client calls.
  - `disabled_keepsCachedModelsOutOfGetModels`: with a catalog already cached, a disabled router's `getModels()` returns an empty list.
  - `html200_and_401_returnEmptyAndNotifyOnce` (Review Focus 1): no exception, and the key string isn't in the exception message passed on.
  - `staleFetch_doesNotRepopulate`: the client blocks on a latch with key A; `resetModels()` runs and the key becomes B; on release the A result is dropped, that caller gets the current snapshot, and the next `getModels()` calls with key B.
  - `staleFailure_doesNotStartBackoff`.
  - `concurrentReaders_seeCompleteSnapshots`, and a returned list is unmodifiable.
  - `chatAndStreaming_callBaseUrlWithBearerKey`: a loopback `HttpServer` as `baseUrl` receives `POST /chat/completions`, `Authorization: Bearer <key>`, and no extra attribution headers. Use a mock `CustomChatModel`; mock `DevoxxGenieStateService.getInstance` and `MCPService.isMCPEnabled` statically, as `getListener()` needs them.
  - `maxTokens_fixedByDefinition_elseFromCustomChatModel`.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement.** Capture state in this order so a reset can't slip between steps:

```java
if (!enabled.getAsBoolean()) return List.of();  // 1. no key read, no request, no backoff
long gen = generation.get();     // 2. capture the generation
String key = keySupplier.get();  // 3. then read the key
// 4. fetch without holding a lock
synchronized (lock) {
    if (gen != generation.get()) return snapshot();  // stale: publish nothing
    // publish the immutable result, or record the failure time
}
```

  `resetModels()` increments the generation and clears state under the same lock. Keep the model-builder options from `OpenRouterChatModelFactory`. The default client uses the fast-fail settings from `NvidiaChatModelFactory` (connect 3s, read and write 5s, no connection retry) and sends `Authorization: Bearer`. Join URLs after stripping a trailing slash from `baseUrl`. The default key supplier is `getApiKey(def.provider())`, and the default enabled supplier is `DevoxxGenieStateService.getInstance().isRouterEnabled(def.provider())`, evaluated at the start of each `getModels()` call. The default notification text is `"Unable to reach <label>, please try again later."`.

- [ ] **Step 4: Run the test class.** Expected: no failures.

- [ ] **Step 5: Commit** with message `feat(router): add generic router chat model factory`.

### Task 4: Move OpenRouter onto the generic path

**Files:**
- Modify: `src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java`
- Modify: `src/main/java/com/devoxx/genie/service/LLMProviderService.java`
- Modify: `src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java`
- Modify: `src/main/java/com/devoxx/genie/ui/panel/LlmProviderPanel.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java` (the two enabled `switch`es only)
- Modify: `src/main/java/com/devoxx/genie/util/DefaultLLMSettingsUtil.java`
- Modify: `src/main/java/com/devoxx/genie/controller/ProjectContextController.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java` (lines using `isOpenRouterEnabled` and `setOpenRouterEnabled` only)
- Modify: `src/main/resources/META-INF/plugin.xml` (remove the `OpenRouterService` entry)
- Delete: `chatmodel/cloud/openrouter/` and `model/openrouter/`
- Delete: `src/test/java/com/devoxx/genie/chatmodel/cloud/openrouter/OpenRouterChatModelFactoryTest.java`, `src/test/java/com/devoxx/genie/service/openrouter/OpenRouterServiceTest.java`, after confirming their cases are covered by Tasks 2 and 3
- Modify tests: `ChatModelFactoryProviderTest`, `LlmProviderPanelTest`, `DefaultLLMSettingsUtilTest`, `ProjectContextControllerTest`
- Test: `src/test/java/com/devoxx/genie/service/LLMProviderServiceRouterTest.java`
- Test: `src/test/java/com/devoxx/genie/service/models/LLMModelRegistryServiceRouterTest.java`

**Interfaces:**
- Consumes: `RouterRegistry`, `RouterChatModelFactory.getCachedModels()`, `isRouterEnabled`, `getRouterKey`.
- Produces: `ChatModelFactoryProvider.getFactoryByProvider("OpenRouter")` returns the shared `RouterChatModelFactory`.

- [ ] **Step 1: Write failing tests:**
  - `enabledRouter_listedWithoutModelsOrCredentials`: enabled OpenRouter, no registry models, `getAvailableModelProviders()` contains it, and the credential service is never called.
  - `disabledRouter_notListed`.
  - `registry_mergesCachedOnly_noFetch`: `LLMModelRegistryService.getModels()` calls neither client nor credentials; once a catalog is cached, its models appear, keyed `"OpenRouter:<model>"`; a disabled router's cached models don't appear.
  - `genericRouter_isApiKeyBased_andSupported_andRefreshable`; `cloudflare_unchangedAtGenericSites`: Cloudflare stays false for `isApiKeyBasedProvider` and `isSupportedProvider`.
  - `restart_restoresRouterSelectionAfterBackgroundLoad` (Review Focus 3) in `LlmProviderPanelTest`: state holds OpenRouter and a model; after the asynchronous load, the combos show that pair and state is unchanged.
  - Update `ChatModelFactoryProviderTest`'s OpenRouter row to `RouterChatModelFactory.class`.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement.**
  - In `ChatModelFactoryProvider`, replace the OpenRouter line with a loop over `RouterRegistry.definitions()`.
  - In `LLMProviderService`, replace OpenRouter's key entry with a registry loop, and append enabled generic routers in `getEnabledCloudModelProviders` directly from `isRouterEnabled`, keeping `.distinct()`.
  - Replace `case OpenRouter ->` with `default -> state.isRouterEnabled(provider)` in the two panel and agent `switch`es that have no default, and in the `LLMProviderService` switch (replacing `default -> false`).
  - Use `RouterRegistry.isGeneric` in `isLocalProvider`, `isApiKeyBasedProvider` and `isSupportedProvider`.
  - Replace `getOpenRouterModels` with `addRouterModels`, using shared factories and `getCachedModels()`.
  - Leave `ProjectContentService` as it is: its OpenRouter `case` keeps the R50K tokenizer.
  - In `LLMProvidersConfigurable`, read and write OpenRouter's flag via `isRouterEnabled` and `setRouterEnabled`.

- [ ] **Step 4: Check the not-found callers.** Read `TokenCalculationService:159`, `AddDirectoryAction:71` and `LanguageModelCostSettingsComponent.loadCurrentCosts`, and confirm each handles a missing router model without throwing. Add a small guard only where one would throw.

- [ ] **Step 5: Run the full test suite:** `./gradlew test 2>&1 | grep -E "FAILED|failed"`. Expected: no failures. Investigate any failure rather than deleting the test.

- [ ] **Step 6: Commit** with message `refactor(router): move OpenRouter onto the generic router factory`.

### Task 5: Routers settings tab

**Files:**
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`, `src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java` (remove `getOpenRouterKey` and `setOpenRouterKey`; update `AzureOpenAiChatModelFactoryTest` and `DevoxxGenieStateServiceTest`)
- Test: `src/test/java/com/devoxx/genie/ui/settings/llm/LLMProvidersRouterSettingsTest.java`

**Interfaces:**
- Consumes: `RouterRegistry`, Task 1 state methods, `RouterChatModelFactory.resetModels()`.
- Produces: `record RouterRow(JCheckBox enabled, JPasswordField key)` and `Map<ModelProvider, RouterRow> getRouterRows()` on `LLMProvidersComponent`. `static final String ROUTERS_WARNING`.

- [ ] **Step 1: Write failing tests:**
  - `tabs_inOrder`: `Local`, `Cloud Direct`, `Routers`, `Custom OpenAI`.
  - `routersTab_showsWarningAndAllRouters`: includes `ROUTERS_WARNING`, a row per definition and Cloudflare's fields; the Cloud Direct tab has no OpenRouter or Cloudflare row, and still has NVIDIA, Azure OpenAI and Bedrock.
  - `warning_matchesSpecText`.
  - Configurable: `isModified_detectsKeyAndCheckbox`, `apply_persistsKeyAndFlag`, `reset_restoresBoth`, `validation_flagsEnabledRouterWithoutKey`.
  - `apply_resetsCatalogOnlyForChangedRouter`.
  - `apply_keyRemoved_clearsCatalogAndStopsRequests` (Review Focus 5).
  - `apply_unchanged_doesNotDeleteKey`: an OpenRouter key survives Apply with no edits.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement.** Rename the tab label at `LLMProvidersComponent.java:320`. Add `createRoutersPanel()` with the warning, a loop using `addProviderSettingRow` and `createTextWithPasswordButton`, then Cloudflare's existing rows moved over unchanged, including their item listeners. Render the warning with an existing info or warning component, and use this exact text:

> **Routers forward your prompts and code to other providers.** Anything you send, including project files added to context, may pass through the router and whichever provider it selects. Each has its own logging, retention and training policies, and a provider may be in another country. DevoxxGenie doesn't verify these services or their privacy claims, and listing one here isn't an endorsement. Check the router's terms before enabling it, and avoid sending confidential code or secrets unless your organisation allows it.

  In the configurable, replace OpenRouter's lines in `isModified`, `apply`, `reset`, `validateEnabledProviders` and `hasEnabledAuxCloudKey` with loops over the definitions. Remove the OpenRouter field and checkbox from the component. In `apply`, capture old key and flag, then call `resetModels()` on a router's factory when either changed.

- [ ] **Step 4: Run the test class and `LlmProviderPanelTest`.** Expected: no failures.

- [ ] **Step 5: Commit** with message `feat(router): add Routers settings tab with privacy warning`.

### Task 6: Asynchronous agent settings loaders

**Files:**
- Create: `src/main/java/com/devoxx/genie/ui/settings/agent/ModelComboLoader.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/agent/AgentSettingsComponent.java` (lines 446-481, 659-677, 690-730, 1294, `toConfig`)
- Test: `src/test/java/com/devoxx/genie/ui/settings/agent/ModelComboLoaderTest.java`

**Interfaces:**
- Produces:

```java
final class ModelComboLoader {
    ModelComboLoader(ComboBox<LanguageModel> combo);                                     // pooled thread + invokeLater with ModalityState.any()
    ModelComboLoader(ComboBox<LanguageModel> combo, Executor background, Executor edt);  // tests
    void load(@Nullable ModelProvider provider, @Nullable String modelToSelect);         // EDT only
    String effectiveModelName();   // selected model name, or the pending saved name; "" when neither
}
```

- [ ] **Step 1: Write failing tests** with controllable executors:
  - `nonRouter_loadsSynchronously_asToday`.
  - `router_doesNotFetchOnCallingThread`.
  - `savedModel_selectedAfterAsyncLoad`.
  - `lateResult_forPreviousProvider_discarded`.
  - `effectiveModelName_duringLoad_returnsSavedModel`.
  - `failedOrEmptyLoad_keepsSavedModelName`.
  - `userChangesProvider_clearsPendingModel`.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement** `ModelComboLoader` with an EDT-only generation counter, and wire it in:
  - `updateSubAgentModelComboBox()` and `updateRowModelComboBox()` delegate to `load(provider, null)`.
  - `restoreProviderSelection` and `selectProvider` call `load(provider, savedModel)` and drop their immediate `restoreModelSelection` and `selectModel` calls.
  - `getSelectedModelName()` and `toConfig()` use `effectiveModelName()`.

  Providers that aren't generic routers keep today's synchronous path, so their behaviour doesn't change.

- [ ] **Step 4: Run the test class.** Expected: no failures.

- [ ] **Step 5: Commit** with message `fix(router): load router models off the EDT in agent settings`.

### Task 7: Add Requesty and DemonRoute, plus docs

**Files:**
- Modify: `src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java`
- Modify: `src/main/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistry.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/cloud/router/RouterRegistryTest.java`
- Modify: `src/test/java/com/devoxx/genie/chatmodel/ChatModelFactoryProviderTest.java`, `src/test/java/com/devoxx/genie/ui/panel/LlmProviderPanelTest.java`
- Modify docs: `docusaurus/docs/llm-providers/cloud-models.md`, `docusaurus/docs/llm-providers/overview.md`, `docusaurus/docs/features/overview.md`, `docusaurus/docs/getting-started/quick-start-cloud.md`, `docusaurus/docs/getting-started/faq.md`, `docusaurus/docs/getting-started/why-devoxxgenie.md`, `README.md`, `AGENTS.md`, `CLAUDE.md`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Consumes: `RouterDefinition`, `RouterRegistry`.

- [ ] **Step 1: Write the failing guard test.** `everyDefinition_isFullyWired`, iterating all definitions, asserts: a `RouterChatModelFactory` from `ChatModelFactoryProvider`; `LLMProviderService.requiresApiKey`; `isApiKeyBasedProvider`; `isSupportedProvider`; a row in `getRouterRows()`; enabled state defaults to off; unique providers, sub-keys and HTTPS base URLs. `registry_hasExpectedRouters`: OpenRouter, Requesty and DemonRoute. Requesty's sample from Task 2 parses using its registered definition.

- [ ] **Step 2: Run and confirm failure.**

- [ ] **Step 3: Implement.** Add two enum lines of type `ROUTER` and two definitions:
  - **Requesty:** base `https://router.requesty.ai/v1`; endpoints `/models/managed`, `/models`; sub-key `requestyKey`; key URL `https://app.requesty.ai/api-keys`; `maxTokens` 4000; fields `context_window`, `input_price`, `output_price`, scale 1,000,000, filter `api` = `chat`.
  - **DemonRoute:** base `https://api.demonroute.com/v1`; endpoint `/models`; sub-key `demonRouteKey`; key URL `https://demonroute.com`; no mapping; no fixed `maxTokens`.

  Hints are neutral and factual, with no marketing. Update `CLAUDE.md`'s "Adding a New LLM Provider" with the two-line router path.

- [ ] **Step 4: Update docs.** Add a Routers section to `cloud-models.md` with the warning's message and the upstream authors' Requesty setup notes, including its EU endpoint note. Update provider lists and the renamed tab. Add a `plugin.xml` change note that says DemonRoute was not tested live.

- [ ] **Step 5: Run the full suite and verifier:** `./gradlew test 2>&1 | grep -E "FAILED|failed"`, then `./gradlew verifyPlugin`. Expected: no failures.

- [ ] **Step 6: Commit** with message `feat(router): add Requesty and DemonRoute as router definitions`, with a `Co-Authored-By` trailer for the Requesty PR author using the identity from that PR's commits, in addition to the session's attribution line.

### Task 8: Verify, open the PR, close the replaced PRs

**Files:** none beyond task notes.

- [ ] **Step 1: Manual check in the IDE** with `./gradlew runIde`. Check the tabs and warning, that an existing OpenRouter key and selection carry over, and that toggling a checkbox updates the chat selection. Requesty needs a real key: if none is available, record that as untested in the PR instead of claiming it works.

- [ ] **Step 2: Finalise TASK-272.** Read `get_backlog_instructions(task-finalization)` first and follow it. Criterion #12 stays open until Step 4.

- [ ] **Step 3: Open the PR** against `master`, ending with the attribution line, and list what wasn't tested live. Update issue #1340 with a link.

- [ ] **Step 4: Close #1303 and #1378** with a comment thanking the author, explaining that the router became one definition on the generic factory, and linking the new PR. Then check criterion #12.
