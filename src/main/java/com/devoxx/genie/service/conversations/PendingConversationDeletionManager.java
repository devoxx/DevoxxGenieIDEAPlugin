package com.devoxx.genie.service.conversations;

import com.devoxx.genie.model.conversation.Conversation;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Undo-deferred conversation deletion (task-236). Deleting a conversation from the history
 * popup does <em>not</em> remove the SQLite row immediately. Instead the conversation id is
 * parked in a pending-deletion set and the actual {@link ConversationStorageService} delete
 * is committed only after an undo grace period. Within that window {@link #undo(String)}
 * cancels the scheduled commit and the conversation reappears untouched.
 *
 * <p>This pending-set pattern deliberately avoids delete-then-re-insert on undo: re-inserting
 * would have to recreate the conversation row plus all chat_messages rows in the right order
 * and could collide with the foreign-key relationship between the two tables.
 *
 * <p>Thread-safety: all state lives in a {@link ConcurrentHashMap}; commit and undo race
 * safely because both use atomic {@code remove(id)} as the single point of arbitration.
 */
@Slf4j
public class PendingConversationDeletionManager {

    /** How long a deletion stays undoable before the SQLite row is actually removed. */
    public static final long DEFAULT_UNDO_GRACE_MS = 5_000;

    private static final class Holder {
        private static final PendingConversationDeletionManager INSTANCE =
                new PendingConversationDeletionManager(
                        (project, conversation) ->
                                ConversationStorageService.getInstance().removeConversation(project, conversation),
                        AppExecutorUtil.getAppScheduledExecutorService(),
                        DEFAULT_UNDO_GRACE_MS);
    }

    public static @NotNull PendingConversationDeletionManager getInstance() {
        return Holder.INSTANCE;
    }

    private record PendingDeletion(@NotNull Conversation conversation,
                                   @NotNull ScheduledFuture<?> commitFuture) {
    }

    private final BiConsumer<Project, Conversation> deletionCommitter;
    private final ScheduledExecutorService scheduler;
    private final long undoGraceMs;
    private final Map<String, PendingDeletion> pending = new ConcurrentHashMap<>();

    /**
     * Visible for tests: inject a mock committer, a controllable scheduler, and a short
     * grace period. Production code uses {@link #getInstance()}.
     */
    PendingConversationDeletionManager(@NotNull BiConsumer<Project, Conversation> deletionCommitter,
                                       @NotNull ScheduledExecutorService scheduler,
                                       long undoGraceMs) {
        this.deletionCommitter = deletionCommitter;
        this.scheduler = scheduler;
        this.undoGraceMs = undoGraceMs;
    }

    /** Schedules {@code conversation} for deletion after the undo grace period. */
    public void scheduleDeletion(@NotNull Project project, @NotNull Conversation conversation) {
        scheduleDeletion(project, conversation, () -> { });
    }

    /**
     * Schedules {@code conversation} for deletion after the undo grace period.
     * Scheduling the same conversation again while a deletion is already pending is a no-op
     * (the first timer keeps running), so a double Delete-press cannot double-commit.
     *
     * @param onCommitted invoked (on the scheduler thread) once the deletion has actually
     *                    been committed — even when the storage delete itself failed — so
     *                    callers can expire the undo notification and refresh their view
     */
    public void scheduleDeletion(@NotNull Project project,
                                 @NotNull Conversation conversation,
                                 @NotNull Runnable onCommitted) {
        String id = conversation.getId();
        pending.computeIfAbsent(id, key -> {
            ScheduledFuture<?> future = scheduler.schedule(
                    () -> commit(project, key, onCommitted), undoGraceMs, TimeUnit.MILLISECONDS);
            log.debug("Scheduled deletion of conversation {} in {} ms", key, undoGraceMs);
            return new PendingDeletion(conversation, future);
        });
    }

    /**
     * Cancels a pending deletion. Returns {@code true} when the conversation was still
     * pending and is now restored; {@code false} when it was unknown or already committed.
     */
    public boolean undo(@NotNull String conversationId) {
        PendingDeletion deletion = pending.remove(conversationId);
        if (deletion == null) {
            return false;
        }
        deletion.commitFuture().cancel(false);
        log.debug("Undid pending deletion of conversation {}", conversationId);
        return true;
    }

    /** True while {@code conversationId} is parked awaiting commit (i.e. still undoable). */
    public boolean isPendingDeletion(@NotNull String conversationId) {
        return pending.containsKey(conversationId);
    }

    /** Snapshot of all conversation ids currently pending deletion — used to filter views. */
    public @NotNull Set<String> pendingIds() {
        return Set.copyOf(pending.keySet());
    }

    private void commit(@NotNull Project project, @NotNull String conversationId, @NotNull Runnable onCommitted) {
        PendingDeletion deletion = pending.remove(conversationId);
        if (deletion == null) {
            // Undone (or already committed) between timer fire and now — nothing to do.
            return;
        }
        try {
            deletionCommitter.accept(project, deletion.conversation());
            log.debug("Committed deletion of conversation {}", conversationId);
        } catch (Exception e) {
            log.warn("Failed to commit deletion of conversation {}: {}", conversationId, e.getMessage());
        } finally {
            try {
                onCommitted.run();
            } catch (Exception e) {
                log.warn("onCommitted callback failed for conversation {}: {}", conversationId, e.getMessage());
            }
        }
    }
}
