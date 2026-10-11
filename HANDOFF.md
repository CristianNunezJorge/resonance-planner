# Handoff · giorno 3

Passaggio di consegne dell'11/10/2026, alla fine del giorno 2, per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 3 (card T12-T16 di docs/09-task.md).
Prima di scrivere codice riassumimi il piano del giorno 3, proponimi la libreria JWT e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Progettazione | Completa e approvata dallo staff: 98 decisioni (D1-D98) |
| Giorni completati | **1 e 2** (T01-T11), tutti uniti su `main` con una pull request per card |
| Test | Backend 64 (`./mvnw verify`), frontend 27 con il 97% di copertura (`npm test`) |
| App online | https://resonance-planner.onrender.com: oggi mostra i sei personaggi letti dal backend |
| API online | https://resonance-planner-api.onrender.com/swagger-ui.html |
| Trello | T01-T11 in "Fatto"; P01-P05 ancora in "In verifica" |

**Cosa esiste già nel codice:**
- **Backend:**
  - `ApiResponse`, gestione centrale degli errori, `AppProperties` con le variabili obbligatorie;
  - schema Flyway con 16 tabelle;
  - catalogo ed eventi demo caricati dai JSON all'avvio;
  - API pubbliche `/api/game/**`;
  - sicurezza di base: tutto ciò che non è pubblico risponde 401 nel formato `ApiResponse`, `/api/admin/**` è solo per ADMIN, CORS è attivo.
- **Frontend:**
  - client HTTP unico (`api/client.ts`);
  - `catalogSlice`;
  - `LoadingState` ed `ErrorState`;
  - tema scuro;
  - pagina di prova con i personaggi.

## Prima di iniziare

