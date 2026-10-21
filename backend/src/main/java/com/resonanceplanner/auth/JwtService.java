package com.resonanceplanner.auth;

import com.resonanceplanner.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and reads the JWT of the login (D45, D99). It is the only class that knows the JWT library,
 * so replacing the library touches this file only.
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    // Fixed by D45; there is no refresh token
    static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(String secret, Clock clock) {
        // The secret is plain text, not base64: the key is made of its UTF-8 bytes (256 bits for 32 characters)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String generateToken(Long userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY)))
                // Always HS256: without it the library would pick HS384 or HS512 for a longer secret
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** The user of a valid token; empty if the token is missing, forged, expired or has unexpected content. */
    public Optional<AuthenticatedUser> parse(String token) {
        return verify(token).map(VerifiedToken::user);
    }

    /** Like {@link #parse}, with the moment the token expires. */
    public Optional<VerifiedToken> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();
            // Only what we write is accepted: HS256 and an expiry date, so a token can never be valid forever
            if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm()) || claims.getExpiration() == null) {
                return Optional.empty();
            }
            String role = claims.get(ROLE_CLAIM, String.class);
            if (role == null) {
                return Optional.empty();
            }
            AuthenticatedUser user = new AuthenticatedUser(Long.valueOf(claims.getSubject()), Role.valueOf(role));
            return Optional.of(new VerifiedToken(user, claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            // Not an error: an expired or garbled token only means "no user". The token itself is never logged.
            log.debug("JWT rejected: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
