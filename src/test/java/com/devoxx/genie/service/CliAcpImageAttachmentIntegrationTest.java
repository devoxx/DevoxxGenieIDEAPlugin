package com.devoxx.genie.service;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.strategy.AbstractPromptExecutionStrategy;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Integration test for the CLI/ACP Runner image attachment flow, covering
 * <a href="https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1280">#1280</a>
 * (attached images silently dropped) and
 * <a href="https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1282">#1282</a>
 * (every follow-up prompt fails with "Expecting single text content" once an image was attached).
 * <p>
 * Unlike {@code AbstractPromptExecutionStrategyTest}, nothing in the prompt-assembly chain is
 * stubbed here: the real {@link FileListManager}, {@link MessageCreationService},
 * {@link ChatMemoryManager} and {@link ChatMemoryService} are wired together behind a mocked
 * {@link ApplicationManager}, so the test exercises the same collaboration the runners use —
 * image attach → multimodal {@link UserMessage} → chat memory → history replay on the next turn.
 * Only the IDE {@link Project}/{@link VirtualFile} and the unused {@link ThreadPoolManager} are mocks.
 * <p>
 * Note: this deliberately does NOT extend {@code AbstractLightPlatformTestCase}. The build runs
 * {@code useJUnitPlatform()} with only the Jupiter engine on the classpath, so JUnit 3/4 style
 * platform test cases are never discovered (see {@code PromptMessageFlowIntegrationTest}, which
 * silently does not execute).
 */
class CliAcpImageAttachmentIntegrationTest {

    private static final String PROJECT_HASH = "project-hash-1280";
    private static final String TAB_ID = "tab-1";
    private static final String OTHER_TAB_ID = "tab-2";
    private static final String IMAGE_PATH = "/Users/dev/Desktop/screenshot.png";

    /** 1x1 transparent PNG — the bytes the runner would have to base64-encode for a vision model. */
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    private static final String PNG_BASE64 = Base64.getEncoder().encodeToString(PNG_BYTES);

    private MockedStatic<ApplicationManager> applicationManagerMock;

    private Project project;
    private FileListManager fileListManager;
    private ChatMemoryService chatMemoryService;
    private ChatMemoryManager chatMemoryManager;
    private TestRunnerStrategy strategy;

    @BeforeEach
    void setUp() {
        project = mock(Project.class);
        when(project.getLocationHash()).thenReturn(PROJECT_HASH);
        when(project.getName()).thenReturn("TestProject");

        Application application = mock(Application.class);

        applicationManagerMock = mockStatic(ApplicationManager.class);
        applicationManagerMock.when(ApplicationManager::getApplication).thenReturn(application);

        // Real collaborators — the whole point of this test
        fileListManager = new FileListManager();
        chatMemoryService = new ChatMemoryService();
        MessageCreationService messageCreationService = new MessageCreationService();
        DevoxxGenieStateService stateService = new DevoxxGenieStateService();

        when(application.getService(FileListManager.class)).thenReturn(fileListManager);
        when(application.getService(ChatMemoryService.class)).thenReturn(chatMemoryService);
        when(application.getService(MessageCreationService.class)).thenReturn(messageCreationService);
        when(application.getService(DevoxxGenieStateService.class)).thenReturn(stateService);

        // ChatMemoryManager's constructor resolves ChatMemoryService, so build it after the stub
        chatMemoryManager = new ChatMemoryManager();
        when(application.getService(ChatMemoryManager.class)).thenReturn(chatMemoryManager);

        strategy = new TestRunnerStrategy(project, chatMemoryManager,
                mock(ThreadPoolManager.class), messageCreationService);

        // Seed both tabs with a system message so prepareMemory() skips system-prompt assembly
        // (skills/DEVOXXGENIE.md lookups), which is not what these issues are about.
        seedMemory(PROJECT_HASH + "-" + TAB_ID);
        seedMemory(PROJECT_HASH + "-" + OTHER_TAB_ID);
    }

