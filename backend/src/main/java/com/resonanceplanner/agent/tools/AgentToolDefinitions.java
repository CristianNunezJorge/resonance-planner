package com.resonanceplanner.agent.tools;

import com.resonanceplanner.agent.client.ToolDefinition;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The five tools the model can ask for (D55, D127): names, descriptions in Italian and JSON Schema of the parameters.
 * The schemas are JSON text, easier to read than nested maps, and are read once at startup into ordered maps, so the
 * request to the model is the same at every call (D58). No schema has a user: the user is the one of the turn (D44).
 */
@Component
public class AgentToolDefinitions {

    static final String GET_PLAYER_STATUS = "get_player_status";
    static final String GET_EVENTS = "get_events";
    static final String GET_CALENDAR = "get_calendar";
    static final String PLAN_SCHEDULE = "plan_schedule";
    static final String CREATE_PLAN_PROPOSAL = "create_plan_proposal";

    private static final String NO_PARAMETERS = """
            {"type": "object", "properties": {}, "additionalProperties": false}
            """;

    private static final String RANGE = """
            {
              "type": "object",
              "properties": {
                "from": {"type": "string", "format": "date",
                         "description": "Primo giorno, AAAA-MM-GG. Se manca: oggi."},
                "to": {"type": "string", "format": "date",
                       "description": "Ultimo giorno, AAAA-MM-GG, al massimo 62 giorni dopo from. Se manca: 13 giorni dopo from."}
              },
              "additionalProperties": false
            }
            """;

    private static final String SCHEDULE = """
            {
              "type": "object",
              "properties": {
                "startDate": {"type": "string", "format": "date",
                              "description": "Primo giorno del piano, AAAA-MM-GG, da oggi in poi. Se manca: oggi."},
                "days": {"type": "integer", "minimum": 1, "maximum": 14,
                         "description": "Quanti giorni pianificare. Se manca: 7."},
                "characterPriority": {"type": "array", "items": {"type": "string"},
                                      "description": "Id dei personaggi del roster in ordine di priorità, per esempio jinhsi. Se manca: l'ordine del roster."},
                "excludedDates": {"type": "array", "items": {"type": "string", "format": "date"}, "maxItems": 14,
                                  "description": "Giorni in cui l'utente non gioca, AAAA-MM-GG."},
                "includeEventActivities": {"type": "boolean",
                                           "description": "Se aggiungere le attività degli eventi attivi. Se manca: true."},
                "maxMinutesPerDay": {"type": "integer", "minimum": 15, "maximum": 240,
                                     "description": "Minuti di gioco al giorno. Se manca: il tempo di gioco dell'utente."}
              },
              "additionalProperties": false
            }
            """;

    private static final String PROPOSAL = """
            {
              "type": "object",
              "properties": {
                "summary": {"type": "string", "minLength": 1, "maxLength": 2000,
                            "description": "Spiegazione breve del piano per l'utente, in italiano: cosa proponi e perché."}
              },
              "required": ["summary"],
              "additionalProperties": false
            }
            """;

    private final List<ToolDefinition> definitions;

    public AgentToolDefinitions(JsonMapper jsonMapper) {
        this.definitions = List.of(
                new ToolDefinition(GET_PLAYER_STATUS,
                        "Legge il roster dell'utente (personaggi con livello, grado e obiettivo), le Waveplate e i "
                                + "Crystals con l'ora in cui saranno pieni, e il tempo di gioco al giorno.",
                        schema(jsonMapper, NO_PARAMETERS)),
                new ToolDefinition(GET_EVENTS,
                        "Elenca gli eventi del gioco tra due date, con i giorni in cui sono attivi, le attività, le "
                                + "ricompense e gli effetti, per esempio le ricompense doppie.",
                        schema(jsonMapper, RANGE)),
                new ToolDefinition(GET_CALENDAR,
                        "Legge il calendario dell'utente tra due date: le attività confermate o fatte, la proposta "
                                + "in attesa di conferma, se c'è, e se l'ultimo piano è superato.",
                        schema(jsonMapper, RANGE)),
                new ToolDefinition(PLAN_SCHEDULE,
                        "Calcola in Java un piano giorno per giorno (farming e attività degli eventi) con Waveplate e "
                                + "minuti, e restituisce la bozza con la copertura dei materiali e gli avvisi. Legge da "
                                + "solo roster, obiettivi, eventi, Waveplate e calendario dell'utente. Non salva "
                                + "niente: per proporlo all'utente chiama poi create_plan_proposal. Per cambiare il piano "
                                + "richiamalo con parametri diversi.",
                        schema(jsonMapper, SCHEDULE)),
                new ToolDefinition(CREATE_PLAN_PROPOSAL,
                        "Salva come proposta l'ultima bozza calcolata da plan_schedule in questo turno, con la tua "
                                + "spiegazione. L'utente la vede nel calendario e deve confermarla: finché non conferma "
                                + "il calendario non cambia.",
                        schema(jsonMapper, PROPOSAL)));
    }

    public List<ToolDefinition> all() {
        return definitions;
    }

    /** The names of the five tools, for the answer to an unknown tool. */
    static List<String> names() {
        return List.of(GET_PLAYER_STATUS, GET_EVENTS, GET_CALENDAR, PLAN_SCHEDULE, CREATE_PLAN_PROPOSAL);
    }

    // Jackson reads a JSON object into a LinkedHashMap, which keeps the order of the keys
    private static Map<String, Object> schema(JsonMapper jsonMapper, String json) {
        return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
