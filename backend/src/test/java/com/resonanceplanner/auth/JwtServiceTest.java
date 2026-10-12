package com.resonanceplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.user.Role;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// Pure unit test: no Spring context, the clock is fixed so the 24 hours of D45 are checked exactly
class JwtServiceTest {

    // 32 characters: the shortest secret AppProperties accepts (256 bits)
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");

    @Test
    void readsBackTheUserAndTheRoleOfAGeneratedToken() {
        String token = serviceAt(NOW).generateToken(42L, Role.ADMIN);

        assertThat(serviceAt(NOW).parse(token)).contains(new AuthenticatedUser(42L, Role.ADMIN));
    }

    @Test
    void acceptsATokenForTwentyFourHoursAndNotAMomentLonger() {
        String token = serviceAt(NOW).generateToken(7L, Role.USER);

        assertThat(serviceAt(NOW.plus(Duration.ofHours(24)).minusSeconds(1)).parse(token)).isPresent();
        assertThat(serviceAt(NOW.plus(Duration.ofHours(24)).plusSeconds(1)).parse(token)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        JwtService other = new JwtService("another-secret-of-at-least-32-chars!!", clockAt(NOW));

        assertThat(serviceAt(NOW).parse(other.generateToken(1L, Role.ADMIN))).isEmpty();
    }

    @Test
    void rejectsATokenWhoseContentWasChanged() {
        String[] parts = serviceAt(NOW).generateToken(1L, Role.USER).split("\\.");
        String forgedPayload = base64Url("{\"sub\":\"1\",\"role\":\"ADMIN\",\"exp\":" + NOW.plusSeconds(3600).getEpochSecond() + "}");

        assertThat(serviceAt(NOW).parse(parts[0] + "." + forgedPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void rejectsATokenWithoutSignature() {
        String unsigned = Jwts.builder().subject("1").claim("role", "ADMIN")
                .expiration(Date.from(NOW.plusSeconds(3600))).compact();

        assertThat(serviceAt(NOW).parse(unsigned)).isEmpty();
    }

    @Test
    void rejectsAValidlySignedTokenWithAnUnknownRoleOrASubjectThatIsNotANumber() {
        assertThat(serviceAt(NOW).parse(signed("1", "SUPERUSER"))).isEmpty();
        assertThat(serviceAt(NOW).parse(signed("not-a-number", "USER"))).isEmpty();
    }

    @Test
    void rejectsAValidlySignedTokenThatNeverExpires() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String forever = Jwts.builder().subject("1").claim("role", "ADMIN").issuedAt(Date.from(NOW)).signWith(key).compact();

        assertThat(serviceAt(NOW).parse(forever)).isEmpty();
    }

    @Test
    void rejectsAValidlySignedTokenThatUsesAnotherHmacAlgorithm() {
        // With a 64-byte secret the key also allows HS512: only the HS256 we write is accepted
        String longSecret = SECRET.repeat(2);
        SecretKey key = new SecretKeySpec(longSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        String hs512 = Jwts.builder().subject("1").claim("role", "ADMIN")
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(3600)))
                .signWith(key, Jwts.SIG.HS512).compact();

        assertThat(new JwtService(longSecret, clockAt(NOW)).parse(hs512)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "garbage", "a.b.c", "a.b.c.d"})
    void rejectsAnythingThatIsNotAToken(String value) {
        assertThat(serviceAt(NOW).parse(value)).isEmpty();
    }

    @Test
    void signsWithHs256EvenWhenTheSecretIsLonger() {
        String longSecret = SECRET.repeat(2);
        JwtService service = new JwtService(longSecret, clockAt(NOW));

        String header = new String(Base64.getUrlDecoder().decode(service.generateToken(1L, Role.USER).split("\\.")[0]),
                StandardCharsets.UTF_8);

        assertThat(header).contains("\"alg\":\"HS256\"");
    }

    private static JwtService serviceAt(Instant instant) {
        return new JwtService(SECRET, clockAt(instant));
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    // A token that is correctly signed with our secret but has content our code never writes
    private static String signed(String subject, String role) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return Jwts.builder().subject(subject).claim("role", role)
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(3600)))
                .signWith(key).compact();
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
