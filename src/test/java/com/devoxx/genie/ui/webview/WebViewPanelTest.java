package com.devoxx.genie.ui.webview;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.ConversationPanel;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Test class for WebView-based panels.
 * (Most methods are ignored as they require a UI context to run)
 */
public class WebViewPanelTest {

    @Test
    @Ignore("This test is for manual testing only")
    public void testConversationPanel() {
        // Create a mock project
        Project project = Mockito.mock(Project.class);
        
        // Create a conversation panel
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
        ConversationPanel panel = new ConversationPanel(resourceBundle);
        
        // Show welcome content
        panel.showWelcome();
        
        try {
            Thread.sleep(1000); // Wait for rendering
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Create a sample chat message context
        ChatMessageContext context1 = ChatMessageContext.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .userPrompt("Hello, how are you?")
                .aiMessage(AiMessage.aiMessage("I'm doing well, thank you for asking! How can I help you today?"))
                .executionTimeMs(500)
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName("gpt-4")
                        .build())
                .build();
        
        // Add a message to the panel
        panel.addChatMessage(context1);
        
        try {
            Thread.sleep(1000); // Wait for rendering
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Add another message with code blocks
        ChatMessageContext context2 = ChatMessageContext.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .userPrompt("Can you show me a Java Hello World example?")
                .aiMessage(AiMessage.aiMessage("""
                        Sure, here's a simple Java Hello World example:
                        
                        ```java
                        public class HelloWorld {
                            public static void main(String[] args) {
                                System.out.println("Hello, World!");
                            }
                        }
                        ```
                        
                        To compile and run this code, you can use the following commands:
                        
                        ```bash
                        javac HelloWorld.java
                        java HelloWorld
                        ```
                        
                        This will output: `Hello, World!`
                        """))
                .executionTimeMs(800)
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.OpenAI)
                        .modelName("gpt-4")
                        .build())
                .build();
        
        panel.addChatMessage(context2);
        
        // Wait for rendering
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
