package com.devoxx.genie.chatmodel.ollama;

import com.devoxx.genie.model.ChatModel;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<PropertiesComponent> mocked = Mockito.mockStatic(PropertiesComponent.class)) {

            PropertiesComponent mockComponent = mock(PropertiesComponent.class);
            when(PropertiesComponent.getInstance()).thenReturn(mockComponent);
            when(mockComponent.getValue("OLLAMA_MODEL_URL")).thenReturn("http://example.com");

            // Instance of the class containing the method to be tested
            OllamaChatModelFactory factory = new OllamaChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.name = "ollama";

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }
}
