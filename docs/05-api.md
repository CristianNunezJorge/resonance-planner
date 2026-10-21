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
| POST | `/api/me/onboarding` | Salva tutta la configurazione guidata in una volta (D50) | 200 con l'utente aggiornato; 409 se già completata |

- **Risposta di `GET` e `PATCH`:** lo stesso `user` della registrazione (`id`, `username`, `email`, `role`, `playTimeMinutes`, `onboardingCompleted`).
- **`PATCH`:** `{ "username": "rover.new", "playTimeMinutes": 60 }`, tutti e due facoltativi (D102). Il tempo di gioco è 30, 60 o 120 (opzioni di `GET /api/game/rules`) e lo username segue le regole della registrazione. 400 con il campo sbagliato (`fields.username` o `fields.playTimeMinutes`), 409 se lo username è già usato. Se un valore non è valido non si salva niente. Email, ruolo e gli altri campi si ignorano.
- **`DELETE`:** 204 senza contenuto; roster, inventario, attività, proposte e messaggi spariscono con l'account.
- **Token di un account eliminato:** tutti gli endpoint sotto `/api/me` rispondono 401 `UNAUTHORIZED`, perché il token vale fino alla scadenza ma il suo utente non esiste più.

### Catalogo di gioco
| Metodo | Percorso | Cosa fa |
|---|---|---|
| GET | `/api/game/rules` | Regole di gioco: Waveplate, livelli di ascensione, opzioni di tempo e `demoEventsAnchorDate`, il giorno da cui si conta l'inizio degli eventi che si ripetono (D29, D122) |
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

**Eventi pubblici (T23, D113).** `GET /api/events?from=2026-10-01&to=2026-10-31` restituisce una voce per ogni edizione con giorni attivi nell'intervallo, in ordine di inizio e poi di nome; lo stesso evento può comparire più volte (Bountiful Crescendo dal 4 al 10 e dal 18 al 24 ottobre). Ogni voce ha `id`, `name`, `type`, `description`, `startDate` ed `endDate` (le date intere dell'edizione, fine compresa), `activeWeekdays` (`null` = tutti i giorni), `activeDates` (i giorni attivi dentro l'intervallo), `rewards`, `activities` (`title`, `minutes`, `frequency`, `unlockDay`) ed `effect` (`null` se non c'è). `GET /api/events/{id}` restituisce la stessa voce per l'edizione in corso oggi (ora italiana, D41) oppure per la prossima, con tutti i suoi giorni attivi; un evento a date fisse già finito mantiene le sue date.
- **400** con `fields.from` o `fields.to` se una data è fuori dalla finestra dal 01/01/2000 al 31/12/2100; con `fields.to` se `to` viene prima di `from` o se l'intervallo supera 62 giorni; **400** senza `fields` se manca una data o non è nel formato `AAAA-MM-GG`.
- **404** se l'evento non esiste.

