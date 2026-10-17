package com.resonanceplanner.agent.tools;

import static com.resonanceplanner.agent.tools.AgentToolDefinitions.CREATE_PLAN_PROPOSAL;
import static com.resonanceplanner.agent.tools.AgentToolDefinitions.GET_CALENDAR;
import static com.resonanceplanner.agent.tools.AgentToolDefinitions.GET_EVENTS;
import static com.resonanceplanner.agent.tools.AgentToolDefinitions.GET_PLAYER_STATUS;
import static com.resonanceplanner.agent.tools.AgentToolDefinitions.PLAN_SCHEDULE;

import com.resonanceplanner.agent.client.ToolCall;
import com.resonanceplanner.agent.client.ToolDefinition;
import com.resonanceplanner.agent.tools.AgentTurn.PlannedDraft;
import com.resonanceplanner.catalog.dto.CharacterSummaryResponse;
import com.resonanceplanner.common.config.Today;
import com.resonanceplanner.common.error.ApiException;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.ErrorCode;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.event.EventRules;
import com.resonanceplanner.planner.PlannerService;
import com.resonanceplanner.planner.ScheduleDraft;
import com.resonanceplanner.planner.ScheduleRequest;
import com.resonanceplanner.planning.dto.CalendarResponse;
import com.resonanceplanner.planning.dto.ProposalResponse;
import com.resonanceplanner.waveplate.dto.WaveplateResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the tool the model asks for, always for the user of the turn (D44, D55, D127). The arguments are checked like
 * the body of a request, and every error of the arguments or of the services goes back to the model as an error
 * result it can correct (D61): only a user that no longer exists (401) and unexpected errors stop the turn.
 */
@Component
public class AgentTools {

    static final int MAX_ARGUMENTS_CHARS = 8_192;
    static final int MAX_SUMMARY_CHARS = 2_000;
    static final int MAX_RESULT_CHARS = 32_000;
    // Without dates the tools read two weeks, the longest plan (D123)
    static final int DEFAULT_RANGE_DAYS = PlannerService.MAX_DAYS;

    static final String STEP_PLAYER_STATUS = "Ho letto il tuo roster e le Waveplate";
    static final String STEP_EVENTS = "Ho controllato gli eventi";
    static final String STEP_CALENDAR = "Ho guardato il tuo calendario";
    static final String STEP_PLAN = "Ho calcolato il piano";
    static final String STEP_PROPOSAL = "Ho preparato la proposta";

    private static final String TOO_LONG =
            "Gli argomenti sono troppo lunghi: al massimo " + MAX_ARGUMENTS_CHARS + " caratteri.";
    private static final String NOT_AN_OBJECT = "Gli argomenti devono essere un oggetto JSON con i parametri dello strumento.";
    private static final String INVALID_JSON =
            "Gli argomenti non sono JSON valido: scrivi un oggetto JSON completo, ogni parametro una volta sola.";
    private static final String TOO_MUCH_DATA =
            "Troppi dati per un solo risultato: chiedi un intervallo di date più breve.";
    private static final String UNKNOWN_PARAMETER = "Parametro sconosciuto.";
    private static final String WRONG_DATE = "Data non valida: scrivila come AAAA-MM-GG.";
    private static final String WRONG_VALUE = "Valore non valido per questo parametro.";
    private static final String NO_PLAN =
            "Nessun piano in questo turno: prima calcola il piano con plan_schedule, poi salvalo.";
    private static final String PROPOSAL_SAVED =
            "Proposta salvata: l'utente la vede nel calendario e può confermare o scartare. Il calendario non è ancora cambiato.";
    private static final int MAX_NAME_IN_MESSAGE = 50;
    private static final String NO_WAVEPLATES = "L'utente non ha ancora salvato le Waveplate: il piano conta "
            + "le Waveplate di un giorno intero anche per oggi.";
    private static final String NO_PLAY_TIME = "L'utente non ha scelto il tempo di gioco: il piano usa "
            + PlannerService.DEFAULT_MINUTES + " minuti al giorno.";

    private final AgentToolDefinitions definitions;
    private final AgentToolServices services;
    private final Today today;
    private final ZoneId zone;
    private final JsonMapper jsonMapper;
    // Stricter than the mapper of the API, like DataFileReader: an unknown key, as a userId, is an error
    private final JsonMapper argumentsMapper;

