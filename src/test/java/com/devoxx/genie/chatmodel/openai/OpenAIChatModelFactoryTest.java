package com.devoxx.genie.chatmodel.openai;

import com.devoxx.genie.model.ChatModel;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OpenAIChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        OpenAIChatModelFactory factory = new OpenAIChatModelFactory("demo", "gpt-3.5-turbo");
        List<String> modelNames = factory.getModelNames();

        ChatModel chatModel= new ChatModel();
        chatModel.setModelName(modelNames.get(0));
        chatModel.setMaxRetries(3);
        chatModel.setMaxTokens(1_000);

        ChatLanguageModel chatLanguageModel = factory.createChatModel(chatModel);
        String generate = chatLanguageModel.generate("Hello, how are you?");
        assertThat(generate).isNotNull();
    }
}
