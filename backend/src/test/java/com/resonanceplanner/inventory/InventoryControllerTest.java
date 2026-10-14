package com.resonanceplanner.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The inventory API through the real filter chain and the Docker Compose test database; every test is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InventoryControllerTest {

    private static final String URL = "/api/me/inventory";
    private static final String SHELL_CREDIT = URL + "/shell-credit";
    private static final String HOWLER_CORE = URL + "/lf-howler-core";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsers() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(SHELL_CREDIT).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startsEmptyBecauseTheInventoryStartsFromZero() throws Exception {
        mockMvc.perform(as(rover, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void savesTheQuantityOfAMaterialAndListsIt() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":12000}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materialId").value("shell-credit"))
                .andExpect(jsonPath("$.data.quantity").value(12000));

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].materialId").value("shell-credit"))
                .andExpect(jsonPath("$.data[0].quantity").value(12000));
    }

    @Test
    void correctsTheQuantityWithoutCreatingASecondRow() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":100}"))).andExpect(status().isOk());
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":40}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(40));

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(40));
        assertThat(jdbc.queryForObject("select count(*) from inventory_item where user_id = ?", Integer.class, rover.getId()))
                .isEqualTo(1);
    }

    @Test
    void acceptsZeroAndKeepsTheMaterialInTheList() throws Exception {
        mockMvc.perform(as(rover, putWith(HOWLER_CORE, "{\"quantity\":0}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(0));

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(0));
    }

    @Test
    void listsTheMaterialsInAlphabeticalOrderOfTheirId() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":1}"))).andExpect(status().isOk());
        mockMvc.perform(as(rover, putWith(HOWLER_CORE, "{\"quantity\":2}"))).andExpect(status().isOk());

        mockMvc.perform(as(rover, get(URL)))
                .andExpect(jsonPath("$.data[0].materialId").value("lf-howler-core"))
                .andExpect(jsonPath("$.data[1].materialId").value("shell-credit"));
    }

    @Test
    void acceptsTheHighestQuantityAndRefusesOneMore() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":999999999}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(999999999));

        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":1000000000}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità non può superare 999999999."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-100", "-2147483648"})
    void neverAcceptsANegativeQuantityAndChangesNothing(String quantity) throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":40}"))).andExpect(status().isOk());

        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":" + quantity + "}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità non può essere negativa."));

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data[0].quantity").value(40));
    }

    @Test
    void refusesAMissingQuantity() throws Exception {
        for (String body : new String[] {"{}", "{\"quantity\":null}"}) {
            mockMvc.perform(as(rover, putWith(SHELL_CREDIT, body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.quantity").value("Inserisci la quantità."));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "10.0", "\"many\"", "99999999999", "true", "[]"})
    void refusesAQuantityThatIsNotAWholeNumber(String quantity) throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":" + quantity + "}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersNotFoundForAMaterialThatIsNotInTheCatalog() throws Exception {
        mockMvc.perform(as(rover, putWith(URL + "/nobody", "{\"quantity\":5}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Questo materiale non esiste."));

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a%27b", "%25", "a;b", "a%2Fb", "a%00b", "..%2F..", "%E2%82%AC"})
    void answersAClientErrorNeverAnErrorOfTheServerForStrangeMaterialIds(String materialId) throws Exception {
        mockMvc.perform(as(rover, putWith(URL + "/" + materialId, "{\"quantity\":5}")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void answersNotFoundForAnEnormousMaterialId() throws Exception {
        mockMvc.perform(as(rover, putWith(URL + "/" + "a".repeat(5000), "{\"quantity\":5}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsTheInventoryOfEachUserApart() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":500}"))).andExpect(status().isOk());
        mockMvc.perform(as(other, putWith(SHELL_CREDIT, "{\"quantity\":7}"))).andExpect(status().isOk());

        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data[0].quantity").value(500));
        mockMvc.perform(as(other, get(URL))).andExpect(jsonPath("$.data[0].quantity").value(7));
    }

    @Test
    void ignoresAUserSentInTheBody() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":9,\"userId\":" + other.getId() + "}")))
                .andExpect(status().isOk());

        mockMvc.perform(as(other, get(URL))).andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(as(rover, get(URL))).andExpect(jsonPath("$.data[0].quantity").value(9));
    }

    @Test
    void disappearsWithTheAccount() throws Exception {
        mockMvc.perform(as(rover, putWith(SHELL_CREDIT, "{\"quantity\":5}"))).andExpect(status().isOk());
        mockMvc.perform(as(other, putWith(SHELL_CREDIT, "{\"quantity\":5}"))).andExpect(status().isOk());

        mockMvc.perform(as(rover, delete("/api/me"))).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from inventory_item where user_id = ?", Integer.class, rover.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from inventory_item where user_id = ?", Integer.class, other.getId())).isEqualTo(1);
    }

    @Test
    void doesNotLetTheDeletedUserBackInWithTheOldToken() throws Exception {
        String token = jwtService.generateToken(rover.getId(), rover.getRole());
        mockMvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isNoContent());

        // The token is valid until it expires (D45), but its user is gone: 401 and not a database error (D102)
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {get(URL),
                putWith(SHELL_CREDIT, "{\"quantity\":5}")}) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(), user.getRole()));
    }

    private static MockHttpServletRequestBuilder putWith(String url, String json) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
