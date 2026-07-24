package com.devoxx.genie.ui.panel.conversation;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityMessageDispatcherTest {

    @Test
    void dispatch_defersActivityHandlingUntilTheUiQueueRuns() {
        List<Runnable> queuedTasks = new ArrayList<>();
        ActivityMessage message = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .build();
        AtomicReference<ActivityMessage> delivered = new AtomicReference<>();

        ActivityMessageDispatcher dispatcher = new ActivityMessageDispatcher(queuedTasks::add);

        dispatcher.dispatch(message, delivered::set);

        assertThat(delivered).hasValue(null);
        assertThat(queuedTasks).hasSize(1);

        queuedTasks.get(0).run();

        assertThat(delivered.get()).isSameAs(message);
    }
}
