package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;
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

    /**
     * Delivers an activity only while the prompt generation that produced it remains active.
     * This prevents an event waiting on the EDT from being attached to a later prompt.
     */
    void dispatch(@NotNull ActivityMessage message,
                  int expectedGeneration,
                  @NotNull IntSupplier currentGeneration,
                  @NotNull Consumer<ActivityMessage> consumer) {
        uiQueue.accept(() -> {
            if (expectedGeneration == currentGeneration.getAsInt()) {
                consumer.accept(message);
            }
        });
    }
}
