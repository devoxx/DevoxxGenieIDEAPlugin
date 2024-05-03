package com.devoxx.genie.ui.util;

import dev.langchain4j.data.message.SystemMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class CircularQueue<E> {

    private final ArrayDeque<E> deque;
    private final int maxSize;

    public CircularQueue(int maxSize) {
        this.maxSize = maxSize;
        this.deque = new ArrayDeque<>(maxSize);
    }

    public void add(E e) {
        if (deque.size() == maxSize) {
            // Check if the first element is a SystemMessage and skip its removal if so
            if (deque.peekFirst() instanceof SystemMessage) {
                // Remove the second element instead
                E first = deque.pollFirst();
                deque.pollFirst();
                deque.offerFirst(first);
            } else {
                deque.pollFirst();
            }
        }
        deque.offerLast(e);
    }

    public E remove() {
        return deque.pollFirst();
    }

    public void removeAll() {
        deque.removeAll(deque.clone());
    }

    public E peek() {
        return deque.peekFirst();
    }

    public int size() {
        return deque.size();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }

    public List<E> asList() {
        return new ArrayList<>(deque);
    }
}
