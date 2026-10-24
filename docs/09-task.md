# 9. Task

**Stato:** completata · 02/10/2026

Il lavoro è diviso in una **fase 0**, da fare mentre si aspetta l'approvazione dello staff, e in **14 giorni** di sviluppo. Ogni attività ha un codice da usare come titolo della card su Trello, l'area, le decisioni a cui si riferisce e il criterio per dirla finita. Vale sempre anche la definizione di "fatto" della fase 8.

**Aree:** `DATI` dati di gioco · `BE` backend · `FE` frontend · `IA` agente · `OPS` repository e deploy · `DOC` documentazione · `QA` verifica

## Tappe

| Giorno | Tappa |
|---|---|
| Prima dell'inizio | Dati di gioco pronti, board Trello pronta, progetto approvato |
| 2 | Primo deploy online, anche se l'app è quasi vuota |
| 6 | **v1** online: tutto tranne l'agente |
| 10 | **v2** online: agente e piano base |
| 11-12 | Margine per i ritardi, poi extra della v3 |
| 14 | Versione finale, README completo, copione della demo |

## Fase 0 · Prima dell'inizio (D88)

| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| P01 | Dati dei sei personaggi: elemento, arma, rarità, ruolo, icona, costi delle prime due ascensioni | DATI | D17, D27, D30 | `characters.json` completo, icone salvate |
| P02 | Tabella EXP dal livello 1 al 40, EXP delle Resonance Potion, eventuale costo in Shell Credit | DATI | D27 | `level-exp.json` e materiali EXP completi |
| P03 | Fonti di farming (Simulation Training, Boss Challenge, nemici comuni, specialità) con drop medi | DATI | D25, D31 | `farming-sources.json` completo, stime segnate |
| P04 | Un evento reale per ciascuno dei tre tipi, con date relative e ripetizione | DATI | D18, D29 | `events.json` con tre eventi |
| P05 | Controllo incrociato sulla seconda fonte e revisione insieme | DATI | D25 | Ogni numero ha una fonte o è segnato come stima |
| P06 | Board Trello con tutte le card di questo documento | DOC | D87 | Board pronta, card della v1 in "Da fare" |

## Settimana 1 · v1

### Giorno 1 · Setup
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T01 | Repository GitHub: cartelle, `.gitignore`, `.env.example`, README iniziale | OPS | D74, D83 | Repository pubblico con la struttura della fase 8 |
| T02 | `docker-compose.yml` con PostgreSQL (database di sviluppo) | OPS | D84 | `docker compose up` avvia il database |
| T03 | Progetto Spring Boot: dipendenze, profili `dev`/`prod`, controllo delle variabili all'avvio | BE | D77, D83 | Il backend parte in locale; senza variabili si ferma con un messaggio chiaro |
| T04 | Migrazione Flyway `V1__schema.sql` con tutto lo schema della fase 4 | BE | D38 | Schema creato; Hibernate in `validate` parte senza errori |
| T05 | Progetto Vite: React, TypeScript, Redux Toolkit, React Router, Bootstrap, SASS, ESLint | FE | D7, D76 | Pagina vuota con router e store funzionanti |
| T06 | `CLAUDE.md` con convenzioni e rimandi ai documenti | DOC | D86 | File nel repository |

### Giorno 2 · Fondamenta e primo deploy
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T07 | `ApiResponse`, eccezioni e `@RestControllerAdvice` con i codici di errore | BE | D46 | Un errore di validazione e un 404 rispondono nel formato `ApiResponse` |
| T08 | Entità del catalogo, lettura di `game-rules.json` e caricamento dei JSON all'avvio | BE | D26, D33, D39 | Al riavvio il catalogo si aggiorna, gli eventi non si duplicano |
| T09 | API pubbliche del catalogo (`/api/game/**`) | BE | D49 | Endpoint documentati in Swagger |
| T10 | `Dockerfile` e primo deploy: frontend, backend e database su Render | OPS | D82, D84 | L'app online risponde e legge il catalogo |
| T11 | Client HTTP del frontend e tema SASS di base (scuro, colori degli elementi) | FE | D65, D76 | Una pagina di prova mostra i personaggi dal backend online |

