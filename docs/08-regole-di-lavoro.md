# 8. Regole di lavoro

**Stato:** completata · 01/10/2026

## Struttura del repository (D74, D75, D76)

Un solo repository GitHub con backend, frontend e documentazione:

```
resonance-planner/
├── README.md                 presentazione del progetto, link alla demo, avvio
├── CLAUDE.md                 convenzioni del progetto per Claude Code (D86)
├── .env.example              elenco delle variabili d'ambiente, senza valori segreti
├── docker-compose.yml        PostgreSQL in locale, per lo sviluppo
├── .github/workflows/ci.yml  controlli automatici, facoltativi (D81)
├── docs/                     progettazione (questi documenti)
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/resonanceplanner/
│       │   ├── auth/         registrazione, login, JWT
│       │   ├── user/         profilo, eliminazione account
│       │   ├── catalog/      personaggi, materiali, fonti, regole di gioco
│       │   ├── event/        eventi e gestione admin
│       │   ├── roster/       personaggi dell'utente
│       │   ├── inventory/    materiali posseduti
│       │   ├── waveplate/    contatore
│       │   ├── farming/      calcolo dei materiali mancanti
│       │   ├── planning/     pianificatore, proposte, attività, calendario
│       │   ├── agent/        ciclo dell'agente e strumenti
│       │   ├── seed/         caricamento dei JSON all'avvio
│       │   └── common/       configurazione, sicurezza, errori, ApiResponse
│       ├── main/resources/
│       │   ├── application.yml, application-dev.yml, application-prod.yml
│       │   ├── db/migration/ V1__schema.sql, ...
│       │   └── data/         i sei file JSON (D26)
└── frontend/
    ├── package.json, vite.config.ts, tsconfig.json
    ├── public/characters/    icone dei personaggi (D30)
    └── src/
        ├── app/              store Redux, router, hook tipizzati
        ├── api/              client HTTP comune
        ├── features/         auth, onboarding, calendar, roster, profile, agent, admin
        ├── components/       componenti condivisi
        ├── styles/           variabili e stili SASS
        └── types/            tipi TypeScript comuni
```

**Backend organizzato per funzionalità** (D75): ogni cartella contiene controller, service, repository, entità e DTO di quella funzionalità. Le dipendenze vanno in una sola direzione: controller → service → repository.

**Frontend organizzato per funzionalità** (D76): ogni cartella di `features/` contiene i suoi componenti, lo slice Redux e le chiamate API. Le chiamate passano tutte da un unico client (`api/client.ts`) che aggiunge il token, apre il formato `ApiResponse` e trasforma gli errori in un tipo unico.

## Versioni (D77)

| Strumento | Versione |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | Ultima versione stabile al momento del setup |
| Build backend | Maven |
| Node.js | Ultima versione LTS |
| React, TypeScript, Vite | Ultime versioni stabili |
| PostgreSQL | 16 o superiore, uguale in locale e online |

Le versioni esatte si fissano il primo giorno e si scrivono nel README.

## Convenzioni di codice

| Cosa | Regola |
|---|---|
| Lingua del codice | Inglese: nomi, commenti, messaggi di commit |
| Classi Java | `PascalCase`; DTO con suffisso `Request` o `Response` (`LoginRequest`) |
| Metodi e variabili | `camelCase`; costanti `UPPER_SNAKE_CASE` |
| Componenti React | `PascalCase`, un componente per file `.tsx` |
| Hook e slice | `useWaveplate`, `calendarSlice.ts` |
| Dimensioni | Funzioni sotto le 50 righe, file sotto le 400 (massimo 800) |
| Annidamento | Massimo 4 livelli: meglio uscire subito dalle funzioni (*early return*) |
| Dati | Oggetti immutabili dove possibile: `record` Java, nessuna modifica diretta dello stato in React |
| Segreti | Mai nel codice né nei commit: solo variabili d'ambiente (D83) |
| Log e debug | Niente `System.out` né `console.log`; il backend usa il logger |

Formattazione e analisi statica (D80): formattatore dell'IDE per il Java, ESLint (già incluso con Vite) per il frontend.

## Git (D78)

- `main` è sempre funzionante e pubblicato online.
- Ogni attività si sviluppa su un ramo: `feat/waveplate-counter`, `fix/onboarding-validation`, `docs/readme-setup`.
- Si unisce a `main` con una pull request, solo dopo che lint e build sono passati (D81). Le pull request si uniscono in un solo commit (*squash merge*).
- I messaggi di commit seguono Conventional Commits:
  ```
  feat: add waveplate counter endpoint
  fix: reject onboarding with target below current level
  docs: add setup instructions to README
  ```
  Tipi: `feat`, `fix`, `refactor`, `docs`, `chore`, `perf`, `ci`.
- Almeno un push al giorno (lo chiede il regolamento) e mai un commit che non compila.
- Ogni rilascio ha un tag: `v1.0.0`, `v2.0.0`, `v3.0.0` (D8).

## Verifica dei flussi (D79)

### Checklist di prova manuale

I percorsi dell'utente si provano dall'inizio alla fine. Da ripetere sulla versione online prima di ogni rilascio (v1, v2, v3) e prima del Demo Day:

