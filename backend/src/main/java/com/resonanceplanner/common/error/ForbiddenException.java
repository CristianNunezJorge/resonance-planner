package com.resonanceplanner.common.error;

/**
 * The user of the token exists but may not do this (403): for example a token with the ADMIN role whose account is
 * not ADMIN anymore. Spring Security checks the role written in the token; the services check the database (D102).
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage());
    }
}
