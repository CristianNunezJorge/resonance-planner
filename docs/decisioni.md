# Registro delle decisioni

Ogni voce riporta cosa abbiamo scelto, come lo implementiamo, perché, e quali alternative abbiamo scartato. Le decisioni nuove si aggiungono in fondo.

---

## D1 · Il cuore del progetto è calendario + agente
**Stato:** Confermata · 30/09/2026

- **Cosa:** calendario degli eventi, gestione delle Waveplate e agente IA sono la parte principale. Il damage calculator è un extra facoltativo.
- **Come:** v1 e v2 sono dedicate a calendario e agente; il damage calculator è nella v3.
- **Perché:** il calendario gestito da un agente è la parte originale del progetto. Esistono già strumenti della community per calcolare danni e statistiche, e il regolamento chiede di non clonare progetti esistenti.
- **Scartate:** damage calculator al centro (troppi dati, già esistente); entrambi a metà (più difficile da rifinire in due settimane).

## D2 · L'agente propone, l'utente conferma
**Stato:** Confermata · 30/09/2026

- **Cosa:** l'agente non scrive mai direttamente nel calendario.
- **Come:** l'unico strumento di scrittura dell'agente salva un piano come *proposta*. Le attività entrano nel calendario solo quando l'utente conferma dall'interfaccia.
- **Perché:** un errore dell'agente non può rovinare i dati, soprattutto durante la demo. Si spiega facilmente allo staff.
- **Scartate:** agente che modifica il calendario da solo (rischioso); agente che dà solo consigli in chat (si perde l'integrazione con il calendario).

## D3 · I calcoli li fa Java, non l'IA
**Stato:** Confermata · 30/09/2026

- **Cosa:** Waveplate, materiali mancanti e giorni di farming sono calcolati dal backend.
- **Come:** service Java con metodi puri, coperti da test JUnit. L'agente li chiama come strumenti (tool use) e usa i risultati per costruire e spiegare il piano.
- **Perché:** i modelli di linguaggio sbagliano spesso l'aritmetica. Un calcolo in Java dà sempre lo stesso risultato ed è facile da testare.
- **Scartate:** far calcolare tutto al modello dentro il prompt.

## D4 · Nessuna API esterna: dataset in JSON caricato nel database
**Stato:** Confermata · 30/09/2026

- **Cosa:** i dati di gioco (personaggi, materiali, costi, eventi) vivono in file JSON del progetto.
- **Come:** all'avvio il backend legge i JSON con Jackson e li salva in PostgreSQL. Il frontend li legge dalle API del backend.
- **Perché:** Wuthering Waves non ha un'API ufficiale. I dati della community non sono ufficiali e possono cambiare o sparire, e dipenderne durante la demo è un rischio. In più così si dimostra la modellazione JPA.
- **Scartate:** leggere i dati in tempo reale da siti della community.

## D5 · Backend Spring Boot con PostgreSQL
**Stato:** Confermata · 30/09/2026

- **Cosa:** backend Java Spring Boot, database PostgreSQL.
- **Come:** API REST a livelli (controller, service, repository), Spring Data JPA, Spring Security con JWT.
- **Perché:** requisito dell'esame; è lo stack del corso.
- **Scartate:** nessuna, è obbligatorio.

## D6 · API Claude per l'agente
**Stato:** Confermata · modello Claude Haiku 4.5 scelto il 01/10/2026 (D59) · sostituita da D126 (17/10/2026): modello gratuito con OpenRouter

- **Cosa:** l'agente usa l'API Claude con tool use, tramite l'SDK Java ufficiale di Anthropic.
- **Come:** la chiave API sta solo sul server come variabile d'ambiente. Tetto di spesa mensile nella Console Anthropic, agente riservato agli utenti loggati, limite di richieste al giorno.
- **Perché:** è l'API studiata nella settimana 19. Claude non ha un piano gratuito fisso, ma i nuovi account ricevono un piccolo credito iniziale, e Haiku 4.5 costa 1 $ in input e 5 $ in output per milione di token: stimiamo pochi dollari per tutto il progetto.
- **Scartate:** piani gratuiti di altri fornitori (limiti bassi e variabili, possibile uso dei dati per l'addestramento); modello locale con Ollama (non funziona sulla versione online).

## D7 · Frontend creato con Vite
**Stato:** Confermata · 01/10/2026 (insieme alla fase 8)

- **Cosa:** progetto React + TypeScript creato con Vite.
- **Come:** stessa struttura React del corso; i test usano Vitest con React Testing Library.
- **Perché:** Create React App, usato nel corso, non è più mantenuto.
- **Scartate:** Create React App.

## D8 · Rilasci incrementali, deploy dalla v1
**Stato:** Confermata · 30/09/2026

- **Cosa:** tre rilasci: v1 core, v2 agente, v3 extra facoltativi.
- **Come:** ogni rilascio è funzionante e pubblicato online prima di passare al successivo.
- **Perché:** lo chiede il regolamento del capstone. Un deploy fatto presto evita che i problemi di configurazione si scoprano l'ultimo giorno.
- **Scartate:** sviluppo tutto insieme con deploy finale.

## D9 · L'app è una demo con un dataset curato
**Stato:** Confermata · 01/10/2026

- **Cosa:** 5-6 personaggi reali del gioco, pochi eventi copiati da eventi passati, calcolo dei materiali fino al livello 20.
- **Come:** tutto sta nei file JSON di D4. Il codice non contiene nulla di specifico di un personaggio o di un evento: aggiungere dati non richiede modifiche al codice.
- **Perché:** basta a dimostrare che l'app funziona e rende la demo prevedibile. I dati da preparare restano pochi, compatibili con due settimane.
- **Scartate:** tutti i personaggi e tutti i livelli (troppi dati da preparare e verificare).

## D10 · Login obbligatorio, dati separati per utente
**Stato:** Confermata · 01/10/2026

- **Cosa:** registrazione e login; ogni utente ha roster, Waveplate e piani suoi.
- **Come:** Spring Security con JWT, password cifrate con BCrypt. Tutte le API dei dati personali leggono l'utente dal token.
- **Perché:** il login è un requisito. Dati separati permettono a più persone di provare la demo nello stesso momento senza disturbarsi.
- **Scartate:** un account demo condiviso (chi prova vedrebbe e modificherebbe i dati degli altri).

## D11 · Esperienza guidata con scelte predefinite
**Stato:** Confermata · 01/10/2026

- **Cosa:** chi prova l'app sceglie tra opzioni preparate (personaggi, livelli, obiettivi) e l'app gli costruisce il piano.
- **Come:** configurazione guidata al primo accesso; chat con l'agente con suggerimenti pronti oltre al testo libero.
- **Perché:** le scelte limitate garantiscono che l'agente abbia sempre i dati che gli servono. Scelte diverse producono piani diversi, e si vede che il risultato non è preimpostato.
- **Scartate:** inserimento libero di qualsiasi personaggio o obiettivo (fuori dal dataset demo).

## D12 · Date degli eventi relative a oggi
**Stato:** Confermata · 01/10/2026

- **Cosa:** gli eventi copiati da quelli passati appaiono sempre come in corso o in arrivo.
- **Come:** nel JSON ogni evento ha un *inizio relativo* e una durata (per esempio "iniziato 3 giorni fa, dura 14 giorni"). All'avvio il backend calcola le date reali partendo da oggi.
- **Perché:** la demo deve sembrare attuale in qualunque giorno venga provata, anche mesi dopo il Demo Day. L'agente ha sempre un futuro da pianificare.
- **Scartate:** date storiche reali (eventi tutti nel passato, niente da pianificare); data simulata (complica tutto il codice che usa la data di oggi).

## D13 · Il calcolo copre il livello del personaggio e l'ascensione al 20
**Stato:** Confermata · 01/10/2026

- **Cosa:** materiali per salire dal livello 1 al 20 e per l'ascensione al livello 20. Armi e abilità restano fuori.
- **Come:** una tabella di costi per livello nel JSON di ogni personaggio.
- **Perché:** con il solo livello servirebbero quasi solo materiali EXP. L'ascensione aggiunge materiali diversi da farmare, e il piano dell'agente diventa più interessante. Da verificare con i dati reali del gioco.
- **Scartate per ora:** livello delle armi e abilità (più dati da preparare, poco valore in più per la demo).

## D14 · Area admin minima per gli eventi
**Stato:** Confermata · 01/10/2026

- **Cosa:** un utente con ruolo ADMIN può creare, modificare e cancellare eventi dall'app.
- **Come:** una pagina admin con una tabella e un form; endpoint protetti dal ruolo ADMIN.
- **Perché:** mostra ruoli e permessi all'esame. Al Demo Day si può aggiungere un evento dal vivo e chiedere all'agente di ripianificare.
- **Scartate:** eventi solo dal file JSON (niente ruoli da mostrare).

## D15 · Accesso ospite "Prova la demo"
**Stato:** Confermata · 01/10/2026 · v3

- **Cosa:** un pulsante che fa entrare senza registrazione, con un utente ospite temporaneo.
- **Come:** crea un utente con ruolo GUEST e lo stesso flusso di configurazione; un job pianificato cancella gli ospiti dopo qualche giorno.
- **Perché:** un recruiter prova l'app con un clic.
- **Scartate per ora:** in v1 (il login normale basta per il requisito).

## D16 · Tempo di gioco giornaliero
**Stato:** Confermata · 01/10/2026

- **Cosa:** l'utente indica quanto gioca al giorno: 30 minuti, 1 ora o 2 ore.
- **Come:** si sceglie nella configurazione guidata e si cambia dal profilo. Ogni tipo di attività ha una durata stimata nei dati di gioco (fase 3); il calcolo distribuisce le attività senza superare il tempo disponibile in ciascun giorno.
- **Perché:** l'app promette di gestire il tempo di gioco. Con questo dato il piano dipende anche dalla persona, non solo dalle Waveplate.
- **Scartate:** orari precisi o giorni della settimana diversi tra loro (più complesso da inserire, poco valore in più per la demo).

## D17 · I sei personaggi della demo
**Stato:** Confermata · 01/10/2026 · dati da verificare nella fase 3

- **Cosa:** Jinhsi (Spectro, Broadblade, 5★), Yinlin (Electro, Rectifier, 5★), Changli (Fusion, Sword, 5★), Lingyang (Glacio, Gauntlets, 5★), Danjin (Havoc, Sword, 4★), Aalto (Aero, Pistols, 4★).
- **Come:** un file JSON per il catalogo, con i costi dal livello 1 al 20 e l'ascensione (D13).
- **Perché:** coprono tutti e sei gli elementi e tutti e cinque i tipi di arma, quindi le scelte nella demo sono varie. Due sono a 4★, personaggi che ogni giocatore ha. Vengono tutti dalle prime versioni del gioco, quindi i loro dati sono stabili e ben documentati.
- **Scartate:** personaggi delle versioni più recenti (dati meno consolidati nelle fonti della community).

## D18 · Tre tipi di evento nella demo
**Stato:** Confermata · 01/10/2026 (insieme alla fase 2)

- **Cosa:** la demo contiene un evento per ognuno di questi tipi:
  1. **Combattimento a livelli** (corretto da D95): una serie di livelli da superare, ognuno un'attività da fare una volta.
  2. **Ricompense doppie su un contenuto di farming**: per qualche giorno un contenuto dà materiali doppi. L'agente deve concentrare lì il farming di quel materiale.
  3. **Evento a punti con negozio a tempo**: bisogna raccogliere un certo numero di punti prima della fine.
- **Come:** nella fase 3 scegliamo un evento reale per ciascun tipo, verificandolo sulle fonti della community, e lo copiamo nel JSON con le date relative di D12.
- **Perché:** tre tipi diversi mettono alla prova il pianificatore in modi diversi. Il secondo è il caso più interessante da mostrare: il piano cambia perché c'è un evento.
- **Scartate:** copiare tutti gli eventi di una versione (troppi dati, poco valore in più).

## D19 · Ruoli USER e ADMIN
**Stato:** Confermata · 01/10/2026

- **Cosa:** due ruoli in v1: USER (chi gioca) e ADMIN (chi gestisce la demo). GUEST arriva in v3 con D15.
- **Come:** ruolo salvato sull'utente e incluso nel token JWT. L'account admin viene creato all'avvio leggendo email e password da variabili d'ambiente, mai scritte nel codice.
- **Perché:** basta un ruolo in più per mostrare permessi diversi. Le credenziali fuori dal codice sono una regola di sicurezza di base.
- **Scartate:** registrazione libera come admin; credenziali admin nel codice o nei file del repository.

## D20 · Configurazione guidata in tre passi
**Stato:** Confermata · 01/10/2026

- **Cosa:** al primo accesso: 1) scelta dei personaggi; 2) livello attuale e obiettivo di ciascuno; 3) Waveplate attuali e tempo di gioco.
- **Come:** una pagina con tre passi e una barra di avanzamento. Finché la configurazione non è completa, dopo il login si torna sempre lì. Tutto resta modificabile dopo, dal roster e dal profilo.
- **Perché:** in meno di un minuto l'utente ha tutti i dati che servono all'agente. Tre passi brevi sono più chiari di un unico modulo lungo.
- **Scartate:** chiedere anche i materiali già posseduti nella configurazione (allungherebbe il primo accesso: si parte da zero, vedi D21).

## D21 · Inventario dei materiali
**Stato:** Confermata · 01/10/2026

- **Cosa:** l'app tiene il conto dei materiali che l'utente possiede.
- **Come:** l'inventario parte da zero e si modifica a mano dalla pagina del roster. Quando l'utente segna come fatta un'attività di farming, l'app aggiunge all'inventario la quantità media prevista.
- **Perché:** il calcolo dei materiali mancanti resta corretto mentre l'utente avanza. In demo si vede il ciclo completo: segno un'attività come fatta e i materiali mancanti scendono.
- **Scartate:** nessun inventario (il calcolo non cambierebbe mai); inventario solo manuale (troppo da aggiornare a mano).

## D22 · Una proposta alla volta, il nuovo piano sostituisce il vecchio
**Stato:** Confermata · 01/10/2026

- **Cosa:** ogni utente ha al massimo una proposta in attesa. Quando conferma un nuovo piano, le attività future e non ancora fatte del piano precedente dell'agente vengono sostituite.
- **Come:** una nuova proposta sostituisce quella in attesa. Alla conferma, le vecchie attività dell'agente passano allo stato "sostituita". Le attività fatte e quelle create a mano non vengono toccate.
- **Perché:** nel calendario non si accumulano piani duplicati o in conflitto, e lo storico di quello che l'utente ha fatto resta.
- **Scartate:** sommare i piani uno sull'altro (doppioni); cancellare le vecchie attività (si perde lo storico).

## D23 · Niente ripianificazione automatica
**Stato:** Confermata · 01/10/2026

