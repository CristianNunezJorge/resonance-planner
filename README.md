# Resonance Planner

Planner per **Wuthering Waves**: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma.

Progetto capstone del Master Full-Stack AI Developer di Epicode. "Resonance Planner" è un nome provvisorio.

> **Stato:** la **v2** è online. Alla v1 (calendario con eventi e attività, Waveplate, roster e inventario con il calcolo dei materiali, profilo e gestione degli eventi per l'admin) aggiunge il piano base, le proposte da confermare e un agente IA con la chat.

## Demo online

| Cosa | Indirizzo |
|---|---|
| Applicazione | https://resonance-planner.onrender.com |
| Documentazione delle API (Swagger) | https://resonance-planner-api.onrender.com/swagger-ui.html |

Il backend usa il piano gratuito di Render: dopo 15 minuti senza richieste si spegne, e la prima richiesta successiva può richiedere fino a tre minuti. L'applicazione lo segnala con un messaggio durante il caricamento.

## Cosa fa

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

**Con la v2:**
- **Piano base:** "Genera piano base" crea un piano di 7 giorni con il pianificatore in Java, senza IA. Funziona sempre, anche quando l'agente non è disponibile.
- **Proposte:** un piano arriva come proposta; nel calendario le sue attività sono tratteggiate, e si conferma o si scarta dalla barra della proposta. Ogni proposta nuova sostituisce quella in attesa.
- **Avviso di cambiamento:** se roster, obiettivi o eventi cambiano dopo l'ultimo piano, il calendario lo dice e propone di rifarlo, con il piano base o con l'agente.
- **Agente IA con la chat:**
  - si chiede in italiano, per esempio "Organizzami la settimana", "Non gioco sabato e domenica" o "Cosa faccio oggi?";
  - l'agente legge roster, eventi, Waveplate e calendario con cinque strumenti, chiede il piano al pianificatore in Java e prepara una proposta con la sua spiegazione;
  - sotto la risposta compaiono i passaggi fatti ("Ho calcolato il piano");
  - "Spiegamelo" nel dettaglio di un evento chiede all'agente di spiegarlo;
  - su computer la chat sta accanto al calendario, su telefono e tablet si apre con il pulsante "Agente".

È una demo con un dataset curato: sei personaggi e tre eventi reali del gioco, calcolo dei materiali fino al livello 40.

### L'agente e i suoi limiti

- **Modello gratuito di terzi:** l'agente usa `nvidia/nemotron-3-ultra-550b-a55b:free` tramite [OpenRouter](https://openrouter.ai), un servizio che dà accesso a molti modelli con un'unica API (D126, D132). Il backend accetta solo modelli gratuiti, il cui nome finisce con `:free`. I messaggi e i dati di gioco (roster, obiettivi, calendario) vanno a quel fornitore, che può conservarli: la chat lo dice, e non vanno scritti dati personali.
- **Limiti giornalieri (D129):** il piano gratuito di OpenRouter dà 50 richieste al giorno per tutto l'account, e un messaggio ne usa da 1 a 6. Per questo:
  - ogni utente ha **5 messaggi al giorno** e al massimo 24 chiamate al modello;
  - tutta l'app si ferma a 40 chiamate al giorno;
  - un utente può avere un solo messaggio in corso;
  - il piano base si ferma a 20 proposte al giorno per utente (contano anche quelle dell'agente).

  Quando un limite finisce, la chat lo spiega e offre il piano base.
- **Il modello non fa conti e non scrive il calendario:** le attività della proposta sono sempre quelle calcolate da Java; il modello sceglie i parametri e spiega. Le sue risposte si mostrano come testo semplice, mai come HTML.
- **Verifica:** dieci scenari reali, dalla pianificazione alle domande fuori tema e ai tentativi di far ignorare le istruzioni, sono descritti con i risultati in [`docs/scenari-agente.md`](docs/scenari-agente.md).

## Come provarla

1. Apri l'applicazione e scegli **"Prova la demo"** per registrarti.
2. Nella configurazione guidata scegli uno o più personaggi, i loro livelli e quanto giochi al giorno.
3. Nel **calendario** guarda gli eventi della settimana e aggiungi un'attività con **"+ Attività"**.
4. Nel **roster** guarda i materiali mancanti; nella scheda **"Inventario"** scrivi quanti materiali hai e i calcoli si aggiornano.
5. Nel calendario prova **"Genera piano base"**, oppure scrivi all'agente nella chat, per esempio "Organizzami la settimana"; poi conferma o scarta la proposta.
6. Dal **profilo** puoi eliminare l'account: spariscono anche tutti i tuoi dati.

## Scelte principali

- **I calcoli li fa Java:** Waveplate, materiali mancanti, run e giorni di farming sono calcolati e testati nel backend. L'agente sceglie i parametri e spiega il piano, ma non fa conti.
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
| IA | OpenRouter con un modello gratuito (`nvidia/nemotron-3-ultra-550b-a55b:free`), chiamato con `RestClient` di Spring; ciclo con gli strumenti scritto da noi (D126, D132) | — |
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

Alla v2 i test sono 860 nel backend e 539 nel frontend, con una copertura intorno al 98%. Le chiamate vere al modello non fanno parte dei test automatici: il ciclo dell'agente si prova con un finto client (D63). I test di integrazione del backend usano il database di test di Docker Compose, che deve essere avviato.

## Limiti noti

- **Render gratuito:** il backend si sveglia in circa tre minuti, e il database gratuito scade dopo 30 giorni e va ricreato.
- **Fuso orario:** "oggi" è sempre in ora italiana, per tutti gli utenti.
- **Dataset curato:** sei personaggi, tre eventi, livelli fino al 40. Alcune quantità medie sono stime, e l'applicazione lo segnala.
- **Ricompense doppie:** nel calendario si vede il limite giornaliero di un evento, ma non ancora quante ricompense doppie restano quel giorno.
- **Agente gratuito:**
  - il modello può essere lento (da qualche secondo a un minuto) o non disponibile se il suo unico fornitore gratuito si ferma: resta il piano base, e si può passare a un altro modello gratuito cambiando `OPENROUTER_MODEL`;
  - con 50 richieste al giorno per tutto l'account, l'agente basta per circa 10 messaggi al giorno in tutta l'app;
  - a volte la spiegazione aggiunge un motivo che il piano non dà: i numeri da seguire sono quelli delle attività nel calendario;
  - i passaggi mostrati sotto una risposta si perdono ricaricando la pagina.

## Crediti

- Wuthering Waves, i suoi nomi e le sue immagini appartengono a **Kuro Games**. Progetto didattico non commerciale.
- Dati di gioco dal [Wuthering Waves Wiki](https://wutheringwaves.fandom.com) su Fandom, con licenza [CC BY-SA](https://www.fandom.com/licensing). Fonti e stime sono elencate in [`backend/src/main/resources/data/README.md`](backend/src/main/resources/data/README.md).

## In English

Resonance Planner is a planner for the game Wuthering Waves, built as the capstone project of the Epicode Full-Stack AI Developer master. Version 2 is online: a calendar that shows time-limited events only on the days they are active, manual tasks, the Waveplate stamina counter, the roster and inventory with the materials still missing for each character, a profile page and an admin area for events. On top of that, a basic plan built without AI, plans shown as proposals to confirm or discard, and an AI agent with a chat, running on a free model through OpenRouter: it reads the user's data through five tools, asks the Java planner for a day-by-day plan and explains it, with daily limits that keep it within the free quota. The Java backend does all the calculations, and the user always confirms the plan. Stack: React + TypeScript, Spring Boot, PostgreSQL, deployed on Render.
