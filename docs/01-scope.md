# 1. Scope

**Stato:** completata · 01/10/2026

## Visione

Resonance Planner è una **demo funzionante** di un planner per Wuthering Waves. Chi la prova sceglie tra opzioni preparate (personaggi, livelli, obiettivi) e vede come l'app organizza il suo tempo di gioco nel calendario, con l'aiuto di un agente IA.

Il dataset è piccolo di proposito, il codice no: aggiungere un personaggio o un evento significa aggiungere dati, senza scrivere nuovo codice (vedi D9).

## Per chi è

- **Staff Epicode e Demo Day**: devono vedere un'app completa che funziona dall'inizio alla fine.
- **Recruiter**: aprono il link dal portfolio e provano l'app da soli.
- **In prospettiva, i giocatori di Wuthering Waves**: la versione completa, con tutti i personaggi, servirebbe a loro.

## Esperienza della demo

1. Chi prova l'app si registra o fa login.
2. Al primo accesso una configurazione guidata gli fa scegliere i personaggi tra quelli disponibili, il loro livello attuale e il livello obiettivo (massimo 40), le Waveplate che ha in quel momento e quanto tempo gioca al giorno.
3. Il calendario mostra gli eventi in corso.
4. Chiede all'agente quello che gli serve, scrivendo o usando i suggerimenti pronti (per esempio "Organizzami la settimana" o "Spiegami l'evento").
5. L'agente propone un piano; l'utente lo controlla e lo conferma.
6. Se cambia obiettivo o arriva un nuovo evento, chiede all'agente di ripianificare.

I flussi nel dettaglio sono il compito della fase 2.

## Cosa vogliamo

### v1 · Core
- Registrazione e login con JWT; ogni utente ha i suoi dati (D10)
- Catalogo demo: 6 personaggi reali e i materiali che servono fino al livello 40, prime due ascensioni comprese (D9, D17, D27)
- Configurazione guidata al primo accesso, con il tempo di gioco giornaliero (D11, D16)
- Calendario con gli eventi demo, con date sempre attuali, e le attività pianificate (D12)
- Area admin per creare, modificare e cancellare gli eventi (D14)
- Eliminazione dell'account con tutti i dati, come richiede il GDPR (D42)
- Contatore Waveplate con l'orario in cui saranno piene
- Calcolo del farming: materiali mancanti, Waveplate, giorni (D3)
- Primo deploy online (D8)

### v2 · Agente IA
- Chat con l'agente, con suggerimenti pronti
- L'agente legge i dati dell'utente e usa il calcolo del farming tramite strumenti del backend (D3)
- Propone un piano che l'utente conferma prima del salvataggio (D2)
- Ripianificazione quando cambiano obiettivi o eventi
- Limite di richieste giornaliere per utente
- Piano base senza IA, che funziona anche quando l'API non risponde (D64)

### v3 · Extra facoltativi
- Damage calculator semplificato
- Notifiche in tempo reale
- Export del piano in Excel
- Accesso ospite "Prova la demo" (D15)

## Cosa non vogliamo

| Fuori scope | Perché |
|---|---|
| Tutti i personaggi del gioco | Ne bastano 5-6 per dimostrare che funziona; gli altri sarebbero solo inserimento dati |
| Livelli oltre il 40 | Il calcolo è lo stesso, cambierebbero solo i dati da preparare (D27) |
| Collegamento all'account di gioco | Non esiste un'API ufficiale: i dati li inserisce l'utente |
| Dati aggiornati in automatico dal gioco | Personaggi ed eventi sono un dataset demo preparato a mano |
| Altri giochi gacha | Il progetto resta su Wuthering Waves |
| App mobile nativa | Basta un sito responsive |
| Funzioni social, chat tra utenti, pagamenti | Non servono allo scopo dell'app |
| Agente che modifica il calendario da solo | Ogni piano va confermato dall'utente (D2) |
| Agente che fa i calcoli | I calcoli sono codice Java (D3) |

## Vincoli

- Due settimane esatte, progetto individuale.
- Idea da far approvare allo staff; presentazione registrata al Demo Day.
- Backend Spring Boot con PostgreSQL obbligatorio (D5).
- Il README deve avere il link alla versione online e le istruzioni per avviare il progetto; Swagger è un extra apprezzato.
- Costi dell'API IA il più bassi possibile (D6).

## Criteri di successo

- La demo funziona dall'inizio alla fine sul sito online, senza intoppi.
- Due utenti con scelte diverse ricevono piani diversi: si vede che il risultato non è preimpostato.
- Il README permette a chiunque di clonare e avviare il progetto.

## Decisioni prese in questa fase

D9-D17, tutte confermate il 01/10/2026. La scelta dei singoli eventi reali avviene nella fase 3, durante la raccolta dei dati (D18).
