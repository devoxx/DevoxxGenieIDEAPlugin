package com.devoxx.genie.service.agent.tool;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

final class FetchedPageCache {

    private static final FetchedPageCache SHARED =
            new FetchedPageCache(Duration.ofMinutes(15), 32, System::currentTimeMillis);

    private final long timeToLiveMillis;

    private final int maxEntries;

    private final LongSupplier currentTimeMillis;

    private final Map<URI, Entry> entries;

    FetchedPageCache(@NotNull Duration timeToLive, int maxEntries, @NotNull LongSupplier currentTimeMillis) {
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.timeToLiveMillis = timeToLive.toMillis();
        this.maxEntries = maxEntries;
        this.currentTimeMillis = currentTimeMillis;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<URI, Entry> eldest) {
                return size() > FetchedPageCache.this.maxEntries;
            }
        };
    }

    static @NotNull FetchedPageCache shared() {
        return SHARED;
    }

    synchronized @NotNull Optional<String> get(@NotNull URI uri) {
        var entry = this.entries.get(uri);
        if (entry == null) {
            return Optional.empty();
        }
        if (this.currentTimeMillis.getAsLong() - entry.storedAtMillis() > this.timeToLiveMillis) {
            this.entries.remove(uri);
            return Optional.empty();
        }
        return Optional.of(entry.content());
    }

    synchronized void put(@NotNull URI uri, @NotNull String content) {
        this.entries.put(uri, new Entry(content, this.currentTimeMillis.getAsLong()));
    }

    private record Entry(String content, long storedAtMillis) {
    }
}
