package com.resonanceplanner.common.error;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Security filters are off: this test is only about turning exceptions into ApiResponse (D46)
@WebMvcTest(controllers = ErrorTestController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void wrapsTheDataOfASuccessfulResponse() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.value").value("ok"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void listsTheInvalidFieldsOfARequestBody() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Alcuni campi non sono validi."))
                .andExpect(jsonPath("$.error.fields.name").value("Il nome è obbligatorio."))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità deve essere almeno 1."));
    }

    @Test
    void listsTheInvalidQueryParametersInItalianEvenWithoutACustomMessage() throws Exception {
        mockMvc.perform(get("/test/range").param("days", "90"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.days").value("deve essere inferiore o uguale a 62"));
    }

    @Test
    void rejectsMalformedJsonWithoutFieldDetails() throws Exception {
        mockMvc.perform(post("/test/items").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Richiesta non valida."))
                .andExpect(jsonPath("$.error.fields").doesNotExist());
    }

    @Test
    void answersNotFoundWithTheMessageOfTheException() throws Exception {
        mockMvc.perform(get("/test/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Oggetto non trovato."));
    }

    @Test
    void answersNotFoundForAnUnknownPath() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Risorsa non trovata."));
    }

    @Test
    void answersConflictWithTheMessageOfTheException() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("Oggetto già presente."));
    }

    @Test
    void answersBadRequestWithTheFieldWhenAServiceRejectsOneValue() throws Exception {
        mockMvc.perform(get("/test/field-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Alcuni campi non sono validi."))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità non è tra quelle ammesse."));
    }

    @Test
    void answersUnauthorizedWhenTheUserOfTheTokenIsGone() throws Exception {
        mockMvc.perform(get("/test/user-gone"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Devi effettuare l'accesso."));
    }

    @Test
    void listsTheFieldsOfTheBodyAndOfThePathWhenBothAreInvalid() throws Exception {
        mockMvc.perform(post("/test/items/0").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.id").value("L'identificativo deve essere positivo."))
                .andExpect(jsonPath("$.error.fields.name").value("Il nome è obbligatorio."))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità deve essere almeno 1."));
    }

    @Test
    void answersBadRequestForAConstraintViolation() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.name").value("Il nome è obbligatorio."))
                .andExpect(jsonPath("$.error.fields.quantity").value("La quantità deve essere almeno 1."));
    }

    @Test
    void answersUnauthorizedForBadCredentials() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Email o password non corrette."));
    }

    @Test
    void answersForbiddenWhenAccessIsDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void keepsTheErrorCodeOfAStatusChosenBySpring() throws Exception {
        mockMvc.perform(get("/test/too-many-requests"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void answersARequestWithTheWrongMethodAsAnInvalidRequest() throws Exception {
        mockMvc.perform(put("/test/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Richiesta non valida."));
    }

    @Test
    void hidesTheDetailsOfAnUnexpectedError() throws Exception {
        mockMvc.perform(get("/test/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Si è verificato un errore imprevisto. Riprova più tardi."))
                .andExpect(content().string(not(containsString("internal detail"))));
    }
}
