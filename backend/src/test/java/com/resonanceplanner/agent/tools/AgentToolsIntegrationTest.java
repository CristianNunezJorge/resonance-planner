package com.resonanceplanner.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.agent.client.ToolCall;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// The tools on the real services and database (D127). No @Transactional on the test: every service commits its own
// transaction, like in production, and the users are removed at the end. Today is the real day.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class AgentToolsIntegrationTest {

    @Autowired
    private AgentTools tools;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Today today;

    @Test
    void readsPlansAndSavesTheProposalOfTheUserOfTheTurn() {
        User user = newUser("tools.flow", true);
        try {
            AgentTurn turn = new AgentTurn(user.getId());

            ToolOutcome status = run(turn, "get_player_status", "{}");
            ToolOutcome plan = run(turn, "plan_schedule", "{\"days\": 3, \"includeEventActivities\": false}");
            ToolOutcome saved = run(turn, "create_plan_proposal", "{\"summary\": \"Tre giorni di farming per Jinhsi.\"}");
            ToolOutcome calendar = run(turn, "get_calendar", "{}");

            assertThat(status.error()).as(status.content()).isFalse();
            assertThat(json(status).at("/roster/0/name").asString()).isEqualTo("Jinhsi");
            assertThat(status.content()).doesNotContain(user.getEmail());
            assertThat(plan.error()).as(plan.content()).isFalse();
            assertThat(json(plan).at("/days").size()).isEqualTo(3);
            assertThat(saved.error()).as(saved.content()).isFalse();
            long proposalId = json(saved).at("/proposalId").asLong();
            assertThat(jdbc.queryForObject("select user_id from plan_proposal where id = ? and status = 'PENDING'",
                    Long.class, proposalId)).isEqualTo(user.getId());
            assertThat(jdbc.queryForObject("select summary from plan_proposal where id = ?", String.class, proposalId))
                    .isEqualTo("Tre giorni di farming per Jinhsi.");
            assertThat(turn.proposal()).hasValueSatisfying(proposal -> assertThat(proposal.id()).isEqualTo(proposalId));
            assertThat(json(calendar).at("/pendingProposal/id").asLong()).isEqualTo(proposalId);
            assertThat(turn.steps()).containsExactly(AgentTools.STEP_PLAYER_STATUS, AgentTools.STEP_PLAN,
                    AgentTools.STEP_PROPOSAL, AgentTools.STEP_CALENDAR);
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void asksForANewPlanWhenTheDataChangedAfterTheModelSawIt() {
        User user = newUser("tools.changed", true);
        try {
            AgentTurn turn = new AgentTurn(user.getId());
            run(turn, "plan_schedule", "{\"days\": 3}");
            jdbc.update("update owned_character set current_level = 10 where user_id = ?", user.getId());

            ToolOutcome outcome = run(turn, "create_plan_proposal", "{\"summary\": \"Piano di prima.\"}");

            assertThat(outcome.error()).isTrue();
            assertThat(json(outcome).at("/error").asString()).contains("richiama plan_schedule");
            assertThat(proposalsOf(user)).isZero();
            assertThat(turn.proposal()).isEmpty();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void neverReachesTheDataOfAnotherUser() {
        User owner = newUser("tools.owner", true);
        User other = newUser("tools.other", false);
        try {
            AgentTurn ownerTurn = new AgentTurn(owner.getId());
            run(ownerTurn, "plan_schedule", "{\"days\": 3}");
            run(ownerTurn, "create_plan_proposal", "{\"summary\": \"Piano del proprietario.\"}");
            AgentTurn otherTurn = new AgentTurn(other.getId());

            ToolOutcome status = run(otherTurn, "get_player_status", "{}");
            ToolOutcome calendar = run(otherTurn, "get_calendar", "{}");
            ToolOutcome asked = run(otherTurn, "get_calendar", "{\"userId\": " + owner.getId() + "}");
            ToolOutcome proposal = run(otherTurn, "create_plan_proposal", "{\"summary\": \"Rubo il piano.\"}");

            assertThat(json(status).at("/roster").isEmpty()).isTrue();
            assertThat(json(calendar).at("/pendingProposal").isNull()).isTrue();
            assertThat(calendar.content()).doesNotContain("Piano del proprietario.");
            assertThat(asked.error()).isTrue();
            assertThat(proposal.error()).isTrue();
            assertThat(proposalsOf(other)).isZero();
            assertThat(proposalsOf(owner)).isEqualTo(1);
        } finally {
            cleanUp(owner);
            cleanUp(other);
        }
    }

    @Test
    void givesTheModelTheWarningOfAPlanWithNothingToDo() {
        User user = newUser("tools.empty", false);
        try {
            AgentTurn turn = new AgentTurn(user.getId());
            run(turn, "plan_schedule", "{\"includeEventActivities\": false}");

            ToolOutcome outcome = run(turn, "create_plan_proposal", "{\"summary\": \"Niente da fare.\"}");

            // The 409 of an empty plan (D124) goes back to the model with the warning of the planner
            assertThat(outcome.error()).isTrue();
            assertThat(json(outcome).at("/error").asString()).contains("roster");
            assertThat(proposalsOf(user)).isZero();
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void stopsTheTurnOfAUserThatNoLongerExists() {
        User user = newUser("tools.deleted", true);
        AgentTurn turn = new AgentTurn(user.getId());
        cleanUp(user);

        assertThatThrownBy(() -> run(turn, "get_calendar", "{}")).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> run(turn, "plan_schedule", "{}")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void checksTheParametersOfTheModelWithTheRulesOfTheApi() {
        User user = newUser("tools.rules", true);
        try {
            AgentTurn turn = new AgentTurn(user.getId());
            LocalDate todayDate = today.date();

            ToolOutcome longRange = run(turn, "get_events",
                    "{\"from\": \"" + todayDate + "\", \"to\": \"" + todayDate.plusDays(70) + "\"}");
            ToolOutcome yesterday = run(turn, "plan_schedule", "{\"startDate\": \"" + todayDate.minusDays(1) + "\"}");
            ToolOutcome halfDay = run(turn, "plan_schedule", "{\"days\": 7.5}");
            ToolOutcome stranger = run(turn, "plan_schedule", "{\"characterPriority\": [\"nobody\"]}");

            assertThat(json(longRange).at("/fields/to").asString()).contains("62");
            assertThat(json(yesterday).at("/fields/startDate").asString()).isNotBlank();
            assertThat(json(halfDay).at("/fields/days").asString()).isNotBlank();
            assertThat(json(stranger).at("/fields/characterPriority").asString()).isNotBlank();
            assertThat(turn.steps()).isEmpty();
            assertThat(turn.lastPlan()).isEmpty();
        } finally {
            cleanUp(user);
        }
    }

    private ToolOutcome run(AgentTurn turn, String name, String arguments) {
        return tools.run(turn, new ToolCall("call_" + UUID.randomUUID(), name, arguments));
    }

    private JsonNode json(ToolOutcome outcome) {
        return jsonMapper.readTree(outcome.content());
    }

    private int proposalsOf(User user) {
        return jdbc.queryForObject("select count(*) from plan_proposal where user_id = ?", Integer.class, user.getId());
    }

    // A name and an email that no earlier run can have left behind, with Jinhsi to plan for if asked
    private User newUser(String prefix, boolean withJinhsi) {
        String unique = prefix + "." + UUID.randomUUID().toString().substring(0, 8);
        User user = User.create(unique, unique + "@example.com", "hash", Role.USER);
        user.changePlayTimeMinutes(60);
        User saved = userRepository.saveAndFlush(user);
        if (withJinhsi) {
            jdbc.update("""
                    insert into owned_character (user_id, character_id, current_level, current_rank, target_level,
                        target_rank)
                    values (?, 'jinhsi', 1, 0, 40, 2)""", saved.getId());
        }
        return saved;
    }

    private void cleanUp(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
    }
}
