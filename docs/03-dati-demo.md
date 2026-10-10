# 3. Dati di gioco e demo

**Stato:** completata · 01/10/2026 · la raccolta dei dati resta da fare (vedi il piano in fondo)

Questa fase definisce quali dati servono, in che formato li salviamo e da dove li prendiamo. La raccolta vera e propria dei numeri si fa dopo, seguendo il piano in fondo al documento.

## Cosa abbiamo verificato sul gioco

Fonte: Wuthering Waves Wiki (Fandom), consultato il 01/10/2026.

| Meccanica | Dato verificato |
|---|---|
| Ricarica Waveplate | 1 ogni 6 minuti, fino a un massimo di 240 |
| Oltre il massimo | L'eccesso si accumula come *Waveplate Crystals*: 1 ogni 12 minuti, fino a 480, convertibili 1:1 in Waveplate |
| Costo dei contenuti | Simulation Training 40 · Forgery Challenge 40 · Boss Challenge 60 · Weekly Challenge 60 · Tacet Field 60 |
| Simulation Training | Dà materiali EXP dei personaggi (Resonance Potion), EXP delle armi o Shell Credit, a seconda del tipo scelto |
| Ascensione | Avviene ai livelli 20, 40, 50, 60, 70 e 80 |
| Ascensione al 20 (esempio Jinhsi) | 5.000 Shell Credit e 4 LF Howler Core (drop di nemici comuni) |
| Ascensione al 40 (esempio Jinhsi) | 10.000 Shell Credit, 3 materiali da boss, 4 specialità locali, 4 MF Howler Core |

**Raccolta completata il 02/10/2026.** I dati sono in `backend/src/main/resources/data/`; fonti, ipotesi e stime sono nel `README.md` della stessa cartella. Dalla raccolta sono nate le decisioni D92-D97: fase SOL3 fissa, costo in crediti come regola, effetti degli eventi con limite giornaliero, combattimento a livelli, eventi attivi solo in certi giorni, stime per nemici e specialità.

### Cosa comporta per la demo

Fino al livello 20 l'unico contenuto che consuma Waveplate è la Simulation Training. I drop dei nemici comuni si ottengono esplorando il mondo: non costano Waveplate ma costano tempo, ed è un bel caso per il tempo di gioco (D16). Il Boss Challenge e le specialità locali entrano con l'ascensione al 40: per questo la demo arriva al livello 40 (D27).

## Quali dati servono

| File | Contenuto | Esempio |
|---|---|---|
| `game-rules.json` | Regole generali: Waveplate, Waveplate Crystals, opzioni di tempo di gioco | massimo 240, 1 ogni 6 minuti |
| `materials.json` | Tutti i materiali usati dalla demo | Shell Credit, Basic Resonance Potion, LF Howler Core |
| `farming-sources.json` | Dove si ottiene ogni materiale: costo in Waveplate, durata stimata, drop medio | Simulation Training: Resonance Potion |
| `level-exp.json` | EXP necessaria per salire da ogni livello al successivo, uguale per tutti i personaggi | livello 1 → 2 |
| `characters.json` | I sei personaggi con elemento, arma, rarità, ruolo e costi di ascensione | Jinhsi |
| `events.json` | Gli eventi della demo con attività ed effetti | evento con ricompense doppie |

I file stanno nel backend e vengono caricati nel database all'avvio (D4). Gli identificativi sono testi stabili e leggibili, come `jinhsi` o `lf-howler-core`, e collegano i file tra loro (D26).

## Formato dei file

I valori segnati come *esempio* servono solo a mostrare la struttura.

### game-rules.json
```json
{
  "waveplate": { "max": 240, "regenMinutes": 6 },
  "waveplateCrystals": { "max": 480, "regenMinutes": 12 },
  "ascensionLevels": [20, 40],
  "maxLevel": 40,
  "shellCreditPerExp": 0.35,
  "sol3Phase": 4,
  "demoEventsAnchorDate": "2026-10-01",
  "playTimeOptionsMinutes": [30, 60, 120]
}
```
`ascensionLevels` elenca i livelli a cui si fanno le ascensioni previste dalla demo (D27). `shellCreditPerExp` è il costo in crediti di ogni punto EXP (D93). `sol3Phase` è la fase a cui si riferiscono le ricompense (D92). `demoEventsAnchorDate` è la data di riferimento da cui partono i cicli degli eventi (D29).

### materials.json
```json
[
  { "id": "shell-credit", "name": "Shell Credit", "category": "CREDIT" },
  { "id": "basic-resonance-potion", "name": "Basic Resonance Potion", "category": "CHARACTER_EXP", "exp": 1000 },
  { "id": "lf-howler-core", "name": "LF Howler Core", "category": "ENEMY_DROP" }
]
```
`exp` c'è solo per i materiali EXP. Il valore 1000 è un esempio.

