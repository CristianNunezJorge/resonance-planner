# Stato del progetto

Riepilogo delle sessioni dal 30/09 al 10/10/2026, per riprendere il lavoro in una nuova sessione.

## In una frase

Il progetto è approvato e il giorno 1 di sviluppo è completato (T01-T06): repository, database in Docker, backend, schema e frontend funzionano in locale. Il prossimo passo è il giorno 2, con il primo deploy su Render.

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
| 10/10 | **Giorno 1: T01-T06** | Repository pubblico, Docker Compose, backend Spring Boot 4.1.1, schema Flyway, frontend Vite; 5 commit su `main`, uno per card, tramite pull request |
| 10/10 | Decisione nata dalla revisione dello schema | D98: colonne `integer` invece di `smallint`, medie dei drop con al massimo due decimali |

### Giorno 1 in dettaglio

| Card | Cosa c'è | Verifica |
|---|---|---|
| T01 | `.gitignore`, `.gitattributes` (LF per `mvnw` e script), `.env.example`, README | Repository pubblico su GitHub |
| T05 | Vite + React + TS, Redux Toolkit con `toastSlice`, React Router con home, 404 e pagina di errore, Bootstrap via SASS, Vitest | Lint, tipi, 5 test (copertura 83%), build |
| T02 | PostgreSQL 16 sulla porta 5433, volume persistente, database di test creato al primo avvio | Container *healthy*, due database |
| T03 | Spring Boot 4.1.1, profili `dev`/`test`/`prod`, `AppProperties` validato, `DatabaseConfig`, springdoc, JaCoCo | Senza una variabile obbligatoria l'avvio si ferma e la nomina |
| T04 | `V1__schema.sql`: 16 tabelle, controlli, chiavi esterne, indice parziale di D37 | 24 test del backend, 8 sullo schema |
| T06 | `CLAUDE.md` con comandi e versioni, README con avvio e controlli, questo documento | — |

## Dove sta ogni cosa

| Cosa | Dove |
|---|---|
| Repository | https://github.com/CristianNunezJorge/resonance-planner |
| Comandi, versioni, convenzioni | `CLAUDE.md` nella cartella principale |
| Indice della progettazione | `docs/README.md` |
| Registro delle decisioni | `docs/decisioni.md` (D1-D98) |
| Documenti di fase | `docs/01-scope.md` ... `docs/09-task.md` |
| Dati di gioco | `backend/src/main/resources/data/*.json` |
| Fonti, ipotesi e stime dei dati | `backend/src/main/resources/data/README.md` |
| Schema del database | `backend/src/main/resources/db/migration/` |
| Icone dei personaggi | `frontend/public/characters/*.webp` |
| Board Trello | https://trello.com/b/JCkNlahy/capstone |
| Proposta per lo staff | https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF |

## Il progetto in breve

