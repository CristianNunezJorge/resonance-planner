# Handoff · giorno 6

Passaggio di consegne del 15/10/2026, a fine giorno 5, per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 6 (T25-T28): attività e calendario in backend, pagina calendario, profilo e gestione eventi, rilascio v1.
Prima di scrivere codice riassumimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1, 2, 3, 4 e 5** (T01-T24), tutti su `main` e online; su Trello sono in "Fatto" |
| Ultimo commit su `main` | `e7f5738`, T24 (PR #29), più il commit di questa chiusura |
| Test | Backend 496 (`./mvnw verify`, copertura 98,5%), frontend 336 con il 98,6% di copertura (`npm test`) |
| Decisioni | D1-D116, tutte *Confermate* (D112-D116 nate nel giorno 5) |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Sito statico con CSP e `nosniff` (Headers, fuori dal repository). Dopo ogni merge controllare Render > Events: l'auto-deploy a volte non parte, e allora Manual Deploy > Deploy latest commit. Se il deploy risponde "Service Unavailable", guardare prima https://status.render.com (il 14/10 un incidente ha fermato per ore i deploy dei servizi gratuiti) |
| Account locale di prova | Nessuno fisso: nelle prove reali si crea un account dalla pagina con una password casuale generata nel browser e lo si cancella alla fine (`DELETE /api/me`). L'admin locale entra con `ADMIN_EMAIL`/`ADMIN_PASSWORD` del `.env`, letti da uno script senza stamparli |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 6 · Calendario e rilascio v1 (T25-T28)

Dettagli in `docs/09-task.md`.

| Card | Cosa | Decisioni |
|---|---|---|
| T25 | Attività manuali, completamento con aggiornamento dell'inventario, endpoint del calendario (`/api/me/tasks`, `/api/me/calendar`) | D21, D24, D35, D51 |
| T26 | Pagina calendario P4: viste, eventi solo nei giorni attivi, attività, finestre di dettaglio, riquadro Waveplate, stati vuoti | D66, D67, D69, D96 |
| T27 | Pagine profilo (P6) e gestione eventi admin (P7) | D14, D42 |
| T28 | Rilascio v1: checklist manuale (punti 1-5 e 10-13), README aggiornato, tag `v1.0.0` | D8 |

## Cosa esiste già nel codice

- **Backend:** `auth/`, `user/` (con `requireAdmin` per gli endpoint admin), `waveplate/`, `roster/`, `inventory/`, `onboarding/`, `catalog/`, `farming/` (calcolo del farming, D112), `event/` (edizioni con `EventCalendar`, API pubbliche e admin, D113-D114), `common/` (`Today` per "oggi" in ora italiana, `ForbiddenException`, limiti di richieste e del corpo JSON).
- **API pronte:** `/api/auth/**`, `/api/me` (profilo, anche `PATCH` e `DELETE`), `/api/me/waveplate`, `/api/me/roster` (con `/{id}/requirements`), `/api/me/inventory`, `/api/me/onboarding`, `/api/me/farming-plan`, `/api/game/**`, `/api/events` (con `/{id}`), `/api/admin/events`.
- **Frontend:** `features/auth/`, `landing/`, `catalog/` (personaggi, regole e materiali), `onboarding/` (P3), `roster/` (P5 completa), `progress/` (`LevelPicker` e funzioni dei livelli), `components/` (`CharacterAvatar`, `ConfirmModal`, `EmptyState`, `FormField`, `SelectField`, `LoadingState`, `ErrorState`...). Pagine ancora segnaposto (`ComingSoonPage`): `/calendario`, `/profilo`, `/admin/eventi` (T26, T27).

## Come lavoriamo (flusso approvato, invariato)

Un ramo per card → test prima del codice → controlli locali (`./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`) → prova reale (backend con il jar; frontend con `frontend-prod`, che manda la CSP) con strumenti testuali (niente screenshot) → revisione del codice con agenti (revisore del linguaggio e revisore di sicurezza, in parallelo) → l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove → push, PR senza firme di Claude, squash merge, ramo cancellato → controllo online.

- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente al commit.
- Online non si creano account di prova (vietato fuori da localhost). Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.

## Promemoria per le prossime card

Sono in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card". I più importanti per il giorno 6:

- **T25:**
  - completare un'attività di farming aggiunge all'inventario la quantità media, con il moltiplicatore di un evento attivo quel giorno solo per le run entro il limite giornaliero (D21, D94). Le edizioni e i giorni attivi li dà `EventCalendar`, "oggi" lo dà `Today`;
  - `InventoryItemRepository.saveQuantity` non svuota il contesto di persistenza; chi somma quantità usa `long` o `Math.addExact` (D108);
  - l'intervallo del calendario è di 62 giorni al massimo, come `EventService.MAX_RANGE_DAYS`;
  - ogni service che scrive ha il suo `*CommitTest` e risponde 401 se l'utente del token non esiste più (D102).
- **T26:**
  - FullCalendar potrebbe inserire stili con JavaScript, e la CSP (`style-src 'self'`) li blocca: provare `npm run preview` subito e, se serve, aggiornare D103;
  - gli eventi arrivano con una voce per edizione (chiave `id` + `startDate`) e si disegnano solo nei giorni di `activeDates` (D96, D113);
  - per le conferme si usa `ConfirmModal`.
- **T27:**
  - la pagina admin usa il corpo e i nomi dei campi del 400 descritti in `docs/05-api.md` (eventi dell'admin);
  - `code` diverso da `null` vuol dire evento della demo, che se cancellato ricompare al riavvio (D39);
  - il profilo usa `PATCH /api/me` e `DELETE /api/me` (conferma scrivendo l'email, F12).
- **T28:** checklist sul sito online, README aggiornato (fonti, CC BY-SA del wiki, immagini di Kuro Games), tag `v1.0.0`.
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101 (cloudflare.com/ips).
6. `GameRules` non controlla all'avvio che `ascensionLevels` sia crescente e finisca a `maxLevel` (con i valori attuali non serve).
7. Limiti noti annotati nelle decisioni del giorno 5: catalogo del farming letto a ogni richiesta (D112), nessun tetto al numero di eventi (D113), nessun `@Version` sugli eventi (D114).

## Note pratiche

- **Su questo computer:**
  - JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`);
  - Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Se i comandi `docker` non rispondono: Restart dal menu della balena, poi se serve `wsl --shutdown` da PowerShell e riaprire Docker Desktop;
  - i test che fanno commit veri lasciano il database di test con il solo utente `admin`.
- **Windows:**
  - i file hanno fine riga CRLF: per le sostituzioni di testo usare lo strumento di modifica o uno script Python che rispetta il fine riga;
  - uno script lungo con heredoc in Bash può non partire: scriverlo in un file nello scratchpad e lanciarlo da lì.
- **Browser di prova:**
  - guidare la pagina con `javascript_tool` (setter nativo più eventi `input`/`change`/`focusout`) e leggere il testo;
  - i server si avviano con `preview_start` (`backend`, `frontend-prod`) e si fermano con `preview_stop`;
  - se la porta 5173 è occupata da un `vite preview` di un'altra sessione, si può usare quello (serve la `dist/` appena ricostruita), senza fermarlo.
- **Render:**
  - servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`;
  - l'API al risveglio impiega circa 3 minuti;
  - con la scheda di Chrome nascosta i clic non aprono i menu: "Manual Deploy" si apre con `button.click()` via JavaScript, e "Deploy latest commit" con la sequenza di eventi del puntatore sull'elemento `[role=menuitem]`.
- **Trello:**
  - descrizioni e spostamenti via JavaScript dalla pagina `trello.com`;
  - per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected";
  - colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
