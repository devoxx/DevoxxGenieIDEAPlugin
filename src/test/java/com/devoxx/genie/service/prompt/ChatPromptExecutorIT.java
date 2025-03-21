package com.devoxx.genie.service.prompt;

import com.devoxx.genie.chatmodel.AbstractLightPlatformTestCase;

import static org.mockito.Mockito.mock;

class ChatPromptExecutorIT extends AbstractLightPlatformTestCase {

//    ResourceBundle resourceBundle = ResourceBundle.getBundle("messages");
//    PromptInputArea promptInputArea;
//    PromptOutputPanel promptOutputPanel;
//
//    ChatPromptExecutor chatPromptExecutor;
//
//    @Override
//    @BeforeEach
//    public void setUp() throws Exception {
//        super.setUp();
//        ApplicationEx applicationEx = mock(ApplicationEx.class);
//        MessageBus messageBus = mock(MessageBus.class);
//        when(applicationEx.getMessageBus()).thenReturn(messageBus);
//
//        MessageBusConnection messageBusConnection = mock(MessageBusConnection.class);
//        when(messageBus.connect()).thenReturn(messageBusConnection);
//
//        ExtensionsArea extensionsArea = mock(ExtensionsArea.class);
//        when(applicationEx.getExtensionArea()).thenReturn(extensionsArea);
//        when(extensionsArea.hasExtensionPoint(Mockito.any(ExtensionPointName.class))).thenReturn(true);
//
//        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
//        when(applicationEx.getService(DevoxxGenieStateService.class)).thenReturn(stateService);
//        when(stateService.getSubmitShortcutWindows()).thenReturn("shift");
//        when(stateService.getSubmitShortcutLinux()).thenReturn("shift");
//        when(stateService.getSubmitShortcutMac()).thenReturn("shift");
//
//        MessageCreationService messageCreationService = mock(MessageCreationService.class);
//        when(applicationEx.getService(MessageCreationService.class)).thenReturn(messageCreationService);
//
//        NonStreamingPromptExecutionService promptExecutionService = mock(NonStreamingPromptExecutionService.class);
//        when(applicationEx.getService(NonStreamingPromptExecutionService.class)).thenReturn(promptExecutionService);
//
//        setApplication(applicationEx);
//
//        promptInputArea = new PromptInputArea(getProject(), resourceBundle);
//        promptOutputPanel = new PromptOutputPanel(getProject(), resourceBundle);
//        chatPromptExecutor = new ChatPromptExecutor(getProject(), promptInputArea);
//    }
//
//    private void setApplication(ApplicationEx applicationEx) throws Exception {
//        java.lang.reflect.Field applicationField = ApplicationManager.class.getDeclaredField("ourApplication");
//        applicationField.setAccessible(true);
//        applicationField.set(null, applicationEx);
//    }
//
//    @Test
//    void test_GetCommandFromPrompt_validInput() {
//        String command = "/review";
//        var chatMessageContext = ChatMessageContext
//                .builder()
//                .userMessage(UserMessage.from(command))
//                .build();
//
//        chatMessageContext.setUserPrompt(command);
//        Optional<String> aCommand = chatPromptExecutor.getCommandFromPrompt(chatMessageContext, promptOutputPanel);
//
//        assertTrue(aCommand.isPresent());
//        assertEquals(command, aCommand.get());
//    }
}
