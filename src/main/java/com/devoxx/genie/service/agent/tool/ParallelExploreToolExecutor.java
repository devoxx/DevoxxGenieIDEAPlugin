package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.SubAgentRunner;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.model.Constant.SUB_AGENT_DEFAULT_PARALLELISM;
import static com.devoxx.genie.model.Constant.SUB_AGENT_TIMEOUT_SECONDS;

/**
 * Tool executor that launches multiple sub-agents in parallel to explore
 * different aspects of the codebase simultaneously.
 * <p>
 * Each sub-agent gets:
 * - Its own ChatModel instance (configurable, can be different from main agent)
 * - Its own isolated ChatMemory
 * - Read-only tool access (read_file, list_files, search_files)
 * - Independent AgentLoopTracker with lower tool call limit
 * <p>
 * Results from all sub-agents are collected and returned as a combined report.
 */
@Slf4j
public class ParallelExploreToolExecutor implements ToolExecutor, AgentLoopTracker.Cancellable {

    private final Project project;
    private final List<SubAgentRunner> activeRunners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ParallelExploreToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        List<String> queries = ToolArgumentParser.getStringArray(request.arguments(), "queries");

        if (queries.isEmpty()) {
            return "Error: 'queries' parameter is required and must be a non-empty array of strings.";
        }

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        int maxParallelism = settings.getSubAgentParallelism() != null
                ? settings.getSubAgentParallelism()
                : SUB_AGENT_DEFAULT_PARALLELISM;
        int timeoutSeconds = settings.getSubAgentTimeoutSeconds() != null
                ? settings.getSubAgentTimeoutSeconds()
                : SUB_AGENT_TIMEOUT_SECONDS;

        // Cap the number of queries to prevent abuse
        int effectiveCount = Math.min(queries.size(), maxParallelism);
        List<String> effectiveQueries = queries.subList(0, effectiveCount);

        log.info("Parallel explore: launching {} sub-agents", effectiveCount);
        publishEvent(AgentType.SUB_AGENT_STARTED, "parallel_explore",
                "Launching " + effectiveCount + " sub-agents", null, 0);

        ExecutorService executor = ThreadPoolManager.getInstance().getSubAgentPool();

        // Create sub-agent runners and submit futures
        List<Future<String>> futures = new ArrayList<>(effectiveCount);
        for (int i = 0; i < effectiveCount; i++) {
            final int index = i;
            final String query = effectiveQueries.get(i);

            SubAgentRunner runner = new SubAgentRunner(project, index, cancelled);
            activeRunners.add(runner);

            publishEvent(AgentType.SUB_AGENT_STARTED, "sub-agent-" + (index + 1),
                    query, null, index + 1);

            futures.add(executor.submit(() -> runner.execute(query)));
        }

        // Collect results with timeout
        List<String> results = new ArrayList<>(effectiveCount);
        for (int i = 0; i < futures.size(); i++) {
            String query = effectiveQueries.get(i);
            try {
                String result = futures.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
                results.add(result);
                publishEvent(AgentType.SUB_AGENT_COMPLETED, "sub-agent-" + (i + 1),
                        query, truncateResult(result), i + 1);
            } catch (TimeoutException e) {
                log.warn("Sub-agent #{} timed out after {}s", i + 1, timeoutSeconds);
                activeRunners.get(i).cancel();
                futures.get(i).cancel(true);
                String timeoutMsg = "Sub-agent #" + (i + 1) + " timed out after " + timeoutSeconds + "s.";
                results.add(timeoutMsg);
                publishEvent(AgentType.SUB_AGENT_ERROR, "sub-agent-" + (i + 1),
                        query, timeoutMsg, i + 1);
            } catch (CancellationException e) {
                results.add("Sub-agent #" + (i + 1) + " was cancelled.");
            } catch (ExecutionException e) {
                log.error("Sub-agent #{} failed", i + 1, e.getCause());
                String errorMsg = "Sub-agent #" + (i + 1) + " error: " + e.getCause().getMessage();
                results.add(errorMsg);
                publishEvent(AgentType.SUB_AGENT_ERROR, "sub-agent-" + (i + 1),
                        query, errorMsg, i + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add("Sub-agent #" + (i + 1) + " interrupted.");
            }
        }

        activeRunners.clear();

        return formatCombinedResults(effectiveQueries, results);
    }

    /**
     * Cancels all running sub-agents.
     * Also implements {@link AgentLoopTracker.Cancellable} so the parent tracker
     * can propagate cancellation to this executor.
     */
    @Override
    public void cancel() {
        cancelled.set(true);
        for (SubAgentRunner runner : activeRunners) {
            runner.cancel();
        }
    }

    @NotNull
    private String formatCombinedResults(@NotNull List<String> queries, @NotNull List<String> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Parallel Exploration Results\n\n");

        for (int i = 0; i < results.size(); i++) {
            sb.append("## Sub-Agent #").append(i + 1);
            if (i < queries.size()) {
                sb.append(": ").append(queries.get(i));
            }
            sb.append("\n\n");
            sb.append(results.get(i));
            sb.append("\n\n---\n\n");
        }

        return sb.toString();
    }

    private String truncateResult(String result) {
        if (result == null) return null;
        return result.length() > 200 ? result.substring(0, 200) + "..." : result;
    }

    private void publishEvent(AgentType type, String toolName, String arguments, String result, int callNumber) {
        if (project.isDisposed()) return;
        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) return;

        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(type)
                    .toolName(toolName)
                    .arguments(arguments)
                    .result(result)
                    .callNumber(callNumber)
                    .maxCalls(0)
                    .projectLocationHash(project.getLocationHash())
                    .build();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                    .onActivityMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish sub-agent event", e);
        }
    }
}
