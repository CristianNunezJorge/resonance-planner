# Stato del progetto

Riepilogo delle sessioni dal 30/09 al 14/10/2026, per riprendere il lavoro in una nuova sessione.

## In una frase

Il progetto è approvato e i giorni 1, 2 e 3 (T01-T16) sono completati, uniti su `main` e online: JWT e ruoli, limiti di richieste, profilo, login e registrazione nel frontend, pagina di presentazione. Il sito statico su Render manda CSP e `nosniff`. Il prossimo passo è il giorno 4 (T17-T21). Dettagli in `HANDOFF.md`.

## Cosa è stato fatto

| Data | Attività | Risultato |
|---|---|---|
| 30/09 | Analisi del programma del corso e dell'idea | Scelto il cuore del progetto: calendario + agente; damage calculator solo come extra (D1) |
| 30/09 | Proposta per lo staff | Artifact con proposta in 14 righe più un allegato tecnico |
| 01/10 | Fasi 1-8 della progettazione | Scope, flussi, dati, modello dati, API, agente, schermate, regole di lavoro |
| 01/10 | Versione snella degli strumenti | Tolti Playwright, Spotless e Prettier; Testcontainers e GitHub Actions facoltativi; deploy su Render come nel corso |
| 02/10 | Fase 9 | Piano di 14 giorni con 43 task, piano B e board |
| 02/10 | Raccolta dei dati di gioco | 6 JSON verificati sul wiki, controllo incrociato, 6 icone scaricate |
| 02/10 | Decisioni nate dalla raccolta | D92-D97, compresi gli eventi attivi giorno per giorno chiesti dall'utente (D96) |
| 02-03/10 | Board Trello | 5 colonne, 54 card, 10 etichette, descrizioni su ogni card |
| 10/10 | Deploy tutto su Render | Anche il database passa su Render invece che su Neon (D82 aggiornata) |
| 10/10 | Ambiente di sviluppo | Installati WSL 2 e Docker Desktop; scelto il JDK 21 già presente |
| 10/10 | **Giorno 1: T01-T06** | Repository pubblico, Docker Compose, backend Spring Boot 4.1.1, schema Flyway, frontend Vite |
| 10/10 | Decisione nata dalla revisione dello schema | D98: colonne `integer` invece di `smallint`, medie dei drop con al massimo due decimali |
| 11/10 | **Giorno 2: T07-T11** | Errori e `ApiResponse`, catalogo dai JSON, API pubbliche con sicurezza e CORS, primo deploy, client HTTP e tema |
| 12-14/10 | **Giorno 3: T12-T16** | JWT (D99-D100), limiti di richieste e correzione per Cloudflare (D101), API del profilo (D102), frontend di login, registrazione, sessione e accesso alle pagine (D103), pagina di presentazione (T16). CSP e `nosniff` impostati su Render. Backend 202 test, frontend 151 con il 98% di copertura |
| 11/10 | Tempi di Render misurati | Al risveglio il backend gratuito risponde in circa 3 minuti: D82 e la fase 8 aggiornate |

### Giorno 2 in dettaglio

| Card | Cosa c'è | Verifica |
|---|---|---|
| T07 | `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler`: validazione, 404, 409, 401/403 di Spring Security, errori imprevisti senza dettagli | 14 test sul gestore degli errori |
| T08 | Entità di catalogo ed eventi, `GameRules`, caricamento dei JSON all'avvio con lettura rigorosa | Due avvii non duplicano nulla; le modifiche dell'admin restano; un file sbagliato non cambia nulla |
| T09 | `GET /api/game/**` (una query per endpoint), sicurezza di base, CORS validato | 64 test del backend in totale |
| T10 | Dockerfile in due fasi; database, backend e sito statico su Render (Frankfurt, piani gratuiti) | Online: catalogo, Swagger in https, HSTS, CORS, 401 |
| T11 | `api/client.ts`, `catalogSlice`, `LoadingState`/`ErrorState`, tema scuro con contrasti AA | 27 test del frontend (97% di copertura); i personaggi arrivano dal backend online |

