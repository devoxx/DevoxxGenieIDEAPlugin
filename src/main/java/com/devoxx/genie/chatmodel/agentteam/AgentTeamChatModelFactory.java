package com.devoxx.genie.chatmodel.agentteam;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.agent.team.AgentRegistry;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Factory for the {@link ModelProvider#AgentTeam} pseudo-provider (TASK-249): its
 * "models" are the team's agents — orchestrator first, then every enabled specialist.
 * Selecting an agent in the model dropdown runs the prompt as that agent.
 * <p>
 * Model creation delegates to the agent's bound provider factory (its Agent Team
 * binding), falling back to Ollama → OpenAI → any enabled provider when the binding is
 * empty — so a fresh setup works without configuration. Streaming is preserved by
 * delegating {@code createStreamingChatModel} the same way.
 */
@Slf4j
public class AgentTeamChatModelFactory implements ChatModelFactory {

    @Override
    public List<LanguageModel> getModels() {
        List<LanguageModel> models = new ArrayList<>();
        for (AgentDefinition def : AgentRegistry.getInstance().getEnabled()) {
            boolean isOrchestrator = AgentRegistry.ORCHESTRATOR_NAME.equals(def.getName());
            LanguageModel model = LanguageModel.builder()
                    .provider(ModelProvider.AgentTeam)
                    .modelName(def.getName())
                    .displayName(def.getName() + (isOrchestrator ? " (coordinates team)" : ""))
                    .apiKeyUsed(false)
                    .inputCost(0)
                    .outputCost(0)
                    .inputMaxTokens(0)
                    .outputMaxTokens(0)
                    .build();
            // Orchestrator first — it is the default team experience
            if (isOrchestrator) {
                models.add(0, model);
            } else {
                models.add(model);
            }
        }
        return models;
    }

    @Override
    public ChatModel createChatModel(@NotNull CustomChatModel customChatModel) {
        ResolvedBinding binding = resolveBinding(customChatModel.getModelName());
        return binding.factory().createChatModel(toUnderlyingConfig(customChatModel, binding));
    }

    @Override
    public StreamingChatModel createStreamingChatModel(@NotNull CustomChatModel customChatModel) {
        ResolvedBinding binding = resolveBinding(customChatModel.getModelName());
        return binding.factory().createStreamingChatModel(toUnderlyingConfig(customChatModel, binding));
    }

    /** The real provider/model an agent name resolves to, plus its factory. */
    public record ResolvedBinding(@NotNull String providerName, @Nullable String modelName,
                                  @NotNull ChatModelFactory factory) {
    }

    /**
     * Resolves an agent name (the pseudo "model") to a concrete provider binding:
     * the agent's own binding first, then Ollama → OpenAI → any enabled provider.
     *
     * @throws IllegalArgumentException with an actionable message when the agent is
     *         unknown or no provider can be resolved
     */
    public static @NotNull ResolvedBinding resolveBinding(@Nullable String agentName) {
        AgentDefinition def = AgentRegistry.getInstance().byName(agentName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Agent Team agent '" + agentName + "'. Configure the team in "
                                + "Settings > Agent > Agent Team."));

        if (def.getModelProvider() != null && !def.getModelProvider().isBlank()
                && !ModelProvider.AgentTeam.getName().equals(def.getModelProvider())) {
            Optional<ChatModelFactory> factory =
                    ChatModelFactoryProvider.getFactoryByProvider(def.getModelProvider());
            if (factory.isPresent()) {
                return new ResolvedBinding(def.getModelProvider(),
                        emptyToNull(def.getModelName()), factory.get());
            }
            log.warn("Agent '{}' is bound to unknown provider '{}' — falling back to defaults",
                    def.getName(), def.getModelProvider());
        }

        // No usable binding: try the same default chain SubAgentRunner established —
        // Ollama (free, local) first, then OpenAI, then anything with a factory.
        for (String candidate : List.of(ModelProvider.Ollama.getName(), ModelProvider.OpenAI.getName())) {
            Optional<ChatModelFactory> factory = ChatModelFactoryProvider.getFactoryByProvider(candidate);
            if (factory.isPresent() && !factory.get().getModels().isEmpty()) {
                return new ResolvedBinding(candidate, null, factory.get());
            }
        }
        throw new IllegalArgumentException(
                "Agent '" + def.getName() + "' has no model binding and no default provider is "
                        + "available. Bind a provider/model to it in Settings > Agent > Agent Team.");
    }

    /**
     * Rewrites the incoming config (whose modelName is the AGENT name) for the underlying
     * provider: real model name, that provider's base URL, and the agent's temperature
     * override when set.
     */
    private static @NotNull CustomChatModel toUnderlyingConfig(@NotNull CustomChatModel original,
                                                               @NotNull ResolvedBinding binding) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        AgentDefinition def = AgentRegistry.getInstance().byName(original.getModelName()).orElse(null);

        CustomChatModel config = new CustomChatModel();
        config.setTemperature(def != null && def.getTemperature() != null
                ? def.getTemperature() : original.getTemperature());
        config.setTopP(original.getTopP());
        config.setMaxRetries(original.getMaxRetries());
        config.setTimeout(original.getTimeout());
        config.setMaxTokens(original.getMaxTokens());

        String modelName = binding.modelName();
        if (modelName == null) {
            List<LanguageModel> models = binding.factory().getModels();
            if (!models.isEmpty()) {
                modelName = models.get(0).getModelName();
            }
        }
        config.setModelName(modelName);

        switch (binding.providerName()) {
            case "Ollama" -> config.setBaseUrl(state.getOllamaModelUrl());
            case "LMStudio" -> config.setBaseUrl(state.getLmstudioModelUrl());
            case "GPT4All" -> config.setBaseUrl(state.getGpt4allModelUrl());
            case "Jan" -> config.setBaseUrl(state.getJanModelUrl());
            case "LLaMA", "LLaMA.c++" -> config.setBaseUrl(state.getLlamaCPPUrl());
            case "Exo" -> config.setBaseUrl(state.getExoModelUrl());
            case "CustomOpenAI" -> config.setBaseUrl(state.getCustomOpenAIUrl());
            default -> { /* Cloud providers don't need a base URL */ }
        }
        return config;
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
