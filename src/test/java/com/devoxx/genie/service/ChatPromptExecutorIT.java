package com.devoxx.genie.service;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatPromptExecutorIT extends AbstractLightPlatformTestCase {

    ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
    PromptInputArea promptInputArea;
    PromptOutputPanel promptOutputPanel;

    ChatPromptExecutor chatPromptExecutor;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ApplicationEx applicationEx = mock(ApplicationEx.class);
        MessageBus messageBus = mock(MessageBus.class);
        when(applicationEx.getMessageBus()).thenReturn(messageBus);

        MessageBusConnection messageBusConnection = mock(MessageBusConnection.class);
        when(messageBus.connect()).thenReturn(messageBusConnection);

        ExtensionsArea extensionsArea = mock(ExtensionsArea.class);
        when(applicationEx.getExtensionArea()).thenReturn(extensionsArea);
        when(extensionsArea.hasExtensionPoint(Mockito.any(ExtensionPointName.class))).thenReturn(true);

        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        when(applicationEx.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
        when(stateService.getSubmitShortcutWindows()).thenReturn("shift");
        when(stateService.getSubmitShortcutLinux()).thenReturn("shift");
        when(stateService.getSubmitShortcutMac()).thenReturn("shift");

        MessageCreationService messageCreationService = mock(MessageCreationService.class);
        when(applicationEx.getService(MessageCreationService.class)).thenReturn(messageCreationService);

        PromptExecutionService promptExecutionService = mock(PromptExecutionService.class);
        when(applicationEx.getService(PromptExecutionService.class)).thenReturn(promptExecutionService);

        setApplication(applicationEx);

        promptInputArea = new PromptInputArea(getProject(), resourceBundle);
        promptOutputPanel = new PromptOutputPanel(getProject(), resourceBundle);
        chatPromptExecutor = new ChatPromptExecutor(getProject(), promptInputArea);
    }

    private void setApplication(ApplicationEx applicationEx) throws Exception {
        java.lang.reflect.Field applicationField = ApplicationManager.class.getDeclaredField("ourApplication");
        applicationField.setAccessible(true);
        applicationField.set(null, applicationEx);
    }

    @Test
    void test_GetCommandFromPrompt_validInput() {
        String command = "/review";
        var chatMessageContext = ChatMessageContext
                .builder()
                .userMessage(UserMessage.from(command))
                .build();

        chatMessageContext.setUserPrompt(command);
        Optional<String> aCommand = chatPromptExecutor.getCommandFromPrompt(chatMessageContext, promptOutputPanel);

        assertTrue(aCommand.isPresent());
        assertEquals(command, aCommand.get());
    }
}
