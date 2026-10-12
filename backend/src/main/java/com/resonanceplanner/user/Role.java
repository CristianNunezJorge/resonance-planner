package com.resonanceplanner.user;

/** Role of an account (D19). It is saved on the user and copied into the JWT. */
public enum Role {
    USER,
    ADMIN,
    // Demo guest of the v3 extra (D15, X02): already allowed by the users table
    GUEST
}
