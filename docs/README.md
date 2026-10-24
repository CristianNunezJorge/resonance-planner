# Resonance Planner · Progettazione

Documenti di progettazione del Capstone Project del Master Full-Stack AI Developer (Epicode). "Resonance Planner" è un nome provvisorio.

**Regola:** non si scrive codice finché le fasi 1-8 non sono confermate. Le fasi sono tutte confermate e lo sviluppo è iniziato il 10/10/2026.

**Per riprendere il lavoro:** [stato-del-progetto.md](stato-del-progetto.md) riassume cosa è stato fatto, le questioni aperte e i prossimi passi.

## Ordine della progettazione

Le fasi vanno fatte in quest'ordine: ognuna usa le decisioni di quella prima.

| Fase | A quali domande risponde | Cosa produce | Stato |
|---|---|---|---|
| 1. Scope | Cosa fa l'app? Cosa non fa? Con quali vincoli? | [01-scope.md](01-scope.md) | Completata |
| 2. Utenti e flussi | Chi usa l'app? Cosa fa, passo per passo? Cosa succede quando qualcosa va storto? | [02-utenti-e-flussi.md](02-utenti-e-flussi.md) | Completata |
| 3. Dati di gioco e demo | Quali personaggi, materiali ed eventi? Da dove prendiamo i dati e in che formato li salviamo? | [03-dati-demo.md](03-dati-demo.md) e formato dei file JSON | Completata (raccolta dati da fare) |
| 4. Modello dati | Quali tabelle, con quali campi e relazioni? | [04-modello-dati.md](04-modello-dati.md) con diagrammi ER | Completata (aggiornata il 10/10 con D98) |
| 5. API REST | Quali endpoint, con quali richieste e risposte? Come gestiamo errori e autenticazione? | [05-api.md](05-api.md) | Completata |
| 6. Agente IA | Quali strumenti usa, con quali istruzioni e limiti? Come lo testiamo? | [06-agente.md](06-agente.md) | Completata |
| 7. Schermate | Quali pagine, cosa mostrano, come ci si muove tra loro? | [07-schermate.md](07-schermate.md) con bozze | Completata |
| 8. Regole di lavoro | Struttura delle cartelle, convenzioni, Git, test, deploy | [08-regole-di-lavoro.md](08-regole-di-lavoro.md) | Completata |
| 9. Task | Cosa si fa, in che ordine, in quale giorno? | [09-task.md](09-task.md) e board Trello | Completata (aggiornata il 24/10 con D138) |

La raccolta dei dati reali è stata completata il 02/10/2026: i file sono in `backend/src/main/resources/data/`, con fonti e stime nel loro README. Ha portato alle decisioni D92-D97.

**Rilasci:** le checklist da seguire sul sito online, con il loro esito, sono in [rilascio-v1.md](rilascio-v1.md), [rilascio-v2.md](rilascio-v2.md) e [rilascio-v3.md](rilascio-v3.md), la checklist finale prima del Demo Day.

**Qualità:** copertura dei test, punteggi di Lighthouse e pulizia del codice (T41) sono in [qualita.md](qualita.md).

**Demo Day:** il copione della presentazione di 3 minuti, con preparazione, piano B e domande probabili (T42), è in [demo.md](demo.md).

## Registro delle decisioni

Tutte le scelte sono in [decisioni.md](decisioni.md). Ogni decisione ha un codice (D1, D2, ...) che gli altri documenti citano, e riporta cosa abbiamo scelto, come lo implementiamo, perché e quali alternative abbiamo scartato.

Ogni decisione ha uno stato:
- **Confermata**: decisa, si implementa così.
- **Proposta**: suggerita, in attesa di conferma.
