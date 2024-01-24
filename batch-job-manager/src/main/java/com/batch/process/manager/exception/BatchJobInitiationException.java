package com.batch.process.manager.exception;

public class BatchJobInitiationException extends Exception {
    private static final long serialVersionUID = 1L;

    public BatchJobInitiationException(String message) {
        super(message);
    }

    public BatchJobInitiationException(String message, Throwable e) {
        super(message, e);
    }
}
