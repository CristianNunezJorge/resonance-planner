# Resonance Planner

Planner per **Wuthering Waves**: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma.

Progetto capstone del Master Full-Stack AI Developer di Epicode. "Resonance Planner" è un nome provvisorio.

> **Stato:** in sviluppo. Oggi online ci sono il catalogo dei personaggi e le API pubbliche; screenshot e funzionalità complete arriveranno con i prossimi rilasci.

## Demo online

| Cosa | Indirizzo |
|---|---|
| Applicazione | https://resonance-planner.onrender.com |
| Documentazione delle API (Swagger) | https://resonance-planner-api.onrender.com/swagger-ui.html |

Il backend usa il piano gratuito di Render: dopo 15 minuti senza richieste si spegne, e la prima richiesta successiva può richiedere fino a tre minuti. L'applicazione lo segnala con un messaggio durante il caricamento.

## Cosa fa

- **Calendario** con gli eventi a tempo, attivi giorno per giorno, e le attività pianificate.
- **Waveplate:** contatore con l'orario in cui saranno piene.
- **Obiettivi dei personaggi:** calcolo dei materiali mancanti fino al livello 40 e delle run necessarie.
- **Agente IA:** propone un piano giorno per giorno. I calcoli li fa il backend in Java, l'agente sceglie i parametri e spiega. L'utente conferma sempre prima che il piano entri nel calendario.
- **Piano base** senza IA, sempre disponibile.

È una demo con un dataset curato: sei personaggi e tre eventi reali del gioco.

## Stack

| Parte | Tecnologie | Versioni |
|---|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS | Node 24, React 19.3, Vite 8.3, TypeScript 6.0, Redux Toolkit 2.13, React Router 8.4, Bootstrap 5.3 |
| Backend | Java, Spring Boot, Maven, Spring Data JPA, Spring Security + JWT | Java 21, Spring Boot 4.1.1, Maven 3.10 (wrapper incluso) |
| Database | PostgreSQL, schema gestito con Flyway | PostgreSQL 16 |
| IA | API Claude con l'SDK Java ufficiale | — |
| Test | JUnit 5, Mockito, JaCoCo, Vitest, React Testing Library | — |
| Deploy | Render (frontend, backend e database) | — |

## Struttura del repository

```
backend/    API Spring Boot e dati di gioco in JSON
frontend/   applicazione React
docs/       progettazione: scope, flussi, modello dati, API, agente, schermate, regole, task
```

La progettazione completa, con il registro di tutte le decisioni, è in [`docs/`](docs/README.md).

## Variabili d'ambiente

L'elenco è in [`.env.example`](.env.example): si copia in `.env` e si completano i valori. Il file `.env` non entra mai nel repository. Se manca una variabile obbligatoria, il backend non parte e il messaggio indica quale.

## Avvio in locale

Servono Docker Desktop, JDK 21 e Node.js 24.

```bash
# 1. Database PostgreSQL sulla porta 5433, con un database di sviluppo e uno di test
docker compose up -d

# 2. Backend su http://localhost:8080 (dalla cartella backend/)
./mvnw spring-boot:run

# 3. Frontend su http://localhost:5173 (dalla cartella frontend/)
npm install
npm run dev
```

Al primo avvio Flyway crea lo schema del database.

## Test e controlli

Da lanciare prima di ogni pull request:

```bash
# Backend: test unitari e di integrazione, report di copertura in target/site/jacoco/
./mvnw verify

# Frontend: lint, controllo dei tipi, test e build
npm run lint && npm run typecheck && npm test && npm run build
```

I test di integrazione del backend usano il database di test di Docker Compose, che deve essere avviato.

## Crediti

- Wuthering Waves, i suoi nomi e le sue immagini appartengono a **Kuro Games**. Progetto didattico non commerciale.
- Dati di gioco dal [Wuthering Waves Wiki](https://wutheringwaves.fandom.com) su Fandom, con licenza [CC BY-SA](https://www.fandom.com/licensing). Fonti e stime sono elencate in [`backend/src/main/resources/data/README.md`](backend/src/main/resources/data/README.md).

## In English

Resonance Planner is a planner for the game Wuthering Waves, built as the capstone project of the Epicode Full-Stack AI Developer master. It combines time-limited events, the Waveplate stamina system and character goals in one calendar. An AI agent based on the Claude API proposes a day-by-day plan: the Java backend does all the calculations, the model picks the parameters and explains the plan, and the user always confirms it. Stack: React + TypeScript, Spring Boot, PostgreSQL. This project is a work in progress.
