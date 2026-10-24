# Rilascio v3 · checklist finale sul sito online

**Card:** T42 · **Decisioni:** D8 (ogni rilascio funziona online), D90 (extra) · **Riferimento:** checklist di prova manuale in `08-regole-di-lavoro.md`, `rilascio-v1.md`, `rilascio-v2.md`, `qualita.md`.

La v3 aggiunge i tre extra (export in Excel, "Prova la demo", notifiche in tempo reale) e la rifinitura del giorno 13 (README, Swagger, accessibilità). Questa è anche la prova da fare prima del Demo Day (`08-regole-di-lavoro.md`), quindi ripete sul sito online tutti i 13 punti della checklist manuale. Poi prova gli extra e i controlli che Lighthouse non fa: tastiera e lettore di schermo.

## Prima di iniziare

- **Sito:** https://resonance-planner.onrender.com. Apri prima https://resonance-planner-api.onrender.com/actuator/health e aspetta `"status":"UP"`: il backend gratuito si sveglia in 3-4 minuti (213 secondi il 23/10).
- **Tre account:**
  - un **ospite** di "Prova la demo" per i punti 1, 4-11, 13 e 14: è un utente nuovo che non resta, perché il punto 13 lo elimina;
  - il **tuo account abituale** per i punti 2, 3, 12, 15, 18 e 19. È anche l'account della demo, quindi alla fine va rimesso come lo chiede `demo.md`;
  - l'**admin** per il punto 10, in un altro browser, così l'ospite resta collegato: `ADMIN_EMAIL` e `ADMIN_PASSWORD` del servizio `resonance-planner-api` su Render (sezione Environment).
- **Registrazione:** online non si creano account di prova. Il modulo di registrazione è stato provato online il 16/10 (`rilascio-v1.md`) e da allora non è cambiato. L'utente nuovo del punto 1 è l'ospite.
- **Quota dell'agente (D129, D134):** i punti 8 e 9 usano i **2 messaggi dell'ospite**, e il punto 9 prova anche il limite. Il tuo account non usa messaggi e li tiene per le prove della demo.
- **Excel:** il file si controlla nel browser (nome, tipo, fogli) senza salvarlo sul computer.
- **Esito:** ✓ se tutto va come previsto, altrimenti ✗ e cosa è successo.

## I 13 punti della checklist manuale

