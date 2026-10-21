# Rilascio v2 · checklist sul sito online

**Card:** T38 · **Decisione:** D8 (ogni rilascio funziona online prima del successivo) · **Riferimento:** checklist di prova manuale in `08-regole-di-lavoro.md`, punti 6-10 e 12, e flusso F6 in `02-flussi.md`.

La v2 aggiunge il piano base, le proposte, l'avviso di cambiamento e l'agente con la chat. I punti 1-5, 11 e 13 sono quelli della v1: sono stati superati il 16/10 (`rilascio-v1.md`), e il codice che provano è coperto dai test automatici di ogni pull request (D81).

## Prima di iniziare

- **Sito:** https://resonance-planner.onrender.com. Apri prima https://resonance-planner-api.onrender.com/swagger-ui.html e aspetta che compaia: il backend gratuito si sveglia in circa tre minuti.
- **Account:** il tuo account abituale, non un account di prova. Online non si creano account di prova; le prove con account nuovi sono state fatte in locale (`scenari-agente.md`).
- **Admin:** per il punto 6 servono `ADMIN_EMAIL` e `ADMIN_PASSWORD` del servizio `resonance-planner-api` su Render (sezione Environment).
- **Quota dell'agente (D129):** 5 messaggi al giorno per utente, e circa 10 messaggi al giorno in tutta l'app, perché il piano gratuito di OpenRouter dà 50 richieste. La checklist usa 3 messaggi: se il contatore ne mostra già più di 2, falla il giorno dopo.
- **Esito:** scrivi ✓ se tutto va come previsto, altrimenti ✗ e cosa è successo.

## Punti da provare

| # | Cosa fare | Cosa deve succedere | Esito |
|---|---|---|---|
| 1 | **Calendario** → **"Genera piano base"** | Notifica *"Piano base pronto: controllalo e confermalo."*; compare la barra **"Proposta in attesa"** e le attività proposte sono **tratteggiate** | ✓ |
| 2 | Nella barra della proposta, **"Conferma"** | Notifica *"Piano confermato: …"*; la barra sparisce e le attività diventano normali | ✓ |
| 3 | Apri un'attività di **farming** di oggi → **"Segna come fatta"** | Notifica *"Attività segnata come fatta. Aggiunti all'inventario: …"*; in **Roster → Inventario** le quantità sono aumentate | ✓ |
| 4 | Nella chat dell'agente (a destra), clic sul suggerimento **"Organizzami la settimana"** | Indicatore *"L'assistente sta lavorando…"*; poi una risposta in italiano, in testo semplice, con sotto i **passaggi** ("Ho calcolato il piano", "Ho preparato la proposta"); nel calendario la nuova **proposta tratteggiata**. Il contatore passa a un messaggio in più | ✓ |
| 5 | Scrivi **"Non gioco sabato e domenica"** e invia | Nuova risposta; la proposta nuova **sostituisce** quella di prima e non ha attività sabato e domenica. Poi **"Conferma"** | ✓ |
| 6 | **"Esci"**, accedi come **admin** → **"Eventi"** → **"+ Nuovo evento"**: date fisse da domani a dopodomani (giorni feriali, che sono nel piano del punto 5), tipo a piacere → **"Salva"**. Poi esci e accedi con il tuo account | Notifica *"Evento creato."*; nel calendario compare l'**avviso** *"Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano."* con **"Chiedi all'agente"** | ✓ |
| 7 | Clic su **"Chiedi all'agente"** | La chat riceve il messaggio *"Roster, obiettivi o eventi sono cambiati dopo il mio ultimo piano: aggiornalo."* e risponde con una proposta nuova, che tiene conto dell'evento | ✓ |
| 8 | Se il contatore è arrivato a **5 / 5** (altrimenti salta il punto) | Il campo della chat è disattivato, la chat dice che i messaggi di oggi sono finiti e offre **"Genera piano base"** | — (saltato: il contatore non è arrivato a 5 / 5) |
| 9 | Apri il calendario **da telefono** (o con gli strumenti per sviluppatori del browser a una larghezza sotto i 992 px) | In basso a destra il pulsante **"Agente"** apre la chat a scomparsa; **Esc** la chiude e il fuoco torna sul pulsante | ✓ |

**Pulizia:** nella barra della proposta del punto 7 scegli **"Scarta"** (o "Conferma", se il piano ti va bene), poi accedi come admin ed elimina l'evento creato al punto 6 (pulsante **"Elimina"** nella tabella).

## Esito

**21/10/2026 · tutti i punti ✓, il punto 8 saltato.** Prova fatta dall'autore del progetto sul sito online, con il suo account abituale e con l'account admin di Render per il punto 6; l'evento di prova è stato poi eliminato. Il punto 8 non si poteva provare, perché la checklist non porta il contatore a 5 / 5: il limite di messaggi è coperto dai test automatici (`AgentQuotaTest` nel backend, `ChatPanel.test.tsx` nel frontend). Nessun problema trovato. Con questo esito si crea il tag `v2.0.0` su `main` (D8).
