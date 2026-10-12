package com.resonanceplanner.user;

/** The shape of a username (D100), shared by the registration and the profile. */
public final class UsernameRules {

    // Letters of any alphabet, digits, dot, hyphen and underscore: no spaces, control or invisible characters
    public static final String PATTERN = "^[\\p{L}\\p{N}_.-]{3,30}$";
    public static final String MESSAGE = "Usa da 3 a 30 caratteri tra lettere, numeri, punto, trattino e underscore.";

    private UsernameRules() {}
}
