# Handoff · giorno 13 (rifinitura e rilascio finale)

Passaggio di consegne del 21/10/2026. Il giorno 12 è chiuso: il terzo extra della v3 (D90), **X03** notifiche in tempo reale (D135), è su `main` e online. Per D90 X04 e X05 si lasciano: i giorni 13 e 14 servono a T39-T43.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 13 con T39 (README completo).
Prima di scrivere proponimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1-12** (T01-T38, X01, X02, X03), su `main` e online |
| Rilasci | **v1** (`v1.0.0`, 16/10) e **v2** (`v2.0.0`, 21/10). Gli extra della v3 sono su `main`; il tag finale arriva con T43 |
| Ultimo commit su `main` | `4ea1b64`, nota su D135 (PR #54), dopo X03 (`116c005`, PR #53), poi la chiusura del giorno 12 |
| Ramo in corso | Nessuno |
| Test | Backend 941 (`./mvnw verify`, copertura 98,4%), frontend 586 (`npm test`) |
| Decisioni | D1-D135, tutte *Confermate* |
| Agente | Modello **gratuito** `nvidia/nemotron-3-ultra-550b-a55b:free` (D132). Limiti di D129, più quelli degli ospiti (D134) |
| Render | Tutto online. La CSP del sito statico ha anche `wss://resonance-planner-api.onrender.com` (D103 aggiornata, D135). Il database **scade il 10/11/2026** |
| Trello | T01-T38 e X01-X03 in "Fatto"; X04-X05 e T39-T43 in Backlog; P01-P05 in "In verifica" |
| Account di prova | Nessuno. L'ospite creato online il 21/10 per provare X03 è stato eliminato |
| Chrome dell'utente | Nessuna sessione del sito aperta: l'utente rientra da solo con email e password |

## Giorni 13-14 · Cosa fare (09-task)

| ID | Card | Note per la card |
|---|---|---|
| T39 | README completo: screenshot, link, avvio, variabili, test, limiti noti, crediti, riassunto in inglese | Citare gli extra della v3: export Excel con Apache POI (D133), "Prova la demo" (D134), notifiche in tempo reale con WebSocket (D135). Il WebSocket `/ws` non è un'API REST: il suo protocollo è in `docs/05-api.md` |
| T40 | Swagger: descrizioni ed esempi per ogni endpoint | `/ws` non compare in Swagger: basta un rimando a `05-api.md` nella descrizione generale |
| T41 | Pulizia del codice, copertura almeno all'80%, accessibilità con Lighthouse | Bundle di circa 670 KB per FullCalendar (questione aperta 6) |
| T42 | Copione della demo, checklist finale sul sito online, prova cronometrata | Vedi i promemoria qui sotto |
| T43 | Rilascio finale con tag | I tag precedenti sono `v1.0.0` e `v2.0.0` |

X04 (GitHub Actions) e X05 (damage calculator) restano nel Backlog: si riprendono solo se l'utente lo decide.

## Cosa ha fatto il giorno 12

- **X03 · Notifiche in tempo reale (D135, PR #53):** WebSocket semplice su `/ws`, senza STOMP né librerie nel frontend (scelta dell'utente). Il browser manda il token nel primo messaggio (`AUTH`), poi un `PING` ogni 25 secondi.
  - **Backend**, pacchetto `notification/`:
    - `NotificationRules` decide le notifiche: Waveplate a 60 minuti dal pieno o già piene, evento che finisce oggi o domani;
    - `Notifier` è il job di ogni minuto e fa il controllo subito dopo l'`AUTH`; dopo il commit di una modifica dell'admin avvisa tutti;
    - `NotificationSessions` è il registro delle connessioni, `SentNotices` la memoria delle notifiche mandate.
  - **Frontend:** `notificationSocket.ts` gestisce la connessione e la riconnessione con attese crescenti; `useNotifications`, nel layout, apre la connessione solo a pagina visibile. La CSP ha `wss://`.
- **Revisione:** tre agenti, nessun problema sopra MEDIUM. Corretti quattro difetti. Otto rischi annotati in D135 (scelta dell'utente), tra cui l'assenza di un tetto alle connessioni.
- **Online:** l'auto-deploy di #53 non è partito, quindi deploy manuale di backend e sito statico. Con un ospite (poi eliminato) le notifiche arrivano. **Su Render però le chiusure decise dal server non arrivano al browser**: il browser vede `1006` dopo 20 secondi. È annotato in D135 con PR #54 ed è accettato dall'utente. Le notifiche funzionano.

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
8. Controllo online, e prima ancora in Render > Events che il deploy sia partito.

Altre regole:
- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente.
- Online non si creano account di prova; un ospite di "Prova la demo" solo con il permesso dell'utente, eliminato alla fine. Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.
- L'agente deve restare gratuito: non proporre modelli a pagamento.
- Prima di togliere o annullare un lavoro, verificare la premessa con l'utente: il 21/10 voleva togliere le notifiche pensando che online non funzionassero, e invece funzionavano.

## Promemoria

- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Se il modello dell'agente smette di rispondere: guardare nei log di Render la riga `OpenRouter answered …` e lo stato del fornitore con `GET https://openrouter.ai/api/v1/models/<id>/endpoints` (gratuito, senza chiave), poi cambiare `OPENROUTER_MODEL` (D132).
- Per la checklist finale di T42 si possono riusare `docs/rilascio-v1.md` e `docs/rilascio-v2.md`. Il punto del contatore a 5 / 5 non si raggiunge con la checklist: per vederlo si scrive il contatore in locale con `psql` (vedi le note pratiche).
- **Copione della demo (T42), prima di presentare:**
  1. svegliare il backend (circa 3 minuti);
  2. fare una **esportazione Excel di prova**: la prima dopo un avvio impiega circa 14 secondi, perché POI carica le sue classi (D133);
  3. se nella demo si mostra "Prova la demo", farlo dopo il risveglio: l'ospite si elimina dal profilo o sparisce da solo dopo 24 ore (D134).
- **Notifiche nella demo (D135):**
  - si vedono solo con la pagina visibile;
  - le Waveplate si mostrano salvando dal riquadro una lettura tra 230 e 239: la notifica arriva entro un minuto (al giro del job), una sola volta per lettura;
  - "Gli eventi sono cambiati" si mostra con due finestre: l'admin modifica un evento in una, l'utente lo vede nell'altra. L'utente deve essere un account diverso, in un'altra finestra privata o in un altro browser, perché le schede dello stesso browser condividono la sessione;
  - una notifica dura 6 secondi.
- Dopo ogni merge controllare in Render > Events che il deploy parta: il 21/10 per #53 non è partito, ed è servito "Manual Deploy" > "Deploy latest commit" su entrambi i servizi.

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
   - ospiti: i rischi accettati dopo la revisione del 21/10 sono elencati in D134;
   - notifiche: solo a pagina visibile, chiusure del server perse su Render, nessun tetto alle connessioni e gli altri rischi accettati in D135.
8. `ANTHROPIC_MODEL` su Render non serve più: si può togliere.

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Il container può fermarsi da solo (per esempio dopo un riavvio di Docker): se i test non trovano il database, rilanciare `docker compose up -d`;
  - `./mvnw verify` non riesce a sostituire il jar se il backend acceso con `preview_start` lo sta usando: fermare prima il backend;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`;
  - il 21/10 i comandi `docker` (anche `docker exec`) si sono bloccati mentre il database continuava a rispondere sulla 5433. Per scrivere nel database locale senza Docker c'è il client del PostgreSQL 18 installato: `"/c/Program Files/PostgreSQL/18/bin/psql.exe" -h localhost -p 5433 -U "$DATABASE_USERNAME" -d resonance_planner`, con `DATABASE_USERNAME` e `PGPASSWORD` presi dal `.env` senza stamparli (come per la chiamata di prova di OpenRouter);
  - **Norton** ispeziona HTTPS e rifirma i certificati: Java non se ne fida. Per chiamare OpenRouter da Java in locale serve `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (già nella configurazione `backend` di `.claude/launch.json`); con Maven si passa con `JAVA_TOOL_OPTIONS`. Su Render non serve. Node si fida dell'archivio di Windows con `node --use-system-ca`. Norton non disturba i WebSocket: un server echo pubblico risponde normalmente.
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
- **Prova delle notifiche (D135):**
  - in locale come la chat: account di prova con Waveplate tra 230 e 239, token in `localStorage`, `/calendario`. La notifica arriva subito dopo l'apertura;
  - le notifiche brevi si leggono con un `MutationObserver` sugli elementi `.toast`, perché durano 6 secondi;
  - per "eventi cambiati" serve un admin: un secondo account promosso con SQL (`update users set role = 'ADMIN' …`), poi un nuovo login via `fetch`, perché il ruolo è nel token;
  - il job manda le notifiche degli eventi entro un minuto;
  - una notifica già mandata non si ripete per la stessa lettura: per rivederla si salva una lettura nuova (`PUT /api/me/waveplate`);
  - online il WebSocket si prova anche dal terminale con il WebSocket di Node 24 (`node --use-system-ca script.mjs`);
  - la scheda deve essere visibile (`document.visibilityState`), altrimenti l'hook non si collega.
- **Windows e script:**
  - i file hanno fine riga CRLF: per le sostituzioni usare lo strumento di modifica, o uno script Python che rispetta il fine riga senza convertirlo due volte;
  - **`sed -i` di Git Bash converte i CRLF in LF** in tutto il file (successo il 21/10 con `decisioni.md`): non usarlo sui documenti;
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
  - "Manual Deploy" si apre con `button.click()` via JavaScript; la voce "Deploy latest commit" (`[role=menuitem]`) vuole la sequenza `pointerdown`, `mousedown`, `pointerup`, `mouseup`, `click`;
  - le intestazioni del sito statico (CSP e `nosniff`) sono in `/static/<id>/headers`: il valore si cambia con il setter nativo e l'evento `input`, poi "Save Changes" ("Updated HTTP Headers successfully."). Dopo, controllare con `curl -I` la pagina iniziale, `/login` e un file in `/assets/`;
  - la pagina dei log disegna solo le righe visibili: per cercare un orario conviene filtrare il testo della pagina.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
