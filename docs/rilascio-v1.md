# Rilascio v1 · checklist sul sito online

**Card:** T28 · **Decisione:** D8 (ogni rilascio funziona online prima del successivo) · **Riferimento:** checklist di prova manuale in `08-regole-di-lavoro.md`, punti 1-5 e 10-13 (i punti 6-9 riguardano piano base e agente, cioè la v2).

## Prima di iniziare

- **Sito:** https://resonance-planner.onrender.com. Apri prima https://resonance-planner-api.onrender.com/swagger-ui.html e aspetta che compaia: il backend gratuito si sveglia in circa tre minuti.
- **Account di prova:** registrane uno nuovo con un'email tua, per esempio un alias. Il punto 13 lo cancella, quindi alla fine non resta niente.
- **Admin:** per i punti 10 e 11 servono `ADMIN_EMAIL` e `ADMIN_PASSWORD` del servizio `resonance-planner-api` su Render (sezione Environment).
- **Esito:** scrivi ✓ se tutto va come previsto, altrimenti ✗ e cosa è successo.

## Punti da provare

| # | Cosa fare | Cosa deve succedere | Esito |
|---|---|---|---|
| 1 | Clic su **"Prova la demo"**, registrati e completa i tre passi della configurazione guidata, per esempio Jinhsi da 1 a 40, 120 Waveplate, 1 ora | Notifica *"Configurazione completata."* e arrivo al **calendario**, con gli eventi della settimana solo nei giorni in cui sono attivi | ✓ |
| 2 | **"Esci"**, poi **"Accedi"** con la password sbagliata; poi con quella giusta | Prima il messaggio *"Email o password non corrette."*; poi il calendario | ✓ |
| 3 | Nel riquadro **Waveplate**, **"Aggiorna"**: scrivi 200 e salva | Notifica *"Waveplate aggiornate."*; il riquadro mostra **200 / 240** e *"piene alle …"* circa 4 ore dopo l'ora attuale (40 Waveplate, una ogni 6 minuti) | ✓ |
| 4 | **Roster** → **"Aggiungi un personaggio"** (per esempio Yinlin), poi **"Modifica"** su un personaggio e cambia il livello attuale | Il personaggio nuovo compare con i suoi materiali mancanti; dopo la modifica cambiano la barra "Materiali", l'elenco "Mancano" e il **totale stimato** | ✓ |
| 5 | **Calendario** → **"+ Attività"**: crea un'attività; aprila e **"Modifica"** il titolo; aprila ed **"Elimina"** (conferma) | Notifiche *"Attività aggiunta: …"*, *"Attività modificata."*, *"Attività eliminata."*; l'attività compare, cambia e sparisce dal calendario | ✓ |
| 10 | **"Esci"** e accedi come **admin** (si apre la configurazione guidata, che per l'admin non serve: usa il link della barra) → **"Eventi"** → **"+ Nuovo evento"**: date fisse in questa settimana, solo sabato e domenica, tipo a piacere → **"Salva"**. Poi esci, accedi con l'account di prova e apri il **calendario** | L'evento compare nella tabella dell'admin (senza l'etichetta "Demo") e, nel calendario dell'utente, **solo sabato e domenica**. L'avviso di cambiamento (D23) arriva con la v2 | ✓ |
| 11 | Con l'account di prova (non admin), scrivi nell'indirizzo `/admin/eventi` | Si torna al **calendario**; nella barra non c'è il link "Eventi" | ✓ |
| 12 | Apri il calendario **da telefono** (o con gli strumenti per sviluppatori del browser a una larghezza sotto i 768 px) | Il calendario diventa un **elenco per giorno**; il riquadro Waveplate sta sopra. La chat a scomparsa arriva con la v2 | ✓ |
| 13 | **Profilo** → **"Elimina account"**: scrivi la tua email → **"Elimina definitivamente"**. Poi prova ad accedere di nuovo | Si torna alla presentazione con *"Account eliminato: …"*; il nuovo accesso risponde *"Email o password non corrette."* | ✓ |

**Pulizia:** accedi di nuovo come admin ed elimina l'evento creato al punto 10 (pulsante **"Elimina"** nella tabella).

## Esito

**16/10/2026 · tutti i punti ✓.** Prova fatta dall'autore del progetto sul sito online, con un account di prova creato per l'occasione e cancellato al punto 13, e con l'account admin di Render per i punti 10-11. Nessun problema trovato. Con questo esito si crea il tag `v1.0.0` su `main` (D8).
