package com.devoxx.genie.service.trace;

/** TruffleHog state of a stored trace turn. */
public enum TraceScanStatus {
    /** Stored with regex redaction only (TruffleHog off, or not installed in {@code auto} mode). */
    NOT_SCANNED,
    /** Scanned, nothing found. */
    CLEAN,
    /** Scanned, findings replaced with {@code [REDACTED_TRUFFLEHOG_<DETECTOR>]}. */
    REDACTED
}
