package com.devoxx.genie.service.prompt.steering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1241 (queue mode): prompts submitted while a task is running are queued
 * per memory key and executed sequentially — one prompt per run — after the
 * current run completes. Unlike {@link SteeringMessageQueue}, entries are
 * independent prompts, never injected into the running loop.
 */
class PendingPromptQueueTest {

    private final PendingPromptQueue queue = new PendingPromptQueue();

    @Test
    void pollNextReturnsPromptsOneAtATimeInFifoOrder() {
        queue.offer("key1", "first question");
        queue.offer("key1", "second question");

        assertThat(queue.pollNext("key1")).isEqualTo("first question");
        assertThat(queue.pollNext("key1")).isEqualTo("second question");
        assertThat(queue.pollNext("key1")).isNull();
    }

    @Test
    void hasPendingReflectsQueueState() {
        assertThat(queue.hasPending("key1")).isFalse();

        queue.offer("key1", "question");

        assertThat(queue.hasPending("key1")).isTrue();
    }

    @Test
    void drainEmptiesTheQueueAndReturnsAllPrompts() {
        queue.offer("key1", "one");
        queue.offer("key1", "two");

        assertThat(queue.drain("key1")).containsExactly("one", "two");
        assertThat(queue.hasPending("key1")).isFalse();
    }

    @Test
    void memoryKeysAreIsolated() {
        queue.offer("key1", "for key1");

        assertThat(queue.pollNext("key2")).isNull();
        assertThat(queue.pollNext("key1")).isEqualTo("for key1");
    }

    @Test
    void nullMemoryKeyIsSafeNoOp() {
        queue.offer(null, "question");

        assertThat(queue.hasPending(null)).isFalse();
        assertThat(queue.pollNext(null)).isNull();
        assertThat(queue.drain(null)).isEmpty();
    }

    @Test
    void getInstanceReturnsSharedSingleton() {
        assertThat(PendingPromptQueue.getInstance()).isSameAs(PendingPromptQueue.getInstance());
    }
}
