package com.devoxx.genie.service.trace;

import org.jetbrains.annotations.NotNull;

/** A {@link TraceEvent} as read back from {@code trace_events}, with its row id, turn and scan status. */
public record StoredTraceEvent(long rowId, @NotNull String conversationId, @NotNull String turnId,
                               @NotNull TraceEvent event, @NotNull TraceScanStatus scanStatus) {}
