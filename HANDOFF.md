# Handoff · giorno 4

Passaggio di consegne del 13/10/2026, a metà del giorno 3, per riprendere lo sviluppo in una nuova sessione di Claude Code.

## Messaggio da incollare nella nuova sessione

```text
Leggi HANDOFF.md, CLAUDE.md e docs/stato-del-progetto.md.
Riprendiamo dal giorno 3: prima i passi su Render (CSP e nosniff del sito statico), poi T16, poi la chiusura del giorno 3.
Prima di scrivere codice riassumimi il piano e chiedimi conferma.
```

## Situazione

| Cosa | Stato |
|---|---|
| Giorni completati | **1 e 2** (T01-T11) |
| Giorno 3 | **T12, T13, T14, T15 uniti su `main` e online**; restano T16 e la CSP su Render |
| Ultimo commit su `main` | `ea63a74`, T15 (PR #17) |
| Test | Backend 202 (`./mvnw verify`), frontend 145 con il 98% di copertura (`npm test`) |
| Decisioni | D1-D103; D99-D103 nate in questa sessione, tutte *Confermate* |
| App online | https://resonance-planner.onrender.com · API https://resonance-planner-api.onrender.com/swagger-ui.html |
| Trello | T12-T15 ancora da spostare in "Fatto" (T01-T11 già lì); P01-P05 in "In verifica" |
| Ramo in corso | `docs/handoff-day-4` (questi file di documentazione, **non ancora committati**) |

## Da fare per primo

1. **Committare questo handoff.** I file modificati sono `HANDOFF.md`, `CLAUDE.md` e `docs/stato-del-progetto.md` sul ramo `docs/handoff-day-4`. Proporre all'utente il messaggio (per esempio `docs: update handoff for day 4`), poi push, PR e squash merge.
2. **Controllare il deploy di T15 su Render.** Il sito statico `resonance-planner` deve avere fatto il deploy di `ea63a74` (Render > Events). Se non è partito (è già successo due volte con l'API, quando GitHub dava errori 502/504): Manual Deploy > Deploy latest commit.
3. **CSP e `nosniff` sul sito statico** (l'utente ha già autorizzato, chiedere conferma solo se cambia qualcosa). Con Claude in Chrome, sezione Headers del servizio `resonance-planner`, percorso `/*`:
   - `Content-Security-Policy`: `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self' https://resonance-planner-api.onrender.com; font-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` (stessa stringa di `frontend/csp.ts` e di D103)
   - `X-Content-Type-Options`: `nosniff`
   - Poi `curl -I` sulla pagina iniziale, su `/login` e su un file di `/assets/`: tutti e tre devono avere le intestazioni. Controllare anche se Render o Cloudflare mandano già `Strict-Transport-Security`.
   - Verificare che il sito funzioni ancora (caricamento dei personaggi, console senza violazioni). Se qualcosa si rompe, togliere l'intestazione e dirlo all'utente.
   - Le intestazioni non stanno nel repository: se si ricrea il sito statico vanno rifatte.
4. **T16 · Pagina di presentazione** (FE · D72): la frase che spiega l'app, i tre passi di "come funziona", i sei personaggi dal `catalogSlice`, i crediti, il pulsante "Prova la demo" verso `/registrati`. Sostituisce l'elenco di prova (`LandingPage`, `CharacterCard`) e la favicon di Vite. Se l'utente è già loggato, la pagina mostra "Vai al calendario" (o alla configurazione) al posto di "Prova la demo". Fatta quando: la pagina P1 mostra i personaggi del catalogo.
5. **Chiusura del giorno 3:** spostare T12-T16 in "Fatto" su Trello (fetch `PUT /1/cards/<shortLink>` dalla pagina `trello.com`, colonna "Fatto" `6abf8ab8f555b9b136580cc4`), aggiornare di nuovo `CLAUDE.md` e lo stato, preparare l'handoff del giorno 4.

## Come lavoriamo (flusso approvato, invariato)

Un ramo per card → test prima del codice → controlli locali (`./mvnw verify`; `npm run lint`, `typecheck`, `npm test`, `npm run build`) → prova reale nel browser con strumenti testuali (niente screenshot) → revisione del codice con agenti (un revisore del linguaggio e uno di sicurezza, in parallelo) → l'utente approva il messaggio di commit (solo l'oggetto, con il codice della card) e le decisioni nuove → push, PR senza firme di Claude, squash merge, ramo cancellato → controllo online.

- Le decisioni nuove vanno in `docs/decisioni.md` come *Proposta* e diventano *Confermata* con l'approvazione dell'utente al commit.
- **Dopo ogni merge guardare Render > Events** che il deploy parta; se `gh pr merge` risponde 502/504 controllare lo stato reale (`gh api repos/CristianNunezJorge/resonance-planner/pulls/N`, `git ls-remote`) prima di ritentare.
- Online non si creano account di prova (vietato fuori da localhost): i flussi con registrazione si provano in locale con backend (jar) e frontend (`.claude/launch.json` ha le configurazioni `backend`, `frontend`, `frontend-prod`; prima `./mvnw verify` per rifare il jar).
- Le decisioni nate in questa sessione: D99 (JJWT), D100 (regole di username, password e admin), D101 (limiti di richieste, con Cloudflare), D102 (profilo), D103 (sessione, accesso alle pagine, CSP).

## Cosa esiste già nel codice

- **Backend:** tutto quello del giorno 2 più `auth/` (registrazione, login, `JwtService`, filtro JWT), `user/` (profilo, `deleteAccount`), `RateLimitFilter` e `application-prod.yml` con i proxy fidati di Cloudflare, `FieldValidationException`, `UnauthorizedException`, `ConflictException(messaggio, campo)`, `OpenApiConfig` (pulsante "Authorize").
- **Frontend:** `features/auth/` (`authSlice`, `ProtectedRoute`, `GuestOnly`, `LoginPage`, `RegisterPage`, `crossTabSync`, `tokenStorage`), `components/` (`NavBar`, `Footer`, `ToastContainer`, `FormField`, `ComingSoonPage` per le pagine ancora da fare), `RootLayout`, `csp.ts`.
- **Pagine segnaposto** (`ComingSoonPage`): `/configurazione`, `/calendario`, `/roster`, `/profilo`, `/admin/eventi`. Si sostituiscono card per card (T21, T24, T26, T27).

## Promemoria per le prossime card

Sono in `docs/stato-del-progetto.md`, tabella "Promemoria per le prossime card". I più importanti:

- **Ogni service che scrive dati dell'utente** controlla che l'utente del token esista e altrimenti lancia `UnauthorizedException` (D102).
- **Endpoint admin (T23/T27):** controllare che l'utente esista e sia ancora ADMIN; il token di un admin eliminato resta valido 24 ore.
- **T24:** `sessionEnded` deve azzerare anche i dati dell'utente nello stato Redux (D103).
- **T26:** la CSP (`style-src 'self'`) potrebbe bloccare gli stili di FullCalendar: provare la build con `npm run preview` e, se serve, aggiornare D103.
- **T17:** fuso `Europe/Rome` (D41); `WaveplateSnapshot` va aggiunto all'entità `User` (rinviato da T12).
- **Database di Render: scade il 10/11/2026.** Ricrearlo prima e **cambiare anche `JWT_SECRET`** (D100).
- Manutenzione programmata di Render il 14/10/2026 alle 03:00 (ora italiana): il backend può riavviarsi.

## Questioni aperte

1. Revisione dei dati delle card P01-P05 da parte dell'utente.
2. Limitare l'accesso di Render su GitHub al solo repository; valutare se chiudere gli accessi esterni al database.
3. Strumento di analisi statica della settimana 16: non ancora indicato (D80).
4. Nome definitivo del progetto (provvisorio).
5. Controllare ogni tanto gli intervalli di Cloudflare in `application-prod.yml` e in D101 (cloudflare.com/ips).

## Note pratiche

- **Su questo computer:** JDK 21 nei comandi Maven (`export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`), Docker Desktop acceso e `docker compose up -d` per il database sulla 5433.
- **Windows:** `pkill` non funziona per i processi Java (usare PowerShell `Stop-Process`); alcuni file hanno fine riga CRLF e gli script di sostituzione di testo falliscono in silenzio (usare lo strumento di modifica).
- **Render:** servizi `resonance-planner` (sito statico), `resonance-planner-api` (Docker), `resonance-planner-db`. L'API al risveglio impiega circa 3 minuti. Le variabili `VITE_` entrano nel codice durante il build.
- **Trello:** descrizioni e spostamenti via JavaScript dalla pagina `trello.com` (vedi `CLAUDE.md`, "Note operative").
