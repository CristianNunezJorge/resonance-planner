package com.resonanceplanner.common.error;

import java.util.Map;

/**
 * Base class of the errors that services throw on purpose. The message is shown to the user,
 * so it must be in Italian and must not contain internal details.
 * An error can name the field it is about: the frontend then shows the message under that input (D46).
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, String> fields;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, String> fields) {
        super(message);
        this.errorCode = errorCode;
        this.fields = Map.copyOf(fields);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** The fields this error is about, with the message for each; empty if it is not about a field. */
    public Map<String, String> getFields() {
        return fields;
    }
}
