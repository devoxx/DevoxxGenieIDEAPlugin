package com.devoxx.genie.service.prompt.memory;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The full-project-context snapshot added via the toolbar is delivered to the model
 * exactly once — via the system prompt — instead of being re-embedded in every user
 * message. The old behavior re-sent (and re-stored in chat memory) the whole project
 * text on every prompt, so a long conversation accumulated one stale project copy per
 * turn and burned the model's context window quadratically.
 *
 * <p>Like {@code CliAcpImageAttachmentIntegrationTest}, nothing in the memory/message
 * chain is stubbed: the real {@link ChatMemoryService}, {@link ChatMemoryManager} and
 * {@link MessageCreationService} are wired behind a mocked {@link ApplicationManager}
 * with a real {@link DevoxxGenieStateService} (all feature flags off by default).
 */
class ProjectContextSystemPromptTest {

    private static final String PROJECT_HASH = "project-hash";
    private static final String TAB_ID = "tab-1";
    private static final String MEMORY_KEY = PROJECT_HASH + "-" + TAB_ID;
    private static final String MARKER = "UNIQUE-PROJECT-MARKER";
    private static final String PROJECT_TEXT = "Project Context:\n" + MARKER + " src/Main.java: public class Main {}";

    private MockedStatic<ApplicationManager> applicationManagerMock;

    private Project project;
    private ChatMemoryService chatMemoryService;
    private ChatMemoryManager chatMemoryManager;
    private MessageCreationService messageCreationService;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        when(project.getLocationHash()).thenReturn(PROJECT_HASH);
        when(project.getBasePath()).thenReturn("/tmp/no-such-project");
        when(project.getName()).thenReturn("TestProject");

        Application application = mock(Application.class);
        applicationManagerMock = mockStatic(ApplicationManager.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);

        // Real collaborators — the whole point of the test
        FileListManager fileListManager = mock(FileListManager.class);
        when(fileListManager.getImageFiles(any(Project.class), anyString())).thenReturn(List.of());
        chatMemoryService = new ChatMemoryService();
        messageCreationService = new MessageCreationService();
        DevoxxGenieStateService stateService = new DevoxxGenieStateService();

