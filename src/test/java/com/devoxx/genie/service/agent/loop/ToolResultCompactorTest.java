package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ToolResultCompactorTest {

    private static final String BIG = "x".repeat(5_000);

    private static ToolExecutionResultMessage result(int i, String text) {
        return ToolExecutionResultMessage.from("id" + i, "read_file", text);
    }

    private static List<ChatMessage> conversation(String... toolResults) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(UserMessage.from("question"));
        for (int i = 0; i < toolResults.length; i++) {
            messages.add(AiMessage.from("thinking " + i));
            messages.add(result(i, toolResults[i]));
        }
        return messages;
    }

    @Test
    void keepsEverything_whenNoMoreToolResultsThanKeepRecent() {
        ToolResultCompactor compactor = new ToolResultCompactor(2, 1_000, 100);
        List<ChatMessage> messages = conversation(BIG, BIG);

        ToolResultCompactor.Result result = compactor.compact(messages);

        assertThat(result.compacted()).isZero();
        assertThat(result.messages()).isSameAs(messages);
    }

    @Test
    void compactsOnlyOlderLargeResults_andKeepsRecentOnesVerbatim() {
        ToolResultCompactor compactor = new ToolResultCompactor(2, 1_000, 100);
        List<ChatMessage> messages = conversation(BIG, "small", BIG, BIG);

        ToolResultCompactor.Result result = compactor.compact(messages);

        assertThat(result.compacted()).isEqualTo(1);
        List<ToolExecutionResultMessage> tools = result.messages().stream()
                .filter(ToolExecutionResultMessage.class::isInstance)
                .map(ToolExecutionResultMessage.class::cast)
                .toList();
        assertThat(tools.get(0).text()).startsWith("x".repeat(100))
                .contains(ToolResultCompactor.MARKER_PREFIX)
                .contains("Call read_file again");
        assertThat(tools.get(0).text().length()).isLessThan(BIG.length());
        assertThat(tools.get(0).id()).isEqualTo("id0");
        assertThat(tools.get(1).text()).isEqualTo("small");
        assertThat(tools.get(2).text()).isEqualTo(BIG);
        assertThat(tools.get(3).text()).isEqualTo(BIG);
        assertThat(result.charsSaved()).isEqualTo(BIG.length() - tools.get(0).text().length());
    }

    @Test
    void doesNotMutateTheInputList() {
        ToolResultCompactor compactor = new ToolResultCompactor(0, 1_000, 100);
        List<ChatMessage> messages = conversation(BIG);

        compactor.compact(messages);

        assertThat(((ToolExecutionResultMessage) messages.get(2)).text()).isEqualTo(BIG);
    }

    @Test
    void compactionIsStable_acrossRoundTrips() {
        ToolResultCompactor compactor = new ToolResultCompactor(1, 1_000, 100);
        List<ChatMessage> first = compactor.compact(conversation(BIG, BIG)).messages();
        List<ChatMessage> second = compactor.compact(conversation(BIG, BIG, BIG)).messages();

        // The first result is compacted identically in both requests (stable prompt prefix).
        assertThat(((ToolExecutionResultMessage) second.get(2)).text())
                .isEqualTo(((ToolExecutionResultMessage) first.get(2)).text());
    }

    @Test
    void keepsUntrustedBlockBalanced() {
        ToolResultCompactor compactor = new ToolResultCompactor(0, 1_000, 100);
        String wrapped = UntrustedContent.wrap("fetch_page", BIG);

        String shortened = compactor.shorten("fetch_page", wrapped);

        assertThat(shortened).startsWith("<" + UntrustedContent.TAG)
                .endsWith("</" + UntrustedContent.TAG + ">");
    }

    @Test
    void preservesErrorFlag() {
        ToolResultCompactor compactor = new ToolResultCompactor(0, 1_000, 100);
        ToolExecutionResultMessage error = ToolExecutionResultMessage.builder()
                .id("e").toolName("run_command").text(BIG).isError(true).build();

        ToolResultCompactor.Result result = compactor.compact(List.of(UserMessage.from("q"), error));

        assertThat(((ToolExecutionResultMessage) result.messages().get(1)).isError()).isTrue();
    }

    @Test
    void neverCompactsSmallResults() {
        ToolResultCompactor compactor = new ToolResultCompactor(0, 1_000, 100);
        assertThat(compactor.shorten("read_file", "y".repeat(999))).isNull();
    }
}
