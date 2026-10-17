package com.resonanceplanner.agent;

import static com.resonanceplanner.agent.FakeModelClient.call;
import static com.resonanceplanner.agent.FakeModelClient.text;
import static com.resonanceplanner.agent.FakeModelClient.tools;
import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.agent.client.ModelMessage;
import com.resonanceplanner.agent.tools.AgentTools;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// The whole loop on the real tools, services and database, with a fake model: no network (D63). No @Transactional on
// the test: the loop has none either, and every tool commits its own (D128). Today is the real day.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class AgentLoopIntegrationTest {

    @Autowired
    private AgentTools agentTools;

    @Autowired
    private Clock clock;

    @Autowired
    private ZoneId appZone;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void organizesTheWeekWithTheToolsAndLeavesAProposalToConfirm() {
        User user = newUser("loop.week");
        try {
            FakeModelClient model = new FakeModelClient().answering(
                    tools(call("call_1", "get_player_status", "{}")),
                    tools(call("call_2", "plan_schedule", "{\"days\": 3, \"includeEventActivities\": false}")),
                    tools(call("call_3", "create_plan_proposal",
                            "{\"summary\": \"Tre giorni di farming per portare Jinhsi avanti.\"}")),
                    text("Ho preparato un piano di tre giorni per Jinhsi: lo trovi nel calendario."));

            AgentReply reply = new AgentLoop(model, agentTools, clock, appZone)
                    .answer(user.getId(), List.of(), "Organizzami i prossimi tre giorni");

            assertThat(reply.text()).isEqualTo("Ho preparato un piano di tre giorni per Jinhsi: lo trovi nel calendario.");
            assertThat(reply.rounds()).isEqualTo(4);
            assertThat(reply.steps()).containsExactly(
                    "Ho letto il tuo roster e le Waveplate", "Ho calcolato il piano", "Ho preparato la proposta");
            assertThat(reply.proposal()).isNotNull();
            assertThat(jdbc.queryForObject("select status from plan_proposal where id = ? and user_id = ?",
                    String.class, reply.proposal().id(), user.getId())).isEqualTo("PENDING");
            assertThat(reply.proposal().tasks()).isNotEmpty();
            // What the tools gave back reached the model
            List<ModelMessage> last = model.requests().getLast().messages();
            assertThat(resultOf(last, "call_1")).contains("Jinhsi");
            assertThat(resultOf(last, "call_3")).contains("proposalId");
        } finally {
            cleanUp(user);
        }
    }

    @Test
    void letsTheModelCorrectItselfAfterAnErrorOfATool() {
        User user = newUser("loop.correct");
        try {
            FakeModelClient model = new FakeModelClient().answering(
                    // A proposal without a plan, then a plan with a wrong parameter, then the right one
                    tools(call("call_1", "create_plan_proposal", "{\"summary\": \"Piano.\"}")),
                    tools(call("call_2", "plan_schedule", "{\"days\": 30}")),
                    tools(call("call_3", "plan_schedule", "{\"days\": 7}")),
                    tools(call("call_4", "create_plan_proposal", "{\"summary\": \"Una settimana per Jinhsi.\"}")),
                    text("Ecco il piano della settimana."));

            AgentReply reply = new AgentLoop(model, agentTools, clock, appZone)
                    .answer(user.getId(), List.of(), "Organizzami la settimana");

            List<ModelMessage> last = model.requests().getLast().messages();
            assertThat(resultOf(last, "call_1")).contains("plan_schedule");
            assertThat(resultOf(last, "call_2")).contains("days");
            assertThat(reply.proposal()).isNotNull();
            assertThat(reply.rounds()).isEqualTo(5);
            assertThat(jdbc.queryForObject("select count(*) from plan_proposal where user_id = ?", Integer.class,
                    user.getId())).isEqualTo(1);
        } finally {
            cleanUp(user);
        }
    }

    private static String resultOf(List<ModelMessage> messages, String callId) {
        return messages.stream()
                .filter(message -> message instanceof ModelMessage.ToolResult result && result.toolCallId().equals(callId))
                .map(message -> ((ModelMessage.ToolResult) message).content())
                .findFirst()
                .orElseThrow();
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
    }
}
