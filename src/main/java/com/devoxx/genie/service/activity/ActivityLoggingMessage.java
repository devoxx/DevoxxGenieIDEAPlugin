package com.devoxx.genie.service.activity;

import com.devoxx.genie.model.activity.ActivityMessage;
import org.jetbrains.annotations.NotNull;

public interface ActivityLoggingMessage {
    void onActivityMessage(@NotNull ActivityMessage message);
}
