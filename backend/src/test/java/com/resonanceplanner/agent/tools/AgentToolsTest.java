package com.resonanceplanner.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.resonanceplanner.agent.client.ToolCall;
import com.resonanceplanner.agent.client.ToolDefinition;
import com.resonanceplanner.catalog.CatalogService;
import com.resonanceplanner.catalog.Element;
import com.resonanceplanner.catalog.WeaponType;
import com.resonanceplanner.catalog.dto.CharacterSummaryResponse;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.NotFoundException;
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
import com.resonanceplanner.planning.TaskOrigin;
import com.resonanceplanner.planning.TaskStatus;
import com.resonanceplanner.planning.dto.CalendarResponse;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.planning.dto.TaskResponse;
import com.resonanceplanner.roster.RosterService;
import com.resonanceplanner.roster.dto.RosterEntryResponse;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.UserService;
import com.resonanceplanner.user.dto.UserResponse;
import com.resonanceplanner.waveplate.WaveplateService;
import com.resonanceplanner.waveplate.dto.WaveplateResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The five tools of the agent (D55, D127) on mocked services: what they read, what they give back, their errors. */
@ExtendWith(MockitoExtension.class)
class AgentToolsTest {

    private static final long USER_ID = 7L;
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    // 17 October 2026, 10:00 in Italy
    private static final Instant NOW = Instant.parse("2026-10-17T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final ScheduleDraft DRAFT = new ScheduleDraft(List.of("jinhsi"),
            List.of(new ScheduleDraft.DayPlan(TODAY, false, 120, 60, 80, 30, List.of(
                    new ScheduleDraft.DraftTask(TaskKind.FARMING, "Farming: Forgery", 30, 80, "forgery-1", 2, 0, null,
                            "jinhsi")))),
            List.of(new ScheduleDraft.Coverage("metallic-drip", "Metallic Drip", 12, 8)), false, true,
            List.of("Servono altri 3 giorni per completare l'obiettivo."));

