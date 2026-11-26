package com.devoxx.genie.service.trace;

import com.devoxx.genie.service.trace.TruffleHogScanner.Finding;
import com.devoxx.genie.service.trace.TruffleHogScanner.FindingStatus;
import com.devoxx.genie.service.trace.TruffleHogScanner.ScanResult;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTraceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(0), ZoneOffset.UTC);

    private record Written(String conversationId, String turnId, List<TraceEvent> events, TraceScanStatus status) {}

    /** Scanner stub: availability and the scan result are set per test. */
    private static final class StubScanner extends TruffleHogScanner {
        boolean available = true;
        ScanResult result;
        final AtomicInteger availabilityChecks = new AtomicInteger();
        final AtomicInteger scans = new AtomicInteger();

        @Override
        public boolean isAvailable(@NotNull String binary) {
            availabilityChecks.incrementAndGet();
            return available;
        }

        @Override
        public @NotNull ScanResult scan(@NotNull List<TraceEvent> events, @NotNull String binary, boolean verify) {
            scans.incrementAndGet();
            return result != null ? result : new ScanResult.Completed(events, List.of(), true);
        }
    }

    private final List<Written> written = new ArrayList<>();
    private final List<String> notices = new ArrayList<>();
    private AgentTraceBuffer buffer;
    private StubScanner scanner;
    private AgentTraceService service;

    @BeforeEach
    void setUp() {
        buffer = new AgentTraceBuffer(CLOCK);
        scanner = new StubScanner();
        AgentTraceService.Notifier notifier = new AgentTraceService.Notifier() {
            @Override
            public void truffleHogRequiredButMissing(@Nullable Project project, @NotNull String binary) {
                notices.add("missing:" + binary);
            }

            @Override
            public void truffleHogRequiredScanFailed(@Nullable Project project, @NotNull String reason) {
                notices.add("failed:" + reason);
            }
        };
        // Direct executor: process synchronously so the test can assert right away.
        service = new AgentTraceService(buffer,
                (c, t, e, s) -> written.add(new Written(c, t, e, s)), scanner, Runnable::run, notifier, CLOCK);
    }

    private static TraceSettings settings(TruffleHogMode mode, TruffleHogOnFindings onFindings, int maxChars) {
        return new TraceSettings(true, maxChars, false, List.of(), mode, onFindings, "", false);
    }

    private void recordTurn(String key, TraceSettings settings, TraceEvent... events) {
        buffer.startTurn(key, UUID.randomUUID(), settings);
        for (TraceEvent e : events) {
            buffer.append(key, e);
        }
    }

    @Test
    void noRecordedTurn_writesNothing() {
        service.persistTurn(null, "k", "conv-1");
        assertThat(written).isEmpty();
    }

    @Test
    void truffleHogOff_writesRegexRedactedTurn_asNotScanned() {
        recordTurn("k", settings(TruffleHogMode.OFF, TruffleHogOnFindings.REDACT, 0),
                TraceEvent.user("my key is AKIA" + "ABCDEFGHIJKLMNOP", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).singleElement().satisfies(w -> {
            assertThat(w.conversationId()).isEqualTo("conv-1");
            assertThat(w.status()).isEqualTo(TraceScanStatus.NOT_SCANNED);
            assertThat(w.events().get(0).content()).isEqualTo("my key is [REDACTED_AWS_ACCESS_KEY]");
        });
        assertThat(scanner.scans).hasValue(0);
        assertThat(buffer.hasTurn("k")).isFalse();
    }

    @Test
    void longToolResults_areTruncatedWithMarker() {
        recordTurn("k", settings(TruffleHogMode.OFF, TruffleHogOnFindings.REDACT, 5),
                TraceEvent.tool("c", "read_file", "0123456789", false, null, 1),
                TraceEvent.assistant("assistant text is never truncated", null, List.of(), "m", null, 2));

        service.persistTurn(null, "k", "conv-1");

        List<TraceEvent> events = written.get(0).events();
        assertThat(events.get(0).content()).isEqualTo("01234\n…[truncated 5 characters]");
        assertThat(events.get(1).content()).isEqualTo("assistant text is never truncated");
    }

    @Test
    void auto_withoutBinary_writesNotScanned_withoutNotifying() {
        scanner.available = false;
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("hi", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).singleElement().extracting(Written::status).isEqualTo(TraceScanStatus.NOT_SCANNED);
        assertThat(notices).isEmpty();
    }

    @Test
    void required_withoutBinary_writesNothing_andNotifiesOnce() {
        scanner.available = false;
        TraceSettings required = settings(TruffleHogMode.REQUIRED, TruffleHogOnFindings.REDACT, 0);

        recordTurn("k", required, TraceEvent.user("one", null, 1));
        service.persistTurn(null, "k", "conv-1");
        recordTurn("k", required, TraceEvent.user("two", null, 2));
        service.persistTurn(null, "k", "conv-1");

        assertThat(written).isEmpty();
        assertThat(notices).containsExactly("missing:trufflehog");
    }

    @Test
    void availabilityCheck_isCached() {
        TraceSettings auto = settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0);
        recordTurn("k", auto, TraceEvent.user("one", null, 1));
        service.persistTurn(null, "k", "c");
        recordTurn("k", auto, TraceEvent.user("two", null, 2));
        service.persistTurn(null, "k", "c");

        assertThat(scanner.availabilityChecks).hasValue(1);
        assertThat(scanner.scans).hasValue(2);
    }

    @Test
    void cleanScan_writesClean() {
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("hi", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).singleElement().extracting(Written::status).isEqualTo(TraceScanStatus.CLEAN);
    }

    @Test
    void findings_withRedact_writesScannerOutput_asRedacted() {
        List<TraceEvent> redacted = List.of(TraceEvent.user("[REDACTED_TRUFFLEHOG_X]", null, 1));
        scanner.result = new ScanResult.Completed(redacted,
                List.of(new Finding("X", FindingStatus.UNVERIFIED, "s3cr3t", "***")), true);
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("s3cr3t", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).singleElement().satisfies(w -> {
            assertThat(w.status()).isEqualTo(TraceScanStatus.REDACTED);
            assertThat(w.events()).isEqualTo(redacted);
        });
    }

    @Test
    void findings_withSkip_dropsTheTurn() {
        scanner.result = new ScanResult.Completed(List.of(),
                List.of(new Finding("X", FindingStatus.UNVERIFIED, "s", "*")), true);
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.SKIP, 0), TraceEvent.user("s", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).isEmpty();
    }

    @Test
    void findingThatCannotBeRemoved_dropsTheTurn() {
        scanner.result = new ScanResult.Completed(List.of(TraceEvent.user("s", null, 1)),
                List.of(new Finding("X", FindingStatus.UNVERIFIED, null, "[REDACTED]")), false);
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("s", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).isEmpty();
    }

    @Test
    void failedScan_inAuto_fallsBackToRegexOnly() {
        scanner.result = new ScanResult.Failed("trufflehog exited with code 2");
        recordTurn("k", settings(TruffleHogMode.AUTO, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("hi", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).singleElement().extracting(Written::status).isEqualTo(TraceScanStatus.NOT_SCANNED);
    }

    @Test
    void failedScan_inRequired_dropsTheTurn_andNotifies() {
        scanner.result = new ScanResult.Failed("trufflehog timed out after 300s");
        recordTurn("k", settings(TruffleHogMode.REQUIRED, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("hi", null, 1));

        service.persistTurn(null, "k", "conv-1");

        assertThat(written).isEmpty();
        assertThat(notices).containsExactly("failed:trufflehog timed out after 300s");
    }

    @Test
    void writerFailure_isSwallowed() {
        AgentTraceService failing = new AgentTraceService(buffer, (c, t, e, s) -> {
            throw new IllegalStateException("disk full");
        }, scanner, Runnable::run, new AgentTraceService.Notifier() {
            @Override
            public void truffleHogRequiredButMissing(@Nullable Project project, @NotNull String binary) {}

            @Override
            public void truffleHogRequiredScanFailed(@Nullable Project project, @NotNull String reason) {}
        }, CLOCK);
        recordTurn("k", settings(TruffleHogMode.OFF, TruffleHogOnFindings.REDACT, 0), TraceEvent.user("hi", null, 1));

        failing.persistTurn(null, "k", "conv-1"); // must not throw
    }
}
