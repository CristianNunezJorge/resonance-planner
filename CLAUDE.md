# Resonance Planner · Capstone Project

Capstone del Master Full-Stack AI Developer (Epicode), progetto individuale di due settimane. È una **demo** di planner per Wuthering Waves: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma. "Resonance Planner" è un nome provvisorio.

## Stato attuale (24/10/2026, giorno 14 · piano completato)

- **Progetto approvato dallo staff.** Progettazione completa: 9 fasi; decisioni D1-D139.
- **Giorno 1 (T01-T06):** repository, Docker Compose, backend Spring Boot, schema Flyway, frontend Vite.
- **Giorno 2 (T07-T11):** `ApiResponse` ed errori, catalogo caricato dai JSON, API pubbliche `/api/game/**` con sicurezza di base e CORS, **primo deploy su Render**, client HTTP e tema scuro. L'app è online (link sotto).
- **Giorno 3 (T12-T16) completato:** JWT e ruoli (D99, D100), limiti di richieste con Cloudflare (D101), API del profilo (D102), login, registrazione, sessione e regole di accesso nel frontend (D103), pagina di presentazione (T16). Su Render il sito statico manda CSP e `X-Content-Type-Options: nosniff` (sezione Headers, non nel repository). Tutto online; su Trello T01-T16 sono in "Fatto".
- **Giorno 4 (T17-T21) completato:** contatore Waveplate e Crystals (D105), roster con le regole di livello e grado (D104), inventario (D108), configurazione guidata in backend (D106, D109, D110: limite alla dimensione del corpo JSON) e in frontend (D111). Decisioni D1-D111. Tutto online; su Trello T01-T21 sono in "Fatto".
- **Giorno 5 (T22-T24) completato:** calcolo dei materiali mancanti e del piano di farming (D112), eventi con edizioni e giorni attivi, API pubbliche e admin (D113, D114), pagina roster e inventario (D115, D116). Decisioni D1-D116. Tutto online; su Trello T01-T24 sono in "Fatto".
- **Giorno 6 (T25-T28) completato · v1 rilasciata:** attività manuali, completamento con l'inventario e calendario in backend (D117, D118), pagina calendario con FullCalendar 7 e riquadro Waveplate (D119, D120), profilo e gestione eventi admin (D121, D122), README e checklist di rilascio (`docs/rilascio-v1.md`, 9 punti ✓ online), tag `v1.0.0`. Tutto online; su Trello T01-T28 sono in "Fatto".
- **Giorno 7 (T29-T31) completato:** pianificatore giorno per giorno (D123), proposte con piano base, conferma e scarto (D124), barra della proposta, attività tratteggiate e avviso di cambiamento con l'impronta dei dati e la migrazione `V2` (D125). Tutto online.
- **Giorno 8 (T32-T34) completato:** per scelta dell'utente l'agente usa un **modello gratuito su OpenRouter** (`nvidia/nemotron-3-super-120b-a12b:free`) al posto dell'API Claude, ed è fuori dal programma del corso (D126); client OpenRouter con `RestClient` (T32), i cinque strumenti limitati all'utente del token (D127), ciclo dell'agente con al massimo 6 giri (D128). Tutto online, ma senza endpoint: la chat arriva con T35-T36.
- **Giorno 9 (T35-T36) completato:** API della chat con salvataggio dei messaggi e limiti (D129: 5 messaggi al giorno per utente, 24 chiamate al modello per utente anche dei turni falliti, 40 per l'app, un turno alla volta, 20 proposte al giorno), migrazione `V3`; chat nel calendario, laterale da 992 px e a scomparsa sotto, con attesa, passaggi, limiti, "Spiegamelo" e "Chiedi all'agente" (D130). Prova vera riuscita. Tutto online.
- **Giorno 10 (T37-T38) completato · v2 rilasciata:** istruzioni di sistema rifinite e dieci scenari reali superati, divisi su due giorni per la quota gratuita (D131, `docs/scenari-agente.md`); dal 20/10 il modello predefinito è `nvidia/nemotron-3-ultra-550b-a55b:free`, anche su Render, perché il fornitore di Super è caduto (D132); README e checklist di rilascio (`docs/rilascio-v2.md`, tutti i punti ✓ online il 21/10, il punto 8 saltato), tag `v2.0.0`. Tutto online; su Trello T01-T38 sono in "Fatto".
- **Giorno 11 (X01-X02) completato · primi extra della v3:** export del piano in Excel con Apache POI (D133: fogli "Piano" ed "Eventi", `GET /api/me/calendar/export`, pulsante "Esporta in Excel"; online la prima esportazione dopo un avvio impiega circa 14 s, accettati) e accesso ospite "Prova la demo" (D134: `POST /api/auth/guest`, ospiti cancellati dopo 24 ore da un job `@Scheduled`, quota dell'agente per gli ospiti con la migrazione `V4`). Tutto online; su Trello X01-X02 sono in "Fatto".
- **Giorno 12 (X03) completato:** notifiche in tempo reale (D135) con un **WebSocket semplice** su `/ws`, senza STOMP (scelta dell'utente). Il token viaggia nel primo messaggio `AUTH`, poi c'è un `PING` ogni 25 secondi. Il job di ogni minuto avvisa per le Waveplate quasi piene e gli eventi in scadenza, e dopo una modifica dell'admin dice a tutti che gli eventi sono cambiati. CSP con `wss://`, anche su Render. Online le notifiche arrivano; su Render però le chiusure decise dal server non arrivano al browser (limite accettato in D135). Tutto online; su Trello X01-X03 sono in "Fatto".
- **Giorno 13 (T39-T41) completato:** README completo con screenshot, verificato con un clone pulito (T39); Swagger con descrizione generale ed esempi su ogni campo (D136); Lighthouse a 100 in accessibilità, best practice e SEO su tutte le pagine (anche online), FullCalendar 7.1.3, calendario caricato solo quando serve (D137), riepilogo in `docs/qualita.md` (T41). `ANTHROPIC_MODEL` tolta da Render. Tutto online; su Trello T01-T41 sono in "Fatto".
- **Giorno 14 (T42-T43) completato · versione finale rilasciata:** copione della demo di 3 minuti sui cinque punti del Demo Day, con preparazione, piano B e domande probabili (`docs/demo.md`, D138: account già pronto, un solo messaggio all'agente, presentazione dal sito online); checklist finale sul sito online, 19 punti tutti ✓ il 23-24/10 (`docs/rilascio-v3.md`). README con lo stato finale e tag `v3.0.0`. Tutto online; su Trello T01-T43 e X01-X03 sono in "Fatto".
- **Dopo il giorno 14 · grafica nuova:** il frontend ha lo stile di Vaelithar, il prototipo dell'autore sullo stesso gioco (D139): terminale scuro, accento ciano, caratteri monospazio, stesse tecnologie. Il pallino del footer dice se le notifiche in tempo reale sono collegate.
- **Prossimo passo:** il piano dei 14 giorni è completo. Da qui si fanno le **modifiche che l'utente decide**, ognuna con lo stesso flusso (ramo, controlli, revisione, commit approvato, PR) e una decisione nuova quando cambia una scelta già presa. X04 e X05 restano fuori (D90), salvo decisione diversa dell'utente. Leggere `HANDOFF.md`.

## Da leggere per riprendere

1. `docs/stato-del-progetto.md`: cosa è stato fatto, cosa resta aperto, promemoria per le prossime card.
2. `docs/09-task.md`: piano giorno per giorno e piano B.
3. `docs/decisioni.md`: registro di tutte le decisioni (cosa, come, perché, alternative scartate).
4. `docs/README.md`: indice delle fasi di progettazione.

I documenti di fase da 01 a 09 sono la fonte di verità. Se il codice deve discostarsene, prima si aggiorna la decisione.

## Come lavorare con l'utente

- Rispondere in **italiano**, in modo semplice e diretto. L'utente è uno studente: al Demo Day deve saper spiegare ogni scelta, quindi spiegare sempre il perché.
- **Ogni decisione nuova** va in `docs/decisioni.md`, con stato *Proposta* finché l'utente non conferma; poi si aggiorna il documento di fase.
- Proporre con una raccomandazione e chiedere conferma: non cambiare da soli decisioni già prese.
- **Restare sugli strumenti del programma del corso.** Se si propone uno strumento fuori programma, segnalarlo esplicitamente.
- Nel browser, quando l'utente guarda lo schermo condiviso: **niente screenshot**, verificare con strumenti testuali.
- Chiedere il permesso prima di scaricare file.
- **Flusso di ogni card:** un ramo, controlli locali, revisione del codice, poi l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card: `feat: ... (T07)`). Poi push, pull request, *squash merge* e cancellazione del ramo. Nessuna firma o co-autore di Claude nei commit e nelle pull request.

## Comandi

Prerequisiti: Docker Desktop avviato, JDK 21, Node 24. Il `.env` nella cartella principale (copiato da `.env.example`) serve a Docker Compose e al backend in `dev`.

```bash
# Database: PostgreSQL 16 sulla porta 5433, con il database resonance_planner
docker compose up -d
docker compose down        # ferma il container, i dati restano
docker compose down -v     # cancella i database locali: al riavvio Flyway ricrea lo schema

# Backend (dalla cartella backend/)
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"   # in Git Bash
./mvnw spring-boot:run     # profilo dev, http://localhost:8080
./mvnw package             # jar in target/

# Frontend (dalla cartella frontend/)
npm install
npm run dev                # http://localhost:5173
npm run lint
npm run typecheck
npm run build
```

Prima di ogni pull request: `./mvnw package` nel backend; `npm run lint`, `npm run typecheck` e `npm run build` nel frontend (D81).

## Stack e versioni (D77)

| Parte | Scelta | Versioni fissate il 10/10/2026 | Decisioni |
|---|---|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS, FullCalendar | Node 24.21 (minimo 24.15), React 19.3, Vite 8.3, TypeScript 6.0, Redux Toolkit 2.13, React Router 8.4, Bootstrap 5.3.8, Sass 1.105, FullCalendar 7.1.3 (dal 23/10, prima 7.1.1) con il tema Bootstrap 5 e `temporal-polyfill` 1.0.5 (D119), caricato solo con il calendario (D137) | D7, D76, D119, D137 |
| Backend | Java, Spring Boot, Maven, Spring Data JPA, Spring Security + JWT, springdoc, WebSocket | Java 21 (Temurin 21.0.12.1), Spring Boot 4.1.1, Maven 3.10 (wrapper), springdoc 3.1.1, Apache POI 5.5.1 (`poi-ooxml`, export Excel), `spring-boot-starter-websocket` (WebSocket semplice, senza STOMP); Swagger con esempi su ogni campo (D136) | D5, D45, D77, D133, D135, D136 |
| Database | PostgreSQL, schema con Flyway (Hibernate in `validate`) | PostgreSQL 16 (Docker `postgres:16`) | D38, D98 |
| IA | OpenRouter con un modello gratuito (`nvidia/nemotron-3-ultra-550b-a55b:free` dal 20/10, D132), chiamato con `RestClient`, ciclo tool-use scritto da noi | — | D54, D59, D126, D132 |
| Verifica | Checklist manuale sul sito online a ogni rilascio | — | D79 |
| Deploy | Tutto su Render: frontend, backend e database PostgreSQL (il database gratuito scade dopo 30 giorni e va ricreato) | — | D82 |

Principi da non perdere:
- I calcoli li fa Java; l'IA sceglie i parametri e spiega (D3).
- Il modello non scrive mai le attività: la proposta è sempre l'ultima bozza del pianificatore (D56).
- L'agente propone, l'utente conferma (D2). Esiste un "piano base" senza IA (D64).
- L'agente deve restare **gratuito**: solo modelli OpenRouter con l'id che finisce in `:free`, controllato all'avvio (D126). Il piano gratuito dà 50 richieste al giorno per tutto l'account, e un messaggio ne usa fino a 6: i limiti di D129 le contano nel database (`agent_model_calls`), giorno UTC. Gli ospiti hanno 2 messaggi al giorno e 18 chiamate per tutti insieme (D134).
- I testi del modello si mostrano sempre come testo semplice, mai HTML (D60, D130).
- Dati personali solo sotto `/api/me`; l'utente si ricava dal token (D44).
- Eventi attivi giorno per giorno (D96), effetti con limite giornaliero (D94), eventi della demo ricorrenti (D29).
- Calcolo dei materiali fino al livello 40, fase SOL3 4 (D27, D92).
- Notifiche in tempo reale (D135): il server decide (D3), il token arriva nel primo messaggio del WebSocket e mai nell'indirizzo, la connessione è aperta solo a pagina visibile, e il testo si mostra come testo semplice.

## Struttura del progetto

La struttura completa prevista è in `docs/08-regole-di-lavoro.md`. Oggi esiste:

```
.env.example                  variabili d'ambiente, senza valori
docker-compose.yml            PostgreSQL locale
backend/
  pom.xml, mvnw, Dockerfile   Spring Boot 4.1.1, Maven wrapper, immagine in due fasi per Render
  src/main/java/com/resonanceplanner/
    common/api/               ApiResponse, ApiError (D46)
    common/error/             ErrorCode, eccezioni, GlobalExceptionHandler
    common/config/            AppProperties (variabili obbligatorie, D83), DatabaseConfig, ClockConfig, Today ("oggi" in ora italiana, D41), SchedulingConfig (@EnableScheduling, D134), OpenApiConfig (token di Swagger e descrizione generale, D53, D136)
    common/security/          SecurityConfig (accessi e CORS), ApiSecurityErrorHandler (401/403)
    common/data/              DataFileReader: lettura rigorosa dei JSON di data/
    catalog/                  entità, repository, GameRules, CatalogService e CatalogController, dto/
    auth/                     registrazione, login e ospite (AuthService), JWT (JwtService, filtro), GuestCleanup (ospiti cancellati dopo 24 ore, D134)
    user/                     User con WaveplateSnapshot, UserService (anche requireAdmin), PlayTimeRules, profilo
    waveplate/                WaveplateRules (calcolo puro), service e API (D28, D105)
    roster/                   OwnedCharacter, RosterRules (livello e grado, D104), service e API
    inventory/                InventoryItem (sola lettura), scrittura con un solo upsert (D108)
    onboarding/               configurazione guidata in una transazione (D50, D109)
    farming/                  NeedCalculator e FarmingCalculator (calcolo puro su FarmingCatalog), service e API (D112)
    event/                    GameEvent, EventCalendar (edizioni e giorni attivi), EventRules, API pubbliche e admin (D113, D114)
    planning/                 PlannedTask, attività manuali e completamento (TaskService, CompletionRewards), calendario (D117, D118), proposte (ProposalService, D124), export Excel (PlanSpreadsheet, CalendarExportService, D133)
    planner/                  pianificatore giorno per giorno: Planner e NeedPool (calcolo puro), PlannerService (legge i dati, D57, D123), PlanInputs (impronta, D125)
    agent/client/             ModelClient e OpenRouterClient: Chat Completions con RestClient, 60 s per chiamata, solo modelli :free (D126)
    agent/tools/              AgentTools (i cinque strumenti), AgentToolDefinitions (schemi), AgentTurn (stato del turno), D127
    agent/                    AgentLoop (ciclo, al massimo 6 giri, conta ogni chiamata), AgentPrompt (istruzioni di sistema), AgentReply, D128
    agent/client/             anche ModelCallCounter e ModelCallDay: chiamate al modello per giorno UTC e utente (D129)
    agent/chat/               AgentChatController (/api/me/agent/messages e /usage), AgentChatService (turno senza transazione), AgentMessageService, AgentQuota (limiti), AgentTurnGate (turni insieme), D129
    notification/             notifiche in tempo reale (D135): NotificationSocketConfig e NotificationSocketHandler (/ws, AUTH e PING), NotificationSessions (connessioni in memoria), NotificationRules (calcolo puro), Notifier (job di ogni minuto, controllo dopo l'AUTH, avviso dopo le modifiche dell'admin), SentNotices
    seed/                     caricamento di catalogo ed eventi demo all'avvio (D39)
  src/main/resources/
    application.yml           comune; application-dev.yml legge ../.env; application-prod.yml per Render
    db/migration/V1__schema.sql, V2__plan_inputs.sql (impronta del piano, D125), V3__agent_model_calls.sql (D129), V4__agent_model_calls_guest.sql (quota degli ospiti, D134)
    data/                     6 JSON di gioco + README con fonti e stime
frontend/
  src/api/client.ts           unico client HTTP: ApiResponse → dati o ApiError (D76); apiPage per le pagine con meta (D130); apiDownload per i file (D133); apiSocketUrl per l'indirizzo del WebSocket (D135)
  src/app/                    store, hook tipizzati, rotte, layout, toastSlice
  src/features/catalog/       tipi, chiamate e catalogSlice (personaggi, regole e materiali caricati una volta), icons.ts
  src/features/landing/       pagina di presentazione P1 (T16): frase, "Prova la demo" con l'ospite (D134), tre passi, personaggi
  src/features/onboarding/    configurazione guidata P3 (T21): bozza in Redux, tre passi, livelli e ascensione (D111)
  src/features/progress/      LevelPicker e funzioni dei livelli (rankOf, draftOf), comuni a configurazione e roster
  src/features/roster/        pagina roster e inventario P5 (T24): rosterSlice, schede, modulo dei livelli (D115)
  src/features/calendar/      pagina calendario P4 (T26, T31): CalendarView (unico componente con FullCalendar), calendarSlice, finestre, ProposalBar e avviso di cambiamento (D119, D120, D124, D125), exportFile.ts ed "Esporta in Excel" (D133); LazyCalendarPage la carica solo quando serve (D137)
  src/features/chat/          chat con l'agente (T36, D130): chatSlice (bozza, invio, pagine), ChatPanel, ChatMessages, ChatComposer, ChatDrawer, format.ts (plainText, limiti, suggerimenti)
  src/features/notifications/ notifiche in tempo reale (D135): notificationSocket.ts (WebSocket nativo, AUTH, PING, riconnessione), useNotifications (montato in RootLayout)
  src/features/waveplate/     riquadro Waveplate, useWaveplate, validateWaveplate comune con la configurazione
  src/features/profile/       pagina profilo P6 (T27): modifica, eliminazione dell'account con l'email (D121), o con il nome per l'ospite (D134)
  src/features/adminEvents/   gestione eventi P7 (T27): tabella, EventFormModal, eventForm.ts, adminEventsSlice (D122)
  src/components/             LoadingState, ErrorState, EmptyState, FormField, SelectField, CharacterAvatar, Modal e ConfirmModal (D116, D120), useFocusTrap, useMediaQuery, usePageVisible, pagine 404 ed errore
  src/styles/                 _theme.scss (tema scuro D65, colori di D139), _terminal.scss (sfondo, header, footer e card nello stile di Vaelithar, D139), _elements.scss, _calendar.scss, _chat.scss, main.scss
  public/characters/          6 icone WebP 256×256; public/robots.txt
docs/                         progettazione (fasi 01-09, decisioni, stato), rilascio-v1/v2/v3.md (checklist online), qualita.md (Lighthouse, T41), demo.md (copione del Demo Day, T42), screenshot/ (immagini del README)
```

## Convenzioni

- Codice, nomi e commit in inglese; documentazione in italiano.
- Conventional Commits (`feat:`, `fix:`, `docs:`, ...), un ramo per attività, `main` sempre funzionante, almeno un push al giorno.
- Backend, dati dell'utente: i repository estendono `Repository` (non `JpaRepository`) e ogni metodo prende l'utente (D44); le scritture che possono arrivare insieme sono un'unica istruzione (upsert, `update ... where`); gli errori su più campi usano `FieldValidationException(Map)`.
- Frontend: la bozza di un modulo lungo sta in uno slice che si azzera con `sessionEnded` e con ogni nuovo login e ignora le risposte di richieste di una sessione finita (`requestId`); messaggi di errore con `text-danger-emphasis` (contrasto AA); per guidare il browser di prova senza screenshot si usano piccoli script con `javascript_tool` (il pannello può non essere visibile e i clic reali falliscono).
- Nessun segreto nel codice: solo variabili d'ambiente (elenco in `docs/08-regole-di-lavoro.md`). Nuove variabili obbligatorie vanno in `AppProperties`, con un messaggio che nomina la variabile.
- Schema: una migrazione già unita a `main` non si modifica mai, se ne crea una nuova (`V2__...`). Le colonne intere sono `integer` (D98) e ogni `CHECK` controlla anche `IS NOT NULL`, perché un controllo che vale NULL viene considerato superato.
- Backend: un errore su un campo si lancia con `ConflictException(messaggio, campo)` o `FieldValidationException(campo, messaggio)` (risposta con `fields`); un service che scrive dati dell'utente risponde 401 con `UnauthorizedException` se l'utente del token non esiste più (D102). Gli errori voluti si lanciano con `NotFoundException`/`ConflictException` e un messaggio in italiano per l'utente; i DTO hanno messaggi di validazione in italiano scritti da noi. Le entità diventano DTO dentro un service `@Transactional(readOnly = true)` (open-in-view è spento).
- Frontend: tutte le chiamate passano da `api/client.ts`; ogni pagina usa `LoadingState` ed `ErrorState` (D69).
- Swagger (D136): ogni campo di un DTO ha `@Schema(example = ...)` (per un esempio `null`: `nullable = true`, e sui riferimenti anche `types = {"object", "null"}`), ogni parametro `@Parameter`; gli esempi delle richieste devono essere richieste valide.
- Definizione di "fatto" in 5 punti: `docs/08-regole-di-lavoro.md`.

## Link utili

- Repository: https://github.com/CristianNunezJorge/resonance-planner
- App online: https://resonance-planner.onrender.com · API e Swagger: https://resonance-planner-api.onrender.com/swagger-ui.html
- Render: servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db` (PostgreSQL 16, Frankfurt, **scade il 10/11/2026**)
- Board Trello: https://trello.com/b/JCkNlahy/capstone
- Proposta per lo staff (artifact): https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF

## Note operative

- **Su questo computer:** il `JAVA_HOME` globale punta al JDK 25, quindi nei comandi va indicato il JDK 21. Il PostgreSQL 18 installato occupa la porta 5432, perciò Docker usa la 5433. In background il wrapper si lancia con `sh ./mvnw` (con `./mvnw` può non essere trovato).
- **Server locali (`.claude/launch.json`):** `frontend` (sviluppo) e `frontend-prod` (build con la CSP, prima serve `npm run build`) usano entrambi la porta 5173, l'unica accettata dal CORS locale: se ne accende uno alla volta. Se il backend esce subito con `Connection to localhost:5433 refused`, il container del database è fermo (succede dopo un riavvio di Docker Desktop): `docker compose up -d`.
- **Variabili facoltative** (`APP_RATELIMIT_*`, `APP_AGENTLIMITS_*`): valgono solo come vere variabili d'ambiente; scritte nel `.env` vengono ignorate, perché il `.env` entra come file di proprietà e solo le obbligatorie sono richiamate con `${...}` in `application.yml`.
- **Lighthouse e screenshot:** con Chrome DevTools (MCP `chrome-devtools`): `lighthouse_audit` con i report nello scratchpad, `take_screenshot` con `filePath` per salvare un'immagine senza guardarla. Dettagli in `HANDOFF.md`.
- **Spring Boot 4:** gli starter hanno nomi nuovi (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`); `DataSourceBuilder` è in `org.springframework.boot.jdbc`.
- **React Router 8:** `RouterProvider` si importa da `react-router/dom`, tutto il resto da `react-router`. Il pacchetto `react-router-dom` non esiste più.
- **Jackson 3:** i pacchetti sono `tools.jackson.*`, ma le annotazioni restano in `com.fasterxml.jackson.annotation`. Il mapper di Spring ignora le chiavi sconosciute; per i nostri file c'è `DataFileReader`, che le rifiuta.
- **Render:** si lavora con Claude in Chrome (nel browser integrato non c'è la sessione). I segreti (password del database, admin) li inserisce l'utente; `JWT_SECRET` e `ADMIN_PASSWORD` sono generati da Render. Il backend gratuito al risveglio impiega circa 3 minuti. Se un deploy risponde "Service Unavailable", guardare prima https://status.render.com; con la scheda di Chrome nascosta i menu della dashboard si aprono solo via JavaScript (dettagli in `HANDOFF.md`). `VITE_API_BASE_URL` è letta al build: dopo averla cambiata serve un nuovo deploy del sito statico. Dopo un merge controllare in Events che il deploy parta (il 21/10 per #53 non è partito; il 23/10 sono partiti da soli). Le variabili d'ambiente del backend sono le 9 che il codice usa (`ANTHROPIC_MODEL` tolta il 23/10). Dietro il proxy di Render le chiusure dei WebSocket decise dal server non arrivano al browser (D135).
- **OpenRouter:** `OPENROUTER_API_KEY` e `OPENROUTER_MODEL` sono nel `.env` e su Render (la chiave la inserisce l'utente). Il piano gratuito dà 20 richieste al minuto e 50 al giorno; un fornitore gratuito può rispondere 429 anche con la quota intatta (Gemma il 17/10).
- **Norton su questo computer** ispeziona HTTPS e rifirma i certificati: Java non se ne fida (`PKIX path building failed`). Per chiamare OpenRouter da Java in locale si usa l'archivio di Windows: `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (già nella configurazione `backend` di `.claude/launch.json`). Su Render non serve.
- Il wiki Fandom di Wuthering Waves blocca WebFetch (HTTP 402): leggerlo con il browser integrato. I contenuti sono CC BY-SA, quindi la fonte va citata.
- **Script e fine riga:** i file sono in CRLF e la shell dimezza le barre rovesciate negli script passati in linea. Per testo con barre rovesciate (regex, escape) usare lo strumento di modifica; in uno script Python che sostituisce testo, non convertire due volte gli a capo (il risultato sarebbe `CR CR LF` e Git vedrebbe il file come binario).
- **Contatori dell'agente in locale:** `agent_model_calls` del database locale è diverso da quello di Render, ma la quota di OpenRouter è la stessa. Per provare un limite senza consumare richieste si scrive la riga a mano con `psql` dentro il container (`docker exec resonance-planner-db ...`).
- Docker Desktop può restare bloccato in avvio (i comandi `docker` non rispondono): Restart dal menu della balena, poi se serve `wsl --shutdown` da PowerShell e riaprire Docker Desktop.
- Trello: per spostare le card con `fetch` serve il cookie `dsc` della pagina nel corpo (form-encoded), altrimenti 403 "CSRF detected". Il testo digitato in una card aperta può diventare una scorciatoia da tastiera. Le descrizioni si scrivono via JavaScript con `execCommand`, dopo aver verificato il focus sull'editor. Le etichette si possono applicare con i tasti 1-0 sulla card, uno alla volta e con una pausa. Il riepilogo `/b/JCkNlahy.json` può essere in ritardo di qualche secondo.
