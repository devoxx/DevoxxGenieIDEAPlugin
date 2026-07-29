package com.devoxx.genie.service.prompt.steering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FIFO queue of independent prompts submitted while a task was running
 * (issue #1241, queue mode — the default). Unlike {@link SteeringMessageQueue},
 * entries are never injected into the running agent loop: each one is executed
 * as its own prompt, one per run, after the current run completes.
 */
public class PendingPromptQueue {

    private static final PendingPromptQueue INSTANCE = new PendingPromptQueue();

    private final Map<String, Queue<String>> pendingByKey = new ConcurrentHashMap<>();

    public static PendingPromptQueue getInstance() {
        return INSTANCE;
    }

    public void offer(String memoryKey, String text) {
        if (memoryKey == null) {
            return;
        }
        pendingByKey.computeIfAbsent(memoryKey, key -> new ConcurrentLinkedQueue<>()).add(text);
    }

    /** Returns and removes the next queued prompt, or null when none is pending. */
    public String pollNext(String memoryKey) {
        Queue<String> queue = memoryKey != null ? pendingByKey.get(memoryKey) : null;
        return queue != null ? queue.poll() : null;
    }

    public boolean hasPending(String memoryKey) {
        Queue<String> queue = memoryKey != null ? pendingByKey.get(memoryKey) : null;
        return queue != null && !queue.isEmpty();
    }

    /** Removes and returns all queued prompts (used when the user stops the run). */
    public List<String> drain(String memoryKey) {
        Queue<String> queue = memoryKey != null ? pendingByKey.remove(memoryKey) : null;
        if (queue == null) {
            return List.of();
        }
        List<String> drained = new ArrayList<>();
        String prompt;
        while ((prompt = queue.poll()) != null) {
            drained.add(prompt);
        }
        return drained;
    }
}
