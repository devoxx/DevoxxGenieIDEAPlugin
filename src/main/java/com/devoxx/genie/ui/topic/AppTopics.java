package com.devoxx.genie.ui.topic;

import com.devoxx.genie.ui.listener.FileSelectionListener;
import com.devoxx.genie.ui.listener.SettingsChangeListener;
import com.intellij.util.messages.Topic;

public class AppTopics {

    public static final Topic<SettingsChangeListener> SETTINGS_CHANGED_TOPIC =
        Topic.create("SettingsChanged", SettingsChangeListener.class);

    public static final Topic<FileSelectionListener> FILE_SELECTION_TOPIC =
        Topic.create("File Selection Topic", FileSelectionListener.class);
}
