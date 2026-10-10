# Resonance Planner · Capstone Project

Capstone del Master Full-Stack AI Developer (Epicode), progetto individuale di due settimane. È una **demo** di planner per Wuthering Waves: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma. "Resonance Planner" è un nome provvisorio.

## Stato attuale (10/10/2026)

- **Progetto approvato dallo staff.** Progettazione completa: 9 fasi e 98 decisioni (D1-D98).
- **Giorno 1 completato (T01-T06):** repository pubblico, Docker Compose, backend Spring Boot con controllo delle variabili, schema Flyway con 16 tabelle, frontend Vite con router, store e test.
- **Prossimo passo:** giorno 2, card T07-T11 di `docs/09-task.md` (errori e `ApiResponse`, catalogo, API pubbliche, primo deploy su Render, client HTTP e tema). Prima leggere i promemoria in `docs/stato-del-progetto.md`.

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
- **Flusso di ogni card:** un ramo, test prima del codice per i calcoli, controlli locali, revisione del codice, poi l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card: `feat: ... (T07)`). Poi push, pull request, *squash merge* e cancellazione del ramo. Nessuna firma o co-autore di Claude nei commit e nelle pull request.

## Comandi

Prerequisiti: Docker Desktop avviato, JDK 21, Node 24. Il `.env` nella cartella principale (copiato da `.env.example`) serve a Docker Compose, al backend in `dev` e ai test.

```bash
# Database: PostgreSQL 16 sulla porta 5433, con i database resonance_planner e resonance_planner_test
docker compose up -d
docker compose down        # ferma il container, i dati restano
docker compose down -v     # cancella i database locali: al riavvio Flyway ricrea lo schema

# Backend (dalla cartella backend/)
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"   # in Git Bash
./mvnw spring-boot:run     # profilo dev, http://localhost:8080
./mvnw test                # test unitari e di integrazione (serve il database)
./mvnw verify              # test + copertura in target/site/jacoco/index.html
./mvnw package             # jar in target/

# Frontend (dalla cartella frontend/)
npm install
npm run dev                # http://localhost:5173
npm run lint
npm run typecheck
npm test
npm run coverage
npm run build
```

Prima di ogni pull request: `./mvnw verify` nel backend; `npm run lint`, `npm run typecheck`, `npm test` e `npm run build` nel frontend (D81).

## Stack e versioni (D77)

| Parte | Scelta | Versioni fissate il 10/10/2026 | Decisioni |
|---|---|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS, FullCalendar | Node 24.21 (minimo 24.15), React 19.3, Vite 8.3, TypeScript 6.0, Redux Toolkit 2.13, React Router 8.4, Bootstrap 5.3.8, Sass 1.105 | D7, D76 |
| Backend | Java, Spring Boot, Maven, Spring Data JPA, Spring Security + JWT, springdoc | Java 21 (Temurin 21.0.12.1), Spring Boot 4.1.1, Maven 3.10 (wrapper), springdoc 3.1.1 | D5, D45, D77 |
| Database | PostgreSQL, schema con Flyway (Hibernate in `validate`) | PostgreSQL 16 (Docker `postgres:16`) | D38, D98 |
| IA | API Claude con l'SDK Java ufficiale, modello `claude-haiku-4-5`, ciclo tool-use scritto da noi | — | D54, D59 |
| Test | JUnit 5 + Mockito, integrazione su PostgreSQL di Docker Compose, Vitest + React Testing Library, checklist manuale | JaCoCo 0.8.15, Vitest 5.0 | D79 |
| Deploy | Tutto su Render: frontend, backend e database PostgreSQL (il database gratuito scade dopo 30 giorni e va ricreato) | — | D82 |

Principi da non perdere:
- I calcoli li fa Java; l'IA sceglie i parametri e spiega (D3).
- Il modello non scrive mai le attività: la proposta è sempre l'ultima bozza del pianificatore (D56).
- L'agente propone, l'utente conferma (D2). Esiste un "piano base" senza IA (D64).
- Dati personali solo sotto `/api/me`; l'utente si ricava dal token (D44).
- Eventi attivi giorno per giorno (D96), effetti con limite giornaliero (D94), eventi della demo ricorrenti (D29).
- Calcolo dei materiali fino al livello 40, fase SOL3 4 (D27, D92).

## Struttura del progetto

La struttura completa prevista è in `docs/08-regole-di-lavoro.md`. Oggi esiste:

```
.env.example                  variabili d'ambiente, senza valori
docker-compose.yml            PostgreSQL locale; docker/postgres/init/ crea il database di test
backend/
  pom.xml, mvnw               Spring Boot 4.1.1, Maven wrapper
  src/main/java/com/resonanceplanner/
    common/config/            AppProperties (variabili obbligatorie, D83), DatabaseConfig
  src/main/resources/
    application.yml           comune; application-dev.yml legge ../.env; application-prod.yml per Render
    db/migration/V1__schema.sql
    data/                     6 JSON di gioco + README con fonti e stime
  src/test/resources/application-test.yml   database di test, valori finti
frontend/
  src/app/                    store, hook tipizzati, rotte, layout, toastSlice
  src/features/landing/       pagina di presentazione (segnaposto fino a T16)
  src/components/             pagine 404 ed errore
  src/styles/main.scss        Bootstrap
  public/characters/          6 icone WebP 256×256
docs/                         progettazione (fasi 01-09, decisioni, stato)
```

## Convenzioni

- Codice, nomi e commit in inglese; documentazione in italiano.
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, ...), un ramo per attività, `main` sempre funzionante, almeno un push al giorno.
- Test prima del codice per tutti i calcoli; copertura minima dell'80% su service e logica.
- Nessun segreto nel codice: solo variabili d'ambiente (elenco in `docs/08-regole-di-lavoro.md`). Nuove variabili obbligatorie vanno in `AppProperties`, con un messaggio che nomina la variabile.
- Schema: una migrazione già unita a `main` non si modifica mai, se ne crea una nuova (`V2__...`). Le colonne intere sono `integer` (D98) e ogni `CHECK` controlla anche `IS NOT NULL`, perché un controllo che vale NULL viene considerato superato.
- Definizione di "fatto" in 6 punti: `docs/08-regole-di-lavoro.md`.

## Link utili

- Repository: https://github.com/CristianNunezJorge/resonance-planner
- Board Trello: https://trello.com/b/JCkNlahy/capstone
- Proposta per lo staff (artifact): https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF

## Note operative

- **Su questo computer:** il `JAVA_HOME` globale punta al JDK 25, quindi nei comandi va indicato il JDK 21. Il PostgreSQL 18 installato occupa la porta 5432, perciò Docker usa la 5433.
- **Spring Boot 4:** gli starter hanno nomi nuovi (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`); nei test si usa `@MockitoBean` al posto di `@MockBean`; `DataSourceBuilder` è in `org.springframework.boot.jdbc`.
- **React Router 8:** `RouterProvider` si importa da `react-router/dom`, tutto il resto da `react-router`. Il pacchetto `react-router-dom` non esiste più.
- Il wiki Fandom di Wuthering Waves blocca WebFetch (HTTP 402): leggerlo con il browser integrato. I contenuti sono CC BY-SA, quindi la fonte va citata.
- Trello: il testo digitato in una card aperta può diventare una scorciatoia da tastiera. Le descrizioni si scrivono via JavaScript con `execCommand`, dopo aver verificato il focus sull'editor. Le etichette si possono applicare con i tasti 1-0 sulla card, uno alla volta e con una pausa. Il riepilogo `/b/JCkNlahy.json` può essere in ritardo di qualche secondo.
