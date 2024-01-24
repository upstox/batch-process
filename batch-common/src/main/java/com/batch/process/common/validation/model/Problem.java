package com.batch.process.common.validation.model;

public class Problem {

    private final String errorCode;

    private final String message;

    private final String propertyPath;

    public Problem(String errorCode, String message, String propertyPath) {
        super();
        this.errorCode = errorCode;
        this.message = message;
        this.propertyPath = propertyPath;
    }

    public Problem(String message, String propertyPath) {
        super();

        if (message != null) {
            String[] messageParts = message.split("-", 2);

            if (messageParts.length > 1) {
                this.errorCode = messageParts[0].trim();
                this.message = messageParts[1].trim();
            } else {
                this.errorCode = "";
                this.message = message.trim();
            }
        } else {
            this.errorCode = "";
            this.message = "";
        }
        this.propertyPath = propertyPath;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    @Override
    public String toString() {
        return "Problem [errorCode=" + errorCode + ", constraintViolation=" + message + ", propertyPath=" + propertyPath
                + "]";
    }
}