**Eventi dell'admin (T23, D114).** `GET /api/admin/events` restituisce gli eventi con la programmazione com'è salvata: `id`, `code` (l'identificativo degli eventi della demo, `null` per quelli creati dall'admin), `name`, `type`, `description`, `schedule` (`type`, `startDate`, `endDate`, `startOffsetDays`, `durationDays`, `repeatEveryDays`), `activeWeekdays`, `rewards`, `activities`, `effect`. `POST` (201) e `PUT /{id}` (200) prendono lo stesso corpo e rispondono con l'evento salvato:
```json
{
  "name": "Torneo del weekend", "type": "COMBAT_STAGES", "description": "Solo sabato e domenica.",
  "schedule": { "type": "FIXED", "startDate": "2026-10-12", "endDate": "2026-10-18" },
  "activeWeekdays": ["SATURDAY", "SUNDAY"],
  "rewards": ["Astrite"],
  "activities": [{ "title": "Livello 1", "minutes": 20, "frequency": "ONCE", "unlockDay": 6 }],
  "effect": null
}
```
- Un evento `RECURRING` usa `startOffsetDays` (contato dalla data di riferimento `demoEventsAnchorDate` di `game-rules.json`, 01/10/2026), `durationDays` e `repeatEveryDays` al posto delle date; i valori dell'altro tipo vengono ignorati. Le date di un evento `FIXED` stanno tra il 01/01/2000 e il 31/12/2100.
- Nessun giorno della settimana, o tutti e sette, vuol dire tutti i giorni. Un evento `FIXED` deve avere almeno un giorno scelto tra le sue date; un evento `RECURRING` di meno di 7 giorni non può sceglierli (400 su `activeWeekdays`).
- `effect` (`type`, `sourceType`, `multiplier`, `dailyLimit`) è obbligatorio per `DOUBLE_DROP` e vietato per gli altri tipi.
- **400** con `fields`, tutti i campi sbagliati insieme, con nomi come `name`, `schedule.endDate`, `rewards[0]`, `activities[0].unlockDay`, `effect.multiplier`; non si salva niente. I limiti sono in D114.
- `PUT` sostituisce tutto; un evento della demo conserva il suo `code`. **404** se l'evento non esiste (prima del 400 delle regole).
- `DELETE` risponde 204; le attività del calendario collegate restano, senza il collegamento. Un evento della demo cancellato ricompare al riavvio (D39). **404** se non esiste.
- **401** se l'utente del token non esiste più, **403** se non è ADMIN, anche se il token dice ADMIN (D102). Un corpo di forma sbagliata riceve il 400 prima di questo controllo.

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

**Inventario (T19).** `GET /api/me/inventory` restituisce `[{ "materialId", "quantity" }]` per i soli materiali che l'utente ha già salvato, in ordine di identificativo: l'inventario parte vuoto e un materiale che non c'è nell'elenco vale 0 (D21, D106). `PUT /api/me/inventory/{materialId}` prende `{ "quantity": 120 }` e risponde con `{ "materialId", "quantity" }`. La quantità sostituisce quella di prima (non si somma) e la prima volta crea la voce.
- **400** con `fields.quantity` se manca, è negativa, supera 999.999.999 o non è un numero intero.
- **404** se il materiale non è nel catalogo.
- **401** se l'utente del token non esiste più (D102).
- Le richieste mandate insieme per lo stesso materiale non danno errore: vince l'ultima (D108).

