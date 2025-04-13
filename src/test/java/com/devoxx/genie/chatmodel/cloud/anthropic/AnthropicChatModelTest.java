package com.devoxx.genie.chatmodel.cloud.anthropic;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "AWS_SECRET_ACCESS_KEY", matches = ".+")
public class AnthropicChatModelTest {

    @Test
    void testTellAJoke() {

        String apiKeyValue = Dotenv.load().get("ANTHROPIC_API_KEY");

        AnthropicChatModel model = AnthropicChatModel.builder()
                .apiKey(apiKeyValue)
                .modelName(AnthropicChatModelName.CLAUDE_3_5_HAIKU_20241022)
                .maxRetries(1)
                .build();

        String response = model.chat("Tell a joke");
        System.out.println(response);
    }
}
