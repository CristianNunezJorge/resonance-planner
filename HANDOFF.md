# Handoff · giorno 10, seconda parte (T38)

Passaggio di consegne del 20/10/2026. T37 è chiuso; T38 (rilascio v2) è a metà: documenti pronti, manca la checklist online fatta dall'utente.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Riprendiamo T38 sul ramo docs/release-v2-t38: ti dico l'esito della checklist di docs/rilascio-v2.md fatta sul sito online.
Poi compila l'esito, proponimi il commit e chiudi T38 (PR, squash merge, tag v2.0.0) e il giorno 10.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-9** e T37 del giorno 10 (T01-T37), su `main` e online |
| Rilascio | **v1** con tag `v1.0.0`; la **v2** si chiude con T38 e il tag `v2.0.0` |
| Ultimo commit su `main` | `8bd8032`, T37 (PR #47) |
| Ramo in corso | `docs/release-v2-t38`, commit `6bb388b`: `docs/rilascio-v2.md` (9 punti, esito da compilare) e README della v2. Pushato, senza PR |
| Test | Backend 860 (`./mvnw verify`, copertura 98,4%), frontend 539 (`npm test`) |
| Decisioni | D1-D132, tutte *Confermate* (D131 e D132 nate in T37) |
| Agente | Modello **gratuito** `nvidia/nemotron-3-ultra-550b-a55b:free` (D132) al posto di Super, il cui unico fornitore il 20/10 è rimasto fuori servizio per ore (404). Super e `google/gemma-4-31b-it:free` sono le alternative. Limiti di D129 invariati |
| Render | `OPENROUTER_MODEL` = Ultra, deploy live dal 20/10 alle 14:48; chat online provata dall'utente: risponde (log: turni da 2 e 3 giri) |
| Trello | T37 in "Fatto", T38 in "In corso" |
| Quota OpenRouter | Il 20/10 (UTC) 36 richieste su 50 dopo le prove. Si azzera a mezzanotte UTC (02:00 in Italia) |
| Account di prova in locale | Tutti cancellati. Online non se ne creano |

## T38 · Cosa resta

1. L'utente fa sul sito online i 9 punti di `docs/rilascio-v2.md` con il suo account, la mattina, con la quota piena: la checklist usa 3 messaggi all'agente (9-18 richieste). Se il suo contatore mostra già più di 2 messaggi, va fatta il giorno dopo.
2. Si compila la sezione "Esito" (data, punti ✓ o ✗) e, se serve, si correggono i problemi trovati.
3. Commit approvato dall'utente, PR (titolo per esempio `docs: release v2 (T38)`), squash merge, ramo cancellato.
4. Tag `v2.0.0` su `main` dopo la checklist superata (D8), con `git tag -a` e push del tag.
5. Chiusura del giorno 10: T38 in "Fatto" su Trello, `HANDOFF.md` per i giorni 11-12 (margine ed extra, D90), `stato-del-progetto.md`, CLAUDE.md.

## Cosa ha fatto T37

- **Istruzioni di sistema** (`agent/AgentPrompt`): tre parti (dati e strumenti, risposte, sicurezza), testo semplice, strumento giusto per ogni richiesta, date dai giorni detti a parole, parametri tenuti nelle modifiche, niente motivi inventati, niente somme fatte dal modello. `AgentPromptTest` controlla nomi degli strumenti, assenza di markdown e lunghezza.
- **Chat:** la pulizia del testo toglie anche il corsivo con un asterisco (D130).
- **Scenari reali:** i dieci di `06-agente.md` passano, due con riserva; dati, criteri, risultati e conclusione in `docs/scenari-agente.md` (D131). Lezioni: il modello legge gli strumenti uno per giro (3-6 chiamate per un piano), a volte aggiunge un motivo non dato dagli strumenti, e un errore rimasto nello storico tende a ripetersi.
- **Modello:** D132, Nemotron Ultra predefinito, in locale (`.env`) e su Render.

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
