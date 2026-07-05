package com.devoxx.genie.ui.compose.viewmodel

import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.ui.compose.model.ActivityStatus
import com.devoxx.genie.ui.compose.model.ConversationState
import com.devoxx.genie.ui.compose.model.TerminalState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.chat.StreamingChatModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.ResourceBundle

class ConversationViewModelTest {

    @Test
    fun `clearConversation during restore keeps chat state and never flashes welcome`() {
        val viewModel = ConversationViewModel()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hello")
                .aiMessage(AiMessage.from("hi"))
                .build()
        )

        // Simulates ConversationHistoryManager.restoreConversation → clearWithoutWelcome
        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()

        val midRestore = viewModel.state
        assertThat(midRestore).isInstanceOf(ConversationState.Chat::class.java)
        assertThat((midRestore as ConversationState.Chat).messages).isEmpty()
        assertThat(midRestore.isRestoringConversation).isTrue()

        // Restored messages arrive, then the flag is cleared
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("restored-1")
                .userPrompt("old prompt")
                .aiMessage(AiMessage.from("old answer"))
                .build()
        )
        viewModel.setRestoringConversation(false)

        val afterRestore = viewModel.state as ConversationState.Chat
        assertThat(afterRestore.messages).hasSize(1)
        assertThat(afterRestore.isRestoringConversation).isFalse()
    }

    @Test
    fun `restored messages keep the restoring flag set in chat state`() {
        val viewModel = ConversationViewModel()

        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("restored-1")
                .userPrompt("old prompt")
                .aiMessage(AiMessage.from("old answer"))
                .build()
        )

        // addChatMessage must not reset the flag — the restore window is only closed
        // by setRestoringConversation(false), a live user prompt, or an explicit welcome.
        val midRestore = viewModel.state as ConversationState.Chat
        assertThat(midRestore.messages).hasSize(1)
        assertThat(midRestore.isRestoringConversation).isTrue()
    }

    @Test
    fun `clearConversation outside restore returns to welcome`() {
        val viewModel = ConversationViewModel()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hello")
                .aiMessage(AiMessage.from("hi"))
                .build()
        )

        viewModel.clearConversation()

        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

    @Test
    fun `live user prompt ends the restore window so a later clear shows welcome`() {
        val viewModel = ConversationViewModel()

        // First prompt of a new conversation goes through clearWithoutWelcome
        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder().id("msg-1").userPrompt("hi").build()
        )

        assertThat(viewModel.state).isInstanceOf(ConversationState.Chat::class.java)

        // New Conversation afterwards must show the welcome screen again
        viewModel.clearConversation()
        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

    @Test
    fun `explicit welcome request clears a stale restore flag`() {
        val viewModel = ConversationViewModel()
        viewModel.setRestoringConversation(true)

        viewModel.loadWelcomeContent(ResourceBundle.getBundle("messages"))

        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
        // Flag was reset — a subsequent clear must also show welcome, not an empty chat
        viewModel.clearConversation()
        assertThat(viewModel.state).isInstanceOf(ConversationState.Welcome::class.java)
    }

    @Test
    fun `theme change updates theme flag without dropping visible chat messages`() {
        val viewModel = ConversationViewModel()
        val initialTheme = viewModel.isDarkTheme

        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("Show me an example")
                .aiMessage(AiMessage.from("```java\nclass Demo {}\n```"))
                .executionTimeMs(1250)
                .build()
        )

        val beforeThemeChange = viewModel.state as ConversationState.Chat
        assertThat(beforeThemeChange.messages).hasSize(1)

        viewModel.onThemeChanged(!initialTheme)

        assertThat(viewModel.isDarkTheme).isEqualTo(!initialTheme)
        val afterThemeChange = viewModel.state as ConversationState.Chat
        assertThat(afterThemeChange.messages).containsExactlyElementsOf(beforeThemeChange.messages)
    }

    @Test
    fun `when show tool activity is off tool entries are still tracked but flagged for hiding`() {
        // Tool entries must always be tracked — the live status line in the AI bubble is
        // derived from them. The setting only flips the per-message rendering flag.
        val viewModel = ConversationViewModel(showToolActivityInChat = { false })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("list_files", "src/main/java"))
        viewModel.onActivityMessage(intermediateResponse("I'll explore the project structure first."))

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.showToolActivity).isFalse()
        assertThat(msg.activityEntries).hasSize(2)

        val toolEntry = msg.activityEntries.first { it.toolName == "list_files" }
        assertThat(toolEntry.isToolActivity).isTrue()
        assertThat(toolEntry.status).isEqualTo(ActivityStatus.RUNNING)

        val reasoning = msg.activityEntries.first { it.toolName == null }
        assertThat(reasoning.isToolActivity).isFalse()
        assertThat(reasoning.content).isEqualTo("I'll explore the project structure first.")
    }

    @Test
    fun `when show tool activity is on both tool calls and agent reasoning are shown`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("list_files", "src/main/java"))
        viewModel.onActivityMessage(intermediateResponse("I'll explore the project structure first."))

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.showToolActivity).isTrue()
        assertThat(msg.activityEntries).hasSize(2)
        assertThat(msg.activityEntries.map { it.toolName }).contains("list_files")
    }

    @Test
    fun `tool response resolves into the request row instead of adding a duplicate entry`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"git status"}"""))
        viewModel.onActivityMessage(toolResponse("run_command", "On branch master"))

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].toolName).isEqualTo("run_command")
        assertThat(entries[0].arguments).contains("git status")
        assertThat(entries[0].status).isEqualTo(ActivityStatus.SUCCESS)
        assertThat(entries[0].result).isEqualTo("On branch master")
    }

    @Test
    fun `hide loading indicator also clears the streaming flag`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hi")
                .streamingChatModel(mock(StreamingChatModel::class.java))
                .build()
        )

        val before = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(before.isStreaming).isTrue()

        // Complete, error and stop all funnel through hideLoadingIndicator — clearing the
        // streaming flag here keeps one lifecycle for both in-flight indicators.
        viewModel.hideLoadingIndicator("msg-1")

        val after = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(after.isStreaming).isFalse()
        assertThat(after.isLoadingIndicatorVisible).isFalse()
    }

    @Test
    fun `ide scale is read at construction and refreshed on appearance settings change`() {
        var scale = 1f
        val viewModel = ConversationViewModel(readIdeScale = { scale })
        assertThat(viewModel.ideScale).isEqualTo(1f)

        // user zooms the IDE in (Appearance → Zoom IDE)
        scale = 1.5f
        viewModel.onAppearanceSettingsChanged()
        assertThat(viewModel.ideScale).isEqualTo(1.5f)

        // and back out again
        scale = 0.9f
        viewModel.onAppearanceSettingsChanged()
        assertThat(viewModel.ideScale).isEqualTo(0.9f)
    }

    @Test
    fun `ide scale defaults to 1 when the platform is unavailable`() {
        // default provider runs without an IntelliJ Application in unit tests
        val viewModel = ConversationViewModel()
        assertThat(viewModel.ideScale).isEqualTo(1f)
    }

    // --- Terminal states (task-234) ---

    @Test
    fun `stop mid-stream sets STOPPED, keeps partial text and blocks further partials`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(
            ChatMessageContext.builder()
                .id("msg-1")
                .userPrompt("hi")
                .streamingChatModel(mock(StreamingChatModel::class.java))
                .build()
        )
        viewModel.updateAiMessageContent(
            ChatMessageContext.builder().id("msg-1").userPrompt("hi")
                .aiMessage(AiMessage.from("partial answer")).build()
        )

        viewModel.setTerminalState("msg-1", TerminalState.STOPPED)

        // A straggling token must not alter the kept partial text or revive streaming
        viewModel.updateAiMessageContent(
            ChatMessageContext.builder().id("msg-1").userPrompt("hi")
                .aiMessage(AiMessage.from("partial answer plus straggler")).build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.STOPPED)
        assertThat(msg.aiResponseMarkdown).isEqualTo("partial answer")
        assertThat(msg.isStreaming).isFalse()
        assertThat(msg.isLoadingIndicatorVisible).isFalse()
    }

    @Test
    fun `error path sets ERROR with the human-readable message`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.setTerminalState("msg-1", TerminalState.ERROR, errorText = "Provider timed out")

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.ERROR)
        assertThat(msg.errorText).isEqualTo("Provider timed out")
        assertThat(msg.isLoadingIndicatorVisible).isFalse()
    }

    @Test
    fun `terminal states are final - a stopped message cannot be re-marked`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.setTerminalState("msg-1", TerminalState.STOPPED)
        viewModel.setTerminalState("msg-1", TerminalState.ERROR, errorText = "late failure")
        viewModel.setTerminalState("msg-1", TerminalState.COMPLETED)

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.STOPPED)
        assertThat(msg.errorText).isNull()
    }

    @Test
    fun `LOOP_LIMIT activity event sets the terminal state with the configured limit`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { false })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(
            ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.LOOP_LIMIT)
                .callNumber(26)
                .maxCalls(25)
                .build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.LOOP_LIMIT)
        assertThat(msg.loopLimitMaxCalls).isEqualTo(25)
    }

    @Test
    fun `sub-agent LOOP_LIMIT does not terminate the top-level message`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { false })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(
            ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.LOOP_LIMIT)
                .subAgentId("explorer-1")
                .maxCalls(10)
                .build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.COMPLETED)
    }

    @Test
    fun `LOOP_LIMIT still accepts the agent wrap-up text streamed after the notice`() {
        val viewModel = ConversationViewModel()
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.setTerminalState("msg-1", TerminalState.LOOP_LIMIT, maxCalls = 25)
        viewModel.updateAiMessageContent(
            ChatMessageContext.builder().id("msg-1").userPrompt("hi")
                .aiMessage(AiMessage.from("best answer with info gathered so far")).build()
        )

        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.terminalState).isEqualTo(TerminalState.LOOP_LIMIT)
        assertThat(msg.aiResponseMarkdown).isEqualTo("best answer with info gathered so far")
    }

    @Test
    fun `retry invokes the submission entry point with the original prompt exactly once`() {
        val retries = mutableListOf<String>()
        val viewModel = ConversationViewModel(onRetryPrompt = { retries.add(it) })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("explain this class").build())
        viewModel.setTerminalState("msg-1", TerminalState.ERROR, errorText = "boom")

        viewModel.onRetryClicked("msg-1")
        viewModel.onRetryClicked("msg-1") // double click — must be a no-op

        assertThat(retries).containsExactly("explain this class")
        val msg = (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }
        assertThat(msg.retryAttempted).isTrue()
    }

    @Test
    fun `retry is ignored for messages that are not in ERROR state`() {
        val retries = mutableListOf<String>()
        val viewModel = ConversationViewModel(onRetryPrompt = { retries.add(it) })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onRetryClicked("msg-1") // still running
        viewModel.setTerminalState("msg-1", TerminalState.STOPPED)
        viewModel.onRetryClicked("msg-1") // stopped, not errored

        assertThat(retries).isEmpty()
    }

    @Test
    fun `messages restored from history default to COMPLETED and render no marker`() {
        val viewModel = ConversationViewModel()
        viewModel.setRestoringConversation(true)
        viewModel.clearConversation()
        viewModel.addChatMessage(
            ChatMessageContext.builder()
                .id("restored-1")
                .userPrompt("old prompt")
                .aiMessage(AiMessage.from("old answer"))
                .build()
        )
        viewModel.setRestoringConversation(false)

        val msg = (viewModel.state as ConversationState.Chat).messages.first()
        assertThat(msg.terminalState).isEqualTo(TerminalState.COMPLETED)
        assertThat(msg.errorText).isNull()
        assertThat(msg.retryAttempted).isFalse()
    }

    // --- Activity timeline pairing (task-233) ---

    @Test
    fun `tool request opens a RUNNING row that resolves to SUCCESS by callNumber`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("read_file", """{"path":"A.java"}""", callNumber = 1))
        viewModel.onActivityMessage(toolRequest("read_file", """{"path":"B.java"}""", callNumber = 2))

        assertThat(activeMessageEntries(viewModel)).allMatch { it.status == ActivityStatus.RUNNING }

        // Resolve the SECOND call — matching is by callNumber, not just tool name
        viewModel.onActivityMessage(toolResponse("read_file", "class B {}", callNumber = 2))

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(2)
        assertThat(entries[0].status).isEqualTo(ActivityStatus.RUNNING)
        assertThat(entries[1].status).isEqualTo(ActivityStatus.SUCCESS)
        assertThat(entries[1].result).isEqualTo("class B {}")
    }

    @Test
    fun `tool request records a start timestamp for the elapsed-time ticker`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        val before = System.currentTimeMillis()
        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"gradle test"}"""))

        val entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.RUNNING)
        assertThat(entry.startedAt).isBetween(before, System.currentTimeMillis())
    }

    @Test
    fun `hide loading indicator resolves dangling RUNNING rows so the ticker stops`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        // Tool whose response never arrives (e.g. run stopped mid-call)
        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"gradle test"}"""))
        assertThat(activeMessageEntries(viewModel).single().status).isEqualTo(ActivityStatus.RUNNING)

        viewModel.hideLoadingIndicator("msg-1")

        assertThat(activeMessageEntries(viewModel).single().status).isEqualTo(ActivityStatus.INFO)
    }

    @Test
    fun `stop terminal state resolves dangling RUNNING rows so the ticker stops`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"gradle test"}"""))
        viewModel.setTerminalState("msg-1", TerminalState.STOPPED)

        assertThat(activeMessageEntries(viewModel).single().status).isEqualTo(ActivityStatus.INFO)
    }

    @Test
    fun `tool error resolves the open row to ERROR`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("run_command", """{"command":"rm x"}"""))
        viewModel.onActivityMessage(
            agentMessage(AgentType.TOOL_ERROR) { it.toolName("run_command").result("Error: boom").callNumber(1) }
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].status).isEqualTo(ActivityStatus.ERROR)
        assertThat(entries[0].result).isEqualTo("Error: boom")
    }

    @Test
    fun `unmatched tool response is tolerated and does not crash or add entries`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        // Response without any request (out-of-order / lost event)
        viewModel.onActivityMessage(toolResponse("read_file", "data", callNumber = 7))

        assertThat(activeMessageEntries(viewModel)).isEmpty()

        // A request arriving after the stray response still opens a normal RUNNING row
        viewModel.onActivityMessage(toolRequest("read_file", """{"path":"A.java"}""", callNumber = 7))
        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].status).isEqualTo(ActivityStatus.RUNNING)
    }

    @Test
    fun `approval lifecycle - requested pauses the row, granted resumes it, response completes it`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("write_file", """{"path":"A.java"}"""))
        viewModel.onActivityMessage(agentMessage(AgentType.APPROVAL_REQUESTED) { it.toolName("write_file") })

        var entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.PENDING_APPROVAL)
        assertThat(entry.content).isEqualTo("Waiting for your approval…")

        viewModel.onActivityMessage(agentMessage(AgentType.APPROVAL_GRANTED) { it.toolName("write_file") })
        entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.RUNNING)

        viewModel.onActivityMessage(toolResponse("write_file", "written", callNumber = 1))
        entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.SUCCESS)
    }

    @Test
    fun `approval denied renders as ERROR and the follow-up tool response cannot flip it back`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("write_file", """{"path":"A.java"}"""))
        viewModel.onActivityMessage(agentMessage(AgentType.APPROVAL_REQUESTED) { it.toolName("write_file") })
        viewModel.onActivityMessage(agentMessage(AgentType.APPROVAL_DENIED) { it.toolName("write_file") })

        var entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.ERROR)
        assertThat(entry.content).contains("Denied")

        // The loop tracker still publishes a TOOL_RESPONSE carrying the denial string —
        // a resolved ERROR row is final and must not become a green check.
        viewModel.onActivityMessage(toolResponse("write_file", "Tool execution was denied by the user.", callNumber = 1))
        entry = activeMessageEntries(viewModel).single()
        assertThat(entry.status).isEqualTo(ActivityStatus.ERROR)
    }

    @Test
    fun `sub-agent events nest as children under the open parallel_explore row`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("parallel_explore", """{"queries":["a","b"]}"""))
        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_STARTED) {
                it.toolName("parallel_explore").arguments("Launching 2 sub-agents")
            }
        )
        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_STARTED) { it.toolName("sub-agent-1").arguments("find usages").callNumber(1) }
        )
        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_STARTED) { it.toolName("sub-agent-2").arguments("check config").callNumber(2) }
        )
        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_COMPLETED) { it.toolName("sub-agent-1").result("found 3 usages").callNumber(1) }
        )
        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_ERROR) { it.toolName("sub-agent-2").result("timed out").callNumber(2) }
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1) // children are nested, not flat
        val parent = entries.single()
        assertThat(parent.toolName).isEqualTo("parallel_explore")
        assertThat(parent.content).isEqualTo("Launching 2 sub-agents")
        assertThat(parent.children).hasSize(2)
        assertThat(parent.children[0].toolName).isEqualTo("sub-agent-1")
        assertThat(parent.children[0].status).isEqualTo(ActivityStatus.SUCCESS)
        assertThat(parent.children[0].result).isEqualTo("found 3 usages")
        assertThat(parent.children[1].toolName).isEqualTo("sub-agent-2")
        assertThat(parent.children[1].status).isEqualTo(ActivityStatus.ERROR)
    }

    @Test
    fun `sub-agent event without an open parallel_explore parent degrades to a top-level row`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(
            agentMessage(AgentType.SUB_AGENT_STARTED) { it.toolName("sub-agent-1").arguments("orphan query").callNumber(1) }
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].toolName).isEqualTo("sub-agent-1")
        assertThat(entries[0].status).isEqualTo(ActivityStatus.RUNNING)
    }

    @Test
    fun `tool calls inside sub-agents pair independently from the main loop via subAgentId`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        // Main loop call 1 and a sub-agent's internal call 1 share toolName + callNumber
        viewModel.onActivityMessage(toolRequest("read_file", """{"path":"A.java"}""", callNumber = 1))
        viewModel.onActivityMessage(
            agentMessage(AgentType.TOOL_REQUEST) {
                it.toolName("read_file").arguments("""{"path":"B.java"}""").callNumber(1).subAgentId("sub-agent-1:p:m")
            }
        )

        // The sub-agent's response must resolve ITS row, not the main loop's
        viewModel.onActivityMessage(
            agentMessage(AgentType.TOOL_RESPONSE) {
                it.toolName("read_file").result("class B {}").callNumber(1).subAgentId("sub-agent-1:p:m")
            }
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(2)
        assertThat(entries.first { it.subAgentId == null }.status).isEqualTo(ActivityStatus.RUNNING)
        assertThat(entries.first { it.subAgentId != null }.status).isEqualTo(ActivityStatus.SUCCESS)
    }

    @Test
    fun `MCP messages get a terminal INFO status immediately - no eternal spinner`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(
            ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .content("Tool call: github_search")
                .build()
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].status).isEqualTo(ActivityStatus.INFO)
        assertThat(entries[0].isToolActivity).isTrue()
    }

    @Test
    fun `tool arguments and results are truncated before entering Compose state`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        val hugeLine = "x".repeat(2000)
        val manyLines = (1..50).joinToString("\n") { "line $it" }
        viewModel.onActivityMessage(toolRequest("run_command", hugeLine))
        viewModel.onActivityMessage(toolResponse("run_command", manyLines, callNumber = 1))

        val entry = activeMessageEntries(viewModel).single()
        assertThat(entry.arguments!!.length).isLessThanOrEqualTo(501) // 499 chars + ellipsis
        assertThat(entry.result!!.lines()).hasSize(11) // 10 lines + "(40 more lines)"
        assertThat(entry.result).endsWith("(40 more lines)")
    }

    @Test
    fun `system prompt entry is labelled SYSTEM PROMPT and keeps the full untruncated prompt`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        // A long prompt (> 10 lines) would be capped with "(N more lines)" for normal tool
        // output; the system prompt entry must show every line so users see exactly what the
        // model received.
        val fullPrompt = (1..30).joinToString("\n") { "prompt line $it" }
        viewModel.onActivityMessage(agentMessage(AgentType.SYSTEM_PROMPT) { it.result(fullPrompt) })

        val entry = activeMessageEntries(viewModel).single()
        assertThat(entry.source).isEqualTo("SYSTEM PROMPT")
        assertThat(entry.result).isEqualTo(fullPrompt)
        assertThat(entry.result).doesNotContain("more lines")
    }

    @Test
    fun `delegate_task sub-agent events nest as keyed children transitioning running to done`() {
        // TASK-246: every delegate_task event shares toolName "delegate_task"; children are
        // keyed by subAgentId (the agent name) so the completion event resolves the started
        // row in place instead of appending a second one.
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("delegate_task", """{"tasks":[...]}"""))
        viewModel.onActivityMessage(subAgentEvent(AgentType.SUB_AGENT_STARTED, "reviewer", "Ollama · qwen3"))
        viewModel.onActivityMessage(subAgentEvent(AgentType.SUB_AGENT_STARTED, "implementer", null))
        viewModel.onActivityMessage(
            subAgentEvent(AgentType.SUB_AGENT_COMPLETED, "reviewer", "Ollama · qwen3", "LGTM, one nit.")
        )

        val entries = activeMessageEntries(viewModel)
        assertThat(entries).hasSize(1)
        val parent = entries.single()
        assertThat(parent.toolName).isEqualTo("delegate_task")
        assertThat(parent.children).hasSize(2)

        val reviewer = parent.children.first { it.subAgentId == "reviewer" }
        assertThat(reviewer.status).isEqualTo(ActivityStatus.SUCCESS)
        assertThat(reviewer.result).isEqualTo("LGTM, one nit.")
        assertThat(reviewer.agentLabel).isEqualTo("Ollama · qwen3")

        val implementer = parent.children.first { it.subAgentId == "implementer" }
        assertThat(implementer.status).isEqualTo(ActivityStatus.RUNNING)
    }

    @Test
    fun `delegate_task error event resolves the child row to error`() {
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(toolRequest("delegate_task", "{}"))
        viewModel.onActivityMessage(subAgentEvent(AgentType.SUB_AGENT_STARTED, "documentalist", null))
        viewModel.onActivityMessage(
            subAgentEvent(AgentType.SUB_AGENT_ERROR, "documentalist", null, "timed out after 120s")
        )

        val child = activeMessageEntries(viewModel).single().children.single()
        assertThat(child.subAgentId).isEqualTo("documentalist")
        assertThat(child.status).isEqualTo(ActivityStatus.ERROR)
        assertThat(child.result).contains("timed out")
    }

    @Test
    fun `sub-agent events without a tracker parent synthesize one block that nests and resolves`() {
        // The normal case when "Show tool activity in chat" is on but agent debug logs
        // are off: no TOOL_REQUEST(delegate_task) event exists. All SUB_AGENT_* events
        // must still form ONE parent block with keyed, resolving children — and the
        // synthesized parent's status must follow its children.
        val viewModel = ConversationViewModel(showToolActivityInChat = { true })
        viewModel.addUserPromptMessage(ChatMessageContext.builder().id("msg-1").userPrompt("hi").build())

        viewModel.onActivityMessage(subAgentEvent(AgentType.SUB_AGENT_STARTED, "reviewer", "Ollama · qwen3"))
        viewModel.onActivityMessage(subAgentEvent(AgentType.SUB_AGENT_STARTED, "implementer", null))

        val running = activeMessageEntries(viewModel).single()
        assertThat(running.toolName).isEqualTo("delegate_task")
        assertThat(running.callNumber).isEqualTo(0) // synthesized marker
        assertThat(running.status).isEqualTo(ActivityStatus.RUNNING)
        assertThat(running.children).hasSize(2)
        assertThat(running.children.first { it.subAgentId == "reviewer" }.agentLabel)
            .isEqualTo("Ollama · qwen3")

        viewModel.onActivityMessage(
            subAgentEvent(AgentType.SUB_AGENT_COMPLETED, "reviewer", "Ollama · qwen3", "LGTM")
        )
        assertThat(activeMessageEntries(viewModel).single().status).isEqualTo(ActivityStatus.RUNNING)

        viewModel.onActivityMessage(
            subAgentEvent(AgentType.SUB_AGENT_ERROR, "implementer", null, "timed out")
        )
        val done = activeMessageEntries(viewModel).single()
        assertThat(done.children).hasSize(2)
        assertThat(done.children.first { it.subAgentId == "reviewer" }.status)
            .isEqualTo(ActivityStatus.SUCCESS)
        assertThat(done.children.first { it.subAgentId == "implementer" }.status)
            .isEqualTo(ActivityStatus.ERROR)
        // Parent derived from children: no child running, one failed → ERROR (and no
        // longer "open", so a NEXT delegation would synthesize a fresh block)
        assertThat(done.status).isEqualTo(ActivityStatus.ERROR)
    }

    private fun subAgentEvent(
        type: AgentType,
        agentName: String,
        modelLabel: String?,
        result: String? = null,
    ): ActivityMessage =
        agentMessage(type) {
            it.toolName("delegate_task").subAgentId(agentName).agentModelLabel(modelLabel).result(result)
        }

    private fun activeMessageEntries(viewModel: ConversationViewModel) =
        (viewModel.state as ConversationState.Chat).messages.first { it.id == "msg-1" }.activityEntries

    private fun agentMessage(
        type: AgentType,
        customize: (ActivityMessage.ActivityMessageBuilder) -> ActivityMessage.ActivityMessageBuilder = { it },
    ): ActivityMessage =
        customize(
            ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(type)
        ).build()

    private fun toolRequest(toolName: String, arguments: String, callNumber: Int = 1): ActivityMessage =
        agentMessage(AgentType.TOOL_REQUEST) {
            it.toolName(toolName).arguments(arguments).callNumber(callNumber).maxCalls(50)
        }

    private fun toolResponse(toolName: String, result: String, callNumber: Int = 1): ActivityMessage =
        agentMessage(AgentType.TOOL_RESPONSE) {
            it.toolName(toolName).result(result).callNumber(callNumber).maxCalls(50)
        }

    private fun intermediateResponse(text: String): ActivityMessage =
        agentMessage(AgentType.INTERMEDIATE_RESPONSE) { it.result(text) }
}
