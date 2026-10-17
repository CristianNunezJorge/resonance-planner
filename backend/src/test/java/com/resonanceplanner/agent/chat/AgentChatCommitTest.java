package com.resonanceplanner.agent.chat;

import static com.resonanceplanner.agent.FakeModelClient.call;
import static com.resonanceplanner.agent.FakeModelClient.text;
import static com.resonanceplanner.agent.FakeModelClient.tools;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.agent.AgentLoop;
import com.resonanceplanner.agent.AgentReply;
import com.resonanceplanner.agent.FakeModelClient;
import com.resonanceplanner.agent.chat.dto.AgentAnswerResponse;
import com.resonanceplanner.agent.client.AgentUnavailableException;
import com.resonanceplanner.agent.client.ModelCallCounter;
import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.agent.tools.AgentTools;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.TooManyRequestsException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.planning.ProposalStatus;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: the loop has none (D128), every tool commits its own, and the two messages are saved
// in a short transaction of their own after the answer. The real loop, tools and database, with a fake model: no
// network (D63). Today is the real day.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class AgentChatCommitTest {

    @Autowired
    private AgentTurnGate gate;

    @Autowired
    private AgentMessageService messageService;

    @Autowired
    private AgentQuota quota;

    @Autowired
    private AgentTools agentTools;

    @Autowired
    private ModelCallCounter modelCalls;

    @Autowired
    private Clock clock;

    @Autowired
    private ZoneId appZone;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesTheMessageTheAnswerAndTheProposalWhenTheTurnEnds() {
        User user = newUser("chat.commit");
        try {
            FakeModelClient model = new FakeModelClient().answering(
                    tools(call("call_1", "plan_schedule", "{\"days\": 3, \"includeEventActivities\": false}")),
                    tools(call("call_2", "create_plan_proposal", "{\"summary\": \"Tre giorni per Jinhsi.\"}")),
                    text("Ho preparato un piano di tre giorni: lo trovi nel calendario."));

            AgentAnswerResponse answer = chat(model).send(user.getId(), "Organizzami tre giorni");

            assertThat(answer.proposal()).isNotNull();
            assertThat(answer.message().proposalId()).isEqualTo(answer.proposal().id());
            assertThat(answer.usage().used()).isEqualTo(1);
            // The three calls to the model count for the user (D129)
            assertThat(modelCalls.callsToday(user.getId())).isEqualTo(3);
            assertThat(jdbc.queryForList("select role || ':' || coalesce(proposal_id, 0) from agent_message "
                    + "where user_id = ? order by id", String.class, user.getId()))
                    .containsExactly("USER:0", "ASSISTANT:" + answer.proposal().id());
            assertThat(jdbc.queryForObject("select status from plan_proposal where id = ?", String.class,
                    answer.proposal().id())).isEqualTo("PENDING");
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void writesNothingWhenTheModelFails() {
        User user = newUser("chat.failure");
        try {
            FakeModelClient model = new FakeModelClient().thenFailingWith(AgentUnavailableException.unavailable());

            assertThatThrownBy(() -> chat(model).send(user.getId(), "Ciao"))
                    .isInstanceOf(AgentUnavailableException.class);

            assertThat(jdbc.queryForObject("select count(*) from agent_message where user_id = ?", Integer.class,
                    user.getId())).isZero();
            // The failed call still counts for the user (D129), and the turn is over: the user can write again
            assertThat(modelCalls.callsToday(user.getId())).isEqualTo(1);
            assertThat(gate.running()).isZero();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void answersUnauthorizedAndSavesNothingWhenTheAccountIsDeletedWhileTheModelThinks() {
        User user = newUser("chat.deleted");
        try {
            FakeModelClient model = new FakeModelClient().answering(text("Eccomi."))
                    .onEachCall(() -> jdbc.update("delete from users where id = ?", user.getId()));

            assertThatThrownBy(() -> chat(model).send(user.getId(), "Ciao"))
                    .isInstanceOf(UnauthorizedException.class);

            assertThat(jdbc.queryForObject("select count(*) from agent_message where user_id = ?", Integer.class,
                    user.getId())).isZero();
            assertThat(gate.running()).isZero();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void savesBothMessagesOfATurnOrNeither() {
        User user = newUser("chat.atomic");
        try {
            // A proposal that does not exist: the answer breaks the foreign key after the message of the user is in
            AgentReply reply = new AgentReply("Ecco il piano.", new ProposalResponse(Long.MAX_VALUE,
                    ProposalStatus.PENDING, "Piano.", Instant.now(), List.of()), List.of(), TokenUsage.NONE, 1);

            assertThatThrownBy(() -> messageService.saveTurn(user.getId(), "Ciao", Instant.now(), reply))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(jdbc.queryForObject("select count(*) from agent_message where user_id = ?", Integer.class,
                    user.getId())).isZero();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void refusesASecondMessageOfTheUserWhileTheFirstWaitsForTheModel() throws Exception {
        User user = newUser("chat.twice");
        CountDownLatch modelCalled = new CountDownLatch(1);
        CountDownLatch secondRefused = new CountDownLatch(1);
        try {
            FakeModelClient model = new FakeModelClient().answering(text("Eccomi.")).onEachCall(() -> {
                modelCalled.countDown();
                await(secondRefused);
            });
            AgentChatService chat = chat(model);
            CompletableFuture<AgentAnswerResponse> first =
                    CompletableFuture.supplyAsync(() -> chat.send(user.getId(), "Primo"));
            assertThat(modelCalled.await(20, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> chat.send(user.getId(), "Secondo"))
                    .isInstanceOfSatisfying(TooManyRequestsException.class,
                            error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.RATE_LIMITED));
            secondRefused.countDown();

            assertThat(first.get(20, TimeUnit.SECONDS).message().content()).isEqualTo("Eccomi.");
            assertThat(jdbc.queryForList("select content from agent_message where user_id = ? order by id",
                    String.class, user.getId())).containsExactly("Primo", "Eccomi.");
        } finally {
            secondRefused.countDown();
            cleanUp(user);
        }
    }

    private AgentChatService chat(FakeModelClient model) {
        return new AgentChatService(gate, messageService, quota, new AgentLoop(model, agentTools, modelCalls, clock, appZone),
                clock);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // A name and an email that no earlier run can have left behind, with Jinhsi to plan for
    private User newUser(String prefix) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        User user = User.create(unique, unique + "@example.com", "hash", Role.USER);
        user.changePlayTimeMinutes(60);
        User saved = userRepository.saveAndFlush(user);
        jdbc.update("""
                insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                    target_rank)
                values (?, 'jinhsi', 1, 0, 40, 2)""", saved.getId());
        return saved;
    }

    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
        // The calls of the day stay after the account is deleted (D129): the test removes its own
        jdbc.update("delete from agent_model_calls where user_id = ?", user.getId());
    }
}
