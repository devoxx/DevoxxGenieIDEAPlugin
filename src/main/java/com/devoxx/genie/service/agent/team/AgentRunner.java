package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.agentteam.AgentTeamChatModelFactory;
import com.devoxx.genie.model.CustomChatModel;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.agent.AgentApprovalProvider;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.model.Constant.SUB_AGENT_MAX_TOOL_CALLS;
import static com.devoxx.genie.model.Constant.SUB_AGENT_MEMORY_SIZE;

/**
 * Runs one delegated Agent Team task: a named {@link AgentDefinition} with its own
 * ChatModel (any provider — this is what enables hybrid local/cloud teams), its own
 * isolated memory, a persona-driven system prompt and a preset-scoped tool provider
 * wrapped in approval + loop tracking.
 * <p>
 * Generalization of {@code SubAgentRunner} (the anonymous read-only explorer used by
 * {@code parallel_explore}): persona, toolset, model binding and budgets all come from
 * the definition. Non-streaming — only the final summary matters to the delegating
 * parent, and slow local models must never degrade the main chat.
 * <p>
 * Mirrors the DockerAgents runner's handoff guarantee: {@link #execute} returns a
 * readable {@link AgentResult} on EVERY exit path.
 */
@Slf4j
public class AgentRunner implements AgentLoopTracker.Cancellable {

    private final Project project;
    private final AgentDefinition definition;
    private final @Nullable String intent;
    private final AtomicBoolean cancelled;
    private @Nullable AgentLoopTracker tracker;
    private String resolvedProviderName;
    private String resolvedModelName;

    public AgentRunner(@NotNull Project project,
                       @NotNull AgentDefinition definition,
                       @Nullable String intent,
                       @NotNull AtomicBoolean cancelled) {
        this.project = project;
        this.definition = definition;
        this.intent = intent;
        this.cancelled = cancelled;
    }

    /**
     * Executes the delegated task. Never throws — every failure mode is converted into
     * an {@link AgentResult} whose summary a parent (and its LLM) can act on.
     */
    public @NotNull AgentResult execute(@NotNull String task) {
        long start = System.currentTimeMillis();
        String agentName = definition.getName();

        if (cancelled.get()) {
            return AgentResult.cancelled(agentName, intent);
        }

        try {
            ChatModel model = resolveModel();
            if (model == null) {
                return AgentResult.error(agentName, intent,
                        "Could not create a chat model for agent '" + agentName + "' (provider: "
                                + describeProviderBinding() + "). Check the agent's model binding in "
                                + "Settings > Agent > Agent Team and that the provider is enabled.",
                        0, elapsed(start), resolvedProviderName, resolvedModelName);
            }

            TeamAgentToolProvider teamTools = new TeamAgentToolProvider(project, definition);
            DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
            boolean autoApproveReadOnly = Boolean.TRUE.equals(settings.getAgentAutoApproveReadOnly());
            ToolProvider approvedTools = new AgentApprovalProvider(teamTools, project, autoApproveReadOnly);

            int maxToolCalls = definition.getMaxToolCalls() != null && definition.getMaxToolCalls() > 0
                    ? definition.getMaxToolCalls()
                    : SUB_AGENT_MAX_TOOL_CALLS;
            tracker = new AgentLoopTracker(approvedTools, maxToolCalls, project, buildLabel());

            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                    .id("team-agent-" + agentName + "-" + UUID.randomUUID())
                    .maxMessages(SUB_AGENT_MEMORY_SIZE)
                    .build();

            if (cancelled.get()) {
                return AgentResult.cancelled(agentName, intent);
            }

            TeamAssistant assistant = AiServices.builder(TeamAssistant.class)
                    .chatModel(model)
                    .toolProvider(tracker)
                    // Issue #1188: keep the per-agent budget authoritative over Langchain4j's
                    // internal 100-round-trip default.
                    .maxToolCallingRoundTrips(tracker.getMaxToolCallingRoundTrips())
                    .chatMemoryProvider(memoryId -> memory)
                    .systemMessageProvider(memoryId -> buildSystemPrompt())
                    .build();

            log.info("Team agent '{}' starting (intent: {}): {}", agentName, intent,
                    task.substring(0, Math.min(80, task.length())));

            String summary = assistant.chat(task);

            if (cancelled.get()) {
                return AgentResult.cancelled(agentName, intent);
            }

            log.info("Team agent '{}' completed with {} tool calls", agentName, tracker.getCallCount());
            return AgentResult.ok(agentName, intent,
                    summary != null ? summary : "(no response)",
                    tracker.getCallCount(), elapsed(start),
                    resolvedProviderName, resolvedModelName);
        } catch (Exception e) {
            log.warn("Team agent '{}' failed", agentName, e);
            return AgentResult.error(agentName, intent,
                    buildErrorSummary(e), tracker != null ? tracker.getCallCount() : 0,
                    elapsed(start), resolvedProviderName, resolvedModelName);
        }
    }

    /** Cancels this agent's tool loop; the next tool call short-circuits. */
    @Override
    public void cancel() {
        cancelled.set(true);
        if (tracker != null) {
            tracker.cancel();
        }
    }

