package com.devoxx.genie.service.agent.codemode;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.tool.ToolArgumentParser;
import com.google.gson.JsonObject;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.service.agent.codemode.CodeModeToolProvider.RUN_SCRIPT_TOOL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class CodeModeToolProviderTest {

    private static final int SCRIPT_CAP = Constant.AGENT_MAX_SCRIPT_TOOL_CALLS;

    private static final Map<String, String> FILES = Map.of(
            "A.java", "class A {}\n// TODO rename A",
            "B.java", "class B {}",
            "C.java", "// TODO delete C\nclass C {}");

    private final List<String> readPaths = new CopyOnWriteArrayList<>();

    private final ToolProviderRequest providerRequest = mock(ToolProviderRequest.class);

    @Test
    void provideToolsAddsRunScriptNextToTheDelegatedTools() {
        var result = new CodeModeToolProvider(fileTools(), SCRIPT_CAP).provideTools(this.providerRequest);

        assertThat(result.aiServiceTools()).extracting(tool -> tool.name())
                .containsExactly("read_file", "list_files", RUN_SCRIPT_TOOL_NAME);
        assertThat(result.toolSpecificationByName(RUN_SCRIPT_TOOL_NAME).description())
                .contains("tools.read_file(args: { path: string }) - Read a file.")
                .contains("tools.list_files(args: {}) - List the project files.");
    }

    @Test
    void aScriptCallsSeveralToolsAndReturnsOnlyWhatItPrints() {
        var output = runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), """
                const files = await tools.list_files({});
                for (const path of files.split("\\n")) {
                  const content = await tools.read_file({ path });
                  content.split("\\n").filter(line => line.includes("TODO")).forEach(line => text(path + ": " + line));
                }
                """);

        assertThat(output).isEqualTo("A.java: // TODO rename A\nC.java: // TODO delete C");
        assertThat(this.readPaths).containsExactly("A.java", "B.java", "C.java");
    }

    @Test
    void aToolResultThatIsJsonComesBackAsAnObject() {
        var provider = new CodeModeToolProvider(singleTool("stats", (request, memoryId) -> "{\"todos\": 2}"), SCRIPT_CAP);

        assertThat(runScript(provider, "const stats = await tools.stats({}); return stats.todos + 1;")).isEqualTo("3");
    }

    @Test
    void theExecuteWithContextPathRunsNestedToolsThroughExecuteWithContext() {
        var runScript = new CodeModeToolProvider(fileTools(), SCRIPT_CAP).provideTools(this.providerRequest)
                .toolExecutorByName(RUN_SCRIPT_TOOL_NAME);
        var context = InvocationContext.builder().chatMemoryId("memory").build();

        var result = runScript.executeWithContext(runScriptRequest("text(await tools.read_file({ path: 'B.java' }));"), context);

        assertThat(result.resultText()).isEqualTo("class B {}");
    }

    @Test
    void aScriptCountsAsOneToolCallNoMatterHowManyToolsItCalls() {
        var tracker = trackedCodeMode(3);

        var output = runScriptThrough(tracker, """
                for (const path of ["A.java", "B.java", "A.java", "B.java", "A.java"]) {
                  await tools.read_file({ path });
                }
                text("read five files");
                """);

        assertThat(output).isEqualTo("read five files");
        assertThat(this.readPaths).hasSize(5);
        assertThat(tracker.getCallCount()).isEqualTo(1);
        assertThat(runScriptThrough(tracker, "return 2;")).isEqualTo("2");
        assertThat(runScriptThrough(tracker, "return 3;")).isEqualTo("3");
        assertThat(runScriptThrough(tracker, "return 4;")).startsWith("Error: Agent loop limit reached (3 tool calls)");
    }

    @Test
    void scriptsWithoutToolCallsStillReachTheLoopLimit() {
        var tracker = trackedCodeMode(2);
        runScriptThrough(tracker, "return 1;");
        runScriptThrough(tracker, "return 2;");

        assertThat(runScriptThrough(tracker, "return 3;")).startsWith("Error: Agent loop limit reached (2 tool calls)");
    }

    @Test
    void nestedCallsBeyondThePerScriptLimitReturnTheLimitErrorToTheScript() {
        var tracker = trackedCodeMode(10, 2);

        var output = runScriptThrough(tracker, """
                for (const path of ["B.java", "B.java", "B.java"]) {
                  text(await tools.read_file({ path }));
                }
                """);

        assertThat(output.lines().toList()).hasSize(3);
        assertThat(output.lines().skip(2).findFirst().orElseThrow()).isEqualTo(
                "Error: this script reached its limit of 2 tool calls. "
                        + "Return what you have, or continue in another run_script call.");
        assertThat(this.readPaths).hasSize(2);
    }

    @Test
    void thePerScriptLimitResetsForEachRun() {
        var tracker = trackedCodeMode(10, 2);
        var script = """
                text(await tools.read_file({ path: "A.java" }));
                text(await tools.read_file({ path: "B.java" }));
                """;

        assertThat(runScriptThrough(tracker, script)).doesNotContain("Error:");
        assertThat(runScriptThrough(tracker, script)).doesNotContain("Error:");
    }

    @Test
    void nestedCallsStillShortCircuitAfterCancel() {
        var secondRan = new AtomicBoolean();
        var holder = new AtomicReference<AgentLoopTracker>();
        var codeMode = new CodeModeToolProvider(request -> ToolProviderResult.builder()
                .add(ToolSpecification.builder().name("cancel_it").description("Cancels.").build(),
                        (ToolExecutor) (r, m) -> {
                            holder.get().cancel();
                            return "cancelled";
                        })
                .add(ToolSpecification.builder().name("second").description("Second.").build(),
                        (ToolExecutor) (r, m) -> {
                            secondRan.set(true);
                            return "ran";
                        })
                .build(), SCRIPT_CAP);
        var tracker = new AgentLoopTracker(codeMode, 10);
        holder.set(tracker);
        codeMode.trackNestedCallsWith(tracker);
        tracker.registerChild(codeMode);

        var output = runScriptThrough(tracker, """
                await tools.cancel_it({});
                text(await tools.second({}));
                """);

        assertThat(output).isEqualTo(AgentLoopTracker.CANCELLED_MESSAGE);
        assertThat(secondRan).isFalse();
    }

    @Test
    void runScriptIsRefusedOnceTheLoopLimitIsReached() {
        var tracker = trackedCodeMode(2);
        runScriptThrough(tracker, "await tools.read_file({ path: 'A.java' });");
        runScriptThrough(tracker, "await tools.read_file({ path: 'A.java' });");

        var output = runScriptThrough(tracker, "text(await tools.read_file({ path: 'B.java' }));");

        assertThat(output).startsWith("Error: Agent loop limit reached (2 tool calls)");
        assertThat(this.readPaths).containsExactly("A.java", "A.java");
    }

    @Test
    void runScriptIsRefusedAfterTheAgentIsCancelled() {
        var tracker = trackedCodeMode(10);
        tracker.cancel();

        assertThat(runScriptThrough(tracker, "text(await tools.read_file({ path: 'A.java' }));"))
                .isEqualTo(AgentLoopTracker.CANCELLED_MESSAGE);
        assertThat(this.readPaths).isEmpty();
    }

    @Test
    void anInfiniteLoopIsStoppedOnceTheScriptTimeBudgetIsUsed() {
        var provider = new CodeModeToolProvider(fileTools(), Duration.ofMillis(300), SCRIPT_CAP);

        assertThat(runScript(provider, "while (true) {}"))
                .startsWith("Error: the script was stopped after");
    }

    @Test
    void timeSpentInsideToolCallsDoesNotCountTowardTheScriptTimeBudget() {
        var provider = new CodeModeToolProvider(singleTool("slow", (request, memoryId) -> {
            sleep(600);
            return "finished";
        }), Duration.ofMillis(300), SCRIPT_CAP);

        assertThat(runScript(provider, "return await tools.slow({});")).isEqualTo("finished");
    }

    @Test
    void cancellingStopsARunningScript() throws Exception {
        var scriptStarted = new CountDownLatch(1);
        var provider = new CodeModeToolProvider(singleTool("started", (request, memoryId) -> {
            scriptStarted.countDown();
            return "ok";
        }), Duration.ofMinutes(1), SCRIPT_CAP);

        var output = CompletableFuture.supplyAsync(
                () -> runScript(provider, "await tools.started({}); while (true) {}"));
        assertThat(scriptStarted.await(10, TimeUnit.SECONDS)).isTrue();
        provider.cancel();

        assertThat(output.get(10, TimeUnit.SECONDS)).isEqualTo(AgentLoopTracker.CANCELLED_MESSAGE);
    }

    @Test
    void aRunCancelledBeforeItStartsNeverRunsTheScript() {
        var run = new CodeModeScriptRun("while (true) {}", "[]", (toolName, argumentsJson) -> "ok", Duration.ofMinutes(1), SCRIPT_CAP);
        run.cancel();

        assertThat(run.run()).isEqualTo(AgentLoopTracker.CANCELLED_MESSAGE);
    }

    @Test
    void aScriptThatUsesTooMuchMemoryFailsWithoutCrashing() {
        var output = runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP),
                "const chunks = []; while (true) chunks.push(new Array(1e5).fill(Math.random()));");

        assertThat(output).startsWith("Error:").containsIgnoringCase("out of memory");
    }

    @Test
    void aToolThatReturnsNullGivesTheScriptAnEmptyString() {
        var provider = new CodeModeToolProvider(singleTool("nothing", (request, memoryId) -> null), SCRIPT_CAP);

        assertThat(runScript(provider, "return JSON.stringify(await tools.nothing({}));")).isEqualTo("\"\"");
    }

    @Test
    void aScriptAwaitingAPromiseThatNeverSettlesIsReported() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "await new Promise(() => {});"))
                .isEqualTo("Error: the script never finished: it awaited a promise that never settles.");
    }

    @Test
    void aSyntaxErrorIsReportedAsAnError() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "const = ;"))
                .startsWith("Error: the script could not run");
    }

    @Test
    void aThrownErrorIsReportedWithItsMessageAndKeepsEarlierOutput() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "text('before'); throw new Error('boom');"))
                .startsWith("before\nError: the script threw Error: boom");
    }

    @Test
    void callingAnUnknownToolReturnsAnErrorStringToTheScript() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "return await tools.delete_everything({});"))
                .isEqualTo("Error: unknown tool 'delete_everything'. Check ALL_TOOLS for the callable tool names.");
    }

    @Test
    void aScriptWithoutOutputExplainsHowToReturnValues() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "await tools.list_files({});"))
                .startsWith("The script finished without output.");
    }

    @Test
    void outputLongerThanTheLimitIsTruncated() {
        var output = runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "return 'x'.repeat(50000);");

        assertThat(output).hasSizeLessThan(CodeModeScriptRun.MAX_OUTPUT_LENGTH + 100)
                .endsWith("(output truncated at 20000 characters; print less)");
    }

    @Test
    void aMissingCodeArgumentIsRejected() {
        var runScript = new CodeModeToolProvider(fileTools(), SCRIPT_CAP).provideTools(this.providerRequest)
                .toolExecutorByName(RUN_SCRIPT_TOOL_NAME);

        assertThat(runScript.execute(ToolExecutionRequest.builder().name(RUN_SCRIPT_TOOL_NAME).arguments("{}").build(), null))
                .isEqualTo("Error: 'code' is required: pass the JavaScript to run.");
    }

    @Test
    void theScriptHasNoNodeFileSystemOrNetworkGlobals() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP),
                "return [typeof require, typeof process, typeof fetch, typeof XMLHttpRequest, typeof setTimeout].join(',');"))
                .isEqualTo("undefined,undefined,undefined,undefined,undefined");
    }

    @Test
    void consoleLogIsCapturedLikeText() {
        assertThat(runScript(new CodeModeToolProvider(fileTools(), SCRIPT_CAP), "console.log('files:', 3, { ok: true });"))
                .isEqualTo("files: 3 {\n  \"ok\": true\n}");
    }

    private String runScript(CodeModeToolProvider provider, String code) {
        return provider.provideTools(this.providerRequest)
                .toolExecutorByName(RUN_SCRIPT_TOOL_NAME)
                .execute(runScriptRequest(code), null);
    }

    private AgentLoopTracker trackedCodeMode(int maxToolCalls) {
        return trackedCodeMode(maxToolCalls, SCRIPT_CAP);
    }

    private AgentLoopTracker trackedCodeMode(int maxToolCalls, int maxScriptToolCalls) {
        var codeMode = new CodeModeToolProvider(fileTools(), maxScriptToolCalls);
        var tracker = new AgentLoopTracker(codeMode, maxToolCalls);
        codeMode.trackNestedCallsWith(tracker);
        tracker.registerChild(codeMode);
        return tracker;
    }

    private String runScriptThrough(AgentLoopTracker tracker, String code) {
        return tracker.provideTools(this.providerRequest)
                .toolExecutorByName(RUN_SCRIPT_TOOL_NAME)
                .execute(runScriptRequest(code), null);
    }

    private static ToolExecutionRequest runScriptRequest(String code) {
        var arguments = new JsonObject();
        arguments.addProperty("code", code);
        return ToolExecutionRequest.builder().name(RUN_SCRIPT_TOOL_NAME).arguments(arguments.toString()).build();
    }

    private ToolProvider fileTools() {
        var readFile = ToolSpecification.builder()
                .name("read_file")
                .description("Read a file. Returns its content.")
                .parameters(JsonObjectSchema.builder().addStringProperty("path").required("path").build())
                .build();
        var listFiles = ToolSpecification.builder().name("list_files").description("List the project files.").build();
        ToolExecutor readFileExecutor = (request, memoryId) -> {
            var path = ToolArgumentParser.getString(request.arguments(), "path");
            this.readPaths.add(path);
            return FILES.getOrDefault(path, "Error: no such file " + path);
        };
        ToolExecutor listFilesExecutor = (request, memoryId) -> "A.java\nB.java\nC.java";
        return request -> ToolProviderResult.builder()
                .add(readFile, readFileExecutor)
                .add(listFiles, listFilesExecutor)
                .build();
    }

    private static ToolProvider singleTool(String name, ToolExecutor executor) {
        var specification = ToolSpecification.builder().name(name).description("A test tool.").build();
        return request -> ToolProviderResult.builder().add(specification, executor).build();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void warmUpRunsWithoutTools() {
        assertThatCode(CodeModeToolProvider::warmUp).doesNotThrowAnyException();
    }
}
