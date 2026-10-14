# Handoff · giorno 5

Passaggio di consegne del 14/10/2026, a fine giorno 4, per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 5 (T22-T24): materiali mancanti e piano di farming, eventi, pagina roster e inventario.
Prima di scrivere codice riassumimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1, 2, 3 e 4** (T01-T21), tutti su `main` e online; su Trello sono in "Fatto" |
| Ultimo commit su `main` | `3a63fd7`, T21 (PR #25), più il commit di questa chiusura |
| Test | Backend 391 (`./mvnw verify`), frontend 275 con il 98% di copertura (`npm test`) |
| Decisioni | D1-D111, tutte *Confermate* (D104-D111 nate nel giorno 4) |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render | Sito statico con CSP e `nosniff` (Headers, fuori dal repository). Dopo ogni merge controllare Render > Events: l'auto-deploy a volte non parte, e allora Manual Deploy > Deploy latest commit |
| Account locale di prova | `prova_t16@example.com` (database locale `resonance_planner`), riportato a "configurazione da fare". La password è nella sessione di T16: si può ricreare un account dalla pagina di registrazione in locale |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 5 · Farming ed eventi (T22-T24)

Dettagli in `docs/09-task.md`.

| Card | Cosa | Decisioni |
|---|---|---|
| T22 | Calcolo dei materiali mancanti, delle run, delle Waveplate e dei minuti (test prima del codice) e API (`/api/me/roster/{id}/requirements`, `/api/me/farming-plan`) | D3, D27 |
| T23 | Eventi: date delle edizioni ricorrenti e giorni attivi (test prima del codice), API pubbliche (`/api/events`) e admin (`/api/admin/events`) | D12, D14, D29, D96 |
| T24 | Pagina roster e inventario (P5), `CharacterAvatar` con ripiego sulle iniziali | D30 |

## Cosa esiste già nel codice

- **Backend:** `auth/`, `user/` (con `WaveplateSnapshot` e `PlayTimeRules`), `waveplate/` (`WaveplateRules`), `roster/` (`OwnedCharacter`, `RosterRules`, `Progress`), `inventory/` (`InventoryItem` in sola lettura, `saveQuantity` con un upsert), `onboarding/`, `catalog/` (costi di ascensione, fonti di farming, `GameRules`), `event/` (entità senza API), `common/` (`FieldValidationException(Map)`, `UniqueViolation`, limiti di richieste, limiti del corpo JSON).
- **API pronte:** `/api/auth/**`, `/api/me` (profilo), `/api/me/waveplate`, `/api/me/roster`, `/api/me/inventory`, `/api/me/onboarding`, `/api/game/**` (compreso `/api/game/rules`).
- **Frontend:** `features/auth/`, `features/landing/`, `features/catalog/` (personaggi e regole), `features/onboarding/` (P3 completa), `components/` (`FormField`, `SelectField`, `LoadingState`, `ErrorState`...). Pagine ancora segnaposto (`ComingSoonPage`): `/calendario`, `/roster`, `/profilo`, `/admin/eventi` (T24, T26, T27).

## Come lavoriamo (flusso approvato, invariato)

Un ramo per card → test prima del codice → controlli locali (`./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`) → prova reale (backend con il jar; frontend con `frontend-prod`, che manda la CSP) con strumenti testuali (niente screenshot) → revisione del codice con agenti (revisore del linguaggio e revisore di sicurezza, in parallelo) → l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove → push, PR senza firme di Claude, squash merge, ramo cancellato → controllo online.

- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente al commit.
- Online non si creano account di prova (vietato fuori da localhost). Il backend online blocca `localhost` per CORS.
- Se l'utente rifiuta un agente, non rilanciarlo: dirgli cosa si fa al suo posto.

## Promemoria per le prossime card

Sono in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card". I più importanti per il giorno 5:

- **T22:** chi somma quantità (inventario, bisogno) usa `long` o `Math.addExact` (D108). Il fuso `Europe/Rome` (D41) non serve ancora: va aggiunto in T22-T23 quando serve "oggi". `RosterRules`, `Progress`, `rankOf` (frontend) e `ascension_cost` (costi per grado) sono la base del calcolo. I repository dei dati dell'utente estendono `Repository` e prendono l'utente.
- **T23:** gli endpoint admin controllano che l'utente esista e sia ancora ADMIN (`UserService.requireAdmin`), con un test del token di un utente eliminato (D102). `hibernate.default_batch_fetch_size` invece di molti `JOIN FETCH`.
- **T24:** `CharacterAvatar` con le iniziali; il `src` delle icone passa da una sola funzione `characterIconUrl` che accetta solo `cartella/nome.webp` (oggi `/${icon}` può diventare `//host`: `CharacterCard`, `CharacterChoice`). La bozza o i dati dell'utente in Redux si azzerano con `sessionEnded` e con ogni nuovo login e ignorano le risposte per `requestId` (schema di `onboardingSlice`).
- **T26:** la CSP (`style-src 'self'`) potrebbe bloccare gli stili di FullCalendar: provare `npm run preview` e, se serve, aggiornare D103.
- **T30:** `InventoryItemRepository.saveQuantity` non svuota il contesto di persistenza: rileggere l'inventario in una nuova transazione.
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Manutenzione programmata di Render il 15/10/2026 alle 03:00 (ora italiana): il backend può riavviarsi.

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente (in "In verifica" su Trello).
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101 (cloudflare.com/ips).
6. `GameRules` non controlla all'avvio che `ascensionLevels` sia crescente e finisca a `maxLevel` (con i valori attuali non serve).

## Note pratiche

- **Su questo computer:** JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`), Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Se i comandi `docker` non rispondono: Restart dal menu della balena, poi se serve `wsl --shutdown` da PowerShell e riaprire Docker Desktop. I test che fanno commit veri lasciano il database di test con il solo utente `admin`: se ne restano altri (una esecuzione interrotta) cancellarli con `docker exec resonance-planner-db psql -U resonance -d resonance_planner_test`.
- **Windows:** `pkill` non funziona per i processi Java (usare PowerShell `Stop-Process`); i file hanno fine riga CRLF e gli script di sostituzione di testo falliscono in silenzio (usare lo strumento di modifica o uno script Python che rispetta il fine riga). Un comando Bash con molti heredoc può non partire: scrivere i file con lo strumento di scrittura.
- **Browser di prova:** il pannello può non essere visibile e i clic reali (`computer`) falliscono: guidare la pagina con `javascript_tool` (script che impostano i campi controllati da React con il setter nativo e inviano `input`/`change`) e leggere il testo. Per i server di prova usare `preview_start` (`backend`, `frontend-prod`) e fermarli con `preview_stop` alla fine.
- **Render:** servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`. L'API al risveglio impiega circa 3 minuti. Nella pagina Headers il pulsante "Add Rule" va premuto con un clic alle coordinate.
- **Trello:** descrizioni e spostamenti via JavaScript dalla pagina `trello.com`; per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected". Colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
