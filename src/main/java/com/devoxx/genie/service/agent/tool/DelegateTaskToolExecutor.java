package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentResult;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.team.AgentRegistry;
import com.devoxx.genie.service.agent.team.AgentRunner;
import com.devoxx.genie.service.agent.team.RemoteAgentBackend;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.model.Constant.SUB_AGENT_DEFAULT_PARALLELISM;
import static com.devoxx.genie.model.Constant.SUB_AGENT_TIMEOUT_SECONDS;

/**
 * The {@code delegate_task} tool: delegates one or more self-contained tasks to named
 * Agent Team specialists, each running as an {@link AgentRunner} with its own
 * provider/model/tools/budget on the sub-agent thread pool.
 * <p>
 * Ports the DockerAgents handoff guarantees in-process:
 * <ul>
 *   <li>Unknown agent names fail fast with the available-agents list — nothing spawns.</li>
 *   <li>Multiple tasks fan out in parallel; the slowest child bounds the call.</li>
 *   <li>Per-task failures (error/timeout/cancel) become structured entries — one dead
 *       child never fails the batch (wait_all semantics).</li>
 *   <li>The merged result contains per task only status metadata and the child's
 *       summary — never transcripts — keeping the orchestrator's context lean.</li>
 * </ul>
 * Registered only for the orchestrating conversation's tool chain; a child's
 * {@code TeamAgentToolProvider} structurally excludes it, so delegation depth is 1.
 */
@Slf4j
public class DelegateTaskToolExecutor implements ToolExecutor, AgentLoopTracker.Cancellable {

    private final Project project;
    private final List<AgentRunner> activeRunners = new CopyOnWriteArrayList<>();
    /** Remote session ids in flight; DELETEd on cancellation (TASK-248). */
    private final List<String> activeRemoteSessions = new CopyOnWriteArrayList<>();
    private volatile @Nullable RemoteAgentBackend activeRemoteBackend;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public DelegateTaskToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    /** One parsed delegation request. */
    record DelegatedTask(String agent, String task, @Nullable String intent) {
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        List<DelegatedTask> tasks = parseTasks(request.arguments());
        if (tasks.isEmpty()) {
            return "Error: 'tasks' must be a non-empty array of {agent, task, intent?} objects.";
        }

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        if (Boolean.TRUE.equals(settings.getAgentTeamRemoteEnabled())
                && settings.getAgentTeamRemoteUrl() != null
                && !settings.getAgentTeamRemoteUrl().isBlank()) {
            return executeRemote(tasks, settings);
        }

        // Fail fast on ANY unknown agent before spawning anything (DockerAgents 404 analog).
        AgentRegistry registry = AgentRegistry.getInstance();
        List<AgentDefinition> resolved = new ArrayList<>(tasks.size());
        for (DelegatedTask task : tasks) {
            Optional<AgentDefinition> def = registry.byName(task.agent());
            if (def.isEmpty() || !def.get().isEnabled()) {
                return "Error: unknown or disabled agent '" + task.agent()
                        + "'. Available agents: " + registry.availableNames() + ".";
            }
            resolved.add(def.get());
        }

        int maxParallelism = settings.getSubAgentParallelism() != null
                ? settings.getSubAgentParallelism()
                : SUB_AGENT_DEFAULT_PARALLELISM;
        if (tasks.size() > Math.max(maxParallelism, 1)) {
            tasks = tasks.subList(0, Math.max(maxParallelism, 1));
            resolved = resolved.subList(0, tasks.size());
        }

        log.info("delegate_task: launching {} agent(s)", tasks.size());
        ExecutorService executor = ThreadPoolManager.getInstance().getSubAgentPool();

        List<Future<AgentResult>> futures = new ArrayList<>(tasks.size());
        List<AgentRunner> runners = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            DelegatedTask task = tasks.get(i);
            AgentDefinition definition = resolved.get(i);

            AgentRunner runner = new AgentRunner(project, definition, task.intent(), cancelled);
            runners.add(runner);
            activeRunners.add(runner);

            publishEvent(AgentType.SUB_AGENT_STARTED, definition.getName(),
                    modelLabelOf(definition), describeTask(task), null, i + 1);
            futures.add(executor.submit(() -> runner.execute(task.task())));
        }

