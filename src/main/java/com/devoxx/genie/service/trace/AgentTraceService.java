package com.devoxx.genie.service.trace;

import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import com.devoxx.genie.service.trace.TruffleHogScanner.ScanResult;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persists recorded agent turns (task-279). When {@code ChatService} has saved a conversation,
 * {@link #persistTurn} takes the tab's buffered turn and, on a single background thread (so
 * turns are written in order and the EDT and streaming are never blocked):
 * <ol>
 *   <li>applies regex redaction and tool-result truncation ({@link TraceRedaction});</li>
 *   <li>runs the TruffleHog scan according to the settings, replacing findings or dropping the turn;</li>
 *   <li>writes the result to {@code trace_events}.</li>
 * </ol>
 * Scanning happens <em>before</em> the write, so a secret TruffleHog finds never reaches the
 * database. Failures are logged and never surface in the chat.
 */
@Slf4j
public final class AgentTraceService {

    /** How long a TruffleHog availability check is reused. */
    static final Duration AVAILABILITY_TTL = Duration.ofMinutes(5);

    /** Where processed turns are written; {@link ConversationStorageService#addTraceTurn} in production. */
    @FunctionalInterface
    interface TraceSink {
        void write(@NotNull String conversationId, @NotNull String turnId, @NotNull List<TraceEvent> events,
                   @NotNull TraceScanStatus status);
    }

    /** User-facing notices; a balloon notification in production. */
    interface Notifier {
        void truffleHogRequiredButMissing(@Nullable Project project, @NotNull String binary);

        void truffleHogRequiredScanFailed(@Nullable Project project, @NotNull String reason);
    }

    private static volatile AgentTraceService instance;

    private final AgentTraceBuffer buffer;
    private final TraceSink sink;
    private final TruffleHogScanner scanner;
    private final Executor executor;
    private final Notifier notifier;
    private final Clock clock;
    private final Map<String, Availability> availability = new ConcurrentHashMap<>();
    private final AtomicBoolean missingNotified = new AtomicBoolean(false);
    private final AtomicBoolean failureNotified = new AtomicBoolean(false);

    private record Availability(boolean available, long checkedAtMs) {}

    AgentTraceService(@NotNull AgentTraceBuffer buffer, @NotNull TraceSink sink, @NotNull TruffleHogScanner scanner,
                      @NotNull Executor executor, @NotNull Notifier notifier, @NotNull Clock clock) {
        this.buffer = buffer;
        this.sink = sink;
        this.scanner = scanner;
        this.executor = executor;
        this.notifier = notifier;
        this.clock = clock;
    }

    public static @NotNull AgentTraceService getInstance() {
        AgentTraceService local = instance;
        if (local == null) {
            synchronized (AgentTraceService.class) {
                local = instance;
                if (local == null) {
                    local = new AgentTraceService(
                            AgentTraceBuffer.getInstance(),
                            (conversationId, turnId, events, status) -> {
                                ConversationStorageService.getInstance()
                                        .addTraceTurn(conversationId, turnId, events, status);
                                publishTraceWritten(conversationId);
                            },
                            new TruffleHogScanner(),
                            AppExecutorUtil.createBoundedApplicationPoolExecutor("DevoxxGenie Agent Trace Writer", 1),
                            new BalloonNotifier(),
                            Clock.systemUTC());
                    instance = local;
                }
            }
        }
        return local;
    }

    /**
     * Hands the buffered turn of {@code memoryKey} to the background writer under
     * {@code conversationId}. Returns immediately; does nothing when no turn was recorded
     * (recording off, or a non-agent strategy such as an external CLI runner).
     */
    public void persistTurn(@Nullable Project project, @NotNull String memoryKey, @NotNull String conversationId) {
        AgentTraceBuffer.Turn turn = buffer.take(memoryKey);
        if (turn == null || turn.events().isEmpty()) {
            return;
        }
        executor.execute(() -> {
            try {
                process(project, turn, conversationId);
            } catch (RuntimeException e) {
                log.warn("Failed to persist agent trace for conversation {}: {}", conversationId, e.toString());
            }
        });
    }