| # | Cosa fare | Cosa deve succedere | Esito |
|---|---|---|---|
| 1 | Dalla presentazione **"Prova la demo"**, poi la configurazione guidata: Jinhsi da 1 a 40, 120 Waveplate, 1 ora | Notifica *"Configurazione completata."* e arrivo al **calendario**, con gli eventi solo nei giorni in cui sono attivi; nella barra il nome con *"(ospite)"* | ✓ 23/10: Bountiful Crescendo arriva a sabato, gli altri due eventi a domenica, come i giorni attivi dell'API |
| 2 | Con il tuo account: **"Esci"**, poi **"Accedi"** con la password sbagliata; poi con quella giusta | Prima *"Email o password non corrette."*; poi il calendario | ✓ 24/10: password scritte dall'autore nel browser integrato |
| 3 | Con il tuo account, riquadro **Waveplate** → **"Aggiorna"**: 200 e salva | *"Waveplate aggiornate."*; **200 / 240** e *"piene alle …"* circa 4 ore dopo l'ora attuale | ✓ 24/10: salvato alle 13:21, *"piene alle 17:21"* |
| 4 | Ospite: **Roster** → **"Aggiungi un personaggio"** (Yinlin), poi **"Modifica"** su Jinhsi e cambia il livello attuale | Yinlin compare con i suoi materiali mancanti; dopo la modifica cambiano l'elenco "Mancano" e il **totale stimato**. La barra "Materiali" misura quanto c'è già nell'inventario: con l'inventario vuoto resta a 0% | ✓ 23/10: *"Yinlin è entrato nel roster."* e *"Jinhsi: livelli aggiornati."*; il totale passa da ~560 a ~520 Waveplate |
| 5 | Ospite: **Calendario** → **"+ Attività"**: crea un'attività, poi **"Modifica"** il titolo, poi **"Elimina"** (conferma) | *"Attività aggiunta: …"*, *"Attività modificata."*, *"Attività eliminata."*; l'attività compare, cambia e sparisce | ✓ 23/10 |
| 6 | Ospite: **"Genera piano base"**, poi **"Conferma"** nella barra | *"Piano base pronto: controllalo e confermalo."* con la barra **"Proposta in attesa"** e le attività **tratteggiate**; poi *"Piano confermato: le attività sono nel calendario."* e attività normali | ✓ 23/10: 26 attività dal 23/10 al 29/10 |
| 7 | Ospite: apri un'attività di **farming** di oggi → **"Segna come fatta"**, e di nuovo **"Segna come fatta"** nella conferma *"Segnare come fatta?"* | *"Attività fatta. Aggiunti all'inventario: …"*; in **Roster → Inventario** le quantità sono aumentate | ✓ 23/10: aggiunti 4 Premium, 20 Advanced e 9 Medium Resonance Potion e 14.400 Shell Credit, gli stessi numeri dell'inventario |
| 8 | Ospite, nella chat: suggerimento **"Organizzami la settimana"** | *"L'assistente sta lavorando…"*, poi una risposta in testo semplice con i **passaggi** e una **proposta tratteggiata**; il contatore passa a *"1 / 2 messaggi oggi"* | ✓ 23/10: risposta in circa 28 secondi, nessun HTML nel testo |
| 9 | Ospite: scrivi **"Non gioco sabato e domenica"**, invia, poi **"Conferma"** | La proposta nuova **sostituisce** quella di prima e non ha attività sabato e domenica. Il contatore è a **2 / 2**: il campo è disattivato e la chat dice *"Hai usato i 2 messaggi di oggi: puoi scrivere di nuovo da mezzanotte. Intanto puoi usare il piano base."* con **"Genera piano base"** | ✓ 23/10: risposta in circa 50 secondi, nessuna attività sabato 24 e domenica 25; disattivati anche "Invia" e i suggerimenti |
| 10 | Nell'altro browser accedi come **admin** → **"Eventi"** → **"+ Nuovo evento"**: date fisse su due giorni feriali dentro il piano del punto 9 → **"Salva"**. Guarda il calendario dell'ospite, con la scheda visibile | Per l'admin *"Evento creato."*, senza l'etichetta "Demo". Nel calendario dell'ospite, subito e senza ricaricare: notifica *"Gli eventi sono cambiati: il calendario è aggiornato."*, l'evento solo nei suoi giorni e l'avviso *"Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano."*. **"Chiedi all'agente"** è attivo solo se all'ospite restano messaggi oggi; **"Genera un nuovo piano"** funziona | ✓ 24/10: notifica e avviso subito dopo il salvataggio, evento solo lunedì 26 e martedì 27. "Chiedi all'agente" era attivo perché era un giorno nuovo (contatore a 0 / 2); "Genera un nuovo piano" ha dato *"Piano base pronto: controllalo e confermalo."* |
| 11 | Ospite: scrivi nell'indirizzo `/admin/eventi` | Si torna al **calendario**; nella barra non c'è il link "Eventi" | ✓ 23/10 |
| 12 | Calendario con una finestra larga meno di 768 px (o un telefono simulato) | Il calendario diventa un **elenco per giorno**, con il riquadro Waveplate sopra; il pulsante **"Agente"** apre la chat a scomparsa, **Esc** la chiude e il focus torna sul pulsante | ✓ 24/10: con l'account dell'autore nel browser integrato, telefono simulato a 375 × 812 |
| 13 | Ospite: **Profilo** → **"Elimina account"**: scrivi il nome dell'ospite → **"Elimina definitivamente"**. Poi apri `/calendario` | Si torna alla presentazione con *"Account eliminato: …"*; `/calendario` porta alla pagina di accesso | ✓ 24/10: *"Account eliminato: i tuoi dati sono stati cancellati."* |

Il punto 14 si fa con l'ospite prima del punto 13, quando ha ancora un piano confermato.

## Extra e controlli in più