### Calendario e attività
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/calendar?from=&to=` | Eventi, attività e proposta in attesa dell'intervallo, in una sola risposta (D51) | 200 |
| GET | `/api/me/calendar/export?from=&to=` | Il piano dell'intervallo in un file Excel (D133) | 200 con il file `.xlsx` |
| POST | `/api/me/tasks` | Crea un'attività manuale (D24) | 201 |
| PUT | `/api/me/tasks/{id}` | Modifica un'attività manuale | 200; 409 se non è manuale |
| DELETE | `/api/me/tasks/{id}` | Cancella un'attività manuale | 204; 409 se non è manuale |
| POST | `/api/me/tasks/{id}/complete` | Segna come fatta; aggiorna l'inventario se è di farming (D21) | 200 |

**Attività manuali (T25, D117).** `POST` (201) e `PUT /{id}` (200) prendono `{ "title": "Giro dei boss", "taskDate": "2026-10-20", "minutes": 30, "notes": "Prima Mourning Aix" }` e rispondono con l'attività. Titolo da 1 a 120 caratteri (gli spazi ai lati si tolgono), giorno tra il 01/01/2000 e il 31/12/2100, durata da 1 a 1440 minuti, note facoltative fino a 1000 caratteri (vuote = `null`). Origine, tipo e stato non si mandano: un'attività manuale è `MANUAL`, `CUSTOM` e nasce `CONFIRMED`. `PUT` sostituisce tutti i campi e lascia lo stato com'è; `DELETE` risponde 204.
- **400** con `fields`, tutti i campi sbagliati insieme (`title`, `taskDate`, `minutes`, `notes`); senza `fields` se il corpo non ha la forma giusta (una data scritta male, un numero con la virgola).
- **404** se l'attività non è dell'utente del token (anche se esiste per un altro), prima del 400 delle date e del 409.
- **409** su `PUT` e `DELETE` se l'attività è dell'agente; su `POST` se l'utente ha già 500 attività manuali.
- **401** se l'utente del token non esiste più (D102), per tutti gli endpoint.

Ogni attività nelle risposte ha `id`, `origin`, `kind`, `status`, `taskDate`, `title`, `notes`, `minutes`, `farmingSourceId` e `runs` (solo `FARMING`), `waveplates` (run × costo della fonte, 0 per gli altri tipi), `eventId` (solo `EVENT`), `characterId` e `completedAt` (`null` finché non è fatta).

**Completamento (T25, D118).** `POST /api/me/tasks/{id}/complete` vale per un'attività `CONFIRMED` e risponde con `{ "task": { ... }, "addedToInventory": [{ "materialId", "quantity" }] }`. Un'attività di farming aggiunge all'inventario, per ogni drop della fonte, la quantità media delle sue run arrotondata per difetto; se nel giorno dell'attività un evento attivo potenzia quel tipo di fonte, il moltiplicatore vale per le prime run del giorno fino al limite giornaliero, contando prima le run già fatte quel giorno. Le altre attività diventano solo `DONE` (`addedToInventory` vuoto). La somma nell'inventario si ferma a 999.999.999: in quel caso `addedToInventory` dice quanto hanno dato le run, non quanto è entrato.
- **409** se l'attività è già fatta o non è nel calendario (proposta, scartata o sostituita).
- **404** e **401** come sopra.

**Calendario (T25, D117).** L'intervallo può coprire al massimo 62 giorni, con gli stessi errori 400 di `/api/events`. `events` sono le voci di `/api/events` (una per edizione, con `activeDates`); `tasks` sono le attività `CONFIRMED` e `DONE` dell'utente nell'intervallo, per giorno e poi in ordine di creazione. `pendingProposal` è la proposta in attesa con tutte le sue attività `PROPOSED`, anche fuori dall'intervallo, oppure `null` (T30, D124). `planOutdated` è `true` quando roster, obiettivi o eventi dei giorni dell'ultimo piano sono cambiati dopo che è stato fatto (T31, D125).

**Export in Excel (X01, D133).** `GET /api/me/calendar/export` ha lo stesso intervallo, gli stessi dati e gli stessi errori del calendario, ma risponde con un file (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="resonance-planner_2026-10-19_2026-10-25.xlsx"`). Il foglio "Piano" ha una riga per attività `CONFIRMED` o `DONE` (Data, Giorno, Tipo, Attività, Personaggio, Fonte, Run, Waveplate, Minuti, Stato, Note) e una riga "Totale" con le formule `SUM` di Waveplate e Minuti; il foglio "Eventi" ha nome, tipo, inizio, fine e giorni attivi nel periodo. La proposta in attesa non c'è. I testi di utenti e admin sono sempre celle di testo, mai formule. Gli errori (400, 401) restano JSON con `ApiResponse`: l'header `Accept` deve ammettere anche `application/json` (il frontend manda `Accept: <tipo del file>, application/json`).

