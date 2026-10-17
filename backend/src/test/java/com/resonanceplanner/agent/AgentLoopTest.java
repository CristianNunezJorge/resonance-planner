package com.resonanceplanner.agent;

import static com.resonanceplanner.agent.FakeModelClient.call;
import static com.resonanceplanner.agent.FakeModelClient.ending;
import static com.resonanceplanner.agent.FakeModelClient.text;
import static com.resonanceplanner.agent.FakeModelClient.tools;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.resonanceplanner.agent.ChatMessage.Speaker;
import com.resonanceplanner.agent.client.FinishReason;
import com.resonanceplanner.agent.client.ModelMessage;
import com.resonanceplanner.agent.client.ModelReply;
import com.resonanceplanner.agent.client.ModelRequest;
import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.agent.client.ToolCall;
import com.resonanceplanner.agent.client.ToolDefinition;
import com.resonanceplanner.agent.tools.AgentToolDefinitions;
import com.resonanceplanner.agent.tools.AgentToolServices;
import com.resonanceplanner.agent.tools.AgentTools;
import com.resonanceplanner.catalog.CatalogService;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.EventService;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.planner.PlannerService;
import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleRequest;
import com.resonanceplanner.planning.CalendarService;
import com.resonanceplanner.planning.ProposalService;
import com.resonanceplanner.planning.ProposalStatus;
import com.resonanceplanner.planning.TaskKind;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.roster.RosterService;
import com.resonanceplanner.user.UserService;
import com.resonanceplanner.testsupport.MutableClock;
import com.resonanceplanner.waveplate.WaveplateService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** The loop of the agent with a fake model and the real tools on mocked services: no network, no cost (D63, D128). */
@ExtendWith(MockitoExtension.class)
class AgentLoopTest {

    private static final long USER_ID = 7L;
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    // Saturday 17 October 2026, 14:30 in Italy
    private static final Instant NOW = Instant.parse("2026-10-17T12:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final ScheduleDraft DRAFT = new ScheduleDraft(List.of("jinhsi"),
            List.of(new ScheduleDraft.DayPlan(TODAY, false, 240, 60, 80, 30, List.of(
                    new ScheduleDraft.DraftTask(TaskKind.FARMING, "Farming: Forgery", 30, 80, "forgery-1", 2, 0, null,
                            "jinhsi")))),
            List.of(), false, false, List.of());

    private final JsonMapper jsonMapper =
            JsonMapper.builder().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();
    private final Logger loopLog = (Logger) LoggerFactory.getLogger(AgentLoop.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @Mock private RosterService rosterService;
    @Mock private CatalogService catalogService;
    @Mock private WaveplateService waveplateService;
    @Mock private UserService userService;
    @Mock private EventService eventService;
    @Mock private CalendarService calendarService;
    @Mock private PlannerService plannerService;
    @Mock private ProposalService proposalService;

    private final MutableClock clock = new MutableClock(NOW);
    private FakeModelClient model;
    private AgentLoop loop;

    @BeforeEach
    void setUp() {
        AgentTools tools = new AgentTools(new AgentToolDefinitions(jsonMapper), new AgentToolServices(rosterService,
                catalogService, waveplateService, userService, eventService, calendarService, plannerService,
                proposalService), new Today(clock, ROME), ROME, jsonMapper);
        model = new FakeModelClient();
        loop = new AgentLoop(model, tools, clock, ROME);
        logs.start();
        loopLog.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        loopLog.detachAppender(logs);
    }

    private AgentReply ask(String message) {
        return loop.answer(USER_ID, List.of(), message);
    }

    private static String textOf(ModelMessage message) {
        return switch (message) {
            case ModelMessage.User user -> user.text();
            case ModelMessage.Assistant assistant -> assistant.text();
            case ModelMessage.ToolResult result -> result.content();
        };
    }

    // ----- The request -----

    @Test
    void answersWithTheTextOfTheModelWhenItNeedsNoTool() {
        model.answering(text("  Ciao! Posso aiutarti a pianificare la settimana.  "));

        AgentReply reply = ask("Ciao");

        assertThat(reply.text()).isEqualTo("Ciao! Posso aiutarti a pianificare la settimana.");
        assertThat(reply.proposal()).isNull();
        assertThat(reply.steps()).isEmpty();
        assertThat(reply.rounds()).isEqualTo(1);
        ModelRequest request = model.requests().getFirst();
        assertThat(request.systemPrompt()).isEqualTo(AgentPrompt.SYSTEM);
        assertThat(request.tools()).extracting(ToolDefinition::name).hasSize(5).contains("plan_schedule");
        assertThat(request.messages()).hasSize(1);
    }

    @Test
    void putsTheDateAndTimeOfItalyBeforeTheMessageOfTheUser() {
        model.answering(text("Ok."));

        ask("Cosa faccio oggi?");

        // The date goes in the new message, not in the instructions, so the instructions stay the same (D58)
        assertThat(textOf(model.requests().getFirst().messages().getLast()))
                .isEqualTo("Oggi è sabato 17 ottobre 2026, ore 14:30 (ora italiana).\n\nCosa faccio oggi?");
        assertThat(AgentPrompt.SYSTEM).doesNotContain("2026");
    }

    @Test
    void sendsTheLastTenMessagesOfTheHistoryWithoutTheEmptyOnes() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            history.add(new ChatMessage(i % 2 == 1 ? Speaker.USER : Speaker.ASSISTANT, "Messaggio " + i));
        }
        history.add(11, new ChatMessage(Speaker.ASSISTANT, "   "));
        model.answering(text("Ok."));

