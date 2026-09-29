package com.devoxx.genie.service.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.model.Constant.SUB_AGENT_MEMORY_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

class SubAgentMemoryTest {

    private static final String ASSIGNMENT = "Investigate the following in the project codebase:\n\n"
            + "The secret word is MANGO-7. Read 6 files one at a time, then reply with only the secret word.";

    private static final int SIX_READS = 6;

    interface Explorer {
        String chat(String userMessage);
    }

    @Test
    void subAgentStillSeesItsAssignmentAfterMoreToolCallsThanItsMemoryWindowHolds() {
        var memory = SubAgentRunner.newSubAgentMemory();
        var requests = new ArrayList<ChatRequest>();
        var readFile = ToolSpecification.builder().name("read_file").description("Reads a file")
                .parameters(JsonObjectSchema.builder().build()).build();

        AiServices.builder(Explorer.class)
                .chatModel(readsFilesThenAnswers(SIX_READS, requests))
                .chatMemoryProvider(memoryId -> memory)
                .systemMessageProvider(memoryId -> "sub-agent system prompt")
                .toolProvider(request -> ToolProviderResult.builder()
                        .add(readFile, (call, memoryId) -> "file content " + call.id()).build())
                .build()
                .chat(ASSIGNMENT);

        assertThat(SIX_READS * 2 + 2).isGreaterThan(SUB_AGENT_MEMORY_SIZE);
        assertThat(requests).hasSize(SIX_READS + 1);
        assertThat(requests).allSatisfy(request -> assertThat(request.messages())
                .filteredOn(UserMessage.class::isInstance)
                .extracting(message -> ((UserMessage) message).singleText())
                .contains(ASSIGNMENT));
    }

    private static ChatModel readsFilesThenAnswers(int reads, List<ChatRequest> requests) {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                requests.add(request);
                int issued = requests.size() - 1;
                if (issued >= reads) {
                    return ChatResponse.builder().aiMessage(AiMessage.from("MANGO-7")).build();
                }
                return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                        .id("call_" + issued).name("read_file").arguments("{}").build())).build();
            }
        };
    }
}
