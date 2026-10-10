# Stato del progetto

Riepilogo delle sessioni dal 30/09 al 03/10/2026, per riprendere il lavoro in una nuova sessione.

## In una frase

La progettazione è completa e confermata, i dati di gioco sono raccolti e la board Trello è pronta. Si aspetta l'approvazione dello staff per iniziare a scrivere codice, partendo dalla card T01.

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

## Dove sta ogni cosa

| Cosa | Dove |
|---|---|
| Indice della progettazione | `docs/README.md` |
| Registro delle decisioni | `docs/decisioni.md` (D1-D97) |
| Documenti di fase | `docs/01-scope.md` ... `docs/09-task.md` |
| Dati di gioco | `backend/src/main/resources/data/*.json` |
| Fonti, ipotesi e stime dei dati | `backend/src/main/resources/data/README.md` |
| Icone dei personaggi | `frontend/public/characters/*.webp` |
| Istruzioni per Claude Code | `CLAUDE.md` nella cartella principale |
| Board Trello | https://trello.com/b/JCkNlahy/capstone |
| Proposta per lo staff | https://claude.ai/artifact/8U2yRsZMJxTCjNkqmfWrSF |

## Il progetto in breve

- **Demo con dataset curato** (D9): sei personaggi reali (Jinhsi, Yinlin, Changli, Lingyang, Danjin, Aalto, D17), tre eventi reali (Bountiful Crescendo, Lament Recon: Solaris Soldier, By Moon's Grace), calcolo dei materiali fino al livello 40 (D27).
- **Esperienza:** registrazione, configurazione guidata in tre passi, calendario con eventi e Waveplate, chat con l'agente che propone un piano da confermare.
- **Rilasci:** v1 core entro il giorno 6, v2 agente entro il giorno 10, poi margine, extra e rifinitura (D8, D90).
- **Agente:** Claude Haiku 4.5, cinque strumenti limitati all'utente del token, pianificatore deterministico in Java, piano base senza IA come riserva (D54-D64).

## Questioni aperte

1. **Revisione dei dati da parte dell'utente.** Le card P01-P05 sono in "In verifica". Da guardare soprattutto le stime (drop dei nemici comuni, specialità, durate) e le date di partenza degli eventi.
2. **Approvazione dello staff.** La proposta in 14 righe è pronta da inviare.
3. **L'allegato tecnico della proposta è in parte superato:** cita Vercel/Netlify e un limite di livello diverso. Le 14 righe restano valide. Se serve un allegato aggiornato, va rigenerato dai documenti attuali.
4. **Strumento di analisi statica della settimana 16:** l'utente non ha ancora indicato quale avete usato. Per ora basta il formattatore dell'IDE più ESLint (D80).
5. **Nome definitivo del progetto:** "Resonance Planner" è provvisorio.
6. **Card T10 su Trello:** il titolo cita ancora Neon; va aggiornato in "Dockerfile e primo deploy su Render" (D82 del 10/10).

## Prossimi passi

1. L'utente rivede i dati; poi P01-P05 passano in "Fatto".
2. Invio della proposta e attesa dell'approvazione.
3. Giorno 1: card T01-T06 di `docs/09-task.md` (repository GitHub, Docker Compose, progetto Spring Boot, Flyway V1, progetto Vite, aggiornamento di `CLAUDE.md`).
4. Giorno 2: primo deploy online (D89).

## Come riprendere in una nuova sessione

Claude Code legge da solo `CLAUDE.md` e la sua memoria persistente. Per partire basta scrivere, per esempio:

> Leggi `HANDOFF.md`, `CLAUDE.md` e `docs/stato-del-progetto.md`, poi riprendiamo dal prossimo passo.

Il messaggio completo, pensato per iniziare lo sviluppo, è in `HANDOFF.md`.

## Lezioni pratiche dalle sessioni

- Il wiki Fandom va letto con il browser integrato, perché blocca il recupero automatico delle pagine.
- Alcuni dati non sono sul wiki (per esempio le probabilità di drop): in quei casi si stima e si segna `estimated: true` (D25).
- Trello: per scrivere nelle card serve il focus verificato sull'editor. In caso contrario i tasti diventano scorciatoie.
- Le operazioni lunghe nel browser vanno spezzate in gruppi di circa un minuto, perché gli strumenti hanno un tempo massimo di risposta.
