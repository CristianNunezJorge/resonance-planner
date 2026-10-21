# Handoff · giorni 11-12 (margine ed extra)

Passaggio di consegne del 21/10/2026. Il giorno 10 è chiuso: la **v2 è rilasciata**, con la checklist online superata e il tag `v2.0.0`. I giorni 11-12 servono al margine e agli extra (D90).

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo i giorni 11-12 con l'extra X01 (export del piano in Excel).
Prima di scrivere codice proponimi la piccola progettazione e la decisione nuova, e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-10** (T01-T38), su `main` e online |
| Rilasci | **v1** con tag `v1.0.0` (16/10) e **v2** con tag `v2.0.0` (21/10): checklist online superate (`docs/rilascio-v1.md`, `docs/rilascio-v2.md`) |
| Ultimo commit su `main` | `4dd1790`, T38 (PR #48), poi la chiusura del giorno 10 |
| Ramo in corso | Nessuno |
| Test | Backend 860 (`./mvnw verify`, copertura 98,4%), frontend 539 (`npm test`) |
| Decisioni | D1-D132, tutte *Confermate* |
| Agente | Modello **gratuito** `nvidia/nemotron-3-ultra-550b-a55b:free` (D132); alternative: Super e `google/gemma-4-31b-it:free`. Il 21/10 il fornitore era attivo e la checklist ha usato 3 messaggi. Limiti di D129 invariati |
| Render | `OPENROUTER_MODEL` = Ultra; tutto online. Il database **scade il 10/11/2026** |
| Trello | T01-T38 in "Fatto"; T39-T43 ed extra X01-X05 in Backlog; P01-P05 in "In verifica" |
| Account di prova | Nessuno, né in locale né online |

## Giorni 11-12 · Cosa fare (D90)

Non ci sono ritardi da recuperare: v1 e v2 sono uscite nei giorni previsti. Il tempo va agli extra, nell'ordine di D90. Con v1 e v2 complete, le loro card possono passare da Backlog a "Da fare" quando si iniziano.

| ID | Extra | Da decidere nella piccola progettazione |
|---|---|---|
| X01 | Export del piano in Excel | Apache POI, argomento della settimana 15 (nel programma del corso). Cosa esportare (attività confermate di un intervallo, la proposta in attesa?), endpoint sotto `/api/me` (D44), limite all'intervallo come per il calendario (62 giorni), colonne del foglio, download dal calendario: oggi `api/client.ts` legge solo risposte JSON |
| X02 | Accesso ospite "Prova la demo" | D15: utente GUEST temporaneo, stessa configurazione guidata, un job pianificato cancella gli ospiti. Da decidere se l'ospite può usare la chat: ogni ospite consumerebbe le 40 chiamate al modello dell'app (D129). Anche i limiti di registrazione (D101) e la pulizia dei dati collegati |
| X03 | Notifiche in tempo reale (Waveplate quasi piene, evento in scadenza) | WebSocket, settimana 18. Lavoro medio |
| X04 | GitHub Actions | Facoltativo, strumento nuovo (D81). I test hanno bisogno di `DATABASE_USERNAME` e `DATABASE_PASSWORD` come variabili d'ambiente |
| X05 | Damage calculator semplificato | Il più costoso: prima una piccola progettazione e dati che oggi non abbiamo (D1) |

Per ogni extra: decisione nuova in `docs/decisioni.md` come *Proposta*, piano riassunto e conferma dell'utente prima del codice, poi il flusso solito della card. Un extra che usa l'agente si prova con la quota piena.

Dopo gli extra, giorni 13-14: T39 README completo, T40 Swagger con descrizioni ed esempi, T41 pulizia, copertura e Lighthouse, T42 copione della demo e checklist finale online, T43 rilascio finale con tag.

## Cosa ha fatto T38

- **Checklist della v2** (`docs/rilascio-v2.md`): 9 punti sul sito online, fatti dall'utente con il suo account e con l'admin di Render. Tutti ✓; il punto 8 (contatore a 5 / 5) è stato saltato perché la checklist usa 3 messaggi, ed è coperto da `AgentQuotaTest` e `ChatPanel.test.tsx`. L'evento di prova è stato eliminato.
- **README** della v2: stato, funzioni nuove, agente gratuito su OpenRouter, numeri dei test.
- **Rilascio:** PR #48 con squash merge, tag annotato `v2.0.0` su `main`.
- Prima della checklist: backend già sveglio e fornitore di Ultra attivo con gli strumenti (`GET https://openrouter.ai/api/v1/models/<id>/endpoints`).

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
   - il `summary` della proposta lo scrive il modello: rischio residuo accettato, tetto a 2.000 caratteri (06-agente.md, "Sicurezza").
8. `ANTHROPIC_MODEL` su Render non serve più: si può togliere.

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
