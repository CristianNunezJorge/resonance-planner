# Qualità del codice e accessibilità

Controllo di T41 del 23/10/2026: copertura dei test, accessibilità, best practice e SEO con Lighthouse, peso del frontend e pulizia del codice. Gli obiettivi sono quelli di D70 (accessibilità WCAG 2.2 AA) e D79 (copertura almeno all'80% su service, calcoli e logica del frontend).

## In sintesi

| Cosa | Risultato |
|---|---|
| Copertura del backend | **98,2%** delle righe, 92,5% dei rami (948 test) |
| Copertura del frontend | **97,6%** delle righe, 92,5% dei rami (589 test) |
| Lighthouse, 9 pagine su computer e telefono | **100** in accessibilità, best practice e SEO su tutte e 18 le misure (prima: accessibilità da 94 a 100, SEO da 82 a 83); 100 anche sulle pagine pubbliche del sito online |
| JavaScript scaricato dalla prima pagina | da 679 KB a **434 KB** (compresso: da 207 a 134 KB) |
| Avvisi della build | nessuno (prima: l'agente di Mockito caricato da solo e il file JavaScript oltre i 500 KB) |

## Copertura dei test

**Backend:** JaCoCo, con `./mvnw verify` (report in `backend/target/site/jacoco/index.html`).

| Pacchetto | Righe | Rami |
|---|---|---|
| `agent` (client del modello, strumenti, ciclo, chat) | 98,0% | 88,4% |
| `auth` | 98,2% | 88,2% |
| `catalog` | 96,0% | 58,3% |
| `common` (errori, sicurezza, configurazione) | 97,6% | 88,5% |
| `event` | 99,4% | 96,3% |
| `farming` | 99,3% | 94,7% |
| `inventory` | 93,5% | 100% |
| `notification` | 99,0% | 95,6% |
| `onboarding` | 100% | 100% |
| `planner` | 99,0% | 94,8% |
| `planning` | 96,7% | 92,9% |
| `roster` | 98,1% | 100% |
| `seed` | 99,4% | 89,3% |
| `user` | 100% | 96,7% |
| `waveplate` | 100% | 100% |
| **Totale** | **98,2%** | **92,5%** |

Restano fuori il metodo `main` dell'applicazione, che i test non eseguono, e la chiamata vera a OpenRouter (`OpenRouterClientLiveTest`, spenta di default). I rami di `catalog` sono più bassi perché sono pochi (12) e quelli mancanti riguardano dati che il catalogo della demo non ha: un grado oltre i livelli di ascensione (`CharacterDetailResponse`) e alcuni controlli delle regole di gioco (`GameRules`, `GameRulesConfig`).

**Frontend:** Vitest con v8, con `npm run coverage` (report nel terminale).

| Cartella | Righe | Rami |
|---|---|---|
| `src/api` | 95,4% | 93,8% |
| `src/app` | 95,7% | 100% |
| `src/components` | 97,3% | 95,6% |
| `features/adminEvents` | 96,3% | 87,9% |
| `features/auth` | 98,0% | 95,5% |
| `features/calendar` | 98,5% | 92,7% |
| `features/catalog` | 98,0% | 79,2% |
| `features/chat` | 92,8% | 86,6% |
| `features/landing` | 100% | 94,6% |
| `features/onboarding` | 100% | 95,7% |
| `features/profile` | 98,0% | 95,9% |
| `features/progress` | 100% | 94,7% |
| `features/roster` | 98,1% | 91,8% |
| `features/waveplate` | 96,7% | 93,5% |
| **Totale** | **97,6%** | **92,5%** |

Le cartelle che mancano (per esempio `features/notifications`) sono al 100% in tutto: il report non le elenca.

## Lighthouse

**Come:** Lighthouse dentro Chrome DevTools, sulla build di produzione servita in locale con la CSP (`npm run build` e `npm run preview`) e il backend locale. Le pagine private sono state aperte con tre account di prova locali (un utente con dati e una proposta, un utente nuovo per la configurazione guidata, un admin), cancellati alla fine. Ogni pagina è misurata su computer e su telefono. Lighthouse in DevTools misura accessibilità, best practice e SEO; la categoria "Agentic Browsing" non è un obiettivo del progetto.

| Pagina | Computer prima | Computer dopo | Telefono prima | Telefono dopo |
|---|---|---|---|---|
| Presentazione `/` | 100 · 100 · 83 | 100 · 100 · 100 | 100 · 100 · 83 | 100 · 100 · 100 |
| Login | 100 · 100 · 82 | 100 · 100 · 100 | 100 · 100 · 82 | 100 · 100 · 100 |
| Registrazione | 100 · 100 · 82 | 100 · 100 · 100 | 100 · 100 · 82 | 100 · 100 · 100 |
| Configurazione guidata | 100 · 96 · 83 | 100 · 100 · 100 | 100 · 100 · 83 | 100 · 100 · 100 |
| Calendario (con proposta e chat) | 97 · 100 · 82 | 100 · 100 · 100 | 94 · 100 · 82 | 100 · 100 · 100 |
| Roster e inventario | 96 · 100 · 83 | 100 · 100 · 100 | 96 · 100 · 83 | 100 · 100 · 100 |
| Profilo | 96 · 100 · 82 | 100 · 100 · 100 | 96 · 100 · 82 | 100 · 100 · 100 |
| Gestione eventi (admin) | 96 · 100 · 82 | 100 · 100 · 100 | 96 · 100 · 82 | 100 · 100 · 100 |
| Pagina non trovata | 100 · 100 · 82 | 100 · 100 · 100 | 100 · 100 · 82 | 100 · 100 · 100 |

I tre numeri sono accessibilità · best practice · SEO. Con la pagina già aperta sono state misurate anche tre finestre: nuova attività e dettaglio di un evento nel calendario, nuovo evento nella gestione admin. Tutte danno 100 · 100 · 100.

**Cosa è stato corretto:**

| Problema trovato | Dove | Correzione |
|---|---|---|
| Contrasto 4,07:1 del testo rosso dei pulsanti "Elimina" e "Rimuovi" (`btn-outline-danger`) sullo sfondo blu notte, sotto il 4,5:1 di AA | Roster, profilo, gestione eventi | Testo e bordo con il rosso chiaro del tema scuro (`--bs-danger-text-emphasis`, circa 7:1), in `_elements.scss` |
| Barre degli eventi alte 22 px, sotto i 24 px dei bersagli di WCAG 2.2 (2.5.8) | Calendario su computer | Altezza minima di 24 px per eventi e attività, in `_calendar.scss` |
| Celle dei giorni con un nome diverso dal testo visibile, e un elenco (`role="list"`) con figli `button` | Calendario | FullCalendar da 7.1.1 a **7.1.3**: la 7.1.2 è dedicata all'accessibilità (tabelle con `role="cell"`, eventi dentro `role="listitem"`). Nessuna violazione della CSP: la libreria non inserisce stili |
| Ritratti dei personaggi (256 px) mostrati a 359 px | Configurazione guidata su computer | Sei colonne sugli schermi larghi, come nella pagina di presentazione |
| Manca la `meta description` | Tutte | Aggiunta in `index.html` |
| `robots.txt` non valido (il sito rispondeva con la pagina dell'app) | Tutte | Aggiunto `public/robots.txt` |

Lighthouse controlla in automatico solo una parte di WCAG. Il focus nelle finestre, il tasto Esc e gli annunci con `role="status"` sono coperti dai test dei componenti; uso da tastiera e lettore di schermo vanno ricontrollati a mano nella checklist finale (T42).

**Sul sito online** (23/10, dopo il deploy di T41), senza account, solo le pagine pubbliche:

| Pagina | Computer | Telefono |
|---|---|---|
| Presentazione `https://resonance-planner.onrender.com/` | 100 · 100 · 100 | 100 · 100 · 100 |
| Login | 100 · 100 · 100 | 100 · 100 · 100 |
| Registrazione | 100 · 100 · 100 | 100 · 100 · 100 |
| Pagina non trovata | 100 · 100 · 100 | 100 · 100 · 100 |

Online valgono anche la CSP e `nosniff` impostati su Render, e `robots.txt` risponde come testo semplice.

I report completi di Lighthouse (HTML e JSON, circa 1 MB l'uno) non sono nel repository: si rifanno con Lighthouse in Chrome DevTools sulle stesse pagine.

## Peso del frontend

Con D137 la pagina del calendario, con FullCalendar, il suo polyfill e il font delle icone, si scarica solo quando l'utente apre il calendario.

| File | Prima | Dopo |
|---|---|---|
| JavaScript della prima pagina | 679 KB (207 KB compressi) | 434 KB (134 KB compressi) |
| CSS della prima pagina | 322 KB (49 KB compressi) | 228 KB (32 KB compressi) |
| JavaScript del calendario | nel file unico | 246 KB (73 KB compressi), solo su `/calendario` |
| CSS del calendario | nel file unico | 94 KB (17 KB compressi), solo su `/calendario` |

La build non avvisa più di un file oltre i 500 KB.

## Pulizia del codice

- **Residui:** nessun `TODO`, `FIXME`, `console.log`, `System.out` o `printStackTrace` nel codice; ESLint e il controllo dei tipi non segnalano nulla; i 589 test del frontend non scrivono avvisi.
- **Codice non usato:** cercato con piccoli script (export del frontend non importati da nessun altro file, classi del backend non nominate da nessun altro file). Nel backend non ce ne sono; nel frontend tutte le funzioni e i tipi esportati sono usati almeno nel loro file, e alcuni tipi sono esportati solo per descrivere la forma dei dati del modulo.
- **File grandi:** il file di codice più lungo è `EventFormModal.tsx` con 485 righe, sotto il limite di 800.
- **Avvisi della build:** l'agente di Mockito ora si carica all'avvio dei test (`maven-dependency-plugin` e `-javaagent` in Surefire, accanto a quello di JaCoCo): spariscono gli avvisi "A Java agent has been loaded dynamically", e i test non si romperanno quando Java vieterà il caricamento dinamico. Restano due avvisi di Maven su repository esterni che rispondono 401 al download di un elenco: sono innocui e non dipendono dal progetto.
