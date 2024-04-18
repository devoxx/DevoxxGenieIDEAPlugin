package com.devoxx.genie.ui;

import com.intellij.util.messages.Topic;

public interface SettingsChangeListener {

    Topic<SettingsChangeListener> TOPIC =
        Topic.create("SettingsChanged", SettingsChangeListener.class);

    void settingsChanged();
}
