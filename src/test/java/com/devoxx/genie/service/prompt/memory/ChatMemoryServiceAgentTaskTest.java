package com.devoxx.genie.service.prompt.memory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryServiceAgentTaskTest {

    private static final String MEMORY_KEY = "project-hash-tab";

    private static final String TASK = "The secret word is MANGO-7. Use list_files, then read 6 files one at a time. "
            + "When you have read all 6, reply with ONLY the secret word.";

    private static final int CHAT_MEMORY_SIZE = 10;

    private static final int LIST_FILES_THEN_SIX_READS = 7;

    interface Assistant {
        AiMessage chat(String userMessage);
    }

    @Test
    void everyAgentRequestStillContainsTheTaskWhenToolCallsOutgrowTheChatMemorySize() {
        var chatMemoryService = new ChatMemoryService();
        chatMemoryService.initializeByKey(MEMORY_KEY, CHAT_MEMORY_SIZE);
        var chatMemory = chatMemoryService.get(MEMORY_KEY);
        var model = new ToolCallingModel(LIST_FILES_THEN_SIX_READS);

        AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> chatMemory)
                .systemMessageProvider(memoryId -> "agent system prompt")
                .toolProvider(fileTools())
                .build()
                .chat(TASK);

        assertThat(model.requests).hasSize(LIST_FILES_THEN_SIX_READS + 1);
        assertThat(model.requests).allSatisfy(request -> assertThat(userTextsIn(request)).contains(TASK));
    }

    @Test
    void plainChatWithoutToolsStillEvictsTheOldestMessagesFirst() {
        var chatMemoryService = new ChatMemoryService();
        chatMemoryService.initializeByKey(MEMORY_KEY, 4);

        for (int turn = 1; turn <= 4; turn++) {
            chatMemoryService.addMessageByKey(MEMORY_KEY, UserMessage.from("question " + turn));
            chatMemoryService.addMessageByKey(MEMORY_KEY, AiMessage.from("answer " + turn));
        }

        assertThat(chatMemoryService.getMessagesByKey(MEMORY_KEY)).containsExactly(
                UserMessage.from("question 3"), AiMessage.from("answer 3"),
                UserMessage.from("question 4"), AiMessage.from("answer 4"));
    }

    private static List<String> userTextsIn(ChatRequest request) {
        return request.messages().stream()
                .filter(UserMessage.class::isInstance)
                .map(message -> ((UserMessage) message).singleText())
                .toList();
    }

    private static ToolProvider fileTools() {
        ToolExecutor fileContent = (request, memoryId) -> "file content of " + request.arguments();
        var listFiles = ToolSpecification.builder().name("list_files").description("Lists files")
                .parameters(JsonObjectSchema.builder().build()).build();
        var readFile = ToolSpecification.builder().name("read_file").description("Reads a file")
                .parameters(JsonObjectSchema.builder().build()).build();
        return request -> ToolProviderResult.builder().add(listFiles, fileContent).add(readFile, fileContent).build();
    }

    private static final class ToolCallingModel implements ChatModel {

        private final int toolCalls;

        private final List<ChatRequest> requests = new ArrayList<>();

        private ToolCallingModel(int toolCalls) {
            this.toolCalls = toolCalls;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            this.requests.add(request);
            int issued = this.requests.size() - 1;
            if (issued >= this.toolCalls) {
                return ChatResponse.builder().aiMessage(AiMessage.from("MANGO-7")).build();
            }
            String tool = issued == 0 ? "list_files" : "read_file";
            return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                    .id("call_" + issued)
                    .name(tool)
                    .arguments("{\"n\":" + issued + "}")
                    .build())).build();
        }
    }
}
