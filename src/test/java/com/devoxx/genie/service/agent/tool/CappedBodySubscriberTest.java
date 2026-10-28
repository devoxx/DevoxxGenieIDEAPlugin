package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

class CappedBodySubscriberTest {

    @Test
    void bodyLargerThanTheCapIsCutAtTheCapAndTheDownloadIsCancelled() {
        var subscriber = new CappedBodySubscriber(10);
        var subscription = new RecordingSubscription();
        subscriber.onSubscribe(subscription);

        subscriber.onNext(List.of(ByteBuffer.wrap("123456".getBytes())));
        subscriber.onNext(List.of(ByteBuffer.wrap("7890abcdef".getBytes())));

        assertThat(subscriber.getBody().toCompletableFuture().join()).isEqualTo("1234567890".getBytes());
        assertThat(subscription.cancelled).isTrue();
    }

    @Test
    void bodySmallerThanTheCapIsReturnedWhole() {
        var subscriber = new CappedBodySubscriber(10);
        var subscription = new RecordingSubscription();
        subscriber.onSubscribe(subscription);

        subscriber.onNext(List.of(ByteBuffer.wrap("abc".getBytes())));
        subscriber.onComplete();

        assertThat(subscriber.getBody().toCompletableFuture().join()).isEqualTo("abc".getBytes());
        assertThat(subscription.cancelled).isFalse();
    }

    private static final class RecordingSubscription implements Flow.Subscription {

        private boolean cancelled;

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }
}
