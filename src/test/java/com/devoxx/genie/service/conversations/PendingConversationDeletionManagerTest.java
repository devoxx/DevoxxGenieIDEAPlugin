package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for the undo-deferred deletion logic (task-236, AC#7):
 * delete &rarr; undo restores; delete &rarr; timeout actually removes via
 * {@link ConversationStorageService}.
 */
class PendingConversationDeletionManagerTest {

    private static final long GRACE_MS = 100;

    private ScheduledExecutorService scheduler;
    private ConversationStorageService storageService;
    private Project project;
    private PendingConversationDeletionManager manager;

    @BeforeEach
    void setUp() {
        scheduler = new ScheduledThreadPoolExecutor(1);
        storageService = mock(ConversationStorageService.class);
        project = mock(Project.class);
        manager = new PendingConversationDeletionManager(
                storageService::removeConversation, scheduler, GRACE_MS);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    private static Conversation conversation(String id) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setTitle("Conversation " + id);
        return conversation;
    }

    @Test
    void deleteThenUndoRestoresConversationAndNeverTouchesStorage() throws Exception {
        Conversation conversation = conversation("c-1");

        manager.scheduleDeletion(project, conversation);
        assertThat(manager.isPendingDeletion("c-1")).isTrue();

        boolean undone = manager.undo("c-1");

        assertThat(undone).isTrue();
        assertThat(manager.isPendingDeletion("c-1")).isFalse();

        // Wait well past the grace period: the cancelled deletion must never fire.
        Thread.sleep(GRACE_MS * 4);
        verify(storageService, never()).removeConversation(project, conversation);
    }

    @Test
    void deleteThenTimeoutActuallyRemovesFromStorage() throws Exception {
        Conversation conversation = conversation("c-2");
        CountDownLatch committed = new CountDownLatch(1);

        manager.scheduleDeletion(project, conversation, committed::countDown);

        assertThat(committed.await(5, TimeUnit.SECONDS))
                .as("deletion should be committed after the undo grace period")
                .isTrue();
        verify(storageService, times(1)).removeConversation(project, conversation);
        assertThat(manager.isPendingDeletion("c-2")).isFalse();
    }

    @Test
    void undoAfterCommitReturnsFalseAndDoesNotResurrect() throws Exception {
        Conversation conversation = conversation("c-3");
        CountDownLatch committed = new CountDownLatch(1);

        manager.scheduleDeletion(project, conversation, committed::countDown);
        assertThat(committed.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(manager.undo("c-3")).isFalse();
        verify(storageService, times(1)).removeConversation(project, conversation);
    }

    @Test
    void undoOfUnknownConversationReturnsFalse() {
        assertThat(manager.undo("does-not-exist")).isFalse();
    }

    @Test
    void pendingIdsReflectsScheduledDeletionsUntilCommitOrUndo() {
        manager.scheduleDeletion(project, conversation("c-4"));
        manager.scheduleDeletion(project, conversation("c-5"));

        assertThat(manager.pendingIds()).containsExactlyInAnyOrder("c-4", "c-5");

        manager.undo("c-4");
        assertThat(manager.pendingIds()).containsExactly("c-5");
    }

    @Test
    void reschedulingSameConversationCommitsOnlyOnce() throws Exception {
        Conversation conversation = conversation("c-6");
        CountDownLatch committed = new CountDownLatch(1);

        manager.scheduleDeletion(project, conversation, committed::countDown);
        // Delete pressed twice (e.g. double Delete-key press) before the grace expires.
        manager.scheduleDeletion(project, conversation, committed::countDown);

        assertThat(committed.await(5, TimeUnit.SECONDS)).isTrue();
        // Give a potential straggling second commit a chance to (incorrectly) fire.
        Thread.sleep(GRACE_MS * 4);
        verify(storageService, times(1)).removeConversation(project, conversation);
    }

    @Test
    void committerFailureStillClearsPendingState() throws Exception {
        Conversation conversation = conversation("c-7");
        CountDownLatch committed = new CountDownLatch(1);
        PendingConversationDeletionManager failingManager = new PendingConversationDeletionManager(
                (p, c) -> {
                    throw new RuntimeException("boom");
                },
                scheduler,
                GRACE_MS);

        failingManager.scheduleDeletion(project, conversation, committed::countDown);

        assertThat(committed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(failingManager.isPendingDeletion("c-7")).isFalse();
    }
}
