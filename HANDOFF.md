# Handoff · giorno 12 (margine ed extra)

Passaggio di consegne del 21/10/2026. Il giorno 11 è chiuso: i primi due extra della v3 (D90) sono su `main` e online, **X01** export del piano in Excel (D133) e **X02** accesso ospite "Prova la demo" (D134). Il giorno 12 continua con gli extra, a partire da X03.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Continuiamo il giorno 12 con l'extra X03 (notifiche in tempo reale).
Prima di scrivere codice proponimi la piccola progettazione e la decisione nuova, e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-11** (T01-T38, X01, X02), su `main` e online |
| Rilasci | **v1** (`v1.0.0`, 16/10) e **v2** (`v2.0.0`, 21/10). Gli extra della v3 entrano su `main` uno alla volta; il tag finale arriva con T43 |
| Ultimo commit su `main` | `8de7506`, X02 (PR #51), poi la chiusura del giorno 11 |
| Ramo in corso | Nessuno |
| Test | Backend 895 (`./mvnw verify`, copertura 98,3%), frontend 560 (`npm test`) |
| Decisioni | D1-D134, tutte *Confermate* |
| Agente | Modello **gratuito** `nvidia/nemotron-3-ultra-550b-a55b:free` (D132). Limiti di D129, più quelli degli ospiti (D134): 2 messaggi al giorno per ospite e 18 chiamate al giorno per tutti gli ospiti, sulle 40 dell'app |
| Render | Tutto online, migrazione `V4` applicata. Il database **scade il 10/11/2026** |
| Trello | T01-T38, X01 e X02 in "Fatto"; X03-X05 e T39-T43 in Backlog; P01-P05 in "In verifica" |
| Account di prova | Nessuno. L'ospite creato online il 21/10 per la prova è stato eliminato dal profilo |
| Chrome dell'utente | Per la prova dell'ospite Claude è uscito dall'account dell'utente sul sito: l'utente rientra da solo con email e password |

## Giorno 12 · Cosa fare (D90)

Gli extra che restano, nell'ordine di D90:

| ID | Extra | Da decidere nella piccola progettazione |
|---|---|---|
| X03 | Notifiche in tempo reale (Waveplate quasi piene, evento in scadenza) | WebSocket, argomento della settimana 18 (nel programma). Da decidere: STOMP su WebSocket di Spring o WebSocket semplice; autenticazione con il JWT nella connessione (il token non va nell'URL); chi calcola quando le Waveplate sono quasi piene (il backend sa già "piene alle…", D105); cosa succede con il server gratuito che si addormenta; CSP del sito statico (`connect-src` deve ammettere `wss://` dell'API); un'alternativa più semplice da valutare: notifiche calcolate nel frontend senza server |
| X04 | GitHub Actions | Facoltativo, strumento nuovo (D81). I test hanno bisogno di PostgreSQL e di `DATABASE_USERNAME` e `DATABASE_PASSWORD` come variabili d'ambiente |
| X05 | Damage calculator semplificato | Il più costoso: prima una piccola progettazione e dati che oggi non abbiamo (D1) |

Per ogni extra: decisione nuova in `docs/decisioni.md` come *Proposta*, piano riassunto e conferma dell'utente prima del codice, poi il flusso solito della card. Se il giorno 12 non basta, X04 e X05 si lasciano: i giorni 13-14 sono per T39-T43 (README completo, Swagger, pulizia e Lighthouse, copione della demo e checklist finale, rilascio finale con tag).

## Cosa ha fatto il giorno 11

- **X01 · Export Excel (D133, PR #50):** Apache POI 5.5.1 (`poi-ooxml`). `PlanSpreadsheet` scrive il foglio "Piano" (una riga per attività confermata o fatta, date vere, totali con `SUM`) e il foglio "Eventi"; i testi di utenti e admin restano celle di testo. `GET /api/me/calendar/export` ha gli stessi dati ed errori del calendario. Nel frontend `apiDownload` nel client unico e il pulsante "Esporta in Excel" (`aria-disabled`, un file alla volta). Online: 14,4 s la prima volta dopo il deploy, poi circa 2 s; l'utente ha accettato i 14 s.
- **X02 · Accesso ospite (D134, PR #51):** `POST /api/auth/guest` crea un utente GUEST (`ospite-xxxxxx`, email `@ospite.invalid` mai mostrata, password che nessuno conosce); al massimo 50 ospiti, 5 ogni 15 minuti per indirizzo; `GuestCleanup` (`@Scheduled`) li cancella dopo 24 ore. Quota dell'agente per gli ospiti con la migrazione `V4` (colonna `guest` in `agent_model_calls`). Nel frontend "Prova la demo" con `enterAsGuest`, "(ospite)" nella barra, profilo senza email e conferma di eliminazione con il nome. Provato online da Claude in Chrome: creazione in 2 s, configurazione dalla pagina, calendario, eliminazione.
- **Rischi accettati in D134:** l'utente non vuole altri limiti, perché la demo la userà lui. Sono annotati nella decisione (50 posti riempibili da una persona, ospiti scaduti contati fino alla pulizia, BCrypt per ogni ospite, sessione di un'altra scheda sostituita).

## Come lavoriamo (flusso approvato, invariato)

1. Un ramo per card.
2. Test prima del codice.
3. Controlli locali: `./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`.
4. Prova reale, con strumenti testuali e niente screenshot:
   - backend con il jar;
   - frontend con `frontend-prod`, che manda la CSP. Prima serve `npm run build`;
   - le chiamate vere al modello solo con il permesso dell'utente.
5. Revisione del codice con agenti: revisore del linguaggio e revisore di sicurezza, in parallelo.
6. L'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove.
7. Push, PR senza firme di Claude, squash merge, ramo cancellato.
8. Controllo online.

Altre regole:
- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente.
- Online non si creano account di prova. Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.
- L'agente deve restare gratuito: non proporre modelli a pagamento.

## Promemoria

- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Se il modello dell'agente smette di rispondere: guardare nei log di Render la riga `OpenRouter answered …` e lo stato del fornitore con `GET https://openrouter.ai/api/v1/models/<id>/endpoints` (gratuito, senza chiave), poi cambiare `OPENROUTER_MODEL` (D132).
- Per la checklist finale di T42 si possono riusare `docs/rilascio-v1.md` e `docs/rilascio-v2.md`. Il punto del contatore a 5 / 5 non si raggiunge con la checklist: per vederlo si scrive il contatore in locale con `psql` (vedi le note pratiche).
- **Copione della demo (T42), prima di presentare:** svegliare il backend (circa 3 minuti), poi fare una **esportazione Excel di prova**: la prima dopo un avvio impiega circa 14 secondi, perché POI carica le sue classi (D133, scelta dell'utente del 21/10). Se nella demo si mostra "Prova la demo", farlo dopo il risveglio; l'ospite si elimina dal profilo o sparisce da solo dopo 24 ore (D134).

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101.
6. Bundle JavaScript di circa 670 KB per FullCalendar: se la presentazione diventa lenta, caricare la pagina del calendario solo quando serve (`lazy`, D119).
7. Limiti noti annotati nelle decisioni:
   - catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114), la bozza dell'evento si perde chiudendo la finestra (D122);
   - limiti del pianificatore (D123), impronta di D125 calcolata dopo la bozza (promemoria di T33);
   - modelli gratuiti che possono sparire, cadere o rispondere 429 (D126, D132);
   - più account possono finire le 40 chiamate dell'app (D129);
   - i passaggi si perdono ricaricando la pagina (D130);
   - il `summary` della proposta lo scrive il modello: rischio residuo accettato, tetto a 2.000 caratteri (06-agente.md, "Sicurezza");
   - export Excel: il file è una fotografia, la prima esportazione dopo un avvio è lenta, qualche query in più (D133);
   - ospiti: i rischi accettati dopo la revisione del 21/10 sono elencati in D134.
8. `ANTHROPIC_MODEL` su Render non serve più: si può togliere.

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container può fermarsi da solo (per esempio dopo un riavvio di Docker): se i test non trovano il database, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`;
  - il 21/10 i comandi `docker` (anche `docker exec`) si sono bloccati mentre il database continuava a rispondere sulla 5433. Per scrivere nel database locale senza Docker c'è il client del PostgreSQL 18 installato: `"/c/Program Files/PostgreSQL/18/bin/psql.exe" -h localhost -p 5433 -U "$DATABASE_USERNAME" -d resonance_planner`, con `DATABASE_USERNAME` e `PGPASSWORD` presi dal `.env` senza stamparli (come per la chiamata di prova di OpenRouter);
  - **Norton** ispeziona HTTPS e rifirma i certificati: Java non se ne fida. Per chiamare OpenRouter da Java in locale serve `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (già nella configurazione `backend` di `.claude/launch.json`); con Maven si passa con `JAVA_TOOL_OPTIONS`. Su Render non serve.
- **Migrazioni:** una migrazione non ancora unita a `main` si può cambiare, ma i database locali (di sviluppo e di test) la conoscono già con il checksum vecchio. Prima di riprovare: `drop table` della tabella nuova e `delete from flyway_schema_history where version = '…'` in entrambi, con `psql` dentro il container. Anche un commento cambia il checksum.
- **OpenRouter:**
  - la chiave è nel `.env` (`OPENROUTER_API_KEY`) e su Render: Claude non la legge e non la scrive. Ha un limite di credito di pochi dollari e scade il 15/01/2027; l'account non ha credito né un metodo di pagamento;
  - nelle impostazioni Privacy sono permessi i fornitori gratuiti che si addestrano sui dati, non quelli che pubblicano i prompt;
  - chiamata di prova vera del solo client (2 delle 50 richieste del giorno), dalla cartella `backend/`, in Git Bash:
    ```bash
    set -a; eval "$(grep -E '^OPENROUTER_(API_KEY|MODEL)=' ../.env | tr -d '\r')"; set +a
    JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT" OPENROUTER_LIVE_TEST=true ./mvnw -q test -Dtest=OpenRouterClientLiveTest -Djacoco.skip=true
    ```
  - le richieste gratuite rimaste oggi si leggono con `GET https://openrouter.ai/api/v1/key` e la chiave nell'header `Authorization` (campo `free_model_daily_requests`);
  - se un modello risponde sempre 429 con la quota intatta, provare l'altro modello gratuito (`OPENROUTER_MODEL`), poi i modelli con strumenti elencati da `https://openrouter.ai/api/v1/models`;
  - **prova di un limite senza consumare richieste:** scrivere la riga in `agent_model_calls` del database locale (per esempio `calls = 19` per l'utente di prova) con `docker exec resonance-planner-db sh -c 'psql -U "$POSTGRES_USER" -d resonance_planner -c "..."'`, poi rimetterla a posto.
- **Prova della chat in locale:**
  - si crea l'account di prova e si fa la configurazione guidata via `fetch` dalla scheda di `frontend-prod` (il CORS ammette `localhost:5173`), poi si mette il token in `localStorage` con la chiave `resonance-planner.token` e si apre `/calendario`;
  - la chat si guida con `javascript_tool`: si clicca un suggerimento e si aspetta che sparisca il testo "sta lavorando" dal `role="status"`.
- **Windows e script:**
  - i file hanno fine riga CRLF: per le sostituzioni usare lo strumento di modifica, o uno script Python che rispetta il fine riga senza convertirlo due volte;
  - la shell dimezza le barre rovesciate negli script in linea (`\r`, `\n` delle regex diventano caratteri veri): per testo con barre rovesciate usare lo strumento di modifica;
  - un helper di modifica è nello scratchpad della sessione (`ed.py`), ma non resta tra le sessioni.
- **Browser di prova:**
  - guidare la pagina con `javascript_tool` (setter nativo più eventi `input`/`change`) e leggere il testo;
  - FullCalendar disegna gli elementi solo se la scheda è **in primo piano** (`tabs_select`): in una scheda nascosta `requestAnimationFrame` non parte e gli elementi restano invisibili;
  - i server si avviano con `preview_start` (`backend`, `frontend-prod`) e si fermano con `preview_stop`. Per la vista telefono `resize_window` con `mobile`, poi riportarlo a `desktop`;
  - ricaricare la pagina dentro uno script interrompe lo script: ricaricare con `navigate`, poi leggere con uno script nuovo;
  - per provare un download senza salvare file sul computer si sostituisce `HTMLAnchorElement.prototype.click` (e si cattura il `Blob` da `URL.createObjectURL`) nella scheda di prova, poi si chiude la scheda;
  - in Chrome tutte le schede del sito condividono il `localStorage`, quindi c'è la sessione dell'utente: per provare "Prova la demo" bisogna uscire dal suo account (con il suo permesso), oppure usare il browser integrato, che è separato.
- **Render:**
  - nella pagina Environment i valori si caricano solo con "Show secret": per cambiare una variabile si apre **Edit**, si preme "Show secret" sulla sua riga, si scrive il valore nuovo e si salva con "Save, rebuild, and deploy". Gli altri valori, non caricati, restano com'erano. Il 20/10 un primo salvataggio non era stato registrato: dopo, ricontrollare il valore con "Show secret" e nei log;
  - servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti; un deploy dura circa 4 minuti, il sito statico circa 1;
  - secondo lo staff di Render (forum) una richiesta HTTP può durare fino a 100 minuti: il turno dell'agente (al massimo 3 minuti) ci sta;
  - salvare le variabili d'ambiente con "Save only" (menu accanto a "Save, rebuild, and deploy"); salvare può comunque far ripartire l'ultima versione;
  - con la scheda di Chrome nascosta i clic non aprono i menu: "Manual Deploy" si apre con `button.click()` via JavaScript.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
