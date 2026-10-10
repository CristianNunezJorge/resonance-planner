# Dati di gioco della demo

Dati raccolti il 02/10/2026. Formato descritto in `docs/03-dati-demo.md`.

## Fonti

Fonte principale: [Wuthering Waves Wiki](https://wutheringwaves.fandom.com) su Fandom, contenuti sotto licenza CC BY-SA. Nomi e immagini di Wuthering Waves appartengono a Kuro Games.

| File | Dato | Pagina del wiki | Controllo incrociato |
|---|---|---|---|
| `game-rules.json` | Ricarica e massimi di Waveplate e Waveplate Crystals | Waveplate | — |
| `game-rules.json` | 0,35 Shell Credit per ogni punto EXP | Resonator/Leveling | — |
| `level-exp.json` | EXP necessaria per ogni livello, 1-39 | Resonator/Leveling | Totali 1→20 (33.300) e 1→40 (208.800) ricalcolati dai valori per livello |
| `materials.json` | EXP delle Resonance Potion (1.000, 3.000, 8.000, 20.000) | Pagine delle singole pozioni | Totale EXP medio della Simulation Training ricalcolato |
| `characters.json` | Elemento, arma, rarità, ruolo, costi delle ascensioni 0→1 e 1→2 | Pagine `<Personaggio>` e `<Personaggio>/Combat` | Materiali e totali di Aalto e Danjin confermati da [SI](https://videogames.si.com/guides/wuthering-waves-aalto) e [Sportskeeda](https://www.sportskeeda.com/esports/wuthering-waves-danjin-ascension-materials) |
| `farming-sources.json` | Ricompense medie della Simulation Training: Resonance Potion | Simulation Training: Resonance Potion | Medie misurate dalla community su 81 run (fase 4) |
| `farming-sources.json` | Crediti della Simulation Training: Shell Credit | Simulation Training: Shell Credit/Jinzhou | — |
| `farming-sources.json` | Ricompense medie dei boss, boss di ogni materiale | Bosses (Boss Challenge), pagine dei materiali | Medie misurate dalla community su 40 run (fase 4) |
| `events.json` | Bountiful Crescendo | Bountiful Crescendo | — |
| `events.json` | Lament Recon: Solaris Soldier | Lament Recon: Solaris Soldier | — |
| `events.json` | By Moon's Grace | By Moon's Grace | — |
| `frontend/public/characters/*.webp` | Icone dei sei personaggi, 256×256 WebP | Immagini `Resonator_<Nome>.png` del wiki | Immagini di proprietà di Kuro Games, usate a scopo didattico non commerciale (D30) |

## Ipotesi

- **Fase SOL3 4** (Union Level 30) per tutte le ricompense. Le ricompense del gioco cambiano con la fase; la demo ne usa una sola.
- **Date degli eventi:** le durate sono quelle reali (7, 39 e 44 giorni); data di inizio e ripetizione sono scelte per la demo, così c'è sempre un evento in corso o in arrivo.
- **Descrizioni degli eventi** scritte da noi, non copiate.

## Stime (`estimated: true` e durate)

| Dato | Valore | Perché è una stima |
|---|---|---|
| Drop dei nemici comuni in 10 minuti | 5 materiali LF e 2 MF | Il wiki non riporta le probabilità di drop |
| Specialità locali raccolte in 10 minuti | 6 | Dipende dal percorso; il wiki indica solo le zone |
| Minuti per run | Simulation Training 3, Boss Challenge 4, esplorazione 10 | Il gioco non fornisce i tempi |
| Minuti delle attività degli eventi | 15 per drill o per obiettivi del giorno, 5 per gli acquisti | Come sopra |

## Limiti noti

- Il limite di 400 uccisioni al giorno per categoria di nemici comuni e la ricomparsa delle specialità locali non sono modellati: con le quantità della demo non vengono mai raggiunti.
- La Forgery Challenge non è modellata: nessun materiale della demo ne ha bisogno.
- La Basic Resonance Potion non cade alla fase 4, ma resta nel catalogo perché l'utente può averla nell'inventario.
