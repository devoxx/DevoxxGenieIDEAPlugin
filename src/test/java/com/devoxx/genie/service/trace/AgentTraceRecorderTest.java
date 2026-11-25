package com.devoxx.genie.service.trace;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentTraceRecorderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC);
    private static final TraceSettings ON = new TraceSettings(true, 50_000, true, List.of(), TruffleHogMode.OFF,
            TruffleHogOnFindings.REDACT, "", false);

    private AgentTraceBuffer buffer;
    private UUID invocation;

    @BeforeEach
    void setUp() {
        buffer = new AgentTraceBuffer(CLOCK);
        invocation = UUID.randomUUID();
    }

    private AgentTraceRecorder main(String key) {
        return new AgentTraceRecorder(buffer, key, null, ON, CLOCK);
    }

    private InvocationContext context(UUID id) {
        InvocationContext ctx = mock(InvocationContext.class);
        when(ctx.invocationId()).thenReturn(id);
        return ctx;
    }

    private AiServiceStartedEvent started(String text, UUID id) {
        InvocationContext ctx = context(id); // create before stubbing: no nested mock() inside when()
        AiServiceStartedEvent e = mock(AiServiceStartedEvent.class);
        when(e.userMessage()).thenReturn(UserMessage.from(text));
        when(e.invocationContext()).thenReturn(ctx);
        return e;
    }

    private static AiServiceResponseReceivedEvent response(AiMessage ai, String model) {
        AiServiceResponseReceivedEvent e = mock(AiServiceResponseReceivedEvent.class);
        when(e.response()).thenReturn(ChatResponse.builder().aiMessage(ai).modelName(model).build());
        return e;
    }

    private static ToolExecutedEvent toolExecuted(String id, String name, String result, boolean error) {
        ToolExecutedEvent e = mock(ToolExecutedEvent.class);
        when(e.request()).thenReturn(ToolExecutionRequest.builder().id(id).name(name).arguments("{}").build());
        when(e.resultText()).thenReturn(result);
        when(e.isError()).thenReturn(error);
        return e;
    }

    @Test
    void agentTurn_isRecordedInOrder_withToolCallsReasoningAndResults() {
        AgentTraceRecorder recorder = main("hash-tab1");
        ToolExecutionRequest call = ToolExecutionRequest.builder()
                .id("call_1").name("read_file").arguments("{\"path\":\"A.java\"}").build();

        recorder.onStarted(started("Fix the test", invocation));
        recorder.onResponse(response(AiMessage.builder().thinking("Read A first")
                .toolExecutionRequests(List.of(call)).build(), "model-x"));
        recorder.onToolExecuted(toolExecuted("call_1", "read_file", "class A {}", false));
        recorder.onResponse(response(AiMessage.from("Fixed."), "model-x"));

        List<TraceEvent> events = buffer.take("hash-tab1").events();
        assertThat(events).containsExactly(
                TraceEvent.user("Fix the test", null, 1_000),
                TraceEvent.assistant(null, "Read A first",
                        List.of(new TraceEvent.ToolCall("call_1", "read_file", "{\"path\":\"A.java\"}")),
                        "model-x", null, 1_000),
                TraceEvent.tool("call_1", "read_file", "class A {}", false, null, 1_000),
                TraceEvent.assistant("Fixed.", null, List.of(), "model-x", null, 1_000));
    }

    @Test
    void toolError_isFlagged() {
        AgentTraceRecorder recorder = main("k");
        recorder.onStarted(started("go", invocation));
        recorder.onToolExecuted(toolExecuted("c1", "run_command", "Error: boom", true));

        assertThat(buffer.take("k").events().get(1).error()).isTrue();
    }

    @Test
    void twoTabs_areRecordedSeparately() {
        AgentTraceRecorder tab1 = main("hash-tab1");
        AgentTraceRecorder tab2 = main("hash-tab2");

        tab1.onStarted(started("first tab", UUID.randomUUID()));
        tab2.onStarted(started("second tab", UUID.randomUUID()));
        tab1.onResponse(response(AiMessage.from("answer 1"), "m"));
        tab2.onResponse(response(AiMessage.from("answer 2"), "m"));

        assertThat(buffer.take("hash-tab1").events()).extracting(TraceEvent::content)
                .containsExactly("first tab", "answer 1");
        assertThat(buffer.take("hash-tab2").events()).extracting(TraceEvent::content)
                .containsExactly("second tab", "answer 2");
    }

    @Test
    void subAgent_appendsToParentTurn_taggedWithItsId_andDoesNotRestartIt() {
        AgentTraceRecorder parent = main("hash-tab1");
        parent.onStarted(started("explore", invocation));
        String parentKey = buffer.keyForInvocation(invocation);
        assertThat(parentKey).isEqualTo("hash-tab1");

        AgentTraceRecorder sub = new AgentTraceRecorder(buffer, parentKey, "sub-agent-1", ON, CLOCK);
        sub.onStarted(started("Investigate X", UUID.randomUUID()));
        sub.onResponse(response(AiMessage.from("X lives in Foo.java"), "small-model"));

        List<TraceEvent> events = buffer.take("hash-tab1").events();
        assertThat(events).extracting(TraceEvent::content).containsExactly("explore", "Investigate X", "X lives in Foo.java");
        assertThat(events).extracting(TraceEvent::subAgentId).containsExactly(null, "sub-agent-1", "sub-agent-1");
    }

    @Test
    void completedInvocation_canNoLongerBeJoinedBySubAgents() {
        AgentTraceRecorder recorder = main("k");
        recorder.onStarted(started("go", invocation));
        InvocationContext ctx = context(invocation);
        AiServiceCompletedEvent done = mock(AiServiceCompletedEvent.class);
        when(done.invocationContext()).thenReturn(ctx);

        recorder.onEnded(done);

        assertThat(buffer.keyForInvocation(invocation)).isNull();
        assertThat(buffer.hasTurn("k")).isTrue(); // still waiting for ChatService to save it
    }

    @Test
    void newTurn_replacesAnUnsavedPreviousTurn() {
        AgentTraceRecorder recorder = main("k");
        recorder.onStarted(started("first", UUID.randomUUID()));
        recorder.onStarted(started("second", UUID.randomUUID()));

        assertThat(buffer.take("k").events()).extracting(TraceEvent::content).containsExactly("second");
    }

    @Test
    void multiPartUserMessage_keepsTextAndNotesOtherContent() {
        InvocationContext ctx = context(invocation);
        AiServiceStartedEvent e = mock(AiServiceStartedEvent.class);
        when(e.userMessage()).thenReturn(UserMessage.from(List.of(TextContent.from("look"),
                ImageContent.from("https://example.invalid/x.png"))));
        when(e.invocationContext()).thenReturn(ctx);

        main("k").onStarted(e);

        assertThat(buffer.take("k").events().get(0).content()).isEqualTo("look\n[IMAGE content]");
    }

    @Test
    void listenerFailure_isSwallowed() {
        AgentTraceRecorder recorder = main("k");
        AiServiceResponseReceivedEvent broken = mock(AiServiceResponseReceivedEvent.class);
        when(broken.response()).thenThrow(new IllegalStateException("boom"));

        @SuppressWarnings("unchecked")
        AiServiceListener<AiServiceResponseReceivedEvent> listener = (AiServiceListener<AiServiceResponseReceivedEvent>)
                recorder.listeners().stream()
                        .filter(l -> l.getEventClass() == AiServiceResponseReceivedEvent.class)
                        .findFirst().orElseThrow();

        listener.onEvent(broken); // must not throw
    }

    @Test
    void eventsWithoutRunningTurn_areIgnored() {
        main("k").onResponse(response(AiMessage.from("orphan"), "m"));
        assertThat(buffer.hasTurn("k")).isFalse();
    }

    @Test
    void staleUnsavedTurns_arePurgedWhenANewTurnStarts() {
        Clock[] now = {Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC)};
        AgentTraceBuffer timed = new AgentTraceBuffer(new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now[0].instant(); }
        });
        timed.startTurn("closed-tab", UUID.randomUUID(), ON);
        now[0] = Clock.fixed(Instant.ofEpochMilli(AgentTraceBuffer.STALE_AFTER.toMillis() + 1), ZoneOffset.UTC);

        timed.startTurn("other-tab", UUID.randomUUID(), ON);

        assertThat(timed.hasTurn("closed-tab")).isFalse();
        assertThat(timed.hasTurn("other-tab")).isTrue();
    }
}