### Giorno 3 · Autenticazione e profilo
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T12 | Spring Security con JWT: registrazione, login, ruoli, admin da variabili, BCrypt, CORS | BE | D10, D19, D45 | Registrazione e login funzionano; senza token l'accesso è negato |
| T13 | Limiti di richieste su login e registrazione e limite generale | BE | D48 | All'undicesimo tentativo la risposta è 429 |
| T14 | API del profilo: lettura, modifica, eliminazione dell'account | BE | D42 | Dopo l'eliminazione i dati collegati non esistono più |
| T15 | Login, registrazione, `authSlice`, `ProtectedRoute`, barra di navigazione, footer | FE | D73 | Flussi F1, F2 e F11 funzionanti |
| T16 | Pagina di presentazione | FE | D72 | Pagina P1 con i personaggi dal catalogo |

### Giorno 4 · Waveplate, roster e configurazione
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T17 | Calcolo delle Waveplate e dei Crystals e API | BE | D28, D36 | Valori giusti sotto il massimo, al massimo e oltre |
| T18 | Roster: regole tra livello e grado e API | BE | D34 | Le combinazioni non valide restituiscono 400 |
| T19 | API dell'inventario | BE | D21 | Quantità modificabile, mai negativa |
| T20 | API della configurazione guidata, in un'unica transazione | BE | D50 | Una richiesta non valida non salva nulla |
| T21 | Configurazione guidata in tre passi | FE | D20 | Flusso F3 completo fino al calendario |

### Giorno 5 · Farming ed eventi
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T22 | Calcolo dei materiali mancanti, delle run, delle Waveplate e dei minuti e API | BE | D3, D27 | Risultati giusti con un personaggio, con più personaggi e con l'obiettivo già raggiunto |
| T23 | Eventi: date delle edizioni ricorrenti e giorni attivi, API pubbliche e API admin | BE | D12, D14, D29, D96 | Date giuste prima, durante e dopo un'edizione e per un evento attivo solo in alcuni giorni |
| T24 | Pagina roster e inventario, `CharacterAvatar` con ripiego sulle iniziali | FE | D30 | Pagina P5 completa, anche con un'icona mancante |

### Giorno 6 · Calendario e rilascio v1
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T25 | Attività manuali, completamento con aggiornamento dell'inventario, endpoint del calendario | BE | D21, D24, D35, D51 | Il completamento funziona con e senza effetto di un evento |
| T26 | Calendario: viste, eventi mostrati solo nei giorni attivi, attività, finestre di dettaglio, riquadro Waveplate, stati vuoti | FE | D66, D67, D69, D96 | Pagina P4 senza la chat, anche da telefono |
| T27 | Pagina profilo e gestione eventi admin | FE | D14, D42 | Pagine P6 e P7 funzionanti |
| T28 | Rilascio v1: checklist manuale (punti 1-5 e 10-13), README aggiornato, tag `v1.0.0` | QA | D8 | Checklist superata sul sito online |

## Settimana 2 · v2, extra e rifinitura

### Giorno 7 · Pianificatore e proposte
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T29 | Pianificatore giorno per giorno | BE | D57 | Piani giusti con evento a ricompense doppie, giorni esclusi, tempo esaurito |
| T30 | Proposte: piano base, conferma, scarto, sostituzione, indice parziale | BE | D22, D37, D64 | Due richieste contemporanee non creano due proposte in attesa |
| T31 | Barra della proposta, attività tratteggiate, pulsante "Genera piano base", avviso di cambiamento | FE | D23, D68 | Punto 6 della checklist funzionante |

### Giorno 8 · Agente: backend
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T32 | Client di OpenRouter dietro un'interfaccia, configurazione di chiave e modello gratuito | IA | D54, D59, D126 | Una chiamata di prova risponde con il modello gratuito |
| T33 | I cinque strumenti, limitati all'utente del token, con validazione dei parametri | IA | D55, D56 | Ogni strumento risponde solo con i dati dell'utente del token |
| T34 | Ciclo dell'agente: massimo 6 giri, errori, passaggi, cache dei prompt | IA | D54, D61, D62 | Il ciclo si ferma dopo 6 giri e gestisce gli errori |

