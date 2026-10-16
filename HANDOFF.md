# Handoff · giorno 7

Passaggio di consegne del 16/10/2026, a fine giorno 6 (v1 rilasciata), per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 7 (T29-T31): pianificatore, proposte e piano base in backend, barra della proposta nel calendario.
Prima di scrivere codice riassumimi il piano e le eventuali decisioni nuove, e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-6** (T01-T28), tutti su `main` e online; su Trello sono in "Fatto" |
| Rilascio | **v1** con tag `v1.0.0`; checklist online superata (`docs/rilascio-v1.md`, 9 punti su 9) |
| Ultimo commit su `main` | `c255984`, T28 (PR #34), più il commit di questa chiusura |
| Test | Backend 554 (`./mvnw verify`, copertura 98,5%), frontend 478 (`npm test`, copertura 97,9%) |
| Decisioni | D1-D122, tutte *Confermate* (D117-D122 nate nel giorno 6) |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Sito statico con CSP e `nosniff` (Headers, fuori dal repository). Dopo ogni merge controllare che il deploy parta (sito e API); se il deploy risponde "Service Unavailable", guardare prima https://status.render.com |
| Account di prova in locale | Nessuno fisso: si crea un account dal browser con una password casuale e lo si cancella alla fine (`DELETE /api/me`). Per provare l'admin senza usare le credenziali del `.env`: promuovere l'account di prova con SQL (`update users set role = 'ADMIN' ...`) e rifare il login, perché il ruolo è nel token |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 7 · Pianificatore e proposte (T29-T31)

Dettagli in `docs/09-task.md` e in `docs/06-agente.md` (sezione "Il pianificatore").

| Card | Cosa | Decisioni |
|---|---|---|
| T29 | Pianificatore giorno per giorno, test prima del codice | D57 |
| T30 | Proposte: piano base, conferma, scarto, sostituzione, indice parziale | D22, D37, D64 |
| T31 | Barra della proposta, attività tratteggiate, "Genera piano base", avviso di cambiamento | D23, D68 |

## Cosa esiste già nel codice

- **Backend:** `auth/`, `user/`, `waveplate/`, `roster/`, `inventory/`, `onboarding/`, `catalog/`, `farming/` (calcolo del farming, D112), `event/` (edizioni e API admin, D113-D114), `planning/` (attività, completamento e calendario, D117-D118), `common/` (`Today`, errori, limiti).
- **API pronte:** `/api/auth/**`, `/api/me` (profilo, `PATCH`, `DELETE`), `/api/me/waveplate`, `/api/me/roster`, `/api/me/inventory`, `/api/me/onboarding`, `/api/me/farming-plan`, `/api/me/tasks` (con `/complete`), `/api/me/calendar`, `/api/game/**` (le regole hanno anche `demoEventsAnchorDate`), `/api/events`, `/api/admin/events`.
- **Frontend:** tutte le pagine della v1. `features/calendar/` (P4), `waveplate/`, `profile/` (P6), `adminEvents/` (P7), oltre a quelle dei giorni precedenti. Componenti comuni: `Modal` (anche `size="lg"`), `ConfirmModal`, `FormField` (anche `type="date"`), `SelectField`, `EmptyState`, `LoadingState`, `ErrorState`. Hook `useLogout` per uscire.

## Come lavoriamo (flusso approvato, invariato)

1. Un ramo per card.
2. Test prima del codice.
3. Controlli locali: `./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`.
4. Prova reale, con strumenti testuali e niente screenshot:
   - backend con il jar;
   - frontend con `frontend-prod`, che manda la CSP.
5. Revisione del codice con agenti: revisore del linguaggio e revisore di sicurezza, in parallelo.
6. L'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove.
7. Push, PR senza firme di Claude, squash merge, ramo cancellato.
8. Controllo online.

Altre regole:
- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente.
- Online non si creano account di prova. Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.

## Promemoria per il giorno 7

Sono anche in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card".

- **T29 (pianificatore):**
  - riusare `NeedCalculator` e la resa per run di `FarmingCalculator` (D112);
  - per le ricompense doppie riusare `CompletionRewards.strongest` e `boostedRuns` (D118), così piano e completamento contano allo stesso modo;
  - i giorni attivi degli eventi vengono da `EventCalendar`, "oggi" da `Today` (D41).
- **T30 (proposte):**
  - un'attività dell'agente ha bisogno di una proposta (vincolo del database), e `PlannedTask` oggi ha solo il costruttore delle attività manuali: serve un costruttore per quelle proposte;
  - quando si sostituisce la proposta in attesa, fare `flush()` dopo averla segnata `REPLACED`: Hibernate esegue gli INSERT prima degli UPDATE e l'indice di D37 scatterebbe;
  - la conferma può bloccare la riga dell'utente con `UserRepository.lockById`, come le scritture delle attività (D118);
  - `GET /api/me/calendar` va esteso con la proposta in attesa (D51, D68). Il campo oggi non c'è, ed è scritto in D117.
- **T31 (frontend):**
  - la barra della proposta e le attività `PROPOSED` tratteggiate si aggiungono a `CalendarPage` e `calendarItems.ts`. I colori vanno in `styles/_calendar.scss`, con il contrasto AA;
  - l'avviso di cambiamento (D23) va progettato: serve sapere quando roster, obiettivi o eventi sono cambiati dopo l'ultimo piano;
  - "2 / 3 doppie oggi" della schermata P4 può arrivare qui, dal pianificatore (D120).
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101.
6. Bundle JavaScript di circa 630 KB per FullCalendar: se la presentazione diventa lenta, caricare la pagina del calendario solo quando serve (`lazy`, D119).
7. Limiti noti annotati nelle decisioni: catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114), la bozza dell'evento si perde chiudendo la finestra (D122).

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container può fermarsi da solo (per esempio dopo un riavvio di Docker): se i test non trovano il database, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`.
- **Windows:**
  - i file hanno fine riga CRLF: per le sostituzioni usare lo strumento di modifica o uno script Python che rispetta il fine riga;
  - uno script lungo con heredoc in Bash può non partire: scriverlo in un file nello scratchpad.
- **Browser di prova:**
  - guidare la pagina con `javascript_tool` (setter nativo più eventi `input`/`change`) e leggere il testo;
  - FullCalendar disegna gli elementi solo se la scheda è **in primo piano** (`tabs_select`): in una scheda nascosta `requestAnimationFrame` non parte e gli elementi restano invisibili;
  - i server si avviano con `preview_start` (`backend`, `frontend-prod`) e si fermano con `preview_stop`. Il pannello stretto mostra la vista telefono del calendario: per la vista computer usare `resize_window` e poi riportarlo a `desktop`.
- **Render:**
  - servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti;
  - con la scheda di Chrome nascosta i clic non aprono i menu: "Manual Deploy" si apre con `button.click()` via JavaScript.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