    public AgentTools(AgentToolDefinitions definitions, AgentToolServices services, Today today, ZoneId appZone,
            JsonMapper jsonMapper) {
        this.definitions = definitions;
        this.services = services;
        this.today = today;
        this.zone = appZone;
        this.jsonMapper = jsonMapper;
        this.argumentsMapper = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                // "7" is not a number of days and "true" is not a yes: each value must have its own JSON type
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // A date must be a text like 2026-10-17: without this a number would become a day counted from 1970
                .defaultLeniency(false)
                .build();
    }

    public List<ToolDefinition> definitions() {
        return definitions.all();
    }

    /** Runs one tool call of the model; a tool that ran adds its step to the turn (D62). */
    public ToolOutcome run(AgentTurn turn, ToolCall call) {
        if (PLAN_SCHEDULE.equals(call.name())) {
            // A plan_schedule that fails, even on its arguments, leaves no plan: the summary of the model would
            // describe the failed one, not the plan before it (D56)
            turn.forgetPlan();
        }
        ToolOutcome outcome;
        try {
            outcome = switch (call.name()) {
                case GET_PLAYER_STATUS -> playerStatus(turn, read(call, ToolArguments.None.class));
                case GET_EVENTS -> events(read(call, ToolArguments.Range.class));
                case GET_CALENDAR -> calendar(turn, read(call, ToolArguments.Range.class));
                case PLAN_SCHEDULE -> plan(turn, read(call, ScheduleRequest.class));
                case CREATE_PLAN_PROPOSAL -> proposal(turn, read(call, ToolArguments.Proposal.class));
                default -> failure("Strumento sconosciuto. Gli strumenti sono: "
                        + String.join(", ", AgentToolDefinitions.names()) + ".", Map.of());
            };
        } catch (UnauthorizedException e) {
            // The user of the token no longer exists: the turn stops with 401 (D102), the model can do nothing
            throw e;
        } catch (ApiException e) {
            outcome = failure(e.getMessage(), e.getFields());
        }
        if (!outcome.error()) {
            turn.stepDone(outcome.step());
        }
        return outcome;
    }

    private ToolOutcome playerStatus(AgentTurn turn, ToolArguments.None none) {
        Long userId = turn.userId();
        Map<String, String> names = services.catalogService().listCharacters().stream()
                .collect(Collectors.toMap(CharacterSummaryResponse::id, CharacterSummaryResponse::name));
        List<ToolResults.RosterCharacter> roster = services.rosterService().list(userId).stream()
                .map(entry -> new ToolResults.RosterCharacter(entry.characterId(),
                        names.getOrDefault(entry.characterId(), entry.characterId()), entry.currentLevel(),
                        entry.currentRank(), entry.targetLevel(), entry.targetRank()))
                .toList();
        ToolResults.Waveplates waveplates = waveplatesOf(userId);
        Integer playTime = services.userService().getProfile(userId).playTimeMinutes();
        List<String> notes = new ArrayList<>();
        if (waveplates == null) {
            notes.add(NO_WAVEPLATES);
        }
        if (playTime == null) {
            notes.add(NO_PLAY_TIME);
        }
        return success(new ToolResults.PlayerStatus(roster, waveplates, playTime, notes), STEP_PLAYER_STATUS);
    }

    // A user who has not saved the Waveplate yet still gets a plan: the planner counts a full day of them (D123)
    private ToolResults.Waveplates waveplatesOf(Long userId) {
        WaveplateResponse waveplate;
        try {
            waveplate = services.waveplateService().get(userId);
        } catch (NotFoundException e) {
            return null;
        }
        return new ToolResults.Waveplates(waveplate.value(), waveplate.max(), inItalianTime(waveplate.fullAt()),
                waveplate.crystals(), waveplate.crystalsMax(), inItalianTime(waveplate.crystalsFullAt()));
    }

    private ToolOutcome events(ToolArguments.Range range) {
        LocalDate from = fromOf(range);
        return bounded(new ToolResults.Events(services.eventService().between(from, toOf(range, from))), STEP_EVENTS);
    }

    private ToolOutcome calendar(AgentTurn turn, ToolArguments.Range range) {
        LocalDate from = fromOf(range);
        CalendarResponse calendar = services.calendarService().between(turn.userId(), from, toOf(range, from));
        ProposalResponse pending = calendar.pendingProposal();
        ToolResults.PendingProposal pendingProposal =
                pending == null ? null : new ToolResults.PendingProposal(pending.id(), pending.summary());
        return bounded(new ToolResults.Calendar(calendar.tasks(), pendingProposal, calendar.planOutdated()),
                STEP_CALENDAR);
    }

