package com.resonanceplanner.common.error;

/** The resource does not exist, or belongs to another user (404, never 403: docs/05-api.md). */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
