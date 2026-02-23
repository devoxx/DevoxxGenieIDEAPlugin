package com.devoxx.genie.ui.topic;

import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.service.mcp.MCPLoggingMessage;
import com.devoxx.genie.ui.listener.*;
import com.devoxx.genie.ui.settings.appearance.AppearanceSettingsEvents;
import com.intellij.util.messages.Topic;

public class AppTopics {

    private AppTopics() {
        /* This utility class should not be instantiated */
    }

    public static final Topic<SettingsChangeListener> SETTINGS_CHANGED_TOPIC =
        Topic.create("SettingsChanged", SettingsChangeListener.class);

    public static final Topic<LLMSettingsChangeListener> LLM_SETTINGS_CHANGED_TOPIC =
        Topic.create("LLMSettingsChanged", LLMSettingsChangeListener.class);

    public static final Topic<CustomPromptChangeListener> CUSTOM_PROMPT_CHANGED_TOPIC =
        Topic.create("CustomPromptChanged", CustomPromptChangeListener.class);

    public static final Topic<PromptSubmissionListener> PROMPT_SUBMISSION_TOPIC =
        Topic.create("PromptSubmission", PromptSubmissionListener.class);

    public static final Topic<ConversationEventListener> CONVERSATION_TOPIC =
        Topic.create("NewConversation", ConversationEventListener.class);

    public static final Topic<RAGStateListener> RAG_STATE_TOPIC =
            Topic.create("RAGStateEnabled", RAGStateListener.class);

    public static final Topic<WebSearchStateListener> WEB_SEARCH_STATE_TOPIC =
            Topic.create("WebSearchState", WebSearchStateListener.class);

    public static final Topic<RAGStateListener> RAG_ACTIVATED_CHANGED_TOPIC =
            Topic.create("RagStateActivated", RAGStateListener.class);

    public static final Topic<ShortcutChangeListener> SHORTCUT_CHANGED_TOPIC =
            Topic.create("shortcutChanged", ShortcutChangeListener.class);
            
    public static final Topic<NewlineShortcutChangeListener> NEWLINE_SHORTCUT_CHANGED_TOPIC =
            Topic.create("newlineShortcutChanged", NewlineShortcutChangeListener.class);

    public static final Topic<MCPLoggingMessage> MCP_TRAFFIC_MSG =
            Topic.create("mcpTrafficMessage", MCPLoggingMessage.class);

    public static final Topic<FileReferencesListener> FILE_REFERENCES_TOPIC =
            Topic.create("fileReferences", FileReferencesListener.class);

    public static final Topic<ConversationSelectionListener> CONVERSATION_SELECTION_TOPIC =
            Topic.create("conversationSelection", ConversationSelectionListener.class);

    public static final Topic<AppearanceSettingsEvents> APPEARANCE_SETTINGS_TOPIC =
            Topic.create("DevoxxGenie Appearance Settings", AppearanceSettingsEvents.class);

    public static final Topic<ActivityLoggingMessage> ACTIVITY_LOG_MSG =
            Topic.create("activityLoggingMessage", ActivityLoggingMessage.class);

}
