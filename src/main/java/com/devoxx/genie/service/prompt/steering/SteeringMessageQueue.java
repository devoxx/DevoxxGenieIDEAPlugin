package com.devoxx.genie.service.prompt.steering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue of user messages typed while an agent task is running (issue #1241).
 * Messages are keyed by chat-memory key and injected into the agent loop on
 * the next round trip by {@link SteeringMessageInjector}.
 *
 * <p>A key is "active" while a running execution has a {@link SteeringMessageInjector}
 * wired that will consume the queue; the UI only offers steering messages for
 * active keys and falls back to the stop behavior otherwise.
 *
 * <p>Thread-safe: offers come from the EDT while drains happen on the
 * agent-loop worker thread.
 */
public class SteeringMessageQueue {

    private static final SteeringMessageQueue INSTANCE = new SteeringMessageQueue();

    private final Map<String, Queue<String>> pendingByKey = new ConcurrentHashMap<>();
    private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();

    public static SteeringMessageQueue getInstance() {
        return INSTANCE;
    }

    // A null memory key must never throw: ConcurrentHashMap rejects null keys,
    // and callers derive the key from contexts that may be absent (e.g. tests,
    // command-only submissions).

    public void offer(String memoryKey, String text) {
        if (memoryKey == null) {
            return;
        }
        pendingByKey.computeIfAbsent(memoryKey, key -> new ConcurrentLinkedQueue<>()).add(text);
    }

    public List<String> drain(String memoryKey) {
        Queue<String> queue = memoryKey != null ? pendingByKey.remove(memoryKey) : null;
        if (queue == null) {
            return List.of();
        }
        List<String> drained = new ArrayList<>();
        String message;
        while ((message = queue.poll()) != null) {
            drained.add(message);
        }
        return drained;
    }

    public boolean hasPending(String memoryKey) {
        Queue<String> queue = memoryKey != null ? pendingByKey.get(memoryKey) : null;
        return queue != null && !queue.isEmpty();
    }

    public void activate(String memoryKey) {
        if (memoryKey != null) {
            activeKeys.add(memoryKey);
        }
    }

    public void deactivate(String memoryKey) {
        if (memoryKey != null) {
            activeKeys.remove(memoryKey);
        }
    }

    public boolean isActive(String memoryKey) {
        return memoryKey != null && activeKeys.contains(memoryKey);
    }

    public List<String> drainAndDeactivate(String memoryKey) {
        deactivate(memoryKey);
        return drain(memoryKey);
    }
}
