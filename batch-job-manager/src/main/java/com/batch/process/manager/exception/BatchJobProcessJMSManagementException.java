package com.batch.process.manager.exception;

public class BatchJobProcessJMSManagementException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BatchJobProcessJMSManagementException(String message) {
        super(message);
    }

    public BatchJobProcessJMSManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
