package com.resonanceplanner.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.resonanceplanner.agent.AgentLoop;
import com.resonanceplanner.agent.AgentReply;
import com.resonanceplanner.agent.ChatMessage;
import com.resonanceplanner.agent.client.AgentUnavailableException;
import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.auth.JwtService;
import com.resonanceplanner.planning.ProposalStatus;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// The chat API (D40, D52, D129) through the real filter chain and the Docker Compose test database; every test is
// rolled back. The loop of the agent is replaced by a mock: here only saving, history and limits matter, the loop has
// its own tests. Today is the real day.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentChatControllerTest {

    private static final String URL = "/api/me/agent";
    private static final List<String> STEPS = List.of("Ho letto il tuo roster e le Waveplate");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private AgentLoop agentLoop;

    private User rover;
    private User other;

    @BeforeEach
    void createTwoUsers() {
        rover = userRepository.saveAndFlush(User.create("rover", "rover@example.com", "hash", Role.USER));
        other = userRepository.saveAndFlush(User.create("other", "other@example.com", "hash", Role.USER));
    }

    @Test
    void needsTheLogin() throws Exception {
        mockMvc.perform(get(URL + "/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(message("Ciao")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL + "/usage")).andExpect(status().isUnauthorized());
    }

    @Nested
    class Sending {

        @Test
        void savesTheMessageWithTheAnswerAndCountsIt() throws Exception {
            answering("Ciao! Posso organizzarti la settimana.", null);

            mockMvc.perform(as(rover, message("Ciao")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message.id").isNumber())
                    .andExpect(jsonPath("$.data.message.role").value("ASSISTANT"))
                    .andExpect(jsonPath("$.data.message.content").value("Ciao! Posso organizzarti la settimana."))
                    .andExpect(jsonPath("$.data.message.proposalId").value(nullValue()))
                    .andExpect(jsonPath("$.data.message.createdAt").isString())
                    .andExpect(jsonPath("$.data.proposal").value(nullValue()))
                    .andExpect(jsonPath("$.data.steps[0]").value("Ho letto il tuo roster e le Waveplate"))
                    .andExpect(jsonPath("$.data.usage.used").value(1))
                    .andExpect(jsonPath("$.data.usage.limit").value(5))
                    .andExpect(jsonPath("$.data.usage.resetsAt").isString())
                    .andExpect(jsonPath("$.data.usage.callsLimitReached").value(false))
                    .andExpect(jsonPath("$.data.usage.appLimitReached").value(false));

            assertThat(jdbc.queryForList("select role || ':' || content from agent_message where user_id = ? "
                    + "order by id", String.class, rover.getId()))
                    .containsExactly("USER:Ciao", "ASSISTANT:Ciao! Posso organizzarti la settimana.");
            verify(agentLoop).answer(rover.getId(), List.of(), "Ciao");
        }

        @Test
        void givesTheAgentTheLastTenSavedMessagesOfTheUserFromTheOldest() throws Exception {
            // Of two days ago, so they do not count in the messages of today
            Instant before = Instant.now().minus(2, ChronoUnit.DAYS);
            for (int i = 1; i <= 12; i++) {
                saved(rover, i % 2 == 1 ? "USER" : "ASSISTANT", "Messaggio " + i, before.plusSeconds(i));
            }
            saved(other, "USER", "Di un altro utente", null);
            answering("Va bene.", null);

            mockMvc.perform(as(rover, message("E adesso?"))).andExpect(status().isOk());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatMessage>> history = ArgumentCaptor.forClass(List.class);
            verify(agentLoop).answer(eq(rover.getId()), history.capture(), eq("E adesso?"));
            assertThat(history.getValue()).extracting(ChatMessage::content)
                    .containsExactly("Messaggio 3", "Messaggio 4", "Messaggio 5", "Messaggio 6", "Messaggio 7",
                            "Messaggio 8", "Messaggio 9", "Messaggio 10", "Messaggio 11", "Messaggio 12");
            assertThat(history.getValue().getFirst().speaker()).isEqualTo(ChatMessage.Speaker.USER);
        }

        @Test
        void linksTheAnswerToTheProposalTheAgentMade() throws Exception {
            Long proposalId = jdbc.queryForObject("""
                    insert into plan_proposal (user_id, status, summary, created_at)
                    values (?, 'PENDING', 'Tre giorni per Jinhsi.', now()) returning id""", Long.class, rover.getId());
            answering("Ho preparato un piano.", new ProposalResponse(proposalId, ProposalStatus.PENDING,
                    "Tre giorni per Jinhsi.", Instant.now(), List.of()));

            mockMvc.perform(as(rover, message("Organizzami la settimana")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message.proposalId").value(proposalId))
                    .andExpect(jsonPath("$.data.proposal.id").value(proposalId))
                    .andExpect(jsonPath("$.data.proposal.summary").value("Tre giorni per Jinhsi."));

            assertThat(jdbc.queryForObject("select proposal_id from agent_message where user_id = ? and role = "
                    + "'ASSISTANT'", Long.class, rover.getId())).isEqualTo(proposalId);
        }

        @Test
        void savesAtMostFourThousandCharactersOfALongAnswer() throws Exception {
            answering("a".repeat(5_000), null);

            mockMvc.perform(as(rover, message("Scrivi tanto")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message.content").value("a".repeat(3_999) + "…"));

            assertThat(jdbc.queryForObject("select length(content) from agent_message where user_id = ? and role = "
                    + "'ASSISTANT'", Integer.class, rover.getId())).isEqualTo(4_000);
        }

        @Test
        void refusesAnEmptyOrTooLongMessageWithoutCallingTheAgent() throws Exception {
            mockMvc.perform(as(rover, message("   ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.content").value("Scrivi un messaggio."));
            mockMvc.perform(as(rover, message("a".repeat(1_001))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.content")
                            .value("Il messaggio può avere al massimo 1000 caratteri."));
            mockMvc.perform(as(rover, post(URL + "/messages").contentType(MediaType.APPLICATION_JSON).content("{}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.content").value("Scrivi un messaggio."));

            verify(agentLoop, never()).answer(anyLong(), any(), anyString());
            assertThat(savedCount(rover)).isZero();
        }

        @Test
        void savesNothingAndCountsNothingWhenTheAgentFails() throws Exception {
            when(agentLoop.answer(anyLong(), any(), anyString())).thenThrow(AgentUnavailableException.unavailable());

            mockMvc.perform(as(rover, message("Ciao")))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error.code").value("AGENT_UNAVAILABLE"));

            assertThat(savedCount(rover)).isZero();
            mockMvc.perform(as(rover, get(URL + "/usage"))).andExpect(jsonPath("$.data.used").value(0));
        }
    }

    @Nested
    class Limits {

        // The check of T35 in docs/09-task.md
        @Test
        void theSixthMessageOfTheDayAnswersTooManyRequests() throws Exception {
            answering("Fatto.", null);
            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(as(rover, message("Messaggio " + i)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.usage.used").value(i));
            }

            mockMvc.perform(as(rover, message("Messaggio 6")))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                    .andExpect(jsonPath("$.error.code").value("AGENT_DAILY_LIMIT"))
                    .andExpect(jsonPath("$.error.message").value(AgentQuota.USER_LIMIT.formatted(5)));

            verify(agentLoop, times(5)).answer(anyLong(), any(), anyString());
            assertThat(savedCount(rover)).isEqualTo(10);
            // The limit is of each user: the other one can still write
            mockMvc.perform(as(other, message("Ciao"))).andExpect(status().isOk());
        }

        @Test
        void countsOnlyTheMessagesOfToday() throws Exception {
            for (int i = 0; i < 5; i++) {
                saved(rover, "USER", "Ieri", Instant.now().minus(2, ChronoUnit.DAYS));
            }
            answering("Fatto.", null);

            mockMvc.perform(as(rover, message("Oggi")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.usage.used").value(1));
        }

        @Test
        void refusesAUserWhoseFailedTurnsUsedTheCallsOfTheDay() throws Exception {
            // 24 calls a day for each user, also of failed turns, and 6 for a turn: with 19 used no turn starts (D129)
            modelCallsToday(rover, 19);

            mockMvc.perform(as(rover, message("Ciao")))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                    .andExpect(jsonPath("$.error.code").value("AGENT_DAILY_LIMIT"))
                    .andExpect(jsonPath("$.error.message").value(AgentQuota.USER_CALLS_LIMIT));
            mockMvc.perform(as(rover, get(URL + "/usage")))
                    .andExpect(jsonPath("$.data.used").value(0))
                    .andExpect(jsonPath("$.data.callsLimitReached").value(true))
                    .andExpect(jsonPath("$.data.appLimitReached").value(false));

            // The other user still has all the calls
            answering("Ciao!", null);
            mockMvc.perform(as(other, message("Ciao"))).andExpect(status().isOk());
        }

        @Test
        void refusesEveryUserWhenTheAppHasLessThanAWholeTurnOfCallsLeft() throws Exception {
            // 40 calls a day and 6 for a turn: with 35 used in the whole app no turn starts (D129)
            modelCallsToday(other, 35);

            mockMvc.perform(as(rover, message("Ciao")))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                    .andExpect(jsonPath("$.error.code").value("AGENT_DAILY_LIMIT"))
                    .andExpect(jsonPath("$.error.message").value(AgentQuota.APP_LIMIT));
            mockMvc.perform(as(rover, get(URL + "/usage")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.used").value(0))
                    .andExpect(jsonPath("$.data.appLimitReached").value(true));

            verify(agentLoop, never()).answer(anyLong(), any(), anyString());
        }
    }

    @Nested
    class History {

        @Test
        void listsTheChatOfTheUserFromTheNewestInPages() throws Exception {
            for (int i = 1; i <= 25; i++) {
                saved(rover, i % 2 == 1 ? "USER" : "ASSISTANT", "Messaggio " + i, null);
            }
            saved(other, "USER", "Di un altro utente", null);

            String first = mockMvc.perform(as(rover, get(URL + "/messages").param("limit", "10")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[0].content").value("Messaggio 25"))
                    .andExpect(jsonPath("$.data[0].role").value("USER"))
                    .andExpect(jsonPath("$.data[9].content").value("Messaggio 16"))
                    .andExpect(jsonPath("$.meta.limit").value(10))
                    .andExpect(jsonPath("$.meta.hasMore").value(true))
                    .andReturn().getResponse().getContentAsString();
            Number oldest = JsonPath.read(first, "$.data[9].id");

            mockMvc.perform(as(rover, get(URL + "/messages").param("before", oldest.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(15)))
                    .andExpect(jsonPath("$.data[0].content").value("Messaggio 15"))
                    .andExpect(jsonPath("$.data[14].content").value("Messaggio 1"))
                    .andExpect(jsonPath("$.meta.limit").value(20))
                    .andExpect(jsonPath("$.meta.hasMore").value(false));
        }

        @Test
        void givesAnEmptyPageToAUserWithoutMessages() throws Exception {
            saved(other, "USER", "Di un altro utente", null);

            mockMvc.perform(as(rover, get(URL + "/messages")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)))
                    .andExpect(jsonPath("$.meta.hasMore").value(false));
        }

        @Test
        void refusesALimitOrACursorOutOfRange() throws Exception {
            mockMvc.perform(as(rover, get(URL + "/messages").param("limit", "0")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.limit").value("Il limite va da 1 a 50."));
            mockMvc.perform(as(rover, get(URL + "/messages").param("limit", "51")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.limit").value("Il limite va da 1 a 50."));
            mockMvc.perform(as(rover, get(URL + "/messages").param("before", "0")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fields.before").value("Indica l'id di un messaggio."));
        }
    }

    @Test
    void answersUnauthorizedWhenTheUserOfTheTokenIsGone() throws Exception {
        User gone = userRepository.saveAndFlush(User.create("gone", "gone@example.com", "hash", Role.USER));
        String token = "Bearer " + jwtService.generateToken(gone.getId(), gone.getRole());
        jdbc.update("delete from users where id = ?", gone.getId());
        answering("Ciao.", null);

        mockMvc.perform(get(URL + "/messages").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(message("Ciao").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL + "/usage").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        verify(agentLoop, never()).answer(anyLong(), any(), anyString());
    }

    private void answering(String text, ProposalResponse proposal) {
        when(agentLoop.answer(anyLong(), any(), anyString()))
                .thenReturn(new AgentReply(text, proposal, STEPS, TokenUsage.NONE, 2));
    }

    private void modelCallsToday(User user, int calls) {
        jdbc.update("""
                insert into agent_model_calls (call_date, user_id, calls) values (?, ?, ?)
                on conflict (call_date, user_id) do update set calls = excluded.calls""",
                LocalDate.now(ZoneOffset.UTC), user.getId(), calls);
    }

    private void saved(User user, String role, String content, Instant createdAt) {
        jdbc.update("insert into agent_message (user_id, role, content, created_at) values (?, ?, ?, ?)",
                user.getId(), role, content, Timestamp.from(createdAt == null ? Instant.now() : createdAt));
    }

    private int savedCount(User user) {
        return jdbc.queryForObject("select count(*) from agent_message where user_id = ?", Integer.class,
                user.getId());
    }

    private static MockHttpServletRequestBuilder message(String content) {
        return post(URL + "/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"" + content + "\"}");
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user.getId(),
                user.getRole()));
    }
}
