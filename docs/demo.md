# Demo · copione di 3 minuti

**Card:** T42 · **Decisione:** D138 · **Dove:** sito online, https://resonance-planner.onrender.com

La presentazione al Demo Day dura **al massimo 3 minuti** e deve raccontare in sintesi il progetto con cinque contenuti:

1. il problema individuato;
2. l'obiettivo raggiunto;
3. gli strumenti e le tecnologie usate;
4. i dati chiave emersi;
5. il valore pratico del lavoro.

Il copione segue questi cinque punti, uno per scena. L'obiettivo raggiunto si **mostra** sul sito online: l'agente propone un piano e tu lo confermi. Gli altri punti si raccontano a voce, con una schermata di appoggio. Admin, notifiche, Excel e telefono non si mostrano: sono provati nella checklist finale, `rilascio-v3.md`.

Il copione funziona sia dal vivo sia in un video registrato. Il messaggio all'agente parte nei primi secondi, così la sua attesa (20-50 secondi nelle prove online del 23/10) passa mentre racconti il problema.

## Preparazione

### Una volta sola: l'account della demo

Usa il tuo account abituale, non un ospite: ha 5 messaggi al giorno per l'agente (l'ospite 2) e la configurazione è già fatta.

- **Roster:** due o tre personaggi con un obiettivo ancora da raggiungere, per esempio Jinhsi, Yinlin e Aalto verso il livello 40. Se tutti sono già al loro obiettivo, il piano ha solo le attività degli eventi.
- **Waveplate:** un valore sotto 180, per esempio 120. Così durante la demo non arriva la notifica "Waveplate quasi piene" (che parte da 60 minuti prima del pieno).
- **Calendario:** nessuna proposta in attesa e nessun piano già confermato in questa settimana, altrimenti il piano nuovo cambia. Nelle prove, alla fine premi **"Scarta"** invece di "Conferma": il calendario resta pronto per la prova successiva.

### 30 minuti prima

1. **Sveglia il backend:** apri https://resonance-planner-api.onrender.com/actuator/health e aspetta che la risposta contenga `"status":"UP"`. Dopo 15 minuti senza richieste il backend gratuito si spegne, e la riaccensione impiega 3-4 minuti (213 secondi il 23/10, D82).
2. **Controlla il modello:** apri https://openrouter.ai/api/v1/models/nvidia/nemotron-3-ultra-550b-a55b:free/endpoints. Se l'elenco degli `endpoints` è vuoto, il fornitore gratuito è caduto. In quel caso cambia `OPENROUTER_MODEL` su Render (D132): il deploy dura circa 4 minuti. Se non c'è tempo, usa il piano B dell'agente.
3. **Accedi** e apri il **calendario**: la prima apertura scarica il file del calendario (D137). Nel pannello **"Agente"** controlla il contatore *"… / 5 messaggi oggi"*: servono almeno **2** messaggi liberi, uno per la demo e uno di riserva.
4. **Fai un'esportazione di prova:** premi **"Esporta in Excel"**. La prima dopo un avvio impiega circa 15 secondi (D133); così, se te la chiedono, la successiva è rapida.
5. **Copia negli appunti** il messaggio della demo: *Organizzami la settimana: non gioco sabato e domenica*. Incollarlo è più rapido e sicuro che scriverlo. Se presenti o provi di sabato o di domenica, scegli un altro giorno libero, per esempio *non gioco mercoledì*: il piano parte da oggi, e con il fine settimana escluso la settimana sullo schermo resterebbe senza attività proposte.
6. **Prepara le schede**, nell'ordine:
   1. **Calendario** (`/calendario`), da cui parte la demo;
   2. **README su GitHub**, aperto sullo schema "Come funziona": https://github.com/CristianNunezJorge/resonance-planner#come-funziona.
7. **Prepara lo schermo:**
   - finestra grande, almeno 992 px di larghezza (sotto, la chat non sta accanto al calendario);
   - zoom tra 100% e 125%;
   - notifiche del sistema spente e le altre schede chiuse.

### 5 minuti prima

- Ricarica l'indirizzo di `/actuator/health`. Se risponde subito, il backend è sveglio.
- Torna sulla scheda del **calendario** e ricaricala.

## Copione

| Tempo | Contenuto | Sullo schermo |
|---|---|---|
| 0:00-0:30 | 1 · Il problema | Calendario: mandi subito il messaggio all'agente |
| 0:30-1:30 | 2 · L'obiettivo raggiunto | Risposta dell'agente, proposta tratteggiata, "Conferma" |
| 1:30-2:00 | 3 · Strumenti e tecnologie | README su GitHub, schema "Come funziona" |
| 2:00-2:35 | 4 · Dati chiave | Stessa scheda |
| 2:35-3:00 | 5 · Valore pratico | Torni al calendario confermato |

