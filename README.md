# Resonance Planner

Planner per **Wuthering Waves**: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma.

Progetto capstone del Master Full-Stack AI Developer di Epicode. "Resonance Planner" è un nome provvisorio.

> **Stato:** la **v1** è online. Comprende calendario con eventi e attività, Waveplate, roster e inventario con il calcolo dei materiali, profilo e gestione degli eventi per l'admin. L'agente IA arriva con la v2.

## Demo online

| Cosa | Indirizzo |
|---|---|
| Applicazione | https://resonance-planner.onrender.com |
| Documentazione delle API (Swagger) | https://resonance-planner-api.onrender.com/swagger-ui.html |

Il backend usa il piano gratuito di Render: dopo 15 minuti senza richieste si spegne, e la prima richiesta successiva può richiedere fino a tre minuti. L'applicazione lo segnala con un messaggio durante il caricamento.

## Cosa fa la v1

- **Configurazione guidata** in tre passi: personaggi da far crescere, livelli e obiettivi, Waveplate e tempo di gioco.
- **Calendario:**
  - vista a settimana o a mese, e su telefono un elenco per giorno;
  - gli eventi a tempo compaiono solo nei giorni in cui sono davvero attivi;
  - attività manuali con titolo, giorno, durata e note;
  - finestre di dettaglio per eventi e attività, e "Segna come fatta".
- **Waveplate:** il valore attuale lo calcola il server, con l'orario in cui saranno piene; si aggiorna da solo ogni minuto.
- **Roster e inventario:** per ogni personaggio i materiali mancanti; per tutto il roster le run da fare, le Waveplate, il tempo e i giorni stimati.
- **Attività di farming segnate come fatte:** aggiungono all'inventario le ricompense medie. Le ricompense doppie di un evento contano solo entro il suo limite giornaliero.
- **Profilo:** username, tempo di gioco, uscita ed eliminazione dell'account con conferma.
- **Area admin:** creazione, modifica ed eliminazione degli eventi.

È una demo con un dataset curato: sei personaggi e tre eventi reali del gioco, calcolo dei materiali fino al livello 40.

**In arrivo con la v2:**
- un agente IA, con un modello gratuito tramite OpenRouter, che propone un piano giorno per giorno;
- un piano base, sempre disponibile, che funziona anche senza IA;
- l'anteprima della proposta nel calendario, da confermare o scartare.

## Come provarla

1. Apri l'applicazione e scegli **"Prova la demo"** per registrarti.
2. Nella configurazione guidata scegli uno o più personaggi, i loro livelli e quanto giochi al giorno.
3. Nel **calendario** guarda gli eventi della settimana e aggiungi un'attività con **"+ Attività"**.
4. Nel **roster** guarda i materiali mancanti; nella scheda **"Inventario"** scrivi quanti materiali hai e i calcoli si aggiornano.
5. Dal **profilo** puoi eliminare l'account: spariscono anche tutti i tuoi dati.

## Scelte principali

- **I calcoli li fa Java:** Waveplate, materiali mancanti, run e giorni di farming sono calcolati e testati nel backend. L'agente della v2 sceglierà i parametri e spiegherà il piano, ma non farà conti.
- **L'agente propone, l'utente conferma:** un piano entra nel calendario solo dopo la conferma.
- **Dati personali solo sotto `/api/me`:** l'utente si ricava sempre dal token, mai da un identificativo nell'URL.
- **Sicurezza:**
  - password con BCrypt e token JWT valido 24 ore;
  - ruoli USER e ADMIN, controllati sul server a ogni richiesta;
  - limiti al numero di richieste;
  - una Content-Security-Policy stretta sul sito.
- **Registro delle decisioni:** ogni scelta ha la sua voce in [`docs/decisioni.md`](docs/decisioni.md), con le alternative scartate.

## Stack

