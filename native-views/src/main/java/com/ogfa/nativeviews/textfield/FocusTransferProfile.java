package com.ogfa.nativeviews.textfield;

import java.util.Objects;

/** Internal state and timing profile for same-frame TextField replacement. */
final class FocusTransferProfile {
    private String pendingFieldId;
    private long startedAtMs;
    private long lastElapsedMs;

    void begin(String fieldId, long nowMs) {
        pendingFieldId = Objects.requireNonNull(fieldId, "Field ID cannot be null.");
        startedAtMs = nowMs;
        lastElapsedMs = 0L;
    }

    boolean consume(String fieldId, long nowMs) {
        if (!Objects.equals(pendingFieldId, fieldId)) return false;
        lastElapsedMs = elapsed(nowMs);
        pendingFieldId = null;
        startedAtMs = 0L;
        return true;
    }

    String expire(long nowMs) {
        if (pendingFieldId == null) return null;
        String expiredFieldId = pendingFieldId;
        lastElapsedMs = elapsed(nowMs);
        pendingFieldId = null;
        startedAtMs = 0L;
        return expiredFieldId;
    }

    boolean hasPending() {
        return pendingFieldId != null;
    }

    String getPendingFieldId() {
        return pendingFieldId;
    }

    long getLastElapsedMs() {
        return lastElapsedMs;
    }

    private long elapsed(long nowMs) {
        return Math.max(0L, nowMs - startedAtMs);
    }
}
