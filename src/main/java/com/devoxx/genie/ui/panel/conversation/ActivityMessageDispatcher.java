package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Serializes activity-log delivery with Swing and Compose UI updates on the IntelliJ EDT.
 */
final class ActivityMessageDispatcher {

    private final Consumer<Runnable> uiQueue;

    ActivityMessageDispatcher() {
        this(runnable -> ApplicationManager.getApplication().invokeLater(runnable));
    }

    ActivityMessageDispatcher(@NotNull Consumer<Runnable> uiQueue) {
        this.uiQueue = uiQueue;
    }

    void dispatch(@NotNull ActivityMessage message, @NotNull Consumer<ActivityMessage> consumer) {
        uiQueue.accept(() -> consumer.accept(message));
    }
}