## Dove sta ogni cosa

| Cosa | Dove |
|---|---|
| App online | https://resonance-planner.onrender.com |
| API e Swagger online | https://resonance-planner-api.onrender.com/swagger-ui.html |
| Repository | https://github.com/CristianNunezJorge/resonance-planner |
| Comandi, versioni, convenzioni | `CLAUDE.md` nella cartella principale |
| Indice della progettazione | `docs/README.md` |
| Registro delle decisioni | `docs/decisioni.md` (D1-D103) |
| Documenti di fase | `docs/01-scope.md` ... `docs/09-task.md` |
| Dati di gioco | `backend/src/main/resources/data/*.json` |
| Schema del database | `backend/src/main/resources/db/migration/` |
| Board Trello | https://trello.com/b/JCkNlahy/capstone |
| Proposta per lo staff | https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF |

## Il progetto in breve

- **Demo con dataset curato** (D9): sei personaggi reali (Jinhsi, Yinlin, Changli, Lingyang, Danjin, Aalto, D17), tre eventi reali (Bountiful Crescendo, Lament Recon: Solaris Soldier, By Moon's Grace), calcolo dei materiali fino al livello 40 (D27).
- **Esperienza:** registrazione, configurazione guidata in tre passi, calendario con eventi e Waveplate, chat con l'agente che propone un piano da confermare.
- **Rilasci:** v1 core entro il giorno 6, v2 agente entro il giorno 10, poi margine, extra e rifinitura (D8, D90).
- **Agente:** Claude Haiku 4.5, cinque strumenti limitati all'utente del token, pianificatore deterministico in Java, piano base senza IA come riserva (D54-D64).

## Questioni aperte

1. **Revisione dei dati da parte dell'utente.** Le card P01-P05 sono in "In verifica". Da guardare soprattutto le stime (drop dei nemici comuni, specialità, durate) e le date di partenza degli eventi.
2. **Database di Render: scade il 10/11/2026.** Va ricreato prima, o comunque a meno di 30 giorni dal Demo Day (D82).
3. **Rifiniture facoltative su Render:** limitare l'accesso di Render su GitHub al solo repository `resonance-planner`; valutare se chiudere gli accessi esterni al database.
4. **L'allegato tecnico della proposta è in parte superato:** cita Vercel/Netlify e un limite di livello diverso. Le 14 righe restano valide.
5. **Strumento di analisi statica della settimana 16:** l'utente non ha ancora indicato quale avete usato. Per ora basta il formattatore dell'IDE più ESLint (D80).
6. **Nome definitivo del progetto:** "Resonance Planner" è provvisorio.

## Promemoria per le prossime card

Emersi dalle revisioni del codice. Non sono difetti di oggi, ma vanno fatti nella card indicata.

| Card | Cosa ricordare |
|---|---|
| T12 | Il filtro JWT si crea dentro `SecurityConfig`, non come `@Component`, altrimenti Spring Boot lo registra due volte. Con un token scaduto o non valido il filtro lascia passare senza utente, così il catalogo pubblico resta usabile; il 401 lo dà già `ApiSecurityErrorHandler`. Togliere l'`InMemoryUserDetailsManager` vuoto. `JWT_SECRET` su Render ha 32 caratteri: la chiave HMAC va presa dai suoi byte UTF-8 (256 bit), non decodificata da base64. Account admin creato all'avvio da `ADMIN_EMAIL`/`ADMIN_PASSWORD` (D19, D39). Test: un token USER su `/api/admin/**` riceve 403 |
| T17-T27 (frontend) | Nei percorsi del frontend gli identificativi vanno codificati con `encodeURIComponent` prima di aggiungere il token (`apiRequest` accetta solo percorsi che iniziano con una sola `/`). Ogni pagina nuova chiama `usePageTitle`; le notifiche si mostrano con `toastShown`; i moduli usano `FormField` e la gestione degli errori di `features/auth/formFailure.ts` |
| T13 | Il filtro dei limiti (D48) lavora prima dei controller: deve scrivere da solo la risposta 429 nel formato `ApiResponse`. L'indirizzo del client arriva dagli header del proxy di Render (`forward-headers-strategy: native`, già attivo). Dalla revisione di T12: (1) controllare una volta su Render cosa restituisce `getRemoteAddr()`, perché se è l'indirizzo del proxy tutti condividono lo stesso contatore (anche i docenti al Demo Day, con 10 tentativi ogni 15 minuti); (2) non leggere `X-Forwarded-For` a mano; (3) registrazione e login hanno contatori separati, perché la registrazione fa anche da test per scoprire le email già usate; (4) contare anche le email sconosciute e non fare un contatore per email, altrimenti la differenza tra 429 e 401 rivela quali account esistono; (5) le mappe dei contatori hanno una dimensione massima e tolgono le voci vecchie (le chiavi le sceglie chi attacca; il server ha 512 MB); (6) il filtro si aggiunge con `addFilterBefore(..., JwtAuthenticationFilter.class)` e non è un `@Component`; scrive da solo il 429 con l'header `Retry-After` |
| T17-T20, T25, T30 | Il token di un utente eliminato resta valido fino alla scadenza (D45): ogni service che scrive dati dell'utente deve prima controllare che esista e, se non c'è più, lanciare `UnauthorizedException` (401), come fa `UserService`; altrimenti la chiave esterna dà un 500 (D102). Gli errori su un campo scoperti dal service si lanciano con `FieldValidationException` (400 con `fields`). Nei test, dopo un'operazione che cancella righe, usare una query o `clearAutomatically`: dentro la transazione del test il contesto di persistenza nasconde la cancellazione |
| T34-T36 | Se si usano risposte asincrone (flusso SSE, `Callable`, `DeferredResult`), il `JwtAuthenticationFilter` deve girare anche sul ridispatch asincrono (`shouldNotFilterAsyncDispatch()` che restituisce `false`), altrimenti la richiesta perde l'utente e riceve 401 |
| Ricreare il database (entro il 10/11) | Cambiare anche `JWT_SECRET` su Render e farlo generare di nuovo: gli identificativi ripartono da 1 e un vecchio token ancora valido indicherebbe un altro utente (D100). Nessuno deve registrarsi prima della creazione dell'admin: `admin` e `ADMIN_EMAIL` sono comunque riservati |
| T19 | Fatta il 14/10 (D108). Per T30 e T22: chi somma quantità all'inventario usa `long` o `Math.addExact` (due valori al massimo stanno in un `int`, tre no) e deve scrivere con `InventoryItemRepository.saveQuantity`, che NON svuota il contesto di persistenza: un `InventoryItem` già letto nella stessa transazione resta com'era, quindi rileggere l'inventario in una nuova transazione. Anche `OwnedCharacterRepository` e `InventoryItemRepository` estendono `Repository` (non `JpaRepository`): ogni metodo nuovo deve prendere l'utente. Stessa finestra residua di T18 (utente eliminato nei millisecondi tra controllo e scrittura: 500 invece di 401) |
| T18 | Fatta il 14/10 (D104). `FieldValidationException` accetta più campi (mappa che conserva l'ordine) e `UniqueViolation.isIn` riconosce la corsa tra due richieste uguali: da riusare in T19 e T20. `RosterRules.problems(Progress)` restituisce i campi sbagliati senza lanciare: T20 la chiama per ogni personaggio e aggiunge il prefisso `characters[i].` ai nomi dei campi. Non usare `findById` e `deleteById` di `OwnedCharacterRepository`, che non hanno l'utente. Finestra residua: se l'account viene eliminato nei millisecondi tra il controllo e la scrittura, la chiave esterna dà 500 invece di 401 (innocua, ma nota). `GameRules` non controlla che `ascensionLevels` sia crescente e finisca a `maxLevel`: con i valori attuali non serve |
| T17 | Fatta il 14/10 (D105). Il fuso `Europe/Rome` (D41) non serve al contatore, che lavora su istanti UTC: va aggiunto in T22-T23, quando serve "oggi". Se l'avviso "Mockito is currently self-attaching" disturba, aggiungere l'agente di Mockito all'`argLine` di Surefire |
| T23 | Con più eventi e le loro collezioni, aggiungere `hibernate.default_batch_fetch_size` invece di molti `JOIN FETCH` |
| T24 | `CharacterAvatar` con le iniziali come ripiego sostituisce l'immagine semplice della scheda di prova. Quando lo stato Redux contiene dati dell'utente (roster, inventario), `sessionEnded` deve azzerarli (D103) |
| T26 | FullCalendar potrebbe inserire gli stili con un `<style>` creato da JavaScript, che la CSP (`style-src 'self'`) blocca: provare la build di produzione con la CSP (`npm run preview`) e, se serve, allargare `style-src` e aggiornare D103 |
| T27 | Un evento demo cancellato dall'admin ricompare al riavvio (D39 inserisce gli eventi mancanti). Se si tolgono attività, le posizioni restano con dei buchi. Gli endpoint `/api/admin/**` (T23) devono controllare che l'utente del token esista e sia ancora ADMIN (es. `UserService.requireAdmin`), perché il token di un admin eliminato resta valido 24 ore (D102); con un test che manda il token di un utente eliminato a ogni percorso `/api/me` e `/api/admin` |
| T30 | Quando si sostituisce la proposta in attesa, fare `flush()` dopo aver segnato la vecchia come `REPLACED`: Hibernate esegue gli INSERT prima degli UPDATE e l'indice di D37 scatterebbe |
| X04 | In GitHub Actions i test hanno bisogno di `DATABASE_USERNAME` e `DATABASE_PASSWORD` come variabili d'ambiente |

## Prossimi passi

1. Giorno 4: card T17-T21 di `docs/09-task.md` (Waveplate e Crystals, roster, inventario, configurazione guidata in backend e frontend).
2. L'utente rivede i dati; poi P01-P05 passano in "Fatto".

## Come riprendere in una nuova sessione

Claude Code legge da solo `CLAUDE.md` e la sua memoria persistente. Per partire basta scrivere, per esempio:

> Leggi `HANDOFF.md`, `CLAUDE.md` e `docs/stato-del-progetto.md`, poi iniziamo il giorno 4 (card T17-T21). Prima di scrivere codice riassumimi il piano e chiedimi conferma.

Prima di avviare backend e test: Docker Desktop acceso e `docker compose up -d`. Per Render serve Chrome con l'estensione Claude in Chrome e la sessione di Render aperta.

## Lezioni pratiche dalle sessioni

- Il wiki Fandom va letto con il browser integrato, perché blocca il recupero automatico delle pagine.
- Alcuni dati non sono sul wiki (per esempio le probabilità di drop): in quei casi si stima e si segna `estimated: true` (D25).
- Trello: per scrivere nelle card serve il focus verificato sull'editor. In caso contrario i tasti diventano scorciatoie.
- Le operazioni lunghe nel browser vanno spezzate in gruppi di circa un minuto, perché gli strumenti hanno un tempo massimo di risposta.
- In SQL un `CHECK` che vale NULL viene considerato superato: ogni regola deve controllare anche `IS NOT NULL`.
- Se si modifica in locale una migrazione già applicata (solo prima di unirla a `main`), Flyway rifiuta il checksum: si ricreano i database con `docker compose down -v`.
- Una revisione del codice prima di ogni pull request ha trovato problemi veri in quasi tutte le card: vale la pena farla sempre.
- I filtri di Spring Security lavorano prima dei controller: i loro errori non passano dal `@RestControllerAdvice`.
- Provare l'immagine Docker in locale con i limiti di Render (`--memory=512m --cpus=0.1`) ha mostrato prima del deploy il rischio di memoria e l'avvio lento.
- Le intestazioni di sicurezza del sito statico (CSP, `nosniff`) si impostano in Render > sito > Headers e non stanno nel repository: se il sito viene ricreato vanno rifatte. Il pulsante "Add Rule" della pagina Headers va premuto con un clic alle coordinate, non con il riferimento dell'elemento.
- Su Render il piano preselezionato per un nuovo servizio web è a pagamento: va scelto Free a mano.
