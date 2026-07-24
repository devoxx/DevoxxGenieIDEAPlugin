# Streaming Agent Activity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make streaming conversations retain the agent tool-activity timeline already available in non-streaming conversations.

**Architecture:** Activity events cross from background tool execution into the Compose conversation UI. Marshal them onto the IntelliJ EDT at the conversation-panel boundary so they serialize with streaming partial/final response updates. A small dispatcher isolates the queueing behavior for unit tests; the view model remains responsible for formatting, pairing, and RAW-event exclusion.

**Tech Stack:** IntelliJ Platform message bus/EDT, Java 17, Kotlin, Compose Desktop, JUnit 5, AssertJ.

---

### Task 1: Serialize conversation activity delivery

**Files:**
- Create: `src/main/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcher.java`
- Create: `src/test/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcherTest.java`
- Modify: `src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java:165-173`

- [ ] **Step 1: Write the failing dispatcher test**

```java
@Test
void dispatch_defersActivityHandlingUntilTheUiQueueRuns() {
    List<Runnable> queued = new ArrayList<>();
    ActivityMessage message = ActivityMessage.builder().source(ActivitySource.AGENT).build();
    AtomicReference<ActivityMessage> delivered = new AtomicReference<>();

    ActivityMessageDispatcher dispatcher = new ActivityMessageDispatcher(queued::add);
    dispatcher.dispatch(message, delivered::set);

    assertThat(delivered).hasValue(null);
    assertThat(queued).hasSize(1);
    queued.getFirst().run();
    assertThat(delivered).hasValueSameInstanceAs(message);
}
```

- [ ] **Step 2: Run the test and confirm it fails because the dispatcher does not exist**

Run: `./gradlew test --tests com.devoxx.genie.ui.panel.conversation.ActivityMessageDispatcherTest`

- [ ] **Step 3: Add the minimal dispatcher and use it from `ConversationPanel`**

```java
final class ActivityMessageDispatcher {
    void dispatch(ActivityMessage message, Consumer<ActivityMessage> consumer) {
        ApplicationManager.getApplication().invokeLater(() -> consumer.accept(message));
    }
}
```

The panel keeps its project-hash filter, then delegates `viewController::onActivityMessage` through this dispatcher.

- [ ] **Step 4: Run the dispatcher test and confirm it passes**

Run: `./gradlew test --tests com.devoxx.genie.ui.panel.conversation.ActivityMessageDispatcherTest`

### Task 2: Guard the streaming lifecycle against timeline loss

**Files:**
- Modify: `src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt`

- [ ] **Step 1: Write a failing lifecycle test**

```kotlin
@Test
fun `streamed response update preserves an open agent tool entry`() {
    val viewModel = ConversationViewModel(showToolActivityInChat = { true })
    viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("time").build())
    viewModel.onActivityMessage(toolRequest("run_command", "{\\\"command\\\":\\\"date\\\"}"))
    viewModel.updateAiMessageContent(ChatMessageContext.builder().id("msg-1").aiMessage(AiMessage.from("answer")).build())
    viewModel.onActivityMessage(toolResponse("run_command", "Fri Jul 24", callNumber = 1))

    assertThat(activeMessageEntries(viewModel).single().status).isEqualTo(ActivityStatus.SUCCESS)
}
```

- [ ] **Step 2: Run the focused Kotlin test and confirm it passes after Task 1’s serialized delivery behavior is in place**

Run: `./gradlew test --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest`

- [ ] **Step 3: Run focused regression verification**

Run: `./gradlew test --tests com.devoxx.genie.ui.panel.conversation.ActivityMessageDispatcherTest --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest`

Expected: both test classes pass.

### Task 3: Validate the task outcome

**Files:**
- Modify: `backlog/tasks/task-254 - Show-agent-tool-activity-during-streaming-responses.md` (through Backlog MCP only)

- [ ] **Step 1: Run the project’s relevant test suite**

Run: `./gradlew test --tests com.devoxx.genie.ui.panel.conversation.ActivityMessageDispatcherTest --tests com.devoxx.genie.ui.compose.viewmodel.ConversationViewModelTest`

- [ ] **Step 2: Inspect the diff**

Run: `git diff --check && git diff -- src/main/java/com/devoxx/genie/ui/panel/conversation/ConversationPanel.java src/main/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcher.java src/test/java/com/devoxx/genie/ui/panel/conversation/ActivityMessageDispatcherTest.java src/test/kotlin/com/devoxx/genie/ui/compose/viewmodel/ConversationViewModelTest.kt`

- [ ] **Step 3: Update Backlog acceptance criteria and notes once verification evidence is available**

Use `backlog.task_edit` to check each satisfied acceptance criterion and record the exact test command/result.
