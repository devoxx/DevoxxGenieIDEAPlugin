package com.devoxx.genie.service.agent;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.service.agent.tool.ReadOnlyToolProvider;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.model.Constant.SUB_AGENT_MAX_TOOL_CALLS;
import static com.devoxx.genie.model.Constant.SUB_AGENT_MEMORY_SIZE;

/**
 * Runs a single sub-agent exploration task.
 * Each sub-agent has its own model instance, memory, and read-only tool access.
 * Uses non-streaming ChatModel since we only need the final result.
 */
@Slf4j
public class SubAgentRunner {

    private static final String SYSTEM_PROMPT =
            "You are a focused code exploration agent. Your job is to investigate a specific " +
            "aspect of a codebase and provide a concise, factual summary of what you find. " +
            "Use the provided tools to read files, list directories, and search for patterns. " +
            "Be thorough but efficient — stay focused on your assigned query. " +
            "Always respond in markdown format.";

    private final Project project;
    private final int agentIndex;
    private final AtomicBoolean cancelled;
    private @Nullable AgentLoopTracker tracker;

    public SubAgentRunner(@NotNull Project project, int agentIndex, @NotNull AtomicBoolean cancelled) {
        this.project = project;
        this.agentIndex = agentIndex;
        this.cancelled = cancelled;
    }

    /**
     * Executes the sub-agent with the given query.
     * Creates its own model, memory, and tools.
     *
     * @param query The exploration query to investigate
     * @return The sub-agent's findings as a string
     */
    public @NotNull String execute(@NotNull String query) {
        if (cancelled.get()) {
            return "Sub-agent #" + (agentIndex + 1) + " cancelled before starting.";
        }

        try {
            ChatModel model = resolveModel();
            if (model == null) {
                return "Sub-agent #" + (agentIndex + 1) + " error: Could not create model. " +
                       "Check that a sub-agent model is configured in Settings > Agent.";
            }

            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                    .id("sub-agent-" + UUID.randomUUID())
                    .maxMessages(SUB_AGENT_MEMORY_SIZE)
                    .build();

            DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
            int maxToolCalls = settings.getSubAgentMaxToolCalls() != null
                    ? settings.getSubAgentMaxToolCalls()
                    : SUB_AGENT_MAX_TOOL_CALLS;

            ToolProvider readOnlyTools = new ReadOnlyToolProvider(project);
            tracker = new AgentLoopTracker(readOnlyTools, maxToolCalls, project);

            // Share cancellation state
            if (cancelled.get()) {
                return "Sub-agent #" + (agentIndex + 1) + " cancelled.";
            }

            SubAssistant assistant = AiServices.builder(SubAssistant.class)
                    .chatModel(model)
                    .toolProvider(tracker)
                    .chatMemoryProvider(memoryId -> memory)
                    .systemMessageProvider(memoryId -> SYSTEM_PROMPT)
                    .build();

            String prompt = "Investigate the following in the project codebase:\n\n" + query +
                    "\n\nProvide a concise summary of what you found, including relevant file paths and code snippets.";

            log.info("Sub-agent #{} starting exploration: {}", agentIndex + 1,
                    query.substring(0, Math.min(80, query.length())));

            String result = assistant.chat(prompt);

            log.info("Sub-agent #{} completed with {} tool calls", agentIndex + 1,
                    tracker.getCallCount());

            return result;
        } catch (Exception e) {
            log.error("Sub-agent #{} failed", agentIndex + 1, e);
            return "Sub-agent #" + (agentIndex + 1) + " error: " + e.getMessage();
        }
    }

    /**
     * Cancel this sub-agent's tool execution loop.
     */
    public void cancel() {
        if (tracker != null) {
            tracker.cancel();
        }
    }

    /**
     * Resolves the ChatModel for this sub-agent.
     * Uses the configured sub-agent model, or falls back to the main agent's provider.
     */
    @Nullable
    private ChatModel resolveModel() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        String providerName = settings.getSubAgentModelProvider();
        String modelName = settings.getSubAgentModelName();

        // If no sub-agent model configured, try to use a sensible default
        if (providerName == null || providerName.isBlank()) {
            log.debug("No sub-agent model configured, attempting to use Ollama or OpenAI");
            return tryDefaultModel();
        }

        return createModelForProvider(providerName, modelName);
    }

    @Nullable
    private ChatModel tryDefaultModel() {
        // Try Ollama first (free, local), then OpenAI
        ChatModel model = createModelForProvider("Ollama", null);
        if (model != null) return model;
        return createModelForProvider("OpenAI", null);
    }

    @Nullable
    private ChatModel createModelForProvider(@NotNull String providerName, @Nullable String modelName) {
        try {
            return ChatModelFactoryProvider.getFactoryByProvider(providerName)
                    .map(factory -> {
                        CustomChatModel config = buildModelConfig(factory, modelName);
                        return factory.createChatModel(config);
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to create sub-agent model for provider {}: {}", providerName, e.getMessage());
            return null;
        }
    }

    @NotNull
    private CustomChatModel buildModelConfig(@NotNull ChatModelFactory factory, @Nullable String modelName) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        CustomChatModel config = new CustomChatModel();
        config.setTemperature(settings.getTemperature() != null ? settings.getTemperature() : 0.0);
        config.setTopP(settings.getTopP() != null ? settings.getTopP() : 0.9);
        config.setMaxRetries(settings.getMaxRetries() != null ? settings.getMaxRetries() : 1);
        config.setTimeout(settings.getTimeout() != null ? settings.getTimeout() : 120);
        config.setMaxTokens(settings.getMaxOutputTokens() != null ? settings.getMaxOutputTokens() : 4000);

        if (modelName != null && !modelName.isBlank()) {
            config.setModelName(modelName);
        } else {
            // Use the first available model from the factory
            var models = factory.getModels();
            if (!models.isEmpty()) {
                config.setModelName(models.get(0).getModelName());
            }
        }

        // Set base URL for local providers
        setBaseUrlIfLocal(config, settings);

        return config;
    }

    private void setBaseUrlIfLocal(@NotNull CustomChatModel config, @NotNull DevoxxGenieStateService settings) {
        String providerName = settings.getSubAgentModelProvider();
        if (providerName == null) return;

        switch (providerName) {
            case "Ollama" -> config.setBaseUrl(settings.getOllamaModelUrl());
            case "LMStudio" -> config.setBaseUrl(settings.getLmstudioModelUrl());
            case "GPT4All" -> config.setBaseUrl(settings.getGpt4allModelUrl());
            case "Jan" -> config.setBaseUrl(settings.getJanModelUrl());
            case "LLaMA" -> config.setBaseUrl(settings.getLlamaCPPUrl());
            case "CustomOpenAI" -> config.setBaseUrl(settings.getCustomOpenAIUrl());
            default -> { /* Cloud providers don't need base URL */ }
        }
    }

    /**
     * Interface for AiServices to create a non-streaming sub-agent assistant.
     */
    interface SubAssistant {
        String chat(String userMessage);
    }
}
