package com.resonanceplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.resonanceplanner.user.Role;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Registration and login through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    private static final String PASSWORD = "S3cret-pass";
    private static final String GUEST = "/api/auth/guest";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void registersAUserAndAnswersWithTheTokenAndNoPassword() throws Exception {
        ResultActions result = register("rover", "Rover@Example.COM", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("rover"))
                .andExpect(jsonPath("$.data.user.email").value("rover@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        String body = result.andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.user.id");
        String token = JsonPath.read(body, "$.data.token");
        assertThat(jwtService.parse(token)).contains(new AuthenticatedUser(id.longValue(), Role.USER));
    }

    @Test
    void storesTheEmailInLowercaseAndOnlyTheHashOfThePassword() throws Exception {
        register("rover", "Rover@Example.COM", PASSWORD).andExpect(status().isCreated());

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = 'rover@example.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void refusesAnEmailAlreadyRegisteredEvenWithDifferentLetterCase() throws Exception {
        register("rover", "rover@example.com", PASSWORD).andExpect(status().isCreated());

        register("other", "ROVER@EXAMPLE.COM", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Questa email è già registrata."))
                .andExpect(jsonPath("$.error.fields.email").value("Questa email è già registrata."));
    }

    @Test
    void refusesAUsernameAlreadyInUse() throws Exception {
        register("rover", "rover@example.com", PASSWORD).andExpect(status().isCreated());

        register("rover", "other@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Questo username è già in uso."))
                .andExpect(jsonPath("$.error.fields.username").value("Questo username è già in uso."));
    }

    @Test
    void reportsEveryInvalidFieldOfTheRegistrationUnderItsName() throws Exception {
        register("ab", "not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.username")
                        .value("Usa da 3 a 30 caratteri tra lettere, numeri, punto, trattino e underscore."))
                .andExpect(jsonPath("$.error.fields.email").value("Inserisci un indirizzo email valido."))
                .andExpect(jsonPath("$.error.fields.password").value("Deve avere almeno 8 caratteri."));
    }

    @Test
    void refusesEmptyRegistrationFields() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.username").value("Inserisci lo username."))
                .andExpect(jsonPath("$.error.fields.email").value("Inserisci l'email."))
                .andExpect(jsonPath("$.error.fields.password").value("Inserisci la password."));
    }

    @Test
    void refusesUsernamesWithSpacesInvisibleOrControlCharacters() throws Exception {
        // The JSON escapes are written out, so the server receives the real character: no space, no no-break space,
        // no zero-width space, no right-to-left override, no NUL (PostgreSQL cannot store it)
        for (String username : new String[] {" rover", "rover ", "ro ver", "rover\\u00A0", "rover\\u200B",
                "rover\\u202E", "rover\\u0000", "a".repeat(31)}) {
            register(username, "rover@example.com", PASSWORD)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.username").exists());
        }
    }

    @Test
    void acceptsUsernamesWithLettersOfAnyAlphabetDigitsAndTheAllowedPunctuation() throws Exception {
        register("Rover_1.x-y", "first@example.com", PASSWORD).andExpect(status().isCreated());
        register("Ровер", "second@example.com", PASSWORD).andExpect(status().isCreated());
        register("a".repeat(30), "third@example.com", PASSWORD).andExpect(status().isCreated());
    }

    @Test
    void refusesToRegisterTheIdentityOfTheAdmin() throws Exception {
        register("Admin", "rover@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Questo username è già in uso."))
                .andExpect(jsonPath("$.error.fields.username").value("Questo username è già in uso."));
        register("rover", "ADMIN@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Questa email è già registrata."))
                .andExpect(jsonPath("$.error.fields.email").value("Questa email è già registrata."));
    }

    @Test
    void stopsAHugeJsonStringBeforeItIsReadIntoMemory() throws Exception {
        // 20,001 characters: Jackson refuses the body while reading it, so there are no validation fields at all.
        // Without the limit of application.yml the request would reach the validation and report fields.password.
        register("rover", "rover@example.com", "a".repeat(20_001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields").doesNotExist());
    }

    @Test
    void acceptsAPasswordMadeOfSpacesAtLogin() throws Exception {
        String spaces = " ".repeat(8);
        register("rover", "rover@example.com", spaces).andExpect(status().isCreated());

        login("rover@example.com", spaces).andExpect(status().isOk());
    }

    @Test
    void refusesALoginPasswordLongerThan72Bytes() throws Exception {
        login("rover@example.com", "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.password").value("La password è troppo lunga (massimo 72 byte)."));
    }

    @Test
    void acceptsAPasswordOfExactly72BytesAndRefusesOneByteMore() throws Exception {
        register("rover", "rover@example.com", "a".repeat(72)).andExpect(status().isCreated());

        register("other", "other@example.com", "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.password").value("La password è troppo lunga (massimo 72 byte)."));
    }

    @Test
    void countsBytesAndNotCharactersForThePasswordLimit() throws Exception {
        // 20 emoji are 40 Java characters but 80 bytes: BCrypt would throw, the user must get a 400
        register("rover", "rover@example.com", "😀".repeat(20))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.password").exists());
    }

    @Test
    void logsInWithEmailInAnyLetterCaseAndReturnsTheTokenAndTheUser() throws Exception {
        register("rover", "rover@example.com", PASSWORD).andExpect(status().isCreated());

        ResultActions result = login("ROVER@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("rover"));

        String token = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.token");
        assertThat(jwtService.parse(token)).hasValueSatisfying(user -> assertThat(user.role()).isEqualTo(Role.USER));
    }

    @Test
    void givesTheSameAnswerForAWrongPasswordAndAnUnknownEmail() throws Exception {
        register("rover", "rover@example.com", PASSWORD).andExpect(status().isCreated());

        login("rover@example.com", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Email o password non corrette."));
        login("nobody@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Email o password non corrette."));
    }

    @Test
    void refusesALoginWithoutEmailOrPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.email").value("Inserisci l'email."))
                .andExpect(jsonPath("$.error.fields.password").value("Inserisci la password."));
    }

    @Test
    void theTokenOfTheRegistrationOpensAProtectedPath() throws Exception {
        String body = register("rover", "rover@example.com", PASSWORD).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(get("/api/probe/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    // The guest of "Prova la demo" (D134): one click, no body, a temporary account that only the token opens
    @Test
    void createsAGuestWithOneClickAndAnswersWithItsToken() throws Exception {
        String body = mockMvc.perform(post(GUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("GUEST"))
                .andExpect(jsonPath("$.data.user.onboardingCompleted").value(false))
                .andReturn().getResponse().getContentAsString();

        String username = JsonPath.read(body, "$.data.user.username");
        String email = JsonPath.read(body, "$.data.user.email");
        assertThat(username).matches("ospite-[a-z0-9]{6}");
        // A domain reserved for names that can never exist (RFC 2606)
        assertThat(email).matches("[0-9a-f-]{36}@ospite\\.invalid");
        Number id = JsonPath.read(body, "$.data.user.id");
        String token = JsonPath.read(body, "$.data.token");
        assertThat(jwtService.parse(token)).contains(new AuthenticatedUser(id.longValue(), Role.GUEST));
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));
    }

    @Test
    void givesEveryGuestADifferentAccount() throws Exception {
        String first = mockMvc.perform(post(GUEST)).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post(GUEST)).andReturn().getResponse().getContentAsString();

        assertThat((Number) JsonPath.read(first, "$.data.user.id"))
                .isNotEqualTo(JsonPath.read(second, "$.data.user.id"));
        assertThat((String) JsonPath.read(first, "$.data.user.username"))
                .isNotEqualTo(JsonPath.read(second, "$.data.user.username"));
    }

    @Test
    void neverLetsAnyoneLogInAsAGuest() throws Exception {
        String body = mockMvc.perform(post(GUEST)).andReturn().getResponse().getContentAsString();
        String email = JsonPath.read(body, "$.data.user.email");

        // Nobody knows the password: its hash is of a random value, and the login gives the usual answer
        login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"));
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2");
    }

    @Test
    void keepsTheGuestsOutOfTheAdminArea() throws Exception {
        String body = mockMvc.perform(post(GUEST)).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.token");

        mockMvc.perform(get("/api/admin/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void refusesANewGuestWhenFiftyAlreadyExist() throws Exception {
        long existing = jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'GUEST'", Long.class);
        for (long i = existing; i < 50; i++) {
            jdbc.update("INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, 'hash', 'GUEST')",
                    "ospite-test" + i, "test" + i + "@ospite.invalid");
        }

        mockMvc.perform(post(GUEST))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.error.message").value("Troppe prove in corso: riprova più tardi o registrati."))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'GUEST'", Long.class)).isEqualTo(50);
    }

    private ResultActions register(String username, String email, String password) throws Exception {
        return send("/api/auth/register", "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(username, email, password));
    }

    private ResultActions login(String email, String password) throws Exception {
        return send("/api/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private ResultActions send(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content(json.getBytes(StandardCharsets.UTF_8)));
    }
}
