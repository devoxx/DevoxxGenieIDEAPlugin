package com.devoxx.genie.ui.settings;

import com.devoxx.genie.chatmodel.cloud.router.RouterRegistry;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Single place that maps a {@link ModelProvider} to its "enabled" setting, shared by the chat provider
 * combo and the agent settings combos.
 * <p>
 * Generic routers (see {@link RouterRegistry}) are resolved through the per-router flag. Every other
 * provider needs an explicit case: an unmapped one is logged and treated as disabled, and
 * {@code ProviderEnabledResolverTest} fails for it so it cannot silently vanish from the combos.
 */
public final class ProviderEnabledResolver {

    private static final Logger LOG = Logger.getInstance(ProviderEnabledResolver.class);

    private ProviderEnabledResolver() {
    }

    /**
     * @param runnersEnabled whether the CLI and ACP runner providers count as enabled for the caller
     */
    public static boolean isEnabled(@NotNull DevoxxGenieStateService state,
                                    @NotNull ModelProvider provider,
                                    boolean runnersEnabled) {
        return switch (provider) {
            case Ollama -> state.isOllamaEnabled();
            case LMStudio -> state.isLmStudioEnabled();
            case GPT4All -> state.isGpt4AllEnabled();
            case Jan -> state.isJanEnabled();
            case Nativ -> state.isNativEnabled();
            case RapidMLX -> state.isRapidMLXEnabled();
            case LLaMA -> state.isLlamaCPPEnabled();
            case Exo -> state.isExoEnabled();
            case CustomOpenAI -> state.isCustomOpenAIUrlEnabled();
            case OpenAI -> state.isOpenAIEnabled();
            case Mistral -> state.isMistralEnabled();
            case Anthropic -> state.isAnthropicEnabled();
            case Groq -> state.isGroqEnabled();
            case DeepInfra -> state.isDeepInfraEnabled();
            case Google -> state.isGoogleEnabled();
            case DeepSeek -> state.isDeepSeekEnabled();
            case Grok -> state.isGrokEnabled();
            case Kimi -> state.isKimiEnabled();
            case GLM -> state.isGlmEnabled();
            case Nvidia -> state.isNvidiaEnabled();
            case Cloudflare -> state.isCloudflareEnabled();
            case AzureOpenAI -> state.isAzureOpenAIEnabled();
            case Bedrock -> state.isAwsEnabled();
            case CLIRunners, ACPRunners -> runnersEnabled;
            default -> RouterRegistry.isGeneric(provider) ? state.isRouterEnabled(provider) : unmapped(provider);
        };
    }

    private static boolean unmapped(ModelProvider provider) {
        LOG.warn("No enabled-setting mapping for provider " + provider + "; add a case to ProviderEnabledResolver");
        return false;
    }
}
