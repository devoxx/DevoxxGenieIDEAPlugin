package com.devoxx.genie.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AgentLoopTrackerTest {

    @Mock
    private ToolProviderRequest providerRequest;

    @Test
    void execute_withinLimit_delegatesToOriginal() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 3);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        ToolExecutor trackedExecutor = result.tools().values().iterator().next();
        ToolExecutionRequest execRequest = createExecRequest("test_tool");

        assertThat(trackedExecutor.execute(execRequest, null)).isEqualTo("success");
        assertThat(trackedExecutor.execute(execRequest, null)).isEqualTo("success");
        assertThat(trackedExecutor.execute(execRequest, null)).isEqualTo("success");
        assertThat(tracker.getCallCount()).isEqualTo(3);
    }

    @Test
    void execute_exceedingLimit_returnsErrorString() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 2);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        ToolExecutor trackedExecutor = result.tools().values().iterator().next();
        ToolExecutionRequest execRequest = createExecRequest("test_tool");

        trackedExecutor.execute(execRequest, null); // 1
        trackedExecutor.execute(execRequest, null); // 2

        String errorResult = trackedExecutor.execute(execRequest, null); // 3 > limit of 2
        assertThat(errorResult).contains("Agent loop limit reached");
        assertThat(errorResult).contains("2 tool calls");
    }

    @Test
    void execute_sharedCounterAcrossMultipleTools() {
        ToolSpecification spec1 = createSpec("tool_a");
        ToolSpecification spec2 = createSpec("tool_b");
        ToolExecutor exec1 = (req, id) -> "result_a";
        ToolExecutor exec2 = (req, id) -> "result_b";

        ToolProvider delegate = req -> ToolProviderResult.builder()
                .add(spec1, exec1)
                .add(spec2, exec2)
                .build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 3);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        // Get executors by iterating - order may vary
        ToolExecutor[] executors = result.tools().values().toArray(new ToolExecutor[0]);

        executors[0].execute(createExecRequest("tool_a"), null); // count=1
        executors[1].execute(createExecRequest("tool_b"), null); // count=2
        executors[0].execute(createExecRequest("tool_a"), null); // count=3

        // 4th call should fail regardless of which tool
        String errorResult = executors[1].execute(createExecRequest("tool_b"), null);
        assertThat(errorResult).contains("Agent loop limit reached");
        assertThat(tracker.getCallCount()).isEqualTo(4);
    }

    @Test
    void reset_resetsCounter() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 5);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        ToolExecutor trackedExecutor = result.tools().values().iterator().next();
        trackedExecutor.execute(createExecRequest("test_tool"), null);
        assertThat(tracker.getCallCount()).isEqualTo(1);

        tracker.reset();
        assertThat(tracker.getCallCount()).isEqualTo(0);
    }

    /**
     * Regression test for issue #1040: skills stopped activating with
     * IllegalStateException("executeWithContext must be called instead").
     *
     * <p>The loop tracker must forward executeWithContext() (the method langchain4j's ToolService
     * actually invokes) to the wrapped executor instead of routing through execute(), and must
     * return the original ToolExecutionResult so skill-activation attributes survive.</p>
     */
    @Test
    void executeWithContext_skillExecutor_forwardsAndPreservesAttributes() {
        ToolSpecification spec = createSpec("activate_skill");
        ToolExecutor skillExecutor = skillStyleExecutor("Skill body", Map.of("activated_skill", "release"));
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, skillExecutor).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 3);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        ToolExecutor wrapped = result.tools().values().iterator().next();
        ToolExecutionResult execResult =
                wrapped.executeWithContext(createExecRequest("activate_skill"), null);

        assertThat(execResult.resultText()).isEqualTo("Skill body");
        assertThat(execResult.attributes()).containsEntry("activated_skill", "release");
        assertThat(tracker.getCallCount()).isEqualTo(1);
    }

    @Test
    void executeWithContext_overLimit_returnsErrorWithoutInvokingSkill() {
        ToolSpecification spec = createSpec("activate_skill");
        ToolExecutor skillExecutor = skillStyleExecutor("Skill body", Map.of("activated_skill", "release"));
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, skillExecutor).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 1);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        ToolExecutor wrapped = result.tools().values().iterator().next();
        wrapped.executeWithContext(createExecRequest("activate_skill"), null); // 1 (allowed)

        ToolExecutionResult execResult =
                wrapped.executeWithContext(createExecRequest("activate_skill"), null); // 2 > limit
        assertThat(execResult.resultText()).contains("Agent loop limit reached");
    }

    /**
     * Builds a ToolExecutor that behaves like the langchain4j-skills executors: {@code execute()}
     * throws, while {@code executeWithContext()} returns the real result.
     */
    private ToolExecutor skillStyleExecutor(String text, Map<String, Object> attributes) {
        return new ToolExecutor() {
            @Override
            public String execute(ToolExecutionRequest request, Object memoryId) {
                throw new IllegalStateException("executeWithContext must be called instead");
            }

            @Override
            public ToolExecutionResult executeWithContext(ToolExecutionRequest request,
                                                          InvocationContext context) {
                return ToolExecutionResult.builder()
                        .resultText(text)
                        .attributes(attributes)
                        .build();
            }
        };
    }

    private ToolSpecification createSpec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description("Test tool")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    private ToolExecutionRequest createExecRequest(String name) {
        return ToolExecutionRequest.builder()
                .name(name)
                .arguments("{}")
                .build();
    }
}