        // wait_all semantics: collect every child; convert per-child failures into
        // structured results instead of failing the batch.
        List<AgentResult> results = new ArrayList<>(tasks.size());
        for (int i = 0; i < futures.size(); i++) {
            DelegatedTask task = tasks.get(i);
            AgentDefinition definition = resolved.get(i);
            int timeoutSeconds = definition.getTimeoutSeconds() != null && definition.getTimeoutSeconds() > 0
                    ? definition.getTimeoutSeconds()
                    : (settings.getSubAgentTimeoutSeconds() != null
                        ? settings.getSubAgentTimeoutSeconds()
                        : SUB_AGENT_TIMEOUT_SECONDS);
            AgentResult result;
            try {
                result = futures.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                runners.get(i).cancel();
                futures.get(i).cancel(true);
                result = AgentResult.timeout(definition.getName(), task.intent(), timeoutSeconds);
            } catch (CancellationException e) {
                result = AgentResult.cancelled(definition.getName(), task.intent());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                result = AgentResult.error(definition.getName(), task.intent(),
                        "Agent '" + definition.getName() + "' failed: " + cause.getMessage(),
                        0, 0, null, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = AgentResult.cancelled(definition.getName(), task.intent());
            }
            results.add(result);

            AgentType eventType = result.status() == AgentResult.Status.OK
                    ? AgentType.SUB_AGENT_COMPLETED : AgentType.SUB_AGENT_ERROR;
            // subAgentId must stay the bare agent name on every event of one delegation —
            // the chat view keys the running→done row transition on it. The provider·model
            // goes in the separate label field.
            publishEvent(eventType, result.agent(), providerModelLabel(result),
                    describeTask(task), truncate(result.summary()), i + 1);
        }

        activeRunners.clear();
        return formatResults(results);
    }

    /**
     * Cancels all running delegated agents. Registered as an
     * {@link AgentLoopTracker.Cancellable} child of the parent conversation's tracker so
     * the user's Stop propagates here.
     */
    @Override
    public void cancel() {
        cancelled.set(true);
        for (AgentRunner runner : activeRunners) {
            runner.cancel();
        }
        RemoteAgentBackend backend = activeRemoteBackend;
        if (backend != null) {
            for (String sessionId : activeRemoteSessions) {
                backend.delete(sessionId);
            }
            activeRemoteSessions.clear();
        }
    }

    /**
     * Remote execution path (TASK-248): the same delegation contract, but each task runs
     * as a one-shot container session on a DockerAgents orchestrator-api. Agent names
     * are validated against the remote /agents directory; per-task failures stay
     * structured entries; cancellation DELETEs the remote sessions.
     */
    private String executeRemote(@NotNull List<DelegatedTask> tasks,
                                 @NotNull DevoxxGenieStateService settings) {
        RemoteAgentBackend backend = new RemoteAgentBackend(settings.getAgentTeamRemoteUrl());
        activeRemoteBackend = backend;

        List<String> remoteAgents;
        try {
            remoteAgents = backend.listAgentNames();
        } catch (Exception e) {
            return "Error: DockerAgents orchestrator-api at " + settings.getAgentTeamRemoteUrl()
                    + " is unreachable (" + e.getMessage() + "). Fix the URL in "
                    + "Settings > Agent > Agent Team or disable remote execution.";
        }
        for (DelegatedTask task : tasks) {
            if (!remoteAgents.contains(task.agent())) {
                return "Error: unknown remote agent '" + task.agent()
                        + "'. Available remote agents: " + String.join(", ", remoteAgents) + ".";
            }
        }

        int timeoutSeconds = settings.getSubAgentTimeoutSeconds() != null
                ? settings.getSubAgentTimeoutSeconds()
                : SUB_AGENT_TIMEOUT_SECONDS;
        ExecutorService executor = ThreadPoolManager.getInstance().getSubAgentPool();

        List<Future<AgentResult>> futures = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            DelegatedTask task = tasks.get(i);
            publishEvent(AgentType.SUB_AGENT_STARTED, task.agent(), "remote",
                    describeTask(task), null, i + 1);
            futures.add(executor.submit(() -> {
                String sessionId = backend.spawn(task.agent(), task.task(), task.intent());
                activeRemoteSessions.add(sessionId);
                try {
                    return backend.waitFor(sessionId, task.agent(), task.intent(), timeoutSeconds);
                } finally {
                    activeRemoteSessions.remove(sessionId);
                }
            }));
        }