| Parte | Tecnologie | Versioni |
|---|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS, FullCalendar | Node 24, React 19.3, Vite 8.3, TypeScript 6.0, Redux Toolkit 2.13, React Router 8.4, Bootstrap 5.3, FullCalendar 7.1 |
| Backend | Java, Spring Boot, Maven, Spring Data JPA, Spring Security + JWT, springdoc | Java 21, Spring Boot 4.1.1, Maven 3.10 (wrapper incluso) |
| Database | PostgreSQL, schema gestito con Flyway | PostgreSQL 16 |
| IA (v2) | OpenRouter con un modello gratuito, chiamato con `RestClient` di Spring (D126) | — |
| Test | JUnit 5, Mockito, JaCoCo, Vitest, React Testing Library | — |
| Deploy | Render (sito statico, backend Docker, database) | — |

## Struttura del repository

```
backend/    API Spring Boot e dati di gioco in JSON
frontend/   applicazione React
docs/       progettazione: scope, flussi, modello dati, API, agente, schermate, regole, task, decisioni
```

La progettazione completa è in [`docs/`](docs/README.md).

## Variabili d'ambiente

L'elenco è in [`.env.example`](.env.example): si copia in `.env` e si completano i valori. Il file `.env` non entra mai nel repository. Se manca una variabile obbligatoria, il backend non parte e il messaggio indica quale.

## Avvio in locale

Servono Docker Desktop, JDK 21 e Node.js 24.

```bash
# 1. Database PostgreSQL sulla porta 5433, con un database di sviluppo e uno di test
docker compose up -d

# 2. Backend su http://localhost:8080 (dalla cartella backend/); JAVA_HOME deve puntare a un JDK 21
./mvnw spring-boot:run

# 3. Frontend su http://localhost:5173 (dalla cartella frontend/)
npm install
npm run dev
```

Al primo avvio Flyway crea lo schema del database, e il backend carica il catalogo, gli eventi della demo e l'account admin (`ADMIN_EMAIL`, `ADMIN_PASSWORD`).

## Test e controlli

Da lanciare prima di ogni pull request:

```bash
# Backend: test unitari e di integrazione, report di copertura in target/site/jacoco/
./mvnw verify

# Frontend: lint, controllo dei tipi, test e build
npm run lint && npm run typecheck && npm test && npm run build
```

Alla v1 i test sono 554 nel backend e 478 nel frontend, con una copertura intorno al 98%. I test di integrazione del backend usano il database di test di Docker Compose, che deve essere avviato.

## Limiti noti

- **Render gratuito:** il backend si sveglia in circa tre minuti, e il database gratuito scade dopo 30 giorni e va ricreato.
- **Fuso orario:** "oggi" è sempre in ora italiana, per tutti gli utenti.
- **Dataset curato:** sei personaggi, tre eventi, livelli fino al 40. Alcune quantità medie sono stime, e l'applicazione lo segnala.
- **Ricompense doppie:** nel calendario si vede il limite giornaliero di un evento, ma non ancora quante ricompense doppie restano quel giorno.

## Crediti

- Wuthering Waves, i suoi nomi e le sue immagini appartengono a **Kuro Games**. Progetto didattico non commerciale.
- Dati di gioco dal [Wuthering Waves Wiki](https://wutheringwaves.fandom.com) su Fandom, con licenza [CC BY-SA](https://www.fandom.com/licensing). Fonti e stime sono elencate in [`backend/src/main/resources/data/README.md`](backend/src/main/resources/data/README.md).

## In English

Resonance Planner is a planner for the game Wuthering Waves, built as the capstone project of the Epicode Full-Stack AI Developer master. Version 1 is online: a calendar that shows time-limited events only on the days they are active, manual tasks, the Waveplate stamina counter, the roster and inventory with the materials still missing for each character, a profile page and an admin area for events. The Java backend does all the calculations. Version 2 adds an AI agent, running on a free model through OpenRouter, that proposes a day-by-day plan, which the user always confirms. Stack: React + TypeScript, Spring Boot, PostgreSQL, deployed on Render.