    private void seedMemory(String memoryKey) {
        chatMemoryService.initializeByKey(memoryKey, 50);
        chatMemoryService.addMessageByKey(memoryKey, SystemMessage.from("You are a helpful coding assistant."));
    }

    @AfterEach
    void tearDown() {
        if (applicationManagerMock != null) {
            applicationManagerMock.close();
        }
    }

    // ---------------------------------------------------------------------
    // Issue #1280 — attached images must reach the agent
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("#1280: an image attached to the tab reaches the runner prompt as a readable file path")
    void imageAttachedToTab_isHandedToTheRunnerAsAFilePath() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);

        String prompt = sendPrompt(TAB_ID, "what is this image ?");

        assertThat(prompt)
                .as("CLI/ACP runners only transport text, so the agent must be told where the image lives")
                .contains("<attached_images>")
                .contains(IMAGE_PATH)
                .contains("</attached_images>");
        assertThat(prompt.indexOf("</attached_images>"))
                .as("the image block must precede the user prompt")
                .isLessThan(prompt.indexOf("what is this image ?"));
    }

    @Test
    @DisplayName("#1280: a prompt without attachments carries no image block")
    void noImageAttached_promptHasNoImageBlock() {
        String prompt = sendPrompt(TAB_ID, "hello");

        assertThat(prompt).doesNotContain("<attached_images>").endsWith("hello");
    }

    @Test
    @DisplayName("#1280: images are looked up per tab, not per project")
    void imageAttachedInOneTab_doesNotLeakIntoAnotherTab() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);

        String otherTabPrompt = sendPrompt(OTHER_TAB_ID, "unrelated question");

        assertThat(otherTabPrompt)
                .as("FileListManager keys images by projectHash-tabId; the strategy must use the same key")
                .doesNotContain("<attached_images>")
                .doesNotContain(IMAGE_PATH);
    }

    // ---------------------------------------------------------------------
    // Issue #1282 — follow-up prompts after an image turn must not fail
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("#1282: the image turn really is stored as a multimodal message (the crash precondition)")
    void imageTurn_isStoredInMemoryAsMultimodalUserMessage() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);

        sendPrompt(TAB_ID, "what is this image ?");

        UserMessage stored = lastUserMessage(PROJECT_HASH + "-" + TAB_ID);
        assertThat(stored.hasSingleText())
                .as("MessageCreationService.addImages() turns the prompt into TextContent + ImageContent")
                .isFalse();
        assertThat(stored.contents()).anyMatch(ImageContent.class::isInstance);
        assertThatCode(stored::singleText)
                .as("this is exactly the call that used to blow up on every follow-up prompt")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("#1282: the follow-up prompt after an image turn is built without failing")
    void followUpPromptAfterImageTurn_doesNotFail() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);
        ChatMessageContext imageTurn = newContext(TAB_ID, "what is this image ?");
        strategy.buildPrompt(imageTurn);
        agentReplies(imageTurn, "I don't see any image attached.");

        ChatMessageContext followUp = newContext(TAB_ID, "what i asked you ?");

        assertThatCode(() -> strategy.buildPrompt(followUp))
                .as("used to throw IllegalStateException: Expecting single text content, but got: ...")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("#1282: history replay keeps the text of the image turn and marks the image")
    void followUpPromptAfterImageTurn_replaysTextAndMarksTheImage() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);
        ChatMessageContext imageTurn = newContext(TAB_ID, "what is this image ?");
        strategy.buildPrompt(imageTurn);
        agentReplies(imageTurn, "I don't see any image attached.");

        String prompt = sendPrompt(TAB_ID, "what i asked you ?");

        assertThat(prompt)
                .contains("<conversation_history>")
                .contains("what is this image ?")
                .contains("[image attached]")
                .contains("[assistant]: I don't see any image attached.")
                .endsWith("what i asked you ?");
    }

    @Test
    @DisplayName("#1282: history replay never inlines the base64 image payload")
    void followUpPromptAfterImageTurn_doesNotLeakBase64Payload() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);
        ChatMessageContext imageTurn = newContext(TAB_ID, "what is this image ?");
        strategy.buildPrompt(imageTurn);
        agentReplies(imageTurn, "I don't see any image attached.");

        String prompt = sendPrompt(TAB_ID, "what i asked you ?");

        assertThat(prompt)
                .as("dumping base64 into a CLI argument would blow up the prompt size for no benefit")
                .doesNotContain(PNG_BASE64);
    }

    @Test
    @DisplayName("#1282 + #1280: the image path stays available on later turns, so the agent can re-read it")
    void followUpTurns_stillCarryTheImagePath() throws IOException {
        attachImage(TAB_ID, IMAGE_PATH);
        ChatMessageContext imageTurn = newContext(TAB_ID, "what is this image ?");
        strategy.buildPrompt(imageTurn);
        agentReplies(imageTurn, "I don't see any image attached.");

        String prompt = sendPrompt(TAB_ID, "what i asked you ?");

        // Attachments are only cleared when a new conversation starts (ConversationManager),
        // so the follow-up prompt repeats the path. That is what compensates for the history
        // marker itself not carrying the path (issue #1282 asked for "[image attached: /path]").
        assertThat(prompt).contains("<attached_images>").contains(IMAGE_PATH);
    }

    @Test
    @DisplayName("text-only conversations get no image marker in their history")
    void textOnlyConversation_hasNoImageMarker() {
        ChatMessageContext first = newContext(TAB_ID, "first question");
        strategy.buildPrompt(first);
        agentReplies(first, "first answer");

        String prompt = sendPrompt(TAB_ID, "second question");

        assertThat(prompt)
                .contains("first question")
                .contains("[assistant]: first answer")
                .doesNotContain("[image attached]");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void attachImage(String tabId, String path) throws IOException {
        VirtualFile imageFile = mock(VirtualFile.class);
        when(imageFile.getName()).thenReturn("screenshot.png");
        when(imageFile.getPath()).thenReturn(path);
        when(imageFile.contentsToByteArray()).thenReturn(PNG_BYTES);
        fileListManager.addFile(project, tabId, imageFile);
    }

    private @org.jetbrains.annotations.NotNull ChatMessageContext newContext(String tabId, String userPrompt) {
        return ChatMessageContext.builder()
                .project(project)
                .tabId(tabId)
                .userPrompt(userPrompt)
                .languageModel(LanguageModel.builder()
                        .provider(ModelProvider.CLIRunners)
                        .modelName("Claude")
                        .displayName("Claude")
                        .build())
                .build();
    }

    private String sendPrompt(String tabId, String userPrompt) {
        return strategy.buildPrompt(newContext(tabId, userPrompt));
    }

    private void agentReplies(ChatMessageContext context, String reply) {
        context.setAiMessage(AiMessage.from(reply));
        chatMemoryManager.addAiResponse(context);
    }

    private UserMessage lastUserMessage(String memoryKey) {
        List<ChatMessage> messages = chatMemoryService.getMessagesByKey(memoryKey);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage userMessage) {
                return userMessage;
            }
        }
        throw new AssertionError("No UserMessage found in memory for key " + memoryKey);
    }

    /** Stand-in for {@code CliPromptStrategy}/{@code AcpPromptStrategy}: same prompt assembly, no process. */
    private static class TestRunnerStrategy extends AbstractPromptExecutionStrategy {

        TestRunnerStrategy(Project project,
                           ChatMemoryManager chatMemoryManager,
                           ThreadPoolManager threadPoolManager,
                           MessageCreationService messageCreationService) {
            super(project, chatMemoryManager, threadPoolManager, messageCreationService);
        }

        String buildPrompt(ChatMessageContext context) {
            return buildPromptWithHistory(context);
        }

        @Override
        protected void executeStrategySpecific(ChatMessageContext context,
                                               PromptOutputPanel panel,
                                               PromptTask<PromptResult> resultTask) {
            // not exercised: this test stops at prompt assembly
        }

        @Override
        protected String getStrategyName() {
            return "Test Runner";
        }

        @Override
        public void cancel() {
            // no-op
        }
    }
}