- **Cosa:** l'agente lavora solo quando l'utente glielo chiede.
- **Come:** quando cambiano il roster, gli obiettivi o gli eventi, il calendario mostra un avviso: "Qualcosa è cambiato: chiedi all'agente di aggiornare il piano".
- **Perché:** coerente con D2 (decide l'utente) e con i costi (D6): nessuna chiamata all'API che l'utente non ha chiesto.
- **Scartate:** ripianificazione automatica a ogni modifica (chiamate e costi imprevedibili, calendario che cambia senza preavviso).

## D24 · Attività manuali
**Stato:** Confermata · 01/10/2026

- **Cosa:** l'utente può creare, modificare e cancellare attività sue nel calendario.
- **Come:** un form con titolo, giorno, durata e note. Le attività manuali sono separate da quelle dell'agente e un nuovo piano non le tocca (D22).
- **Perché:** il calendario resta utile anche senza agente, ed è un CRUD completo da mostrare all'esame.
- **Scartate:** calendario modificabile solo dall'agente (troppo rigido).

## D25 · Fonti dei dati e regole di verifica
**Stato:** Confermata · 01/10/2026

- **Cosa:** fonte principale il Wuthering Waves Wiki su Fandom, più un database della community per il controllo incrociato.
- **Come:** ogni numero viene da una fonte. Se manca, lo stimiamo e lo segniamo con `estimated: true`; l'app mostra quei valori come "stima". README e footer dell'app citano il wiki.
- **Perché:** il wiki ha dati incompleti (per esempio mancano le ricompense della Simulation Training), quindi serve una seconda fonte. I contenuti del wiki sono sotto licenza CC BY-SA, che richiede di citare la fonte.
- **Scartate:** dati presi da una sola fonte senza controllo; numeri a memoria.

## D26 · Sei file JSON con identificativi leggibili
**Stato:** Confermata · 01/10/2026

- **Cosa:** `game-rules.json`, `materials.json`, `farming-sources.json`, `level-exp.json`, `characters.json`, `events.json`.
- **Come:** stanno nelle risorse del backend. Gli identificativi sono testi stabili (`jinhsi`, `lf-howler-core`) che collegano i file tra loro. I valori fissi come elementi e tipi di arma sono in inglese maiuscolo (`SPECTRO`, `BROADBLADE`), come le enum Java che li rappresenteranno. Il formato completo è in `03-dati-demo.md`.
- **Perché:** un file per tipo di dato è facile da leggere e da correggere. Gli identificativi testuali rendono i file comprensibili senza il database.
- **Scartate:** un unico file enorme; identificativi numerici nei file (illeggibili e fragili se si riordinano i dati).

## D27 · Livello massimo della demo: 40
**Stato:** Confermata · 01/10/2026 · sostituisce il limite al 20 di D9 e D13

- **Cosa:** il calcolo arriva al livello 40, seconda ascensione compresa.
- **Come:** il codice non ha limiti fissi: il livello massimo dipende solo dai dati. Servono la tabella EXP fino al 40 e i costi delle prime due ascensioni di ogni personaggio.
- **Perché:** fino al 20 l'unico contenuto a Waveplate è la Simulation Training e l'ascensione richiede solo Shell Credit e 4 drop di nemici comuni. Al 40 entrano anche Boss Challenge, specialità locali e drop di livello superiore: il piano dell'agente diventa vario e realistico. Il 60 era permesso, ma non aggiunge tipi di contenuto nuovi, solo quantità più grandi e piani più lunghi. Salire al 60 in futuro vorrà dire solo aggiungere dati.
- **Scartate:** 20 (piano ridotto a "quante run di Simulation Training"); 60 (più dati da raccogliere senza meccaniche nuove).

## D28 · Il contatore modella anche i Waveplate Crystals
**Stato:** Confermata · 01/10/2026

- **Cosa:** oltre le 240 Waveplate, il contatore calcola i Waveplate Crystals accumulati (1 ogni 12 minuti, fino a 480).
- **Come:** stesso calcolo del contatore, con una seconda fase dopo il massimo. Le regole sono in `game-rules.json`, non scritte nel codice.
- **Perché:** è la meccanica reale del gioco. Raggiungere il massimo non spreca energia, la rallenta: il piano può tenerne conto invece di forzare l'utente a giocare subito.
- **Scartate:** ignorare i Crystals (il contatore direbbe cose sbagliate a chi gioca davvero).

## D29 · Eventi della demo ricorrenti
**Stato:** Confermata · 01/10/2026 · precisa D12

- **Cosa:** ogni evento della demo ha una durata e si ripete a intervalli fissi, così c'è sempre un evento in corso o in arrivo.
- **Come:** nel JSON ogni evento ha `startOffsetDays`, `durationDays` e `repeatEveryDays`, calcolati a partire da una data di riferimento fissa. Il backend ricava l'edizione in corso o la prossima.
- **Perché:** con date ricalcolate solo "rispetto a oggi", un evento risulterebbe ogni giorno "iniziato 3 giorni fa" e non andrebbe mai avanti. Con il ciclo, gli eventi avanzano giorno per giorno come nel gioco, e le attività pianificate restano coerenti con le date.
- **Scartate:** date ricalcolate a ogni lettura (eventi bloccati); date ricalcolate solo all'avvio del server (cambierebbero a ogni riavvio della piattaforma).

## D30 · Immagini dei personaggi
**Stato:** Confermata · 01/10/2026

- **Cosa:** icone ufficiali dei personaggi, se si trovano. Se mancano, un avatar con le iniziali e il colore dell'elemento.
- **Come:** le icone si scaricano durante la raccolta dati e si salvano nel progetto; il JSON del personaggio ne indica il percorso. Il componente React mostra l'avatar generato quando l'icona manca o non si carica, quindi il ripiego c'è sempre.
- **Perché:** le icone originali rendono la demo riconoscibile. Non abbiamo trovato regole pubbliche di Kuro Games sull'uso delle immagini nei progetti dei fan: il progetto è non commerciale, README e footer indicano che le immagini appartengono a Kuro Games, e se servisse si passa agli avatar generati senza toccare altro.
- **Scartate:** immagini caricate direttamente da siti esterni (fragili, e spesso i siti lo vietano).

## D31 · Le durate sono stime nostre
**Stato:** Confermata · 01/10/2026

- **Cosa:** quanto dura una run o un'attività di un evento lo stimiamo noi.
- **Come:** il campo `minutesPerRun` e i minuti delle attività sono nei JSON, documentati come stime. L'app mostra le durate con il simbolo "~".
- **Perché:** il gioco non fornisce questi tempi, ma servono per rispettare il tempo di gioco giornaliero (D16).
- **Scartate:** ignorare le durate (D16 non sarebbe applicabile).

## D32 · Chiavi primarie testuali per il catalogo, numeriche per il resto
**Stato:** Confermata · 01/10/2026

- **Cosa:** materiali, fonti e personaggi hanno come chiave l'identificativo testuale dei JSON (`jinhsi`). Utenti, attività, proposte ed eventi hanno chiavi numeriche generate dal database.
- **Come:** `@Id String` per il catalogo, `@Id @GeneratedValue Long` per il resto. Gli eventi della demo conservano il loro identificativo testuale nella colonna `code`.
- **Perché:** il catalogo arriva dai JSON e non cambia: tenere lo stesso identificativo rende i dati leggibili anche quando l'agente li usa ("jinhsi" invece di "7"). I dati creati dagli utenti, invece, non hanno un nome naturale.
- **Scartate:** chiavi numeriche ovunque (servirebbe una tabella di conversione per i JSON); UUID (più pesanti, senza vantaggi reali qui perché ogni query è già filtrata per utente).

## D33 · Le regole di gioco restano in memoria, non nel database
**Stato:** Confermata · 01/10/2026

- **Cosa:** massimi e ricarica delle Waveplate, livelli di ascensione, data di riferimento degli eventi e opzioni di tempo di gioco.
- **Come:** `game-rules.json` viene letto all'avvio in un oggetto di configurazione Java immutabile, usato dai service.
- **Perché:** sono poche costanti che non cambiano mai durante l'uso. Una tabella aggiungerebbe codice senza dare nulla in più.
- **Scartate:** tabella delle regole; costanti scritte nel codice (cambiarle richiederebbe di ricompilare).

## D34 · Livello e grado di ascensione
**Stato:** Confermata · 01/10/2026

- **Cosa:** ogni personaggio del roster ha un livello e un grado di ascensione (rank), sia attuali sia obiettivo. Il grado 0 arriva fino al livello 20, il grado 1 fino al 40, il grado 2 indica l'ascensione fatta al 40.
- **Come:** i costi stanno in una sola tabella, `ascension_cost` (personaggio, grado, materiale, quantità). I livelli massimi di ogni grado si ricavano da `ascensionLevels` nelle regole di gioco. Il backend controlla che il livello non superi il massimo del grado e che l'obiettivo non sia più basso della situazione attuale.
- **Perché:** nel gioco "livello 20" può voler dire prima o dopo l'ascensione, e i materiali cambiano. Il grado elimina l'ambiguità.
- **Scartate:** solo il livello (non distingue il prima e il dopo); una tabella delle ascensioni più una dei costi (due tabelle per lo stesso dato).

## D35 · Un'unica tabella per tutte le attività
**Stato:** Confermata · 01/10/2026

- **Cosa:** attività di farming, attività di eventi e attività manuali stanno tutte in `planned_task`.
- **Come:** tre colonne le distinguono: `origin` (chi l'ha creata), `kind` (che tipo è) e `status`. I campi specifici di un tipo sono facoltativi, e un controllo nel database impedisce le combinazioni sbagliate.
- **Perché:** il calendario le mostra tutte insieme e hanno quasi gli stessi campi. Una sola tabella vuol dire una sola query e un solo endpoint per il calendario.
- **Scartate:** una tabella per tipo (tre query e tre endpoint per mostrare lo stesso calendario); ereditarietà JPA tra entità (più complessa da spiegare e da interrogare).

## D36 · Le Waveplate sono campi dell'utente
**Stato:** Confermata · 01/10/2026

- **Cosa:** valore, Crystals e momento dell'ultimo aggiornamento stanno nella tabella `users`.
- **Come:** un `@Embeddable` `WaveplateSnapshot` dentro l'entità utente. Il valore attuale si calcola al momento della lettura.
- **Perché:** ogni utente ha un solo contatore. Una tabella separata non aggiungerebbe nulla.
- **Scartate:** storico di tutti gli aggiornamenti (non serve a nessuna funzione).

## D37 · Una sola proposta in attesa, garantita dal database
**Stato:** Confermata · 01/10/2026

- **Cosa:** il database impedisce che un utente abbia due proposte in attesa (`PENDING`).
- **Come:** indice unico parziale di PostgreSQL su `plan_proposal(user_id)`, limitato alle righe con stato `PENDING`. Il service sostituisce comunque la proposta precedente prima di crearne una nuova (D22).
- **Perché:** la regola resta valida anche se due richieste arrivano nello stesso momento, cosa che un controllo solo nel codice non garantisce.
- **Scartate:** controllo solo nel service.

## D38 · Schema del database gestito con Flyway
**Stato:** Confermata · 01/10/2026 · tecnologia nuova rispetto al corso

- **Cosa:** lo schema si crea e si aggiorna con migrazioni SQL versionate.
- **Come:** file `V1__schema.sql`, `V2__...` nel backend; Flyway li applica all'avvio. Hibernate è impostato su `validate`: controlla che le entità corrispondano allo schema, ma non lo modifica.
- **Perché:** con `ddl-auto=update` lo schema cambia in modo invisibile, e alcune cose, come l'indice parziale di D37 e i controlli tra colonne, non si possono creare. Le migrazioni rendono il database identico in locale e online e documentano ogni cambiamento.
- **Scartate:** `ddl-auto=update`, usato nel corso (comodo, ma imprevedibile in produzione).

## D39 · Caricamento dei dati all'avvio
**Stato:** Confermata · 01/10/2026

- **Cosa:** il catalogo si aggiorna dai JSON a ogni avvio. Gli eventi della demo si inseriscono solo se mancano. L'account admin si crea solo se non esiste.
- **Come:** un componente Java eseguito all'avvio legge i JSON con Jackson. Il catalogo viene aggiornato per identificativo; gli eventi vengono cercati per `code`; l'admin per email (D19).
- **Perché:** correggere un numero nel JSON basta per aggiornare il catalogo. Le modifiche che l'admin fa a un evento della demo, invece, non vengono cancellate al riavvio: le piattaforme gratuite riavviano spesso il server.
- **Scartate:** ricaricare tutto a ogni avvio (perderebbe le modifiche dell'admin); caricare i dati con Flyway (i dati vivono nei JSON, non nell'SQL).

## D40 · La chat con l'agente è salvata
**Stato:** Confermata · 01/10/2026 · dettagli nella fase 6

- **Cosa:** i messaggi dell'utente e le risposte dell'agente, solo il testo.
- **Come:** tabella `agent_message`. I passaggi interni dell'agente (le chiamate agli strumenti) non si salvano: a ogni richiesta l'agente rilegge i dati aggiornati con i suoi strumenti.
- **Perché:** la chat resta dopo un ricaricamento della pagina, e il limite giornaliero (D6) si conta dai messaggi salvati.
- **Scartate:** chat tenuta solo nel browser (si perde e non permette di contare le richieste in modo affidabile).

## D41 · Un solo fuso orario per l'app
**Stato:** Confermata · 01/10/2026

- **Cosa:** "oggi", i giorni del calendario e il limite giornaliero si calcolano sull'ora italiana (Europe/Rome).
- **Come:** fuso orario in configurazione, usato da tutti i calcoli sulle date. I momenti si salvano in UTC.
- **Perché:** gli utenti della demo sono soprattutto in Italia. Un fuso per utente complicherebbe ogni calcolo per un vantaggio piccolo.
- **Scartate:** fuso orario per utente (lo segnaliamo come limite noto nel README).

## D42 · Eliminazione dell'account
**Stato:** Confermata · 01/10/2026 · aggiunta allo scope v1

- **Cosa:** l'utente può cancellare il suo account dal profilo, e con lui tutti i suoi dati.
- **Come:** un endpoint che elimina l'utente; il database cancella a cascata roster, inventario, attività, proposte e messaggi.
- **Perché:** è il "diritto all'oblio" del GDPR, studiato nella settimana 20. Costa pochissimo e all'esame mostra attenzione ai dati personali.
- **Scartate:** nessuna eliminazione (gli utenti della demo resterebbero per sempre).

## D43 · Convenzioni del modello dati
**Stato:** Confermata · 01/10/2026

- **Cosa:** regole comuni per tutte le tabelle e le entità.
- **Come:** nomi in inglese e `snake_case`; enum salvate come testo; momenti in `timestamptz` (UTC) e giorni in `date`; relazioni caricate solo quando servono (`LAZY`); ogni query sui dati personali filtrata per l'utente del token.
- **Perché:** uno schema coerente si legge e si spiega meglio. Le enum come testo non si rompono se si aggiunge un valore; il filtro per utente impedisce a chiunque di vedere i dati degli altri.
- **Scartate:** enum salvate come numero (si rompono se cambia l'ordine dei valori).

## D44 · Struttura degli URL
**Stato:** Confermata · 01/10/2026

- **Cosa:** tutte le API sotto `/api`, senza numero di versione. I dati personali sotto `/api/me`, le funzioni admin sotto `/api/admin`.
- **Come:** in `/api/me/**` l'utente si ricava dal token; nessun URL contiene l'identificativo dell'utente.
- **Perché:** non potendo scrivere l'id di un altro utente nell'URL, si elimina alla radice un intero tipo di vulnerabilità (l'accesso ai dati altrui cambiando un numero). La versione nell'URL serve quando un'API ha client esterni; qui c'è solo il nostro frontend.
- **Scartate:** URL come `/api/users/{id}/tasks` (da proteggere uno per uno); `/api/v1` (complessità inutile con un solo client).

## D45 · Token JWT nell'header, valido 24 ore
**Stato:** Confermata · 01/10/2026

- **Cosa:** il token viaggia nell'header `Authorization: Bearer`, il frontend lo salva nel `localStorage`, scade dopo 24 ore e non c'è refresh token.
- **Come:** Spring Security valida il token a ogni richiesta, senza sessioni sul server. Logout = il frontend cancella il token. Per ridurre il rischio di furto via XSS: React protegge già l'output, non si usa `dangerouslySetInnerHTML`, e il sito imposta una Content-Security-Policy.
- **Perché:** frontend e backend avranno indirizzi diversi (D82: due servizi separati su Render), e i cookie tra siti diversi sono sempre più bloccati dai browser. È anche il metodo visto nel corso.
- **Scartate:** cookie `httpOnly` (più sicuro contro XSS, ma fragile tra domini diversi e richiede la protezione CSRF); refresh token (più codice, poco utile per una demo).
- **Limite noto:** dopo il logout il token resta tecnicamente valido fino alla scadenza.

## D46 · Formato unico delle risposte ed errori
**Stato:** Confermata · 01/10/2026

- **Cosa:** ogni risposta ha `success`, `data` ed `error`; gli elenchi paginati aggiungono `meta`. Gli errori hanno un `code` fisso, un `message` in italiano e, per la validazione, i campi sbagliati.
- **Come:** una classe `ApiResponse<T>` per tutte le risposte e un `@RestControllerAdvice` che trasforma ogni eccezione nel formato di errore con il codice HTTP giusto (tabella in `05-api.md`). Gli errori imprevisti si registrano nei log con i dettagli, ma rispondono solo con un messaggio generico.
- **Perché:** il frontend gestisce tutte le risposte con lo stesso codice. Nessun dettaglio interno (stack trace, query) arriva all'utente.
- **Scartate:** DTO senza involucro con Problem Details (RFC 7807) per gli errori (standard valido, ma due formati diversi da gestire nel frontend).

## D47 · DTO separati dalle entità, con validazione
**Stato:** Confermata · 01/10/2026

- **Cosa:** ogni endpoint riceve e restituisce DTO dedicati, mai le entità JPA.
- **Come:** DTO come `record` Java con annotazioni di Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@Min`...). Le regole che dipendono dal gioco (livelli, gradi) le controllano i service.
- **Perché:** le entità possono contenere dati da non esporre, come l'hash della password, e cambiare il database non deve cambiare le API. La validazione all'ingresso blocca i dati sbagliati prima che arrivino alla logica.
- **Scartate:** restituire direttamente le entità.

## D48 · Limiti di richieste e CORS
**Stato:** Confermata · 01/10/2026

- **Cosa:** login e registrazione: 10 tentativi ogni 15 minuti per IP. Agente: 20 messaggi al giorno per utente. Tutto il resto: 120 richieste al minuto per IP. CORS aperto solo al dominio del frontend.
- **Come:** un filtro Spring per i limiti per IP (contatori in memoria) e il conteggio dei messaggi salvati per l'agente (D40). Il dominio del frontend arriva da una variabile d'ambiente.
- **Perché:** il limite sul login frena chi prova a indovinare le password; quello sull'agente protegge il credito dell'API (D6); quello generale evita abusi. Con CORS ristretto nessun altro sito può usare le API dal browser di un utente.
- **Scartate:** nessun limite; libreria esterna per i limiti (per questi numeri basta un filtro semplice).
- **Limite noto:** i contatori in memoria si azzerano al riavvio del server; per una demo va bene.

## D49 · Catalogo ed eventi leggibili senza login
**Stato:** Confermata · 01/10/2026

- **Cosa:** le API `GET` del catalogo di gioco e degli eventi sono pubbliche.
- **Come:** Spring Security lascia aperte solo quelle in lettura; creare, modificare e cancellare eventi resta riservato all'admin.
- **Perché:** sono dati pubblici del gioco, senza nulla di personale. La pagina di presentazione può mostrare personaggi ed eventi a chi non è ancora registrato.
- **Scartate:** tutto dietro login (nessun vantaggio di sicurezza reale).

## D50 · La configurazione guidata si salva in una sola richiesta
**Stato:** Confermata · 01/10/2026

- **Cosa:** i tre passi della configurazione si inviano insieme alla fine.
- **Come:** il frontend tiene le scelte in Redux mentre l'utente va avanti; all'ultimo passo invia `POST /api/me/onboarding`, che il backend salva in un'unica transazione.
- **Perché:** non restano mai configurazioni a metà nel database. Se una parte non è valida, non si salva niente e l'utente vede subito cosa correggere.
- **Scartate:** una richiesta per ogni passo (stati intermedi da gestire e da ripulire).

## D51 · Un solo endpoint per il calendario
**Stato:** Confermata · 01/10/2026

- **Cosa:** `GET /api/me/calendar` restituisce insieme eventi, attività e proposta in attesa di un intervallo di date.
- **Come:** il service unisce eventi (con le date calcolate) e attività in una risposta. Per creare e modificare le attività restano gli endpoint di `/api/me/tasks`.
- **Perché:** la pagina principale si carica con una sola chiamata e i dati sono sempre coerenti tra loro.
- **Scartate:** tre chiamate separate dal frontend.

## D52 · Risposta dell'agente sincrona in v2
**Stato:** Confermata · 01/10/2026 · i dettagli sono in D61

- **Cosa:** il frontend invia il messaggio e aspetta la risposta completa, che può richiedere alcuni secondi.
- **Come:** una normale richiesta POST; nel frattempo la chat mostra che l'agente sta lavorando. La risposta contiene il messaggio, l'eventuale proposta e il conteggio delle richieste usate.
- **Perché:** è il modo più semplice di integrare l'agente. Lo streaming (testo che compare man mano) è un miglioramento che valutiamo nella fase 6.
- **Scartate per ora:** streaming con Server-Sent Events.

## D53 · Swagger pubblico
**Stato:** Confermata · 01/10/2026

- **Cosa:** la documentazione interattiva delle API è visibile a tutti.
- **Come:** springdoc-openapi con il pulsante per inserire il token JWT e provare gli endpoint protetti. Ogni endpoint ha descrizione ed esempi.
- **Perché:** il regolamento la indica come tocco professionale, e chi valuta il progetto può esplorare le API senza leggere il codice. Non espone nulla di segreto: senza token gli endpoint protetti restano chiusi.
- **Scartate:** Swagger solo in locale (lo staff non lo vedrebbe sulla versione online).

## D54 · Ciclo dell'agente scritto da noi, con l'SDK Java ufficiale
**Stato:** Confermata · 01/10/2026 · il ciclo resta nostro, l'SDK di Anthropic è sostituito da `RestClient` e OpenRouter (D126, 17/10/2026)

- **Cosa:** il ciclo "Claude chiede uno strumento → il backend lo esegue → restituisce il risultato" è codice nostro, con l'SDK Java ufficiale di Anthropic.
- **Come:** un `AgentService` ripete la chiamata finché Claude chiede strumenti, per un massimo di 6 giri. Gli strumenti richiesti nello stesso turno si eseguono tutti e i risultati tornano in un unico messaggio. Strumenti e istruzioni di sistema sono all'inizio della richiesta e marcati per la cache dei prompt; si verifica che la cache funzioni guardando i token letti dalla cache nelle risposte.
- **Perché:** il ciclo è breve, si spiega facilmente all'esame e lascia il pieno controllo su limiti, errori e passaggi da mostrare. Non dipende da funzioni ancora in beta.
- **Scartate:** Tool Runner dell'SDK (comodo, ma in beta per Java e meno trasparente da spiegare); librerie di terze parti per agenti.

## D55 · Cinque strumenti, tutti limitati all'utente del token
**Stato:** Confermata · 01/10/2026

- **Cosa:** `get_player_status`, `get_events`, `get_calendar`, `plan_schedule`, `create_plan_proposal`.
- **Come:** ogni strumento è un metodo Java che riceve l'utente dal contesto della richiesta. Nessuno strumento ha un parametro `userId`. Parametri validati come quelli delle API.
- **Perché:** pochi strumenti ben definiti vogliono dire meno giri del ciclo, quindi meno costi e meno errori. Senza `userId` il modello non può raggiungere i dati di altri utenti, qualunque cosa gli si scriva.
- **Scartate:** uno strumento per ogni endpoint (troppi giri); strumenti generici come "esegui una query" (pericolosi).

## D56 · Il modello non scrive mai le attività del piano
**Stato:** Confermata · 01/10/2026

- **Cosa:** la proposta salvata è sempre l'ultima bozza prodotta da `plan_schedule` nello stesso turno.
- **Come:** `create_plan_proposal` riceve solo la spiegazione (`summary`). Per cambiare il piano il modello richiama `plan_schedule` con parametri diversi.
- **Perché:** ogni numero del piano (giorni, run, Waveplate, minuti) viene dal pianificatore Java. Il modello sceglie e spiega, non calcola (D3).
- **Scartate:** il modello che passa l'elenco delle attività (numeri inventabili, JSON lungo e soggetto a errori).

## D57 · Pianificatore deterministico giorno per giorno
**Stato:** Confermata · 01/10/2026

- **Cosa:** `plan_schedule` distribuisce attività di eventi e run di farming nei giorni, rispettando Waveplate e tempo di gioco.
- **Come:** algoritmo "greedy" (prende la scelta migliore giorno per giorno): prima le attività degli eventi, poi le fonti con ricompense doppie, poi le altre secondo la priorità dei personaggi. Dettagli in `06-agente.md`.
- **Perché:** semplice da implementare, testare e spiegare, e dà risultati sensati per la demo. Lo stesso servizio fa funzionare anche il piano base senza IA (D64).
- **Scartate:** ottimizzazione matematica completa (troppo complessa per due settimane).
- **Limite noto:** non risparmia Waveplate in vista di eventi futuri.

## D58 · Istruzioni di sistema e contesto
**Stato:** Confermata · 01/10/2026

- **Cosa:** istruzioni brevi in italiano (testo in `06-agente.md`); a ogni richiesta si inviano gli ultimi 10 messaggi della chat e la data di oggi.
- **Come:** istruzioni e strumenti fissi in testa, storico e messaggio nuovo in coda. La data di oggi va nel messaggio nuovo, non nelle istruzioni, così le istruzioni restano identiche e la cache funziona.
- **Perché:** istruzioni chiare e brevi danno risposte più coerenti. Lo storico limitato tiene sotto controllo i token; i dati aggiornati arrivano comunque dagli strumenti.
- **Scartate:** inviare tutta la chat (costi che crescono senza limite); mettere i dati dell'utente nelle istruzioni (diventerebbero vecchi e romperebbero la cache).

## D59 · Modello configurabile
**Stato:** Confermata · 01/10/2026 · Claude Haiku 4.5 · modello cambiato da D126 (17/10/2026): `nvidia/nemotron-3-super-120b-a12b:free`

- **Cosa:** il modello si cambia in configurazione. Le opzioni sono Claude Haiku 4.5, Claude Sonnet 5.5 e Claude Opus 5.5; costi stimati in `06-agente.md`.
- **Come:** proprietà di configurazione con il nome del modello e, per Sonnet 5.5 e Opus 5.5, il livello di ragionamento (effort).
- **Perché:** si può sviluppare con un modello e cambiarlo per la demo senza toccare il codice. Siccome calcoli e calendario li fa Java, al modello resta il compito di capire la richiesta, scegliere i parametri e spiegare.
- **Scelta:** Claude Haiku 4.5, il più economico, senza ragionamento esteso. Per passare a un altro modello basta cambiare la configurazione.

## D60 · Sicurezza dell'agente
**Stato:** Confermata · 01/10/2026

- **Cosa:** l'agente può solo leggere i dati del suo utente e creare bozze; i testi che arrivano dagli strumenti sono trattati come dati.
- **Come:** strumenti senza operazioni distruttive (D55, D56); istruzioni di sistema che lo dicono esplicitamente; chiave API solo sul server; log con utente, strumenti, token e durata, ma senza il testo dei messaggi.
- **Perché:** anche un tentativo riuscito di manipolare l'agente non può produrre danni, perché gli strumenti non lo permettono. I log senza testi rispettano la privacy (GDPR).
- **Scartate:** affidare la sicurezza solo alle istruzioni del prompt.

## D61 · Errori e limiti di una richiesta
**Stato:** Confermata · 01/10/2026 · gli errori di OpenRouter, compreso il 429 del piano gratuito, sono in D126

- **Cosa:** massimo 6 giri, messaggi utente fino a 1.000 caratteri, 60 secondi di attesa per ogni chiamata all'API, risposta sincrona (D52).
- **Come:** gli errori di uno strumento tornano al modello come risultato di errore, così può correggersi. Gli errori dell'API diventano 502 `AGENT_UNAVAILABLE`. Se una richiesta fallisce, i messaggi non si salvano e non contano nel limite giornaliero.
- **Perché:** il costo massimo di un messaggio è prevedibile, l'utente non resta in attesa per sempre e non perde richieste per colpa di un errore.
- **Scartate:** nessun limite di giri (costi imprevedibili se il modello entra in un ciclo).

## D62 · Passaggi mostrati dopo la risposta
**Stato:** Confermata · 01/10/2026 · modifica il flusso F6

- **Cosa:** durante l'attesa la chat mostra un indicatore; con la risposta arriva l'elenco dei passaggi fatti ("Ho letto il tuo roster", "Ho calcolato il piano").
- **Come:** il backend ricava i passaggi dagli strumenti chiamati e li restituisce nel campo `steps` della risposta.
- **Perché:** con la risposta sincrona non si possono mostrare i passaggi in tempo reale, ma mostrarli dopo rende comunque chiaro cosa ha fatto l'agente. I passaggi vengono dal backend, quindi sono sempre veri.
- **Scartate per ora:** passaggi in tempo reale con lo streaming (miglioramento futuro).

## D63 · Come testiamo l'agente
**Stato:** Confermata · 01/10/2026

- **Cosa:** test JUnit di pianificatore e strumenti; test del ciclo con un finto client dell'API; dieci scenari reali verificati prima della demo.
- **Come:** il client dell'API sta dietro un'interfaccia Java, così nei test si sostituisce con una versione che restituisce risposte preparate. Gli scenari reali sono elencati in `06-agente.md`.
- **Perché:** la parte deterministica si testa in modo completo e gratuito; il comportamento del modello si controlla con pochi scenari mirati, a costo quasi zero.
- **Scartate:** test automatici contro l'API vera a ogni build (costosi e con risultati che variano).

## D64 · Piano base senza IA
**Stato:** Confermata · 01/10/2026 · aggiunta allo scope v2

- **Cosa:** un pulsante "Genera piano base" crea una proposta con il pianificatore e parametri predefiniti, senza chiamare l'API.
- **Come:** endpoint `POST /api/me/proposals/basic`, che usa lo stesso servizio di `plan_schedule` e un testo di spiegazione standard. Non conta nel limite dell'agente.
- **Perché:** la demo funziona anche se l'API non risponde o il limite è finito. Mostra anche che il pianificatore è un pezzo indipendente, e che l'agente aggiunge la personalizzazione e la spiegazione a parole.
- **Scartate:** nessuna alternativa all'agente (la demo dipenderebbe del tutto da un servizio esterno).

## D65 · Tema scuro ispirato al gioco
**Stato:** Confermata · 01/10/2026

- **Cosa:** solo tema scuro in v1: sfondo blu notte, accento dorato, un colore per ogni elemento.
- **Come:** Bootstrap personalizzato con variabili SASS. Gli stati non dipendono solo dal colore (D70).
- **Perché:** richiama l'interfaccia di Wuthering Waves e rende la demo riconoscibile. Un tema solo dimezza il lavoro sui colori e sui contrasti.
- **Scartate per ora:** tema chiaro e scuro con selettore (doppio lavoro sui colori personalizzati: eventuale v3).

## D66 · Layout del calendario
**Stato:** Confermata · 01/10/2026

- **Cosa:** su computer, calendario a sinistra e pannello a destra con Waveplate e chat dell'agente. Su telefono, calendario a tutta larghezza e chat in un pannello a scomparsa.
- **Come:** griglia di Bootstrap per il computer; componente Offcanvas di Bootstrap per la chat su telefono.
- **Perché:** su computer si vede l'anteprima del piano nel calendario mentre si parla con l'agente (D2). Su telefono non c'è spazio per entrambi.
- **Scartate:** chat in una pagina separata (l'anteprima nel calendario non si vedrebbe mentre si chatta).

## D67 · Viste del calendario senza orari
**Stato:** Confermata · 01/10/2026

- **Cosa:** vista settimanale e mensile a giorni interi; su telefono un elenco per giorno.
- **Come:** viste `dayGridWeek`, `dayGridMonth` e `listWeek` di FullCalendar.
- **Perché:** le attività hanno un giorno e una durata, non un orario (D16). Una griglia a ore sarebbe vuota e fuorviante; su telefono una griglia di 7 colonne è illeggibile.
- **Scartate:** vista a ore (`timeGridWeek`).

## D68 · La proposta si vede nel calendario
**Stato:** Confermata · 01/10/2026

- **Cosa:** le attività proposte appaiono tratteggiate con la scritta "Proposta"; una barra fissa in alto riassume la proposta e offre Conferma e Scarta.
- **Come:** il calendario riceve la proposta in attesa insieme a eventi e attività (D51) e le dà uno stile diverso.
- **Perché:** l'utente vede l'effetto del piano prima di accettarlo, cioè il punto centrale di D2.
- **Scartate:** proposta visibile solo come elenco nella chat.

## D69 · Stati e conferme in ogni pagina
**Stato:** Confermata · 01/10/2026

- **Cosa:** ogni pagina gestisce caricamento, contenuto vuoto ed errore; le azioni riuscite mostrano una notifica breve; le azioni irreversibili chiedono conferma.
- **Come:** componenti comuni `LoadingState`, `EmptyState`, `ErrorState`, toast e modali di Bootstrap. I pulsanti si disattivano durante l'invio.
- **Perché:** l'app non resta mai bianca o muta, e nessuno cancella qualcosa per errore. Durante la demo dal vivo, uno stato di errore chiaro vale molto più di una pagina vuota.
- **Scartate:** finestre native del browser (`window.confirm`), non personalizzabili.

## D70 · Accessibilità di livello AA
**Stato:** Confermata · 01/10/2026

- **Cosa:** obiettivo WCAG 2.2 livello AA.
- **Come:** contrasto minimo 4,5:1, uso completo da tastiera, etichette su tutti i campi, stati non solo a colori, annunci per le risposte dell'agente.
- **Perché:** è un requisito di qualità professionale e si verifica facilmente con Lighthouse.
- **Scartate:** nessuna attenzione specifica all'accessibilità.

## D71 · Lingua dell'interfaccia
**Stato:** Confermata · 01/10/2026

- **Cosa:** interfaccia e percorsi in italiano; nomi del gioco in inglese come nelle fonti; codice in inglese.
- **Come:** testi italiani nei componenti; tipi di evento tradotti per l'utente ("Ricompense doppie") ma salvati in inglese (`DOUBLE_DROP`).
- **Perché:** il pubblico della demo è italiano, mentre i nomi di gioco in inglese sono quelli che i giocatori cercano sul wiki.
- **Scartate:** interfaccia multilingua (fuori dal tempo disponibile).

## D72 · Pagina di presentazione pubblica
**Stato:** Confermata · 01/10/2026

- **Cosa:** spiegazione in una frase, tre passi di "come funziona", i sei personaggi della demo, crediti.
- **Come:** dati dei personaggi dal catalogo pubblico (D49); pulsante "Prova la demo" verso la registrazione (e verso l'accesso ospite in v3).
- **Perché:** è la prima cosa che vede un recruiter dal link del portfolio: deve capire subito cosa fa l'app.
- **Scartate:** login come prima pagina (chi arriva non saprebbe cosa sta per provare).

## D73 · Regole di accesso alle pagine
**Stato:** Confermata · 01/10/2026

- **Cosa:** le pagine protette portano al login; la configurazione incompleta porta alla configurazione; l'area admin è riservata all'admin.
- **Come:** un componente `ProtectedRoute` con React Router che legge utente e ruolo dallo stato Redux. I controlli veri restano nel backend (D44): il frontend serve solo a guidare l'utente.
- **Perché:** l'utente finisce sempre nella pagina giusta, senza vedere schermate che non può usare.
- **Scartate:** controlli solo nel frontend (aggirabili; per questo il backend controlla comunque ogni richiesta).

## D74 · Un solo repository
**Stato:** Confermata · 01/10/2026

- **Cosa:** backend, frontend e documentazione nello stesso repository GitHub.
- **Come:** cartelle `backend/`, `frontend/` e `docs/`, un README principale, una sola configurazione dei controlli automatici.
- **Perché:** allo staff e ai recruiter basta un link. Una modifica che tocca API e interfaccia sta in una sola pull request.
- **Scartate:** due repository separati (due README, due link, modifiche collegate divise).

## D75 · Backend organizzato per funzionalità
**Stato:** Confermata · 01/10/2026

- **Cosa:** pacchetti come `auth`, `roster`, `planning`, `agent`, ognuno con i suoi controller, service, repository, entità e DTO.
- **Come:** un pacchetto `common` per configurazione, sicurezza, errori e `ApiResponse`. Le dipendenze vanno sempre controller → service → repository.
- **Perché:** tutto quello che riguarda una funzionalità sta in un posto solo, ed è più facile da trovare e da spiegare.
- **Scartate:** pacchetti per tipo (`controllers`, `services`...), comodi all'inizio ma dispersivi quando i file crescono.

## D76 · Frontend organizzato per funzionalità
**Stato:** Confermata · 01/10/2026

- **Cosa:** cartelle in `features/` (auth, onboarding, calendar, roster, profile, agent, admin) con componenti, slice Redux e chiamate API; componenti condivisi a parte.
- **Come:** un unico client HTTP basato su `fetch` aggiunge il token, apre il formato `ApiResponse` e trasforma gli errori in un tipo unico.
- **Perché:** stessa logica del backend; la gestione di token ed errori sta in un punto solo invece che in ogni componente.
- **Scartate:** cartelle per tipo (`components`, `reducers`...); librerie HTTP aggiuntive come axios (`fetch` basta ed è quello del corso).

## D77 · Versioni degli strumenti
**Stato:** Confermata · 01/10/2026

- **Cosa:** Java 21 LTS, Maven, Spring Boot e strumenti frontend nelle ultime versioni stabili, Node.js LTS, PostgreSQL 16 o superiore.
- **Come:** le versioni esatte si fissano il primo giorno e si scrivono nel README.
- **Perché:** versioni con supporto attivo. La stessa versione di PostgreSQL in locale e online evita sorprese.
- **Scartate:** versioni non più supportate.

## D78 · Flusso di lavoro con Git
**Stato:** Confermata · 01/10/2026

- **Cosa:** `main` sempre funzionante; un ramo per ogni attività; pull request unite solo con i controlli verdi; Conventional Commits; tag per ogni rilascio.
- **Come:** rami `feat/...`, `fix/...`, `docs/...`; unione in un solo commit (*squash merge*); almeno un push al giorno.
- **Perché:** il regolamento chiede di non pubblicare codice rotto e di fare push ogni giorno. Uno storico ordinato è anche un biglietto da visita nel portfolio.
- **Scartate:** lavorare sempre direttamente su `main`.

## D79 · Strategia di test
**Stato:** Confermata · 01/10/2026

- **Cosa:** test unitari e di integrazione nel backend (settimana 16), test unitari nel frontend (settimana 10), checklist di prova manuale dei flussi principali prima di ogni rilascio. Test scritti prima del codice per i calcoli. Copertura minima dell'80% su service, calcoli e logica del frontend.
- **Come:** JUnit 5 e Mockito nel backend, con i test di integrazione su un database PostgreSQL di test avviato con Docker Compose; Vitest e React Testing Library nel frontend; JaCoCo e Vitest per la copertura. La checklist è in `08-regole-di-lavoro.md`.
- **Perché:** i calcoli sono il cuore del progetto e devono essere sicuramente giusti. I test di integrazione richiedono un vero PostgreSQL per l'indice parziale (D37) e le migrazioni Flyway (D38). Tutti gli strumenti, tranne JaCoCo per misurare la copertura, sono quelli del corso.
- **Facoltativo:** Testcontainers, che avvia da solo il PostgreSQL di test; da aggiungere solo se avanza tempo.
- **Scartate:** H2 in memoria (non supporta tutto quello che usiamo di PostgreSQL); test end-to-end con Playwright (fuori dal programma e costosi da imparare in due settimane).

## D80 · Formattazione e analisi statica
**Stato:** Confermata · 01/10/2026

- **Cosa:** formattatore dell'IDE per il Java, ESLint per il frontend.
- **Come:** il formattatore dell'IDE si applica prima di ogni commit; ESLint arriva già configurato con il progetto Vite e si lancia con `npm run lint`. Se nella settimana 16 avete usato uno strumento di analisi statica, si usa quello.
- **Perché:** il codice resta ordinato, come chiede il regolamento ("codice perfetto e professionale"), senza strumenti nuovi da imparare.
- **Scartate:** Spotless e Prettier (utili, ma nuovi rispetto al corso e non indispensabili).

## D81 · Controlli prima di unire, GitHub Actions facoltativo
**Stato:** Confermata · 01/10/2026 · GitHub Actions è nuovo rispetto al corso

- **Cosa:** prima di unire una pull request si lanciano test, lint e build in locale. Se avanza tempo, gli stessi controlli girano da soli con GitHub Actions.
- **Come:** due comandi documentati nel README (uno per il backend, uno per il frontend). GitHub Actions è un file `.github/workflows/ci.yml` con un database PostgreSQL di servizio per i test di integrazione.
- **Perché:** il regolamento chiede di non pubblicare codice rotto. I controlli in locale bastano; quelli automatici li rendono impossibili da dimenticare e fanno bella figura nel portfolio, ma sono uno strumento in più da imparare.
- **Scartate:** unire senza controlli.

## D82 · Deploy tutto su Render
**Stato:** Confermata · 10/10/2026 · scelta dell'utente, sostituisce la versione del 01/10 con il database su Neon · piani ricontrollati e tempi di avvio misurati al primo deploy (11/10/2026)

- **Cosa:** tutto su Render: frontend come sito statico, backend come immagine Docker, database come PostgreSQL di Render.
- **Come:** pubblicazione automatica da `main`. Il frontend mostra un messaggio se il backend si sta riaccendendo; prima del Demo Day si apre il sito con qualche minuto di anticipo. Il backend riceve l'indirizzo del database dalle variabili d'ambiente di Render.
- **Perché:** Render è la piattaforma usata nella settimana 20. Con una sola piattaforma c'è un solo pannello da gestire e niente strumenti fuori dal programma.
- **Scartate:** database su Neon (la versione precedente di questa decisione: non scade, ma è una piattaforma in più fuori dal programma); Vercel per il frontend (una piattaforma in più senza vantaggi reali).
- **Limiti noti:**
  - il backend gratuito si spegne dopo 15 minuti di inattività; al risveglio la prima risposta arriva in poco meno di 3 minuti (2 min e 43 s, misurati l'11/10/2026 con 0,1 CPU), quindi il messaggio del frontend dice "fino a tre minuti";
  - il PostgreSQL gratuito di Render scade 30 giorni dopo la creazione; dopo altri 14 giorni di tolleranza viene cancellato, dati compresi.
- **Come si gestisce la scadenza:** il database si ricrea senza perdere nulla di importante. Lo schema lo rifà Flyway (D38) e catalogo, eventi demo e account admin li ricarica l'avvio del backend (D39). Si perdono solo gli account creati da chi ha provato la demo. Va creato o ricreato a meno di 30 giorni dal Demo Day e rinnovato ogni mese finché il progetto deve restare nel portfolio, oppure si passa al piano a pagamento di Render.

## D83 · Configurazione con variabili d'ambiente
**Stato:** Confermata · 01/10/2026

- **Cosa:** tre profili Spring (`dev`, `test`, `prod`); tutti i segreti e gli indirizzi in variabili d'ambiente.
- **Come:** `.env.example` nel repository senza valori, `.env` escluso da Git. All'avvio il backend controlla le variabili obbligatorie e si ferma con un messaggio chiaro se ne manca una.
- **Perché:** nessun segreto finisce su GitHub, e un errore di configurazione si scopre subito invece che alla prima richiesta.
- **Scartate:** valori scritti in `application.yml`.

## D84 · Docker per lo sviluppo e il deploy
**Stato:** Confermata · 01/10/2026

- **Cosa:** `docker compose up` avvia PostgreSQL in locale; il backend ha un `Dockerfile` in più fasi, usato da Render.
- **Come:** la prima fase compila con Maven, la seconda contiene solo Java e il file eseguibile.
- **Perché:** chiunque cloni il progetto ha il database con un comando. L'immagine finale è piccola e uguale in locale e online.
- **Scartate:** PostgreSQL installato a mano sul computer.

## D85 · README in italiano con riassunto in inglese
**Stato:** Confermata · 01/10/2026

- **Cosa:** README completo in italiano, con una breve sezione in inglese.
- **Come:** contenuto elencato in `08-regole-di-lavoro.md`: demo, Swagger, stack, avvio, variabili, test, limiti noti, crediti.
- **Perché:** staff e mercato principale sono italiani; il riassunto in inglese rende il progetto leggibile anche per recruiter stranieri.
- **Scartate:** solo italiano o solo inglese.

## D86 · Regole per usare Claude Code
**Stato:** Confermata · 01/10/2026

- **Cosa:** un file `CLAUDE.md` con le convenzioni del progetto; ogni modifica letta e capita prima di accettarla; revisione del codice prima di ogni pull request.
- **Come:** `CLAUDE.md` riassume stack, struttura, convenzioni e rimanda a `docs/decisioni.md`.
- **Perché:** il regolamento chiede di saper discutere ogni dettaglio del progetto. Claude Code accelera il lavoro, ma il codice deve restare tuo.
- **Scartate:** accettare le modifiche senza rileggerle.

## D87 · Board Trello
**Stato:** Confermata · 02/10/2026

- **Cosa:** cinque colonne (Backlog, Da fare, In corso, In verifica, Fatto), etichette per rilascio e area, una card per ogni attività di `09-task.md`.
- **Come:** titolo della card con il codice dell'attività; nella descrizione le decisioni di riferimento e il criterio "fatta quando"; una checklist con la definizione di "fatto". Al massimo due card "In corso" alla volta.
- **Perché:** il regolamento chiede di seguire l'avanzamento su Trello o Asana. Con i codici, board, documenti e commit si collegano tra loro.
- **Scartate:** board con una card per funzionalità intera (troppo grandi per vedere l'avanzamento giorno per giorno).

## D88 · Fase 0 durante l'attesa dell'approvazione
**Stato:** Confermata · 02/10/2026

- **Cosa:** raccolta dei dati di gioco e preparazione della board prima dei 14 giorni.
- **Come:** attività P01-P06 di `09-task.md`, con il browser per leggere le fonti (piano di raccolta in `03-dati-demo.md`).
- **Perché:** non è codice, quindi rispetta il consiglio del regolamento di non iniziare subito a programmare. Al giorno 1 i dati sono già pronti e i 14 giorni vanno tutti allo sviluppo.
- **Scartate:** raccogliere i dati durante lo sviluppo (toglierebbe almeno un giorno).

## D89 · Primo deploy il giorno 2
**Stato:** Confermata · 02/10/2026

- **Cosa:** al secondo giorno backend, frontend e database sono già online, anche se l'app fa ancora pochissimo.
- **Come:** attività T10, subito dopo il catalogo pubblico.
- **Perché:** i problemi di deploy (variabili, CORS, connessione al database) si scoprono quando c'è poco codice e c'è tutto il tempo per risolverli. Da lì ogni rilascio è solo un push.
- **Scartate:** deploy alla fine della v1 o del progetto.

## D90 · Giorni 11-12 come margine, poi extra in ordine di priorità
**Stato:** Confermata · 02/10/2026

- **Cosa:** i giorni 11 e 12 servono prima a recuperare i ritardi; il tempo che avanza va agli extra in quest'ordine: export Excel, accesso ospite, notifiche in tempo reale, GitHub Actions, damage calculator.
- **Come:** gli extra stanno nel Backlog di Trello e si spostano in "Da fare" solo con v1 e v2 complete.
- **Perché:** l'ordine mette prima ciò che costa poco e vale molto. Il damage calculator è ultimo perché richiede una progettazione e dati che oggi non abbiamo.
- **Scartate:** extra pianificati come obbligatori (un ritardo li farebbe saltare comunque, ma con il rischio di trascinare v1 e v2).

## D91 · Piano B in caso di ritardo
**Stato:** Confermata · 02/10/2026

- **Cosa:** regole decise in anticipo su cosa rimandare se il lavoro rallenta.
- **Come:** tabella in `09-task.md`. La priorità è sempre: v1 completa, poi piano base, poi agente, poi extra.
- **Perché:** decidere a mente fredda cosa tagliare evita di farlo di corsa l'ultimo giorno. Il piano base garantisce una demo funzionante anche se l'agente desse problemi.
- **Scartate:** decidere caso per caso al momento.

## D92 · Fase SOL3 fissa per le ricompense
**Stato:** Confermata · 02/10/2026 · nata dalla raccolta dati

- **Cosa:** tutte le ricompense di Simulation Training e Boss Challenge sono quelle della fase SOL3 4 (Union Level 30).
- **Come:** il valore è in `game-rules.json` (`sol3Phase`); i drop medi in `farming-sources.json` sono quelli della fase 4.
- **Perché:** nel gioco le ricompense crescono con la fase. Una fase sola tiene i dati semplici. La 4 è una fase intermedia: dalla 4 in su le ricompense crescono poco.
- **Scartate per ora:** far scegliere la fase all'utente (servirebbero i drop di tutte le otto fasi per ogni fonte).

## D93 · Costo in Shell Credit del livellamento come regola
**Stato:** Confermata · 02/10/2026 · nata dalla raccolta dati · modifica la fase 4

- **Cosa:** salire di livello costa 0,35 Shell Credit per ogni punto EXP.
- **Come:** `shellCreditPerExp` in `game-rules.json`. La colonna `credit_cost_to_next` di `level_exp` non serve più.
- **Perché:** il wiki conferma che il costo è proporzionale all'EXP. Una regola sola sostituisce 39 valori da copiare.
- **Scartate:** il costo salvato livello per livello (dati ripetuti).

## D94 · Effetti degli eventi per tipo di fonte, con limite giornaliero
**Stato:** Confermata · 02/10/2026 · nata dalla raccolta dati · modifica la fase 4

- **Cosa:** l'effetto di un evento si applica a un tipo di fonte (per esempio tutta la Simulation Training) e può avere un limite di run al giorno.
- **Come:** `effect_farming_source_id` diventa `effect_source_type`; si aggiunge `effect_daily_limit`. Il pianificatore e il completamento delle attività applicano il moltiplicatore solo alle prime run del giorno, fino al limite.
- **Perché:** l'evento reale Bountiful Crescendo raddoppia Simulation Training e Forgery Challenge insieme, ma solo per 3 run al giorno in totale. Senza il limite il piano prometterebbe materiali che il gioco non dà.
- **Scartate:** effetto su una sola fonte senza limite (non rappresenta l'evento reale).

## D95 · Evento di combattimento a livelli
**Stato:** Confermata · 02/10/2026 · nata dalla raccolta dati · corregge D18

- **Cosa:** il primo tipo di evento di D18 diventa "combattimento a livelli" (`COMBAT_STAGES`) invece di "combattimento con sblocchi giornalieri".
- **Come:** l'evento ha una serie di livelli, ognuno un'attività da fare una volta; se un livello si sblocca solo da un certo giorno, lo si indica con `unlockDay` (D96).
- **Perché:** l'evento reale scelto, Lament Recon: Solaris Soldier, ha 7 drill, ma il wiki non conferma che si sblocchino un giorno alla volta. Il nuovo tipo descrive l'evento senza inventare dettagli.
- **Scartate:** tenere "sblocchi giornalieri" (non verificato).

## D96 · Disponibilità degli eventi giorno per giorno
**Stato:** Confermata · 02/10/2026 · richiesta dall'utente, dettagli confermati

- **Cosa:** un evento può essere attivo solo in certi giorni della settimana (per esempio il lunedì sì e il martedì no), e una sua attività può sbloccarsi solo da un certo giorno dell'evento. L'utente vede chiaramente, giorno per giorno, quali eventi sono attivi.
- **Come:**
  - nei dati: `activeWeekdays` nell'evento (vuoto = tutti i giorni) e `unlockDay` nelle attività (vuoto = dal primo giorno);
  - nel database: tabella `event_active_weekday` e colonna `event_activity.unlock_day`;
  - nelle API: ogni evento restituisce l'elenco dei giorni in cui è attivo nell'intervallo richiesto (`activeDates`);
  - nel pianificatore: attività ed effetti solo nei giorni attivi e dopo lo sblocco;
  - nell'interfaccia: il calendario mostra l'evento solo nei giorni attivi; ogni giorno elenca gli "eventi attivi oggi"; il dettaglio dell'evento indica i giorni ("attivo lun, mer, ven"); il form admin ha le caselle dei giorni.
- **Perché:** un piano realistico deve sapere in quali giorni un evento c'è davvero, e l'utente deve capirlo a colpo d'occhio per organizzarsi.
- **Scartate:** eventi sempre continui dall'inizio alla fine (non rappresentano gli eventi giornalieri).

## D97 · Stime per nemici comuni e specialità
**Stato:** Confermata · 02/10/2026 · nata dalla raccolta dati

- **Cosa:** 10 minuti di caccia danno in media 5 materiali LF e 2 MF; 10 minuti di raccolta danno 6 specialità locali.
- **Come:** valori in `farming-sources.json` con `estimated: true`, mostrati nell'app come stima (D25). Il limite di 400 uccisioni al giorno e la ricomparsa delle specialità non sono modellati.
- **Perché:** il wiki non riporta probabilità di drop né tempi di ricomparsa. Le quantità della demo sono così basse che quei limiti non si raggiungono mai.
- **Scartate:** lasciare questi materiali fuori dal calcolo (il piano sarebbe incompleto).

## D98 · Tipi numerici dello schema
**Stato:** Confermata · 10/10/2026 · nata dalla revisione di `V1__schema.sql` (T04)

- **Cosa:** tutte le colonne numeriche intere usano `integer`, mai `smallint`; le quantità medie dei drop restano `numeric(8,2)`, quindi i dati hanno al massimo due decimali.
- **Come:** in `V1__schema.sql` le nove colonne prima `smallint` (rarità, gradi, livelli, posizioni, limite giornaliero) sono `integer` e le entità useranno `Integer`. In `farming-sources.json` la media della Premium Resonance Potion passa da 1,099 a 1,10.
- **Perché:** Hibernate 7 in modalità `validate` (D38) rifiuta un campo `Integer` su una colonna `smallint`, e la colonna d'ordine di una lista JPA è sempre un intero. Con tabelle così piccole lo spazio risparmiato da `smallint` non conta. Con `numeric(8,2)` PostgreSQL avrebbe arrotondato 1,099 senza avvisare: meglio arrotondare il dato in modo esplicito (differenza dello 0,1%).
- **Scartate:** tenere `smallint` e usare `Short` nelle entità (conversioni scomode in tutto il codice); `numeric(8,3)` per conservare il terzo decimale.

## D99 · Libreria JWT: JJWT
**Stato:** Confermata · 12/10/2026 · nata dall'inizio di T12

- **Cosa:** i token JWT si creano e si leggono con JJWT (`io.jsonwebtoken`), versione 0.13.0.
- **Come:** dipendenze `jjwt-api`, `jjwt-impl` e `jjwt-jackson`; la versione sta in una proprietà del `pom.xml`, perché Spring Boot non la gestisce. Tutto il codice che usa la libreria sta in `JwtService`. La chiave è fatta dai byte UTF-8 di `JWT_SECRET` (testo semplice, non base64) e la firma è sempre HS256. Il token contiene l'identificativo dell'utente (`sub`), il ruolo e le date di emissione e scadenza (24 ore, D45). Il filtro `JwtAuthenticationFilter` viene creato dentro `SecurityConfig` e non è un bean, altrimenti Spring Boot lo registrerebbe due volte. Con un token mancante, scaduto o non valido il filtro non identifica nessuno e la richiesta prosegue: i percorsi pubblici funzionano, quelli protetti rispondono 401.
- **Perché:** è la libreria vista nel corso (D45 sceglie già il metodo del corso) e lascia a noi il controllo del filtro, che serve per tenere usabile il catalogo anche con un token scaduto. Con Spring Boot 4 il test che crea e rilegge un token passa.
- **Limiti noti:** l'ultima versione (0.13.0, agosto 2025) non supporta Jackson 3. `jjwt-jackson` porta quindi anche Jackson 2, accanto al Jackson 3 di Spring Boot 4: i pacchetti sono diversi e nei test non c'è nessun conflitto. Se la libreria non venisse più aggiornata, si cambia solo `JwtService`.
- **Scartate:** il resource server di Spring con Nimbus (ufficiale, ma fuori dal programma; per quanto ne so risponde 401 a un token scaduto anche sui percorsi pubblici, quindi andrebbe aggirato); `jjwt-gson` (sostituirebbe Jackson 2 con Gson, senza vantaggi).

## D100 · Regole di username, password e account admin
**Stato:** Confermata · 12/10/2026 · nata da T12

- **Cosa:** lo username ha da 3 a 30 caratteri tra lettere (di qualsiasi alfabeto), numeri, punto, trattino e underscore. La password ha da 8 caratteri a 72 byte. L'email si salva in minuscolo e senza spazi ai lati. L'account admin si crea all'avvio solo se manca, e nessuno può registrare il suo username o la sua email.
- **Come:**
  - **Username:** una lista di caratteri ammessi (`@Pattern`), non "tutto tranne gli spazi ai lati".
  - **Password:** il limite si controlla sui byte con `@MaxUtf8Bytes`, non sui caratteri (`@Size`). Una password di soli spazi è ammessa, quindi al login si controlla solo che non sia vuota.
  - **Corpo delle richieste:** Jackson rifiuta qualsiasi stringa JSON oltre 20.000 caratteri (`spring.jackson.factory.constraints.read.max-string-length`), prima di leggerla in memoria.
  - **Login:** email sconosciuta e password sbagliata danno la stessa risposta (401 `BAD_CREDENTIALS`) e lo stesso tempo, perché con un'email sconosciuta si controlla comunque un hash BCrypt.
  - **Registrazione:** una richiesta in gara che perde sugli indici unici di email e username riceve lo stesso 409 delle altre. Lo username `admin` (in qualsiasi combinazione di maiuscole) e l'email di `ADMIN_EMAIL` rispondono 409 come se fossero già usati.
  - **Admin:** `AdminSeeder` crea l'account con username `admin` e non modifica mai un account esistente. Se l'email di `ADMIN_EMAIL` è già di un account non admin, l'avvio prosegue con un avviso e quell'account non diventa admin. Se lo username `admin` è di un altro account, l'avvio si ferma con un messaggio che lo spiega. Per cambiare la password dell'admin dopo la creazione si elimina la sua riga in `users` e si riavvia.
  - **Token:** `JwtService` accetta solo i token HS256 con una data di scadenza.
- **Perché:**
  - **Byte e non caratteri:** BCrypt usa solo i primi 72 byte. Spring Security 7.1.1 fa fallire `encode` con un'eccezione oltre quel limite (verificato), quindi con `@Size` una password lunga o piena di emoji (4 byte l'una) darebbe un errore 500 invece di un 400.
  - **Lista di caratteri per lo username:** così non può contenere spazi, caratteri di controllo (il NUL, che PostgreSQL non salva, darebbe un 500) né caratteri invisibili o che invertono la direzione del testo.
  - **Limite di 20.000 caratteri:** login e registrazione sono i primi percorsi pubblici con un corpo; il limite predefinito di Jackson (100 milioni di caratteri) permetterebbe a una sola richiesta di riempire i 512 MB del server gratuito.
  - **Login con tempo uguale:** altrimenti il tempo di risposta rivelerebbe quali email sono registrate.
  - **Admin mai promosso:** promuovere un account esistente darebbe il controllo a chi si è registrato per primo con quell'email.
  - **Identità dell'admin riservata:** l'account admin si crea poco dopo che il server ha iniziato ad ascoltare, e il database di Render va ricreato prima del 10/11/2026. In quei secondi qualcuno potrebbe registrare `admin` o l'email dell'admin, e l'avvio successivo non riuscirebbe a creare l'account.
  - **Admin creato una volta sola:** così una password cambiata dopo resiste ai riavvii del piano gratuito, come per gli eventi (D39).
- **Limiti noti:**
  - La registrazione dice se un'email o uno username sono già usati, perché il flusso F1 lo prevede (messaggio sotto il campo): è un compromesso con la semplicità d'uso. T13 limita anche la registrazione.
  - L'unicità dello username distingue le maiuscole (`Rover` e `rover` sono due account). Lo username non è mostrato agli altri utenti, quindi non c'è un rischio di scambio di persona; se un giorno lo fosse, servirebbe un indice su `lower(username)`.
  - Dopo aver ricreato il database va cambiato anche `JWT_SECRET`: gli identificativi ripartono da 1 e un token ancora valido del vecchio database indicherebbe un altro utente.
- **Scartate:**
  - **Password:** nessun requisito di complessità (maiuscole, simboli), perché per una demo basta la lunghezza minima.
  - **Admin:** aggiornare la password dell'admin a ogni avvio da `ADMIN_PASSWORD`; promuovere l'account trovato con la stessa email; creare l'admin prima che il server ascolti (più lontano dalla struttura di D39).

## D101 · Come funzionano i limiti di richieste
**Stato:** Confermata · 12/10/2026 · nata da T13

- **Cosa:** i numeri sono quelli di D48. Login e registrazione hanno un contatore ciascuno (10 tentativi ogni 15 minuti per indirizzo); tutte le altre richieste hanno un contatore comune (120 al minuto); il controllo di salute ha un contatore generoso a parte (600 al minuto). Superato il limite la risposta è 429 `RATE_LIMITED`, con l'header `Retry-After` e un messaggio che dice quanto aspettare.
- **Come:**
  - **Filtro:** `RateLimitFilter`, con contatori in memoria e senza librerie esterne (D48). Lo crea `SecurityConfig` e sta dopo il filtro CORS e prima di quello JWT: il 429 mantiene gli header CORS e una richiesta rifiutata non costa quasi nulla. `Retry-After` è tra gli header che il CORS lascia leggere al frontend.
  - **Finestra fissa:** la prima richiesta apre la finestra e il contatore riparte quando finisce. Un tentativo conta anche se riesce.
  - **Indirizzo del client:** quello che Tomcat ricava dagli header del proxy (`getRemoteAddr()`). Per IPv6 si usano i primi 64 bit.
  - **Dietro Render e Cloudflare:** la richiesta passa da Cloudflare e da un proxy di Render, e `X-Forwarded-For` finisce con "client vero, nodo Cloudflare". Il primo deploy ha mostrato (12/10/2026) che Tomcat si fermava al nodo Cloudflare, diverso quasi a ogni richiesta: 50 tentativi di login su 56 passavano. Per questo `application-prod.yml` aggiunge gli intervalli di Cloudflare ai proxy fidati di Tomcat (`server.tomcat.remoteip.internal-proxies`): Tomcat salta il nodo e si ferma al client vero, che Cloudflare ha aggiunto e che nessun client può falsificare. Un test con un Tomcat vero e il profilo `prod` (`ClientAddressBehindProxyTest`) lo verifica. Quello che non è un indirizzo (con una porta attaccata, un nome, un testo lungo) va in un unico contatore "unknown": un valore sbagliato limita troppo e si vede subito, invece di non limitare per niente.
  - **Controllo di salute:** `/actuator/health` ha un contatore suo perché Render lo chiama di continuo e un 429 potrebbe farlo riavviare, ma non è illimitato perché interroga il database. Le richieste di verifica del CORS (preflight) non arrivano al filtro.
  - **Memoria:** i contatori scaduti si eliminano ogni minuto e ce ne sono al massimo 200.000 (circa 36 MB). Se sono tutti occupati, login e registrazione di un client nuovo ricevono 429, mentre il resto dell'API passa senza contatore.
  - **Valori modificabili:** proprietà `app.rate-limit.*`, cioè le variabili d'ambiente `APP_RATELIMIT_AUTHATTEMPTS`, `APP_RATELIMIT_AUTHWINDOW`, `APP_RATELIMIT_GENERALREQUESTS` e `APP_RATELIMIT_GENERALWINDOW`. I valori predefiniti sono quelli di D48. Le finestre ammesse vanno da 1 secondo a 24 ore, i limiti da 1 a 1.000.000; un numero semplice è in minuti per il login e in secondi per il resto.
  - **Log:** all'avvio si registrano i valori in uso. Al primo rifiuto di ogni finestra si registrano il contatore, l'indirizzo e gli header `X-Forwarded-For` e `CF-Connecting-IP` (ripuliti dai caratteri strani).
  - **`/logout`:** il percorso predefinito di Spring Security è disattivato: il logout è il frontend che dimentica il token (D45).
- **Perché:**
  - **Contatori separati:** la registrazione costa un calcolo BCrypt e fa da test per scoprire le email già usate; il login frena chi indovina le password. Con un contatore unico, chi si registra consumerebbe i tentativi del login e viceversa.
  - **Finestra fissa:** è la più semplice da spiegare e da testare. Il suo difetto (fino al doppio dei tentativi a cavallo di due finestre) non conta con questi numeri.
  - **Primi 64 bit di IPv6:** chi usa IPv6 possiede miliardi di indirizzi della sua rete e cambierebbe indirizzo a ogni tentativo.
  - **Valori modificabili:** servono ai test (altrimenti tutte le classi di test condividerebbero un contatore) e al Demo Day: se i valutatori usano la stessa rete si alza il limite con una variabile d'ambiente, senza cambiare il codice. I limiti sui valori evitano che un errore di scrittura (per esempio una finestra di 15 millisecondi) spenga la protezione senza avvisare.
  - **Memoria piena e catalogo aperto:** le chiavi le sceglie chi manda le richieste, e il server gratuito ha 512 MB; ma il catalogo pubblico non deve diventare irraggiungibile perché qualcuno riempie i contatori.
- **Limiti noti:**
  - I contatori si azzerano al riavvio (D48) e più istanze del server non li condividerebbero (su Render ce n'è una sola).
  - Gli intervalli di Cloudflare (letti il 12/10/2026 da https://www.cloudflare.com/ips) vanno ricontrollati ogni tanto: se ne mancasse uno, i client che passano da lì tornerebbero a essere contati per nodo.
  - Chi esce da indirizzi Cloudflare (WARP, Workers) viene saltato anche lui, e può scegliersi l'indirizzo scrivendolo in `X-Forwarded-For`. È un caso raro che accettiamo.
  - Chi dispone di molti indirizzi IPv4 diversi aggira un limite per indirizzo. Chi ha una rete IPv6 più grande di un /64 (un /48 ne contiene 65.536) ha altrettanti contatori. Una casa o un ufficio con IPv6 condivide invece un solo contatore, come con l'IPv4 dietro un router.
- **Scartate:** una libreria per i limiti (D48); un contatore unico per login e registrazione; un contatore per email (permetterebbe di bloccare dall'esterno l'account di una persona che si conosce, e le chiavi le sceglierebbe chi attacca); un limite totale sul login (un solo client potrebbe bloccare tutti); la finestra scorrevole (più codice per lo stesso risultato in una demo).

## D102 · Modifica ed eliminazione del profilo
**Stato:** Confermata · 12/10/2026 · nata da T14

- **Cosa:** `PATCH /api/me` cambia solo lo username e il tempo di gioco. Email, ruolo e gli altri campi dell'account non si cambiano: se arrivano nella richiesta si ignorano. I campi che mancano restano come sono, e una richiesta vuota è ammessa e non cambia niente. Il tempo di gioco deve essere una delle opzioni delle regole di gioco (30, 60 o 120 minuti). `DELETE /api/me` cancella l'account con tutti i suoi dati. Se il token appartiene a un utente che non esiste più, i tre endpoint rispondono 401.
- **Come:**
  - **Username:** stesse regole della registrazione (formato di D100, non quello di un altro utente, non `admin`), con un solo controllo condiviso in `UserService`. Tenere il proprio username va bene.
  - **Errori su un campo:** `FieldValidationException` per le regole che conosce solo il service (D47) risponde 400 con il campo, come la validazione del corpo.
  - **Tutto o niente:** se un valore non è valido non si salva niente. Se due richieste prendono lo stesso username insieme, l'indice unico decide e la perdente riceve lo stesso 409; ogni altro errore del database non si traveste da "username già usato".
  - **Aggiornamenti contemporanei:** `@DynamicUpdate` su `User`: un `UPDATE` scrive solo le colonne cambiate, così due richieste che modificano campi diversi dello stesso utente (profilo, configurazione guidata, Waveplate) non si annullano a vicenda.
  - **Eliminazione:** una sola istruzione `delete from users`; il database cancella a cascata roster, inventario, proposte, attività e messaggi (D42). Un test lo prova con righe in tutte le cinque tabelle, compresi i collegamenti verso le proposte.
  - **Swagger:** il pulsante "Authorize" per inserire il token (D53); solo gli endpoint del profilo mostrano il lucchetto.
- **Perché:**
  - **Email non modificabile:** è la credenziale di accesso, e cambiarla vorrebbe dire verificare il nuovo indirizzo, ma la demo non invia email.
  - **Campi ignorati:** il DTO della modifica non contiene ruolo, email o identificativo, quindi nessuno può cambiarli per errore o per attacco (mass assignment).
  - **401 per l'utente scomparso:** il token resta valido fino alla scadenza (D45); senza questo controllo la richiesta finirebbe in un errore di chiave esterna, cioè un 500. Vale anche per le card successive.
  - **Un'istruzione sola:** non serve caricare l'utente con tutte le sue relazioni, e la cascata resta nel database.
- **Limiti noti:**
  - L'admin può eliminare il proprio account (il profilo vale per USER e ADMIN): al riavvio successivo `AdminSeeder` lo ricrea con `ADMIN_PASSWORD`.
  - La conferma scrivendo la propria email (F12) è solo nel frontend; il backend si fida del token. Chi riesce a rubare il token (per esempio con un attacco XSS) può quindi eliminare l'account, e lo stesso vale per un clic su "Try it out" in Swagger: la descrizione del pulsante "Authorize" lo avverte.
  - Solo gli endpoint sotto `/api/me` controllano che l'utente del token esista ancora. Il token di un admin eliminato (per esempio cambiando la password dell'admin con "elimina la riga e riavvia") resterebbe valido come ADMIN su `/api/admin/**` fino alla scadenza: la card degli endpoint admin deve controllare che l'utente esista e sia ancora admin, e per annullare tutti i token insieme si cambia `JWT_SECRET`.
  - Con `PATCH` si può provare uno username dopo l'altro (409 se è usato, 200 se è libero) fino a 120 volte al minuto per indirizzo, molto più che con la registrazione. Gli username non sono segreti né credenziali (il login usa l'email), quindi lo accettiamo.
  - Caratteri di altri alfabeti simili a quelli latini (una "а" cirillica) aggirano il controllo sul nome `admin`. Oggi nessuna schermata mostra gli username agli altri utenti, quindi non c'è un rischio di scambio di persona; se un giorno li mostrasse, servirebbe un controllo sui caratteri simili.
  - Un numero con decimali (`60.9`) o scritto come testo (`"60"`) viene letto come `60`, e un campo `null` vale come un campo mancante.
- **Scartate:** cambiare l'email; `PUT` al posto di `PATCH`; 404 invece di 401 per l'utente eliminato (l'utente non è una risorsa che si cerca: è l'identità del token); eliminazione "morbida" con un contrassegno invece della cancellazione (non rispetterebbe il diritto all'oblio di D42).

## D103 · Sessione, accesso alle pagine e Content-Security-Policy nel frontend
**Stato:** Confermata · 13/10/2026 · nata da T15

- **Cosa:** il token sta nel `localStorage` (D45) e a ogni avvio l'app chiede a `GET /api/me` chi è. Se il server rifiuta il token (scaduto, account eliminato) la sessione finisce con un avviso e si torna al login; dopo il nuovo login si ritorna alla pagina che si stava aprendo. Le pagine seguono la tabella di D73. Il sito statico manda la Content-Security-Policy qui sotto.
- **Come:**
  - **Sessione:** `authSlice` tiene token e utente; solo il token si salva nel browser (`tokenStorage`, con `try/catch` perché in una finestra privata il `localStorage` può dare errore). `restoreSession` parte da `main.tsx` prima del primo disegno; se il server non risponde, il token resta e la pagina propone "Riprova". Il client HTTP chiama un gestore, con il token rifiutato, quando una richiesta con token riceve 401 (anche se la risposta non ha il formato `ApiResponse`): lo registra `main.tsx`, così il client non dipende da Redux. La sessione finisce solo se quel token è ancora quello attuale, così la risposta in ritardo di una sessione già chiusa non butta fuori la successiva.
  - **Più schede:** un login o un logout in un'altra scheda dello stesso browser vale anche qui (evento `storage`, `crossTabSync.ts`). Un token fallito non cancella quello che un'altra scheda ha salvato nel frattempo.
  - **Token nel browser:** si accetta solo ciò che ha la forma di un JWT (tre parti, al massimo 2048 caratteri); il resto si butta, così non viene mai mandato al server come intestazione. Il client accetta solo percorsi che iniziano con una sola `/`, perché il token va solo al nostro backend.
  - **Redux DevTools** sono accesi solo in sviluppo: le azioni di login e registrazione contengono la password e il token.
  - **Accesso alle pagine:** `ProtectedRoute` (con le regole `role` e `onboarding`) e `GuestOnly` per login e registrazione. Le pagine che arrivano con le card successive (calendario, configurazione, roster, profilo, eventi admin) sono per ora la stessa pagina segnaposto, così le regole di D73 funzionano e si provano subito.
  - **Moduli:** il server controlla ogni campo e la pagina mostra il suo messaggio sotto il campo giusto (`fields`), con il fuoco sul primo campo sbagliato; gli altri errori (credenziali, troppe richieste, server non raggiungibile) vanno in un avviso. Per questo le risposte 409 sull'email e sullo username portano anche `fields` (modifica compatibile del backend, `ApiException` può nominare un campo).
  - **Logout (F11):** prima si torna alla presentazione, poi il token sparisce. `flushSync` serve perché in un browser vero, senza, il logout avveniva mentre la pagina privata era ancora a schermo e mandava al login.
  - **Content-Security-Policy:** definita in `frontend/csp.ts` e usata da `npm run preview`, che serve la build con lo stesso header. Su Render si incolla nella sezione Headers del sito statico, per il percorso `/*`:
    `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self' https://resonance-planner-api.onrender.com; font-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`
    Insieme alla CSP, nella stessa sezione, `X-Content-Type-Options: nosniff` (il sito risponde con la pagina iniziale anche per un percorso inesistente, e senza questa intestazione il browser potrebbe leggerla come script). Dopo averle incollate si controlla con `curl -I` la pagina iniziale, un percorso come `/login` e un file in `/assets/`: tutti e tre devono avere le intestazioni.
- **Perché:**
  - **Chiedere l'utente al server:** il token da solo non dice se l'account esiste ancora, e salvare anche l'utente lo lascerebbe fuori data.
  - **Gestore del 401 nel client:** una sola regola per tutte le pagine (anche quelle delle card future), invece di controllare il 401 in ogni schermata.
  - **Messaggi dal server:** un'unica fonte per le regole (formato dello username, lunghezza della password) e nessun controllo ripetuto nel frontend.
  - **CSP:** D45 la sceglie per ridurre il danno di un attacco XSS con il token nel `localStorage`. Senza script o stili in linea e senza script di altri siti, uno script iniettato non può girare, e senza altre destinazioni in `connect-src` non può mandare dati a un altro sito con una richiesta. Verificata sul sito compilato: nessuna violazione, e registrazione, login e logout funzionano.
  - **Navigazione e focus:** al cambio di pagina il focus va al contenuto (la pagina che si lascia ha tolto l'elemento che lo aveva) e ogni pagina ha il suo titolo; le notifiche sono annunciate da un'area `status` sempre presente (gli errori hanno un `alert` proprio), non si chiudono mentre il mouse o la tastiera sono sopra e un errore dura di più.
- **Limiti noti:**
  - Il `localStorage` resta leggibile da qualsiasi script che giri nella pagina: la CSP lo rende molto più difficile, non impossibile (D45). Uno script che girasse comunque potrebbe usare il token contro il nostro backend (anche per eliminare l'account) o uscire dalla pagina portandolo nell'indirizzo; la CSP non ha una direttiva per la navigazione. Anche il backend potrebbe fare da cassetta delle lettere: lo script si registra con un altro account e scrive pezzi del token nello username (`PATCH /api/me`), poi li legge dal suo profilo. Serve già uno script in esecuzione, quindi è un rischio residuo, che crescerebbe se un giorno ci fosse un campo di testo libero.
  - Il calendario (T26) avrebbe potuto inserire gli stili con uno `<style>` creato da JavaScript, che `style-src 'self'` blocca. Verificato il 15/10/2026: con FullCalendar 7 (D119) gli stili sono file CSS della build e la CSP resta com'è.
  - Quando le prossime schede aggiungeranno dati dell'utente nello stato Redux (roster, piano), `sessionEnded` dovrà azzerare anche quelli, altrimenti l'utente successivo nella stessa visita li vedrebbe per un momento.
  - La CSP su Render si imposta a mano dal pannello e non sta nel repository: va rifatta se si ricrea il sito statico. Il server di sviluppo di Vite non la manda.
  - Il pulsante "Esci" non invalida il token sul server: resta valido fino alla scadenza (D45).
  - Caricare l'app con un token salvato costa una richiesta (`GET /api/me`) prima di mostrare le pagine private; con il backend gratuito appena svegliato può volerci del tempo, e `LoadingState` lo spiega.
- **Scartate:** i cookie `httpOnly` e il refresh token (già scartati da D45); salvare nel browser tutto lo stato Redux con un pacchetto come `redux-persist` (complica, e l'utente andrebbe comunque riverificato); una libreria per i moduli (due moduli semplici); intestazioni di sicurezza in più oltre alla CSP (possibili in seguito: `X-Content-Type-Options` e `Referrer-Policy`).

## D104 · Livello e grado di ascensione: intervalli validi
**Stato:** Confermata · 14/10/2026

- **Cosa:** il grado `r` ammette i livelli da `ascensionLevels[r-1]` (1 per il grado 0) a `ascensionLevels[r]`. Con i valori attuali: grado 0 = livelli 1-20, grado 1 = 20-40, grado 2 = solo 40. L'obiettivo di un personaggio deve essere uguale o superiore alla situazione attuale, confrontando prima il grado e poi il livello.
- **Come:** una classe di regole pure `RosterRules` (T18), che legge `ascensionLevels` da `game-rules.json` e non ha database. Una combinazione non valida risponde 400 con il nome del campo (`FieldValidationException`).
- **Perché:** D34 dice a che livello arriva ogni grado ma non il minimo. Il livello 20 esiste sia prima dell'ascensione (grado 0) sia dopo (grado 1): senza un minimo per grado, "livello 5 e grado 1" sarebbe accettato anche se nel gioco non esiste.
- **Scartate:** controllare solo il massimo del grado (accetta combinazioni impossibili); grado ricavato dal livello (non distingue il prima e il dopo dell'ascensione, vedi D34).

## D105 · Waveplate e Crystals sono due valori indipendenti
**Stato:** Confermata · 14/10/2026

- **Cosa:** l'utente può salvare qualsiasi coppia di valori con Waveplate da 0 a 240 e Crystals da 0 a 480. I Crystals restano accumulati quando le Waveplate vengono spese; ricominciano a crescere solo quando le Waveplate sono di nuovo al massimo.
- **Come:** `WaveplateRules.requireValidReading` controlla i due intervalli e dà 400 con il campo (`value` o `crystals`). `WaveplateRules.stateAt` fa partire i Crystals dal momento in cui le Waveplate sono piene, non dalla lettura. Il momento della lettura lo sceglie sempre il server (`Clock`, arrotondato ai microsecondi), mai il client. `fullAt` e `crystalsFullAt` possono essere nel passato: sono il momento in cui il contatore è diventato pieno.
- **Perché:** nel gioco chi ha speso le Waveplate conserva i Crystals (per esempio 100 Waveplate e 300 Crystals insieme). La prima versione di questa decisione, che accettava Crystals solo a Waveplate piene, avrebbe rifiutato letture reali: l'ha corretta l'utente durante la revisione di T17.
- **Scartate:** vietare i Crystals a Waveplate non piene (rifiuta stati veri del gioco); far scegliere al client il momento della lettura (manipolabile e dipendente dall'orologio del dispositivo).

## D106 · Configurazione guidata: regole di validazione
**Stato:** Confermata · 14/10/2026

- **Cosa:** `POST /api/me/onboarding` (T20) richiede almeno un personaggio, nessun personaggio ripetuto, un tempo di gioco tra le opzioni di `game-rules.json` e una lettura di Waveplate valida (D105). Ogni personaggio rispetta D104. L'inventario non fa parte della richiesta: parte vuoto (D21). `GET /api/me/inventory` restituisce solo i materiali già salvati.
- **Come:** tutti i controlli prima di scrivere, poi un'unica transazione (D50): una richiesta non valida non salva niente e risponde 400 con tutti i campi sbagliati che riesce a indicare; se la configurazione è già completata risponde 409.
- **Perché:** l'utente vede subito cosa correggere e il database non contiene mai configurazioni a metà.
- **Scartate:** ignorare in silenzio i duplicati (l'utente non saprebbe quale valore è stato usato); permettere zero personaggi (il calendario e il piano non avrebbero niente da calcolare).

## D107 · I numeri interi nell'API devono essere interi
**Stato:** Confermata · 14/10/2026

- **Cosa:** un valore con la virgola dove serve un numero intero (per esempio `12.5` o `100.0` per le Waveplate) viene rifiutato con 400, invece di essere troncato in silenzio.
- **Come:** `spring.jackson.deserialization.accept-float-as-int: false` in `application.yml`: vale per tutta l'API. Il corpo illeggibile risponde "Richiesta non valida." senza il dettaglio di Jackson e senza il campo `fields`; il frontend deve mandare numeri interi (lo fa già). Le stringhe numeriche (`"180"`) restano accettate dalla coercizione predefinita di Jackson: se si vuole vietarle serve `allow-coercion-of-scalars: false`, che però agisce ovunque.
- **Perché:** livelli, Waveplate e quantità sono sempre interi: accettare `12.5` come `12` nasconderebbe un errore del client. Il `playTimeMinutes: 60.0` del profilo ora dà 400 invece di essere accettato.
- **Scartate:** controllare il decimale campo per campo (ripetuto in ogni DTO); lasciare il comportamento predefinito (troncamento silenzioso).

## D108 · Inventario: limite, errori e scrittura in un'unica istruzione
**Stato:** Confermata · 14/10/2026

- **Cosa:** `PUT /api/me/inventory/{materialId}` imposta la quantità di un materiale (un numero intero da 0 a 999.999.999) al posto di quella di prima, e la prima volta crea la voce. Un materiale che non è nel catalogo dà 404; una quantità negativa, troppo grande, mancante o non intera dà 400 con il campo `quantity`. `GET /api/me/inventory` elenca solo i materiali già salvati (D106) in ordine di identificativo, con `materialId` e `quantity`.
- **Come:** la quantità si scrive con un solo `insert ... on conflict (user_id, material_id) do update`, senza prima controllare se la riga c'è. L'entità `InventoryItem` è in sola lettura (`@Immutable`). Il repository estende `Repository` e non `JpaRepository`, così `findAll`, `findById` e `deleteById` (che non hanno l'utente) non esistono; lo stesso vale ora per il roster.
- **Perché:** con "controlla e poi inserisci" due richieste uguali, mandate insieme, farebbero fallire la seconda sull'indice univoco; con un'unica istruzione vince l'ultima, senza errori (c'è un test con 8 richieste in parallelo). Il tetto lascia spazio alle somme delle card successive: due valori al massimo stanno in un `int`, tre no, quindi chi somma quantità (T30, T22) deve usare `long` o `Math.addExact`.
- **Scartate:** una voce per ogni materiale del catalogo, anche a zero (22 righe da creare a ogni registrazione e un catalogo che cambia lascerebbe buchi); correggere la quantità con `POST` e `PATCH` separati (due modi per fare la stessa cosa); nessun tetto (una somma potrebbe superare il limite dell'`int` in silenzio).

## D109 · La configurazione guidata sostituisce quello che c'era
**Stato:** Confermata · 14/10/2026

- **Cosa:** `POST /api/me/onboarding` (T20) è il primo quadro completo dell'utente: i personaggi che ha scelto sostituiscono il roster già presente (aggiunto prima con l'API), e le Waveplate e il tempo di gioco sostituiscono quelli salvati prima. L'inventario non si tocca (D21). Risponde con l'utente aggiornato (`onboardingCompleted: true`), così il frontend aggiorna lo stato senza una seconda richiesta.
- **Come:** l'ordine delle risposte è: corpo di forma sbagliata (400, prima del servizio) → utente che non esiste più (401) → regole del gioco violate (400 con tutti i campi, nomi come `characters[0].currentLevel`, `waveplate.value`, `playTimeMinutes`) → configurazione già completata (409). Quindi chi ha già completato e manda un corpo non valido riceve il 400, non il 409. Poi la configurazione si "prenota" con una sola istruzione (`update ... where onboarding_completed = false`): di due invii nello stesso momento, per esempio un doppio clic, ne vince uno e l'altro riceve 409. Tutto sta in una transazione: se una scrittura fallisce a metà non resta niente. Se la stessa persona aggiunge un personaggio da un'altra scheda nel momento esatto del salvataggio, riceve 409 "il tuo roster è cambiato: riprova".
- **Perché:** il roster va sostituito perché chi ha usato l'API a mano prima del primo accesso (per esempio da Swagger) non deve fallire sul vincolo di unicità: la configurazione dice cosa vuole adesso. Prenotare con una sola istruzione evita due configurazioni sovrapposte senza bloccare la tabella.
- **Scartate:** unire la configurazione al roster già presente (non si capirebbe quale valore vince per un personaggio che c'è in entrambi); rifiutare la configurazione se il roster non è vuoto (l'utente dovrebbe cancellare a mano personaggi che non ha scelto nel passo 1); un controllo "leggi e poi scrivi" sul flag (due invii insieme passerebbero entrambi).

## D110 · Limite alla dimensione del corpo JSON
**Stato:** Confermata · 14/10/2026

- **Cosa:** un corpo JSON più lungo di 100.000 caratteri o con più di 5.000 elementi viene rifiutato con 400 ("Richiesta non valida.") prima di costruire la richiesta.
- **Come:** `spring.jackson.factory.constraints.read.max-document-length` e `max-token-count` in `application.yml`, accanto a `max-string-length` (D100). Valgono per tutta l'API e per la lettura dei file di `data/`, che sono lunghi al massimo 6 KB. Un corpo vero è piccolo: la configurazione guidata con i sei personaggi della demo pesa circa 1 KB.
- **Perché:** `@Size(max = 50)` su una lista scatta solo dopo che Jackson ha creato tutti gli elementi: un utente con un account poteva mandare un corpo di qualche MB (milioni di `{}`) e riempire i 512 MB del server gratuito; il 400 con un campo per ogni elemento sbagliato avrebbe anche amplificato la risposta. Il limite sul numero di elementi e sulla lunghezza ferma la richiesta mentre viene letta.
- **Scartate:** controllare la lunghezza nel `RateLimitFilter` (conta le richieste, non i byte; e il corpo non è ancora letto); un limite in Tomcat (non vale per i corpi JSON); contare solo sul limite di Cloudflare sul corpo (fuori dal nostro controllo e non presente in locale).

## D111 · Il grado di ascensione non si vede: livello e una casella
**Stato:** Confermata · 14/10/2026

- **Cosa:** nella configurazione guidata (e poi nel roster) l'utente sceglie un livello da 1 a 40 e, solo quando il livello è uno dei tetti (20 e 40, da `game-rules.json`), una casella "Ho già fatto l'ascensione al 20" (livello attuale) o "Voglio fare l'ascensione al 40" (obiettivo). Il grado che parte nella richiesta si ricava da livello e casella (D104).
- **Come:** grado = numero di tetti sotto il livello, più 1 se il livello è un tetto e la casella è spuntata. Quando il livello cambia, la casella torna "non fatta". Un personaggio appena scelto parte da livello 1 e ha come obiettivo il livello massimo con l'ascensione (l'obiettivo più comune). Il controllo "obiettivo non più basso dell'attuale" confronta prima il grado e poi il livello, come il backend.
- **Perché:** "livello 20" può essere prima o dopo l'ascensione e il giocatore pensa in livelli, non in gradi. Con questa interfaccia le combinazioni impossibili (per esempio livello 5 con grado 1) non si possono nemmeno scegliere; il server controlla comunque tutto.
- **Scartate:** un campo "grado" accanto al livello (obbliga a conoscere un concetto che il gioco non mostra); due liste di livelli (1-20 prima e dopo l'ascensione) in un'unica select (confuse).

## D112 · Regole del calcolo del farming
**Stato:** Confermata · 14/10/2026

- **Cosa:** `GET /api/me/farming-plan` e `GET /api/me/roster/{characterId}/requirements` (T22) calcolano in Java (D3) i materiali mancanti, le run di ogni fonte, le Waveplate, i minuti e i giorni stimati.
- **Come:**
  1. il fabbisogno di un personaggio è l'EXP dal livello attuale a quello obiettivo (`level_exp`), gli Shell Credit per tutta quell'EXP (D93, per eccesso, personaggio per personaggio: nel piano complessivo al massimo 1 credito in più per personaggio) e i costi delle ascensioni dopo il grado attuale, fino al grado obiettivo compreso;
  2. l'EXP si conta in punti: l'inventario la copre con le Resonance Potion, ognuna per la sua EXP. Gli Shell Credit del livellamento si contano su tutta l'EXP, anche quella coperta dalle pozioni, perché nel gioco si pagano quando le pozioni si usano;
  3. la fonte di un materiale è quella che ne dà di più per run (a parità, quella con l'identificativo che viene prima); per l'EXP conta l'EXP delle pozioni che la fonte dà;
  4. i materiali si calcolano in un ordine fisso (boss, nemici comuni, specialità, EXP e per ultimi gli Shell Credit) e ogni run toglie dal fabbisogno tutto quello che dà, anche i drop secondari;
  5. le run si arrotondano per eccesso; i conti sono esatti, con numeri interi `long` (le medie dei drop hanno al massimo due decimali, D98);
  6. i giorni stimati sono il più grande tra Waveplate ÷ ricarica di un giorno (240) e minuti ÷ tempo di gioco, entrambi per eccesso; se il tempo di gioco non è ancora stato scelto valgono `null`. Waveplate attuali, Crystals ed eventi non contano: li usa il pianificatore (D57);
  7. il piano complessivo somma il fabbisogno di tutto il roster e conta l'inventario una volta sola; la vista di un personaggio considera tutto l'inventario a sua disposizione e aggiunge la percentuale di completamento: per ogni materiale la parte già posseduta (al massimo 100, per difetto), poi la media, per difetto; 100 se non manca niente;
  8. `containsEstimates` è vero se almeno una fonte usata ha un drop stimato (D25, D97).
  9. il catalogo si legge dal database a ogni richiesta (quattro query piccole); un dato mancante (ascensione, livello, fonte di un materiale) è un errore dei dati e dà 500 senza dettagli. Un test calcola tutti i personaggi del catalogo reale da 1 a 40, così l'errore si scopre prima.
- **Perché:** le quattro Resonance Potion si possono scambiare tra loro: contando i punti non serve decidere quali usare. Ricavare la fonte dai dati evita una tabella scritta nel codice. Contare i drop secondari dà un numero vicino a quello che farà il pianificatore (fase 6, passo 5), che a ogni run toglie tutto quello che la run dà; l'ordine fisso mette prima le fonti i cui drop secondari (pozioni e crediti) riducono le run successive. I conti interi evitano gli errori di arrotondamento della virgola mobile.
- **Scartate:** EXP espressa in pozioni di un solo tipo (bisogna scegliere quali usare e quelle già possedute di altri tipi non conterebbero); ignorare i drop secondari (con Jinhsi da 1 a 40 servirebbero 4 run di pozioni invece di 3, 2 di crediti invece di 1 e 3 di Howler invece di 2); una fonte scritta nel codice per ogni materiale; giorni calcolati con le Waveplate attuali e i Crystals (è il lavoro del pianificatore: qui basta una stima); per ora, il catalogo tenuto in memoria (una copia costruita prima del caricamento all'avvio resterebbe vecchia, e quattro query piccole non pesano per la demo).

## D113 · Edizioni degli eventi nell'API
**Stato:** Confermata · 15/10/2026

- **Cosa:** `GET /api/events?from=&to=` (T23) restituisce una voce per ogni edizione che tocca l'intervallo, con le date intere dell'edizione e `activeDates`, i giorni attivi dentro l'intervallo (D96). `GET /api/events/{id}` restituisce l'edizione in corso oppure la prossima.
- **Come:**
  1. l'edizione numero *k* di un evento ricorrente inizia alla data di riferimento (`demoEventsAnchorDate`) + `startOffsetDays` + *k* × `repeatEveryDays` e dura `durationDays` giorni, fine compresa (D29); *k* si ricava con una divisione, senza cicli sugli anni. Un evento `FIXED` ha una sola edizione, con le sue date;
  2. `from` e `to` sono obbligatori, stanno tra il 01/01/2000 e il 31/12/2100, `to` non può venire prima di `from` e l'intervallo copre al massimo 62 giorni, come il calendario; altrimenti 400 con il campo. La finestra di date tiene i calcoli lontani dai limiti di `LocalDate`, dove una data come l'anno 999999999 darebbe un errore 500;
  3. un'edizione senza giorni attivi nell'intervallo (per esempio un evento del weekend in un intervallo dal lunedì al mercoledì) non compare;
  4. le voci sono in ordine di inizio, poi di nome; lo stesso evento può comparire più volte, con date diverse;
  5. "oggi" si calcola con il fuso `Europe/Rome` (D41), nella configurazione `app.time-zone`; l'orologio dell'app resta UTC. Per un evento `FIXED` già finito il dettaglio restituisce comunque le sue date.
- **Perché:** FullCalendar vuole un oggetto per ogni blocco da disegnare: in 62 giorni Bountiful Crescendo ha fino a 5 edizioni. Le date intere dell'edizione dicono quando l'evento inizia e finisce davvero, anche se l'intervallo ne mostra solo una parte. Il limite di 62 giorni tiene piccola la risposta.
- **Scartate:** una voce per evento con l'elenco delle edizioni (il frontend dovrebbe spezzarla da solo); date tagliate all'intervallo (si perderebbe la fine vera dell'evento); `from` e `to` facoltativi con un valore predefinito (una regola in più da spiegare, senza un uso concreto).
- **Limite noto:** non c'è un tetto al numero di eventi. Ogni richiesta pubblica li legge tutti e ripete descrizione, ricompense e attività per ogni edizione: con i pochi eventi della demo la risposta è piccola, ma con centinaia di eventi creati dall'admin andrebbe aggiunto un tetto o una cache.

## D114 · Regole degli eventi creati e modificati dall'admin
**Stato:** Confermata · 15/10/2026

- **Cosa:** `POST` e `PUT /api/admin/events` (T23) accettano un evento solo se rispetta queste regole:

  | Campo | Regole |
  |---|---|
  | Nome e descrizione | Nome da 1 a 120 caratteri, descrizione da 1 a 2.000 |
  | Evento `FIXED` | Inizio e fine obbligatori, tra il 01/01/2000 e il 31/12/2100, fine ≥ inizio, al massimo 366 giorni |
  | Evento `RECURRING` | Scostamento da −3650 a 3650, durata da 1 a 366, ripetizione ≥ durata e ≤ 3650 |
  | Giorni della settimana | Facoltativi: nessuno o tutti e sette = tutti i giorni (D96). Un evento `FIXED` deve avere almeno un giorno scelto tra l'inizio e la fine; un evento `RECURRING` di meno di 7 giorni non può sceglierli, perché le sue edizioni scorrono nella settimana e in alcune i giorni scelti mancherebbero |
  | Ricompense | Al massimo 10, ognuna da 1 a 120 caratteri |
  | Attività | Al massimo 20; titolo da 1 a 120 caratteri; minuti da 1 a 600; `unlockDay` vuoto o da 1 alla durata dell'evento |
  | Effetto | Obbligatorio per `DOUBLE_DROP`, vietato per gli altri tipi; moltiplicatore > 1 e ≤ 10 con al massimo due decimali; limite giornaliero vuoto o da 1 a 100 |

- **Come:** gli errori di forma (campo mancante, testo troppo lungo) li controlla la validazione dei DTO; quelli che dipendono da più campi (date, effetto e tipo, `unlockDay` e durata) una classe di regole pure. Il 400 elenca tutti i campi sbagliati insieme, con nomi come `schedule.endDate`, `activities[0].unlockDay`, `effect.multiplier`. `PUT` sostituisce tutto, anche negli eventi della demo, che conservano il loro `code` (D39). `DELETE` cancella l'evento; le attività del calendario collegate restano, senza il collegamento (`ON DELETE SET NULL`). Ogni endpoint controlla che l'utente del token esista ancora (401) e sia ancora ADMIN (403), perché il token vale 24 ore (D45, D102). Lo scostamento di un evento `RECURRING` si conta dalla data di riferimento degli eventi della demo (`demoEventsAnchorDate`, 01/10/2026).
- **Perché:** i limiti sono larghi per l'uso vero della demo, ma impediscono date fuori scala, corpi enormi e un effetto su un evento che non lo prevede (D94). Un evento di ricompense doppie senza effetto non direbbe al pianificatore cosa raddoppiare.
- **Scartate:** nessun limite oltre a quelli del database (un admin distratto potrebbe creare un evento di 100 anni o con migliaia di attività); effetto facoltativo per tutti i tipi (un effetto su un evento a punti non ha senso nel gioco).
- **Limiti noti:** un corpo di forma sbagliata riceve il 400 prima del controllo dell'admin nel database, come negli altri endpoint (la validazione del DTO avviene prima del service), quindi un token di un admin cancellato con un corpo sbagliato riceve 400 e non 401; il messaggio non rivela dati. Due modifiche dello stesso evento nello stesso momento non sono protette (nessun `@Version`): vince l'ultima, e in casi rari una delle due può dare 500. Per un solo admin della demo basta.

## D115 · La pagina del roster chiede i requisiti personaggio per personaggio
**Stato:** Confermata · 15/10/2026

- **Cosa:** la pagina P5 (T24) carica il roster, l'inventario, il piano complessivo (`/api/me/farming-plan`) e i requisiti di ogni personaggio (`/api/me/roster/{id}/requirements`), una richiesta per personaggio. Dopo ogni modifica (personaggio aggiunto, cambiato o tolto, quantità dell'inventario) ricarica tutto dal server.
- **Come:** un `rosterSlice` tiene questi dati; le richieste partono insieme (`Promise.all`). Al primo caricamento la pagina mostra `LoadingState`; dopo una modifica i dati restano visibili mentre arrivano quelli nuovi. Lo slice si azzera con `sessionEnded` e con ogni nuovo login e ignora le risposte di un caricamento superato (`requestId`), come `onboardingSlice` (D103).
- **Perché:** le API esistono già e i numeri li calcola sempre Java (D3): ricaricare dopo una modifica evita di rifare i conti nel browser. Con i 6 personaggi della demo sono 9 richieste piccole.
- **Scartate:** un nuovo endpoint con i requisiti di tutti i personaggi insieme (altro backend per un guadagno piccolo); aggiornare i numeri nel browser dopo una modifica (calcoli duplicati, che potrebbero dare risultati diversi da quelli del server).

## D116 · Finestra di conferma senza il JavaScript di Bootstrap
**Stato:** Confermata · 15/10/2026

- **Cosa:** le azioni irreversibili (per prima "Rimuovi" nel roster) chiedono conferma in una finestra `ConfirmModal` con gli stili delle modali di Bootstrap (D69), gestita da React.
- **Come:** il componente usa le classi `.modal` e `.modal-backdrop`, senza stili inline (CSP, D103). Quando si apre il focus va su "Annulla", Tab resta dentro la finestra, Esc la chiude e alla chiusura il focus torna al pulsante che l'ha aperta (WCAG 2.1.2, 2.4.3).
- **Perché:** il progetto importa solo gli stili di Bootstrap. Il suo JavaScript cambia il DOM per conto suo, mentre in React è React a decidere cosa c'è nella pagina: due padroni dello stesso elemento creano errori difficili da trovare.
- **Scartate:** `window.confirm` (esclusa da D69, non personalizzabile); `react-bootstrap` (una libreria in più, fuori dal programma del corso); l'elemento nativo `<dialog>` (l'ambiente dei test, jsdom, non lo gestisce del tutto).

## D117 · Attività manuali e calendario
**Stato:** Confermata · 15/10/2026 · nata da T25

- **Cosa:** `POST`, `PUT` e `DELETE /api/me/tasks` gestiscono le attività manuali (D24); `GET /api/me/calendar` restituisce eventi e attività di un intervallo in una sola risposta (D51).
- **Come:**
  - **Campi di un'attività manuale:** titolo da 1 a 120 caratteri (senza spazi ai lati), giorno tra il 01/01/2000 e il 31/12/2100 (la stessa finestra degli eventi), durata da 1 a 1440 minuti, note facoltative fino a 1000 caratteri (vuote = nessuna nota). Origine, tipo e stato non arrivano dal corpo: un'attività manuale è sempre `MANUAL`, `CUSTOM` e nasce `CONFIRMED`.
  - **Modifica e cancellazione:** solo per le attività manuali (409 per quelle dell'agente), anche quando sono già fatte; la modifica sostituisce tutti i campi e lascia lo stato com'è. Un'attività di un altro utente risponde 404.
  - **Limite:** al massimo 500 attività manuali per utente (409 oltre). La creazione blocca la riga dell'utente (vedi D118), così due richieste nello stesso momento non superano il limite.
  - **Calendario:** `events` sono le stesse voci di `/api/events` (una per edizione, con `activeDates`, D113); `tasks` sono le attività `CONFIRMED` e `DONE` dell'intervallo, per giorno e poi in ordine di creazione, con le Waveplate già calcolate (run × costo della fonte, 0 per gli altri tipi). Le date seguono le regole di `/api/events` (al massimo 62 giorni). La proposta in attesa si aggiungerà con le proposte (T30).
- **Perché:** il form del calendario chiede titolo, giorno, durata e note (P4); i limiti sono larghi per l'uso vero ma impediscono testi enormi e milioni di righe per un solo account. Riusare le voci degli eventi evita una seconda forma degli stessi dati: la finestra di dettaglio dell'evento ha già tutto.
- **Scartate:** personaggio collegato all'attività manuale (non è nel form di P4); nessun limite al numero di attività; un campo `pendingProposal` sempre vuoto fino a T30 (un campo senza dati da spiegare).

## D118 · Completamento di un'attività
**Stato:** Confermata · 15/10/2026 · nata da T25

- **Cosa:** `POST /api/me/tasks/{id}/complete` segna come fatta un'attività del calendario; un'attività di farming aggiunge all'inventario quello che le sue run danno in media (D21), con il moltiplicatore di un evento solo per le run entro il limite giornaliero (D94).
- **Come:**
  1. si può completare solo un'attività `CONFIRMED` (409 se è già `DONE` o se è una proposta, scartata o sostituita); non si può annullare (l'inventario si corregge a mano);
  2. per ogni drop della fonte: quantità media × run normali + quantità media × moltiplicatore × run potenziate, **arrotondata per difetto** sul totale dell'attività; un materiale che arriva a 0 non si aggiunge. Tutti i drop contano, anche quelli secondari (pozioni e crediti), come nel calcolo del farming (D112);
  3. il moltiplicatore è quello di un evento **attivo nel giorno dell'attività** (D96) con un effetto sul tipo della fonte; se ce ne sono più di uno vale il più forte (moltiplicatore più alto, poi limite più alto), senza sommarli;
  4. le run potenziate sono le prime del giorno fino al limite: le run dello stesso tipo di fonte già fatte quel giorno (altre attività `DONE`) lo consumano per prime; senza limite tutte le run sono potenziate;
  5. l'inventario si aggiorna con un'istruzione sola per materiale (insert o somma), che si ferma a 999.999.999 (D108);
  6. ogni scrittura delle attività (creazione, modifica, cancellazione, completamento) blocca la riga dell'utente (`SELECT ... FOR NO KEY UPDATE`) fino alla fine della transazione: due completamenti della stessa attività nello stesso momento ne fanno uno (l'altro riceve 409), due attività dello stesso giorno si dividono il limite e una modifica non può riportare indietro un'attività appena completata. Il blocco non ferma le altre scritture dell'utente (inventario, roster);
  7. la risposta contiene l'attività aggiornata e `addedToInventory`, con la quantità che le run hanno dato per ogni materiale, così il frontend può mostrarla. Vicino al massimo l'inventario cresce di meno, perché si ferma a 999.999.999.
- **Perché:** il giorno dell'attività è quello per cui il piano è stato calcolato, e dà un risultato che non dipende da quando l'utente preme il pulsante. L'arrotondamento per difetto non promette materiali che il gioco potrebbe non aver dato, ed è lo stesso criterio delle percentuali (D112). Il blocco sulla riga dell'utente è la soluzione più semplice per due richieste in gara, e una prova con 6 richieste insieme lo verifica.
- **Scartate:** il moltiplicatore del giorno in cui si preme "Segna come fatta" (un'attività segnata in ritardo perderebbe l'evento, e i test dipenderebbero dall'orologio); arrotondare ogni run (con medie sotto 1 un'attività non darebbe mai niente); sommare gli effetti di più eventi (nei dati della demo non succede, e scegliere il più forte non promette materiali in più); impedire di completare un'attività di un giorno futuro (una regola in più, poco utile in una demo).

## D119 · FullCalendar 7 con il tema Bootstrap 5
**Stato:** Confermata · 15/10/2026 · nata da T26

- **Cosa:** il calendario P4 usa FullCalendar 7 con il suo tema Bootstrap 5 (D7, D67).
- **Come:**
  - pacchetti con la versione fissata in `package.json`: `@fullcalendar/react` 7.1.1 (calendario, viste `dayGridWeek`, `dayGridMonth`, `listWeek`, lingua italiana), `@fullcalendar/bootstrap5` 7.1.1, `temporal-polyfill` 1.0.5 (richiesto da FullCalendar 7), `bootstrap-icons` 1.13.1 (richiesto dal tema). Si è scelta la 7.1.1, uscita da più di una settimana, e non la 7.1.2, uscita da tre giorni;
  - gli stili sono file CSS normali (`skeleton.css`, `theme.css`) che Vite mette nella build: la CSP di D103 (`style-src 'self'`) resta com'è. Verificato con la build di produzione e `npm run preview`: nessuna violazione, nessun `<style>` inserito da JavaScript. Gli unici stili in linea sono quelli che React imposta con il DOM (CSSOM), che la CSP permette;
  - il tema prende i colori da Bootstrap e segue `data-bs-theme="dark"`; i colori di eventi e attività sono variabili CSS nostre (`styles/_calendar.scss`), passate agli elementi con `color` e `contrastColor`, con il testo blu notte su sfondi chiari (contrasto da 6:1 a 11:1);
  - la barra dei comandi è nostra, in italiano (periodo precedente, oggi, successivo, Settimana/Mese, "+ Attività"), e guida il calendario con il controller di FullCalendar (`useCalendarController`). Solo `CalendarView` conosce FullCalendar: nei test della pagina è sostituito da un elenco di pulsanti;
  - gli elementi sono raggiungibili da tastiera (pulsanti con Invio) e su telefono, sotto i 768 px, la vista è l'elenco per giorno.
- **Perché:** FullCalendar 6 inserisce i suoi stili con JavaScript, e la CSP li bloccherebbe: si dovrebbe aggiungere `'unsafe-inline'` a `style-src`, come temeva D103. La versione 7 non lo fa. Il tema Bootstrap 5 evita di riscrivere a mano i colori del tema scuro (D65).
- **Scartate:** FullCalendar 6 con `style-src 'unsafe-inline'` (CSP più debole); un calendario scritto da noi (più codice e più accessibilità da curare, per una griglia che FullCalendar fa già); il tema predefinito di FullCalendar (andrebbe ricolorato tutto per il tema scuro).
- **Limiti noti:** FullCalendar 7 è uscito a giugno 2026, quindi è giovane. I pacchetti di FullCalendar e `temporal-polyfill` li pubblica l'autore di FullCalendar dai repository ufficiali, `bootstrap-icons` il team di Bootstrap; nessuno ha script di installazione e `npm audit` non trova vulnerabilità (15/10/2026). Il rischio che resta è quello di ogni dipendenza: un aggiornamento compromesso girerebbe nella stessa pagina del token (D45), per questo le versioni sono fissate. Il bundle JavaScript passa a circa 630 KB (195 KB compressi) e Vite avvisa che supera i 500 KB: si può caricare la pagina del calendario solo quando serve (`lazy` di React Router) se la pagina di presentazione diventa lenta. `bootstrap-icons` aggiunge un font di circa 130 KB, scaricato solo quando serve.

## D120 · Come la pagina del calendario usa i dati
**Stato:** Confermata · 15/10/2026 · nata da T26

- **Cosa:** la pagina P4 (T26) mostra eventi e attività dell'intervallo sullo schermo, le finestre di dettaglio, il modulo delle attività manuali e il riquadro Waveplate. Restano per la v2: barra della proposta, avviso di cambiamento, chat, "Genera piano base" e "Spiegamelo".
- **Come:**
  1. a ogni cambio di periodo o di vista FullCalendar dice quali giorni sono sullo schermo e la pagina chiede `GET /api/me/calendar` per quei giorni. Un `calendarSlice` tiene eventi, attività e intervallo, si azzera al logout e a ogni nuovo login, e ignora le risposte di un caricamento superato (`requestId`, D103);
  2. un evento si disegna con un pezzo per ogni gruppo di giorni attivi consecutivi (D96): attivo lunedì e mercoledì vuol dire due pezzi, e il martedì resta vuoto;
  3. dopo ogni modifica (attività creata, modificata, cancellata o fatta) la pagina chiede di nuovo lo stesso intervallo, come il roster (D115). "Segna come fatta" e "Elimina" chiedono conferma (D69, D116); dopo un completamento la notifica elenca i materiali aggiunti all'inventario e il roster dimentica i suoi numeri, così li ricarica alla prossima visita;
  4. il modulo dell'attività controlla solo il giorno e i minuti interi, per poter mandare una richiesta valida; le altre regole sono del server e i suoi messaggi tornano sotto il campo giusto (D103, D117);
  5. il riquadro Waveplate chiede i valori al server ogni minuto, solo mentre la pagina è visibile, e subito quando torna visibile: il conto lo fa Java (D3). Se un aggiornamento fallisce restano gli ultimi valori, con la scritta "Valori non aggiornati". "Aggiorna" salva i valori letti nel gioco, con lo stesso controllo della configurazione guidata (`validateWaveplate`, ora in comune);
  6. "2 / 3 doppie oggi" della schermata P4 non c'è ancora: si mostra il limite dell'evento ("per le prime 3 run del giorno"). Il numero che resta richiederebbe di rifare nel browser un calcolo del backend: arriverà quando il pianificatore creerà le attività di farming;
  7. le finestre usano un componente `Modal` comune (portale, fuoco dentro la finestra, Esc, fuoco restituito all'elemento che l'ha aperta o, se non c'è più, al contenuto della pagina); `ConfirmModal` è costruita sopra.
- **Perché:** come in D115, i numeri li calcola sempre il server e ricaricare dopo una modifica evita conti doppi. Chiedere i valori delle Waveplate ogni minuto costa una richiesta piccola e tiene il riquadro corretto senza duplicare la formula del backend.
- **Scartate:** caricare tutti gli eventi e le attività una volta sola (senza un intervallo la risposta crescerebbe senza limite); calcolare le Waveplate nel browser (formula duplicata, D3); finestre native del browser (D69).

## D121 · Pagina del profilo
**Stato:** Confermata · 16/10/2026 · nata da T27

- **Cosa:** la pagina P6 (T27) permette di cambiare username e tempo di gioco, di uscire e di eliminare l'account (F11, F12, D42).
- **Come:**
  - "Salva" manda `PATCH /api/me` solo con i campi cambiati (D102), così uno username invariato non viene mai controllato; se non è cambiato niente non parte nessuna richiesta. L'utente aggiornato entra nello stato con `userUpdated`; gli errori del server vanno sotto il campo (`username`, `playTimeMinutes`), con il fuoco sul campo;
  - l'email si vede ma non si cambia;
  - "Elimina account" apre una finestra in cui si scrive la propria email: il pulsante "Elimina definitivamente" si attiva solo quando coincide, senza badare a maiuscole e spazi ai lati (il server salva l'email in minuscolo). Dopo `DELETE /api/me` si esce come con "Esci": prima la pagina di presentazione, poi il token sparisce, poi la notifica;
  - "Esci" della barra e del profilo usano lo stesso hook `useLogout`, con quell'ordine (D103).
- **Perché:** scrivere l'email è la conferma chiesta da F12 per un'operazione che non si può annullare; mandare solo i campi cambiati evita errori su valori che l'utente non ha toccato.
- **Scartate:** conferma con un solo pulsante (troppo facile da premere per sbaglio); chiedere di nuovo la password (il backend non ha un endpoint per verificarla, e la sessione è già autenticata).
- **Limiti noti:** la conferma con l'email protegge da un clic sbagliato, non da chi ha già rubato il token: `DELETE /api/me` chiede solo il token (D45). Anche l'admin può eliminare il proprio account: nessuno può registrarsi con la sua email o con lo username `admin` (D100), e al riavvio successivo il server lo ricrea da `ADMIN_EMAIL` e `ADMIN_PASSWORD`.

## D122 · Pagina admin degli eventi
**Stato:** Confermata · 16/10/2026 · nata da T27

- **Cosa:** la pagina P7 (T27) mostra gli eventi in una tabella e li crea, modifica ed elimina con le API admin (D14, D114).
- **Come:**
  1. l'elenco sta in un `adminEventsSlice`, come roster e calendario: si ricarica dopo ogni modifica, si azzera al logout e a ogni login, e ignora le risposte superate (D103, D115);
  2. la tabella mostra nome (con l'etichetta "Demo" per gli eventi del file, D39), tipo, date ("dal 12/10/2026 al 18/10/2026" oppure "ogni 14 giorni, per 7 giorni") e giorni attivi; su telefono scorre in orizzontale;
  3. il modulo è in una finestra larga. La sua bozza sta nello stato della finestra e non in Redux: esiste solo finché la finestra è aperta, e chiuderla o uscire la cancella. È un'eccezione voluta alla regola di `CLAUDE.md` sulle bozze dei moduli lunghi, nata per la configurazione a tre passi, dove la bozza deve sopravvivere tra un passo e l'altro;
  4. bozza e corpo della richiesta si convertono con funzioni pure (`eventForm.ts`, test scritti prima). I numeri restano testo finché non si salva; la pagina controlla solo quello che serve per mandare un corpo valido (interi, moltiplicatore con la virgola o il punto e al massimo due decimali, almeno un giorno della settimana). Tutte le altre regole sono del server, e i suoi messaggi tornano sotto il campo con lo stesso nome, anche nelle righe delle ricompense e delle attività (`activities[0].unlockDay`); un messaggio per un campo che il modulo non mostra va in cima;
  5. all'inizio i sette giorni sono tutti spuntati (tutti = tutti i giorni, D96); i campi dell'effetto compaiono solo per "Ricompense doppie" (D94);
  6. `GET /api/game/rules` restituisce anche `demoEventsAnchorDate`: il modulo dice da quale giorno si conta l'inizio di un evento che si ripete, senza scrivere la data nel codice del frontend;
  7. "Elimina" chiede conferma e avvisa che le attività collegate restano senza il collegamento; per un evento della demo aggiunge che ricompare al prossimo riavvio del server (D39).
- **Perché:** lo stesso schema delle altre pagine rende la pagina prevedibile; tenere la bozza nella finestra evita uno slice in più per dati che vivono pochi minuti, mentre le funzioni pure testano la parte delicata, cioè la conversione dei numeri e dei campi.
- **Scartate:** una pagina separata per il modulo (`/admin/eventi/nuovo`: più rotte per lo stesso lavoro); la bozza in Redux (più codice, nessun vantaggio per una finestra); controllare nel browser tutte le regole di D114 (doppioni delle regole del server, che potrebbero divergere).
- **Limiti noti:** chiudere la finestra (Annulla, Esc) perde la bozza senza chiedere conferma: per un admin solo, nella demo, è un compromesso accettato. Durante il salvataggio i campi sono bloccati; togliendo una riga spariscono gli errori di quel gruppo, perché gli indici delle righe cambiano.

## D123 · Regole di dettaglio del pianificatore
**Stato:** Confermata · 16/10/2026 · nata da T29 · precisa D57

- **Cosa:** come il pianificatore (D57) sceglie l'ordine dei personaggi, calcola le risorse di ogni giorno e inserisce attività degli eventi e ricompense doppie.
- **Come:**
  1. **priorità dei personaggi:** ogni personaggio ha il suo fabbisogno. L'inventario si assegna nell'ordine di priorità, e ogni run copre prima il personaggio più importante che ha ancora bisogno di quel materiale; quello che avanza va al successivo (per esempio le pozioni di un boss farmato per il primo). Dentro un personaggio l'ordine dei materiali è quello di D112 (boss, nemici, specialità, EXP, crediti) e ogni materiale usa la fonte migliore di D112. I personaggi non nominati in `characterPriority` seguono nell'ordine del roster;
  2. **Waveplate del giorno:** oggi quelle attuali più i Crystals, gli altri giorni quelle che si ricaricano in 24 ore (240). Quelle avanzate non passano al giorno dopo;
  3. **minuti del giorno:** il tempo di gioco meno le attività manuali e le attività già fatte di quel giorno. Le attività dell'agente confermate e non fatte non si sottraggono, perché la conferma del nuovo piano le sostituisce (D22);
  4. **attività degli eventi**, per prime: quelle `DAILY` in ogni giorno attivo dal giorno di sblocco, se non sono già state fatte quel giorno; quelle `ONCE` una volta per edizione, nel primo giorno in cui c'è il tempo, e mai se sono già state fatte nell'edizione. Un'attività che non ci sta nei minuti rimasti si salta;
  5. **ricompense doppie:** in ogni giorno, prima delle altre run, le run sulle fonti del tipo potenziato che servono ancora, fino al limite giornaliero meno le run di quel tipo già fatte. Effetto più forte e run potenziate si calcolano con le stesse funzioni del completamento (`CompletionRewards`, D118), così piano e "Segna come fatta" contano allo stesso modo;
  6. **run normali:** si prende il primo materiale ancora mancante secondo la priorità; se la sua fonte non è più possibile (Waveplate o minuti finiti) si passa al materiale successivo, così le fonti senza Waveplate usano i minuti rimasti;
  7. le run della stessa fonte nello stesso giorno diventano un'unica attività;
  8. **risultato:** per ogni giorno le attività con Waveplate e minuti, poi per ogni materiale quanto manca e quanto il piano copre, e gli avvisi (obiettivi già raggiunti, giorni che servirebbero ancora, stimati con il calcolo di D112);
  9. la resa per run e la scelta della fonte migliore stanno in una classe comune (`FarmingYields`), usata sia dal calcolo del farming sia dal pianificatore.
- **Perché:** la priorità che segue le run è facile da spiegare ("prima finisco Jinhsi") e lascia comunque ai personaggi successivi quello che avanza. Le regole sui minuti e sulle attività già fatte evitano che rigenerare il piano riproponga cose fatte o tolga tempo per attività che stanno per essere sostituite.
- **Scartate:** un fabbisogno unico per tutto il roster come in D112 (la priorità non conterebbe e l'EXP comune passerebbe davanti ai materiali del secondo personaggio); sottrarre le attività dell'agente confermate (il nuovo piano userebbe solo il tempo avanzato); portare le Waveplate avanzate al giorno dopo (nel gioco diventano Crystals, un calcolo in più per una demo).
- **Limiti noti:**
  - oltre a quello di D57, se oggi è escluso i Crystals non si usano nel primo giorno pianificato;
  - un'attività `ONCE` che non ci sta mai nel tempo non genera un avviso;
  - il piano conta le medie in centesimi come D112, mentre il completamento arrotonda per difetto il totale dell'attività (D118): la copertura può promettere qualche unità in più di quella che arriverà davvero nell'inventario;
  - se nello stesso giorno due eventi potenziano tipi di fonte diversi e le Waveplate non bastano, passa prima il tipo che viene prima nell'elenco dei tipi, non quello più utile (nei dati della demo non succede);
  - le run della stessa fonte nello stesso giorno restano attribuite al primo personaggio per cui sono state pianificate;
  - le attività di un evento già fatte si riconoscono dal titolo ("Evento: attività"): se l'admin rinomina l'evento o l'attività, il piano la ripropone.
- **Parametri:** `excludedDates` contiene al massimo 14 date, come i giorni di un piano, così una richiesta non può mandare elenchi enormi.

## D124 · Regole delle proposte
**Stato:** Confermata · 16/10/2026 · nata da T30 · precisa D22, D37, D64 e D68

- **Cosa:** come nasce, si conferma e si scarta una proposta (D22), e come il calendario mostra quella in attesa (D51, D68).
- **Come:**
  1. **piano vuoto:** se il pianificatore non trova nessuna attività, né run né attività degli eventi (per esempio roster vuoto o obiettivi raggiunti senza eventi in corso, oppure giorni tutti esclusi), il piano base risponde 409 con l'avviso del pianificatore e non crea una proposta. Se ci sono attività degli eventi la proposta nasce, e l'avviso ("Il roster è vuoto…") finisce nel testo;
  2. **testo del piano base:** `summary` è un testo standard ("Piano base di 7 giorni, 18 attività.") seguito dagli avvisi del pianificatore, che così si vedono nella barra della proposta;
  3. **la proposta si conferma solo nel giorno in cui è nata** (ora italiana, D41): una proposta di un giorno precedente risponde 409, perché è stata calcolata con le Waveplate e le attività di quel giorno;
  4. **conferma (D22):** le attività della proposta passano da `PROPOSED` a `CONFIRMED`; le attività dell'agente `CONFIRMED` da oggi in poi diventano `REPLACED`. Restano come sono le attività fatte, quelle manuali e quelle dei giorni passati. È la regola su cui conta il pianificatore (D123, punto 3);
  5. **scarto e sostituzione:** la riga della proposta resta con lo stato `REJECTED` o `REPLACED` (è lo storico, e i messaggi della chat la collegano), ma le sue attività `PROPOSED` si cancellano;
  6. **calendario:** `GET /api/me/calendar` restituisce anche `pendingProposal` (`null` se non c'è) con `id`, `summary`, `createdAt` e tutte le sue attività, anche fuori dall'intervallo richiesto (al massimo 14 giorni). `GET /api/me/proposals/pending` restituisce lo stesso oggetto;
  7. **errori:** 404 se la proposta non è dell'utente; 409 se non è più in attesa; 401 se l'utente del token non esiste più (D102);
  8. **richieste insieme:** ogni scrittura blocca la riga dell'utente (`lockById`, come D118); se scatta comunque l'indice di D37, la risposta è 409 e non 500. Prima di inserire la nuova proposta, la vecchia viene segnata `REPLACED` e scritta subito nel database (`flush`), perché Hibernate esegue gli inserimenti prima degli aggiornamenti;
  9. **un solo punto di salvataggio:** `ProposalService` salva una bozza del pianificatore (`ScheduleDraft`) con il suo testo. Lo usano il piano base e, in T33, lo strumento `create_plan_proposal` dell'agente (D56).
- **Perché:** una barra "Conferma / Scarta" senza attività confonde; una proposta vecchia conterrebbe giorni passati e risorse che non ci sono più. Cancellare le attività mai entrate nel calendario evita che ogni clic su "Genera piano base" lasci decine di righe per sempre, mentre la riga della proposta conserva lo storico.
- **Scartate:** proposta vuota con il solo avviso; confermare una proposta di un altro giorno togliendo i giorni passati (più regole da spiegare per un caso raro); tenere le attività scartate e sostituite con il loro stato (righe che crescono a ogni prova); mandare nel calendario solo le attività della proposta dentro l'intervallo (la barra ha bisogno del riassunto di tutta la proposta).

## D125 · Avviso di cambiamento con l'impronta dei dati
**Stato:** Confermata · 16/10/2026 · nata da T31 · precisa D23

- **Cosa:** il calendario avvisa quando roster, obiettivi o eventi sono cambiati dopo l'ultimo piano (D23).
- **Come:**
  1. quando si salva una proposta, il backend calcola un'**impronta** (hash SHA-256) dei dati da cui è nata: il roster con livelli, gradi e obiettivi, e gli eventi nei giorni del piano con date, giorni attivi, attività ed effetti. La migrazione `V2` aggiunge a `plan_proposal` l'impronta (`inputs_hash`) e i giorni del piano (`first_day`, `last_day`), che servono a ricalcolarla sugli stessi giorni;
  2. `GET /api/me/calendar` ricalcola l'impronta dell'ultimo piano (la proposta più recente fra quella in attesa e quelle confermate) e restituisce `planOutdated: true` se è diversa;
  3. il calendario mostra sopra le attività l'avviso "Roster, obiettivi o eventi sono cambiati dopo il tuo ultimo piano", con il pulsante "Genera piano base" (con la chat diventerà "chiedi all'agente");
  4. gli eventi si confrontano solo nei giorni del piano: se no, ogni giorno nuovo farebbe entrare nella finestra un'altra edizione e l'avviso comparirebbe sempre;
  5. non contano l'inventario e le attività fatte (cambiano a ogni "Segna come fatta" e l'avviso non sparirebbe mai), né il tempo di gioco, che D23 non nomina;
  6. i piani salvati prima della migrazione non hanno l'impronta e non mostrano l'avviso; se una modifica viene annullata, l'impronta torna uguale e l'avviso sparisce.
- **Perché:** confronta i dati veri e non le date di modifica, non tocca i service del roster e dell'admin, e si spiega con una frase: "il piano ricorda da quali dati è nato".
- **Scartate:** una data di "ultima modifica" sull'utente e sugli eventi (va aggiornata in ogni service che scrive, ed è facile dimenticarne uno); un avviso tenuto solo nel browser (si perde ricaricando la pagina).

## D126 · Agente gratuito con OpenRouter
**Stato:** Confermata · 17/10/2026 · scelta dell'utente, con Nemotron come modello predefinito · modifica D6, D54, D59 e D61

- **Cosa:** l'agente usa un modello **gratuito** tramite OpenRouter, al posto dell'API Claude a pagamento. OpenRouter è un servizio che dà accesso a molti modelli di fornitori diversi con un'unica API e un'unica chiave; alcuni modelli sono gratuiti. È **fuori dal programma del corso**, che ha studiato l'API Claude (settimana 19, D6).
- **Come:**
  1. **formato:** l'API di OpenRouter compatibile con OpenAI (Chat Completions, `POST https://openrouter.ai/api/v1/chat/completions`), chiamata con `RestClient` di Spring, che è già nel progetto: nessuna libreria nuova. Il client sta dietro l'interfaccia `ModelClient`, con tipi nostri per messaggi, strumenti e chiamate agli strumenti: il ciclo (T34) e il finto client dei test (D63) non dipendono dal fornitore;
  2. **modello:** `nvidia/nemotron-3-super-120b-a12b:free`, letto da `OPENROUTER_MODEL`; si cambia con una riga, come diceva D59. Nella chiamata di prova del 17/10 ha chiesto lo strumento giusto e ha risposto in italiano. `google/gemma-4-31b-it:free`, la prima scelta, riceveva sempre 429 dal suo fornitore gratuito (Google AI Studio), anche con la quota giornaliera intatta: resta l'alternativa;
  3. **solo gratuito:** il backend non parte se il nome del modello non finisce con `:free`. In più la chiave ha un limite di credito di 5 $, e l'account non ha credito né un metodo di pagamento;
  4. **variabili:** `OPENROUTER_API_KEY` e `OPENROUTER_MODEL` al posto di `ANTHROPIC_API_KEY` e `ANTHROPIC_MODEL`, obbligatorie in `AppProperties`;
  5. **limiti del piano gratuito:** 20 richieste al minuto e 50 al giorno per tutto l'account (1.000 al giorno se un giorno si comprano almeno 10 $ di credito). Un messaggio all'agente fa da 3 a 6 chiamate, quindi bastano per circa 10 messaggi al giorno in tutta l'app. Quando il limite finisce OpenRouter risponde 429, che diventa 502 `AGENT_UNAVAILABLE` con un messaggio che lo dice; il piano base resta disponibile (D64);
  6. **ogni risposta del modello** ha al massimo 2.048 token: una risposta della chat è breve, e così il testo resta sotto i limiti di lettura di Jackson (D110). Il client legge al massimo 1 MB di risposta, così una risposta enorme non può riempire i 512 MB del server gratuito;
  7. **cache dei prompt:** le parti fisse restano in testa (D58), ma con i modelli gratuiti la cache non è garantita: i token letti dalla cache si registrano solo se il fornitore li riporta;
  8. **dati:** nelle impostazioni di OpenRouter sono permessi i fornitori gratuiti che possono addestrarsi sui dati, non quelli che pubblicano i prompt. Al modello vanno solo dati di gioco e i messaggi della chat, mai email o password (D60); la chat (T36) dirà all'utente che i messaggi vanno a un modello gratuito di terzi;
  9. **tempo:** ogni chiamata gira in un thread virtuale e ha 60 secondi in tutto, risposta compresa (D61); allo scadere viene interrotta. Il timeout del client HTTP da solo non basta: smette di aspettare gli header, ma non un server che manda gli header e poi tace (lo mostra un test);
  10. **errori:** chiave rifiutata, modello non trovato, tempo scaduto, risposta non valida, troppo grande o errore del fornitore diventano 502 `AGENT_UNAVAILABLE`. Nei log restano lo stato HTTP e il codice d'errore di OpenRouter; il messaggio d'errore solo a livello DEBUG, spento di norma, perché alcuni fornitori vi ripetono parte del prompt (D60). Una chiave con spazi o a capo ferma l'avvio, perché farebbe fallire ogni chiamata.
- **Perché:** l'agente deve costare zero. Il ciclo, gli strumenti e il pianificatore restano nostri e non cambiano: cambia solo chi risponde. Siccome i calcoli li fa Java (D3), al modello resta il compito di capire la richiesta, scegliere i parametri e spiegare, che un modello gratuito di media grandezza sa fare; lo si verifica con gli scenari di `06-agente.md`.
- **Scartate:** API Claude con Haiku 4.5 (D6, D59: a pagamento, circa 9 $ per lo sviluppo); SDK di Anthropic puntato su OpenRouter (OpenRouter garantisce quel formato solo con i modelli Claude); SDK di OpenAI (una libreria in più, fuori programma); Spring AI (fuori programma, e nasconde il ciclo che D54 vuole scritto da noi); `openrouter/free`, che sceglie ogni volta un modello gratuito diverso (prove non ripetibili); modello locale con Ollama (non funziona online, D6).
- **Limiti noti:** l'elenco dei modelli gratuiti cambia nel tempo, quindi il modello scelto può sparire (si cambia `OPENROUTER_MODEL`); i limiti giornalieri sono condivisi da tutti gli utenti della demo, quindi un solo utente può esaurirli (il limite per utente di D48 si decide in T35); un fornitore gratuito può rispondere 429 anche quando la quota è intatta (successo con Gemma il 17/10); un modello gratuito può essere meno preciso di Claude nello scegliere gli strumenti.

## D127 · Regole degli strumenti dell'agente
**Stato:** Confermata · 17/10/2026 · nata da T33 · precisa D55, D56, D61 e D62

- **Cosa:** come i cinque strumenti (D55) leggono gli argomenti del modello, cosa restituiscono e come salvano la proposta.
- **Come:**
  1. **struttura:** pacchetto `agent/tools/`. `AgentTools` ha le cinque definizioni e smista per nome; `AgentTurn` è lo stato di un turno (utente del token, ultima bozza con i suoi parametri, proposta salvata, passaggi); `ToolOutcome` è il risultato per il modello (testo JSON, errore sì o no) con il passaggio da mostrare;
  2. **niente `userId`** (D44, D55): l'utente è sempre quello del turno. Un `userId` negli argomenti è una chiave sconosciuta, quindi un errore;
  3. **argomenti come il corpo di una richiesta:** un oggetto JSON di al massimo 8 KB, letto in modo rigoroso: una chiave sconosciuta o scritta due volte è un errore (come in `DataFileReader`), ogni valore ha il suo tipo JSON (`"7"` non è un numero), date `AAAA-MM-GG`. Argomenti vuoti o `null` valgono `{}`, perché alcuni modelli li mandano così per gli strumenti senza parametri. Ogni errore torna al modello come risultato d'errore con il messaggio e i campi, così può correggersi (D61): mai un 500;
  4. **risultati:**
     - `get_player_status`: i personaggi del roster con nome, livello e grado attuali e obiettivo; Waveplate e Crystals con l'ora italiana in cui sono pieni; il tempo di gioco. Niente email né altri dati del profilo. Se le Waveplate o il tempo di gioco non sono ancora salvati, il valore è `null` e una nota dice al modello cosa conta il pianificatore (un giorno intero di Waveplate, 60 minuti);
     - `get_events` e `get_calendar`: senza date, da oggi per 14 giorni; al massimo 62 giorni e dentro le date dell'app, come le API (D113). Un risultato oltre 32.000 caratteri torna come errore che chiede un intervallo più breve: descrizioni e note sono testi scritti da persone, e due mesi di testi sono tanti token e tanto spazio per istruzioni nascoste (D60). `get_events` restituisce le edizioni come `/api/events`; `get_calendar` le attività confermate e fatte, la proposta in attesa (solo id e testo) e `planOutdated` (D125);
     - `plan_schedule`: i parametri di `ScheduleRequest` (D123) e la bozza del pianificatore (giorni, copertura, avvisi). Il turno ricorda la bozza e i parametri;
     - `create_plan_proposal`: `summary` obbligatorio, da 1 a 2.000 caratteri;
  5. **la proposta è la bozza vista dal modello (D56):** `create_plan_proposal`, in una sola transazione, blocca l'utente, ricalcola la bozza con i parametri dell'ultimo `plan_schedule` del turno e la confronta con quella che il modello ha visto. Se è lo stesso piano la salva con `ProposalService`, se no risponde al modello che i dati sono cambiati e di richiamare `plan_schedule`. Si confronta il piano (attività di ogni giorno, copertura, avvisi), non le risorse libere: una Waveplate che si ricarica durante il turno non lo cambia. Si salva la bozza appena ricalcolata. Senza un `plan_schedule` nel turno è un errore, e un `plan_schedule` che fallisce cancella il piano di prima, perché la spiegazione del modello descriverebbe quello fallito; il 409 di un piano vuoto (D124) torna al modello con l'avviso del pianificatore;
  6. **passaggi (D62):** ogni strumento riuscito dà una frase fissa: "Ho letto il tuo roster e le Waveplate", "Ho controllato gli eventi", "Ho guardato il tuo calendario", "Ho calcolato il piano", "Ho preparato la proposta". Il ciclo (T34) le raccoglie senza doppioni;
  7. **cosa non va al modello:** se l'utente del token non esiste più il turno si ferma con 401 (D102); un errore inatteso resta un 500. Uno strumento sconosciuto torna al modello come errore, con i nomi dei cinque;
  8. **schemi:** descrizioni in italiano, scritti come testo JSON con `additionalProperties: false` e letti all'avvio in mappe ordinate, così la richiesta al modello è sempre uguale (D58).
- **Perché:** tra `plan_schedule` e `create_plan_proposal` c'è una chiamata al modello di qualche secondo: salvare la bozza così com'era rischierebbe un piano calcolato su dati superati, con l'impronta di D125 sbagliata. Ricalcolare e confrontare dà la stessa garanzia di `createBasic` senza tenere aperta una transazione mentre si aspetta il modello.
- **Scartate:** salvare la bozza del turno senza ricalcolarla (dati forse superati); tenere bloccato l'utente tra due chiamate al modello (una transazione aperta fino a 60 secondi); un parametro `userId` negli strumenti (il modello potrebbe chiedere i dati di altri, D55); restituire i DTO delle API così come sono (`UserResponse` contiene l'email).