### Proposte dell'agente
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/proposals/pending` | La proposta in attesa con le sue attività | 200; `data` vuoto se non ce n'è |
| POST | `/api/me/proposals/{id}/confirm` | Conferma: le attività entrano nel calendario e sostituiscono il piano precedente (D22) | 200 |
| POST | `/api/me/proposals/{id}/reject` | Scarta la proposta | 200 |
| POST | `/api/me/proposals/basic` | Crea una proposta con il pianificatore e i parametri predefiniti, senza IA (D64) | 201 |

Le proposte nascono solo dal pianificatore, attraverso l'agente o il piano base: non c'è un endpoint per scriverle a mano.

**Proposte (T30, D124).** Una proposta è `{ "id", "summary", "createdAt", "tasks": [...] }`, con le attività nella stessa forma di `/api/me/tasks` (stato `PROPOSED`). `GET /pending` restituisce la proposta in attesa o `data` vuoto. `POST /basic` (201) chiama il pianificatore con i parametri predefiniti e sostituisce la proposta in attesa; il testo è standard, seguito dagli avvisi del pianificatore. `POST /{id}/confirm` risponde con la proposta confermata (attività `CONFIRMED`) e sostituisce le attività dell'agente confermate da oggi in poi (D22); `POST /{id}/reject` risponde con la proposta scartata e senza attività, perché quelle proposte si cancellano.
- **409** se il pianificatore non trova niente da fare (con il suo avviso), se la proposta non è più in attesa o se è di un giorno precedente.
- **404** se la proposta non è dell'utente del token.
- **401** se l'utente del token non esiste più (D102).

### Agente
| Metodo | Percorso | Cosa fa | Risposta |
|---|---|---|---|
| GET | `/api/me/agent/messages?before=&limit=` | Storico della chat, dal più recente, a pagine | 200 |
| POST | `/api/me/agent/messages` | Invia un messaggio e aspetta la risposta (D52) | 200; 429 ai limiti di D129 |
| GET | `/api/me/agent/usage` | Messaggi usati oggi, limite, orario di azzeramento e stato dei limiti sulle chiamate | 200 |

**Chat (T35, D129).** Un messaggio è `{ "id", "role", "content", "proposalId", "createdAt" }`, con `role` `USER` o `ASSISTANT` e `proposalId` vuoto se il messaggio non ha prodotto una proposta.
- **Storico:** `GET /messages` restituisce i messaggi dal più recente, `limit` da 1 a 50 (20 se manca). Per la pagina successiva si passa come `before` l'id del messaggio più vecchio ricevuto. La risposta ha `meta: { "limit": 20, "hasMore": true }`. La pagina parte da un id e non da un numero, così un messaggio nuovo non fa scorrere le pagine.
- **Invio:** `POST /messages` con `content` da 1 a 1.000 caratteri (400 sul campo `content`). Il messaggio dell'utente e la risposta si salvano solo se l'agente risponde (D61); la risposta salvata ha al massimo 4.000 caratteri, perché torna nello storico di ogni richiesta.
- **Utilizzo:** `GET /usage` restituisce `{ "used", "limit", "resetsAt", "callsLimitReached", "appLimitReached" }`. `callsLimitReached` è vero quando all'utente non resta un turno intero di chiamate al modello (contano anche i turni falliti), `appLimitReached` quando non resta a tutta l'app; entrambi ripartono a mezzanotte UTC (D129).
- **429** ai limiti giornalieri dell'utente (messaggi o chiamate) o di tutta l'app (`AGENT_DAILY_LIMIT`), con un turno già in corso o con troppi turni insieme (`RATE_LIMITED`); sempre con `Retry-After`.
- **502** se l'agente non risponde (`AGENT_UNAVAILABLE`, D61, D126).
- **401** se l'utente del token non esiste più (D102).

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
Il backend salva tutto in un'unica transazione: o va a buon fine tutto, o niente. Risponde con l'utente aggiornato (lo stesso `user` della registrazione, con `onboardingCompleted: true` e `playTimeMinutes`).
- Almeno un personaggio e al massimo 50 (nessuno ripetuto); ogni personaggio ha la forma e le regole di `POST /api/me/roster` (D104); `waveplate.crystals` è facoltativo e vale 0; `playTimeMinutes` è una delle opzioni di `GET /api/game/rules`.
- **400** con `fields`, tutti i campi sbagliati insieme, in ordine di passo, con nomi come `characters[1].currentLevel`, `characters[1].characterId`, `waveplate.value`, `waveplate.crystals`, `playTimeMinutes`. Non si salva niente. Un corpo di forma sbagliata (valore mancante, numero con la virgola) è 400 senza il dettaglio dei campi che il servizio avrebbe controllato.
- **409** se la configurazione è già completata, e anche se lo stesso utente cambia il roster da un'altra scheda nel momento del salvataggio (si riprova). Il 400 viene prima del 409.
- **401** se l'utente del token non esiste più (D102).
- Il roster già presente, le Waveplate e il tempo di gioco salvati prima vengono sostituiti; l'inventario resta (D109).
- Due invii nello stesso momento: ne passa uno, l'altro riceve 409.
- Un corpo JSON più lungo di 100.000 caratteri o con più di 5.000 elementi è rifiutato con 400 prima di essere letto (D110).

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
`GET /api/me/farming-plan` → `data` (Jinhsi da livello 1 grado 0 a livello 40 grado 2, inventario vuoto, 60 minuti al giorno; elenchi accorciati):
```json
{
  "materials": [
    { "materialId": "elegy-tacet-core", "name": "Elegy Tacet Core", "category": "BOSS_DROP", "required": 3, "owned": 0, "missing": 3 },
    { "materialId": "character-exp", "name": "EXP personaggio", "category": "CHARACTER_EXP", "required": 208800, "owned": 0, "missing": 208800 },
    { "materialId": "shell-credit", "name": "Shell Credit", "category": "CREDIT", "required": 88080, "owned": 0, "missing": 88080 }
  ],
  "sources": [
    { "farmingSourceId": "boss-mourning-aix", "name": "Boss Challenge: Mourning Aix", "runs": 2, "waveplates": 120, "minutes": 8 },
    { "farmingSourceId": "sim-resonance-potion", "name": "Simulation Training: Resonance Potion", "runs": 3, "waveplates": 120, "minutes": 9 }
  ],
  "totals": { "waveplates": 280, "minutes": 50, "estimatedDays": 2 },
  "containsEstimates": true
}
```
Regole del calcolo in D112 (T22):
- **Materiali:** solo quelli che servono, nell'ordine del calcolo (boss, nemici comuni, specialità, EXP, Shell Credit). L'EXP è una sola riga, `character-exp`, in punti: le Resonance Potion dell'inventario contano per la loro EXP e non compaiono come righe a parte. `missing` non è mai negativo.
- **Fonti:** nell'ordine in cui il calcolo le sceglie; ogni run toglie dal fabbisogno anche i drop secondari. Le fonti senza costo in Waveplate (nemici comuni, specialità) hanno `waveplates: 0`.
- **Totali:** `estimatedDays` è il più grande tra Waveplate ÷ 240 e minuti ÷ tempo di gioco, per eccesso; è `null` se l'utente non ha ancora scelto il tempo di gioco. Con il roster vuoto tutto vale 0.
- `containsEstimates` è `true` se una fonte usata ha un drop stimato (D25, D97).
- L'inventario si conta una volta sola per tutto il roster.

`GET /api/me/roster/{characterId}/requirements` → `data`: lo stesso calcolo per un solo personaggio, come se tutto l'inventario fosse per lui, con in più `characterId` e `completionPercent` (da 0 a 100: per ogni materiale la parte già posseduta, poi la media, sempre per difetto; 100 se non manca niente).
```json
{ "characterId": "jinhsi", "completionPercent": 33, "materials": ["..."], "sources": ["..."], "totals": { "waveplates": 240, "minutes": 48, "estimatedDays": 1 }, "containsEstimates": true }
```
Esempio: Jinhsi come sopra, con 3 Elegy Tacet Core e 10 LF Howler Core già nell'inventario. Senza le run del boss servono 4 run di Resonance Potion invece di 3.
- **404** se il personaggio non è nel roster dell'utente del token (anche se è nel roster di un altro o non esiste).
- **401** se l'utente del token non esiste più (D102), per entrambi gli endpoint.

### Calendario
`GET /api/me/calendar?from=2026-09-28&to=2026-10-04` → `data` (eventi accorciati: ogni voce ha anche `description`, `rewards` e `activities`, come in `/api/events`):
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
      "taskDate": "2026-10-01", "title": "Simulation Training: Resonance Potion ×3", "notes": null,
      "minutes": 9, "farmingSourceId": "sim-resonance-potion", "runs": 3, "waveplates": 120,
      "eventId": null, "characterId": "jinhsi", "completedAt": null
    }
  ]
}
```
Il secondo evento è attivo solo nel weekend: anche se l'edizione va dal 28 settembre all'11 ottobre, `activeDates` contiene solo sabato e domenica, e il calendario lo disegna solo in quei giorni (D96).