    // Like the mapper of Spring (application.yml): 7.5 for a whole number is an error
    private final JsonMapper jsonMapper =
            JsonMapper.builder().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).build();

    @Mock private RosterService rosterService;
    @Mock private CatalogService catalogService;
    @Mock private WaveplateService waveplateService;
    @Mock private UserService userService;
    @Mock private EventService eventService;
    @Mock private CalendarService calendarService;
    @Mock private PlannerService plannerService;
    @Mock private ProposalService proposalService;

    private AgentTools tools;
    private AgentTurn turn;

    @BeforeEach
    void setUp() {
        Today today = new Today(Clock.fixed(NOW, ZoneOffset.UTC), ROME);
        tools = new AgentTools(new AgentToolDefinitions(jsonMapper), new AgentToolServices(rosterService,
                catalogService, waveplateService, userService, eventService, calendarService, plannerService,
                proposalService), today, ROME, jsonMapper);
        turn = new AgentTurn(USER_ID);
    }

    private ToolOutcome run(String name, String arguments) {
        return tools.run(turn, new ToolCall("call_1", name, arguments));
    }

    private JsonNode json(ToolOutcome outcome) {
        return jsonMapper.readTree(outcome.content());
    }

    // ----- Definitions -----

    @Test
    void offersTheFiveToolsWithStrictSchemasInItalian() {
        List<ToolDefinition> definitions = tools.definitions();

        assertThat(definitions).extracting(ToolDefinition::name).containsExactly(
                "get_player_status", "get_events", "get_calendar", "plan_schedule", "create_plan_proposal");
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.description()).isNotBlank();
            assertThat(definition.parameters()).containsEntry("type", "object")
                    .containsEntry("additionalProperties", false);
            // No tool can be asked for the data of another user (D55)
            assertThat(definition.parameters().toString()).doesNotContainIgnoringCase("userId");
        });
    }

    @Test
    void writesThePlanParametersInTheOrderOfTheDocument() {
        ToolDefinition planSchedule = tools.definitions().get(3);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) planSchedule.parameters().get("properties");
        assertThat(properties.keySet()).containsExactly("startDate", "days", "characterPriority", "excludedDates",
                "includeEventActivities", "maxMinutesPerDay");
        assertThat(tools.definitions().get(4).parameters().get("required")).isEqualTo(List.of("summary"));
    }

    // ----- get_player_status -----

    @Test
    void readsTheRosterWithNamesTheWaveplatesInItalianTimeAndThePlayTime() {
        when(rosterService.list(USER_ID)).thenReturn(List.of(new RosterEntryResponse("jinhsi", 20, 1, 40, 2)));
        when(catalogService.listCharacters()).thenReturn(List.of(new CharacterSummaryResponse(
                "jinhsi", "Jinhsi", Element.SPECTRO, WeaponType.BROADBLADE, 5, "Main DPS", "characters/jinhsi.webp")));
        when(waveplateService.get(USER_ID)).thenReturn(new WaveplateResponse(120, 30, 240, 480,
                Instant.parse("2026-10-17T20:00:00Z"), Instant.parse("2026-10-18T08:00:00Z"), NOW));
        when(userService.getProfile(USER_ID))
                .thenReturn(new UserResponse(USER_ID, "rover", "rover@example.com", Role.USER, 60, true));

        ToolOutcome outcome = run("get_player_status", "{}");

        assertThat(outcome.error()).isFalse();
        assertThat(outcome.step()).isEqualTo("Ho letto il tuo roster e le Waveplate");
        JsonNode status = json(outcome);
        assertThat(status.at("/roster/0/characterId").asString()).isEqualTo("jinhsi");
        assertThat(status.at("/roster/0/name").asString()).isEqualTo("Jinhsi");
        assertThat(status.at("/roster/0/currentLevel").asInt()).isEqualTo(20);
        assertThat(status.at("/roster/0/targetRank").asInt()).isEqualTo(2);
        assertThat(status.at("/waveplates/value").asInt()).isEqualTo(120);
        assertThat(status.at("/waveplates/fullAt").asString()).isEqualTo("2026-10-17T22:00:00+02:00");
        assertThat(status.at("/waveplates/crystals").asInt()).isEqualTo(30);
        assertThat(status.at("/playTimeMinutes").asInt()).isEqualTo(60);
        // The model never receives the email or other data of the profile (D60, D127)
        assertThat(outcome.content()).doesNotContain("rover@example.com", "rover\"", "USER");
        assertThat(turn.steps()).containsExactly("Ho letto il tuo roster e le Waveplate");
    }

    @Test
    void tellsTheModelWhatCountsWhenWaveplatesAndPlayTimeAreNotSavedYet() {
        when(rosterService.list(USER_ID)).thenReturn(List.of());
        when(catalogService.listCharacters()).thenReturn(List.of());
        when(waveplateService.get(USER_ID)).thenThrow(new NotFoundException("Non hai ancora salvato le tue Waveplate."));
        when(userService.getProfile(USER_ID))
                .thenReturn(new UserResponse(USER_ID, "rover", "rover@example.com", Role.USER, null, false));

        ToolOutcome outcome = run("get_player_status", "{}");

        assertThat(outcome.error()).isFalse();
        JsonNode status = json(outcome);
        assertThat(status.at("/waveplates").isNull()).isTrue();
        assertThat(status.at("/playTimeMinutes").isNull()).isTrue();
        assertThat(status.at("/notes/0").asString()).contains("Waveplate");
        assertThat(status.at("/notes/1").asString()).contains("60 minuti");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"userId\": 2}", "{\"user_id\": 2}"})
    void refusesAUserInTheArgumentsAndReadsNothing(String arguments) {
        ToolOutcome outcome = run("get_player_status", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(outcome.step()).isNull();
        assertThat(json(outcome).at("/error").asString()).contains("Parametro sconosciuto");
        verifyNoInteractions(rosterService, waveplateService, userService);
        assertThat(turn.steps()).isEmpty();
    }

    // ----- get_events -----

    @Test
    void readsTheEventsFromTodayForFourteenDaysWithoutDates() {
        when(eventService.between(TODAY, TODAY.plusDays(13))).thenReturn(List.of(event()));

        ToolOutcome outcome = run("get_events", "{}");

        assertThat(outcome.error()).isFalse();
        assertThat(outcome.step()).isEqualTo("Ho controllato gli eventi");
        assertThat(json(outcome).at("/events/0/name").asString()).isEqualTo("Simulazione doppia");
    }

    @Test
    void readsTheEventsOfTheRangeAndFourteenDaysFromTheStartWithoutTheEnd() {
        when(eventService.between(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25))).thenReturn(List.of());
        when(eventService.between(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 2))).thenReturn(List.of());

        assertThat(run("get_events", "{\"from\": \"2026-10-20\", \"to\": \"2026-10-25\"}").error()).isFalse();
        assertThat(run("get_events", "{\"from\": \"2026-10-20\"}").error()).isFalse();

        verify(eventService).between(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25));
        verify(eventService).between(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 2));
        // The same step twice is one step
        assertThat(turn.steps()).containsExactly("Ho controllato gli eventi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"from\": \"17/10/2026\"}", "{\"from\": \"2026-02-30\"}", "{\"from\": 20261017}"})
    void answersWithTheFieldWhenADateIsWrittenWrong(String arguments) {
        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/from").asString()).contains("AAAA-MM-GG");
        verifyNoInteractions(eventService);
    }

    @Test
    void givesTheModelTheErrorsOfTheServiceWithTheirFields() {
        when(eventService.between(any(), any()))
                .thenThrow(new FieldValidationException("to", "L'intervallo può coprire al massimo 62 giorni."));

        ToolOutcome outcome = run("get_events", "{\"from\": \"2026-10-17\", \"to\": \"2027-01-30\"}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).isNotBlank();
        assertThat(json(outcome).at("/fields/to").asString()).isEqualTo("L'intervallo può coprire al massimo 62 giorni.");
        assertThat(turn.steps()).isEmpty();
    }

    // ----- get_calendar -----

    @Test
    void readsTheTasksThePendingProposalAndWhetherThePlanIsOutdated() {
        TaskResponse task = new TaskResponse(11L, TaskOrigin.MANUAL, TaskKind.CUSTOM, TaskStatus.CONFIRMED, TODAY,
                "Boss settimanale", "Con gli amici", 20, null, null, 0, null, null, null);
        ProposalResponse pending = new ProposalResponse(5L, ProposalStatus.PENDING, "Piano base di 7 giorni.", NOW,
                List.of(task));
        when(calendarService.between(USER_ID, TODAY, TODAY.plusDays(13)))
                .thenReturn(new CalendarResponse(List.of(event()), List.of(task), pending, true));

        ToolOutcome outcome = run("get_calendar", "{}");

        assertThat(outcome.error()).isFalse();
        assertThat(outcome.step()).isEqualTo("Ho guardato il tuo calendario");
        JsonNode calendar = json(outcome);
        assertThat(calendar.at("/tasks/0/title").asString()).isEqualTo("Boss settimanale");
        assertThat(calendar.at("/pendingProposal/id").asLong()).isEqualTo(5L);
        assertThat(calendar.at("/pendingProposal/summary").asString()).isEqualTo("Piano base di 7 giorni.");
        assertThat(calendar.at("/pendingProposal/tasks").isMissingNode()).isTrue();
        assertThat(calendar.at("/planOutdated").asBoolean()).isTrue();
        // The events have their own tool
        assertThat(calendar.has("events")).isFalse();
    }

    @Test
    void readsACalendarWithoutAPendingProposal() {
        when(calendarService.between(USER_ID, TODAY, TODAY.plusDays(13)))
                .thenReturn(new CalendarResponse(List.of(), List.of(), null, false));

        JsonNode calendar = json(run("get_calendar", "{}"));

        assertThat(calendar.at("/pendingProposal").isNull()).isTrue();
        assertThat(calendar.at("/tasks").isEmpty()).isTrue();
    }

    // ----- plan_schedule -----

    @Test
    void calculatesThePlanWithTheParametersOfTheModelAndRemembersIt() {
        ScheduleRequest expected = new ScheduleRequest(LocalDate.of(2026, 10, 18), 5, List.of("jinhsi"),
                List.of(LocalDate.of(2026, 10, 19)), false, 90);
        when(plannerService.draft(USER_ID, expected)).thenReturn(DRAFT);

        ToolOutcome outcome = run("plan_schedule", """
                {"startDate": "2026-10-18", "days": 5, "characterPriority": ["jinhsi"],
                 "excludedDates": ["2026-10-19"], "includeEventActivities": false, "maxMinutesPerDay": 90}
                """);

        assertThat(outcome.error()).isFalse();
        assertThat(outcome.step()).isEqualTo("Ho calcolato il piano");
        JsonNode draft = json(outcome);
        assertThat(draft.at("/days/0/tasks/0/title").asString()).isEqualTo("Farming: Forgery");
        assertThat(draft.at("/coverage/0/covered").asInt()).isEqualTo(8);
        assertThat(draft.at("/warnings/0").asString()).startsWith("Servono altri 3 giorni");
        assertThat(turn.lastPlan()).hasValueSatisfying(plan -> {
            assertThat(plan.request()).isEqualTo(expected);
            assertThat(plan.draft()).isEqualTo(DRAFT);
        });
    }

    @Test
    void calculatesThePlanWithTheDefaultsWhenTheModelGivesNoParameters() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);

        assertThat(run("plan_schedule", "{}").error()).isFalse();
    }

    @Test
    void givesTheModelEveryWrongParameterOfThePlannerAndForgetsNoPlan() {
        when(plannerService.draft(any(), any())).thenThrow(new FieldValidationException(Map.of(
                "days", "Il piano copre da 1 a 14 giorni.")));

        ToolOutcome outcome = run("plan_schedule", "{\"days\": 30}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/days").asString()).isEqualTo("Il piano copre da 1 a 14 giorni.");
        assertThat(turn.lastPlan()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"days\": \"sette\"}                 | days",
        "{\"days\": 7.5}                       | days",
        // Each value must have its own JSON type: no text for a number or a yes
        "{\"days\": \"7\"}                     | days",
        "{\"includeEventActivities\": \"true\"} | includeEventActivities",
        "{\"includeEventActivities\": \"forse\"} | includeEventActivities",
        "{\"characterPriority\": \"jinhsi\"}   | characterPriority",
        "{\"excludedDates\": [\"domani\"]}     | excludedDates"
    })
    void answersWithTheFieldWhenAParameterHasTheWrongType(String arguments, String field) {
        ToolOutcome outcome = run("plan_schedule", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/" + field).asString()).isNotBlank();
        verifyNoInteractions(plannerService);
    }

    @Test
    void forgetsThePlanWhenTheNextPlanScheduleFails() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        run("plan_schedule", "{}");

        ToolOutcome failed = run("plan_schedule", "{\"days\": \"tanti\"}");
        ToolOutcome proposal = run("create_plan_proposal", "{\"summary\": \"Il piano di prima.\"}");

        // The summary would describe the plan that failed: nothing is saved (D56)
        assertThat(failed.error()).isTrue();
        assertThat(turn.lastPlan()).isEmpty();
        assertThat(proposal.error()).isTrue();
        verifyNoInteractions(proposalService);
    }

    // ----- create_plan_proposal -----

    @Test
    void savesTheLastPlanOfTheTurnWithTheExplanationOfTheModel() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        ProposalResponse saved = new ProposalResponse(9L, ProposalStatus.PENDING, "Concentro il farming.", NOW,
                List.of());
        when(proposalService.saveIfUnchanged(USER_ID, ScheduleRequest.defaults(), DRAFT, "Concentro il farming."))
                .thenReturn(saved);
        run("plan_schedule", "{}");

        ToolOutcome outcome = run("create_plan_proposal", "{\"summary\": \"  Concentro il farming.  \"}");

        assertThat(outcome.error()).isFalse();
        assertThat(outcome.step()).isEqualTo("Ho preparato la proposta");
        assertThat(json(outcome).at("/proposalId").asLong()).isEqualTo(9L);
        assertThat(json(outcome).at("/message").asString()).contains("confermare");
        assertThat(turn.proposal()).contains(saved);
        assertThat(turn.steps()).containsExactly("Ho calcolato il piano", "Ho preparato la proposta");
    }

    @Test
    void asksForAPlanFirstWhenTheTurnHasNone() {
        ToolOutcome outcome = run("create_plan_proposal", "{\"summary\": \"Ecco il piano.\"}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).contains("plan_schedule");
        verifyNoInteractions(proposalService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"summary\": \"   \"}", "{\"summary\": null}", "{\"summary\": {\"testo\": \"x\"}}"})
    void needsAnExplanationForTheUser(String arguments) {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        run("plan_schedule", "{}");

        ToolOutcome outcome = run("create_plan_proposal", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/summary").asString()).isNotBlank();
        verify(proposalService, never()).saveIfUnchanged(any(), any(), any(), any());
    }

    @Test
    void refusesAnExplanationLongerThanTwoThousandCharacters() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        run("plan_schedule", "{}");

        ToolOutcome outcome = run("create_plan_proposal", "{\"summary\": \"" + "a".repeat(2001) + "\"}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/summary").asString()).contains("2000");
    }

    @Test
    void tellsTheModelToPlanAgainWhenTheDataChanged() {
        when(plannerService.draft(USER_ID, ScheduleRequest.defaults())).thenReturn(DRAFT);
        when(proposalService.saveIfUnchanged(any(), any(), any(), any()))
                .thenThrow(new ConflictException("I dati sono cambiati: richiama plan_schedule."));
        run("plan_schedule", "{}");

        ToolOutcome outcome = run("create_plan_proposal", "{\"summary\": \"Ecco il piano.\"}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).isEqualTo("I dati sono cambiati: richiama plan_schedule.");
        assertThat(turn.proposal()).isEmpty();
    }

    // ----- Every tool -----

    @Test
    void answersAnUnknownToolWithTheNamesOfTheFive() {
        ToolOutcome outcome = run("delete_calendar", "{}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString())
                .contains("get_player_status", "get_events", "get_calendar", "plan_schedule", "create_plan_proposal");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"testo\"", "42"})
    void answersArgumentsThatAreNotAJsonObject(String arguments) {
        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).contains("oggetto JSON");
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"from\": ", "{} {}", "{\"from\": \"2026-10-20\", \"from\": \"2026-10-21\"}"})
    void answersArgumentsThatAreNotValidJson(String arguments) {
        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).contains("JSON valido");
        verifyNoInteractions(eventService);
    }

    @Test
    void answersListsNestedTooDeeplyWithoutGoingDown() {
        String arguments = "{\"from\": " + "[".repeat(2_000) + "]".repeat(2_000) + "}";

        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        verifyNoInteractions(eventService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "  "})
    void readsNoArgumentsAsTheDefaults(String arguments) {
        when(eventService.between(TODAY, TODAY.plusDays(13))).thenReturn(List.of());

        // Some models send nothing for a tool without parameters
        assertThat(run("get_events", arguments).error()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"from\": \"+999999999-12-31\"}", "{\"from\": \"1999-12-31\"}"})
    void refusesAStartOutsideTheDatesOfTheApp(String arguments) {
        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/fields/from").asString()).isNotBlank();
        verifyNoInteractions(eventService);
    }

    @Test
    void asksForFewerDaysWhenTheResultIsTooLong() {
        EventResponse longEvent = new EventResponse(3L, "Evento", EventType.DOUBLE_DROP, "x".repeat(2_000), TODAY,
                TODAY.plusDays(6), null, List.of(TODAY), List.of(), List.of(), null);
        when(eventService.between(any(), any())).thenReturn(Collections.nCopies(20, longEvent));

        ToolOutcome outcome = run("get_events", "{}");

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).contains("intervallo di date più breve");
        assertThat(turn.steps()).isEmpty();
    }

    @Test
    void refusesArgumentsLongerThanEightKilobytes() {
        String arguments = "{\"from\": \"2026-10-17\", \"padding\": \"" + "x".repeat(AgentTools.MAX_ARGUMENTS_CHARS)
                + "\"}";

        ToolOutcome outcome = run("get_events", arguments);

        assertThat(outcome.error()).isTrue();
        assertThat(json(outcome).at("/error").asString()).contains("troppo lunghi");
        verifyNoInteractions(eventService);
    }

    @Test
    void stopsTheTurnWhenTheUserOfTheTokenNoLongerExists() {
        when(calendarService.between(any(), any(), any())).thenThrow(new UnauthorizedException());

        assertThatThrownBy(() -> run("get_calendar", "{}")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void letsAnUnexpectedErrorThrough() {
        when(eventService.between(any(), any())).thenThrow(new IllegalStateException("Database down"));

        assertThatThrownBy(() -> run("get_events", "{}")).isInstanceOf(IllegalStateException.class);
    }

    private static EventResponse event() {
        return new EventResponse(3L, "Simulazione doppia", EventType.DOUBLE_DROP, "Ricompense doppie.", TODAY,
                TODAY.plusDays(6), null, List.of(TODAY), List.of("Astrite"), List.of(), null);
    }
}
