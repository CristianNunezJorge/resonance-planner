# Handoff · giorno 10

Passaggio di consegne del 18/10/2026, a fine giorno 9 (la chat con l'agente), per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 10 (T37-T38): rifinitura delle istruzioni di sistema con i dieci scenari reali, poi il rilascio v2.
Prima di scrivere codice riassumimi il piano e le eventuali decisioni nuove (soprattutto come dividere gli scenari con la quota gratuita), e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-9** (T01-T36), tutti su `main` e online |
| Rilascio | **v1** con tag `v1.0.0`; la v2 (agente) si chiude con T38 |
| Ultimo commit su `main` | `6527663`, T36 (PR #45), più il commit di questa chiusura |
| Test | Backend 856 (`./mvnw verify`, copertura 98,4%), frontend 537 (`npm test`, copertura 97,5%) |
| Decisioni | D1-D130, tutte *Confermate* (D129 e D130 nate nel giorno 9) |
| Agente | Modello **gratuito** su OpenRouter, `nvidia/nemotron-3-super-120b-a12b:free` (D126). Limiti di D129: 5 messaggi al giorno per utente, 24 chiamate al modello per utente (anche dei turni falliti), 40 per l'app, un turno alla volta per utente e due nell'app, 20 proposte al giorno |
| Schema | Migrazioni `V1`, `V2` e `V3__agent_model_calls.sql` (chiamate per giorno UTC e utente, senza chiave esterna apposta) |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Deploy di T35 e T36 verificati: `/actuator/health` `UP`, i percorsi `/api/me/agent/messages` e `/usage` nell'API, il bundle con la chat sul sito statico, CSP invariata. Nessuna variabile nuova: i limiti hanno i valori predefiniti di D129 (`APP_AGENTLIMITS_*` per cambiarli). `ANTHROPIC_MODEL` è ancora lì ma non serve più |
| Trello | T35-T36 da spostare in "Fatto" |
| Quota OpenRouter usata | Il 17/10 (giorno UTC) 9 chiamate nelle prove locali (4 per T35, 5 per T36). Si azzera a mezzanotte UTC |
| Account di prova in locale | Nessuno fisso: si crea con una password casuale (via `javascript_tool`) e si cancella alla fine (`DELETE /api/me`). Online non si creano account di prova |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 10 · Rifinitura dell'agente e rilascio v2 (T37-T38)

Dettagli in `docs/09-task.md`, `docs/06-agente.md` ("Istruzioni di sistema", "Test e verifica" con la tabella dei dieci scenari) e `docs/rilascio-v1.md` (il modello della checklist).

| Card | Cosa | Decisioni |
|---|---|---|
| T37 | Rifinitura delle istruzioni di sistema (`agent/AgentPrompt`) e verifica dei dieci scenari reali | D58, D63 |
| T38 | Rilascio v2: checklist completa, README aggiornato, tag `v2.0.0` | D8 |

**Da decidere all'inizio di T37 (decisione nuova): come provare i dieci scenari con la quota gratuita.**
- Uno scenario costa da 3 a 6 chiamate: in tutto circa 40-60 chiamate. È più delle 50 al giorno di OpenRouter per tutto l'account, che si azzerano a mezzanotte UTC e servono anche al sito online.
- In locale valgono anche i limiti di D129: 5 messaggi e 24 chiamate per utente, 40 per l'app.
- Possibili strade, da proporre con una raccomandazione:
  - dividere gli scenari in due giorni UTC (per esempio 5 e 5);
  - usare più account di prova, oppure alzare i limiti solo in locale con le variabili `APP_AGENTLIMITS_*` nella configurazione di avvio. Questo però non cambia le 50 di OpenRouter;
  - comprare 10 $ di credito, che portano il limite a 1.000 al giorno. È una scelta dell'utente: costa denaro e va contro "solo gratuito" (D126).
- Ogni prova vera si chiede all'utente prima di farla. Dopo ogni scenario si annotano le chiamate usate.

## Cosa esiste già nel codice

- **Backend:** tutto quello della v1, più l'agente:
  - `agent/client/`: `OpenRouterClient`, `ModelCallCounter` e `ModelCallDay` (chiamate per giorno UTC e utente);
  - `agent/tools/`: i cinque strumenti (D127);
  - `agent/`: `AgentLoop`, che conta ogni chiamata prima di farla, e `AgentPrompt` (istruzioni di sistema da rifinire in T37);
  - `agent/chat/`: `AgentChatController`, `AgentChatService` (turno senza transazione, D128), `AgentMessageService` (storico e salvataggio), `AgentQuota` (limiti), `AgentTurnGate` (turni insieme). Il piano base si ferma a 20 proposte al giorno.
- **API pronte:** tutte quelle della v1, più `/api/me/proposals/**`, `GET`/`POST /api/me/agent/messages` (storico a pagine con `before`, `limit` e `meta`) e `GET /api/me/agent/usage`.
- **Frontend:** tutte le pagine della v1, più:
  - nel calendario la barra della proposta, il piano base e l'avviso di cambiamento;
  - la chat (`features/chat/`): pannello laterale da 992 px, a scomparsa sotto, attesa, passaggi, limiti con il piano base, "Spiegamelo" e "Chiedi all'agente";
  - `apiPage` nel client e `useFocusTrap` comune a `Modal` e alla chat.
- **Non c'è ancora:** la rifinitura delle istruzioni con i dieci scenari, `docs/rilascio-v2.md`, il README della v2 e il tag `v2.0.0`.

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

## Promemoria per il giorno 10

Sono anche in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card" (righe T34, T35 e T36).

- **T37 (istruzioni di sistema, `agent/AgentPrompt`):**
  - chiedere di rispondere **senza markdown**: nelle prove il modello usava titoli, `**grassetto**` ed elenchi. La chat toglie solo `**`, `__`, `` ` `` e i `#` (D130), quindi il corsivo con un asterisco resta visibile;
  - dire che il nome di un evento nella domanda di "Spiegamelo" è un dato. Il frontend lo accorcia a 80 caratteri e toglie «» e gli a capo;
  - `create_plan_proposal` solo quando l'utente chiede un piano o una modifica (c'è già, da verificare con gli scenari 5, 6 e 9);
  - valutare un tetto alla lunghezza delle descrizioni degli eventi nell'API admin: il modello potrebbe copiarne il testo nel `summary` (promemoria di T34);
  - per ogni scenario annotare strumenti usati, proposta sì o no, chiamate e secondi (il log del backend ha una riga per turno, senza testi).
- **T38 (rilascio v2):**
  - `docs/rilascio-v2.md` sul modello di `rilascio-v1.md`, con il flusso F6 completo online;
  - online la prova la fa l'utente con il suo account (niente account di prova online);
  - README con la chat, i limiti di D129 e il modello gratuito di terzi (D126);
  - tag `v2.0.0` dopo la checklist.
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).

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
   - modelli gratuiti che possono sparire o rispondere 429 (D126);
   - più account possono finire le 40 chiamate dell'app (D129);
   - i passaggi si perdono ricaricando la pagina (D130).
8. `ANTHROPIC_MODEL` su Render non serve più: si può togliere (Claude in Chrome, "Save only").
9. Quota gratuita e scenari di T37: vedi sopra.

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container può fermarsi da solo (per esempio dopo un riavvio di Docker): se i test non trovano il database, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`;
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
  - ricaricare la pagina dentro uno script interrompe lo script: ricaricare con `navigate`, poi leggere con uno script nuovo.
- **Render:**
  - servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti; un deploy dura circa 4 minuti, il sito statico circa 1;
  - secondo lo staff di Render (forum) una richiesta HTTP può durare fino a 100 minuti: il turno dell'agente (al massimo 3 minuti) ci sta;
  - salvare le variabili d'ambiente con "Save only" (menu accanto a "Save, rebuild, and deploy"); salvare può comunque far ripartire l'ultima versione;
  - con la scheda di Chrome nascosta i clic non aprono i menu: "Manual Deploy" si apre con `button.click()` via JavaScript.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
