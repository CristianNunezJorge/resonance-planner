# Handoff · giorno 4

Passaggio di consegne del 14/10/2026, a fine giorno 3, per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Iniziamo il giorno 4 (T17-T21): Waveplate, roster, inventario e configurazione guidata.
Prima di scrivere codice riassumimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1, 2 e 3** (T01-T16), tutti su `main` e online; su Trello sono in "Fatto" |
| Ultimo commit su `main` | `4036886`, T16 (PR #19), più il commit di questa chiusura |
| Test | Backend 202 (`./mvnw verify`), frontend 151 con il 98% di copertura (`npm test`) |
| Decisioni | D1-D103, tutte *Confermate* |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Render, sito statico | Manda `Content-Security-Policy` (stessa stringa di `frontend/csp.ts` e D103), `X-Content-Type-Options: nosniff` e, da Render, `Strict-Transport-Security`. Le intestazioni stanno in Render > Headers, **non nel repository**: se il sito viene ricreato vanno rifatte |
| Trello | T01-T16 in "Fatto"; P01-P05 in "In verifica" (aspettano la revisione dell'utente) |
| Ramo in corso | nessuno: `main` è pulito |

## Giorno 4 · Waveplate, roster e configurazione (T17-T21)

Dettagli in `docs/09-task.md`.

| Card | Cosa | Decisioni |
|---|---|---|
| T17 | Calcolo di Waveplate e Crystals (test prima del codice) e API; aggiungere `WaveplateSnapshot` all'entità `User` (rinviato da T12) | D28, D36, D41 |
| T18 | Roster: regole tra livello e grado (test prima del codice) e API; le combinazioni non valide restituiscono 400 | D34 |
| T19 | API dell'inventario: quantità modificabile, mai negativa | D21 |
| T20 | API della configurazione guidata, in un'unica transazione; una richiesta non valida non salva nulla | D50 |
| T21 | Configurazione guidata in tre passi (frontend); flusso F3 completo fino al calendario | D20 |

## Come lavoriamo (flusso approvato, invariato)

Un ramo per card → test prima del codice → controlli locali (`./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`) → prova reale nel browser con strumenti testuali (niente screenshot) → revisione del codice con agenti (un revisore del linguaggio e uno di sicurezza, in parallelo) → l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove → push, PR senza firme di Claude, squash merge, ramo cancellato → controllo online.

- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente al commit.
- **Dopo ogni merge guardare Render > Events** che il deploy parta; se `gh pr merge` risponde 502/504 controllare lo stato reale (`gh api repos/CristianNunezJorge/resonance-planner/pulls/N`, `git ls-remote`) prima di ritentare.
- Online non si creano account di prova (vietato fuori da localhost): i flussi con registrazione si provano in locale con backend (jar) e frontend (`.claude/launch.json` ha le configurazioni `backend`, `frontend`, `frontend-prod`; prima `./mvnw verify` per rifare il jar). Il backend online blocca `localhost` per CORS.
- Le decisioni nate nel giorno 3: D99 (JJWT), D100 (regole di username, password e admin), D101 (limiti di richieste, con Cloudflare), D102 (profilo), D103 (sessione, accesso alle pagine, CSP).

## Cosa esiste già nel codice

- **Backend:** tutto quello del giorno 2 più `auth/` (registrazione, login, `JwtService`, filtro JWT), `user/` (profilo, `deleteAccount`), `RateLimitFilter` e `application-prod.yml` con i proxy fidati di Cloudflare, `FieldValidationException`, `UnauthorizedException`, `ConflictException(messaggio, campo)`, `OpenApiConfig` (pulsante "Authorize").
- **Frontend:** `features/auth/` (`authSlice`, `ProtectedRoute`, `GuestOnly`, `LoginPage`, `RegisterPage`, `crossTabSync`, `tokenStorage`, `navigation.ts` con `homePath`), `features/landing/` (`LandingPage`, `HowItWorks`, `CharacterCard`: pagina P1 completa), `components/` (`NavBar`, `Footer` con i crediti, `ToastContainer`, `FormField`, `ComingSoonPage` per le pagine ancora da fare), `RootLayout`, `csp.ts`.
- **Pagine segnaposto** (`ComingSoonPage`): `/configurazione`, `/calendario`, `/roster`, `/profilo`, `/admin/eventi`. Si sostituiscono card per card (T21, T24, T26, T27).

## Promemoria per le prossime card

Sono in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card". I più importanti per il giorno 4:

- **Ogni service che scrive dati dell'utente** (T17-T20) controlla che l'utente del token esista e altrimenti lancia `UnauthorizedException` (D102). Gli errori su un campo scoperti dal service si lanciano con `FieldValidationException`.
- **T17:** fuso `Europe/Rome` in configurazione (D41); `WaveplateSnapshot` va aggiunto all'entità `User` (con una migrazione `V2__...`: mai modificare una migrazione già unita).
- **Frontend (T21):** percorsi con `encodeURIComponent` per gli identificativi; ogni pagina chiama `usePageTitle`; notifiche con `toastShown`; moduli con `FormField` e `features/auth/formFailure.ts`.
- **Più avanti:** T23 (controllo ADMIN negli endpoint admin), T24 (`sessionEnded` azzera i dati dell'utente in Redux), T26 (la CSP `style-src 'self'` potrebbe bloccare gli stili di FullCalendar: provare `npm run preview`).
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Manutenzione programmata di Render il 15/10/2026 alle 03:00 (ora italiana): il backend può riavviarsi.

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente.
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101 (cloudflare.com/ips).

## Note pratiche

- **Su questo computer:** JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`), Docker Desktop acceso e `docker compose up -d` per il database sulla 5433. Se i comandi `docker` non rispondono, Docker Desktop è bloccato in avvio: Restart dal menu della balena, poi se serve `wsl --shutdown` da PowerShell e riaprire Docker Desktop.
- **Windows:** `pkill` non funziona per i processi Java (usare PowerShell `Stop-Process`); alcuni file hanno fine riga CRLF e gli script di sostituzione di testo falliscono in silenzio (usare lo strumento di modifica).
- **Render:** servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`. L'API al risveglio impiega circa 3 minuti. Le variabili `VITE_` entrano nel codice durante il build. Nella pagina Headers il pulsante "Add Rule" va premuto con un clic alle coordinate, non con il riferimento dell'elemento.
- **Trello:** descrizioni e spostamenti via JavaScript dalla pagina `trello.com`; per `PUT /1/cards/<shortLink>` serve il cookie `dsc` nel corpo form-encoded, altrimenti 403 "CSRF detected" (vedi `CLAUDE.md`, "Note operative"). Colonna "Fatto": `6abf8ab8f555b9b136580cc4`.
