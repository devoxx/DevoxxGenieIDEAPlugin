package com.devoxx.genie.service.agent.codemode;

import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

@Slf4j
public class CodeModeToolProvider implements ToolProvider, AgentLoopTracker.Cancellable {

    public static final String RUN_SCRIPT_TOOL_NAME = "run_script";

    static final Duration DEFAULT_SCRIPT_TIME_BUDGET = Duration.ofSeconds(30);

    private static final String CODE_ARGUMENT = "code";

    private final ToolProvider delegate;

    private final Duration scriptTimeBudget;

    private final Set<CodeModeScriptRun> activeRuns = ConcurrentHashMap.newKeySet();

    private volatile UnaryOperator<ToolExecutor> nestedCallTracking = UnaryOperator.identity();

    public CodeModeToolProvider(@NotNull ToolProvider delegate) {
        this(delegate, DEFAULT_SCRIPT_TIME_BUDGET);
    }

    CodeModeToolProvider(@NotNull ToolProvider delegate, @NotNull Duration scriptTimeBudget) {
        this.delegate = Objects.requireNonNull(delegate, "The delegate tool provider can not be null");
        this.scriptTimeBudget = Objects.requireNonNull(scriptTimeBudget, "The script time budget can not be null");
    }

    public void trackNestedCallsWith(@NotNull AgentLoopTracker tracker) {
        Objects.requireNonNull(tracker, "The tracker can not be null");
        this.nestedCallTracking = tracker::tracked;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        var delegated = this.delegate.provideTools(request);
        if (delegated.aiServiceTools().stream().anyMatch(tool -> RUN_SCRIPT_TOOL_NAME.equals(tool.name()))) {
            log.warn("Code mode skipped: another provider already exposes a '{}' tool", RUN_SCRIPT_TOOL_NAME);
            return delegated;
        }
        var callableTools = callableToolsOf(delegated);
        return ToolProviderResult.builder()
                .addAll(delegated.aiServiceTools())
                .add(runScriptSpecification(callableTools), new RunScriptExecutor(callableTools))
                .build();
    }

    @Override
    public void cancel() {
        this.activeRuns.forEach(CodeModeScriptRun::cancel);
    }

    private Map<String, CallableTool> callableToolsOf(ToolProviderResult delegated) {
        var tracking = this.nestedCallTracking;
        var callableTools = new LinkedHashMap<String, CallableTool>();
        for (var tool : delegated.aiServiceTools()) {
            callableTools.put(tool.name(),
                    new CallableTool(tool.toolSpecification(), tracking.apply(tool.toolExecutor())));
        }
        return callableTools;
    }

    private ToolSpecification runScriptSpecification(Map<String, CallableTool> callableTools) {
        return ToolSpecification.builder()
                .name(RUN_SCRIPT_TOOL_NAME)
                .description(CodeModeToolDescription.render(specificationsOf(callableTools),
                        this.scriptTimeBudget.toSeconds()))
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty(CODE_ARGUMENT,
                                "JavaScript source: the body of an async function. "
                                        + "Use await for tool calls and text(value) for anything you need to see.")
                        .required(CODE_ARGUMENT)
                        .build())
                .build();
    }

    private static List<ToolSpecification> specificationsOf(Map<String, CallableTool> callableTools) {
        return callableTools.values().stream().map(CallableTool::specification).toList();
    }

    private String runScript(ToolExecutionRequest request,
                             Map<String, CallableTool> callableTools,
                             NestedInvocation invocation) {
        var code = ToolArgumentParser.getString(request.arguments(), CODE_ARGUMENT);
        if (code == null || code.isBlank()) {
            return "Error: '" + CODE_ARGUMENT + "' is required: pass the JavaScript to run.";
        }
        var run = new CodeModeScriptRun(
                code,
                CodeModeToolDescription.catalogJsonOf(specificationsOf(callableTools)),
                (toolName, argumentsJson) -> callNestedTool(toolName, argumentsJson, callableTools, invocation),
                this.scriptTimeBudget);
        this.activeRuns.add(run);
        try {
            return run.run();
        } finally {
            this.activeRuns.remove(run);
        }
    }

    private static String callNestedTool(String toolName,
                                         String argumentsJson,
                                         Map<String, CallableTool> callableTools,
                                         NestedInvocation invocation) {
        var tool = callableTools.get(toolName);
        if (tool == null) {
            return "Error: unknown tool '" + toolName + "'. Check ALL_TOOLS for the callable tool names.";
        }
        var nestedRequest = ToolExecutionRequest.builder()
                .id(UUID.randomUUID().toString())
                .name(toolName)
                .arguments(argumentsJson)
                .build();
        return invocation.invoke(tool.executor(), nestedRequest);
    }

    private static String textOf(@Nullable ToolExecutionResult result) {
        if (result == null) {
            return "";
        }
        try {
            return result.resultText();
        } catch (RuntimeException e) {
            return String.valueOf(result.result());
        }
    }

    @FunctionalInterface
    private interface NestedInvocation {

        String invoke(ToolExecutor executor, ToolExecutionRequest request);
    }

    private record CallableTool(ToolSpecification specification, ToolExecutor executor) {
    }

    private final class RunScriptExecutor implements ToolExecutor {

        private final Map<String, CallableTool> callableTools;

        private RunScriptExecutor(Map<String, CallableTool> callableTools) {
            this.callableTools = callableTools;
        }

        @Override
        public String execute(ToolExecutionRequest request, Object memoryId) {
            return runScript(request, this.callableTools,
                    (executor, nestedRequest) -> executor.execute(nestedRequest, memoryId));
        }

        @Override
        public ToolExecutionResult executeWithContext(ToolExecutionRequest request, InvocationContext context) {
            var output = runScript(request, this.callableTools,
                    (executor, nestedRequest) -> textOf(executor.executeWithContext(nestedRequest, context)));
            return ToolExecutionResult.builder().resultText(output).build();
        }
    }
}
