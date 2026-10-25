package com.resonanceplanner.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger gets the "Authorize" button: paste the token of the login and the protected endpoints can be tried (D53).
 * Only the controllers that ask for {@link #BEARER_AUTH} show the lock.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT", description = "Il token che restituiscono la registrazione e il login, senza la parola Bearer. "
                + "Vale come una password per 24 ore e permette anche di eliminare l'account: usalo solo qui e prova con un account di prova.")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    /** The page at the top of Swagger (D136): how to try the API, and what every endpoint has in common. */
    private static final String DESCRIPTION = """
            API REST di **Resonance Planner**, il planner per Wuthering Waves: calendario, Waveplate, roster e un \
            agente IA che propone un piano da confermare. Applicazione: https://resonance-planner.onrender.com, \
            codice e documentazione: https://github.com/CristianNunezJorge/resonance-planner

            ### Come provarla
            1. `POST /api/auth/register`, `/api/auth/login` o `/api/auth/guest` (account ospite di 24 ore): la \
            risposta contiene `token`.
            2. Premi **Authorize** e incolla il token, senza la parola Bearer.
            3. Gli endpoint con il lucchetto usano l'utente del token: i dati personali stanno sotto `/api/me` e non \
            si indica mai un id utente.

            Gli esempi già compilati in "Try it out" sono richieste valide. Usa un account di prova: con il token si \
            può anche eliminare l'account.

            ### Formato delle risposte
            Ogni risposta JSON ha la stessa forma:
            ```json
            { "success": true, "data": { }, "error": null }
            ```
            In caso di errore:
            ```json
            {
              "success": false,
              "data": null,
              "error": {
                "code": "VALIDATION_ERROR",
                "message": "Alcuni campi non sono validi.",
                "fields": { "title": "Scrivi un titolo." }
              }
            }
            ```
            `fields` c'è solo negli errori di validazione. Le pagine di un elenco (lo storico della chat) hanno \
            anche `meta`.

            ### Codici di errore
            | Stato | `code` | Quando |
            |---|---|---|
            | 400 | `VALIDATION_ERROR` | Dati non validi, con `fields` |
            | 401 | `UNAUTHORIZED`, `BAD_CREDENTIALS` | Token mancante, scaduto o di un account eliminato; email o \
            password sbagliate |
            | 403 | `FORBIDDEN` | Area admin senza il ruolo ADMIN |
            | 404 | `NOT_FOUND` | Risorsa che non esiste o non è tua |
            | 409 | `CONFLICT` | Operazione non possibile nello stato attuale |
            | 429 | `RATE_LIMITED`, `AGENT_DAILY_LIMIT` | Troppe richieste o limite giornaliero dell'agente, con \
            l'header `Retry-After` (secondi da aspettare) |
            | 502 | `AGENT_UNAVAILABLE` | L'assistente non risponde |
            | 500 | `INTERNAL_ERROR` | Errore imprevisto |

            I codici specifici di ogni endpoint sono nella sua descrizione.

            ### Limiti di richieste
            Con i valori predefiniti, per indirizzo IP: 10 tentativi di login ogni 15 minuti (e altrettanti di \
            registrazione), 5 ospiti ogni 15 minuti, 120 richieste al minuto per tutto il resto. L'agente \
            risponde a 5 messaggi al giorno per utente, 2 per un ospite.

            ### Date e orari
            Le date sono `AAAA-MM-GG` e "oggi" è sempre in ora italiana. Gli istanti sono in UTC, in ISO 8601 \
            (per esempio `2026-10-22T14:05:00Z`).

            ### Notifiche in tempo reale
            Il WebSocket `/ws` non è un'API REST e Swagger non lo mostra. È un WebSocket semplice, senza STOMP, \
            che si apre solo dalle origini del CORS; i messaggi sono JSON con un campo `type`.
            - Il browser manda come primo messaggio `{ "type": "AUTH", "token": "eyJ..." }`, perché il token non \
            va mai nell'indirizzo, poi `{ "type": "PING" }` ogni 25 secondi.
            - Il server manda `{ "type": ..., "message": ... }` con i tipi `WAVEPLATE_ALMOST_FULL`, `WAVEPLATE_FULL`, \
            `EVENT_ENDING` ed `EVENTS_CHANGED`; `message` è testo semplice in italiano.
            - Chiusure: `4401` token rifiutato (il browser non riprova), `4408` nessun messaggio da più di 90 \
            secondi, `1008` primo messaggio diverso da `AUTH` o illeggibile, `1009` messaggio oltre 8 KB.
            """;

    @Bean
    OpenAPI resonancePlannerApi() {
        // 1.0 is the version of the contract of the API, not of the application
        return new OpenAPI().info(new Info().title("Resonance Planner API").version("1.0").description(DESCRIPTION));
    }
}
