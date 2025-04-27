package com.devoxx.genie.ui.webview.template;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.webview.WebServer;
import dev.langchain4j.data.message.AiMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Test class for HTML template generators.
 */
public class TemplateTest {

    @Test
    @Ignore("This test is for manual inspection only")
    public void testTemplateGeneration() {
        // Mock WebServer
        WebServer webServer = Mockito.mock(WebServer.class);
        Mockito.when(webServer.getPrismCssUrl()).thenReturn("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css");
        Mockito.when(webServer.getPrismJsUrl()).thenReturn("https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js");
        
        // Test ConversationTemplate
        ConversationTemplate conversationTemplate = new ConversationTemplate(webServer);
        String conversationHtml = conversationTemplate.generate();
        System.out.println("Conversation Template HTML:");
        System.out.println(conversationHtml);
        Assert.assertTrue(conversationHtml.contains("<!DOCTYPE html>"));
        Assert.assertTrue(conversationHtml.contains("conversation-container"));
        
        // Test WelcomeTemplate
        ResourceBundle resourceBundle = Mockito.mock(ResourceBundle.class);
        Mockito.when(resourceBundle.getString("welcome.title")).thenReturn("Welcome to DevoxxGenie");
        Mockito.when(resourceBundle.getString("welcome.description")).thenReturn("This is a description");
        Mockito.when(resourceBundle.getString("welcome.instructions")).thenReturn("These are instructions");
        Mockito.when(resourceBundle.getString("welcome.tip")).thenReturn("This is a tip");
        Mockito.when(resourceBundle.getString("welcome.enjoy")).thenReturn("Enjoy!");
        
        WelcomeTemplate welcomeTemplate = new WelcomeTemplate(webServer, resourceBundle);
        String welcomeHtml = welcomeTemplate.generate();
        System.out.println("\nWelcome Template HTML:");
        System.out.println(welcomeHtml);
        Assert.assertTrue(welcomeHtml.contains("Welcome to DevoxxGenie"));
        Assert.assertTrue(welcomeHtml.contains("Features"));
        
        // Test ChatMessageTemplate
        ChatMessageContext chatMessageContext = ChatMessageContext.builder()
                .id(UUID.randomUUID().toString())
                .userPrompt("Hello, how are you?")
                .aiMessage(AiMessage.aiMessage("I'm doing well! Here's some code:\n```java\nSystem.out.println(\"Hello World\");\n```"))
                .executionTimeMs(500)
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName("gpt-4")
                        .build())
                .build();
        
        ChatMessageTemplate chatMessageTemplate = new ChatMessageTemplate(webServer, chatMessageContext);
        String messageHtml = chatMessageTemplate.generate();
        System.out.println("\nChat Message Template HTML:");
        System.out.println(messageHtml);
        Assert.assertTrue(messageHtml.contains("user-message"));
        Assert.assertTrue(messageHtml.contains("assistant-message"));
        Assert.assertTrue(messageHtml.contains("Hello, how are you?"));
        Assert.assertTrue(messageHtml.contains("language-java"));
    }
}