        loop.answer(USER_ID, history, "E adesso?");

        List<ModelMessage> messages = model.requests().getFirst().messages();
        assertThat(messages).hasSize(11);
        assertThat(messages.getFirst()).isEqualTo(new ModelMessage.User("Messaggio 3"));
        assertThat(messages.get(1)).isEqualTo(new ModelMessage.Assistant("Messaggio 4", List.of()));
        assertThat(messages.get(9)).isEqualTo(new ModelMessage.Assistant("Messaggio 12", List.of()));
    }

    @Test
    void refusesAnEmptyOrTooLongMessage() {
        assertThatThrownBy(() -> ask("   ")).isInstanceOf(FieldValidationException.class)
                .extracting(e -> ((FieldValidationException) e).getFields())
                .isEqualTo(Map.of("content", AgentLoop.WRONG_MESSAGE));
        assertThatThrownBy(() -> ask("a".repeat(AgentLoop.MAX_MESSAGE_CHARS + 1)))
                .isInstanceOf(FieldValidationException.class);
        assertThat(model.requests()).isEmpty();
    }

    @Test
    void acceptsAMessageOfExactlyOneThousandCharacters() {
        model.answering(text("Ok."));

        assertThat(ask("a".repeat(AgentLoop.MAX_MESSAGE_CHARS)).text()).isEqualTo("Ok.");
    }

    @Test
    void startsTheHistoryWithAMessageOfTheUser() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            history.add(new ChatMessage(i % 2 == 1 ? Speaker.USER : Speaker.ASSISTANT, "Messaggio " + i));
        }
        model.answering(text("Ok."));

        loop.answer(USER_ID, history, "E adesso?");

        // The last 10 start with an answer (message 2): some free models refuse that, so it is left out
        List<ModelMessage> messages = model.requests().getFirst().messages();
        assertThat(messages).hasSize(10);
        assertThat(messages.getFirst()).isEqualTo(new ModelMessage.User("Messaggio 3"));
    }

    // ----- Tools -----

    @Test
    void runsTheToolsTheModelAsksForAndSendsBackTheirResults() {
        when(eventService.between(TODAY, TODAY.plusDays(13))).thenReturn(List.of(event("Simulazione doppia")));
        model.answering(tools(call("call_a", "get_events", "{}")), text("C'è la Simulazione doppia."));

        AgentReply reply = ask("Quali eventi ci sono?");

        assertThat(reply.text()).isEqualTo("C'è la Simulazione doppia.");
        assertThat(reply.rounds()).isEqualTo(2);
        assertThat(reply.steps()).containsExactly("Ho controllato gli eventi");
        List<ModelMessage> second = model.requests().get(1).messages();
        assertThat(second).hasSize(3);
        assertThat(second.get(1)).isInstanceOf(ModelMessage.Assistant.class);
        assertThat(second.get(2)).isInstanceOfSatisfying(ModelMessage.ToolResult.class, result -> {
            assertThat(result.toolCallId()).isEqualTo("call_a");
            assertThat(result.content()).contains("Simulazione doppia");
        });
    }

    @Test
    void sendsTheSameFixedPartsAtEveryRoundAndOnlyAddsToTheConversation() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(call("call_a", "get_events", "{}")), tools(call("call_b", "get_events", "{}")),
                text("Nessun evento."));

        ask("Eventi?");

        List<ModelRequest> requests = model.requests();
        assertThat(requests).hasSize(3);
        for (int i = 1; i < requests.size(); i++) {
            // The prefix the provider can keep in cache never changes (D54)
            assertThat(requests.get(i).systemPrompt()).isEqualTo(requests.getFirst().systemPrompt());
            assertThat(requests.get(i).tools()).isEqualTo(requests.getFirst().tools());
            assertThat(requests.get(i).messages()).startsWith(requests.get(i - 1).messages().toArray(ModelMessage[]::new));
        }
    }

    @Test
    void runsEveryToolOfAReplyInOrder() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(call("call_1", "get_events", "{}"), call("call_2", "get_events",
                "{\"from\": \"2026-10-20\"}")), text("Fatto."));

        ask("Eventi?");

        List<ModelMessage> second = model.requests().get(1).messages();
        assertThat(second.subList(2, 4)).extracting(message -> ((ModelMessage.ToolResult) message).toolCallId())
                .containsExactly("call_1", "call_2");
    }

    @Test
    void runsAtMostFiveToolsOfAReply() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        List<ToolCall> calls = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            calls.add(call("call_" + i, "get_events", "{}"));
        }
        model.answering(tools(calls.toArray(ToolCall[]::new)), text("Fatto."));

        ask("Eventi?");

        verify(eventService, times(AgentLoop.MAX_TOOLS_PER_REPLY)).between(any(), any());
        List<ModelMessage> second = model.requests().get(1).messages();
        // Every call gets its result: the ones after the fifth get an error
        assertThat(second).hasSize(2 + 7);
        assertThat(textOf(second.get(7))).contains("Troppi strumenti");
        assertThat(textOf(second.get(8))).contains("Troppi strumenti");
    }

    @Test
    void givesTheModelAFixedErrorWhenAToolFailsUnexpectedly() {
        when(eventService.between(any(), any())).thenThrow(new IllegalStateException("Connessione al database persa"));
        model.answering(tools(call("call_a", "get_events", "{}")), text("Non riesco a leggere gli eventi."));

        AgentReply reply = ask("Eventi?");

        assertThat(reply.text()).isEqualTo("Non riesco a leggere gli eventi.");
        assertThat(textOf(model.requests().get(1).messages().get(2))).isEqualTo(AgentLoop.TOOL_FAILED)
                .doesNotContain("database");
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(line -> assertThat(line).contains("get_events", "IllegalStateException"))
                .allSatisfy(line -> assertThat(line).doesNotContain("Connessione al database persa"));
    }

    @Test
    void stopsTheTurnWhenTheUserNoLongerExists() {
        when(calendarService.between(any(), any(), any())).thenThrow(new UnauthorizedException());
        model.answering(tools(call("call_a", "get_calendar", "{}")));

        assertThatThrownBy(() -> ask("Cosa faccio oggi?")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void letsAnErrorOfTheModelThrough() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(call("call_a", "get_events", "{}")))
                .thenFailingWith(new ApiException(ErrorCode.AGENT_UNAVAILABLE, "L'assistente non è disponibile."));

        assertThatThrownBy(() -> ask("Ciao")).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.AGENT_UNAVAILABLE);
        // The failed turn is in the logs too, with what it spent
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(line -> assertThat(line).contains("user 7 failed after 1 rounds", "AGENT_UNAVAILABLE"));
    }

    @Test
    void answersWithAFixedTextWhenTheModelFailsAfterSavingTheProposal() {
        ProposalResponse saved = givenAPlanThatCanBeSaved();
        model.answering(tools(call("call_a", "plan_schedule", "{}")),
                        tools(call("call_b", "create_plan_proposal", "{\"summary\": \"Farming per Jinhsi.\"}")))
                .thenFailingWith(new ApiException(ErrorCode.AGENT_UNAVAILABLE, "L'assistente non è disponibile."));

        AgentReply reply = ask("Organizzami la settimana");

        // The proposal is in the calendar: the user is told so instead of getting an error
        assertThat(reply.text()).isEqualTo(AgentLoop.PROPOSAL_READY);
        assertThat(reply.proposal()).isEqualTo(saved);
    }

    @Test
    void stopsWhenTheTimeOfTheTurnIsOver() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        // Every call of the model takes 70 seconds: after two calls the 2 minutes are over
        model.onEachCall(() -> clock.advance(Duration.ofSeconds(70)))
                .answering(tools(call("call_a", "get_events", "{}")), tools(call("call_b", "get_events", "{}")),
                        text("Troppo tardi."));

        assertThatThrownBy(() -> ask("Eventi?")).isInstanceOf(AgentStoppedException.class);
        assertThat(model.requests()).hasSize(2);
    }

    @Test
    void runsAtMostTenToolsInATurn() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(fiveCalls("a")), tools(fiveCalls("b")), tools(call("call_c", "get_events", "{}")),
                text("Fatto."));

        ask("Eventi?");

        verify(eventService, times(AgentLoop.MAX_TOOLS_PER_TURN)).between(any(), any());
        assertThat(textOf(model.requests().get(3).messages().getLast())).isEqualTo(AgentLoop.TOO_MANY_TOOLS_IN_TURN);
    }

    @Test
    void keepsTheTextOfAReplyThatAlsoAsksForTools() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(new ModelReply("Controllo gli eventi.", List.of(call("call_a", "get_events", "{}")),
                FinishReason.TOOL_CALLS, TokenUsage.NONE), text("Non ci sono eventi."));

        AgentReply reply = ask("Eventi?");

        assertThat(reply.text()).isEqualTo("Non ci sono eventi.");
        assertThat(textOf(model.requests().get(1).messages().get(1))).isEqualTo("Controllo gli eventi.");
    }

    @Test
    void sendsTheModelTheErrorOfAnUnknownTool() {
        model.answering(tools(call("call_a", "delete_calendar", "{}")), text("Non posso farlo."));

        ask("Cancella tutto");

        assertThat(textOf(model.requests().get(1).messages().get(2))).contains("Strumento sconosciuto");
    }

    @Test
    void addsANoteWhenTheLastPlanIsNotTheSavedProposal() {
        ProposalResponse saved = givenAPlanThatCanBeSaved();
        model.answering(tools(call("call_a", "plan_schedule", "{}")),
                tools(call("call_b", "create_plan_proposal", "{\"summary\": \"Farming per Jinhsi.\"}")),
                tools(call("call_c", "plan_schedule", "{}")),
                text("Ecco un piano diverso."));

        AgentReply reply = ask("Organizzami la settimana");

        assertThat(reply.text()).isEqualTo("Ecco un piano diverso." + AgentLoop.OLDER_PROPOSAL_NOTE);
        assertThat(reply.proposal()).isEqualTo(saved);
    }

    private static ToolCall[] fiveCalls(String prefix) {
        ToolCall[] calls = new ToolCall[AgentLoop.MAX_TOOLS_PER_REPLY];
        for (int i = 0; i < calls.length; i++) {
            calls[i] = call("call_" + prefix + i, "get_events", "{}");
        }
        return calls;
    }

    // ----- The end of a turn -----

    @Test
    void stopsAfterSixRoundsWithAMessageToRephrase() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        for (int i = 1; i <= AgentLoop.MAX_ROUNDS; i++) {
            model.answering(tools(call("call_" + i, "get_events", "{}")));
        }

        assertThatThrownBy(() -> ask("Eventi?"))
                .isInstanceOf(AgentStoppedException.class)
                .hasMessage(AgentStoppedException.MESSAGE)
                .extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.AGENT_UNAVAILABLE);
        assertThat(model.requests()).hasSize(AgentLoop.MAX_ROUNDS);
    }

    @ParameterizedTest
    @CsvSource({"'Il piano è', LENGTH", "'', CONTENT_FILTER", "'   ', STOP"})
    void stopsOnATruncatedBlockedOrEmptyAnswer(String text, FinishReason reason) {
        model.answering(ending(text, reason));

        assertThatThrownBy(() -> ask("Organizzami la settimana")).isInstanceOf(AgentStoppedException.class);
    }

    @Test
    void acceptsAnAnswerWhoseReasonIsUnknown() {
        model.answering(ending("Ecco la risposta.", FinishReason.OTHER));

        assertThat(ask("Ciao").text()).isEqualTo("Ecco la risposta.");
    }

    @ParameterizedTest
    @CsvSource({"'Ho preparato un piano che', LENGTH", "'', CONTENT_FILTER", "'  ', STOP"})
    void answersWithAFixedTextWhenTheProposalIsSavedButTheAnswerIsMissing(String text, FinishReason reason) {
        ProposalResponse saved = givenAPlanThatCanBeSaved();
        model.answering(tools(call("call_a", "plan_schedule", "{}")),
                tools(call("call_b", "create_plan_proposal", "{\"summary\": \"Farming per Jinhsi.\"}")),
                ending(text, reason));

        AgentReply reply = ask("Organizzami la settimana");

        assertThat(reply.text()).isEqualTo(AgentLoop.PROPOSAL_READY);
        assertThat(reply.proposal()).isEqualTo(saved);
        assertThat(reply.steps()).containsExactly("Ho calcolato il piano", "Ho preparato la proposta");
    }

    @Test
    void answersWithAFixedTextWhenTheProposalIsSavedButTheRoundsAreOver() {
        ProposalResponse saved = givenAPlanThatCanBeSaved();
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(call("call_a", "plan_schedule", "{}")),
                tools(call("call_b", "create_plan_proposal", "{\"summary\": \"Farming per Jinhsi.\"}")));
        for (int i = 3; i <= AgentLoop.MAX_ROUNDS; i++) {
            model.answering(tools(call("call_" + i, "get_events", "{}")));
        }

        AgentReply reply = ask("Organizzami la settimana");

        assertThat(reply.text()).isEqualTo(AgentLoop.PROPOSAL_READY);
        assertThat(reply.proposal()).isEqualTo(saved);
        assertThat(reply.rounds()).isEqualTo(AgentLoop.MAX_ROUNDS);
    }

    @Test
    void addsUpTheTokensOfEveryRound() {
        when(eventService.between(any(), any())).thenReturn(List.of());
        model.answering(tools(call("call_a", "get_events", "{}")), text("Fatto."));

        TokenUsage usage = ask("Eventi?").usage();

        assertThat(usage).isEqualTo(FakeModelClient.TOOL_TOKENS.plus(FakeModelClient.TEXT_TOKENS));
    }

    // ----- Logs -----

    @Test
    void logsWhatHappenedWithoutAnyText() {
        when(eventService.between(any(), any())).thenReturn(List.of(event("Evento segretissimo")));
        model.answering(tools(call("call_a", "get_events", "{}"), call("call_b", "cancella_tutto", "{}")),
                text("Risposta riservata"));

        ask("Il mio messaggio privato");

        List<String> lines = logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("user 7", "2 rounds", "get_events", "unknown"));
        assertThat(lines).allSatisfy(line -> assertThat(line)
                .doesNotContain("Il mio messaggio privato", "Risposta riservata", "Evento segretissimo", "cancella_tutto"));
    }

    private ProposalResponse givenAPlanThatCanBeSaved() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        ProposalResponse saved = new ProposalResponse(9L, ProposalStatus.PENDING, "Farming per Jinhsi.", NOW,
                List.of());
        when(proposalService.saveIfUnchanged(eq(USER_ID), eq(ScheduleRequest.defaults()), eq(DRAFT), any()))
                .thenReturn(saved);
        return saved;
    }

    private static EventResponse event(String name) {
        return new EventResponse(3L, name, EventType.DOUBLE_DROP, "Ricompense doppie.", TODAY, TODAY.plusDays(6),
                null, List.of(TODAY), List.of("Astrite"), List.of(), null);
    }
}
