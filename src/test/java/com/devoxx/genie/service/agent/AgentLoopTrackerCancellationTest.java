package com.devoxx.genie.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AgentLoopTrackerCancellationTest {

    @Mock
    private ToolProviderRequest providerRequest;

    @Test
    void cancel_propagatesToRegisteredChildren() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 10);

        AtomicBoolean child1Cancelled = new AtomicBoolean(false);
        AtomicBoolean child2Cancelled = new AtomicBoolean(false);

        tracker.registerChild(() -> child1Cancelled.set(true));
        tracker.registerChild(() -> child2Cancelled.set(true));

        assertThat(child1Cancelled.get()).isFalse();
        assertThat(child2Cancelled.get()).isFalse();

        tracker.cancel();

        assertThat(child1Cancelled.get()).isTrue();
        assertThat(child2Cancelled.get()).isTrue();
        assertThat(tracker.isCancelled()).isTrue();
    }

    @Test
    void registerChild_whenAlreadyCancelled_cancelsImmediately() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 10);
        tracker.cancel();

        AtomicBoolean childCancelled = new AtomicBoolean(false);
        tracker.registerChild(() -> childCancelled.set(true));

        assertThat(childCancelled.get()).isTrue();
    }

    @Test
    void cancelled_toolCall_returnsErrorMessage() {
        ToolSpecification spec = createSpec("test_tool");
        ToolExecutor original = (req, id) -> "success";
        ToolProvider delegate = req -> ToolProviderResult.builder().add(spec, original).build();

        AgentLoopTracker tracker = new AgentLoopTracker(delegate, 10);
        ToolProviderResult result = tracker.provideTools(providerRequest);

        tracker.cancel();

        ToolExecutor trackedExecutor = result.tools().values().iterator().next();
        String cancelResult = trackedExecutor.execute(createExecRequest("test_tool"), null);
        assertThat(cancelResult).contains("cancelled");
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
