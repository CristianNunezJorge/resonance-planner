# Resonance Planner · Capstone Project

Capstone del Master Full-Stack AI Developer (Epicode), progetto individuale di due settimane. È una **demo** di planner per Wuthering Waves: un calendario che unisce eventi a tempo, Waveplate e obiettivi di crescita dei personaggi, con un agente IA che propone un piano che l'utente conferma. "Resonance Planner" è un nome provvisorio.

## Stato attuale (03/10/2026)

- **Progettazione completa:** 9 fasi e 97 decisioni (D1-D97), tutte confermate.
- **Dati di gioco raccolti:** 6 JSON e 6 icone (vedi sotto).
- **Board Trello pronta**, con 54 card, etichette e descrizioni.
- **Codice non ancora iniziato:** non c'è ancora un repository Git. Si parte dopo l'approvazione dello staff, dalla card T01.

## Da leggere per riprendere

1. `HANDOFF.md`: passaggio di consegne per iniziare lo sviluppo, con le istruzioni del giorno 1.
2. `docs/stato-del-progetto.md`: cosa è stato fatto, cosa resta aperto, prossimi passi.
3. `docs/README.md`: indice delle fasi di progettazione.
4. `docs/decisioni.md`: registro di tutte le decisioni (cosa, come, perché, alternative scartate).
5. `docs/09-task.md`: piano giorno per giorno e piano B.

I documenti di fase da 01 a 09 sono la fonte di verità. Se il codice deve discostarsene, prima si aggiorna la decisione.

## Come lavorare con l'utente

- Rispondere in **italiano**, in modo semplice e diretto. L'utente è uno studente: al Demo Day deve saper spiegare ogni scelta, quindi spiegare sempre il perché.
- **Ogni decisione nuova** va in `docs/decisioni.md`, con stato *Proposta* finché l'utente non conferma; poi si aggiorna il documento di fase.
- Proporre con una raccomandazione e chiedere conferma: non cambiare da soli decisioni già prese.
- **Restare sugli strumenti del programma del corso.** Se si propone uno strumento fuori programma, segnalarlo esplicitamente.
- Nel browser, quando l'utente guarda lo schermo condiviso: **niente screenshot**, verificare con strumenti testuali.
- Chiedere il permesso prima di scaricare file.

## Stack (decisioni principali)

| Parte | Scelta | Decisioni |
|---|---|---|
| Frontend | React + TypeScript con Vite, Redux Toolkit, React Router, Bootstrap + SASS, FullCalendar | D7, D76 |
| Backend | Java 21, Spring Boot, Maven, Spring Data JPA, PostgreSQL, Spring Security + JWT | D5, D45, D77 |
| Database | Schema con Flyway (Hibernate in `validate`) | D38 |
| IA | API Claude con l'SDK Java ufficiale, modello `claude-haiku-4-5`, ciclo tool-use scritto da noi | D54, D59 |
| Test | JUnit 5 + Mockito, integrazione su PostgreSQL di Docker Compose, Vitest + React Testing Library, checklist manuale | D79 |
| Deploy | Tutto su Render: frontend, backend e database PostgreSQL (il database gratuito scade dopo 30 giorni e va ricreato) | D82 |

Principi da non perdere:
- I calcoli li fa Java; l'IA sceglie i parametri e spiega (D3).
- Il modello non scrive mai le attività: la proposta è sempre l'ultima bozza del pianificatore (D56).
- L'agente propone, l'utente conferma (D2). Esiste un "piano base" senza IA (D64).
- Dati personali solo sotto `/api/me`; l'utente si ricava dal token (D44).
- Eventi attivi giorno per giorno (D96), effetti con limite giornaliero (D94), eventi della demo ricorrenti (D29).
- Calcolo dei materiali fino al livello 40, fase SOL3 4 (D27, D92).

## Struttura del progetto

Quella prevista è in `docs/08-regole-di-lavoro.md`. Esiste già:

```
backend/src/main/resources/data/   6 JSON di gioco + README con fonti e stime
frontend/public/characters/        6 icone WebP 256×256
docs/                              progettazione (fasi 01-09, decisioni, stato)
```

## Convenzioni

- Codice, nomi e commit in inglese; documentazione in italiano.
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, ...), un ramo per attività, `main` sempre funzionante, almeno un push al giorno.
- Test prima del codice per tutti i calcoli; copertura minima dell'80% su service e logica.
- Nessun segreto nel codice: solo variabili d'ambiente (elenco in `docs/08-regole-di-lavoro.md`).
- Definizione di "fatto" in 6 punti: `docs/08-regole-di-lavoro.md`.

## Link utili

- Board Trello: https://trello.com/b/JCkNlahy/capstone
- Proposta per lo staff (artifact): https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF

## Note operative

- Il wiki Fandom di Wuthering Waves blocca WebFetch (HTTP 402): leggerlo con il browser integrato. I contenuti sono CC BY-SA, quindi la fonte va citata.
- Trello: il testo digitato in una card aperta può diventare una scorciatoia da tastiera. Le descrizioni si scrivono via JavaScript con `execCommand`, dopo aver verificato il focus sull'editor. Le etichette si possono applicare con i tasti 1-0 sulla card, uno alla volta e con una pausa. Il riepilogo `/b/JCkNlahy.json` può essere in ritardo di qualche secondo.
