package com.resonanceplanner.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The profile API through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "S3cret-pass";

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsers() {
        // A real BCrypt hash, so a login with the password really works while the account exists
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", passwordEncoder.encode(PASSWORD), Role.USER));
    }

    @Test
    void worksForEveryRoleOfTheTokens() throws Exception {
        User admin = userRepository.saveAndFlush(User.create("the.admin", "boss@example.com", "hash", Role.ADMIN));
        User guest = userRepository.saveAndFlush(User.create("a.guest", "guest@example.com", "hash", Role.GUEST));

        mockMvc.perform(as(admin, get("/api/me"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"));
        mockMvc.perform(as(guest, get("/api/me"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("GUEST"));
        mockMvc.perform(as(guest, patchWith("{\"playTimeMinutes\":30}"))).andExpect(status().isOk());
    }

    @Test
    void treatsAnExplicitNullLikeAMissingField() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":null,\"playTimeMinutes\":null}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rover"))
                .andExpect(jsonPath("$.data.playTimeMinutes").doesNotExist());
    }

    @Test
    void answersWithTheProfileOfTheUserOfTheToken() throws Exception {
        mockMvc.perform(as(rover, get("/api/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(rover.getId()))
                .andExpect(jsonPath("$.data.username").value("rover"))
                .andExpect(jsonPath("$.data.email").value("rover@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.playTimeMinutes").doesNotExist())
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void changesTheUsername() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"rover.new\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rover.new"))
                .andExpect(jsonPath("$.data.playTimeMinutes").doesNotExist());

        assertThat(usernameOf(rover)).isEqualTo("rover.new");
    }

    @Test
    void changesThePlayTimeToOneOfTheOptionsOfTheGameRules() throws Exception {
        for (int minutes : new int[] {30, 60, 120}) {
            mockMvc.perform(as(rover, patchWith("{\"playTimeMinutes\":" + minutes + "}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.playTimeMinutes").value(minutes))
                    .andExpect(jsonPath("$.data.username").value("rover"));
        }

        assertThat(jdbc.queryForObject("SELECT play_time_minutes FROM users WHERE id = ?", Integer.class, rover.getId()))
                .isEqualTo(120);
    }

    @Test
    void changesBothFieldsInOneRequest() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"roverino\",\"playTimeMinutes\":60}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("roverino"))
                .andExpect(jsonPath("$.data.playTimeMinutes").value(60));
    }

    @Test
    void leavesEverythingAsItIsWhenNothingIsSent() throws Exception {
        mockMvc.perform(as(rover, patchWith("{}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("rover"));
    }

    @Test
    void refusesAPlayTimeThatIsNotAnOptionOfTheGameRules() throws Exception {
        for (String value : new String[] {"45", "0", "-30", "1000"}) {
            mockMvc.perform(as(rover, patchWith("{\"playTimeMinutes\":" + value + "}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fields.playTimeMinutes")
                            .value("Scegli un tempo di gioco tra 30, 60 o 120 minuti."));
        }
    }

    @Test
    void savesNothingWhenOneOfTheFieldsIsNotValid() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"changed\",\"playTimeMinutes\":45}")))
                .andExpect(status().isBadRequest());

        assertThat(usernameOf(rover)).isEqualTo("rover");
    }

    @Test
    void appliesTheRulesOfTheRegistrationToTheUsername() throws Exception {
        for (String username : new String[] {"ab", "ro ver", " rover", "rover\\u200B", "a".repeat(31)}) {
            mockMvc.perform(as(rover, patchWith("{\"username\":\"" + username + "\"}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.username")
                            .value("Usa da 3 a 30 caratteri tra lettere, numeri, punto, trattino e underscore."));
        }
    }

    @Test
    void refusesAUsernameOfSomeoneElseAndTheUsernameOfTheAdmin() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"other\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Questo username è già in uso."))
                .andExpect(jsonPath("$.error.fields.username").value("Questo username è già in uso."));
        mockMvc.perform(as(rover, patchWith("{\"username\":\"ADMIN\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("Questo username è già in uso."))
                .andExpect(jsonPath("$.error.fields.username").value("Questo username è già in uso."));

        assertThat(usernameOf(rover)).isEqualTo("rover");
    }

    @Test
    void acceptsTheUsernameThatTheUserAlreadyHas() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"rover\",\"playTimeMinutes\":30}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playTimeMinutes").value(30));
    }

    @Test
    void ignoresTheFieldsThatCannotBeChanged() throws Exception {
        mockMvc.perform(as(rover, patchWith("""
                {"id":999999,"email":"hacker@example.com","role":"ADMIN","onboardingCompleted":true,"passwordHash":"x"}""")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(rover.getId()))
                .andExpect(jsonPath("$.data.email").value("rover@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false));

        assertThat(jdbc.queryForObject("SELECT role || '|' || email || '|' || password_hash FROM users WHERE id = ?",
                String.class, rover.getId())).isEqualTo("USER|rover@example.com|" + rover.getPasswordHash());
    }

    @Test
    void touchesOnlyTheUserOfTheToken() throws Exception {
        mockMvc.perform(as(rover, patchWith("{\"username\":\"changed\",\"playTimeMinutes\":120}")))
                .andExpect(status().isOk());

        assertThat(usernameOf(other)).isEqualTo("other");
        assertThat(jdbc.queryForObject("SELECT play_time_minutes FROM users WHERE id = ?", Integer.class, other.getId()))
                .isNull();
    }

    @Test
    void deletesTheAccountAndEverythingLinkedToItButNothingOfOtherUsers() throws Exception {
        fillWithLinkedData(rover);
        fillWithLinkedData(other);

        mockMvc.perform(as(rover, delete("/api/me")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        for (String table : new String[] {"owned_character", "inventory_item", "plan_proposal", "planned_task",
                "agent_message"}) {
            assertThat(countFor(table, rover)).as("%s of the deleted user", table).isZero();
            assertThat(countFor(table, other)).as("%s of the other user", table).isPositive();
        }
        assertThat(userRepository.existsById(rover.getId())).isFalse();
        assertThat(userRepository.existsById(other.getId())).isTrue();
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldTokenOrThePassword() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        // The password really works before the deletion: without this the check after it would prove nothing
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rover@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // The token is still valid until it expires (D45), but its user is gone: 401 and not a database error
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {get("/api/me"),
                patchWith("{\"username\":\"again\"}"), delete("/api/me")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rover@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void letsTheEmailBeRegisteredAgainAfterTheDeletion() throws Exception {
        mockMvc.perform(as(rover, delete("/api/me"))).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rover\",\"email\":\"rover@example.com\",\"password\":\"S3cret-pass\"}"))
                .andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }

    private static MockHttpServletRequestBuilder patchWith(String json) {
        return patch("/api/me").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String usernameOf(User user) {
        return jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, user.getId());
    }

    private int countFor(String table, User user) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE user_id = ?", Integer.class, user.getId());
    }

    // One row in each table that depends on the user, including the links that point to a proposal (they have no
    // cascade of their own: they must disappear in the same delete)
    private void fillWithLinkedData(User user) {
        jdbc.update("INSERT INTO owned_character (user_id, character_id, current_level, current_rank, target_level, "
                + "target_rank) VALUES (?, 'jinhsi', 1, 0, 40, 2)", user.getId());
        jdbc.update("INSERT INTO inventory_item (user_id, material_id, quantity) VALUES (?, 'shell-credit', 100)",
                user.getId());
        Long proposalId = jdbc.queryForObject("INSERT INTO plan_proposal (user_id, status, summary) "
                + "VALUES (?, 'PENDING', 'A plan') RETURNING id", Long.class, user.getId());
        jdbc.update("INSERT INTO planned_task (user_id, proposal_id, origin, kind, status, task_date, title, minutes) "
                + "VALUES (?, ?, 'AGENT', 'CUSTOM', 'PROPOSED', ?, 'Proposed activity', 30)",
                user.getId(), proposalId, LocalDate.of(2026, 10, 20));
        jdbc.update("INSERT INTO planned_task (user_id, origin, kind, status, task_date, title, minutes) "
                + "VALUES (?, 'MANUAL', 'CUSTOM', 'CONFIRMED', ?, 'My activity', 15)", user.getId(), LocalDate.of(2026, 10, 21));
        jdbc.update("INSERT INTO agent_message (user_id, role, content, proposal_id) VALUES (?, 'USER', 'Hello', NULL)",
                user.getId());
        jdbc.update("INSERT INTO agent_message (user_id, role, content, proposal_id) VALUES (?, 'ASSISTANT', 'A plan', ?)",
                user.getId(), proposalId);
    }
}
