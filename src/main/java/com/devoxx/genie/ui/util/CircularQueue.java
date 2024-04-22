package com.devoxx.genie.ui.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class CircularQueue<E> {

    private ArrayDeque<E> deque;
    private int maxSize;

    public CircularQueue(int maxSize) {
        this.maxSize = maxSize;
        this.deque = new ArrayDeque<>(maxSize);
    }

    public void add(E e) {
        if (deque.size() == maxSize) {
            deque.pollFirst();
        }
        deque.offerLast(e);
    }

    public E remove() {
        return deque.pollFirst();
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
