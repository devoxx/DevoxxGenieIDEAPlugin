package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractPromptExecutionStrategyTest {

    @Mock
    private Project mockProject;
    
    @Mock
    private ChatMemoryManager mockChatMemoryManager;
    
    @Mock
    private ThreadPoolManager mockThreadPoolManager;
    
    @Mock
    private MessageCreationService mockMessageCreationService;
    
    @Mock
    private ChatMessageContext mockChatMessageContext;
    
    private TestPromptExecutionStrategy testStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockStaticDependencies();
        
        // Use the constructor that bypasses ApplicationManager.getApplication()
        testStrategy = new TestPromptExecutionStrategy(
            mockProject, 
            mockChatMemoryManager, 
            mockThreadPoolManager,
            mockMessageCreationService
        );
    }
    
    private void mockStaticDependencies() {
        // Mock ApplicationManager first
        try (MockedStatic<ApplicationManager> applicationManagerMockedStatic = mockStatic(ApplicationManager.class)) {
            // Create a mock Application
            com.intellij.openapi.application.Application mockApplication = mock(com.intellij.openapi.application.Application.class);
            applicationManagerMockedStatic.when(ApplicationManager::getApplication).thenReturn(mockApplication);
            
            // Setup services in the application
            when(mockApplication.getService(ChatMemoryManager.class)).thenReturn(mockChatMemoryManager);
            when(mockApplication.getService(ThreadPoolManager.class)).thenReturn(mockThreadPoolManager);
            when(mockApplication.getService(MessageCreationService.class)).thenReturn(mockMessageCreationService);
            
            // Mock static getInstance methods
            try (var chatMemoryManagerStaticMock = mockStatic(ChatMemoryManager.class);
                 var threadPoolManagerStaticMock = mockStatic(ThreadPoolManager.class);
                 var messageCreationServiceStaticMock = mockStatic(MessageCreationService.class)) {
                
                chatMemoryManagerStaticMock.when(ChatMemoryManager::getInstance)
                        .thenReturn(mockChatMemoryManager);
                
                threadPoolManagerStaticMock.when(ThreadPoolManager::getInstance)
                        .thenReturn(mockThreadPoolManager);
                
                messageCreationServiceStaticMock.when(MessageCreationService::getInstance)
                        .thenReturn(mockMessageCreationService);
            }
        }
    }

    /**
     * Test implementation of the abstract class
     */
    private static class TestPromptExecutionStrategy extends AbstractPromptExecutionStrategy {
        
        public TestPromptExecutionStrategy(Project project) {
            super(project);
        }
        
        @Override
        protected void executeStrategySpecific(ChatMessageContext context, 
                                             com.devoxx.genie.ui.panel.PromptOutputPanel panel, 
                                             com.devoxx.genie.service.prompt.threading.PromptTask<com.devoxx.genie.service.prompt.result.PromptResult> resultTask) {
            // No implementation needed for test
        }
        
        @Override
        protected String getStrategyName() {
            return "Test Strategy";
        }
        
        @Override
        public void cancel() {
            // No implementation needed for test
        }
        
        /**
         * Create a constructor that doesn't rely on ApplicationManager for tests
         */
        public TestPromptExecutionStrategy(Project project, 
                                          ChatMemoryManager chatMemoryManager,
                                          ThreadPoolManager threadPoolManager) {
            super(project, chatMemoryManager, threadPoolManager);
        }
        
        /**
         * Create a constructor that also passes MessageCreationService for tests
         */
        public TestPromptExecutionStrategy(Project project, 
                                          ChatMemoryManager chatMemoryManager,
                                          ThreadPoolManager threadPoolManager,
                                          MessageCreationService messageCreationService) {
            super(project, chatMemoryManager, threadPoolManager, messageCreationService);
        }
        
        /**
         * Expose protected method for testing
         */
        public String testBuildPromptWithHistory(ChatMessageContext context) {
            return buildPromptWithHistory(context);
        }
    }
    
    @Test
    void buildPromptWithHistory_ShouldIncludeFilesContext_WhenFilesAreAttached() {
        // Given: A context with filesContext set (simulating attached files)
        String userPrompt = "Explain this code";
        String filesContext = "File: /path/to/Test.java\npublic class Test {}\n";
        
        ChatMessageContext realContext = ChatMessageContext.builder()
            .project(mockProject)
            .userPrompt(userPrompt)
            .filesContext(filesContext)
            .languageModel(LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .build())
            .build();
        
        // Mock empty message history
        List<ChatMessage> emptyMessages = new ArrayList<>();
        emptyMessages.add(UserMessage.from(userPrompt)); // The current message
        when(mockChatMemoryManager.getMessages(mockProject)).thenReturn(emptyMessages);
        
        // When: Building the prompt
        String result = testStrategy.testBuildPromptWithHistory(realContext);
        
        // Then: The prompt SHOULD include the files context
        // NOTE: This test currently FAILS because of the bug - filesContext is not included
        assertThat(result)
            .as("Prompt should include attached files context for ACP/CLI runners")
            .contains("<attached_files>")
            .contains(filesContext)
            .contains("</attached_files>")
            .contains(userPrompt);
    }
    
    @Test
    void buildPromptWithHistory_ShouldNotIncludeAttachedFilesTags_WhenNoFilesAttached() {
        // Given: A context WITHOUT filesContext (no attached files)
        String userPrompt = "Hello, how are you?";
        
        ChatMessageContext realContext = ChatMessageContext.builder()
            .project(mockProject)
            .userPrompt(userPrompt)
            .filesContext(null) // No files attached
            .languageModel(LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .build())
            .build();
        
        // Mock empty message history
        List<ChatMessage> emptyMessages = new ArrayList<>();
        emptyMessages.add(UserMessage.from(userPrompt));
        when(mockChatMemoryManager.getMessages(mockProject)).thenReturn(emptyMessages);
        
        // When: Building the prompt
        String result = testStrategy.testBuildPromptWithHistory(realContext);
        
        // Then: The prompt should NOT include attached_files tags
        assertThat(result)
            .as("Prompt should not include attached_files tags when no files are attached")
            .doesNotContain("<attached_files>")
            .contains(userPrompt);
    }
    
    @Test
    void buildPromptWithHistory_ShouldIncludeFilesContextWithConversationHistory() {
        // Given: A context with files AND conversation history
        String userPrompt = "Explain this code";
        String filesContext = "File: /path/to/Test.java\npublic class Test {}\n";
        
        ChatMessageContext realContext = ChatMessageContext.builder()
            .project(mockProject)
            .userPrompt(userPrompt)
            .filesContext(filesContext)
            .languageModel(LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("gpt-4")
                .displayName("GPT-4")
                .build())
            .build();
        
        // Mock conversation history (previous exchanges + current message)
        List<ChatMessage> messagesWithHistory = new ArrayList<>();
        messagesWithHistory.add(UserMessage.from("Previous question"));
        messagesWithHistory.add(AiMessage.from("Previous answer"));
        messagesWithHistory.add(UserMessage.from(userPrompt)); // Current message
        when(mockChatMemoryManager.getMessages(mockProject)).thenReturn(messagesWithHistory);
        
        // When: Building the prompt
        String result = testStrategy.testBuildPromptWithHistory(realContext);
        
        // Then: The prompt should include both history AND files context
        assertThat(result)
            .as("Prompt should include both conversation history and files context")
            .contains("<conversation_history>")
            .contains("[user]: Previous question")
            .contains("[assistant]: Previous answer")
            .contains("</conversation_history>")
            .contains("<attached_files>")
            .contains(filesContext)
            .contains("</attached_files>")
            .contains(userPrompt);
    }
}
