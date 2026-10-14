# 5. API REST

**Stato:** completata · 01/10/2026

## Principi

- Tutte le API stanno sotto `/api` e parlano JSON (D44).
- I dati personali stanno sotto `/api/me`: l'utente si ricava sempre dal token, mai da un identificativo nell'URL (D44).
- Tutte le risposte hanno lo stesso formato, sia in caso di successo sia di errore (D46).
- Il backend riceve e restituisce DTO (Java `record`), mai le entità JPA (D47).
- Date dei giorni nel formato `2026-10-01`; momenti in ISO 8601 UTC, per esempio `2026-10-01T14:00:00Z`.
- La documentazione interattiva è su Swagger, all'indirizzo `/swagger-ui.html` (D53).

## Autenticazione e ruoli

Dopo login o registrazione il backend restituisce un token JWT. Il frontend lo salva e lo manda in ogni richiesta (D45):

```
Authorization: Bearer eyJhbGciOi...
```

| Percorsi | Chi può usarli |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` (e `guest` nella v3) | Tutti. Gli altri percorsi sotto `/api/auth` richiedono il login |
| `GET /api/game/**`, `GET /api/events/**` | Tutti (D49) |
| `/api/me/**` | Utenti con login (USER, ADMIN, GUEST) |
| `/api/admin/**` | Solo ADMIN |
| `/swagger-ui.html`, `/v3/api-docs`, `/actuator/health` | Tutti |

## Formato delle risposte (D46)

Successo:
```json
{
  "success": true,
  "data": { "id": 12, "username": "rover" },
  "error": null
}
```

Errore:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Alcuni campi non sono validi.",
    "fields": { "password": "Deve avere almeno 8 caratteri." }
  }
}
```

`code` serve al frontend per decidere cosa fare; `message` è già in italiano e si può mostrare all'utente; `fields` c'è solo per gli errori di validazione. Gli elenchi paginati aggiungono un campo `meta` (vedi i messaggi dell'agente).

## Codici di stato

| Codice | Quando | `error.code` |
|---|---|---|
| 200 | Operazione riuscita | — |
| 201 | Risorsa creata | — |
| 204 | Cancellazione riuscita, nessun contenuto | — |
| 400 | Dati non validi | `VALIDATION_ERROR` |
| 401 | Token mancante, scaduto o non valido; credenziali sbagliate | `UNAUTHORIZED`, `BAD_CREDENTIALS` |
| 403 | Ruolo non sufficiente | `FORBIDDEN` |
| 404 | Risorsa inesistente, o di un altro utente | `NOT_FOUND` |
| 409 | Conflitto con lo stato attuale (email già usata, personaggio già nel roster, attività non modificabile). Se riguarda un campo (email o username già usati) c'è anche `fields`, come per la validazione | `CONFLICT` |
| 429 | Troppe richieste o limite giornaliero dell'agente raggiunto | `RATE_LIMITED`, `AGENT_DAILY_LIMIT` |
| 502 | L'API dell'IA non risponde o risponde con un errore | `AGENT_UNAVAILABLE` |
| 500 | Errore imprevisto: dettagli solo nei log del server, mai nella risposta | `INTERNAL_ERROR` |

Una risorsa di un altro utente risponde 404, non 403: così non si rivela nemmeno che esiste.

Gli altri errori della richiesta che non hanno una riga propria (metodo HTTP non ammesso, formato non supportato) mantengono il loro codice di stato, per esempio 405, e usano `VALIDATION_ERROR` con il messaggio "Richiesta non valida.".

## Endpoint

### Autenticazione
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| POST | `/api/auth/register` | Registra un utente | 201, token e utente |
| POST | `/api/auth/login` | Login | 200, token e utente |
| POST | `/api/auth/guest` | Entra come ospite (v3, D15) | 201, token e utente |

- **Registrazione:** `{ "username", "email", "password" }`. Username da 3 a 30 caratteri tra lettere, numeri, punto, trattino e underscore; password da 8 caratteri a 72 byte (D100). `admin` e l'email dell'admin non si possono registrare. L'email si salva in minuscolo. 409 `CONFLICT` se email o username sono già usati.
- **Login:** `{ "email", "password" }`. Email o password sbagliate danno sempre lo stesso 401 `BAD_CREDENTIALS`.
- **Risposta di entrambi:** `{ "token": "eyJ...", "user": { "id", "username", "email", "role", "playTimeMinutes", "onboardingCompleted" } }`. Il token vale 24 ore e contiene identificativo e ruolo (D45, D99).

### Profilo
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me` | Dati del profilo | 200 |
| PATCH | `/api/me` | Modifica username o tempo di gioco | 200 |
| DELETE | `/api/me` | Elimina account e dati (D42) | 204 |
| POST | `/api/me/onboarding` | Salva tutta la configurazione guidata in una volta (D50) | 200; 409 se già completata |

- **Risposta di `GET` e `PATCH`:** lo stesso `user` della registrazione (`id`, `username`, `email`, `role`, `playTimeMinutes`, `onboardingCompleted`).
- **`PATCH`:** `{ "username": "rover.new", "playTimeMinutes": 60 }`, tutti e due facoltativi (D102). Il tempo di gioco è 30, 60 o 120 (opzioni di `GET /api/game/rules`) e lo username segue le regole della registrazione. 400 con il campo sbagliato (`fields.username` o `fields.playTimeMinutes`), 409 se lo username è già usato. Se un valore non è valido non si salva niente. Email, ruolo e gli altri campi si ignorano.
- **`DELETE`:** 204 senza contenuto; roster, inventario, attività, proposte e messaggi spariscono con l'account.
- **Token di un account eliminato:** tutti gli endpoint sotto `/api/me` rispondono 401 `UNAUTHORIZED`, perché il token vale fino alla scadenza ma il suo utente non esiste più.

### Catalogo di gioco
| Metodo | Percorso | Cosa fa |
|---|---|---|
| GET | `/api/game/rules` | Regole di gioco: Waveplate, livelli di ascensione, opzioni di tempo |
| GET | `/api/game/characters` | Elenco dei personaggi |
| GET | `/api/game/characters/{id}` | Dettaglio con i costi di ascensione |
| GET | `/api/game/materials` | Elenco dei materiali |
| GET | `/api/game/farming-sources` | Fonti di farming con i drop |

### Eventi
| Metodo | Percorso | Chi | Cosa fa |
|---|---|---|---|
| GET | `/api/events?from=&to=` | Tutti | Eventi attivi nell'intervallo, con le date già calcolate (D29) e l'elenco dei giorni in cui sono davvero attivi, `activeDates` (D96) |
| GET | `/api/events/{id}` | Tutti | Dettaglio di un evento |
| GET | `/api/admin/events` | ADMIN | Tutti gli eventi con i dati di programmazione |
| POST | `/api/admin/events` | ADMIN | Crea un evento |
| PUT | `/api/admin/events/{id}` | ADMIN | Modifica un evento |
| DELETE | `/api/admin/events/{id}` | ADMIN | Cancella un evento |

### Waveplate
| Metodo | Percorso | Cosa fa |
|---|---|---|
| GET | `/api/me/waveplate` | Valori attuali calcolati e orari in cui saranno pieni; 404 se non hai ancora salvato nessuna lettura |
| PUT | `/api/me/waveplate` | Salva i valori letti nel gioco (`{ "value": 180, "crystals": 0 }`, `crystals` facoltativo, vale 0); 400 con `fields` se un valore non è possibile (D105) |

### Roster, inventario e calcolo del farming
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/roster` | Personaggi del roster con livelli e obiettivi | 200 |
| POST | `/api/me/roster` | Aggiunge un personaggio | 201; 409 se c'è già |
| PUT | `/api/me/roster/{characterId}` | Modifica livelli e obiettivo | 200 |
| DELETE | `/api/me/roster/{characterId}` | Toglie il personaggio | 204 |
| GET | `/api/me/roster/{characterId}/requirements` | Materiali mancanti per l'obiettivo di quel personaggio | 200 |
| GET | `/api/me/inventory` | Materiali posseduti | 200 |
| PUT | `/api/me/inventory/{materialId}` | Corregge la quantità di un materiale | 200 |
| GET | `/api/me/farming-plan` | Calcolo complessivo: materiali mancanti, run, Waveplate, minuti e giorni stimati | 200 |

Nel roster il personaggio si indica con il suo identificativo (`jinhsi`): ogni utente può averlo una volta sola.

**Roster (T18).** `POST /api/me/roster` prende `{ "characterId", "currentLevel", "currentRank", "targetLevel", "targetRank" }`; `PUT /api/me/roster/{characterId}` prende gli stessi campi tranne `characterId`, che è quello dell'URL (se c'è anche nel corpo viene ignorato). La risposta di `GET`, `POST` e `PUT` ha `characterId`, i quattro valori e nient'altro: nome, elemento e icona sono nel catalogo pubblico. La lista è nell'ordine in cui i personaggi sono stati aggiunti.
- **400** con `fields`, tutti i campi sbagliati insieme e sempre nello stesso ordine (`characterId`, `currentLevel`, `currentRank`, `targetLevel`, `targetRank`): personaggio che non esiste nel catalogo, livello e grado che non esistono nel gioco o obiettivo più basso della situazione attuale (D104). Il 400 viene prima del 409. Non si salva niente.
- **409** con `fields.characterId` se il personaggio è già nel roster.
- **404** (mai 403) se il personaggio non è nel roster dell'utente del token: vale anche per un personaggio che sta nel roster di un altro utente. Nei percorsi con `{characterId}` un identificativo che non esiste dà lo stesso 404.
- **401** se l'utente del token non esiste più (D102).

### Calendario e attività
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/calendar?from=&to=` | Eventi, attività e proposta in attesa dell'intervallo, in una sola risposta (D51) | 200 |
| POST | `/api/me/tasks` | Crea un'attività manuale (D24) | 201 |
| PUT | `/api/me/tasks/{id}` | Modifica un'attività manuale | 200; 409 se non è manuale |
| DELETE | `/api/me/tasks/{id}` | Cancella un'attività manuale | 204; 409 se non è manuale |
| POST | `/api/me/tasks/{id}/complete` | Segna come fatta; aggiorna l'inventario se è di farming (D21) | 200 |

L'intervallo del calendario può coprire al massimo 62 giorni.

### Proposte dell'agente
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/proposals/pending` | La proposta in attesa con le sue attività | 200; `data` vuoto se non ce n'è |
| POST | `/api/me/proposals/{id}/confirm` | Conferma: le attività entrano nel calendario e sostituiscono il piano precedente (D22) | 200 |
| POST | `/api/me/proposals/{id}/reject` | Scarta la proposta | 200 |
| POST | `/api/me/proposals/basic` | Crea una proposta con il pianificatore e i parametri predefiniti, senza IA (D64) | 201 |

Le proposte nascono solo dal pianificatore, attraverso l'agente o il piano base: non c'è un endpoint per scriverle a mano.

### Agente
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/agent/messages?before=&limit=` | Storico della chat, dal più recente, a pagine | 200 |
| POST | `/api/me/agent/messages` | Invia un messaggio e aspetta la risposta (D52) | 200; 429 al limite giornaliero |
| GET | `/api/me/agent/usage` | Richieste usate oggi, limite e orario di azzeramento | 200 |

## Esempi delle richieste principali

### Configurazione guidata
`POST /api/me/onboarding`
```json
{
  "characters": [
    { "characterId": "jinhsi", "currentLevel": 1, "currentRank": 0, "targetLevel": 40, "targetRank": 2 },
    { "characterId": "aalto", "currentLevel": 20, "currentRank": 1, "targetLevel": 40, "targetRank": 1 }
  ],
  "waveplate": { "value": 180, "crystals": 0 },
  "playTimeMinutes": 60
}
```
Il backend salva tutto in un'unica transazione: o va a buon fine tutto, o niente.

### Stato delle Waveplate
`GET /api/me/waveplate` → `data`:
```json
{
  "value": 196,
  "crystals": 0,
  "max": 240,
  "crystalsMax": 480,
  "fullAt": "2026-10-01T16:24:00Z",
  "crystalsFullAt": "2026-10-05T16:24:00Z",
  "recordedAt": "2026-10-01T12:00:00Z"
}
```
`fullAt` e `crystalsFullAt` sono i momenti in cui i due contatori sono pieni: se un contatore è già pieno sono nel passato (il momento in cui lo è diventato). I Crystals ripartono solo quando le Waveplate sono piene (D28, D105). Il momento della lettura (`recordedAt`) lo sceglie sempre il server.

### Calcolo del farming
`GET /api/me/farming-plan` → `data`:
```json
{
  "materials": [
    { "materialId": "lf-howler-core", "name": "LF Howler Core", "required": 8, "owned": 3, "missing": 5 }
  ],
  "sources": [
    { "farmingSourceId": "sim-resonance-potion", "runs": 12, "waveplates": 480, "minutes": 36 }
  ],
  "totals": { "waveplates": 600, "minutes": 95, "estimatedDays": 3 },
  "containsEstimates": true
}
```
Numeri di esempio. `containsEstimates` è `true` se il calcolo usa valori stimati (D25).

### Calendario
`GET /api/me/calendar?from=2026-09-28&to=2026-10-04` → `data`:
```json
{
  "events": [
    {
      "id": 1, "name": "Bountiful Crescendo", "type": "DOUBLE_DROP",
      "startDate": "2026-09-29", "endDate": "2026-10-05",
      "activeWeekdays": null,
      "activeDates": ["2026-09-29", "2026-09-30", "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04"],
      "effect": { "type": "DROP_MULTIPLIER", "sourceType": "SIMULATION_TRAINING", "multiplier": 2, "dailyLimit": 3 }
    },
    {
      "id": 4, "name": "Evento di prova creato dall'admin", "type": "POINTS_SHOP",
      "startDate": "2026-09-28", "endDate": "2026-10-11",
      "activeWeekdays": ["SATURDAY", "SUNDAY"],
      "activeDates": ["2026-10-03", "2026-10-04"],
      "effect": null
    }
  ],
  "tasks": [
    {
      "id": 40, "origin": "AGENT", "kind": "FARMING", "status": "CONFIRMED",
      "taskDate": "2026-10-01", "title": "Simulation Training: Resonance Potion ×3",
      "minutes": 9, "waveplates": 120, "characterId": "jinhsi"
    }
  ],
  "pendingProposal": null
}
```
Il secondo evento è attivo solo nel weekend: anche se l'edizione va dal 28 settembre all'11 ottobre, `activeDates` contiene solo sabato e domenica, e il calendario lo disegna solo in quei giorni (D96).

### Messaggio all'agente
`POST /api/me/agent/messages`
```json
{ "content": "Organizzami la settimana per portare Jinhsi al 40." }
```
→ `data`:
```json
{
  "message": { "id": 88, "role": "ASSISTANT", "content": "Ho preparato un piano di 5 giorni...", "createdAt": "2026-10-01T14:02:10Z" },
  "proposal": { "id": 7, "summary": "Concentro la Simulation Training nei giorni dell'evento...", "tasks": [] },
  "steps": ["Ho letto il tuo roster", "Ho controllato gli eventi", "Ho calcolato il piano"],
  "usage": { "used": 3, "limit": 20, "resetsAt": "2026-10-01T22:00:00Z" }
}
```
`proposal` è vuoto se l'agente ha solo risposto senza preparare un piano. `steps` elenca i passaggi fatti, ricavati dagli strumenti chiamati (D62). `resetsAt` è la mezzanotte italiana (D41) espressa in UTC.

## Limiti di richieste (D48)

| Cosa | Limite |
|---|---|
| Login e registrazione | 10 tentativi ogni 15 minuti per indirizzo IP, con un contatore per il login e uno per la registrazione (D101) |
| Messaggi all'agente | 20 al giorno per utente |
| Tutte le altre API | 120 richieste al minuto per indirizzo IP |

Superato un limite, la risposta è 429 `RATE_LIMITED` con l'header `Retry-After` (secondi da aspettare) e un messaggio che dice quanto aspettare ("Troppe richieste: riprova tra 12 minuti."). Il controllo di salute ha un contatore suo (600 al minuto) e le richieste di verifica del CORS non contano. Il frontend può leggere `Retry-After` (è tra gli header esposti dal CORS). I numeri si possono cambiare con le variabili d'ambiente `APP_RATELIMIT_*` (D101).

## Fuori scope

| Cosa | Perché |
|---|---|
| Versione nell'URL (`/api/v1`) | C'è un solo client, il nostro frontend (D44) |
| Refresh token | Il token dura 24 ore; dopo si rifà il login (D45) |
| Annullare il completamento di un'attività | L'inventario si corregge a mano |
| Risposta dell'agente in streaming | Si valuta nella fase 6 come miglioramento (D52) |
