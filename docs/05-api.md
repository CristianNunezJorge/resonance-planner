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
| `/api/auth/**` | Tutti |
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
| 409 | Conflitto con lo stato attuale (email già usata, personaggio già nel roster, attività non modificabile) | `CONFLICT` |
| 429 | Troppe richieste o limite giornaliero dell'agente raggiunto | `RATE_LIMITED`, `AGENT_DAILY_LIMIT` |
| 502 | L'API dell'IA non risponde o risponde con un errore | `AGENT_UNAVAILABLE` |
| 500 | Errore imprevisto: dettagli solo nei log del server, mai nella risposta | `INTERNAL_ERROR` |

Una risorsa di un altro utente risponde 404, non 403: così non si rivela nemmeno che esiste.

## Endpoint

### Autenticazione
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| POST | `/api/auth/register` | Registra un utente | 201, token e utente |
| POST | `/api/auth/login` | Login | 200, token e utente |
| POST | `/api/auth/guest` | Entra come ospite (v3, D15) | 201, token e utente |

### Profilo
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me` | Dati del profilo | 200 |
| PATCH | `/api/me` | Modifica username o tempo di gioco | 200 |
| DELETE | `/api/me` | Elimina account e dati (D42) | 204 |
| POST | `/api/me/onboarding` | Salva tutta la configurazione guidata in una volta (D50) | 200; 409 se già completata |

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
| GET | `/api/me/waveplate` | Valori attuali calcolati e orari in cui saranno pieni |
| PUT | `/api/me/waveplate` | Salva i valori letti nel gioco |

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
| Login e registrazione | 10 tentativi ogni 15 minuti per indirizzo IP |
| Messaggi all'agente | 20 al giorno per utente |
| Tutte le altre API | 120 richieste al minuto per indirizzo IP |

Superato un limite, la risposta è 429 con l'orario in cui si può riprovare.

## Fuori scope

| Cosa | Perché |
|---|---|
| Versione nell'URL (`/api/v1`) | C'è un solo client, il nostro frontend (D44) |
| Refresh token | Il token dura 24 ore; dopo si rifà il login (D45) |
| Annullare il completamento di un'attività | L'inventario si corregge a mano |
| Risposta dell'agente in streaming | Si valuta nella fase 6 come miglioramento (D52) |
