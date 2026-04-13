package com.devoxx.genie.service.analytics;

import com.intellij.openapi.application.ApplicationManager;
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

    /**
     * Fail-silent broadcast helper. Any exception — including null message bus in test
     * environments — is swallowed so settings {@code apply()} paths never crash because of
     * analytics plumbing.
     */
    static void notifySettingsChanged() {
        try {
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(TOPIC)
                    .settingsChanged();
        } catch (Exception | Error ignored) {
            // Best-effort — settings changes must never fail because analytics is unreachable.
        }
    }
}
