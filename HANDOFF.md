# Handoff · giorno 14 (demo e rilascio finale)

Passaggio di consegne del 23/10/2026. Il giorno 13 è chiuso: T39 (README completo), T40 (Swagger) e T41 (pulizia, copertura e Lighthouse) sono su `main` e online. Restano T42 e T43.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 14 con T42 (copione della demo e checklist finale online).
Prima di scrivere proponimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-13** (T01-T41, X01, X02, X03), su `main` e online |
| Rilasci | **v1** (`v1.0.0`, 16/10) e **v2** (`v2.0.0`, 21/10). Il tag finale arriva con T43 |
| Ultimo commit su `main` | `4ebaafc`, T41 (PR #58), dopo T40 (`53b8428`, PR #57) e T39 (`bc162fe`, PR #56), poi la chiusura del giorno 13 |
| Ramo in corso | Nessuno |
| Test | Backend 948 (`./mvnw verify`, copertura delle righe 98,2%), frontend 589 (`npm test`, 97,6%) |
| Lighthouse | 100 in accessibilità, best practice e SEO su 9 pagine in locale (computer e telefono) e sulle pagine pubbliche online: `docs/qualita.md` |
| Decisioni | D1-D137, tutte *Confermate* |
| Agente | Modello **gratuito** `nvidia/nemotron-3-ultra-550b-a55b:free` (D132). Limiti di D129, più quelli degli ospiti (D134) |
| Render | Tutto online. `ANTHROPIC_MODEL` tolta il 23/10: le variabili del backend sono le 9 che il codice usa. Il database **scade il 10/11/2026** |
| Trello | T01-T41 e X01-X03 in "Fatto"; T42-T43 e X04-X05 in Backlog; P01-P05 in "In verifica" |
| Account di prova | Nessuno, né in locale né online |

## Giorno 14 · Cosa fare (09-task)

| ID | Card | Note per la card |
|---|---|---|
| T42 | Copione della demo, checklist finale sul sito online, prova cronometrata | Riusare `docs/rilascio-v1.md` e `docs/rilascio-v2.md`, più gli extra (Excel, "Prova la demo", notifiche) e i controlli che Lighthouse non fa: uso da tastiera e lettore di schermo (`docs/qualita.md`). Vedi i promemoria qui sotto |
| T43 | Rilascio finale con tag | I tag precedenti sono `v1.0.0` e `v2.0.0`. Aggiornare lo "Stato" del README, che oggi parla della v2 con tre extra |

X04 (GitHub Actions) e X05 (damage calculator) restano nel Backlog: si riprendono solo se l'utente lo decide.

## Cosa ha fatto il giorno 13

- **T39 · README completo (PR #56):**
  - quattro screenshot in `docs/screenshot/`, fatti in locale con un account di prova e una risposta vera dell'agente;
  - extra della v3, schema "Come funziona" in Mermaid, guida di avvio passo passo con PowerShell, tabella delle variabili, limiti nuovi, riassunto in inglese;
  - verificato con un clone pulito da GitHub e un database nuovo: l'app parte e i test passano;
  - dalla revisione: le variabili facoltative (`APP_RATELIMIT_*`, `APP_AGENTLIMITS_*`) funzionano solo come vere variabili d'ambiente, **nel `.env` vengono ignorate** (provato).
- **T40 · Swagger (PR #57, D136):**
  - descrizione generale in `OpenApiConfig` (token e "Authorize", formato `ApiResponse`, codici di errore, limiti, date, rimando a `05-api.md` per `/ws`);
  - `@Schema(example)` su tutti i campi dei DTO, `@Parameter` sui parametri, nomi distinti per gli schemi annidati degli eventi, risposte in `application/json`;
  - `OpenApiDocumentationTest`: tutti gli endpoint documentati, ogni campo con un esempio del tipo giusto, e gli esempi mandati come richieste vere che il server accetta.
- **T41 · Qualità (PR #58, D137):**
  - Lighthouse da 94-100 / 82-83 a 100 ovunque: contrasto dei pulsanti `btn-outline-danger`, eventi del calendario alti almeno 24 px, FullCalendar **7.1.3** (correzioni di accessibilità della 7.1.2), `meta description`, `robots.txt`, griglia dei ritratti;
  - calendario caricato solo quando serve (`LazyCalendarPage`): la prima pagina scarica 434 KB di JavaScript invece di 679; la pagina d'errore torna alla presentazione con una ricarica completa;
  - agente di Mockito caricato da Surefire: niente più avvisi;
  - riepilogo in `docs/qualita.md`.

## Come lavoriamo (flusso approvato, invariato)

1. Un ramo per card.
2. Test prima del codice.
3. Controlli locali: `./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`.
4. Prova reale, con strumenti testuali e niente screenshot (gli screenshot del README sono un'eccezione chiesta dall'utente):
   - backend con il jar;
   - frontend con `frontend-prod`, che manda la CSP. Prima serve `npm run build`;
   - le chiamate vere al modello solo con il permesso dell'utente.
5. Revisione del codice con agenti: revisore del linguaggio e revisore di sicurezza, in parallelo.
6. L'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove.
7. Push, PR senza firme di Claude, squash merge, ramo cancellato.
8. Controllo online, e prima ancora in Render > Events che il deploy sia partito (il 23/10 sono partiti da soli).

Altre regole:
- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente.
- Online non si creano account di prova; un ospite di "Prova la demo" solo con il permesso dell'utente, eliminato alla fine. Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.
- L'agente deve restare gratuito: non proporre modelli a pagamento.
- Prima di scaricare pacchetti o cambiare una versione fissata in una decisione, chiedere (il 23/10 per FullCalendar 7.1.3).

## Promemoria

- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Se il modello dell'agente smette di rispondere: guardare nei log di Render la riga `OpenRouter answered …` e lo stato del fornitore con `GET https://openrouter.ai/api/v1/models/<id>/endpoints` (gratuito, senza chiave), poi cambiare `OPENROUTER_MODEL` (D132).
- **Copione della demo (T42), prima di presentare:**
  1. svegliare il backend (circa 3 minuti);
  2. fare una **esportazione Excel di prova**: la prima dopo un avvio impiega circa 14 secondi (D133);
  3. aprire una volta il calendario: il suo file si scarica solo allora (D137);
  4. se si mostra "Prova la demo", farlo dopo il risveglio: l'ospite si elimina dal profilo o sparisce da solo dopo 24 ore (D134);
  5. Swagger online ha ora esempi pronti per "Try it out" (D136): gli esempi della registrazione (`rover`, `rover@example.com`) sono uguali per tutti, quindi un secondo tentativo dà 409.
- **Notifiche nella demo (D135):**
  - si vedono solo con la pagina visibile;
  - le Waveplate si mostrano salvando dal riquadro una lettura tra 230 e 239: la notifica arriva entro un minuto, una sola volta per lettura;
  - "Gli eventi sono cambiati" si mostra con due finestre: l'admin modifica un evento in una, l'utente lo vede nell'altra, con account diversi in browser o finestre private diverse;
  - una notifica dura 6 secondi.
- Il punto del contatore a 5 / 5 messaggi non si raggiunge con la checklist: per vederlo si scrive il contatore in locale con `psql` (vedi le note pratiche).
- Per alzare un limite in locale (per esempio i messaggi dell'agente) serve una vera variabile d'ambiente: `APP_AGENTLIMITS_DAILYMESSAGES=10 ./mvnw spring-boot:run`. Nel `.env` viene ignorata.

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101.
6. Limiti noti annotati nelle decisioni:
   - catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114), la bozza dell'evento si perde chiudendo la finestra (D122);
   - limiti del pianificatore (D123), impronta di D125 calcolata dopo la bozza (promemoria di T33);
   - modelli gratuiti che possono sparire, cadere o rispondere 429 (D126, D132);
   - più account possono finire le 40 chiamate dell'app (D129);
   - i passaggi si perdono ricaricando la pagina (D130);
   - il `summary` della proposta lo scrive il modello: rischio residuo accettato, tetto a 2.000 caratteri (06-agente.md, "Sicurezza");
   - export Excel: il file è una fotografia, la prima esportazione dopo un avvio è lenta (D133);
   - ospiti: rischi accettati in D134;
   - notifiche: solo a pagina visibile, chiusure del server perse su Render, nessun tetto alle connessioni (D135);
   - esempi di Swagger composti campo per campo, a volte non del caso dell'endpoint (D136);
   - se il file del calendario non si scarica compare la pagina d'errore al posto di tutta la pagina; doppio "Caricamento…" alla prima apertura (D137).

La questione del bundle di FullCalendar (D119) è chiusa da D137; `ANTHROPIC_MODEL` su Render è stata tolta.

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container si ferma dopo un riavvio del computer: se il backend esce subito con codice 1, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - **in background** (`run_in_background`) `./mvnw` può non essere trovato: usare `cd "C:/Users/.../backend" && sh ./mvnw verify`;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`;
  - se i comandi `docker` si bloccano mentre il database risponde sulla 5433, per scrivere nel database locale c'è il client del PostgreSQL 18 installato: `"/c/Program Files/PostgreSQL/18/bin/psql.exe" -h localhost -p 5433 -U "$DATABASE_USERNAME" -d resonance_planner`, con `DATABASE_USERNAME` e `PGPASSWORD` presi dal `.env` senza stamparli;
  - **Norton** ispeziona HTTPS e rifirma i certificati: Java non se ne fida. Per chiamare OpenRouter da Java in locale serve `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (già nella configurazione `backend` di `.claude/launch.json`); con Maven si passa con `JAVA_TOOL_OPTIONS`. Su Render non serve.
- **Swagger (D136):** `OpenApiDocumentationTest` fa fallire la build se un endpoint o un campo nuovo non ha un esempio, o se un esempio viene rifiutato dal server. Un DTO di richiesta nuovo va aggiunto all'elenco `REQUEST_BODIES` del test. I controller che esistono solo nei test sono nascosti con `@Hidden` ed elencati in `TEST_CONTROLLERS`.
- **Lighthouse:** si fa con Chrome DevTools (MCP `chrome-devtools`, `lighthouse_audit` con `outputDirPath` nello scratchpad). Per le pagine private: account di prova creati via `fetch`, token in `localStorage` (`resonance-planner.token`), un contesto isolato per account; un admin si ottiene promuovendo un account con SQL e rifacendo il login. Lo script di riepilogo dei report si riscrive in pochi minuti (punteggi e controlli falliti da `report.json`).
- **Screenshot su file senza guardarli:** Chrome DevTools `take_screenshot` con `filePath` e `format: "webp"`.
- **Migrazioni:** una migrazione non ancora unita a `main` si può cambiare, ma i database locali la conoscono già con il checksum vecchio. Prima di riprovare: `drop table` della tabella nuova e `delete from flyway_schema_history where version = '…'` in entrambi.
- **OpenRouter:**
  - la chiave è nel `.env` (`OPENROUTER_API_KEY`) e su Render: Claude non la legge e non la scrive. Ha un limite di credito di pochi dollari e scade il 15/01/2027;
  - chiamata di prova vera del solo client (2 delle 50 richieste del giorno), dalla cartella `backend/`, in Git Bash:
    ```bash
    set -a; eval "$(grep -E '^OPENROUTER_(API_KEY|MODEL)=' ../.env | tr -d '\r')"; set +a
    JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT" OPENROUTER_LIVE_TEST=true ./mvnw -q test -Dtest=OpenRouterClientLiveTest -Djacoco.skip=true
    ```
  - le richieste gratuite rimaste oggi si leggono con `GET https://openrouter.ai/api/v1/key` e la chiave nell'header `Authorization` (campo `free_model_daily_requests`);
  - nelle impostazioni Privacy sono permessi i fornitori gratuiti che si addestrano sui dati, non quelli che pubblicano i prompt;
  - se un modello risponde sempre 429 con la quota intatta, provare l'altro modello gratuito (`OPENROUTER_MODEL`), poi i modelli con strumenti elencati da `https://openrouter.ai/api/v1/models`;
  - **prova di un limite senza consumare richieste:** scrivere la riga in `agent_model_calls` del database locale con `docker exec resonance-planner-db sh -c 'psql -U "$POSTGRES_USER" -d resonance_planner -c "..."'`, poi rimetterla a posto.
- **Prova della chat e delle notifiche in locale:** account di prova e configurazione guidata via `fetch` dalla scheda di `frontend-prod`, token in `localStorage`, poi `/calendario`. La chat si guida con `javascript_tool`; le notifiche brevi si leggono con un `MutationObserver` sugli elementi `.toast`. La scheda deve essere visibile.
- **Windows e script:**
  - i file hanno fine riga CRLF: per le sostituzioni usare lo strumento di modifica, o uno script Python che rispetta il fine riga;
  - **`sed -i` di Git Bash converte i CRLF in LF**: non usarlo sui documenti;
  - gli script Python con molte virgolette vanno scritti in un file e poi eseguiti: in linea nella shell possono rompersi.
- **Browser di prova:**
  - guidare la pagina con `javascript_tool` (setter nativo più eventi `input`/`change`) e leggere il testo;
  - FullCalendar disegna gli elementi solo se la scheda è **in primo piano**;
  - i server si avviano con `preview_start` (`backend`, `frontend-prod`) e si fermano con `preview_stop`;
  - ricaricare la pagina dentro uno script interrompe lo script: ricaricare con `navigate`, poi leggere con uno script nuovo;
  - per provare un download senza salvare file si sostituisce `HTMLAnchorElement.prototype.click` (e si cattura il `Blob` da `URL.createObjectURL`), poi si chiude la scheda;
  - in Chrome le schede del sito condividono il `localStorage`, quindi c'è la sessione dell'utente: per provare "Prova la demo" bisogna uscire dal suo account (con il suo permesso) oppure usare il browser integrato o un contesto isolato di DevTools;
  - online il WebSocket si prova anche dal terminale con il WebSocket di Node 24 (`node --use-system-ca script.mjs`); per "eventi cambiati" serve un admin, cioè un account promosso con SQL che rifà il login.
- **Render:**
  - nella pagina Environment i valori si caricano solo con "Show secret"; "Edit" apre la modifica, il pulsante rosso di una riga la segna da eliminare ("Restore" la riporta), e il menu "Choose an option" accanto a "Save, rebuild, and deploy" ha "Save only" (voce `[role=menuitem]` da premere con la sequenza `pointerdown`, `mousedown`, `pointerup`, `mouseup`, `click`). Dopo, ricaricare e ricontrollare;
  - il 23/10 "Save only" non ha avviato un deploy;
  - servizi `resonance-planner` (sito statico, `/static/srv-db5o6o0473hc73brh1eg`), `resonance-planner-api` (Docker, `/web/srv-db5p43ad0e5s7387vc4g`), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti; un deploy dura circa 4 minuti, il sito statico circa 1;
  - "Manual Deploy" si apre con `button.click()` via JavaScript;
  - le intestazioni del sito statico (CSP e `nosniff`) sono in `/static/<id>/headers`.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
