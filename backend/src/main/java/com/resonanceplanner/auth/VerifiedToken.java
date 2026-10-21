package com.resonanceplanner.auth;

import java.time.Instant;

/** A valid token: its user, and the moment it stops being valid (the WebSocket of D135 closes the connection then). */
public record VerifiedToken(AuthenticatedUser user, Instant expiresAt) {}
