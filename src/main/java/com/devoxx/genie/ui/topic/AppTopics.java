package com.devoxx.genie.ui.topic;

import com.devoxx.genie.ui.listener.ChatMessageManagementService;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.intellij.util.messages.Topic;

public class AppTopics {

    public static final Topic<SettingsChangeListener> SETTINGS_CHANGED_TOPIC =
        Topic.create("SettingsChanged", SettingsChangeListener.class);

    public static final Topic<ChatMessageManagementService> CHAT_MESSAGES_CHANGED_TOPIC =
        Topic.create("chatChanged", ChatMessageManagementService.class);

}
