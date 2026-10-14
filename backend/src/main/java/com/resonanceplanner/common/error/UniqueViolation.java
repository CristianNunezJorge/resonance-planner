package com.resonanceplanner.common.error;

import java.sql.SQLException;

/**
 * Tells a duplicate that two requests created at the same moment from the other errors of the database. The check of
 * the service comes first; this catches the request that passed it a moment before the other one was saved.
 */
public final class UniqueViolation {

    // PostgreSQL answers 23505 for a unique index; any other integrity error is a bug that must not look like a duplicate
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private UniqueViolation() {}

    /** True if the error, or one of its causes, is a violation of a unique index. */
    public static boolean isIn(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION_SQL_STATE.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
