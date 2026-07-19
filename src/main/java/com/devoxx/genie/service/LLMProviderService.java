package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.spec.AcpToolConfig;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.models.LLMModelRegistryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.devoxx.genie.model.enumarations.ModelProvider.*;

public class LLMProviderService {

    private static final EnumMap<ModelProvider, Supplier<String>> providerKeyMap = new EnumMap<>(ModelProvider.class);

    static {
        providerKeyMap.put(OpenAI, () -> DevoxxGenieStateService.getInstance().getOpenAIKey());
        providerKeyMap.put(Anthropic, () -> DevoxxGenieStateService.getInstance().getAnthropicKey());
        providerKeyMap.put(Mistral, () -> DevoxxGenieStateService.getInstance().getMistralKey());
        providerKeyMap.put(Groq, () -> DevoxxGenieStateService.getInstance().getGroqKey());
        providerKeyMap.put(DeepInfra, () -> DevoxxGenieStateService.getInstance().getDeepInfraKey());
        providerKeyMap.put(Google, () -> DevoxxGenieStateService.getInstance().getGeminiKey());
        providerKeyMap.put(DeepSeek, () -> DevoxxGenieStateService.getInstance().getDeepSeekKey());
        providerKeyMap.put(OpenRouter, () -> DevoxxGenieStateService.getInstance().getOpenRouterKey());
        providerKeyMap.put(Grok, () -> DevoxxGenieStateService.getInstance().getGrokKey());
        providerKeyMap.put(Kimi, () -> DevoxxGenieStateService.getInstance().getKimiKey());
        providerKeyMap.put(GLM, () -> DevoxxGenieStateService.getInstance().getGlmKey());
        providerKeyMap.put(Nvidia, () -> DevoxxGenieStateService.getInstance().getNvidiaKey());
        providerKeyMap.put(AzureOpenAI, () -> DevoxxGenieStateService.getInstance().getAzureOpenAIKey());
        providerKeyMap.put(Cloudflare, () -> DevoxxGenieStateService.getInstance().getCloudflareKey());
        providerKeyMap.put(Bedrock, () -> switch (Optional.ofNullable(DevoxxGenieStateService.getInstance().getAwsBedrockAuthMode())
                .orElse(AwsBedrockAuthMode.defaultMode())) {
            case ACCESS_KEY -> DevoxxGenieStateService.getInstance().getAwsSecretKey();
            case PROFILE -> DevoxxGenieStateService.getInstance().getAwsProfileName();
            case BEARER_TOKEN -> DevoxxGenieStateService.getInstance().getAwsBearerToken();
        });
    }

    @NotNull
    public static LLMProviderService getInstance() {
        return ApplicationManager.getApplication().getService(LLMProviderService.class);
    }

    public List<ModelProvider> getAvailableModelProviders() {
        List<ModelProvider> providers = new ArrayList<>();
        providers.addAll(getEnabledCloudModelProviders());
        providers.addAll(getLocalModelProviders());
        providers.addAll(getOptionalProviders());
        providers.addAll(getCliRunnersProvider());
        providers.addAll(getAcpRunnersProvider());

        return providers;
    }

    private List<ModelProvider> getLocalModelProviders() {
        // CLIRunners and ACPRunners are declared as Type.LOCAL but are only available
        // when the user has configured (and enabled) at least one CLI/ACP tool. They are
        // added conditionally by getCliRunnersProvider()/getAcpRunnersProvider(), so they
        // must be excluded from the blanket local list here to avoid appearing unconditionally.
        return ModelProvider.fromType(Type.LOCAL).stream()
                .filter(provider -> provider != ModelProvider.CLIRunners
                        && provider != ModelProvider.ACPRunners)
                .toList();
    }

    /**
     * Get the cloud LLM providers the user has enabled in the settings.
     *
     * <p>Deliberately checks only the non-secret enabled flags, never the credential
     * store: reading API keys here would hit the OS keychain (PasswordSafe) on every
     * IDE startup just to populate the provider combo box, triggering macOS keychain
     * dialogs. Keys are read lazily via {@link #getApiKey} when a prompt is executed.</p>
     *
     * @return List of LLM providers
     */
    private List<ModelProvider> getEnabledCloudModelProviders() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        return LLMModelRegistryService.getInstance().getModels()
            .stream()
            .filter(LanguageModel::isApiKeyUsed)
            .map(LanguageModel::getProvider)
            .distinct()
            .filter(provider -> switch (provider) {
                case OpenAI -> stateService.isOpenAIEnabled();
                case Anthropic -> stateService.isAnthropicEnabled();
                case Mistral -> stateService.isMistralEnabled();
                case Groq -> stateService.isGroqEnabled();
                case DeepInfra -> stateService.isDeepInfraEnabled();
                case Google -> stateService.isGoogleEnabled();
                case DeepSeek -> stateService.isDeepSeekEnabled();
                case OpenRouter -> stateService.isOpenRouterEnabled();
                case Grok -> stateService.isGrokEnabled();
                case Kimi -> stateService.isKimiEnabled();
                case GLM -> stateService.isGlmEnabled();
                case Nvidia -> stateService.isNvidiaEnabled();
                case AzureOpenAI -> stateService.isAzureOpenAIEnabled();
                case Bedrock -> stateService.isAwsEnabled();
                case Cloudflare -> stateService.isCloudflareEnabled();
                default -> false;
            })
            .toList();
    }

    private @NotNull List<ModelProvider> getOptionalProviders() {
        List<ModelProvider> optionalModelProviders = new ArrayList<>();

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowAzureOpenAIFields())) {
            optionalModelProviders.add(AzureOpenAI);
        }

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowAwsFields())) {
            optionalModelProviders.add(Bedrock);
        }


        return optionalModelProviders;
    }

    private @NotNull List<ModelProvider> getAcpRunnersProvider() {
        boolean hasEnabledAcpTool = DevoxxGenieStateService.getInstance().getAcpTools().stream()
                .anyMatch(AcpToolConfig::isEnabled);
        if (hasEnabledAcpTool) {
            return List.of(ModelProvider.ACPRunners);
        }
        return List.of();
    }

    private @NotNull List<ModelProvider> getCliRunnersProvider() {
        boolean hasEnabledCliTool = DevoxxGenieStateService.getInstance().getCliTools().stream()
                .anyMatch(CliToolConfig::isEnabled);
        if (hasEnabledCliTool) {
            return List.of(ModelProvider.CLIRunners);
        }
        return List.of();
    }

    /**
     * Does the provider need a credential (API key, or AWS credentials for Bedrock) to work?
     *
     * @param provider the model provider
     * @return true when a credential must be configured before prompts can be executed
     */
    public static boolean requiresApiKey(ModelProvider provider) {
        return providerKeyMap.containsKey(provider);
    }

    /**
     * Get the API key for the specified model provider.
     *
     * @param provider The model provider for which to retrieve the API key.
     * @return The API key as a string, or an empty string if not configured.
     */
    public String getApiKey(ModelProvider provider) {
        return Optional.ofNullable(providerKeyMap.get(provider))
                .map(Supplier::get)
                .filter(key -> !key.isBlank())
                .orElse("");
    }
}
