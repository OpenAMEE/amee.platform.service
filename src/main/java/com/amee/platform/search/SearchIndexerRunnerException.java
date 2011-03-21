package com.amee.platform.search;

public class SearchIndexerRunnerException extends Exception {

    public enum Reason {UNKNOWN, FULL, DUPLICATE}

    private Reason reason = Reason.UNKNOWN;

    public SearchIndexerRunnerException() {
        super();
    }

    public SearchIndexerRunnerException(Reason reason) {
        this();
        this.reason = reason;
    }

    public boolean isReasonFull() {
        return reason.equals(Reason.FULL);
    }

    public boolean isReasonDuplicate() {
        return reason.equals(Reason.DUPLICATE);
    }
}