### Completamento di un'attività di farming
`POST /api/me/tasks/40/complete` (l'attività qui sopra, il 01/10 con Bountiful Crescendo attivo e nessun'altra run di Simulation Training fatta quel giorno) → `data`:
```json
{
  "task": { "id": 40, "status": "DONE", "completedAt": "2026-10-01T18:20:00Z", "...": "..." },
  "addedToInventory": [
    { "materialId": "premium-resonance-potion", "quantity": 6 },
    { "materialId": "advanced-resonance-potion", "quantity": 30 },
    { "materialId": "medium-resonance-potion", "quantity": 14 },
    { "materialId": "shell-credit", "quantity": 21600 }
  ]
}
```
Le 3 run sono tutte entro il limite di 3, quindi valgono doppio: 3 × 2 × 1,10 = 6,6 Premium Resonance Potion, arrotondate per difetto a 6 (D118).

### Messaggio all'agente
`POST /api/me/agent/messages`
```json
{ "content": "Organizzami la settimana per portare Jinhsi al 40." }
```
→ `data`:
```json
{
  "message": { "id": 88, "role": "ASSISTANT", "content": "Ho preparato un piano di 5 giorni...", "proposalId": 7, "createdAt": "2026-10-01T14:02:10Z" },
  "proposal": { "id": 7, "summary": "Concentro la Simulation Training nei giorni dell'evento...", "tasks": [] },
  "steps": ["Ho letto il tuo roster e le Waveplate", "Ho calcolato il piano", "Ho preparato la proposta"],
  "usage": { "used": 3, "limit": 5, "resetsAt": "2026-10-01T22:00:00Z", "callsLimitReached": false, "appLimitReached": false }
}
```
`proposal` è vuoto se l'agente ha solo risposto senza preparare un piano. `steps` elenca i passaggi fatti, ricavati dagli strumenti chiamati (D62). `resetsAt` è la mezzanotte italiana (D41) espressa in UTC.