1. **Docker Desktop** acceso, poi `docker compose up -d`: PostgreSQL sulla porta 5433.
2. **JDK 21** nei comandi Maven. In Git Bash: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`. Il JDK globale del PC è il 25.
3. **Chrome** aperto con l'estensione Claude in Chrome e le sessioni di **Render** e **Trello** attive. Servono per verificare il deploy e spostare le card.
4. `git pull` su `main`.

## Come lavoriamo (flusso già approvato)

- **Per ogni card:**
  1. un ramo dedicato;
  2. test prima del codice (RED, poi GREEN);
  3. controlli locali;
  4. revisione del codice con un agente;
  5. l'utente approva il messaggio di commit: solo l'oggetto, con il codice della card, per esempio `feat: add JWT login (T12)`;
  6. push, pull request **senza firme né co-autori di Claude**, *squash merge* e cancellazione del ramo.
- **Dopo l'unione:** Render pubblica da solo `main`, e si verifica online.
- **Regole con l'utente:**
  - decisioni nuove in `docs/decisioni.md` con stato *Proposta*, finché l'utente non conferma;
  - chiedere prima di scaricare file;
  - niente screenshot quando l'utente guarda lo schermo;
  - i segreti li scrive sempre l'utente.

## Giorno 3: le cinque card

### T12 · Spring Security con JWT (BE · D10, D19, D44, D45)
- **Decisione da prendere per prima:** la libreria JWT, che nessun documento fissa. Da registrare come D99.
  - **Raccomandazione:** JJWT (`io.jsonwebtoken`), la libreria usata nel corso.
  - **Alternativa:** il resource server di Spring con Nimbus. È ufficiale, ma fuori dal programma.
- **Entità `User`** sulla tabella `users`:
  - email salvata in minuscolo e password con BCrypt;
  - ruolo, tempo di gioco, configurazione completata;
  - le Waveplate come `@Embeddable`, salvate tutte insieme (D36);
  - colonne intere mappate come `Integer` (D98).
- **Endpoint:** `POST /api/auth/register` (201, token e utente) e `POST /api/auth/login` (200).
- **Errori:** `CONFLICT` se l'email è già usata, `BAD_CREDENTIALS` con un messaggio unico.
- **Token:** dura 24 ore e contiene il ruolo.
- **Admin creato all'avvio** da `ADMIN_EMAIL`/`ADMIN_PASSWORD`, nello `StartupSeeder` (D19, D39).
- **Trappole già note:**
  - il filtro JWT si crea dentro `SecurityConfig`, **non** come `@Component`, altrimenti gira due volte;
  - con un token scaduto o non valido il filtro lascia passare senza utente, così il catalogo pubblico resta usabile;
  - va tolto l'`InMemoryUserDetailsManager` vuoto;
  - `JWT_SECRET` su Render ha 32 caratteri: la chiave HMAC va presa dai suoi **byte UTF-8**, non decodificata da base64.
- **Fatta quando:** ci sono i test di registrazione, login e accesso negato senza token, più un token USER che riceve 403 su `/api/admin/**`.
- **Da allineare:** il flusso F2 di `02-utenti-e-flussi.md` scrive "Email o password non corretti", mentre il codice usa "non corrette", che è la forma corretta perché entrambe le parole sono femminili. Va corretto il documento.

### T13 · Limiti di richieste (BE · D48)
- Un filtro con contatori in memoria, **senza librerie esterne** (D48):
  - login e registrazione: 10 tentativi ogni 15 minuti per IP;
  - tutto il resto: 120 richieste al minuto per IP;
  - il limite dell'agente arriva con T35.
- Il filtro lavora prima dei controller, quindi **scrive da solo** il 429 `RATE_LIMITED` nel formato `ApiResponse`.
- L'IP del client arriva dagli header del proxy di Render: `server.forward-headers-strategy: native` è già attivo in `prod`.
- **Fatta quando:** all'undicesimo tentativo la risposta è 429.

### T14 · API del profilo (BE · D42, D44)
- `GET /api/me`, `PATCH /api/me` (username o tempo di gioco, tra le opzioni di `GameRules`) e `DELETE /api/me` (204).
- L'utente si ricava sempre dal token, mai dall'URL.
- **Fatta quando:** dopo l'eliminazione i dati collegati non esistono più. La cascata nel database c'è già ed è testata (D42).

### T15 · Login e registrazione nel frontend (FE · D45, D73)
- Pagine `/login` e `/registrati`, con gli errori sotto i campi ricavati da `fields`.
- `authSlice` con il token nel `localStorage` (D45).
- `ProtectedRoute` (D73), barra di navigazione, footer con i crediti (Kuro Games, wiki CC BY-SA) e logout (F11).
- Il client HTTP accetta già il parametro `token`. Gli identificativi nei percorsi vanno codificati con `encodeURIComponent`.
- **Content-Security-Policy** sul sito statico, dalla sezione Headers di Render (D45).
- **Fatta quando:** i flussi F1, F2 e F11 funzionano.

### T16 · Pagina di presentazione (FE · D72)
- Contenuto:
  - la frase che spiega l'app;
  - i tre passi di "come funziona";
  - i sei personaggi presi dal `catalogSlice`, che c'è già;
  - i crediti;
  - il pulsante "Prova la demo" verso `/registrati`.
- Sostituisce l'elenco di prova di T11 e la favicon di Vite.
- **Fatta quando:** la pagina P1 mostra i personaggi del catalogo.

A fine giornata: aggiornare `CLAUDE.md` e `docs/stato-del-progetto.md`, spostare le card su Trello, preparare il handoff del giorno 4.

## Questioni aperte

1. **Libreria JWT:** da decidere all'inizio di T12 (D99).
2. **Revisione dei dati** delle card P01-P05, da parte dell'utente.
3. **Database di Render: scade il 10/11/2026.** Va ricreato prima, o comunque a meno di 30 giorni dal Demo Day (D82).
4. **Rifiniture facoltative su Render:** limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
5. **Strumento di analisi statica** della settimana 16: non ancora indicato (D80).
6. **Nome del progetto** provvisorio.

## Note pratiche

- **Render:**
  - **Servizi:** `resonance-planner` (sito statico), `resonance-planner-api` (Docker) e `resonance-planner-db` (PostgreSQL 16), tutti a Frankfurt e con piano gratuito.
  - **Avvio:** al risveglio il backend impiega circa 3 minuti, e il frontend lo segnala dopo 5 secondi.
  - **Piano dei nuovi servizi:** quello preselezionato è a pagamento, va scelto Free a mano.
  - **Variabili `VITE_`:** entrano nel codice durante il build, quindi dopo averle cambiate serve un nuovo deploy del sito statico.
  - **Segreti:** li scrive l'utente; `JWT_SECRET` e `ADMIN_PASSWORD` li ha generati Render.
- **Trello:** le card si spostano con `fetch('/1/cards/<shortLink>', { method: 'PUT' })` dalla pagina `trello.com`, usando il cookie `dsc`. La colonna "Fatto" ha id `6abf8ab8f555b9b136580cc4`.
- **Flyway:** una migrazione già su `main` non si modifica, se ne crea una nuova (`V2__...`). Se si cambia in locale una migrazione non ancora unita: `docker compose down -v`.
- **Dettagli tecnici** di Spring Boot 4, Jackson 3 e React Router 8: in `CLAUDE.md`, sezione "Note operative".
- **Promemoria delle card successive** (T17, T23, T24, T27, T30, X04): in `docs/stato-del-progetto.md`.
