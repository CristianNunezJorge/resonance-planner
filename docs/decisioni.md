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
**Stato:** Confermata · modello Claude Haiku 4.5 scelto il 01/10/2026 (D59)

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
**Stato:** Confermata · 01/10/2026

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
**Stato:** Confermata · 01/10/2026 · Claude Haiku 4.5

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
**Stato:** Confermata · 01/10/2026

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
**Stato:** Confermata · 10/10/2026 · scelta dell'utente, sostituisce la versione del 01/10 con il database su Neon · piani da ricontrollare al primo deploy

- **Cosa:** tutto su Render: frontend come sito statico, backend come immagine Docker, database come PostgreSQL di Render.
- **Come:** pubblicazione automatica da `main`. Il frontend mostra un messaggio se il backend si sta riaccendendo; prima del Demo Day si apre il sito con qualche minuto di anticipo. Il backend riceve l'indirizzo del database dalle variabili d'ambiente di Render.
- **Perché:** Render è la piattaforma usata nella settimana 20. Con una sola piattaforma c'è un solo pannello da gestire e niente strumenti fuori dal programma.
- **Scartate:** database su Neon (la versione precedente di questa decisione: non scade, ma è una piattaforma in più fuori dal programma); Vercel per il frontend (una piattaforma in più senza vantaggi reali).
- **Limiti noti:**
  - il backend gratuito si spegne dopo 15 minuti di inattività e si riaccende in circa un minuto;
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
