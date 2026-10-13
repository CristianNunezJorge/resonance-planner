package com.resonanceplanner.common.error;

import java.util.Map;

/** The request clashes with the current state, for example an email already in use (409). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    /** The clash is about one field (a username already taken): the message is shown under that input. */
    public ConflictException(String message, String field) {
        super(ErrorCode.CONFLICT, message, Map.of(field, message));
    }
}
