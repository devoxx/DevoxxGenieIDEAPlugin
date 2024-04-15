package com.devoxx.genie.chatmodel.gpt4all;

import com.devoxx.genie.model.ChatModel;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GPT4AllChatModelFactoryTest {

    @Test
    void testCreateChatModel() {
        try (MockedStatic<PropertiesComponent> mocked = Mockito.mockStatic(PropertiesComponent.class)) {

            PropertiesComponent mockComponent = mock(PropertiesComponent.class);
            when(PropertiesComponent.getInstance()).thenReturn(mockComponent);
            when(mockComponent.getValue("GPT4ALL_MODEL_URL")).thenReturn("http://example.com");

            // Instance of the class containing the method to be tested
            GPT4AllChatModelFactory factory = new GPT4AllChatModelFactory();

            // Create a dummy ChatModel
            ChatModel chatModel = new ChatModel();
            chatModel.name = "gtp4all";

            // Call the method
            ChatLanguageModel result = factory.createChatModel(chatModel);
            assertThat(result).isNotNull();
        }
    }
}