### farming-sources.json
```json
[
  {
    "id": "sim-resonance-potion",
    "name": "Simulation Training: Resonance Potion",
    "type": "SIMULATION_TRAINING",
    "waveplateCost": 40,
    "minutesPerRun": 3,
    "drops": [
      { "materialId": "basic-resonance-potion", "avgQuantity": 5, "estimated": true }
    ]
  },
  {
    "id": "overworld-howler",
    "name": "Nemici comuni: Howler",
    "type": "OVERWORLD",
    "waveplateCost": 0,
    "minutesPerRun": 10,
    "drops": [
      { "materialId": "lf-howler-core", "avgQuantity": 3, "estimated": true }
    ]
  }
]
```
`minutesPerRun` è sempre una nostra stima (D31). `estimated: true` segnala un numero non verificato su una fonte.

### level-exp.json
```json
[
  { "level": 1, "expToNext": 500 },
  { "level": 2, "expToNext": 650 }
]
```
Valori di esempio. La tabella arriva fino al livello massimo della demo (D27).

### characters.json
```json
[
  {
    "id": "jinhsi",
    "name": "Jinhsi",
    "element": "SPECTRO",
    "weaponType": "BROADBLADE",
    "rarity": 5,
    "role": "Main Damage Dealer",
    "icon": "characters/jinhsi.webp",
    "ascensions": [
      {
        "toRank": 1,
        "atLevel": 20,
        "cost": [
          { "materialId": "shell-credit", "quantity": 5000 },
          { "materialId": "lf-howler-core", "quantity": 4 }
        ]
      }
    ]
  }
]
```
L'ascensione al 20 di Jinhsi è un dato reale.

### events.json
```json
[
  {
    "id": "bountiful-crescendo",
    "name": "Bountiful Crescendo",
    "type": "DOUBLE_DROP",
    "description": "Simulation Training e Forgery Challenge danno ricompense doppie, fino a 3 volte al giorno.",
    "schedule": { "startOffsetDays": 3, "durationDays": 7, "repeatEveryDays": 14, "activeWeekdays": null },
    "rewards": ["Ricompense doppie, massimo 3 al giorno"],
    "activities": [],
    "effect": { "type": "DROP_MULTIPLIER", "sourceType": "SIMULATION_TRAINING", "multiplier": 2, "dailyLimit": 3 }
  },
  {
    "id": "lament-recon-solaris-soldier",
    "name": "Lament Recon: Solaris Soldier",
    "type": "COMBAT_STAGES",
    "description": "Evento di combattimento con 7 drill a ondate di nemici.",
    "schedule": { "startOffsetDays": -2, "durationDays": 39, "repeatEveryDays": 42, "activeWeekdays": null },
    "rewards": ["Medaglia e avatar esclusivi"],
    "activities": [
      { "title": "Drill 1", "minutes": 15, "frequency": "ONCE", "unlockDay": null }
    ],
    "effect": null
  }
]
```
Estratto del file reale. Le date si calcolano con `schedule` (D29). I tre tipi di evento sono `COMBAT_STAGES`, `DOUBLE_DROP` e `POINTS_SHOP` (D18, D95).

**Eventi giornalieri (D96):** `activeWeekdays` limita l'evento ad alcuni giorni della settimana, per esempio `["MONDAY", "WEDNESDAY", "FRIDAY"]`; `null` vuol dire tutti i giorni. `unlockDay` indica da quale giorno dell'evento un'attività diventa disponibile; `null` vuol dire dal primo giorno.

**Effetti (D94):** `sourceType` indica a quale tipo di fonte si applica il moltiplicatore; `dailyLimit` quante run al giorno ne beneficiano (`null` = tutte).

## Fonti e regole di verifica (D25)

- **Fonte principale:** Wuthering Waves Wiki su Fandom. I contenuti sono sotto licenza CC BY-SA, quindi il README e il footer dell'app citano la fonte.
- **Seconda fonte:** un database della community, per controllare i numeri e coprire quelli che mancano sul wiki. Lo scegliamo durante la raccolta.
- **Regola:** ogni numero viene da una fonte. Se manca, lo stimiamo noi e lo segniamo con `estimated: true`; l'app mostra quei valori come "stima".

## Piano di raccolta

**Completato il 02/10/2026**, icone dei personaggi comprese (in `frontend/public/characters/`, formato WebP). Passi seguiti:

1. Per ognuno dei sei personaggi: elemento, arma, rarità, ruolo, costi di ascensione fino al livello massimo della demo.
2. Tabella dell'EXP per livello e valori EXP delle Resonance Potion.
3. Ricompense medie di Simulation Training e degli altri contenuti necessari.
4. Un evento reale per ognuno dei tre tipi di D18, con descrizione, durata e ricompense.
5. Controllo incrociato sulla seconda fonte e compilazione dei JSON.
6. Revisione insieme prima di considerare i dati definitivi.

La raccolta usa il browser per leggere le pagine: i dati vanno copiati e controllati, non presi alla cieca.
