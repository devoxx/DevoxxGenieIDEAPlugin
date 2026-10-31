package com.devoxx.genie.service.agent.tool;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class CappedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

    private final int maxBytes;

    private final ByteArrayOutputStream received = new ByteArrayOutputStream();

    private final CompletableFuture<byte[]> body = new CompletableFuture<>();

    private Flow.Subscription subscription;

    CappedBodySubscriber(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    public CompletionStage<byte[]> getBody() {
        return this.body;
    }

    @Override
    public void onSubscribe(@NotNull Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(@NotNull List<ByteBuffer> buffers) {
        for (ByteBuffer buffer : buffers) {
            var length = Math.min(buffer.remaining(), this.maxBytes - this.received.size());
            var chunk = new byte[length];
            buffer.get(chunk);
            this.received.write(chunk, 0, length);
            if (this.received.size() >= this.maxBytes) {
                this.subscription.cancel();
                this.body.complete(this.received.toByteArray());
                return;
            }
        }
        this.subscription.request(1);
    }

    @Override
    public void onError(@NotNull Throwable error) {
        this.body.completeExceptionally(error);
    }

    @Override
    public void onComplete() {
        this.body.complete(this.received.toByteArray());
    }
}
