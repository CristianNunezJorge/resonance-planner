# Resonance Planner

Planner per **Wuthering Waves**: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma.

Progetto capstone del Master Full-Stack AI Developer di Epicode. "Resonance Planner" è un nome provvisorio.

> **Stato:** in sviluppo. Questo README viene completato durante il progetto: link alla demo online, screenshot, avvio e test arriveranno con i prossimi rilasci.

## Cosa fa

- **Calendario** con gli eventi a tempo, attivi giorno per giorno, e le attività pianificate.
- **Waveplate:** contatore con l'orario in cui saranno piene.
- **Obiettivi dei personaggi:** calcolo dei materiali mancanti fino al livello 40 e delle run necessarie.
- **Agente IA:** propone un piano giorno per giorno. I calcoli li fa il backend in Java, l'agente sceglie i parametri e spiega. L'utente conferma sempre prima che il piano entri nel calendario.
- **Piano base** senza IA, sempre disponibile.

È una demo con un dataset curato: sei personaggi e tre eventi reali del gioco.

## Stack

| Parte | Tecnologie |
|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS |
| Backend | Java 21, Spring Boot, Maven, Spring Data JPA, Spring Security + JWT |
| Database | PostgreSQL, schema gestito con Flyway |
| IA | API Claude con l'SDK Java ufficiale |
| Test | JUnit 5, Mockito, Vitest, React Testing Library |
| Deploy | Render (frontend, backend e database) |

## Struttura del repository

```
backend/    API Spring Boot e dati di gioco in JSON
frontend/   applicazione React
docs/       progettazione: scope, flussi, modello dati, API, agente, schermate, regole, task
```

La progettazione completa, con il registro di tutte le decisioni, è in [`docs/`](docs/README.md).

## Variabili d'ambiente

L'elenco è in [`.env.example`](.env.example): si copia in `.env` e si completano i valori. Il file `.env` non entra mai nel repository.

## Crediti

- Wuthering Waves, i suoi nomi e le sue immagini appartengono a **Kuro Games**. Progetto didattico non commerciale.
- Dati di gioco dal [Wuthering Waves Wiki](https://wutheringwaves.fandom.com) su Fandom, con licenza [CC BY-SA](https://www.fandom.com/licensing). Fonti e stime sono elencate in [`backend/src/main/resources/data/README.md`](backend/src/main/resources/data/README.md).

## In English

Resonance Planner is a planner for the game Wuthering Waves, built as the capstone project of the Epicode Full-Stack AI Developer master. It combines time-limited events, the Waveplate stamina system and character goals in one calendar. An AI agent based on the Claude API proposes a day-by-day plan: the Java backend does all the calculations, the model picks the parameters and explains the plan, and the user always confirms it. Stack: React + TypeScript, Spring Boot, PostgreSQL. This project is a work in progress.