    /** Redacts, scans and writes one turn. Synchronous; runs on the writer thread. */
    void process(@Nullable Project project, @NotNull AgentTraceBuffer.Turn turn, @NotNull String conversationId) {
        TraceSettings settings = turn.settings();
        TraceRedactionOptions options = TraceRedaction.optionsFor(settings);
        List<TraceEvent> events = TraceRedaction.apply(turn.events(), settings, options);

        TruffleHogMode mode = settings.truffleHogMode();
        if (mode == TruffleHogMode.OFF) {
            sink.write(conversationId, turn.turnId(), events, TraceScanStatus.NOT_SCANNED);
            return;
        }

        String binary = settings.truffleHogBinary();
        if (!isAvailable(binary)) {
            if (mode == TruffleHogMode.REQUIRED) {
                log.info("Agent trace not recorded: TruffleHog is required but '{}' was not found", binary);
                if (missingNotified.compareAndSet(false, true)) {
                    notifier.truffleHogRequiredButMissing(project, binary);
                }
                return;
            }
            sink.write(conversationId, turn.turnId(), events, TraceScanStatus.NOT_SCANNED);
            return;
        }

        ScanResult result = scanner.scan(events, binary, settings.truffleHogVerify());
        if (result instanceof ScanResult.NotFound notFound) {
            availability.remove(binary);
            handleUnscanned(project, mode, conversationId, turn, events, "trufflehog not found: " + notFound.binary());
        } else if (result instanceof ScanResult.Failed failed) {
            handleUnscanned(project, mode, conversationId, turn, events, failed.reason());
        } else if (result instanceof ScanResult.Completed completed) {
            handleCompleted(settings, conversationId, turn, completed);
        }
    }

    private void handleUnscanned(@Nullable Project project, @NotNull TruffleHogMode mode, @NotNull String conversationId,
                                 @NotNull AgentTraceBuffer.Turn turn, @NotNull List<TraceEvent> events,
                                 @NotNull String reason) {
        if (mode == TruffleHogMode.REQUIRED) {
            log.warn("Agent trace not recorded: TruffleHog scan failed ({})", reason);
            if (failureNotified.compareAndSet(false, true)) {
                notifier.truffleHogRequiredScanFailed(project, reason);
            }
            return;
        }
        log.warn("TruffleHog scan failed ({}); storing agent trace with regex redaction only", reason);
        sink.write(conversationId, turn.turnId(), events, TraceScanStatus.NOT_SCANNED);
    }

    private void handleCompleted(@NotNull TraceSettings settings, @NotNull String conversationId,
                                 @NotNull AgentTraceBuffer.Turn turn, @NotNull ScanResult.Completed completed) {
        if (completed.findings().isEmpty()) {
            sink.write(conversationId, turn.turnId(), completed.events(), TraceScanStatus.CLEAN);
            return;
        }
        // Only masked values are logged (Finding#toString never includes the raw secret).
        log.info("TruffleHog found {} secret(s) in an agent trace turn: {}", completed.findings().size(),
                completed.findings());
        if (completed.findings().stream().anyMatch(f -> f.status() == TruffleHogScanner.FindingStatus.VERIFIED)) {
            log.warn("TruffleHog reported a verified (live) secret in an agent trace; consider rotating it");
        }
        if (settings.truffleHogOnFindings() == TruffleHogOnFindings.SKIP) {
            log.info("Agent trace turn dropped (TruffleHog findings, 'skip' setting)");
            return;
        }
        if (!completed.allRemoved()) {
            log.warn("Agent trace turn dropped: a TruffleHog finding could not be removed safely");
            return;
        }
        sink.write(conversationId, turn.turnId(), completed.events(), TraceScanStatus.REDACTED);
    }

    private static void publishTraceWritten(@NotNull String conversationId) {
        try {
            com.intellij.openapi.application.ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AgentTraceListener.TOPIC).traceWritten(conversationId);
        } catch (RuntimeException e) {
            log.debug("Could not publish traceWritten: {}", e.toString());
        }
    }

    private boolean isAvailable(@NotNull String binary) {
        long now = clock.millis();
        Availability cached = availability.get(binary);
        if (cached != null && now - cached.checkedAtMs() < AVAILABILITY_TTL.toMillis()) {
            return cached.available();
        }
        boolean available = scanner.isAvailable(binary);
        availability.put(binary, new Availability(available, now));
        return available;
    }

    private static final class BalloonNotifier implements Notifier {
        @Override
        public void truffleHogRequiredButMissing(@Nullable Project project, @NotNull String binary) {
            NotificationUtil.sendWarningNotification(project,
                    "Agent trace recording requires TruffleHog, but '" + binary + "' was not found, so no traces "
                            + "are being recorded. Install it from github.com/trufflesecurity/trufflehog "
                            + "(e.g. 'brew install trufflehog'), set its path in Settings → DevoxxGenie → Debug, "
                            + "or change the TruffleHog scan setting to Auto.");
        }

        @Override
        public void truffleHogRequiredScanFailed(@Nullable Project project, @NotNull String reason) {
            NotificationUtil.sendWarningNotification(project,
                    "Agent trace not recorded: the required TruffleHog scan failed (" + reason + ").");
        }
    }
}
