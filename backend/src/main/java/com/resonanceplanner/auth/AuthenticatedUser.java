package com.resonanceplanner.auth;

import com.resonanceplanner.user.Role;

/**
 * Who is making the request, as read from the JWT. Controllers get it with {@code @AuthenticationPrincipal}:
 * the user always comes from the token and never from the URL (D44).
 */
public record AuthenticatedUser(Long id, Role role) {}
