package com.resonanceplanner.common.error;

/**
 * Base class of the errors that services throw on purpose. The message is shown to the user,
 * so it must be in Italian and must not contain internal details.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
