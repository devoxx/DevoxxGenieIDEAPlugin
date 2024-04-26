package com.devoxx.genie.chatmodel.anthropic;

import com.devoxx.genie.model.ChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AnthropicChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        // Instance of the class containing the method to be tested
        var factory = new AnthropicChatModelFactory("apiKey", "modelName");

        // Create a dummy ChatModel
        ChatModel chatModel = new ChatModel();
        chatModel.setBaseUrl("http://localhost:8080");

        // Call the method
        ChatLanguageModel result = factory.createChatModel(chatModel);
        assertThat(result).isNotNull();
    }
}
