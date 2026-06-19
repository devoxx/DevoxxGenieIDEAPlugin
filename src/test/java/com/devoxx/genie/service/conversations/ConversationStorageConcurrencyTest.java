package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reproduces the "two chats closed at once lose their history" bug: several tabs writing
 * concurrently must not drop a conversation due to SQLite write-lock contention.
 */
class ConversationStorageConcurrencyTest {

    @TempDir
    Path tempDir;

    private Conversation newConversation(String id) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setTitle("conv-" + id);
        c.setTimestamp(LocalDateTime.now().toString());
        c.setModelName("m");
        c.setLlmProvider("p");
        c.setApiKeyUsed(false);
        c.setInputCost(0L);
        c.setOutputCost(0L);
        c.setContextWindow(0);
        c.setExecutionTimeMs(0);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(true, "hi " + id, c.getTimestamp()));
        messages.add(new ChatMessage(false, "bye " + id, c.getTimestamp()));
        c.setMessages(messages);
        return c;
    }

    @Test
    void concurrentWritesFromMultipleTabsArePersisted() throws Exception {
        Path db = tempDir.resolve("conversations.db");
        ConversationStorageService storage = new ConversationStorageService(db.toString());

        Project project = mock(Project.class);
        when(project.getLocationHash()).thenReturn("hash-1");

        int tabs = 8;
        ExecutorService pool = Executors.newFixedThreadPool(tabs);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tabs);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int i = 0; i < tabs; i++) {
            String id = String.valueOf(i);
            pool.submit(() -> {
                try {
                    start.await();                 // release all threads at once
                    storage.addConversation(project, newConversation(id));
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(failure.get()).as("no write should fail with SQLITE_BUSY").isNull();
        assertThat(storage.getConversations(project)).hasSize(tabs);
    }
}
