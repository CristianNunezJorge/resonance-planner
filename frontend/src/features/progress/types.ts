// A level and, only at a level where an ascension is needed, whether that ascension is done (D104): together they
// say the rank, which the user never sees. Shared by the guided setup and the roster
export interface LevelDraft {
  level: number
  ascended: boolean
}
