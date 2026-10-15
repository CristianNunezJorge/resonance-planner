// Shapes of GET and PUT /api/me/waveplate (D28, D105): the values are computed by the server at the moment of the
// request, from the last reading and the time passed

export interface WaveplateStatus {
  value: number
  crystals: number
  max: number
  crystalsMax: number
  // When each counter is full; in the past if it already is
  fullAt: string
  crystalsFullAt: string
  recordedAt: string
}

/** What the user reads in the game. */
export interface WaveplateReading {
  value: number
  crystals: number
}