- **Demo con dataset curato** (D9): sei personaggi reali (Jinhsi, Yinlin, Changli, Lingyang, Danjin, Aalto, D17), tre eventi reali (Bountiful Crescendo, Lament Recon: Solaris Soldier, By Moon's Grace), calcolo dei materiali fino al livello 40 (D27).
- **Esperienza:** registrazione, configurazione guidata in tre passi, calendario con eventi e Waveplate, chat con l'agente che propone un piano da confermare.
- **Rilasci:** v1 core entro il giorno 6, v2 agente entro il giorno 10, poi margine, extra e rifinitura (D8, D90).
- **Agente:** Claude Haiku 4.5, cinque strumenti limitati all'utente del token, pianificatore deterministico in Java, piano base senza IA come riserva (D54-D64).

## Questioni aperte

1. **Revisione dei dati da parte dell'utente.** Le card P01-P05 sono in "In verifica". Da guardare soprattutto le stime (drop dei nemici comuni, specialità, durate) e le date di partenza degli eventi.
2. **L'allegato tecnico della proposta è in parte superato:** cita Vercel/Netlify e un limite di livello diverso. Le 14 righe restano valide.
3. **Strumento di analisi statica della settimana 16:** l'utente non ha ancora indicato quale avete usato. Per ora basta il formattatore dell'IDE più ESLint (D80).
4. **Nome definitivo del progetto:** "Resonance Planner" è provvisorio.

## Promemoria per le prossime card

Emersi dalle revisioni del codice del giorno 1. Non sono difetti di oggi, ma vanno fatti nella card indicata.

| Card | Cosa ricordare |
|---|---|
| T07-T10 | Spring Security oggi blocca tutto tranne `/actuator/health` e stampa nel log una password generata. Prima del deploy serve una configurazione minima che apra `/api/game/**` (D49), Swagger (D53) e `/actuator/health`, e che tolga l'utente generato |
| T08 e seguenti | Entità con `GenerationType.IDENTITY`, `Integer` per le colonne intere (D98), `BigDecimal` per `numeric`, enum con `@Enumerated(EnumType.STRING)` (D43) |
| T10 | Nel Dockerfile `SPRING_PROFILES_ACTIVE=prod` e immagine `eclipse-temurin:21`. In `application-prod.yml`, `server.forward-headers-strategy: framework`, perché Swagger funzioni in HTTPS dietro Render. Sul sito statico, la regola di riscrittura `/* → /index.html` e la versione di Node |
| T11 | `src/vite-env.d.ts` con il tipo di `VITE_API_BASE_URL`. Tema scuro: con Bootstrap 5.3 le variabili si cambiano con `@import` dei file di Bootstrap, non con `@use ... with`, oppure con `data-bs-theme="dark"` |
| T16 | Sostituire la favicon di Vite |
| T17 | Fuso orario `Europe/Rome` in configurazione (D41). Se l'avviso "Mockito is currently self-attaching" disturba, aggiungere l'agente di Mockito all'`argLine` di Surefire |
| T30 | Quando si sostituisce la proposta in attesa, fare `flush()` dopo aver segnato la vecchia come `REPLACED`: Hibernate esegue gli INSERT prima degli UPDATE e l'indice di D37 scatterebbe |
| X04 | In GitHub Actions i test hanno bisogno di `DATABASE_USERNAME` e `DATABASE_PASSWORD` come variabili d'ambiente |

## Prossimi passi

1. Giorno 2: card T07-T11 di `docs/09-task.md`, con il primo deploy online (D89).
2. Il database gratuito di Render scade dopo 30 giorni: crearlo o ricrearlo a meno di 30 giorni dal Demo Day e segnarsi la data.
3. L'utente rivede i dati; poi P01-P05 passano in "Fatto".

## Come riprendere in una nuova sessione

Claude Code legge da solo `CLAUDE.md` e la sua memoria persistente. Per partire basta scrivere, per esempio:

> Leggi `CLAUDE.md` e `docs/stato-del-progetto.md`, poi iniziamo il giorno 2 (card T07-T11). Prima di scrivere codice riassumimi il piano e chiedimi conferma.

Prima di avviare backend e test: Docker Desktop acceso e `docker compose up -d`.

## Lezioni pratiche dalle sessioni

- Il wiki Fandom va letto con il browser integrato, perché blocca il recupero automatico delle pagine.
- Alcuni dati non sono sul wiki (per esempio le probabilità di drop): in quei casi si stima e si segna `estimated: true` (D25).
- Trello: per scrivere nelle card serve il focus verificato sull'editor. In caso contrario i tasti diventano scorciatoie.
- Le operazioni lunghe nel browser vanno spezzate in gruppi di circa un minuto, perché gli strumenti hanno un tempo massimo di risposta.
- In SQL un `CHECK` che vale NULL viene considerato superato: ogni regola deve controllare anche `IS NOT NULL`.
- Se si modifica in locale una migrazione già applicata (solo prima di unirla a `main`), Flyway rifiuta il checksum: si ricreano i database con `docker compose down -v`.
- Una revisione del codice prima di ogni pull request ha trovato problemi veri in tutte e tre le card di codice: vale la pena farla sempre.
