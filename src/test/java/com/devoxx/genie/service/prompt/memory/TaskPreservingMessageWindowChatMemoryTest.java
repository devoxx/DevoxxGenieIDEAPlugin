package com.devoxx.genie.service.prompt.memory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskPreservingMessageWindowChatMemoryTest {

    private static final SystemMessage SYSTEM = SystemMessage.from("system");

    private static final int SEEDS = 2_000;

    @Nested
    class BehavesExactlyLikeMessageWindowChatMemory {

        @Test
        void forEveryPlainChatWithoutToolCalls() {
            for (long seed = 0; seed < SEEDS; seed++) {
                var random = new Random(seed);
                int maxMessages = 1 + random.nextInt(8);
                var original = MessageWindowChatMemory.withMaxMessages(maxMessages);
                var preserving = TaskPreservingMessageWindowChatMemory.withMaxMessages(maxMessages);

                for (int step = 0; step < 40; step++) {
                    var message = plainChatMessage(random, step);
                    original.add(message);
                    preserving.add(message);

                    assertThat(preserving.messages()).as("seed %d step %d", seed, step).isEqualTo(original.messages());
                }
            }
        }

        @Test
        void forEveryAgentConversationWhoseWindowIsTooSmallToHoldTheTaskAndOneExchange() {
            for (long seed = 0; seed < SEEDS; seed++) {
                var random = new Random(seed);
                int maxMessages = 1 + random.nextInt(3);
                var original = MessageWindowChatMemory.withMaxMessages(maxMessages);
                var preserving = TaskPreservingMessageWindowChatMemory.withMaxMessages(maxMessages);

                for (Step step : agentConversation(random, 1)) {
                    original.add(step.message());
                    preserving.add(step.message());

                    assertThat(preserving.messages()).as("seed %d", seed).isEqualTo(original.messages());
                }
            }
        }

        @Test
        void forEveryAgentConversationUntilTheOriginalWouldEvictAUserMessageOfTheRunningToolLoop() {
            int conversationsWhereTheTaskWasKept = 0;
            for (long seed = 0; seed < SEEDS; seed++) {
                var random = new Random(seed);
                int maxMessages = 4 + random.nextInt(12);
                var original = MessageWindowChatMemory.withMaxMessages(maxMessages);
                var preserving = TaskPreservingMessageWindowChatMemory.withMaxMessages(maxMessages);
                boolean diverged = false;

                for (Step step : agentConversation(random, 3)) {
                    original.add(step.message());
                    preserving.add(step.message());
                    var expected = original.messages();
                    var actual = preserving.messages();

                    if (!diverged && !actual.equals(expected)) {
                        diverged = true;
                        conversationsWhereTheTaskWasKept++;
                        assertThat(countOf(step.runningTurnUserMessages(), actual))
                                .as("seed %d: the only allowed difference is a kept user message of the running loop", seed)
                                .isGreaterThan(countOf(step.runningTurnUserMessages(), expected));
                    }
                    assertThat(actual).as("seed %d", seed).hasSizeLessThanOrEqualTo(maxMessages);
                    assertEveryToolResultFollowsItsToolCall(actual, seed);
                }
            }
            assertThat(conversationsWhereTheTaskWasKept).isGreaterThan(SEEDS / 4);
        }

        @Test
        void whenMessagesAreReplacedWithSet() {
            for (long seed = 0; seed < SEEDS; seed++) {
                var random = new Random(seed);
                int maxMessages = 1 + random.nextInt(8);
                var messages = IntStream.range(0, 30).mapToObj(step -> plainChatMessage(random, step)).toList();
                var original = MessageWindowChatMemory.withMaxMessages(maxMessages);
                var preserving = TaskPreservingMessageWindowChatMemory.withMaxMessages(maxMessages);

                original.set(messages);
                preserving.set(messages);
                assertThat(preserving.messages()).as("seed %d list", seed).isEqualTo(original.messages());

                original.set(messages::iterator);
                preserving.set(messages::iterator);
                assertThat(preserving.messages()).as("seed %d iterable", seed).isEqualTo(original.messages());
            }
        }

        @Test
        void whenMessagesAreAddedAndReadAsynchronously() {
            for (long seed = 0; seed < 200; seed++) {
                var random = new Random(seed);
                int maxMessages = 1 + random.nextInt(8);
                var messages = IntStream.range(0, 30).mapToObj(step -> plainChatMessage(random, step)).toList();
                var original = MessageWindowChatMemory.withMaxMessages(maxMessages);
                var preserving = TaskPreservingMessageWindowChatMemory.withMaxMessages(maxMessages);

                original.addAsync(messages).join();
                preserving.addAsync(messages).join();
                assertThat(preserving.messagesAsync().join()).as("seed %d", seed).isEqualTo(original.messagesAsync().join());

                original.setAsync(messages).join();
                preserving.setAsync(messages).join();
                assertThat(preserving.messages()).as("seed %d", seed).isEqualTo(original.messages());
            }
        }

        @Test
        void whenTwoMemoriesShareOneStoreAndOneIsCleared() {
            var store = new InMemoryChatMemoryStore();
            var first = TaskPreservingMessageWindowChatMemory.builder().id("first").chatMemoryStore(store).maxMessages(5).build();
            var second = TaskPreservingMessageWindowChatMemory.builder().id("second").chatMemoryStore(store).maxMessages(5).build();

            first.add(UserMessage.from("first question"));
            second.add(UserMessage.from("second question"));
            first.clear();

            assertThat(first.messages()).isEmpty();
            assertThat(second.messages()).containsExactly(UserMessage.from("second question"));
            assertThat(first.id()).isEqualTo("first");
        }

        @Test
        void whenMaxMessagesIsMissingOrNotPositive() {
            assertThatThrownBy(() -> TaskPreservingMessageWindowChatMemory.builder().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("maxMessages must be greater than zero, but is: null");
            assertThatThrownBy(() -> TaskPreservingMessageWindowChatMemory.withMaxMessages(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("maxMessages must be greater than zero, but is: 0");
            assertThatThrownBy(() -> MessageWindowChatMemory.withMaxMessages(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("maxMessages must be greater than zero, but is: 0");
        }
    }

    @Nested
    class KeepsTheTaskOfARunningToolLoop {

        @Test
        void whenToolExchangesOutgrowTheWindowItEvictsTheOldestFinishedExchangeInstead() {
            var memory = TaskPreservingMessageWindowChatMemory.withMaxMessages(10);
            var task = UserMessage.from("The secret word is MANGO-7");
            memory.add(SYSTEM);
            memory.add(task);
            for (int call = 1; call <= 5; call++) {
                memory.add(toolCall("call_" + call));
                memory.add(toolResult("call_" + call));
            }

            assertThat(memory.messages()).containsExactly(
                    SYSTEM, task,
                    toolCall("call_2"), toolResult("call_2"),
                    toolCall("call_3"), toolResult("call_3"),
                    toolCall("call_4"), toolResult("call_4"),
                    toolCall("call_5"), toolResult("call_5"));
        }

        @Test
        void whenTheUserSteersTheAgentMidRunBothUserMessagesAreKept() {
            var memory = TaskPreservingMessageWindowChatMemory.withMaxMessages(8);
            var task = UserMessage.from("task");
            var steering = UserMessage.from("also check the tests");
            memory.add(SYSTEM);
            memory.add(task);
            memory.add(toolCall("call_1"));
            memory.add(toolResult("call_1"));
            memory.add(steering);
            for (int call = 2; call <= 4; call++) {
                memory.add(toolCall("call_" + call));
                memory.add(toolResult("call_" + call));
            }

            assertThat(memory.messages()).containsExactly(
                    SYSTEM, task, steering,
                    toolCall("call_3"), toolResult("call_3"),
                    toolCall("call_4"), toolResult("call_4"));
        }

        @Test
        void onlyUntilTheFinalAnswerArrivesAfterWhichTheTaskIsEvictedFirstLikeTheOriginal() {
            var memory = TaskPreservingMessageWindowChatMemory.withMaxMessages(6);
            var task = UserMessage.from("task");
            memory.add(SYSTEM);
            memory.add(task);
            memory.add(toolCall("call_1"));
            memory.add(toolResult("call_1"));
            memory.add(toolCall("call_2"));
            memory.add(toolResult("call_2"));
            memory.add(AiMessage.from("final answer"));

            assertThat(memory.messages()).containsExactly(
                    SYSTEM, toolCall("call_1"), toolResult("call_1"),
                    toolCall("call_2"), toolResult("call_2"), AiMessage.from("final answer"));
        }

        @Test
        void neverByEvictingTheToolCallWhoseResultsAreStillArriving() {
            var memory = TaskPreservingMessageWindowChatMemory.withMaxMessages(4);
            var original = MessageWindowChatMemory.withMaxMessages(4);
            var task = UserMessage.from("task");
            var parallelCalls = toolCall("call_1", "call_2", "call_3");
            List<ChatMessage> sequence = List.of(SYSTEM, task, parallelCalls,
                    toolResult("call_1"), toolResult("call_2"), toolResult("call_3"));

            for (ChatMessage message : sequence) {
                memory.add(message);
                original.add(message);
                assertThat(memory.messages()).isEqualTo(original.messages());
            }
        }
    }

    private record Step(ChatMessage message, List<UserMessage> runningTurnUserMessages) {
    }

    private static List<Step> agentConversation(Random random, int maxParallelToolCalls) {
        var steps = new ArrayList<Step>();
        int turns = 1 + random.nextInt(4);
        for (int turn = 0; turn < turns; turn++) {
            steps.add(new Step(SYSTEM, List.of()));
            var runningTurnUserMessages = new ArrayList<UserMessage>();
            var task = UserMessage.from("task " + turn);
            runningTurnUserMessages.add(task);
            steps.add(new Step(task, List.copyOf(runningTurnUserMessages)));
            int exchanges = random.nextInt(9);
            for (int exchange = 0; exchange < exchanges; exchange++) {
                var ids = new String[1 + random.nextInt(maxParallelToolCalls)];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = turn + "-" + exchange + "-" + i;
                }
                steps.add(new Step(toolCall(ids), List.copyOf(runningTurnUserMessages)));
                for (String id : ids) {
                    steps.add(new Step(toolResult(id), List.copyOf(runningTurnUserMessages)));
                }
                if (random.nextInt(6) == 0) {
                    var steering = UserMessage.from("steer " + turn + "-" + exchange);
                    runningTurnUserMessages.add(steering);
                    steps.add(new Step(steering, List.copyOf(runningTurnUserMessages)));
                }
            }
            steps.add(new Step(AiMessage.from("answer " + turn), List.of()));
        }
        return steps;
    }

    private static ChatMessage plainChatMessage(Random random, int step) {
        return switch (random.nextInt(5)) {
            case 0 -> SystemMessage.from("system A");
            case 1 -> SystemMessage.from("system B");
            case 2 -> AiMessage.from("answer " + step);
            default -> UserMessage.from("question " + step);
        };
    }

    private static AiMessage toolCall(String... ids) {
        var requests = new ArrayList<ToolExecutionRequest>();
        for (String id : ids) {
            requests.add(ToolExecutionRequest.builder().id(id).name("read_file").arguments("{\"id\":\"" + id + "\"}").build());
        }
        return AiMessage.from(requests);
    }

    private static ToolExecutionResultMessage toolResult(String id) {
        return ToolExecutionResultMessage.from(id, "read_file", "content " + id);
    }

    private static long countOf(List<UserMessage> wanted, List<ChatMessage> messages) {
        return wanted.stream().filter(messages::contains).count();
    }

    private static void assertEveryToolResultFollowsItsToolCall(List<ChatMessage> messages, long seed) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof ToolExecutionResultMessage result) {
                int call = i - 1;
                while (call >= 0 && messages.get(call) instanceof ToolExecutionResultMessage) {
                    call--;
                }
                assertThat(call).as("seed %d: orphan tool result %s", seed, result.id()).isGreaterThanOrEqualTo(0);
                assertThat(messages.get(call)).as("seed %d", seed).isInstanceOfSatisfying(AiMessage.class,
                        ai -> assertThat(ai.toolExecutionRequests()).extracting(ToolExecutionRequest::id).contains(result.id()));
            }
        }
    }
}
