package com.devoxx.genie.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ThreadUtilsTest {

    // --- sleep(long) tests ---

    @Test
    void sleep_shortDuration_completesWithoutException() {
        assertThatCode(() -> ThreadUtils.sleep(10))
                .doesNotThrowAnyException();
    }

    @Test
    void sleep_zeroDuration_completesWithoutException() {
        assertThatCode(() -> ThreadUtils.sleep(0))
                .doesNotThrowAnyException();
    }

    @Test
    void sleep_actuallyWaits() {
        long startTime = System.currentTimeMillis();
        ThreadUtils.sleep(50);
        long elapsed = System.currentTimeMillis() - startTime;

        // Allow some tolerance for scheduling
        assertThat(elapsed).isGreaterThanOrEqualTo(40);
    }

    @Test
    void sleep_interruptedThread_restoresInterruptFlag() {
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            ThreadUtils.sleep(5000);
            // After being interrupted, the interrupt flag should be restored
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        });

        testThread.start();
        try {
            testThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Thread should have finished quickly (not waited 5 seconds)
        assertThat(testThread.isAlive()).isFalse();
    }

    // --- sleep(long, String) tests ---

    @Test
    void sleepWithMessage_shortDuration_completesWithoutException() {
        assertThatCode(() -> ThreadUtils.sleep(10, "Test error message"))
                .doesNotThrowAnyException();
    }

    @Test
    void sleepWithMessage_zeroDuration_completesWithoutException() {
        assertThatCode(() -> ThreadUtils.sleep(0, "Test error message"))
                .doesNotThrowAnyException();
    }

    @Test
    void sleepWithMessage_actuallyWaits() {
        long startTime = System.currentTimeMillis();
        ThreadUtils.sleep(50, "Should not appear");
        long elapsed = System.currentTimeMillis() - startTime;

        assertThat(elapsed).isGreaterThanOrEqualTo(40);
    }

    @Test
    void sleepWithMessage_interruptedThread_restoresInterruptFlag() {
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            ThreadUtils.sleep(5000, "Thread was interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        });

        testThread.start();
        try {
            testThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(testThread.isAlive()).isFalse();
    }

    @Test
    void sleepWithMessage_externalInterrupt_restoresInterruptFlag() throws InterruptedException {
        Thread testThread = new Thread(() -> {
            ThreadUtils.sleep(5000, "Externally interrupted");
            // Interrupt flag should be set after being interrupted
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        });

        testThread.start();
        // Give thread time to enter sleep
        Thread.sleep(50);
        testThread.interrupt();
        testThread.join(2000);

        assertThat(testThread.isAlive()).isFalse();
    }

    @Test
    void sleep_externalInterrupt_restoresInterruptFlag() throws InterruptedException {
        Thread testThread = new Thread(() -> {
            ThreadUtils.sleep(5000);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        });

        testThread.start();
        Thread.sleep(50);
        testThread.interrupt();
        testThread.join(2000);

        assertThat(testThread.isAlive()).isFalse();
    }

    @Test
    void sleepWithMessage_nullMessage_doesNotThrow() {
        assertThatCode(() -> ThreadUtils.sleep(10, null))
                .doesNotThrowAnyException();
    }
}