    private ToolOutcome plan(AgentTurn turn, ScheduleRequest request) {
        ScheduleDraft draft = services.plannerService().draft(turn.userId(), request);
        turn.planned(request, draft);
        return success(draft, STEP_PLAN);
    }

    // The proposal is the last plan of the turn, calculated again before it is saved (D56, D127)
    private ToolOutcome proposal(AgentTurn turn, ToolArguments.Proposal arguments) {
        String summary = arguments.summary() == null ? "" : arguments.summary().strip();
        if (summary.isEmpty()) {
            throw new FieldValidationException("summary", "Scrivi la spiegazione del piano per l'utente.");
        }
        if (summary.length() > MAX_SUMMARY_CHARS) {
            throw new FieldValidationException("summary",
                    "La spiegazione può avere al massimo " + MAX_SUMMARY_CHARS + " caratteri.");
        }
        PlannedDraft plan = turn.lastPlan().orElseThrow(() -> new ConflictException(NO_PLAN));
        ProposalResponse saved = services.proposalService()
                .saveIfUnchanged(turn.userId(), plan.request(), plan.draft(), summary);
        turn.proposalSaved(saved);
        return success(new ToolResults.ProposalSaved(saved.id(), saved.tasks().size(), PROPOSAL_SAVED), STEP_PROPOSAL);
    }

    // The window of the dates of the app (D113) is checked first: a year like +999999999 cannot get 13 more days
    private LocalDate fromOf(ToolArguments.Range range) {
        LocalDate from = range.from() == null ? today.date() : range.from();
        if (!EventRules.isInWindow(from)) {
            throw new FieldValidationException("from", EventRules.DATE_WINDOW);
        }
        return from;
    }

    private static LocalDate toOf(ToolArguments.Range range, LocalDate from) {
        return range.to() == null ? from.plusDays(DEFAULT_RANGE_DAYS - 1L) : range.to();
    }

    private String inItalianTime(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zone));
    }

    /** The arguments of the model as the given type, or an ApiException that tells the model what is wrong. */
    private <T> T read(ToolCall call, Class<T> type) {
        if (call.arguments().length() > MAX_ARGUMENTS_CHARS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, TOO_LONG);
        }
        JsonNode node;
        try {
            node = argumentsMapper.readTree(call.arguments());
        } catch (JacksonException e) {
            // Also a text cut in the middle, a key written twice or lists nested too deep
            throw new ApiException(ErrorCode.VALIDATION_ERROR, INVALID_JSON);
        }
        // Some models send nothing, or null, for a tool without parameters: the same as {}
        if (node == null || node.isMissingNode() || node.isNull()) {
            node = argumentsMapper.createObjectNode();
        }
        if (!node.isObject()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, NOT_AN_OBJECT);
        }
        try {
            return argumentsMapper.treeToValue(node, type);
        } catch (UnrecognizedPropertyException e) {
            String name = shortened(e.getPropertyName());
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Parametro sconosciuto: " + name + ". Usa solo i parametri dello strumento.",
                    Map.of(name, UNKNOWN_PARAMETER));
        } catch (JacksonException e) {
            throw wrongValue(e);
        }
    }

    // The parameter is the first name in the path of the error: excludedDates for a wrong date in that list
    private static ApiException wrongValue(JacksonException e) {
        String field = e.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (field == null) {
            return new ApiException(ErrorCode.VALIDATION_ERROR, WRONG_VALUE);
        }
        boolean date = e instanceof MismatchedInputException mismatch && mismatch.getTargetType() == LocalDate.class;
        return new FieldValidationException(shortened(field), date ? WRONG_DATE : WRONG_VALUE);
    }

    private static String shortened(String name) {
        String safe = name == null ? "" : name;
        return safe.length() <= MAX_NAME_IN_MESSAGE ? safe : safe.substring(0, MAX_NAME_IN_MESSAGE) + "...";
    }

    private ToolOutcome success(Object result, String step) {
        return ToolOutcome.success(jsonMapper.writeValueAsString(result), step);
    }

    // Events and tasks hold texts written by people (descriptions, notes): two months of them can be a lot, and every
    // text is also room for instructions hidden in the data (D60). The model asks again for fewer days
    private ToolOutcome bounded(Object result, String step) {
        String content = jsonMapper.writeValueAsString(result);
        return content.length() <= MAX_RESULT_CHARS
                ? ToolOutcome.success(content, step)
                : failure(TOO_MUCH_DATA, Map.of());
    }

    private ToolOutcome failure(String message, Map<String, String> fields) {
        return ToolOutcome.failure(jsonMapper.writeValueAsString(new ToolResults.Failure(message, fields)));
    }
}
