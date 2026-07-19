# Cloudflare AI Gateway Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-class Cloudflare LLM provider that talks to the Cloudflare AI Gateway OpenAI-compatible (`/compat`) endpoint, assembling the base URL from an account ID + gateway name and authenticating with a single Cloudflare API token.

**Architecture:** Mirror two existing patterns — OpenRouter (a gateway provider whose models are fetched dynamically and injected into `LLMModelRegistryService.getModels()` when a key exists) and Custom OpenAI (best-effort `/models` probe + manual model-name override). A new `CloudflareChatModelFactory` builds an `OpenAiChatModel`/`OpenAiStreamingChatModel` against the assembled base URL. A pure `CloudflareGatewayUrl` helper does URL assembly so it is unit-testable in isolation.

**Tech Stack:** Java 21, langchain4j `OpenAiChatModel`, Lombok, JUnit 5 + Mockito + AssertJ, IntelliJ Platform settings (`DevoxxGenieStateService`).

## Global Constraints

- Build with JDK 21: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home`
- Test invocation piped to grep per repo convention: `./gradlew test --tests "ClassName" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
- Provider display name and enum constant name are both exactly `Cloudflare`.
- Auth is single-token BYOK only: `Authorization: Bearer <cloudflareKey>`. No dual-header mode.
- Base URL format (verbatim): `https://gateway.ai.cloudflare.com/v1/{accountId}/{gatewayName}/compat`
- Gateway name field default is exactly `default`.
- Secret token stored via the credential store (keychain), never as a plaintext option field.
- Never cache built `LanguageModel` objects — cache model ids only (settings-derived fields must stay live).
- Reuse the `LocalLLMProviderUtil.getModelsFromUrl(String, Class, OkHttpClient, String bearerToken)` overload (already on this branch from PR #1211).

---

### Task 1: Provider enum, credential key, and settings state

**Files:**
- Modify: `src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java`
- Modify: `src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java`
- Modify: `src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java`
- Test: `src/test/java/com/devoxx/genie/model/enumarations/ModelProviderCloudflareTest.java`

**Interfaces:**
- Consumes: nothing (foundation task).
- Produces:
  - `ModelProvider.Cloudflare` (constant name `Cloudflare`, display name `Cloudflare`, `Type.CLOUD`).
  - `CredentialKey.CLOUDFLARE_KEY` with subKey `"cloudflareKey"`.
  - State accessors on `DevoxxGenieStateService` / `DevoxxGenieSettingsService`:
    - `String getCloudflareKey()` / `void setCloudflareKey(String)` (keychain-backed)
    - `String getCloudflareAccountId()` / `void setCloudflareAccountId(String)` (default `""`)
    - `String getCloudflareGatewayName()` / `void setCloudflareGatewayName(String)` (default `"default"`)
    - `String getCloudflareModelName()` / `void setCloudflareModelName(String)` (default `""`)
    - `boolean isCloudflareModelNameEnabled()` / `void setCloudflareModelNameEnabled(boolean)` (default `false`)
    - `boolean isCloudflareEnabled()` / `void setCloudflareEnabled(boolean)` (default `false`)

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/devoxx/genie/model/enumarations/ModelProviderCloudflareTest.java`:

```java
package com.devoxx.genie.model.enumarations;

import com.devoxx.genie.service.credentials.CredentialKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelProviderCloudflareTest {

    @Test
    void cloudflareProviderIsRegisteredAsCloud() {
        ModelProvider provider = ModelProvider.fromString("Cloudflare");
        assertThat(provider).isEqualTo(ModelProvider.Cloudflare);
        assertThat(provider.getType()).isEqualTo(ModelProvider.Type.CLOUD);
        assertThat(provider.getName()).isEqualTo("Cloudflare");
    }

    @Test
    void cloudflareCredentialKeyMatchesLegacyFieldName() {
        assertThat(CredentialKey.CLOUDFLARE_KEY.getSubKey()).isEqualTo("cloudflareKey");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.model.enumarations.ModelProviderCloudflareTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: compilation failure — `Cloudflare` / `CLOUDFLARE_KEY` do not exist.

- [ ] **Step 3: Add the enum constant**

In `ModelProvider.java`, add to the `Type.CLOUD` block (after `Nvidia("NVIDIA", Type.CLOUD),`):

```java
    Cloudflare("Cloudflare", Type.CLOUD),
```

- [ ] **Step 4: Add the credential key**

In `CredentialKey.java`, add after `NVIDIA_KEY ("nvidiaKey"),`:

```java
    CLOUDFLARE_KEY       ("cloudflareKey"),
```

- [ ] **Step 5: Add state fields in `DevoxxGenieStateService.java`**

Near the Custom OpenAI plain fields (around line 127-149), add:

```java
    private String cloudflareAccountId = "";
    private String cloudflareGatewayName = "default";
    private String cloudflareModelName = "";
    private boolean isCloudflareModelNameEnabled = false;
    private boolean isCloudflareEnabled = false;
```

In the secret-field option block (near `@OptionTag("customOpenAIApiKey")`, around line 134), add:

```java
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) @OptionTag("cloudflareKey")
    private String cloudflareKey = "";
```

In the hand-written credential accessor block (near line 898, alongside `getCustomOpenAIApiKey`), add:

```java
    @Transient @Override public @NotNull String getCloudflareKey()       { return creds().getCredential(CredentialKey.CLOUDFLARE_KEY); }
    @Transient @Override public void          setCloudflareKey(String v) { creds().setCredential(CredentialKey.CLOUDFLARE_KEY, v); }
```

- [ ] **Step 6: Declare accessors in `DevoxxGenieSettingsService.java`**

Add alongside the other Custom OpenAI declarations:

```java
    String getCloudflareKey();
    void setCloudflareKey(String key);
    String getCloudflareAccountId();
    void setCloudflareAccountId(String accountId);
    String getCloudflareGatewayName();
    void setCloudflareGatewayName(String gatewayName);
    String getCloudflareModelName();
    void setCloudflareModelName(String modelName);
    boolean isCloudflareModelNameEnabled();
    void setCloudflareModelNameEnabled(boolean enabled);
    boolean isCloudflareEnabled();
    void setCloudflareEnabled(boolean enabled);
```

Note: the plain (non-secret) fields' getters/setters are Lombok-generated on `DevoxxGenieStateService`; declaring them on the interface makes them visible to callers. Lombok generates `isCloudflareEnabled()`/`setCloudflareEnabled(boolean)` and `isCloudflareModelNameEnabled()`/`setCloudflareModelNameEnabled(boolean)` for the `boolean isX` fields, matching the Custom OpenAI convention.

- [ ] **Step 7: Run test to verify it passes**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.model.enumarations.ModelProviderCloudflareTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, 2 tests pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/devoxx/genie/model/enumarations/ModelProvider.java \
        src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java \
        src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java \
        src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java \
        src/test/java/com/devoxx/genie/model/enumarations/ModelProviderCloudflareTest.java
git commit -m "feat(cloudflare): add provider enum, credential key, and settings state"
```

---

### Task 2: CloudflareGatewayUrl helper + CloudflareChatModelFactory

**Files:**
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareGatewayUrl.java`
- Create: `src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareChatModelFactory.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareGatewayUrlTest.java`
- Test: `src/test/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareChatModelFactoryTest.java`

**Interfaces:**
- Consumes: `DevoxxGenieStateService` accessors from Task 1; `LocalLLMProviderUtil.getModelsFromUrl(String, Class, OkHttpClient, String)`; `ResponseDTO`/`Model` from `com.devoxx.genie.model.gpt4all`.
- Produces:
  - `CloudflareGatewayUrl.compatBaseUrl(String accountId, String gatewayName)` → `String` (the `.../compat` base, no trailing slash) or `null` when `accountId` is blank.
  - `CloudflareChatModelFactory implements ChatModelFactory` with `getModels()`, `createChatModel`, `createStreamingChatModel`, `resetModels`.

- [ ] **Step 1: Write the failing test for URL assembly**

Create `src/test/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareGatewayUrlTest.java`:

```java
package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudflareGatewayUrlTest {

    @Test
    void assemblesCompatBaseUrlFromAccountAndGateway() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("acct123", "default"))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/compat");
    }

    @Test
    void trimsWhitespaceAndStraySlashesInInputs() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("  acct123 ", " my-gw/ "))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/my-gw/compat");
    }

    @Test
    void returnsNullWhenAccountIdBlank() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("   ", "default")).isNull();
    }

    @Test
    void fallsBackToDefaultGatewayWhenBlank() {
        assertThat(CloudflareGatewayUrl.compatBaseUrl("acct123", "  "))
                .isEqualTo("https://gateway.ai.cloudflare.com/v1/acct123/default/compat");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareGatewayUrlTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: compilation failure — `CloudflareGatewayUrl` does not exist.

- [ ] **Step 3: Implement `CloudflareGatewayUrl`**

Create `src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareGatewayUrl.java`:

```java
package com.devoxx.genie.chatmodel.cloud.cloudflare;

import org.jetbrains.annotations.Nullable;

/**
 * Assembles the Cloudflare AI Gateway OpenAI-compatible base URL from an account id and gateway name:
 * {@code https://gateway.ai.cloudflare.com/v1/{accountId}/{gatewayName}/compat}.
 *
 * <p>langchain4j's OpenAI client then appends {@code /chat/completions}; the model probe appends
 * {@code /models}. Both resolve correctly against this {@code /compat} base.</p>
 */
public final class CloudflareGatewayUrl {

    private static final String ROOT = "https://gateway.ai.cloudflare.com/v1/";
    /** Cloudflare auto-creates a gateway named "default" on first authenticated request. */
    public static final String DEFAULT_GATEWAY = "default";

    private CloudflareGatewayUrl() {
    }

    /**
     * @param accountId   the Cloudflare account id (required)
     * @param gatewayName the gateway name; blank falls back to {@link #DEFAULT_GATEWAY}
     * @return the {@code .../compat} base URL (no trailing slash), or {@code null} when the account id is blank
     */
    public static @Nullable String compatBaseUrl(String accountId, String gatewayName) {
        String account = accountId == null ? "" : stripSlashes(accountId.trim());
        if (account.isEmpty()) {
            return null;
        }
        String gateway = gatewayName == null ? "" : stripSlashes(gatewayName.trim());
        if (gateway.isEmpty()) {
            gateway = DEFAULT_GATEWAY;
        }
        return ROOT + account + "/" + gateway + "/compat";
    }

    private static String stripSlashes(String value) {
        String v = value;
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
```

- [ ] **Step 4: Run URL test to verify it passes**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareGatewayUrlTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, 4 tests pass.

- [ ] **Step 5: Write the failing test for the factory**

Create `src/test/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareChatModelFactoryTest.java`:

```java
package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudflareChatModelFactoryTest {

    private static final String MODELS_URL =
            "https://gateway.ai.cloudflare.com/v1/acct123/default/compat/models";

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<MCPService> mockedMCPService;
    private DevoxxGenieStateService mockState;

    @BeforeEach
    void setUp() {
        mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getCloudflareAccountId()).thenReturn("acct123");
        when(mockState.getCloudflareGatewayName()).thenReturn("default");
        when(mockState.getCloudflareKey()).thenReturn("cf-token");
        when(mockState.getCloudflareModelName()).thenReturn("");
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(false);
        when(mockState.getAgentModeEnabled()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isMCPEnabled).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedMCPService != null) mockedMCPService.close();
    }

    @Test
    void createChatModelReturnsNonNull() {
        CustomChatModel model = new CustomChatModel();
        model.setModelName("openai/gpt-4o-mini");
        model.setTemperature(0.7);
        model.setTopP(0.9);
        model.setMaxTokens(256);
        model.setMaxRetries(3);
        model.setTimeout(30);
        ChatModel result = new CloudflareChatModelFactory().createChatModel(model);
        assertThat(result).isNotNull();
    }

    @Test
    void createStreamingChatModelReturnsNonNull() {
        CustomChatModel model = new CustomChatModel();
        model.setModelName("openai/gpt-4o-mini");
        model.setTemperature(0.7);
        model.setTopP(0.9);
        model.setTimeout(30);
        StreamingChatModel result = new CloudflareChatModelFactory().createStreamingChatModel(model);
        assertThat(result).isNotNull();
    }

    @Test
    void getModelsProbesAssembledCompatUrlWithBearerToken() {
        ResponseDTO response = buildResponse("openai/gpt-4o-mini", "anthropic/claude-4-5-sonnet");
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), eq("cf-token")))
                    .thenReturn(response);

            assertThat(new CloudflareChatModelFactory().getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("openai/gpt-4o-mini", "anthropic/claude-4-5-sonnet");

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), eq("cf-token")), times(1));
        }
    }

    @Test
    void getModelsWithModelNameOverrideSkipsProbe() {
        when(mockState.isCloudflareModelNameEnabled()).thenReturn(true);
        when(mockState.getCloudflareModelName()).thenReturn("openai/gpt-4o");
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            assertThat(new CloudflareChatModelFactory().getModels())
                    .extracting(LanguageModel::getModelName)
                    .containsExactly("openai/gpt-4o");
            util.verifyNoInteractions();
        }
    }

    @Test
    void getModelsWithBlankAccountIdReturnsEmpty() {
        when(mockState.getCloudflareAccountId()).thenReturn("");
        assertThat(new CloudflareChatModelFactory().getModels()).isEmpty();
    }

    @Test
    void getModelsCachesIdsAndProbesOnlyOnce() {
        try (MockedStatic<LocalLLMProviderUtil> util = Mockito.mockStatic(LocalLLMProviderUtil.class)) {
            util.when(() -> LocalLLMProviderUtil.getModelsFromUrl(
                            eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)))
                    .thenReturn(buildResponse("openai/gpt-4o-mini"));

            CloudflareChatModelFactory factory = new CloudflareChatModelFactory();
            factory.getModels();
            factory.getModels();

            util.verify(() -> LocalLLMProviderUtil.getModelsFromUrl(
                    eq(MODELS_URL), eq(ResponseDTO.class), any(OkHttpClient.class), nullable(String.class)), times(1));
        }
    }

    private static ResponseDTO buildResponse(String... modelIds) {
        ResponseDTO dto = new ResponseDTO();
        dto.setData(java.util.Arrays.stream(modelIds).map(id -> {
            com.devoxx.genie.model.gpt4all.Model m = new com.devoxx.genie.model.gpt4all.Model();
            m.setId(id);
            return m;
        }).toList());
        return dto;
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactoryTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: compilation failure — `CloudflareChatModelFactory` does not exist.

- [ ] **Step 7: Implement `CloudflareChatModelFactory`**

Create `src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/CloudflareChatModelFactory.java`:

```java
package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ThinkingSupport;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cloudflare AI Gateway provider. Talks to the OpenAI-compatible {@code /compat} endpoint of a
 * user-specified account + gateway, authenticating with a single Cloudflare API token (BYOK).
 *
 * <p>Follows the Custom OpenAI patterns: a fast-fail best-effort {@code /models} probe, a model-name
 * override that skips the probe, and caching of model ids only (never built {@link LanguageModel}s).</p>
 */
public class CloudflareChatModelFactory implements ChatModelFactory {

    private static final Logger LOG = Logger.getInstance(CloudflareChatModelFactory.class);

    /** Modern gateway models; Cloudflare's /compat/models does not report a context length. */
    private static final int DEFAULT_CONTEXT_WINDOW = 128_000;

    private static final OkHttpClient MODELS_PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .retryOnConnectionFailure(false)
            .build();

    /** Model ids from the last probe; {@code null} = not yet fetched. Cleared by {@link #resetModels()}. */
    private volatile List<String> cachedModelIds = null;

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl(state))
                .apiKey(apiKeyOrPlaceholder(state))
                .modelName(resolveModelName(customChatModel))
                .maxRetries(customChatModel.getMaxRetries())
                .temperature(customChatModel.getTemperature())
                .maxTokens(customChatModel.getMaxTokens())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .topP(customChatModel.getTopP())
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl(state))
                .apiKey(apiKeyOrPlaceholder(state))
                .modelName(resolveModelName(customChatModel))
                .temperature(customChatModel.getTemperature())
                .topP(customChatModel.getTopP())
                .timeout(Duration.ofSeconds(customChatModel.getTimeout()))
                .returnThinking(ThinkingSupport.isEnabled())
                .listeners(getListener())
                .build();
    }

    @Override
    public List<LanguageModel> getModels() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        // Honour an explicit model name: skip discovery entirely.
        if (state.isCloudflareModelNameEnabled()) {
            String override = state.getCloudflareModelName();
            if (override != null && !override.isBlank()) {
                return List.of(toLanguageModel(override.trim()));
            }
        }

        List<String> cached = cachedModelIds;
        if (cached == null) {
            cached = fetchModelIdsFromServer(state);
            if (!cached.isEmpty()) {
                cachedModelIds = cached;
            }
        }
        return cached.stream()
                .map(this::toLanguageModel)
                .collect(Collectors.toList());
    }

    @Override
    public void resetModels() {
        cachedModelIds = null;
    }

    private List<String> fetchModelIdsFromServer(@NotNull DevoxxGenieStateService state) {
        String base = CloudflareGatewayUrl.compatBaseUrl(state.getCloudflareAccountId(), state.getCloudflareGatewayName());
        if (base == null) {
            return Collections.emptyList();
        }
        try {
            String modelsUrl = base + "/models";
            String token = state.getCloudflareKey();
            String bearer = (token != null && !token.isBlank()) ? token : null;
            ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(modelsUrl, ResponseDTO.class, MODELS_PROBE_CLIENT, bearer);
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData().stream()
                    .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                    .map(Model::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.debug("Could not fetch models from Cloudflare AI Gateway: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private @NotNull LanguageModel toLanguageModel(@NotNull String modelId) {
        return LanguageModel.builder()
                .provider(ModelProvider.Cloudflare)
                .modelName(modelId)
                .displayName(modelId)
                .inputCost(0.0)
                .outputCost(0.0)
                .inputMaxTokens(DEFAULT_CONTEXT_WINDOW)
                .apiKeyUsed(true)
                .build();
    }

    private static String baseUrl(@NotNull DevoxxGenieStateService state) {
        String base = CloudflareGatewayUrl.compatBaseUrl(state.getCloudflareAccountId(), state.getCloudflareGatewayName());
        // langchain4j appends /chat/completions; a trailing slash keeps the join clean.
        return base == null ? null : base + "/";
    }

    private static String apiKeyOrPlaceholder(@NotNull DevoxxGenieStateService state) {
        String token = state.getCloudflareKey();
        return (token != null && !token.isBlank()) ? token : "na";
    }

    private static String resolveModelName(@NotNull CustomChatModel customChatModel) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        if (state.isCloudflareModelNameEnabled()) {
            String override = state.getCloudflareModelName();
            if (override != null && !override.isBlank()) {
                return override.trim();
            }
        }
        String selected = customChatModel.getModelName();
        return (selected != null && !selected.isBlank()) ? selected : "default";
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.chatmodel.cloud.cloudflare.*" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, 10 tests pass (4 URL + 6 factory).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/devoxx/genie/chatmodel/cloud/cloudflare/ \
        src/test/java/com/devoxx/genie/chatmodel/cloud/cloudflare/
git commit -m "feat(cloudflare): add gateway URL helper and chat model factory"
```

---

### Task 3: Register factory and wire provider discovery

**Files:**
- Modify: `src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java`
- Modify: `src/main/java/com/devoxx/genie/service/LLMProviderService.java`
- Modify: `src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java`
- Test: `src/test/java/com/devoxx/genie/service/LLMProviderServiceCloudflareTest.java`

**Interfaces:**
- Consumes: `CloudflareChatModelFactory` (Task 2), `ModelProvider.Cloudflare` and state accessors (Task 1).
- Produces: `Cloudflare` resolvable via `ChatModelFactoryProvider.getFactoryByProvider("Cloudflare")`; `LLMProviderService.requiresApiKey(Cloudflare) == true`; Cloudflare models injected into `LLMModelRegistryService.getModels()` when key + account id present.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/devoxx/genie/service/LLMProviderServiceCloudflareTest.java`:

```java
package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactory;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LLMProviderServiceCloudflareTest {

    @Test
    void cloudflareResolvesToItsFactory() {
        Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider("Cloudflare");
        assertThat(factory).isPresent();
        assertThat(factory.get()).isInstanceOf(CloudflareChatModelFactory.class);
    }

    @Test
    void cloudflareRequiresApiKey() {
        assertThat(LLMProviderService.requiresApiKey(ModelProvider.Cloudflare)).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.service.LLMProviderServiceCloudflareTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: FAIL — factory not registered / provider not in key map.

- [ ] **Step 3: Register the factory**

In `ChatModelFactoryProvider.java`, add the import:

```java
import com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactory;
```

and add to the `static {}` block (alphabetically near the top):

```java
        FACTORY_SUPPLIERS.put(ModelProvider.Cloudflare, CloudflareChatModelFactory::new);
```

- [ ] **Step 4: Wire the API key map and enabled switch in `LLMProviderService.java`**

In the `static {}` `providerKeyMap` block, add:

```java
        providerKeyMap.put(Cloudflare, () -> DevoxxGenieStateService.getInstance().getCloudflareKey());
```

In `getEnabledCloudModelProviders()`, add a case to the switch:

```java
                case Cloudflare -> stateService.isCloudflareEnabled();
```

- [ ] **Step 5: Inject Cloudflare models in `LLMModelRegistryService.java`**

Add the import:

```java
import com.devoxx.genie.chatmodel.cloud.cloudflare.CloudflareChatModelFactory;
```

In `getModels()`, add a call after `getOpenRouterModels(modelsCopy);`:

```java
        getCloudflareModels(modelsCopy);
```

Add the method next to `getOpenRouterModels(...)`:

```java
    private static void getCloudflareModels(Map<String, LanguageModel> modelsCopy) {
        // Add Cloudflare AI Gateway models when the API key and account id are configured.
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String apiKey = state.getCloudflareKey();
        String accountId = state.getCloudflareAccountId();
        if (apiKey != null && !apiKey.isEmpty() && accountId != null && !accountId.isBlank()) {
            new CloudflareChatModelFactory().getModels().forEach(model ->
                modelsCopy.put(ModelProvider.Cloudflare.getName() + ":" + model.getModelName(), model));
        }
    }
```

Add the import for `DevoxxGenieStateService` if not already present:

```java
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew test --tests "com.devoxx.genie.service.LLMProviderServiceCloudflareTest" --tests "com.devoxx.genie.chatmodel.ChatModelFactoryProviderTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`; the completeness check in `ChatModelFactoryProviderTest` still passes.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/devoxx/genie/chatmodel/ChatModelFactoryProvider.java \
        src/main/java/com/devoxx/genie/service/LLMProviderService.java \
        src/main/java/com/devoxx/genie/service/models/LLMModelRegistryService.java \
        src/test/java/com/devoxx/genie/service/LLMProviderServiceCloudflareTest.java
git commit -m "feat(cloudflare): register factory and wire provider discovery"
```

---

### Task 4: Settings UI

**Files:**
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java`
- Modify: `src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java`

**Interfaces:**
- Consumes: state accessors (Task 1). No new produced API — this task wires UI fields to existing state.
- Produces: getters on `LLMProvidersComponent` for the new fields, consumed only within `LLMProvidersConfigurable`:
  - `getCloudflareEnabledCheckBox()`, `getCloudflareApiKeyField()`, `getCloudflareAccountIdField()`, `getCloudflareGatewayNameField()`, `getCloudflareModelNameEnabledCheckBox()`, `getCloudflareModelNameField()` (all Lombok `@Getter`-generated from the field declarations).

- [ ] **Step 1: Declare the UI fields in `LLMProvidersComponent.java`**

Near the other cloud provider key fields (around line 109-117):

```java
    private final JPasswordField cloudflareApiKeyField = new JPasswordField(stateService.getCloudflareKey());
    private final JTextField cloudflareAccountIdField = new JTextField(stateService.getCloudflareAccountId());
    private final JTextField cloudflareGatewayNameField = new JTextField(stateService.getCloudflareGatewayName());
    private final JTextField cloudflareModelNameField = new JTextField(stateService.getCloudflareModelName());
```

Near the enabled checkboxes (around line 147-177):

```java
    private final JCheckBox cloudflareEnabledCheckBox = new JCheckBox("", stateService.isCloudflareEnabled());
    private final JCheckBox cloudflareModelNameEnabledCheckBox = new JCheckBox("", stateService.isCloudflareModelNameEnabled());
```

- [ ] **Step 2: Add the rows to the panel**

In the panel-building method, after the OpenRouter row (around line 271-272), add:

```java
        addProviderSettingRow(panel, gbc, "Cloudflare API Key", cloudflareEnabledCheckBox,
                createTextWithPasswordButton(cloudflareApiKeyField, "https://dash.cloudflare.com/profile/api-tokens"));
        addHintText(panel, gbc, "Cloudflare API token sent as <code>Authorization: Bearer</code>. Downstream provider keys (OpenAI, Anthropic, …) are stored in your Cloudflare dashboard (BYOK).");
        addProviderSettingRow(panel, gbc, "Cloudflare Account ID", cloudflareAccountIdField);
        addProviderSettingRow(panel, gbc, "Cloudflare Gateway Name", cloudflareGatewayNameField);
        addHintText(panel, gbc, "Base URL is built as <code>https://gateway.ai.cloudflare.com/v1/&lt;account&gt;/&lt;gateway&gt;/compat</code>. Gateway defaults to <code>default</code> (auto-created by Cloudflare).");
        addProviderSettingRow(panel, gbc, "Cloudflare Model", cloudflareModelNameEnabledCheckBox, cloudflareModelNameField);
        addHintText(panel, gbc, "When enabled, this exact provider/model name (e.g. <code>openai/gpt-4o-mini</code>) is used and the dropdown is not auto-discovered from <code>/compat/models</code>.");
```

- [ ] **Step 3: Add the enable-toggle listeners**

Near the other `addItemListener` calls (around line 334):

```java
        cloudflareEnabledCheckBox.addItemListener(e -> updateUrlFieldState(cloudflareEnabledCheckBox, cloudflareApiKeyField));
        cloudflareModelNameEnabledCheckBox.addItemListener(e -> updateUrlFieldState(cloudflareModelNameEnabledCheckBox, cloudflareModelNameField));
```

- [ ] **Step 4: Add getters (if the class does not use class-level `@Getter`)**

If `LLMProvidersComponent` uses field-level Lombok `@Getter` or class-level `@Getter`, the getters exist automatically. Otherwise, add:

```java
    public JPasswordField getCloudflareApiKeyField() { return cloudflareApiKeyField; }
    public JTextField getCloudflareAccountIdField() { return cloudflareAccountIdField; }
    public JTextField getCloudflareGatewayNameField() { return cloudflareGatewayNameField; }
    public JTextField getCloudflareModelNameField() { return cloudflareModelNameField; }
    public JCheckBox getCloudflareEnabledCheckBox() { return cloudflareEnabledCheckBox; }
    public JCheckBox getCloudflareModelNameEnabledCheckBox() { return cloudflareModelNameEnabledCheckBox; }
```

Verify first: `grep -n "@Getter\|getOpenRouterApiKeyField" src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java` — if `getOpenRouterApiKeyField()` is not explicitly defined, Lombok is generating it and you add nothing.

- [ ] **Step 5: Wire `isModified` in `LLMProvidersConfigurable.java`**

In the `isModified()` method, alongside the other provider checks (around line 74-141):

```java
        isModified |= isFieldModified(llmSettingsComponent.getCloudflareApiKeyField(), stateService.getCloudflareKey());
        isModified |= isFieldModified(llmSettingsComponent.getCloudflareAccountIdField(), stateService.getCloudflareAccountId());
        isModified |= isFieldModified(llmSettingsComponent.getCloudflareGatewayNameField(), stateService.getCloudflareGatewayName());
        isModified |= isFieldModified(llmSettingsComponent.getCloudflareModelNameField(), stateService.getCloudflareModelName());
        isModified |= stateService.isCloudflareEnabled() != llmSettingsComponent.getCloudflareEnabledCheckBox().isSelected();
        isModified |= stateService.isCloudflareModelNameEnabled() != llmSettingsComponent.getCloudflareModelNameEnabledCheckBox().isSelected();
```

- [ ] **Step 6: Wire `apply` in `LLMProvidersConfigurable.java`**

In `apply()`/the settings-persisting method (around line 193-235):

```java
        settings.setCloudflareKey(new String(llmSettingsComponent.getCloudflareApiKeyField().getPassword()));
        settings.setCloudflareAccountId(llmSettingsComponent.getCloudflareAccountIdField().getText());
        settings.setCloudflareGatewayName(llmSettingsComponent.getCloudflareGatewayNameField().getText());
        settings.setCloudflareModelName(llmSettingsComponent.getCloudflareModelNameField().getText());
        settings.setCloudflareEnabled(llmSettingsComponent.getCloudflareEnabledCheckBox().isSelected());
        settings.setCloudflareModelNameEnabled(llmSettingsComponent.getCloudflareModelNameEnabledCheckBox().isSelected());
```

- [ ] **Step 7: Wire `reset` in `LLMProvidersConfigurable.java`**

In `reset()` (around line 422-466):

```java
        llmSettingsComponent.getCloudflareApiKeyField().setText(settings.getCloudflareKey());
        llmSettingsComponent.getCloudflareAccountIdField().setText(settings.getCloudflareAccountId());
        llmSettingsComponent.getCloudflareGatewayNameField().setText(settings.getCloudflareGatewayName());
        llmSettingsComponent.getCloudflareModelNameField().setText(settings.getCloudflareModelName());
        llmSettingsComponent.getCloudflareEnabledCheckBox().setSelected(settings.isCloudflareEnabled());
        llmSettingsComponent.getCloudflareModelNameEnabledCheckBox().setSelected(settings.isCloudflareModelNameEnabled());
```

- [ ] **Step 8: Add the credential-validation check**

In the validation method (around line 264-268, where `checkApiKeyProvider(...)` is called per provider):

```java
        checkApiKeyProvider(problems, "Cloudflare", llmSettingsComponent.getCloudflareEnabledCheckBox(), llmSettingsComponent.getCloudflareApiKeyField());
```

- [ ] **Step 9: Compile and verify the full provider test set**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew compileJava 2>&1 | grep -E "error:|BUILD"`
Expected: `BUILD SUCCESSFUL`.

Then: `./gradlew test --tests "com.devoxx.genie.chatmodel.cloud.cloudflare.*" --tests "com.devoxx.genie.service.LLMProviderServiceCloudflareTest" --tests "com.devoxx.genie.model.enumarations.ModelProviderCloudflareTest" --tests "com.devoxx.genie.chatmodel.ChatModelFactoryProviderTest" 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, all pass.

- [ ] **Step 10: Manual smoke test**

Run: `export JAVA_HOME=/Users/stephan/Library/Java/JavaVirtualMachines/azul-21.0.5/Contents/Home; ./gradlew runIde`
In Settings → Tools → DevoxxGenie → LLM Providers: enable Cloudflare, enter a token + account id, leave gateway as `default`, Apply. Confirm the provider appears in the main model dropdown and (with a valid token) models load; verify a prompt round-trips.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersComponent.java \
        src/main/java/com/devoxx/genie/ui/settings/llm/LLMProvidersConfigurable.java
git commit -m "feat(cloudflare): add settings UI for the Cloudflare provider"
```

---

## Self-Review

**Spec coverage:**
- Enum constant → Task 1. ✅
- Factory + URL assembly + probe + override + graceful degradation → Task 2. ✅
- State fields (key/accountId/gatewayName/modelName/isModelNameEnabled/isEnabled) → Task 1. ✅
- `LLMProviderService` key map + enabled switch → Task 3. ✅
- `LLMModelRegistryService` injection with `apiKeyUsed(true)` → Task 3 (models built with `apiKeyUsed(true)` in Task 2's `toLanguageModel`). ✅
- `ChatModelFactoryProvider` registration → Task 3. ✅
- Settings UI (fields, hints, isModified/apply/reset/validation) → Task 4. ✅
- Bearer-token probe reuse → Task 2 `fetchModelIdsFromServer`. ✅
- Tests: URL helper, factory suite, provider resolution, completeness → Tasks 2 & 3. ✅

**Placeholder scan:** No TBD/TODO; every code step shows complete code. Step 4 of Task 4 is conditional (Lombok vs. explicit getters) but gives the exact grep to decide and the exact code for the explicit case.

**Type consistency:** `getCloudflareKey`/`getCloudflareAccountId`/`getCloudflareGatewayName`/`getCloudflareModelName`/`isCloudflareModelNameEnabled`/`isCloudflareEnabled` used identically across Tasks 1-4. `CloudflareGatewayUrl.compatBaseUrl(String, String)` and `toLanguageModel(String)` signatures match their call sites. `getModelsFromUrl(String, Class, OkHttpClient, String)` matches the PR #1211 overload. Probed URL `.../v1/acct123/default/compat/models` is consistent between `baseUrl`/`fetchModelIdsFromServer` and the test constant.
