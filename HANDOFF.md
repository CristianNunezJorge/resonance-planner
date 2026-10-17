# Handoff · giorno 9

Passaggio di consegne del 17/10/2026, a fine giorno 8 (l'agente in backend), per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 9 (T35-T36): API della chat con salvataggio dei messaggi e limiti, poi la chat nel frontend.
Prima di scrivere codice riassumimi il piano e le eventuali decisioni nuove (soprattutto i limiti), e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-8** (T01-T34), tutti su `main` e online |
| Rilascio | **v1** con tag `v1.0.0`; la v2 (agente) è prevista a fine giorno 10 |
| Ultimo commit su `main` | `a5fdea3`, T34 (PR #42), più il commit di questa chiusura |
| Test | Backend 798 (`./mvnw verify`, copertura 98,1%), frontend 492 (`npm test`, copertura 97,9%) |
| Decisioni | D1-D128, tutte *Confermate* (D126-D128 nate nel giorno 8) |
| Agente | Modello **gratuito** su OpenRouter, `nvidia/nemotron-3-super-120b-a12b:free` (D126, scelta dell'utente, fuori dal programma del corso). Solo modelli `:free`: il backend non parte con un modello a pagamento |
| Schema | Migrazioni `V1__schema.sql` e `V2__plan_inputs.sql`; la tabella `agent_message` c'è già da V1 |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Deploy di T34 verificato (`/actuator/health` `UP`). Variabili nuove `OPENROUTER_API_KEY` (messa dall'utente) e `OPENROUTER_MODEL`. `ANTHROPIC_MODEL` è ancora lì ma non serve più |
| Trello | T32-T34 da spostare in "Fatto" |
| Account di prova in locale | Nessuno fisso: si crea un account con una password casuale (script o `javascript_tool`) e lo si cancella alla fine (`DELETE /api/me`). Online non si creano account di prova |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 9 · Agente: chat (T35-T36)

Dettagli in `docs/09-task.md`, `docs/05-api.md` ("Messaggio all'agente") e `docs/06-agente.md`.

| Card | Cosa | Decisioni |
|---|---|---|
| T35 | Salvataggio dei messaggi, storico a pagine, limite giornaliero, API di utilizzo | D40, D48, D52, D61 |
| T36 | Chat: messaggi, suggerimenti, passaggi, contatore, attesa, pannello a scomparsa su telefono | D11, D62, D66 |

**Da decidere all'inizio di T35 (decisione nuova):** i limiti. OpenRouter gratuito dà **50 richieste al giorno per tutto l'account** (1.000 se un giorno si comprano 10 $ di credito) e 20 al minuto; un messaggio all'agente usa **fino a 6 richieste**. I 20 messaggi al giorno per utente di D48 sono quindi troppi. La revisione di sicurezza di T34 propone:
- un contatore globale delle chiamate al modello, cioè i `rounds` di ogni turno, anche di quelli falliti;
- un limite per utente al giorno e al minuto;
- un solo turno alla volta per utente (429 se ce n'è già uno in corso);
- un limite di richieste sull'endpoint (D101);
- un limite apposito per il piano base (`POST /api/me/proposals/basic`).

Il criterio di T35 in `09-task.md` ("il ventunesimo messaggio del giorno risponde 429") va aggiornato al limite scelto.

## Cosa esiste già nel codice

- **Backend:** `auth/`, `user/`, `waveplate/`, `roster/`, `inventory/`, `onboarding/`, `catalog/`, `farming/`, `event/`, `planner/`, `planning/` (attività, calendario, proposte), `common/`. Novità del giorno 8:
  - `agent/client/`: `ModelClient`, `OpenRouterClient` (Chat Completions con `RestClient`, 60 secondi in tutto per chiamata, al massimo 1 MB di risposta e 2.048 token, solo modelli `:free`), `AgentUnavailableException` (502, con un messaggio apposta per il 429);
  - `agent/tools/`: `AgentTools.run(turn, call)` con i cinque strumenti, argomenti letti in modo rigoroso, ogni errore come risultato per il modello; `AgentTurn` (utente, ultimo piano, proposta, passaggi); `ProposalService.saveIfUnchanged` ricalcola il piano e lo confronta prima di salvarlo (D127);
  - `agent/`: `AgentLoop.answer(userId, history, message)` (al massimo 6 chiamate al modello, 2 minuti, 5 strumenti per risposta e 10 per turno), `AgentPrompt` (istruzioni di sistema e riga della data), `AgentReply` (testo, proposta o `null`, passaggi, token, giri), `AgentStoppedException` (502).
- **API pronte:** `/api/auth/**`, `/api/me` (profilo, `PATCH`, `DELETE`), `/api/me/waveplate`, `/api/me/roster`, `/api/me/inventory`, `/api/me/onboarding`, `/api/me/farming-plan`, `/api/me/tasks` (con `/complete`), `/api/me/calendar` (con `pendingProposal` e `planOutdated`), `/api/me/proposals` (`pending`, `basic`, `{id}/confirm`, `{id}/reject`), `/api/game/**`, `/api/events`, `/api/admin/events`.
- **Frontend:** tutte le pagine della v1; nel calendario la barra della proposta, le attività proposte tratteggiate, "Genera piano base" e l'avviso di cambiamento.
- **Non c'è ancora:** `POST /api/me/agent/messages`, lo storico (`GET /api/me/agent/messages`), l'utilizzo (`GET /api/me/agent/usage`), il service che salva in `agent_message`, i limiti dell'agente e la chat nel frontend.

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
- L'agente deve restare gratuito: non proporre modelli a pagamento.

## Promemoria per il giorno 9

Sono anche in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card" (righe T32, T33, T34 e T35).

- **T35 (backend):**
  - il DTO del messaggio usa `AgentLoop.MAX_MESSAGE_CHARS` (1.000), con un messaggio di validazione in italiano sul campo `content`;
  - lo storico da passare ad `answer` è quello salvato, senza il messaggio nuovo: il ciclo prende da solo gli ultimi 10 con un testo;
  - **nessuna transazione intorno ad `answer`** (D128): i due messaggi si salvano dopo, in una transazione breve, e solo se `answer` riesce. `AgentStoppedException`, `AgentUnavailableException` e ogni altro errore non salvano e non contano nel limite dell'utente (D61), ma le chiamate fatte contano nel contatore globale;
  - la risposta salvata collega la proposta (`agent_message.proposal_id`) e ha un tetto di lunghezza (circa 4.000 caratteri), perché torna nello storico di ogni richiesta;
  - la risposta dell'endpoint è quella di `05-api.md`: `message`, `proposal`, `steps`, `usage` con `resetsAt` alla mezzanotte italiana (D41);
  - mai nei log il corpo della richiesta, `AgentReply`, `ModelReply` o `ToolCall`, perché i loro `toString` contengono i testi; il livello DEBUG di `com.resonanceplanner.agent` resta spento in produzione;
  - `UnauthorizedException` (utente eliminato) dà 401 come negli altri service (D102).
- **T36 (frontend):**
  - mostrare la risposta, `steps` e `summary` sempre come testo semplice, senza markdown né link;
  - dire nella chat che i messaggi vanno a un modello gratuito di terzi (D126);
  - dopo una proposta, ricaricare il calendario; l'avviso di cambiamento può proporre anche di chiedere all'agente;
  - durante l'attesa un indicatore, perché una risposta può richiedere fino a qualche minuto.
- **T37 (giorno 10):** rifinire le istruzioni di sistema (`agent/AgentPrompt`) con i dieci scenari di `06-agente.md`; valutare un tetto alla lunghezza delle descrizioni degli eventi nell'API admin, perché il modello potrebbe copiarne il testo nel `summary`.
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101.
6. Bundle JavaScript di circa 630 KB per FullCalendar: se la presentazione diventa lenta, caricare la pagina del calendario solo quando serve (`lazy`, D119).
7. Limiti noti annotati nelle decisioni: catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114), la bozza dell'evento si perde chiudendo la finestra (D122), limiti del pianificatore (D123), modelli gratuiti che possono sparire o rispondere 429 (D126), impronta di D125 calcolata dopo la bozza (promemoria di T33).
8. Limiti dell'agente e del piano base: da decidere in T35 (vedi sopra).
9. `ANTHROPIC_MODEL` su Render non serve più: si può togliere (Claude in Chrome, "Save only").

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container può fermarsi da solo (per esempio dopo un riavvio di Docker): se i test non trovano il database, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`;
  - **Norton** ispeziona HTTPS e rifirma i certificati: Java non se ne fida. Per chiamare OpenRouter da Java in locale serve `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (già nella configurazione `backend` di `.claude/launch.json`); con Maven si passa con `JAVA_TOOL_OPTIONS`. Su Render non serve.
- **OpenRouter:**
  - la chiave è nel `.env` (`OPENROUTER_API_KEY`) e su Render: Claude non la legge e non la scrive. La chiave ha un limite di credito di pochi dollari (6 $ secondo l'API il 17/10), scade il 15/01/2027, e l'account non ha credito né un metodo di pagamento;
  - nelle impostazioni Privacy sono permessi i fornitori gratuiti che si addestrano sui dati, non quelli che pubblicano i prompt;
  - chiamata di prova vera (2 delle 50 richieste del giorno), dalla cartella `backend/`, in Git Bash:
    ```bash
    set -a; eval "$(grep -E '^OPENROUTER_(API_KEY|MODEL)=' ../.env | tr -d '\r')"; set +a
    JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT" OPENROUTER_LIVE_TEST=true ./mvnw -q test -Dtest=OpenRouterClientLiveTest -Djacoco.skip=true
    ```
  - le richieste gratuite rimaste oggi si leggono con `GET https://openrouter.ai/api/v1/key` e la chiave nell'header `Authorization` (campo `free_model_daily_requests`);
  - se un modello risponde sempre 429 con la quota intatta, provare l'altro modello gratuito (`OPENROUTER_MODEL`), poi i modelli con strumenti elencati da `https://openrouter.ai/api/v1/models`.
- **Windows:**
  - i file hanno fine riga CRLF: per le sostituzioni usare lo strumento di modifica o uno script Python che rispetta il fine riga;
  - uno script lungo con heredoc in Bash può non partire, e le barre rovesciate possono dimezzarsi: scriverlo in un file nello scratchpad.
- **Browser di prova:**
  - guidare la pagina con `javascript_tool` (setter nativo più eventi `input`/`change`) e leggere il testo;
  - FullCalendar disegna gli elementi solo se la scheda è **in primo piano** (`tabs_select`): in una scheda nascosta `requestAnimationFrame` non parte e gli elementi restano invisibili;
  - i server si avviano con `preview_start` (`backend`, `frontend-prod`) e si fermano con `preview_stop`. Il pannello stretto mostra la vista telefono del calendario: per la vista computer usare `resize_window` e poi riportarlo a `desktop`.
- **Render:**
  - servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti; un deploy dura circa 4 minuti;
  - salvare le variabili d'ambiente con "Save only" (menu accanto a "Save, rebuild, and deploy"); salvare può comunque far ripartire l'ultima versione;
  - con la scheda di Chrome nascosta i clic non aprono i menu: "Manual Deploy" si apre con `button.click()` via JavaScript.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
