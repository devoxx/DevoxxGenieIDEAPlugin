package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.UserMessage;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.ResourceBundle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatPromptExecutorIT extends AbstractLightPlatformTestCase {

    ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
    PromptInputArea promptInputArea;
    PromptOutputPanel promptOutputPanel;

    ChatPromptExecutor chatPromptExecutor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Application application = mock(Application.class);
        MessageBus messageBus = mock(MessageBus.class);
        when(application.getMessageBus()).thenReturn(messageBus);

        // Mockito.doNothing().when(messageBus).syncPublisher(Mockito.any()); // If needed
        MessageBusConnection messageBusConnection = mock(MessageBusConnection.class);
        when(messageBus.connect()).thenReturn(messageBusConnection);

        ExtensionsArea extensionsArea = mock(ExtensionsArea.class);
        when(application.getExtensionArea()).thenReturn(extensionsArea);
        when(extensionsArea.hasExtensionPoint(Mockito.any(ExtensionPointName.class))).thenReturn(true);

        // Use reflection or a dedicated method to set the application mock
        setApplication(application);

        promptInputArea = new PromptInputArea(getProject(), resourceBundle);
        promptOutputPanel = new PromptOutputPanel(getProject(), resourceBundle);
        chatPromptExecutor = new ChatPromptExecutor(promptInputArea);
    }

    @Test
    public void testGetCommandFromPrompt_validInput() {

        var chatMessageContext = ChatMessageContext
                .builder()
                .userMessage(UserMessage.from("/review"))
                .build();

        Optional<String> command = chatPromptExecutor.getCommandFromPrompt(chatMessageContext, promptOutputPanel);

        assertEquals("add", command.get());
    }

    private void setApplication(Application application) throws Exception {
        java.lang.reflect.Field applicationField = ApplicationManager.class.getDeclaredField("ourApplication");
        applicationField.setAccessible(true);
        applicationField.set(null, application);
    }

//
//    @Test
//    public void testGetCommandFromPrompt_emptyInput() {
//        // Simulate empty user input (just pressing Enter)
//        String input = "\n";
//        InputStream in = new ByteArrayInputStream(input.getBytes());
//        System.setIn(in);
//
//        YourClass yourClassInstance = new YourClass();
//        String command = yourClassInstance.getCommandFromPrompt();
//
//        assertEquals("", command); // Or assertNull(command) if that's the expected behavior for empty input
//    }
//
//    @Test
//    public void testGetCommandFromPrompt_multipleSpaces() {
//        // Simulate user input with multiple spaces
//        String input = "   delete   \n";
//        InputStream in = new ByteArrayInputStream(input.getBytes());
//        System.setIn(in);
//
//        YourClass yourClassInstance = new YourClass();
//        String command = yourClassInstance.getCommandFromPrompt();
//
//        assertEquals("delete", command); // Assuming the method trims whitespace
//    }
//
//    @Test
//    public void testGetCommandFromPrompt_mixedCase() {
//        // Simulate user input with mixed case
//        String input = "UpDaTe\n";
//        InputStream in = new ByteArrayInputStream(input.getBytes());
//        System.setIn(in);
//
//        YourClass yourClassInstance = new YourClass();
//        String command = yourClassInstance.getCommandFromPrompt();
//
//        assertEquals("update", command); // Assuming the method converts to lowercase
//    }
}
