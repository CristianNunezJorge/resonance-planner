# Handoff · da progettazione a sviluppo

Passaggio di consegne del 10/10/2026, per iniziare lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
La progettazione è chiusa: iniziamo lo sviluppo dal giorno 1 (card T01-T06 di docs/09-task.md).
Prima di scrivere codice riassumimi il piano del giorno 1 e chiedimi conferma.
```

## Situazione

- **Progettazione completa:** 9 fasi e 97 decisioni confermate (D1-D97), più l'aggiornamento di D82 del 10/10.
- **Pronto:** 6 JSON di gioco in `backend/src/main/resources/data/`, 6 icone in `frontend/public/characters/`, la [board Trello](https://trello.com/b/JCkNlahy/capstone) con 54 card.
- **Da fare:** tutto il codice. Non esiste ancora un repository Git.

## Da sapere prima di iniziare

| Tema | Dove | In breve |
|---|---|---|
| Regole di lavoro con l'utente | `CLAUDE.md` | Italiano, proporre e chiedere conferma, strumenti del programma, nessuno screenshot quando l'utente guarda |
| Decisioni | `docs/decisioni.md` | Fonte di verità: se il codice deve cambiare una scelta, prima si aggiorna la decisione |
| Schema del database | `docs/04-modello-dati.md` | Include gli aggiornamenti del 02/10 (D93, D94, D96) |
| API | `docs/05-api.md` | Endpoint, formato `{ success, data, error }`, codici di errore |
| Struttura, convenzioni, Git, test | `docs/08-regole-di-lavoro.md` | Cartelle per funzionalità, Conventional Commits, definizione di "fatto" |
| Piano giorno per giorno | `docs/09-task.md` | 43 task, tappe, piano B |

## Questioni aperte

1. **Approvazione dello staff:** chiedere all'utente se è arrivata prima di iniziare.
2. **Revisione dei dati:** le card P01-P05 sono ancora in "In verifica" su Trello.
3. **Card T10 su Trello:** il titolo cita ancora Neon, ma il deploy è tutto su Render (D82 del 10/10).
4. **Analisi statica della settimana 16:** lo strumento non è stato indicato; per ora formattatore dell'IDE più ESLint (D80).

## Giorno 1: cosa fare

L'ordine segue `docs/09-task.md`. Il primo commit crea la struttura su `main`; dal secondo in poi, un ramo per ogni attività.

**T01 · Repository**
- `git init` nella cartella del progetto, ramo `main`, repository pubblico su GitHub.
- `.gitignore` per Maven, Node, IDE e `.env`.
- `.env.example` con le variabili elencate in `docs/08-regole-di-lavoro.md`, senza valori.
- README iniziale, da completare nel corso del progetto (D85).
- Il primo commit include `docs/`, i JSON e le icone, ma non `.env`.

**T02 · Docker Compose**
- Servizio PostgreSQL 16 con volume persistente e porta 5432.
- Due database: sviluppo e test (D79), creati da uno script di inizializzazione.

**T03 · Backend**
- Spring Initializr: Java 21, Maven, pacchetto `com.resonanceplanner`.
- Dipendenze: Web, Data JPA, Security, Validation, Flyway, PostgreSQL Driver, Actuator. springdoc-openapi va aggiunto a mano nel `pom.xml`.
- Prima di iniziare verificare l'ultima versione stabile di Spring Boot (D77).
- Profili `dev`, `test` e `prod`; all'avvio il backend controlla le variabili obbligatorie e si ferma se ne manca una (D83).
- **Attenzione:** generare il progetto senza sovrascrivere `backend/src/main/resources/data/`.

**T04 · Schema**
- `V1__schema.sql` con tutte le tabelle di `docs/04-modello-dati.md`: controlli, indici e l'indice unico parziale di D37.
- Hibernate in `validate`: il backend deve partire senza errori (D38).

**T05 · Frontend**
- Progetto Vite React + TypeScript in `frontend/`, con Redux Toolkit, React Router, Bootstrap e SASS (D7, D76).
- **Attenzione:** non sovrascrivere `frontend/public/characters/`. Se il generatore rifiuta una cartella non vuota, crearlo altrove e unire.
- Struttura delle cartelle come in `docs/08-regole-di-lavoro.md`.

**T06 · CLAUDE.md**
- Aggiungere i comandi reali per avviare, testare e fare il build di backend e frontend.
- Aggiornare la sezione "Stato attuale".

A fine giornata: spostare le card su Trello, fare il push, aggiornare `docs/stato-del-progetto.md`.

## Promemoria per i giorni successivi

- **Giorno 2:** primo deploy, tutto su Render (D82, D89). Il database gratuito scade dopo 30 giorni: crearlo o ricrearlo a meno di 30 giorni dal Demo Day.
- **Calcoli** (Waveplate, farming, eventi, pianificatore): test prima del codice (D79).
- **Agente** (giorni 8-10): leggere prima `docs/06-agente.md` e le linee guida ufficiali dell'API Claude per l'SDK Java. Modello `claude-haiku-4-5` (D59).
