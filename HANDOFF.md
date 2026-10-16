# Handoff · giorno 8

Passaggio di consegne del 16/10/2026, a fine giorno 7 (pianificatore e proposte), per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 8 (T32-T34): client dell'API Claude, i cinque strumenti e il ciclo dell'agente in backend.
Prima di scrivere codice riassumimi il piano e le eventuali decisioni nuove, e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-7** (T01-T31), tutti su `main` e online |
| Rilascio | **v1** con tag `v1.0.0`; la v2 (agente) è prevista a fine giorno 10 |
| Ultimo commit su `main` | `17ea229`, T31 (PR #38), più il commit di questa chiusura |
| Test | Backend 626 (`./mvnw verify`, copertura 98,2%), frontend 492 (`npm test`, copertura 97,9%) |
| Decisioni | D1-D125, tutte *Confermate* (D123-D125 nate nel giorno 7) |
| Schema | Migrazioni `V1__schema.sql` e `V2__plan_inputs.sql` (impronta del piano, D125), applicate anche online |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Deploy di T31 verificato con richieste pubbliche (endpoint delle proposte e `planOutdated` nello Swagger, testi nuovi nel bundle del sito). Se il deploy risponde "Service Unavailable", guardare prima https://status.render.com |
| Trello | T29-T31 da spostare in "Fatto" se non è già stato fatto a fine sessione (serve Claude in Chrome) |
| Account di prova in locale | Nessuno fisso: si crea un account con una password casuale (script o `javascript_tool`) e lo si cancella alla fine (`DELETE /api/me`). Online non si creano account di prova |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 8 · Agente: backend (T32-T34)

Dettagli in `docs/09-task.md` e in `docs/06-agente.md`.

| Card | Cosa | Decisioni |
|---|---|---|
| T32 | Client dell'API Claude dietro un'interfaccia, configurazione di chiave e modello | D54, D59 |
| T33 | I cinque strumenti, limitati all'utente del token, con validazione dei parametri | D55, D56 |
| T34 | Ciclo dell'agente: massimo 6 giri, errori, passaggi, cache dei prompt; test con finto client | D54, D61, D62, D63 |

## Cosa esiste già nel codice

- **Backend:** `auth/`, `user/`, `waveplate/`, `roster/`, `inventory/`, `onboarding/`, `catalog/`, `farming/` (calcolo del farming e `FarmingYields`, D112), `event/` (edizioni e API admin, D113-D114), `planner/` (pianificatore puro `Planner` e `NeedPool`, `PlannerService`, impronta `PlanInputs`, D123, D125), `planning/` (attività, completamento, calendario, proposte con `ProposalService`, D117, D118, D124), `common/` (`Today` con `dateOf`, errori, limiti).
- **API pronte:** `/api/auth/**`, `/api/me` (profilo, `PATCH`, `DELETE`), `/api/me/waveplate`, `/api/me/roster`, `/api/me/inventory`, `/api/me/onboarding`, `/api/me/farming-plan`, `/api/me/tasks` (con `/complete`), `/api/me/calendar` (con `pendingProposal` e `planOutdated`), `/api/me/proposals` (`pending`, `basic`, `{id}/confirm`, `{id}/reject`), `/api/game/**`, `/api/events`, `/api/admin/events`.
- **Frontend:** tutte le pagine della v1; nel calendario la barra della proposta (`ProposalBar`), le attività proposte tratteggiate, "Genera piano base" e l'avviso di cambiamento.
- **Non c'è ancora:** l'SDK di Anthropic nel `pom.xml`, i valori di `ANTHROPIC_API_KEY` e `ANTHROPIC_MODEL` (in `.env.example` sono vuoti), la tabella `agent_message` usata da un service (esiste solo nello schema).

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

## Promemoria per il giorno 8

Sono anche in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card".

- **Prima di T32:** serve la **chiave API di Anthropic** con un po' di credito (circa 9 $ per 300 richieste, D59). La crea l'utente nella console di Anthropic e la mette nel `.env` e su Render: Claude non inserisce chiavi. Va aggiunta ad `AppProperties` come variabile obbligatoria, con un messaggio che la nomina. Lo strumento (SDK Java ufficiale di Anthropic) va confermato come scelta del corso o segnalato come fuori programma (D54).
- **T32 (client):** il client sta dietro un'interfaccia, così i test di T34 usano un finto client senza rete e senza costi (D63). Modello `claude-haiku-4-5` letto dalla configurazione (D59); timeout di 60 secondi per chiamata.
- **T33 (strumenti):**
  - `plan_schedule` = `PlannerService.draft(userId, ScheduleRequest)`. I parametri arrivano dal modello: vanno convertiti in `ScheduleRequest` e ogni errore (400 con `fields`, data scritta male) torna al modello come risultato di errore, non come 500 (D61). `excludedDates` ha al massimo 14 date (D123);
  - `create_plan_proposal` = `ProposalService.save(userId, draft, summary)` con l'**ultima bozza del turno** (D56). Per non salvare una bozza calcolata su dati superati, bloccare l'utente (`UserRepository.lockById`) prima di calcolarla, come fa `createBasic`. Il 409 di un piano senza attività torna al modello con l'avviso del pianificatore;
  - nessuno strumento ha un parametro `userId`: l'utente viene sempre dal token (D44, D55);
  - `ScheduleDraft` ha già copertura e avvisi in italiano da dare al modello.
- **T34 (ciclo):** massimo 6 giri; i passaggi ("Ho letto il tuo roster"...) li ricava il backend dagli strumenti chiamati (D62); le parti fisse (strumenti, istruzioni) all'inizio per la cache dei prompt; nei log niente testo dei messaggi (D60).
- **T35 (giorno 9):** decidere il limite apposito per `POST /api/me/proposals/basic` insieme al limite giornaliero dell'agente (D48).
- **T36 (giorno 9):** `summary` delle proposte va sempre mostrato come testo (lo scrive il modello); l'avviso di cambiamento può proporre anche di chiedere all'agente.
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101.
6. Bundle JavaScript di circa 630 KB per FullCalendar: se la presentazione diventa lenta, caricare la pagina del calendario solo quando serve (`lazy`, D119).
7. Limiti noti annotati nelle decisioni: catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114), la bozza dell'evento si perde chiudendo la finestra (D122), limiti del pianificatore (D123: copertura in centesimi contro il completamento arrotondato, due eventi potenziati nello stesso giorno, attività già fatte riconosciute dal titolo).
8. Limite apposito per il piano base: da decidere in T35.

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
