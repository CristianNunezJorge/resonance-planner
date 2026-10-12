package com.resonanceplanner.common.error;

/**
 * The token is valid but its user does not exist anymore, for example after the deletion of the account (401).
 * The token stays valid until it expires (D45), so the services of the personal area must check the user.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage());
    }
}