1. Registrazione di un utente nuovo → configurazione guidata completa → arrivo al calendario.
2. Login con credenziali sbagliate → messaggio di errore; login corretto → calendario.
3. Aggiornamento delle Waveplate → valore e orario "piene alle" corretti.
4. Aggiunta e modifica di un personaggio nel roster → materiali mancanti aggiornati.
5. Creazione, modifica e cancellazione di un'attività manuale.
6. Piano base → anteprima tratteggiata → conferma → attività nel calendario.
7. Attività di farming segnata come fatta → inventario aggiornato.
8. (v2) Richiesta all'agente → proposta con spiegazione e passaggi → conferma.
9. (v2) Richiesta di modifica della proposta → la nuova sostituisce la vecchia.
10. Admin: creazione di un evento → visibile nel calendario con l'avviso di cambiamento.
11. Utente non admin che apre la gestione eventi → rimandato al calendario.
12. Calendario su telefono: vista elenco e chat a scomparsa.
13. Eliminazione dell'account → dati cancellati, login non più possibile.

## Controlli prima di unire (D81)

Prima di ogni pull request si lanciano in locale:

| Parte | Controlli |
|---|---|
| Backend | Compilazione e jar |
| Frontend | ESLint, controllo dei tipi TypeScript, build |

I due comandi sono scritti nel README. **Facoltativo:** se avanza tempo, GitHub Actions esegue gli stessi controlli da solo a ogni pull request. Dopo l'unione, Render pubblica da solo la nuova versione di `main`.

## Deploy (D82)

| Parte | Piattaforma | Piano gratuito |
|---|---|---|
| Frontend | Render, sito statico | Pubblicato a ogni push su `main` |
| Backend | Render, come immagine Docker | Si spegne dopo 15 minuti di inattività e alla prima richiesta si riaccende in poco meno di 3 minuti (2 min e 43 s, misurati l'11/10/2026) |
| Database | Render, PostgreSQL | Scade 30 giorni dopo la creazione; dopo altri 14 giorni viene cancellato |

Tutto sta su Render, la piattaforma usata nella settimana 20.

**Per la scadenza del database:**
- ricrearlo non fa perdere nulla di importante: Flyway rifà lo schema e l'avvio del backend ricarica catalogo, eventi demo e account admin (D38, D39); si perdono solo gli account di chi ha provato la demo;
- crearlo o ricrearlo a meno di 30 giorni dal Demo Day, e segnarsi la data di scadenza;
- finché il progetto deve restare nel portfolio, ricrearlo ogni mese oppure passare al piano a pagamento di Render.

**Per la riaccensione del backend:**
- se la prima richiesta è lenta, dopo 5 secondi il frontend mostra "Il server si sta avviando: può volerci fino a tre minuti.";
- prima del Demo Day si apre il sito qualche minuto prima, così il backend è già acceso.

I piani gratuiti cambiano spesso: si ricontrollano il giorno del primo deploy.

## Ambienti e variabili (D83, D84)

| Profilo | Dove | Database |
|---|---|---|
| `dev` | Il tuo computer | PostgreSQL in Docker con `docker compose up` |
| `prod` | Render | PostgreSQL di Render |

| Variabile | A cosa serve |
|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Connessione al database |
| `JWT_SECRET` | Firma dei token, almeno 256 bit casuali |
| `OPENROUTER_API_KEY` | Chiave dell'API di OpenRouter (D126) |
| `OPENROUTER_MODEL` | Modello gratuito, per esempio `nvidia/nemotron-3-ultra-550b-a55b:free` (D132): deve finire con `:free` (D59, D126) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Account admin creato all'avvio (D19) |
| `CORS_ALLOWED_ORIGINS` | Dominio del frontend (D48) |
| `VITE_API_BASE_URL` | Indirizzo del backend, per il frontend |
| `APP_RATELIMIT_AUTHATTEMPTS`, `APP_RATELIMIT_AUTHWINDOW`, `APP_RATELIMIT_GENERALREQUESTS`, `APP_RATELIMIT_GENERALWINDOW` | Facoltative: limiti di richieste (D48, D101). Predefiniti: 10 tentativi ogni 15 minuti per login e registrazione, 120 richieste al minuto per il resto. Una finestra scritta come numero semplice vale minuti (`AUTHWINDOW`) o secondi (`GENERALWINDOW`) |

- `.env.example` elenca le variabili senza valori ed entra nel repository; `.env` con i valori veri è escluso da Git.
- All'avvio il backend controlla che le variabili obbligatorie ci siano, e se ne manca una si ferma con un messaggio chiaro.

## Documentazione (D85)

Il README principale contiene: descrizione e screenshot, link alla demo online e a Swagger, stack, istruzioni per clonare e avviare il progetto, variabili d'ambiente, comandi per i controlli, limiti noti (D41, D45, D57), crediti (Kuro Games, Wuthering Waves Wiki con licenza CC BY-SA) e un link a `docs/`. È in italiano, con un breve riassunto in inglese per i recruiter stranieri.

## Definizione di "fatto"

Un'attività è fatta quando:
1. il codice fa quello che dice il documento di progettazione;
2. codice formattato, e lint e build passano;
3. l'hai riletta e la sai spiegare riga per riga;
4. i documenti sono aggiornati, se la decisione è cambiata;
5. è unita a `main` ed è online.

## Uso di Claude Code (D86)

- `CLAUDE.md` nel repository riassume convenzioni e decisioni principali, così ogni sessione parte dalle stesse regole.
- Ogni modifica proposta va letta e capita prima di accettarla: al Demo Day devi saper spiegare ogni dettaglio del codice (lo chiede il regolamento).
- Prima di ogni pull request si fa una revisione del codice, anche con l'aiuto di Claude Code.
- I messaggi di commit li controlli tu.