## Limiti di richieste (D48)

| Cosa | Limite |
|---|---|
| Login e registrazione | 10 tentativi ogni 15 minuti per indirizzo IP, con un contatore per il login e uno per la registrazione (D101) |
| Messaggi all'agente | 5 al giorno per utente; chiamate al modello: 24 al giorno per utente (anche dei turni falliti) e 40 in tutta l'app; un turno alla volta per utente e due in tutta l'app (D129) |
| Piano base | 20 proposte al giorno per utente (D129) |
| Tutte le altre API | 120 richieste al minuto per indirizzo IP |

Superato un limite, la risposta è 429 `RATE_LIMITED` con l'header `Retry-After` (secondi da aspettare) e un messaggio che dice quanto aspettare ("Troppe richieste: riprova tra 12 minuti."). Il controllo di salute ha un contatore suo (600 al minuto) e le richieste di verifica del CORS non contano. Il frontend può leggere `Retry-After` (è tra gli header esposti dal CORS). I numeri si possono cambiare con le variabili d'ambiente `APP_RATELIMIT_*` (D101).

## Fuori scope

| Cosa | Perché |
|---|---|
| Versione nell'URL (`/api/v1`) | C'è un solo client, il nostro frontend (D44) |
| Refresh token | Il token dura 24 ore; dopo si rifà il login (D45) |
| Annullare il completamento di un'attività | L'inventario si corregge a mano |
| Risposta dell'agente in streaming | Si valuta nella fase 6 come miglioramento (D52) |
