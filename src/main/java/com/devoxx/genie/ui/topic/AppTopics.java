package com.devoxx.genie.ui.topic;

import com.devoxx.genie.ui.listener.ChatMemorySizeListener;
import com.devoxx.genie.ui.listener.CustomPromptChangeListener;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.intellij.util.messages.Topic;

public class AppTopics {

    public static final Topic<SettingsChangeListener> SETTINGS_CHANGED_TOPIC =
        Topic.create("SettingsChanged", SettingsChangeListener.class);

    public static final Topic<ChatMemorySizeListener> CHAT_MEMORY_SIZE_TOPIC =
        new Topic<>("CHAT_MEMORY_SIZE_TOPIC", ChatMemorySizeListener.class);

    public static final Topic<LLMSettingsChangeListener> LLM_SETTINGS_CHANGED_TOPIC =
        Topic.create("LLMSettingsChanged", LLMSettingsChangeListener.class);

    public static final Topic<CustomPromptChangeListener> CUSTOM_PROMPT_CHANGED_TOPIC =
        Topic.create("CustomPromptChanged", CustomPromptChangeListener.class);
}