I testi da dire sono una traccia: usali con parole tue. Le frasi tra parentesi quadre si tagliano per prime se il tempo stringe. Ogni scena dice anche quali decisioni la spiegano, per le domande.

### 1 · Il problema (0:00-0:30)

**Fai:** sei sul **calendario**. Clic nel campo *"Scrivi all'agente…"*, incolla il messaggio (Ctrl+V) e premi Invio. Compare *"L'assistente sta lavorando… può richiedere fino a qualche minuto."*

**Dici:**
> Questo è Resonance Planner. Chiedo subito al mio agente di organizzarmi la settimana, e intanto vi racconto il problema. In Wuthering Waves, un gioco gacha, ogni settimana bisogna incastrare tre cose: eventi che durano pochi giorni, le Waveplate, cioè l'energia che si ricarica col tempo, e i materiali per far crescere i personaggi. Fare questi conti a mano è lento, ed è facile sprecare energia o perdere un evento.

**Decisioni:** D1 (calendario e agente al centro), D9 (dataset curato: 6 personaggi, 3 eventi).

### 2 · L'obiettivo raggiunto (0:30-1:30)

**Fai, quando arriva la risposta:**
- indica i **passaggi** sotto la risposta (*"Ho calcolato il piano"*, *"Ho preparato la proposta"*);
- indica la barra **"Proposta in attesa"** e le attività **tratteggiate**: sabato e domenica sono vuoti;
- premi **"Conferma"**: compare *"Piano confermato: le attività sono nel calendario."* e le attività diventano normali.

Se la risposta non è ancora arrivata, comincia dalla seconda frase (eventi e Waveplate) e torna alla chat quando compare.

**Dici:**
> L'obiettivo era un calendario che pianifica per te, con un agente che propone e una persona che decide. Ecco la risposta: l'agente ha letto i miei dati con cinque strumenti e ha chiesto il piano a un pianificatore scritto in Java; sotto ci sono i passaggi che ha fatto. [Gli eventi compaiono solo nei giorni in cui sono attivi, e le Waveplate le calcola il server.] Le attività proposte sono tratteggiate e il fine settimana è libero, come ho chiesto. Il modello non fa conti e non scrive nel calendario: niente entra finché non confermo.

**Decisioni:** D2 (l'agente propone, l'utente conferma), D3 (i calcoli li fa Java), D56 (il modello non scrive le attività), D62 (passaggi), D68 (proposta nel calendario), D96 (eventi attivi giorno per giorno).

### 3 · Strumenti e tecnologie (1:30-2:00)

**Fai:** passa alla scheda del **README** su GitHub, sullo schema "Come funziona".

**Dici:**
> Il frontend è React con TypeScript, Redux Toolkit e FullCalendar. Il backend è Spring Boot, con Spring Security e JWT, e PostgreSQL con lo schema gestito da Flyway. L'agente usa un modello gratuito tramite OpenRouter, con un ciclo di strumenti che ho scritto io. È tutto online su Render, [con il backend in Docker e le API documentate in Swagger].

