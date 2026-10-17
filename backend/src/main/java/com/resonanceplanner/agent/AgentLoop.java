package com.resonanceplanner.agent;

import com.resonanceplanner.agent.client.FinishReason;
import com.resonanceplanner.agent.client.ModelCallCounter;
import com.resonanceplanner.agent.client.ModelClient;
import com.resonanceplanner.agent.client.ModelMessage;
import com.resonanceplanner.agent.client.ModelReply;
import com.resonanceplanner.agent.client.ModelRequest;
import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.agent.client.ToolCall;
import com.resonanceplanner.agent.client.ToolDefinition;
import com.resonanceplanner.agent.tools.AgentTools;
import com.resonanceplanner.agent.tools.AgentTurn;
import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The loop of the agent (D54, D128): it sends the conversation to the model, runs the tools it asks for and sends
 * back their results, until the model answers with a text. At most 6 calls to the model in 2 minutes, 5 tools per
 * reply and 10 per turn. Every call counts in the calls of the user and of the whole app before it is made, so also the
 * calls of a turn that fails (D129). It has no transaction of its own: every tool opens and closes its own, so none
 * stays open while the model thinks. The logs say what happened, never the text of the chat or of the tools (D60).
 */
@Service
public class AgentLoop {

    public static final int MAX_ROUNDS = 6;
    static final int MAX_TOOLS_PER_REPLY = 5;
    static final int MAX_TOOLS_PER_TURN = 10;
    public static final int MAX_HISTORY = 10;
    // After this no new call to the model starts: with 60 seconds per call a turn ends within 3 minutes
    static final Duration MAX_TURN_TIME = Duration.ofMinutes(2);
    public static final int MAX_MESSAGE_CHARS = 1_000;

    static final String PROPOSAL_READY = "Non sono riuscito a completare la risposta, ma ho preparato una proposta: "
            + "la trovi nel calendario, da confermare o scartare.";
    static final String OLDER_PROPOSAL_NOTE = "\n\nNota: l'ultimo piano che ho calcolato non è stato salvato. "
            + "Nel calendario c'è la proposta di prima, da confermare o scartare.";
    static final String TOO_MANY_TOOLS = "{\"error\":\"Troppi strumenti in una sola risposta: chiedine al massimo "
            + MAX_TOOLS_PER_REPLY + " alla volta.\"}";
    static final String TOO_MANY_TOOLS_IN_TURN =
            "{\"error\":\"Hai già usato tutti gli strumenti di questo turno: rispondi con i dati che hai.\"}";
    static final String TOOL_FAILED =
            "{\"error\":\"Errore interno dello strumento: riprova più tardi o rispondi senza questi dati.\"}";
    static final String WRONG_MESSAGE = "Il messaggio deve avere da 1 a " + MAX_MESSAGE_CHARS + " caratteri.";

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final String UNKNOWN_TOOL = "unknown";

    private final ModelClient modelClient;
    private final AgentTools tools;
    private final ModelCallCounter modelCalls;
    private final Clock clock;
    private final ZoneId zone;
    private final Set<String> knownTools;

    public AgentLoop(ModelClient modelClient, AgentTools tools, ModelCallCounter modelCalls, Clock clock,
            ZoneId appZone) {
        this.modelClient = modelClient;
        this.tools = tools;
        this.modelCalls = modelCalls;
        this.clock = clock;
        this.zone = appZone;
        this.knownTools = tools.definitions().stream().map(ToolDefinition::name).collect(Collectors.toSet());
    }

