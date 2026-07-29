package com.devoxx.genie.service.prompt.steering;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1241: messages typed while an agent task is running are queued per
 * memory key and injected into the agent loop on the next round trip.
 */
class SteeringMessageQueueTest {

    private final SteeringMessageQueue queue = new SteeringMessageQueue();

    @Test
    void drainReturnsOfferedMessagesInFifoOrder() {
        queue.offer("key1", "first");
        queue.offer("key1", "second");

        assertThat(queue.drain("key1")).containsExactly("first", "second");
    }

    @Test
    void drainEmptiesTheQueue() {
        queue.offer("key1", "message");

        queue.drain("key1");

        assertThat(queue.drain("key1")).isEmpty();
        assertThat(queue.hasPending("key1")).isFalse();
    }

    @Test
    void drainOnUnknownKeyReturnsEmptyList() {
        assertThat(queue.drain("unknown")).isEmpty();
    }

    @Test
    void memoryKeysAreIsolated() {
        queue.offer("key1", "for key1");
        queue.offer("key2", "for key2");

        assertThat(queue.drain("key1")).containsExactly("for key1");
        assertThat(queue.hasPending("key2")).isTrue();
    }

    @Test
    void hasPendingReflectsQueueState() {
        assertThat(queue.hasPending("key1")).isFalse();

        queue.offer("key1", "message");

        assertThat(queue.hasPending("key1")).isTrue();
    }

    @Test
    void keyIsInactiveByDefault() {
        assertThat(queue.isActive("key1")).isFalse();
    }

    @Test
    void activateMarksKeyActiveUntilDeactivated() {
        queue.activate("key1");

        assertThat(queue.isActive("key1")).isTrue();
        assertThat(queue.isActive("key2")).isFalse();

        queue.deactivate("key1");

        assertThat(queue.isActive("key1")).isFalse();
    }

    @Test
    void drainAndDeactivateReturnsLeftoversAndDeactivates() {
        queue.activate("key1");
        queue.offer("key1", "never consumed");

        assertThat(queue.drainAndDeactivate("key1")).containsExactly("never consumed");
        assertThat(queue.isActive("key1")).isFalse();
        assertThat(queue.hasPending("key1")).isFalse();
    }

    @Test
    void nullMemoryKey_isSafeNoOp() {
        // ConcurrentHashMap rejects null keys; a null memory key must never throw.
        queue.offer(null, "message");
        queue.activate(null);
        queue.deactivate(null);

        assertThat(queue.isActive(null)).isFalse();
        assertThat(queue.hasPending(null)).isFalse();
        assertThat(queue.drain(null)).isEmpty();
        assertThat(queue.drainAndDeactivate(null)).isEmpty();
    }

    @Test
    void getInstanceReturnsSharedSingleton() {
        assertThat(SteeringMessageQueue.getInstance()).isSameAs(SteeringMessageQueue.getInstance());
    }
}
