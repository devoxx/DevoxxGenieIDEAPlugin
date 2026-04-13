package com.devoxx.genie.service.analytics;

import com.intellij.util.messages.Topic;

/**
 * MessageBus topic broadcast whenever a tracked DevoxxGenie setting changes (task-209, AC #22).
 *
 * <p>The analytics session-snapshot service subscribes to this topic so the feature-enablement
 * snapshot re-arms on any settings mutation — no need to wire per-panel {@code apply()} hooks
 * into every settings component.
 *
 * <p>Publishers call {@code
 * ApplicationManager.getApplication().getMessageBus().syncPublisher(DevoxxGenieSettingsChangedTopic.TOPIC)
 * .settingsChanged()} after committing their state changes.
 */
public interface DevoxxGenieSettingsChangedTopic {

    Topic<DevoxxGenieSettingsChangedTopic> TOPIC =
            Topic.create("DevoxxGenie settings changed", DevoxxGenieSettingsChangedTopic.class);

    /** Fired after any tracked setting has been written back to {@code DevoxxGenieStateService}. */
    void settingsChanged();
}