    /**
     * Persona-first system prompt. The agent's instruction leads (it defines the role);
     * project root and output-contract fragments follow — the same context the
     * DockerAgents runner injects around each persona.
     */
    private @NotNull String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder(definition.getInstruction() != null
                ? definition.getInstruction() : "");
        String basePath = project.getBasePath();
        if (basePath != null) {
            sb.append("\n<PROJECT_ROOT>").append(basePath).append("</PROJECT_ROOT>\n")
                    .append("All file paths in tool calls are relative to this project root directory.\n");
        }
        sb.append("""

                You are a one-shot delegated agent: you cannot ask follow-up questions.
                Complete the task with the context given, then answer with a terse,
                self-contained summary in markdown — it is the only thing your caller sees.
                """);
        return sb.toString();
    }

    /**
     * Resolves the ChatModel: the definition's provider/model binding first, falling back
     * to the conversation's active provider/model. Per-agent temperature overrides the
     * global setting — parameters are threaded through the config, never read globally
     * inside the loop.
     */
    private @Nullable ChatModel resolveModel() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        String providerName = definition.getModelProvider();
        String modelName = definition.getModelName();

        if (providerName == null || providerName.isBlank()) {
            providerName = settings.getSelectedProvider(project.getLocationHash());
            modelName = settings.getSelectedLanguageModel(project.getLocationHash());
        }
        if (providerName == null || providerName.isBlank()) {
            return null;
        }
        // TASK-249: when the conversation itself runs on the "Agent Team" pseudo-provider,
        // inheriting it would recurse. Resolve this agent's own binding (or the default
        // provider chain) instead.
        if (ModelProvider.AgentTeam.getName().equals(providerName)) {
            try {
                var binding = AgentTeamChatModelFactory.resolveBinding(definition.getName());
                providerName = binding.providerName();
                modelName = binding.modelName() != null ? binding.modelName() : "";
            } catch (IllegalArgumentException e) {
                log.warn("Team agent '{}' could not resolve a real provider: {}",
                        definition.getName(), e.getMessage());
                return null;
            }
        }

        resolvedProviderName = providerName;
        resolvedModelName = modelName;

        try {
            return ChatModelFactoryProvider.getFactoryByProvider(providerName)
                    .map(factory -> {
                        CustomChatModel config = buildModelConfig(factory, definition.getModelName() != null
                                && !definition.getModelName().isBlank()
                                ? definition.getModelName() : resolvedModelName);
                        if (resolvedModelName == null || resolvedModelName.isEmpty()) {
                            resolvedModelName = config.getModelName();
                        }
                        return factory.createChatModel(config);
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to create model for team agent '{}' (provider {}): {}",
                    definition.getName(), providerName, e.getMessage());
            return null;
        }
    }

    private @NotNull CustomChatModel buildModelConfig(@NotNull ChatModelFactory factory, @Nullable String modelName) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        CustomChatModel config = new CustomChatModel();
        config.setTemperature(definition.getTemperature() != null
                ? definition.getTemperature()
                : (settings.getTemperature() != null ? settings.getTemperature() : 0.0));
        config.setTopP(settings.getTopP() != null ? settings.getTopP() : 0.9);
        config.setMaxRetries(settings.getMaxRetries() != null ? settings.getMaxRetries() : 1);
        config.setTimeout(settings.getTimeout() != null ? settings.getTimeout() : 120);
        config.setMaxTokens(settings.getMaxOutputTokens() != null ? settings.getMaxOutputTokens() : 4000);

        if (modelName != null && !modelName.isBlank()) {
            config.setModelName(modelName);
        } else {
            var models = factory.getModels();
            if (!models.isEmpty()) {
                config.setModelName(models.get(0).getModelName());
            }
        }

        setBaseUrlIfLocal(config, settings);
        return config;
    }

    private void setBaseUrlIfLocal(@NotNull CustomChatModel config, @NotNull DevoxxGenieStateService settings) {
        if (resolvedProviderName == null) return;

        switch (resolvedProviderName) {
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
     * Converts an execution failure into an actionable summary — the local-model
     * resilience lesson from the DockerAgents runner: a model/server that rejects
     * tool-calling must produce advice, not a stack trace.
     */
    private @NotNull String buildErrorSummary(@NotNull Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        String base = "Agent '" + definition.getName() + "' failed: " + msg;
        String lower = msg.toLowerCase(Locale.ROOT);
        if (isLocalProvider() && (lower.contains("tool") || lower.contains("function")
                || lower.contains("400") || lower.contains("does not support"))) {
            base += ". The selected local model may not support tool calling — configure a "
                    + "tool-capable model for agent '" + definition.getName()
                    + "' in Settings > Agent > Agent Team.";
        }
        return base;
    }

    private boolean isLocalProvider() {
        if (resolvedProviderName == null) return false;
        return switch (resolvedProviderName) {
            case "Ollama", "LMStudio", "GPT4All", "Jan", "LLaMA", "CustomOpenAI", "Exo" -> true;
            default -> false;
        };
    }

    private @NotNull String describeProviderBinding() {
        if (definition.getModelProvider() == null || definition.getModelProvider().isBlank()) {
            return "inherited conversation model";
        }
        return AgentRegistry.modelLabel(definition);
    }

    /** Label like {@code reviewer:Ollama:qwen3} for activity events and log lines. */
    private @NotNull String buildLabel() {
        StringBuilder label = new StringBuilder(definition.getName());
        if (resolvedProviderName != null && !resolvedProviderName.isEmpty()) {
            label.append(":").append(resolvedProviderName);
            if (resolvedModelName != null && !resolvedModelName.isEmpty()) {
                label.append(":").append(resolvedModelName);
            }
        }
        return label.toString();
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    public @Nullable String getResolvedProviderName() {
        return resolvedProviderName;
    }

    public @Nullable String getResolvedModelName() {
        return resolvedModelName;
    }

    /** Non-streaming assistant contract — only the final summary matters. */
    interface TeamAssistant {
        String chat(String userMessage);
    }
}