        when(application.getService(FileListManager.class)).thenReturn(fileListManager);
        when(application.getService(ChatMemoryService.class)).thenReturn(chatMemoryService);
        when(application.getService(MessageCreationService.class)).thenReturn(messageCreationService);
        when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        // ChatMemoryManager's constructor resolves ChatMemoryService, so build it after the stub
        chatMemoryManager = new ChatMemoryManager();
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) {
            applicationManagerMock.close();
        }
    }

    // ---------------------------------------------------------------------
    // The core regression: exactly one copy of the project text in memory
    // ---------------------------------------------------------------------

    @Test
    void projectSnapshotAppearsExactlyOnceInMemoryAfterManyPrompts() {
        runTurn("first question", "first answer", true);
        runTurn("second question", "second answer", true);
        runTurn("third question", "third answer", true);

        List<ChatMessage> messages = chatMemoryService.getMessagesByKey(MEMORY_KEY);

        long copies = messages.stream()
                .filter(message -> messageText(message).contains(MARKER))
                .count();
        assertThat(copies)
                .as("the project snapshot must live in exactly one message, not one per turn")
                .isEqualTo(1);
        assertThat(messages.get(0))
                .as("that one copy is the system message, kept at index 0")
                .isInstanceOf(SystemMessage.class);
        messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(message -> ((UserMessage) message).singleText())
                .forEach(text -> assertThat(text)
                        .as("no user message may carry the project snapshot")
                        .doesNotContain(MARKER));
    }

    @Test
    void prepareMemoryAddsProjectSectionToSystemPromptWhenAddedMidConversation() {
        runTurn("first question", "first answer", false);
        SystemMessage before = (SystemMessage) chatMemoryService.getMessagesByKey(MEMORY_KEY).get(0);
        assertThat(before.text())
                .doesNotContain("<ProjectFilesContext>");

        runTurn("second question", "second answer", true);

        List<ChatMessage> messages = chatMemoryService.getMessagesByKey(MEMORY_KEY);
        assertThat(messages).hasSize(5);
        SystemMessage updated = (SystemMessage) messages.get(0);
        assertThat(updated.text())
                .as("the updated system message must stay at index 0 and carry the snapshot")
                .contains("<ProjectFilesContext>")
                .contains(MARKER);
        assertThat(messageText(messages.get(1)))
                .as("the earlier user turn must be untouched")
                .contains("first question");
        assertThat(messageText(messages.get(2))).contains("first answer");
    }

    @Test
    void prepareMemoryRemovesProjectSectionWhenProjectContextRemoved() {
        runTurn("first question", "first answer", true);
        SystemMessage withProject = (SystemMessage) chatMemoryService.getMessagesByKey(MEMORY_KEY).get(0);
        assertThat(withProject.text()).contains(MARKER);

        runTurn("second question", "second answer", false);

        List<ChatMessage> messages = chatMemoryService.getMessagesByKey(MEMORY_KEY);
        SystemMessage after = (SystemMessage) messages.get(0);
        assertThat(after.text())
                .doesNotContain("<ProjectFilesContext>")
                .doesNotContain(MARKER);
    }

    @Test
    void prepareMemoryInstallsMissingSystemMessageForRestoredConversation() {
        // Simulates restoreConversationByKey: flat user/AI pairs and no system message
        // (the bug that made restored conversations run without persona/tool instructions).
        chatMemoryService.initializeByKey(MEMORY_KEY, 50);
        chatMemoryService.addMessageByKey(MEMORY_KEY, UserMessage.from("old question"));
        chatMemoryService.addMessageByKey(MEMORY_KEY, AiMessage.from("old answer"));

        ChatMessageContext context = newContext("next question");
        context.setProjectContext(PROJECT_TEXT);
        chatMemoryManager.prepareMemory(context);

        // prepareMemory installs the system message on top of the two restored
        // messages — the new user message is added by the caller, after prepareMemory.
        List<ChatMessage> messages = chatMemoryService.getMessagesByKey(MEMORY_KEY);
        assertThat(messages).hasSize(3);
        SystemMessage installed = (SystemMessage) messages.get(0);
        assertThat(installed.text())
                .as("the installed system message (at index 0) picks up the active project context at once")
                .contains("<ProjectFilesContext>")
                .contains(MARKER);
        assertThat(messages.get(1)).isEqualTo(UserMessage.from("old question"));
        assertThat(messages.get(2)).isEqualTo(AiMessage.from("old answer"));
    }

    // ---------------------------------------------------------------------
    // System prompt assembly
    // ---------------------------------------------------------------------

    @Test
    void systemPromptContainsProjectSectionOnlyWhenProjectContextActive() {
        assertThat(ChatMemoryManager.buildAugmentedSystemPrompt(project, null))
                .doesNotContain("<ProjectFilesContext>");
        assertThat(ChatMemoryManager.buildAugmentedSystemPrompt(project, null, "   "))
                .doesNotContain("<ProjectFilesContext>");

        String withProject = ChatMemoryManager.buildAugmentedSystemPrompt(project, null, PROJECT_TEXT);
        assertThat(withProject)
                .contains("<ProjectFilesContext>")
                .contains(MARKER)
                .contains("</ProjectFilesContext>");
    }

    // ---------------------------------------------------------------------
    // OpenAI o1 models have no system message — the snapshot travels with
    // every user message instead (mirroring how o1 receives the base prompt)
    // ---------------------------------------------------------------------

    @Test
    void o1UserMessageCarriesProjectSectionBecauseO1HasNoSystemMessage() {
        ChatMessageContext context = newContext("what does the project do?");
        context.setLanguageModel(LanguageModel.builder()
                .provider(ModelProvider.OpenAI)
                .modelName("o1-preview")
                .displayName("OpenAI o1")
                .build());
        context.setProjectContext(PROJECT_TEXT);

        messageCreationService.addUserMessageToContext(context);

        assertThat(context.getUserMessage().singleText())
                .contains("<SystemPrompt>")
                .contains("<ProjectFilesContext>")
                .contains(MARKER);
    }

    @Test
    void nonO1UserMessageDoesNotCarryProjectSection() {
        ChatMessageContext context = newContext("what does the project do?");
        context.setProjectContext(PROJECT_TEXT);

        messageCreationService.addUserMessageToContext(context);

        assertThat(context.getUserMessage().singleText())
                .as("for model-based providers the snapshot belongs to the system prompt only")
                .doesNotContain(MARKER)
                .doesNotContain("<ProjectFilesContext>");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** One full turn: prepare memory, build + store the user message, store the AI reply. */
    private void runTurn(String userPrompt, String aiReply, boolean projectContextActive) {
        ChatMessageContext context = newContext(userPrompt);
        if (projectContextActive) {
            context.setProjectContext(PROJECT_TEXT);
        }
        chatMemoryManager.prepareMemory(context);
        messageCreationService.addUserMessageToContext(context);
        chatMemoryManager.addUserMessage(context);
        context.setAiMessage(AiMessage.from(aiReply));
        chatMemoryManager.addAiResponse(context);
    }

    private ChatMessageContext newContext(String userPrompt) {
        return ChatMessageContext.builder()
                .project(project)
                .tabId(TAB_ID)
                .userPrompt(userPrompt)
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.Anthropic)
                        .modelName("claude-3-5-sonnet")
                        .displayName("Anthropic Claude")
                        .build())
                .build();
    }

    private static String messageText(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.hasSingleText() ? userMessage.singleText() : String.valueOf(userMessage);
        }
        if (message instanceof ToolExecutionResultMessage toolResult) {
            return toolResult.text();
        }
        return String.valueOf(message);
    }
}