**Decisioni:** D5 (Spring Boot e PostgreSQL), D38 (Flyway), D45 (JWT), D53 (Swagger), D82 (Render), D126 (modello gratuito), D128 (ciclo dell'agente).

### 4 · Dati chiave (2:00-2:35)

**Fai:** resta sullo schema.

**Dici:**
> Qualche numero. In 14 giorni ho fatto tre rilasci online e documentato 138 decisioni. Lighthouse dà 100 in accessibilità, best practice e SEO su tutte le pagine, e la checklist finale sul sito online è passata in tutti i 19 punti. Sull'agente ho provato dieci scenari reali: un piano costa da 3 a 6 chiamate al modello, e il piano gratuito ne dà 50 al giorno per tutta l'app. Per questo ci sono limiti giornalieri e un piano base senza IA, [che è servito davvero quando il fornitore gratuito è rimasto fermo per ore].

**Decisioni:** D64 (piano base), D79 (verifica sul sito), D129 (limiti), D131 (scenari), D132 (cambio di modello), `qualita.md`.

### 5 · Valore pratico (2:35-3:00)

**Fai:** torna alla scheda del **calendario**, con il piano confermato.

**Dici:**
> Il valore pratico: chi gioca vede in un posto solo cosa fare ogni giorno, con materiali e tempi già calcolati. E lo schema si può riusare in altri planner: l'IA propone e spiega, il codice calcola, la persona decide. Potete provarla su resonance-planner.onrender.com con "Prova la demo". Grazie.

**Decisioni:** D2, D3, D134 (ospite).

## Se qualcosa va storto

| Cosa succede | Cosa fai | Cosa dici |
|---|---|---|
| *"Il server si sta avviando: può volerci fino a tre minuti."* | Racconta le scene 1, 3 e 4 sulla scheda del README, poi torna al calendario | "Il backend gratuito si spegne quando non lo usa nessuno" |
| Alle 0:50 l'agente non ha ancora risposto | Racconta eventi e Waveplate, poi passa alla scena 3 lasciando la chat visibile; quando la risposta arriva, mostrala e conferma | "Il modello è gratuito e a volte è lento" |
| L'agente risponde con un errore, o non ha risposto alle 2:15 | Torna al calendario, **"Genera piano base"**, poi **"Conferma"** | "Per questo esiste il piano base: lo stesso pianificatore Java, senza IA" (D64) |
| La chat dice che i messaggi di oggi sono finiti | **"Genera piano base"**, che la chat stessa offre | "I limiti proteggono la quota gratuita" (D129) |
| La proposta ha attività sabato o domenica | **"Scarta"** | "Decide sempre l'utente: scarto e chiedo di nuovo" (D2) |
| Compare l'avviso giallo *"Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano."* | Niente, oppure una frase | "È l'avviso di cambiamento: il piano non è più aggiornato" (D125) |
| Arriva una notifica in basso (Waveplate o evento che finisce) | Niente: sparisce dopo 6 secondi | Se c'è tempo: "Notifiche in tempo reale con un WebSocket" (D135) |

## Domande probabili

| Domanda | Risposta breve |
|---|---|
| Perché un modello gratuito su OpenRouter e non Claude o GPT? | Una demo pubblica non deve costare: il backend accetta solo modelli che finiscono in `:free`. Il modello si cambia con una variabile d'ambiente, senza toccare il codice (D126, D132). |
| Come fai a fidarti di quello che dice il modello? | Non fa calcoli e non scrive nel calendario: sceglie i parametri, Java calcola, e la proposta va confermata. I suoi testi si mostrano solo come testo semplice, mai come HTML (D2, D3, D56, D60). |
| E se il modello non risponde? | La chat lo dice e offre il piano base, che usa lo stesso pianificatore senza IA (D64). Le chiamate al modello sono contate nel database, anche quelle fallite (D129). |
| Come funziona il login? | Token JWT nell'header `Authorization`, valido 24 ore, con i ruoli USER e ADMIN. I dati personali stanno solo sotto `/api/me` e l'utente si ricava sempre dal token (D19, D44, D45). |
| Come proteggi l'app dagli abusi? | Limiti di richieste su login, registrazione e accesso ospite (D101), limiti giornalieri dell'agente (D129), password con le regole di D100. |
| Come l'hai verificata? | Sul sito online: una checklist a ogni rilascio, l'ultima con 19 punti, e dieci scenari reali dell'agente (D63, D79, `rilascio-v3.md`, `scenari-agente.md`). Lighthouse dà 100 su tutte le pagine (`qualita.md`). |
| Perché Flyway? | Lo schema è versionato insieme al codice e Hibernate lo controlla soltanto (D38). Una migrazione già pubblicata non si modifica mai: se ne aggiunge una nuova. |
| Perché un WebSocket semplice e non STOMP? | Il server manda solo notifiche brevi, e il browser manda solo l'accesso e un `PING` ogni 25 secondi: STOMP aggiungerebbe un protocollo che non serve. Il token arriva nel primo messaggio, `AUTH`, e mai nell'indirizzo (D135). |
| Come si calcolano le Waveplate? | Il server salva il valore e l'ora della lettura, poi aggiunge una Waveplate ogni 6 minuti fino al massimo, e oltre accumula i Crystals (D28, D105). |
| Cosa faresti con più tempo? | Controlli automatici con GitHub Actions e il damage calculator (X04, X05); più personaggi ed eventi, che sono solo dati JSON e non richiedono codice nuovo (D4, D9). |
| L'app è accessibile? | Obiettivo WCAG 2.2 AA (D70): Lighthouse 100 su tutte le pagine, uso da tastiera e lettore di schermo provati a mano (`qualita.md`, `rilascio-v3.md`). |

## Prove cronometrate

**Non fatte, per scelta dell'autore (24/10/2026).** Il criterio di T42 chiedeva due prove dall'inizio alla fine. L'autore ha deciso di saltarle: prima della presentazione apre tutto e lo lascia pronto, come nella sezione "Preparazione" (D138). Le singole parti sono già state provate nella checklist finale (`rilascio-v3.md`): richiesta all'agente e conferma della proposta (risposta in 20-50 secondi il 23/10), piano base, notifiche ed Excel.

Se si decide di fare comunque una prova: dall'inizio alla fine, ad alta voce e con il cronometro, annotando la durata di ogni scena; alla fine della scena 2 si preme "Scarta" invece di "Conferma".
