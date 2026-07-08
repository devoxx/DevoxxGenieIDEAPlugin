package com.devoxx.genie.ui.compose.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.devoxx.genie.model.LanguageModel
import com.devoxx.genie.model.activity.ActivityMessage
import com.devoxx.genie.model.activity.ActivitySource
import com.devoxx.genie.model.agent.AgentType
import com.devoxx.genie.model.request.ChatMessageContext
import com.devoxx.genie.service.blog.BlogFeedService
import com.devoxx.genie.service.blog.BlogPost
import com.devoxx.genie.service.prompt.response.streaming.ThinkingResponseFormatter
import com.devoxx.genie.service.skill.SkillRegistry
import com.devoxx.genie.ui.compose.model.*
import com.devoxx.genie.ui.settings.DevoxxGenieStateService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.ResourceBundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ConversationViewModel(
    /**
     * Project the welcome screen belongs to. Used to look up the project-scoped
     * {@link SkillRegistry} so the welcome page can list any currently-enabled skills.
     * May be {@code null} in tests / non-project contexts — the skills list then
     * stays empty and the section is hidden.
     */
    private val project: Project? = null,
    /**
     * Whether tool-call activity (tool requests/responses, MCP messages) should be shown
     * in the chat output. Pure agent reasoning text is always shown regardless. Defaults to
     * the "Show tool activity in chat output" setting; injectable for tests.
     */
    private val showToolActivityInChat: () -> Boolean = {
        try {
            DevoxxGenieStateService.getInstance().showToolActivityInChat == true
        } catch (_: Exception) {
            false
        }
    },
    /**
     * Supplies the current IDE zoom factor (Appearance → Zoom IDE). Compose density does
     * not track this factor, so font sizes are multiplied by it explicitly. Defaults to
     * the platform's UISettingsUtils; injectable for tests running without an Application.
     */
    private val readIdeScale: () -> Float = {
        try {
            com.intellij.ide.ui.UISettingsUtils.getInstance().currentIdeScale
        } catch (_: Throwable) {
            1f
        }
    },
    /**
     * Invoked when the user clicks Retry on an inline error card. Receives the original
     * user prompt; the host is expected to route it through the normal prompt submission
     * entry point (PromptExecutionController.handlePromptSubmission flow). Injectable for
     * tests; defaults to a no-op.
     */
    private val onRetryPrompt: (String) -> Unit = {},
) {

    var state: ConversationState by mutableStateOf(
        ConversationState.Welcome(
            resourceBundle = ResourceBundle.getBundle("messages"),
            customPrompts = loadCustomPrompts(),
            skills = loadSkills(),
            blogPosts = loadBlogPosts(),
            hasMcpServers = loadHasMcpServers(),
        )
    )
        private set

    var isDarkTheme: Boolean by mutableStateOf(
        // ThemeDetector's static init needs the IntelliJ Application; guard it like the
        // other platform accesses in this class so plain unit tests can construct the model.
        try {
            com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme()
        } catch (_: Throwable) {
            false
        }
    )
        private set

    var customFontSize: Int by mutableStateOf(readFontSize())
        private set

    var customCodeFontSize: Int by mutableStateOf(readCodeFontSize())
        private set

    var ideScale: Float by mutableStateOf(readIdeScale())
        private set

    fun onThemeChanged(dark: Boolean) {
        isDarkTheme = dark
    }

    fun onAppearanceSettingsChanged() {
        customFontSize = readFontSize()
        customCodeFontSize = readCodeFontSize()
        ideScale = readIdeScale()
    }

    private fun readFontSize(): Int {
        return try {
            val state = DevoxxGenieStateService.getInstance()
            if (state.useCustomFontSize == true) state.customFontSize ?: 13 else 13
        } catch (_: Exception) { 13 }
    }

    private fun readCodeFontSize(): Int {
        return try {
            val state = DevoxxGenieStateService.getInstance()
            if (state.useCustomCodeFontSize == true) state.customCodeFontSize ?: 12 else 12
        } catch (_: Exception) { 12 }
    }

    private var activeMessageId: String? = null
    private val activityDeactivated = AtomicBoolean(false)
    private val activityGeneration = AtomicInteger(0)

    /**
     * True while a conversation restore (or a clear-without-welcome) is in progress.
     * While set, [clearConversation] resets to an *empty chat* instead of the welcome
     * screen, so the Welcome screen can never flash mid-restore — important now that
     * Welcome ↔ Chat switches crossfade and a transient Welcome state would be visibly
     * animated. The flag is released by [setRestoringConversation], by a live user
     * prompt, or by an explicit [loadWelcomeContent] request.
     */
    private val restoringConversation = AtomicBoolean(false)

    fun loadWelcomeContent(resourceBundle: ResourceBundle) {
        // An explicit welcome request (New Conversation / clear) ends any restore
        // window. Restoration itself is fully synchronous with Compose, so this can
        // never run mid-restore — mid-restore clears go through clearConversation,
        // which is guarded by the flag above.
        restoringConversation.set(false)
        state = ConversationState.Welcome(
            resourceBundle = resourceBundle,
            customPrompts = loadCustomPrompts(),
            skills = loadSkills(),
            blogPosts = loadBlogPosts(),
            hasMcpServers = loadHasMcpServers(),
        )
        refreshBlogPostsAsync()
        refreshSkillsAsync()
    }

    fun updateCustomPrompts(resourceBundle: ResourceBundle) {
        val current = state
        if (current is ConversationState.Welcome) {
            state = current.copy(
                customPrompts = loadCustomPrompts(),
                skills = loadSkills(),
                hasMcpServers = loadHasMcpServers(),
            )
        }
    }

    fun addUserPromptMessage(context: ChatMessageContext) {
        // A live user prompt means any restore / clear-without-welcome is over
        restoringConversation.set(false)

        // Deactivate previous activity handlers
        deactivateActivityHandlers()

        val newMessage = MessageUiModel(
            id = context.id,
            userPrompt = context.userPrompt ?: "",
            isLoadingIndicatorVisible = true,
            isStreaming = context.streamingChatModel != null,
            modelName = formatModelDisplayName(context.languageModel),
            showToolActivity = showToolActivityInChat(),
        )

        val currentState = state
        val messages = when (currentState) {
            is ConversationState.Chat -> currentState.messages + newMessage
            is ConversationState.Welcome -> listOf(newMessage)
        }
        state = ConversationState.Chat(messages = messages)

        // Re-activate with the new message ID
        activityDeactivated.set(false)
        activityGeneration.incrementAndGet()
        activeMessageId = context.id
    }

    fun updateAiMessageContent(context: ChatMessageContext) {
        val aiText = context.aiMessage?.text() ?: return
        updateMessage(context.id) { msg ->
            // A stopped or errored message is final: straggling tokens arriving after the
            // terminal state was set must not alter the (partial) text or make the message
            // look completed. LOOP_LIMIT still accepts updates — the LLM legitimately
            // streams its wrap-up answer after the limit notice was raised.
            if (msg.terminalState == TerminalState.STOPPED || msg.terminalState == TerminalState.ERROR) {
                return@updateMessage msg
            }
            msg.copy(
                aiResponseMarkdown = ThinkingResponseFormatter.extractAnswer(aiText),
                thinkingMarkdown = ThinkingResponseFormatter.extractThinking(aiText),
                executionTimeMs = context.executionTimeMs,
                tokenUsage = buildTokenUsage(context) ?: msg.tokenUsage,
                modelName = formatModelDisplayName(context.languageModel).ifEmpty { msg.modelName },
            )
        }
    }

    fun addChatMessage(context: ChatMessageContext) {
        val aiText = context.aiMessage?.text() ?: ""
        val message = MessageUiModel(
            id = context.id,
            userPrompt = context.userPrompt ?: "",
            aiResponseMarkdown = ThinkingResponseFormatter.extractAnswer(aiText),
            thinkingMarkdown = ThinkingResponseFormatter.extractThinking(aiText),
            modelName = formatModelDisplayName(context.languageModel),
            executionTimeMs = context.executionTimeMs,
            tokenUsage = buildTokenUsage(context) ?: TokenUsageInfo(),
        )

        val currentState = state
        // copy() preserves isRestoringConversation — restored messages arrive through
        // this method and must not end the restore window (see clearConversation).
        state = when (currentState) {
            is ConversationState.Chat -> currentState.copy(messages = currentState.messages + message)
            is ConversationState.Welcome -> ConversationState.Chat(messages = listOf(message))
        }
    }

    fun addSystemMessage(markdownContent: String) {
        val message = MessageUiModel(
            id = "system_${System.currentTimeMillis()}",
            userPrompt = "",
            aiResponseMarkdown = markdownContent,
        )

        val currentState = state
        // copy() preserves isRestoringConversation while a restore is in progress.
        state = when (currentState) {
            is ConversationState.Chat -> currentState.copy(messages = currentState.messages + message)
            is ConversationState.Welcome -> ConversationState.Chat(messages = listOf(message))
        }
    }

    fun addFileReferences(context: ChatMessageContext, files: List<VirtualFile>) {
        val fileModels = files.map { file ->
            FileReferenceUiModel(
                filePath = file.path,
                fileName = file.name,
            )
        }
        updateMessage(context.id) { msg ->
            msg.copy(fileReferences = msg.fileReferences + fileModels)
        }
    }

    fun onActivityMessage(message: ActivityMessage) {
        if (activityDeactivated.get()) return

        // Raw request/response captures carry the full LLM payload JSON as content — they
        // belong to the Activity Log tool window only, never to the in-chat timeline.
        if (message.source == ActivitySource.RAW) return

        val msgId = activeMessageId ?: return

        // Agent loop limit reached — surface a durable terminal notice in the chat
        // instead of (only) the Logs tool window. Sub-agent limits are not terminal
        // for the top-level message, so they fall through to regular activity handling.
        if (message.source == ActivitySource.AGENT &&
            message.agentType == AgentType.LOOP_LIMIT &&
            message.subAgentId == null
        ) {
            setTerminalState(msgId, TerminalState.LOOP_LIMIT, maxCalls = message.maxCalls)
            return
        }

        // Agent intermediate responses (reasoning text) — show as activity entry
        // instead of appending to aiResponseMarkdown (which gets overwritten by streaming updates)
        if (message.source == ActivitySource.AGENT &&
            message.agentType == AgentType.INTERMEDIATE_RESPONSE
        ) {
            val reasoning = message.result ?: return
            val entry = ActivityEntryUiModel(
                source = "AGENT",
                content = reasoning,
            )
            updateMessage(msgId) { msg ->
                msg.copy(activityEntries = msg.activityEntries + entry)
            }
            return
        }

        // Everything below is "tool activity". It is always *tracked* in the model — the
        // live status line in the AI bubble is derived from it regardless of settings —
        // but the detailed rows only *render* when MessageUiModel.showToolActivity is set.
        if (message.source == ActivitySource.AGENT) {
            when (message.agentType) {
                AgentType.TOOL_REQUEST -> appendToolRequest(msgId, message)
                AgentType.TOOL_RESPONSE -> resolveToolEntry(msgId, message, ActivityStatus.SUCCESS)
                AgentType.TOOL_ERROR -> resolveToolEntry(msgId, message, ActivityStatus.ERROR)
                AgentType.APPROVAL_REQUESTED -> markPendingApproval(msgId, message)
                AgentType.APPROVAL_GRANTED -> resolveApproval(msgId, message, granted = true)
                AgentType.APPROVAL_DENIED -> resolveApproval(msgId, message, granted = false)
                AgentType.SUB_AGENT_STARTED,
                AgentType.SUB_AGENT_COMPLETED,
                AgentType.SUB_AGENT_ERROR -> handleSubAgentEvent(msgId, message)
                AgentType.SYSTEM_PROMPT -> appendInfoEntry(msgId, message, "SYSTEM PROMPT", truncate = false)
                else -> appendInfoEntry(msgId, message)
            }
            return
        }

        // MCP / RAG sourced messages are one-shot log lines with no paired response
        // event — give them a terminal status immediately (no eternal spinner).
        appendInfoEntry(msgId, message)
    }

    /** Opens a new RUNNING timeline row for a TOOL_REQUEST. */
    private fun appendToolRequest(msgId: String, message: ActivityMessage) {
        val entry = ActivityEntryUiModel(
            source = message.source?.name ?: "UNKNOWN",
            content = message.content ?: "",
            toolName = message.toolName,
            arguments = truncateForChat(message.arguments),
            callNumber = message.callNumber,
            maxCalls = message.maxCalls,
            status = ActivityStatus.RUNNING,
            startedAt = System.currentTimeMillis(),
            isToolActivity = true,
            subAgentId = message.subAgentId,
        )
        updateMessage(msgId) { msg ->
            msg.copy(activityEntries = msg.activityEntries + entry)
        }
    }

    /**
     * Appends a lifecycle-less entry (MCP/RAG lines, stray agent events).
     *
     * [truncate] caps the arguments/result preview to 10 lines for noisy tool output. The
     * system prompt entry sets it false so users see the complete prompt the model received.
     */
    private fun appendInfoEntry(
        msgId: String,
        message: ActivityMessage,
        sourceLabel: String = message.source?.name ?: "UNKNOWN",
        truncate: Boolean = true,
    ) {
        val entry = ActivityEntryUiModel(
            source = sourceLabel,
            content = message.content ?: "",
            toolName = message.toolName,
            arguments = if (truncate) truncateForChat(message.arguments) else message.arguments,
            result = if (truncate) truncateForChat(message.result) else message.result,
            callNumber = message.callNumber,
            maxCalls = message.maxCalls,
            status = ActivityStatus.INFO,
            isToolActivity = true,
            subAgentId = message.subAgentId,
        )
        updateMessage(msgId) { msg ->
            msg.copy(activityEntries = msg.activityEntries + entry)
        }
    }

    /**
     * Resolves the open row matching this TOOL_RESPONSE/TOOL_ERROR by
     * (subAgentId, toolName, callNumber) instead of appending a new entry. Only
     * RUNNING/PENDING_APPROVAL rows can transition — a row already resolved to ERROR
     * (e.g. by APPROVAL_DENIED) is final, so the denial's follow-up TOOL_RESPONSE
     * cannot flip it back to SUCCESS. Unmatched and out-of-order events are ignored.
     */
    private fun resolveToolEntry(msgId: String, message: ActivityMessage, newStatus: ActivityStatus) {
        updateMessage(msgId) { msg ->
            val idx = msg.activityEntries.indexOfLast { entry ->
                entry.toolName == message.toolName &&
                    entry.callNumber == message.callNumber &&
                    entry.subAgentId == message.subAgentId &&
                    entry.isOpen()
            }
            if (idx < 0) return@updateMessage msg
            msg.replaceEntryAt(idx) { entry ->
                entry.copy(status = newStatus, result = truncateForChat(message.result))
            }
        }
    }

    /**
     * APPROVAL_REQUESTED has no callNumber (it is published by the approval layer, not
     * the loop tracker) — it marks the most recent RUNNING row for the same tool, which
     * is the request the approval dialog was opened for.
     */
    private fun markPendingApproval(msgId: String, message: ActivityMessage) {
        updateMessage(msgId) { msg ->
            val idx = msg.activityEntries.indexOfLast { entry ->
                entry.toolName == message.toolName && entry.status == ActivityStatus.RUNNING
            }
            if (idx < 0) return@updateMessage msg
            msg.replaceEntryAt(idx) { entry ->
                entry.copy(status = ActivityStatus.PENDING_APPROVAL, content = "Waiting for your approval…")
            }
        }
    }

    /** APPROVAL_GRANTED resumes the row; APPROVAL_DENIED resolves it as an error. */
    private fun resolveApproval(msgId: String, message: ActivityMessage, granted: Boolean) {
        updateMessage(msgId) { msg ->
            val idx = msg.activityEntries.indexOfLast { entry ->
                entry.toolName == message.toolName && entry.status == ActivityStatus.PENDING_APPROVAL
            }
            if (idx < 0) return@updateMessage msg
            msg.replaceEntryAt(idx) { entry ->
                if (granted) {
                    entry.copy(status = ActivityStatus.RUNNING, content = "")
                } else {
                    entry.copy(status = ActivityStatus.ERROR, content = "Denied by user")
                }
            }
        }
    }

    /**
     * SUB_AGENT_* events nest as child rows under the open parallel_explore call.
     * The launch announcement (toolName "parallel_explore") becomes the parent's
     * content line; per-sub-agent events become RUNNING → SUCCESS/ERROR children
     * matched by their "sub-agent-N" name. Without an open parent (defensive),
     * events are appended as top-level rows.
     */
    private fun handleSubAgentEvent(msgId: String, message: ActivityMessage) {
        updateMessage(msgId) { msg ->
            val parentIdx = msg.activityEntries.indexOfLast { entry ->
                entry.toolName == PARALLEL_EXPLORE_TOOL && entry.isOpen()
            }

            if (message.toolName == PARALLEL_EXPLORE_TOOL) {
                // Launch announcement — annotate the parent row, no child needed
                if (parentIdx < 0) return@updateMessage msg
                return@updateMessage msg.replaceEntryAt(parentIdx) { parent ->
                    parent.copy(content = message.arguments ?: parent.content)
                }
            }

            val status = when (message.agentType) {
                AgentType.SUB_AGENT_STARTED -> ActivityStatus.RUNNING
                AgentType.SUB_AGENT_COMPLETED -> ActivityStatus.SUCCESS
                else -> ActivityStatus.ERROR
            }

            if (parentIdx < 0) {
                // No open parallel_explore row — degrade gracefully to a top-level entry
                val orphan = ActivityEntryUiModel(
                    source = message.source?.name ?: "UNKNOWN",
                    content = message.content ?: "",
                    toolName = message.toolName,
                    arguments = truncateForChat(message.arguments),
                    result = truncateForChat(message.result),
                    callNumber = message.callNumber,
                    status = status,
                    isToolActivity = true,
                )
                return@updateMessage msg.copy(activityEntries = msg.activityEntries + orphan)
            }

            msg.replaceEntryAt(parentIdx) { parent ->
                val childIdx = parent.children.indexOfLast { it.toolName == message.toolName }
                if (childIdx < 0) {
                    val child = ActivityEntryUiModel(
                        source = message.source?.name ?: "UNKNOWN",
                        content = message.content ?: "",
                        toolName = message.toolName,
                        arguments = truncateForChat(message.arguments),
                        result = truncateForChat(message.result),
                        callNumber = message.callNumber,
                        status = status,
                        isToolActivity = true,
                    )
                    parent.copy(children = parent.children + child)
                } else {
                    val children = parent.children.toMutableList()
                    children[childIdx] = children[childIdx].copy(
                        status = status,
                        result = truncateForChat(message.result) ?: children[childIdx].result,
                    )
                    parent.copy(children = children)
                }
            }
        }
    }

    private fun ActivityEntryUiModel.isOpen(): Boolean =
        status == ActivityStatus.RUNNING || status == ActivityStatus.PENDING_APPROVAL

    private fun MessageUiModel.replaceEntryAt(
        index: Int,
        transform: (ActivityEntryUiModel) -> ActivityEntryUiModel,
    ): MessageUiModel {
        val entries = activityEntries.toMutableList()
        entries[index] = transform(entries[index])
        return copy(activityEntries = entries)
    }

    fun deactivateActivityHandlers() {
        activityDeactivated.set(true)
        activityGeneration.incrementAndGet()
        activeMessageId = null
    }

    fun hideLoadingIndicator(messageId: String) {
        // Complete, error and stop all end up here — clearing the streaming flag in the
        // same place keeps one lifecycle for both the loading dots and the streaming caret.
        // Rows still open at this point (e.g. a tool whose response was lost, or a stopped
        // run) are resolved to INFO so neither the spinner nor the elapsed-time ticker can
        // outlive the run.
        updateMessage(messageId) { msg ->
            msg.copy(
                isLoadingIndicatorVisible = false,
                isStreaming = false,
                activityEntries = finalizeOpenEntries(msg.activityEntries),
            )
        }
    }

    /** Resolves any still-open (RUNNING/PENDING_APPROVAL) rows — including children — to INFO. */
    private fun finalizeOpenEntries(
        entries: List<ActivityEntryUiModel>,
    ): List<ActivityEntryUiModel> {
        if (entries.none { it.isOpen() || it.children.any { child -> child.isOpen() } }) return entries
        return entries.map { entry ->
            val children = if (entry.children.any { it.isOpen() }) {
                entry.children.map { child ->
                    if (child.isOpen()) child.copy(status = ActivityStatus.INFO) else child
                }
            } else {
                entry.children
            }
            if (entry.isOpen()) {
                entry.copy(status = ActivityStatus.INFO, children = children)
            } else if (children !== entry.children) {
                entry.copy(children = children)
            } else {
                entry
            }
        }
    }

    fun markMCPLogsAsCompleted(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(mcpLogsCompleted = true)
        }
    }

    /**
     * Marks a message with an explicit terminal state. Terminal states are mutually
     * exclusive and final: the first non-COMPLETED state wins and later calls are
     * ignored, so e.g. a STOPPED message can never be re-marked as ERROR by a
     * straggling failure callback. Also clears the loading/streaming affordances
     * (the in-flight status line must not outlive the run).
     */
    fun setTerminalState(
        messageId: String,
        state: TerminalState,
        errorText: String? = null,
        maxCalls: Int = 0,
    ) {
        if (state == TerminalState.COMPLETED) return
        updateMessage(messageId) { msg ->
            if (msg.terminalState != TerminalState.COMPLETED) {
                msg // already terminal — final, ignore
            } else {
                msg.copy(
                    terminalState = state,
                    errorText = errorText?.takeIf { it.isNotBlank() },
                    loopLimitMaxCalls = maxCalls,
                    isLoadingIndicatorVisible = false,
                    isStreaming = false,
                    activityEntries = finalizeOpenEntries(msg.activityEntries),
                )
            }
        }
    }

    /**
     * Retry handler for the inline error card. Re-submits the original user prompt
     * through [onRetryPrompt] exactly once per message — the [MessageUiModel.retryAttempted]
     * flag guards against double-submission (the button also renders disabled).
     */
    fun onRetryClicked(messageId: String) {
        val current = state as? ConversationState.Chat ?: return
        val msg = current.messages.firstOrNull { it.id == messageId } ?: return
        if (msg.terminalState != TerminalState.ERROR || msg.retryAttempted) return
        if (msg.userPrompt.isBlank()) return
        updateMessage(messageId) { it.copy(retryAttempted = true) }
        try {
            onRetryPrompt(msg.userPrompt)
        } catch (e: Exception) {
            // best-effort — a failed re-submission must not break the chat UI
            logger.debug("Retry re-submission failed for message $messageId", e)
        }
    }

    fun clearConversation() {
        if (restoringConversation.get()) {
            // Mid-restore (or clearing before the first prompt of a new chat):
            // reset to an empty chat so the welcome screen never flashes.
            state = ConversationState.Chat(messages = emptyList(), isRestoringConversation = true)
            return
        }
        val current = state
        val rb = when (current) {
            is ConversationState.Welcome -> current.resourceBundle
            is ConversationState.Chat -> ResourceBundle.getBundle("messages")
        }
        state = ConversationState.Welcome(
            resourceBundle = rb,
            customPrompts = loadCustomPrompts(),
            skills = loadSkills(),
            blogPosts = loadBlogPosts(),
            hasMcpServers = loadHasMcpServers(),
        )
    }

    fun setRestoringConversation(restoring: Boolean) {
        restoringConversation.set(restoring)
        val current = state
        if (current is ConversationState.Chat) {
            state = current.copy(isRestoringConversation = restoring)
        }
    }

    // No-ops — these were needed for JCEF but not for Compose
    fun cancelPendingWelcomeLoad() { /* no-op */ }
    fun refreshForNewConversation() { /* no-op */ }

    private fun updateMessage(messageId: String, transform: (MessageUiModel) -> MessageUiModel) {
        val current = state
        if (current is ConversationState.Chat) {
            val updated = current.messages.map { msg ->
                if (msg.id == messageId) transform(msg) else msg
            }
            state = current.copy(messages = updated)
        }
    }

    /**
     * Builds the per-message token usage info from a completed context, or null when the
     * provider returned no token usage (e.g. some local models). The model's input context
     * window is carried along so the chat bubble can show how full the window was.
     */
    private fun buildTokenUsage(context: ChatMessageContext): TokenUsageInfo? {
        val usage = context.tokenUsage ?: return null
        return TokenUsageInfo(
            inputTokens = usage.inputTokenCount()?.toLong() ?: 0,
            outputTokens = usage.outputTokenCount()?.toLong() ?: 0,
            cost = context.cost,
            contextWindowMax = context.languageModel?.inputMaxTokens?.toLong() ?: 0,
        )
    }

    private fun formatModelDisplayName(model: LanguageModel?): String {
        val name = model?.modelName ?: return ""
        val provider = model.provider?.name ?: return name
        return "$provider : $name"
    }

    private fun loadBlogPosts(): List<BlogPostUi> {
        return try {
            BlogFeedService.getInstance().initialPosts
                .take(5)
                .map { it.toUi() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun refreshBlogPostsAsync() {
        try {
            BlogFeedService.getInstance().refreshRemoteAsync { fresh ->
                val current = state
                if (current is ConversationState.Welcome) {
                    state = current.copy(
                        blogPosts = fresh.take(5).map { it.toUi() },
                    )
                }
            }
        } catch (_: Exception) {
            // best-effort — ignore
        }
    }

    private fun BlogPost.toUi(): BlogPostUi = BlogPostUi(
        title = title(),
        description = description(),
        date = date(),
        url = url(),
    )

    private fun loadCustomPrompts(): List<CustomPromptUi> {
        return try {
            val settings = DevoxxGenieStateService.getInstance()
            val webSearchEnabled = settings.isWebSearchEnabled == true
            settings.commands
                .filter { cmd ->
                    // /search only appears when Web Search is enabled in settings
                    cmd.name != com.devoxx.genie.model.Constant.SEARCH_COMMAND || webSearchEnabled
                }
                .map { CustomPromptUi(name = it.name, prompt = it.prompt) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Reads the currently-enabled skills from the project-scoped {@link SkillRegistry}.
     * Returns an empty list when no project is associated (tests, default project),
     * when the registry has not yet been loaded, or when every detected skill is disabled.
     */
    private fun loadSkills(): List<SkillUi> {
        val proj = project ?: return emptyList()
        return try {
            val registry = SkillRegistry.getInstance(proj) ?: return emptyList()
            val disabled = DevoxxGenieStateService.getInstance().disabledSkillNames ?: emptySet()
            registry.peekAllSkills()
                .asSequence()
                .filter { entry -> !disabled.contains(entry.name()) }
                .map { entry ->
                    SkillUi(
                        name = entry.name(),
                        description = entry.description() ?: "",
                        source = entry.source().label(),
                    )
                }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Whether at least one MCP server is configured. Drives the welcome-page
     * "Add MCP" setup nudge. Reads the project-independent application settings.
     */
    private fun loadHasMcpServers(): Boolean {
        return try {
            DevoxxGenieStateService.getInstance()
                .mcpSettings
                ?.mcpServers
                ?.isNotEmpty() ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Triggers a pooled-thread re-scan of the skill directories and, on completion,
     * updates the welcome state with whatever the registry produced. Cheap to call:
     * the registry no-ops when the cache is already populated.
     */
    private fun refreshSkillsAsync() {
        val proj = project ?: return
        try {
            val registry = SkillRegistry.getInstance(proj) ?: return
            registry.reloadAsync {
                val current = state
                if (current is ConversationState.Welcome) {
                    state = current.copy(skills = loadSkills(), hasMcpServers = loadHasMcpServers())
                }
            }
        } catch (_: Exception) {
            // best-effort — keep the welcome screen working if anything fails
        }
    }

    companion object {
        private val logger = Logger.getInstance(ConversationViewModel::class.java)

        private const val PARALLEL_EXPLORE_TOOL = "parallel_explore"

        /** Same inline-preview limits as AgentMcpLogPanel. */
        private const val MAX_LINE_LENGTH = 500
        private const val MAX_LINES = 10

        /**
         * Truncates tool arguments/results before they enter Compose state — the full
         * payload (a tool result can be megabytes) stays in the Logs tool window only.
         * Limits mirror AgentMcpLogPanel's inline preview: 500 chars/line, 10 lines.
         */
        internal fun truncateForChat(text: String?): String? {
            if (text.isNullOrEmpty()) return text
            val lines = text.lines()
            val kept = lines.take(MAX_LINES).map { line ->
                if (line.length > MAX_LINE_LENGTH) line.take(MAX_LINE_LENGTH - 1) + "…" else line
            }
            val dropped = lines.size - MAX_LINES
            return if (dropped > 0) {
                (kept + "($dropped more lines)").joinToString("\n")
            } else {
                kept.joinToString("\n")
            }
        }
    }
}