        List<AgentResult> results = new ArrayList<>(tasks.size());
        for (int i = 0; i < futures.size(); i++) {
            DelegatedTask task = tasks.get(i);
            AgentResult result;
            try {
                // The api blocks server-side for timeoutSeconds; the extra margin covers transport.
                result = futures.get(i).get(timeoutSeconds + 60L, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                futures.get(i).cancel(true);
                result = AgentResult.timeout(task.agent(), task.intent(), timeoutSeconds);
            } catch (CancellationException e) {
                result = AgentResult.cancelled(task.agent(), task.intent());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                result = AgentResult.error(task.agent(), task.intent(),
                        "Remote agent '" + task.agent() + "' failed: " + cause.getMessage(),
                        0, 0, "remote", null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = AgentResult.cancelled(task.agent(), task.intent());
            }
            results.add(result);

            AgentType eventType = result.status() == AgentResult.Status.OK
                    ? AgentType.SUB_AGENT_COMPLETED : AgentType.SUB_AGENT_ERROR;
            publishEvent(eventType, task.agent(), "remote",
                    describeTask(task), truncate(result.summary()), i + 1);
        }

        activeRemoteBackend = null;
        return formatResults(results);
    }

    static @NotNull List<DelegatedTask> parseTasks(@Nullable String arguments) {
        List<DelegatedTask> tasks = new ArrayList<>();
        if (arguments == null || arguments.isBlank()) {
            return tasks;
        }
        try {
            JsonObject json = JsonParser.parseString(arguments).getAsJsonObject();
            JsonElement tasksElement = json.get("tasks");
            if (tasksElement == null || !tasksElement.isJsonArray()) {
                return tasks;
            }
            JsonArray array = tasksElement.getAsJsonArray();
            for (JsonElement item : array) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject obj = item.getAsJsonObject();
                String agent = stringField(obj, "agent");
                String task = stringField(obj, "task");
                if (agent == null || agent.isBlank() || task == null || task.isBlank()) {
                    continue;
                }
                tasks.add(new DelegatedTask(agent.trim(), task, stringField(obj, "intent")));
            }
        } catch (Exception e) {
            log.debug("Failed to parse delegate_task arguments", e);
        }
        return tasks;
    }

    private static @Nullable String stringField(@NotNull JsonObject obj, @NotNull String key) {
        JsonElement element = obj.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : null;
    }

    /**
     * Renders the summary-only report the orchestrator receives. Per DockerAgents'
     * result contract, a child's summary is the ONLY content exposed — no transcripts.
     */
    static @NotNull String formatResults(@NotNull List<AgentResult> results) {
        StringBuilder sb = new StringBuilder("# Delegation Results\n\n");
        for (AgentResult result : results) {
            sb.append("## ").append(result.label());
            if (result.intent() != null && !result.intent().isBlank()) {
                sb.append(" — ").append(result.intent());
            }
            sb.append("\n");
            sb.append("Status: ").append(result.status().name().toLowerCase());
            if (result.toolCalls() > 0) {
                sb.append(" · ").append(result.toolCalls()).append(" tool calls");
            }
            if (result.durationMs() > 0) {
                sb.append(" · ").append(result.durationMs() / 1000).append("s");
            }
            sb.append("\n\n").append(result.summary()).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    private static @NotNull String describeTask(@NotNull DelegatedTask task) {
        String base = task.intent() != null && !task.intent().isBlank()
                ? task.intent() + ": " : "";
        return base + truncate(task.task());
    }

    private static @Nullable String truncate(@Nullable String text) {
        if (text == null) return null;
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    /** "Ollama · qwen3" from a definition's binding, or null when inheriting. */
    private static @Nullable String modelLabelOf(@NotNull AgentDefinition definition) {
        if (definition.getModelProvider() == null || definition.getModelProvider().isBlank()) {
            return null;
        }
        String label = definition.getModelProvider();
        if (definition.getModelName() != null && !definition.getModelName().isBlank()) {
            label += " · " + definition.getModelName();
        }
        return label;
    }

    /** "Ollama · qwen3" from a result's resolved provider/model, or null when unknown. */
    private static @Nullable String providerModelLabel(@NotNull AgentResult result) {
        if (result.provider() == null || result.provider().isBlank()) {
            return null;
        }
        String label = result.provider();
        if (result.model() != null && !result.model().isBlank()) {
            label += " · " + result.model();
        }
        return label;
    }

    private void publishEvent(AgentType type, String agentName, @Nullable String agentModelLabel,
                              String arguments, String result, int callNumber) {
        if (project.isDisposed()) return;
        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())
                && !Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getShowToolActivityInChat())) {
            return;
        }
        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(type)
                    .toolName("delegate_task")
                    .arguments(arguments)
                    .result(result)
                    .callNumber(callNumber)
                    .maxCalls(0)
                    .subAgentId(agentName)
                    .agentModelLabel(agentModelLabel)
                    .projectLocationHash(project.getLocationHash())
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish delegate_task event", e);
        }
    }
}