    /**
     * The answer to a new message of the user, with the saved chat before it (D40, D58).
     *
     * @throws FieldValidationException if the message is empty or longer than 1,000 characters (400, D61)
     * @throws AgentStoppedException if the model reaches no answer (502, D61)
     * @throws UnauthorizedException if the user of the token no longer exists (401, D102)
     */
    public AgentReply answer(Long userId, List<ChatMessage> history, String message) {
        Objects.requireNonNull(history, "history");
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_CHARS) {
            throw new FieldValidationException("content", WRONG_MESSAGE);
        }
        Turn turn = new Turn(new AgentTurn(userId), clock.instant());
        List<ModelMessage> messages = new ArrayList<>(historyOf(history));
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        messages.add(new ModelMessage.User(AgentPrompt.withToday(message, now)));
        while (turn.rounds < MAX_ROUNDS) {
            if (turn.elapsed().compareTo(MAX_TURN_TIME) >= 0) {
                return stopped(turn, "the time of the turn is over");
            }
            Optional<ModelReply> sent = send(turn, messages);
            if (sent.isEmpty()) {
                return stopped(turn, "the model failed");
            }
            ModelReply reply = sent.get();
            if (reply.toolCalls().isEmpty()) {
                return finish(turn, reply);
            }
            messages.add(reply.asMessage());
            // Every call gets a result, also the ones beyond the limits: the format wants one for each id. The tools
            // of the last round still run, so a proposal saved there is not lost
            for (int i = 0; i < reply.toolCalls().size(); i++) {
                ToolCall call = reply.toolCalls().get(i);
                messages.add(new ModelMessage.ToolResult(call.id(), resultOf(turn, call, i)));
            }
        }
        return stopped(turn, "the rounds are over");
    }

    // Empty when the model fails after a proposal was saved in the turn: that proposal is in the calendar, so the
    // turn ends telling the user where to find it. Any other failure is logged without its message and thrown
    private Optional<ModelReply> send(Turn turn, List<ModelMessage> messages) {
        try {
            modelCalls.add(turn.agentTurn.userId());
            return Optional.of(turn.counted(modelClient.send(
                    new ModelRequest(AgentPrompt.SYSTEM, tools.definitions(), List.copyOf(messages)))));
        } catch (ApiException e) {
            if (e.getErrorCode() == ErrorCode.AGENT_UNAVAILABLE && turn.agentTurn.proposal().isPresent()) {
                return Optional.empty();
            }
            logFailure(turn, e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            logFailure(turn, e.getClass().getSimpleName());
            throw e;
        }
    }

    private String resultOf(Turn turn, ToolCall call, int indexInReply) {
        if (indexInReply >= MAX_TOOLS_PER_REPLY) {
            return TOO_MANY_TOOLS;
        }
        if (turn.toolNames.size() >= MAX_TOOLS_PER_TURN) {
            return TOO_MANY_TOOLS_IN_TURN;
        }
        return runTool(turn, call);
    }

    // The last messages with a text, starting with the user: an empty one would tell the model nothing, and some
    // free models refuse a conversation that starts with an answer
    private static List<ModelMessage> historyOf(List<ChatMessage> history) {
        List<ChatMessage> withText = history.stream().filter(message -> !message.content().isBlank()).toList();
        List<ChatMessage> last = withText.subList(Math.max(0, withText.size() - MAX_HISTORY), withText.size());
        if (!last.isEmpty() && last.getFirst().speaker() == ChatMessage.Speaker.ASSISTANT) {
            last = last.subList(1, last.size());
        }
        return last.stream().map(AgentLoop::toModelMessage).toList();
    }

    private static ModelMessage toModelMessage(ChatMessage message) {
        if (message.speaker() == ChatMessage.Speaker.USER) {
            return new ModelMessage.User(message.content());
        }
        return new ModelMessage.Assistant(message.content(), List.of());
    }

    private String runTool(Turn turn, ToolCall call) {
        String name = knownTools.contains(call.name()) ? call.name() : UNKNOWN_TOOL;
        turn.toolNames.add(name);
        try {
            return tools.run(turn.agentTurn, call).content();
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RuntimeException e) {
            // The class and the place say what failed; the message could hold data of the user (D60)
            StackTraceElement[] trace = e.getStackTrace();
            log.warn("Tool {} failed for user {}: {} at {}{}", name, turn.agentTurn.userId(),
                    e.getClass().getSimpleName(), trace.length == 0 ? "unknown place" : trace[0],
                    e.getCause() == null ? "" : ", caused by " + e.getCause().getClass().getSimpleName());
            return TOOL_FAILED;
        }
    }

    private AgentReply finish(Turn turn, ModelReply reply) {
        boolean complete = reply.finishReason() == FinishReason.STOP || reply.finishReason() == FinishReason.OTHER;
        if (!complete || reply.text().isBlank()) {
            return stopped(turn, "the answer ended with " + reply.finishReason()
                    + (reply.text().isBlank() ? " and no text" : ""));
        }
        log.info("Agent turn of user {} done in {} rounds, tools {}, tokens {} in / {} out / {} cached, {} ms",
                turn.agentTurn.userId(), turn.rounds, turn.toolNames, turn.usage.inputTokens(),
                turn.usage.outputTokens(), turn.usage.cachedInputTokens(), turn.elapsed().toMillis());
        AgentTurn agentTurn = turn.agentTurn;
        boolean olderProposal = agentTurn.proposal().isPresent() && !agentTurn.proposalIsLastPlan();
        return turn.reply(reply.text().strip() + (olderProposal ? OLDER_PROPOSAL_NOTE : ""));
    }

    // No answer from the model: a proposal saved in the turn still exists, so the user is told where to find it
    private AgentReply stopped(Turn turn, String why) {
        log.warn("Agent turn of user {} stopped after {} rounds: {}, tools {}, tokens {} in / {} out, {} ms",
                turn.agentTurn.userId(), turn.rounds, why, turn.toolNames, turn.usage.inputTokens(),
                turn.usage.outputTokens(), turn.elapsed().toMillis());
        if (turn.agentTurn.proposal().isPresent()) {
            return turn.reply(PROPOSAL_READY);
        }
        throw new AgentStoppedException();
    }

    private static void logFailure(Turn turn, String what) {
        log.warn("Agent turn of user {} failed after {} rounds: {}, tools {}, tokens {} in / {} out, {} ms",
                turn.agentTurn.userId(), turn.rounds, what, turn.toolNames, turn.usage.inputTokens(),
                turn.usage.outputTokens(), turn.elapsed().toMillis());
    }

    /** What the loop counts in one turn, besides what the tools keep in {@link AgentTurn}: one thread, one call. */
    private final class Turn {

        private final AgentTurn agentTurn;
        private final Instant start;
        private final List<String> toolNames = new ArrayList<>();
        private int rounds;
        private TokenUsage usage = TokenUsage.NONE;

        private Turn(AgentTurn agentTurn, Instant start) {
            this.agentTurn = agentTurn;
            this.start = start;
        }

        private ModelReply counted(ModelReply reply) {
            rounds++;
            usage = usage.plus(reply.usage());
            return reply;
        }

        private Duration elapsed() {
            return Duration.between(start, clock.instant());
        }

        private AgentReply reply(String text) {
            return new AgentReply(text, agentTurn.proposal().orElse(null), agentTurn.steps(), usage, rounds);
        }
    }
}