| # | Cosa fare | Cosa deve succedere | Esito |
|---|---|---|---|
| 14 | Ospite, prima del punto 13: **"Esporta in Excel"** | Il pulsante diventa *"Esportazione…"*, poi *"Piano esportato."*; il file si chiama `resonance-planner_<da>_<a>.xlsx` e ha i fogli **"Piano"** ed **"Eventi"**. La prima esportazione dopo un avvio può impiegare circa 15 secondi (D133) | ✓ 23/10: 15,9 secondi, la prima dopo il risveglio; fogli controllati nel browser, senza salvare il file |
| 15 | Con il tuo account, calendario visibile: **"Aggiorna"** le Waveplate a **235** | Entro un minuto la notifica *"Waveplate quasi piene: 235/240, piene alle …"*, una sola volta. Poi rimetti **120** | ✓ 24/10: *"Waveplate quasi piene: 235/240, piene alle 13:52."* entro 55 secondi; nessun'altra in 154 secondi; poi rimesse a 120 |
| 16 | Swagger online: apri `GET /api/game/characters` → **"Try it out"** → **"Execute"** | Risposta 200 con i sei personaggi; in cima la descrizione generale con "Authorize" e i codici di errore (D136) | ✓ 24/10: 200 con Aalto, Changli, Danjin, Jinhsi, Lingyang e Yinlin; 41 endpoint |
| 17 | Swagger online: apri lo schema di `POST /api/auth/register` | Ogni campo ha un esempio (`rover`, `rover@example.com`). Non premere "Execute": gli esempi sono uguali per tutti e creerebbero un account | ✓ 24/10: esempio con `rover`, `rover@example.com` e una password di prova |
| 18 | **Tastiera**, con il tuo account. Dalla barra dell'indirizzo premi **Tab**; poi **Invio** su "Vai al contenuto", Tab fino a **"+ Attività"** e **Invio**; nella finestra Tab e Maiusc+Tab; poi **Esc**. Infine Tab fino al campo della chat | Il primo Tab mostra **"Vai al contenuto"** e il focus si vede sempre. Nella finestra il focus resta dentro, Esc la chiude e il focus torna su "+ Attività". Al campo della chat si arriva senza mouse | ✓ 24/10: tasti veri nel browser integrato, largo 903 px; ogni pulsante con l'anello del focus; la chat si raggiunge dal pulsante "Agente" (29 Tab dopo "+ Attività") |
| 19 | **Lettore di schermo**: Assistente vocale di Windows (**Ctrl+Win+Invio**), con il tuo account, sul calendario. Leggi il titolo della pagina, la barra dei comandi, il riquadro Waveplate e la chat; salva le Waveplate a 120 | Si sentono il titolo della scheda, i nomi dei pulsanti (anche "Periodo precedente" e "Periodo successivo"), i valori delle Waveplate, e la notifica *"Waveplate aggiornate."* senza spostare il focus | ✓ 24/10: provato dall'autore nel browser integrato |

**Pulizia:**
- con l'admin, elimina l'evento del punto 10 (pulsante **"Elimina"** nella tabella);
- l'ospite è già eliminato al punto 13;
- il tuo account deve tornare come lo chiede `demo.md`: Waveplate a 120, nessuna proposta in attesa.

## Esito

**23-24/10/2026 · tutti i 19 punti ✓.** Nessun problema trovato nell'app.

- **Come:** Claude ha guidato il browser con strumenti testuali, senza screenshot; l'autore ha scritto le password (punti 2 e 10) e ha fatto il punto 19. L'ospite `ospite-55tyos` ha usato i suoi 2 messaggi con l'agente ed è stato eliminato al punto 13; l'evento di prova del punto 10 è stato eliminato subito dopo.
- **Tempi misurati:** risveglio del backend in 213 e 159 secondi; risposte dell'agente in circa 28 e 50 secondi; prima esportazione Excel in 15,9 secondi; notifica delle Waveplate entro 55 secondi.
- **Corretto nel documento durante la prova:**
  - la barra "Materiali" del roster misura l'inventario, quindi con l'inventario vuoto resta a 0% (punto 4);
  - "Segna come fatta" chiede una conferma (punto 7);
  - "Chiedi all'agente" dopo un cambiamento è attivo se restano messaggi oggi: il punto 10 è stato fatto il giorno dopo i punti 8-9, con il contatore di nuovo a 0 / 2.
- **Fatto in un altro modo:** la finestra di Chrome usata per la prova non si lasciava ridimensionare, quindi il punto 12 è stato fatto con il telefono simulato del browser integrato e l'account dell'autore.
- **Rispetto alla v2:** il limite dei messaggi, saltato il 21/10 (`rilascio-v2.md`, punto 8), è provato online con l'ospite a 2 / 2 (punto 9).

Con questo esito si crea il tag finale su `main` (T43, D8).
