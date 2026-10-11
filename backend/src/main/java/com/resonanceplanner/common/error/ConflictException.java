package com.resonanceplanner.common.error;

/** The request clashes with the current state, for example an email already in use (409). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