### Giorno 9 · Agente: chat
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T35 | Salvataggio dei messaggi, storico a pagine, limite giornaliero, API di utilizzo | IA | D40, D48, D129 | Il sesto messaggio del giorno risponde 429 |
| T36 | Chat: messaggi, suggerimenti, passaggi, contatore, attesa, pannello a scomparsa su telefono | FE | D11, D62, D66 | Flusso F6 completo dal calendario |

### Giorno 10 · Rifinitura dell'agente e rilascio v2
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T37 | Rifinitura delle istruzioni di sistema e verifica dei 10 scenari reali | IA | D58, D63 | Tutti gli scenari passano |
| T38 | Rilascio v2: checklist completa, README aggiornato, tag `v2.0.0` | QA | D8 | Checklist superata sul sito online |

### Giorni 11-12 · Margine ed extra (D90)
Prima si recuperano eventuali ritardi di v1 e v2. Il tempo che resta va agli extra, in quest'ordine:

| ID | Attività | Area | Rif. | Perché in questa posizione |
|---|---|---|---|---|
| X01 | Export del piano in Excel | BE, FE | Apache POI, settimana 15 | Poco lavoro, argomento del corso |
| X02 | Accesso ospite "Prova la demo" | BE, FE | D15 | Poco lavoro, molto utile per i recruiter |
| X03 | Notifiche in tempo reale (Waveplate quasi piene, evento in scadenza) | BE, FE | WebSocket, settimana 18 | Lavoro medio, argomento del corso |
| X04 | GitHub Actions | OPS | D81 | Facoltativo, strumento nuovo |
| X05 | Damage calculator semplificato | BE, FE | D1 | Il più costoso: prima richiede una piccola progettazione e dati in più |

### Giorni 13-14 · Rifinitura e demo
| ID | Attività | Area | Rif. | Fatta quando |
|---|---|---|---|---|
| T39 | README completo: screenshot, link, avvio, variabili, limiti noti, crediti, riassunto in inglese | DOC | D85 | Una persona esterna riesce ad avviare il progetto seguendolo |
| T40 | Swagger: descrizioni ed esempi per ogni endpoint | BE | D53 | Ogni endpoint ha descrizione ed esempio |
| T41 | Pulizia del codice e controllo dell'accessibilità con Lighthouse | QA | D70 | Punteggio di accessibilità salvato |
| T42 | Copione della demo e checklist finale sul sito online | QA | D138 | Copione scritto e checklist finale superata sul sito online |
| T43 | Rilascio finale con tag | OPS | D8 | Tag su `main`, sito online aggiornato |

## Se qualcosa va in ritardo (D91)

| Situazione | Cosa si fa |
|---|---|
| Fine del giorno 6 senza v1 completa | La gestione eventi admin (T27) slitta al giorno 7; il resto della v1 ha la precedenza |
| Fine del giorno 8 con l'agente in difficoltà | Si consolida il piano base (D64): la demo funziona anche senza agente |
| Fine del giorno 10 senza v2 completa | I giorni 11-12 vanno tutti alla v2, niente extra |
| Due giorni persi in qualsiasi momento | Si rinuncia a tutta la v3 |

Se nelle due settimane non lavori nei weekend, i giorni effettivi sono 10: si tolgono i giorni di margine 11-12, e gli extra si fanno solo se v1 e v2 sono in anticipo.

## Board Trello (D87)

| Colonna | Cosa contiene |
|---|---|
| Backlog | Le card dei rilasci successivi (v2, rifinitura) e gli extra della v3 |
| Da fare | Le card del rilascio in corso |
| In corso | Al massimo due card alla volta |
| In verifica | Card con codice scritto, in attesa di prova e rilettura |
| Fatto | Card che rispettano la definizione di "fatto" |

- **Etichette:** `v1` (verde), `v2` (giallo), `v3` (arancione) per il rilascio; `BE` (rosso), `FE` (viola), `IA` (blu), `DATI` (azzurro), `OPS` (lime), `DOC` (rosa), `QA` (nero) per l'area. Le card di fase 0 e rifinitura hanno solo l'etichetta dell'area. Scorciatoia: con il mouse sulla card, i tasti da 1 a 0 attivano o tolgono le etichette in quest'ordine.
- **Card:** titolo con codice e nome ("T17 · Calcolo delle Waveplate"); nella descrizione rilascio, area, decisioni di riferimento, criterio "fatta quando" e il rimando alla definizione di "fatto" della fase 8.
