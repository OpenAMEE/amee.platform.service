package com.amee.platform.search;

/**
 * A RuntimeException specific to to {@link LuceneService} implementations.
 */
public class LuceneServiceException extends RuntimeException {

    public LuceneServiceException() {
        super();
    }

    public LuceneServiceException(String message) {
        super(message);
    }

    public LuceneServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LuceneServiceException(Throwable cause) {
        super(cause);
    }
}
