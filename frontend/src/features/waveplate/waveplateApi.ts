import { apiRequest } from '../../api/client'
import type { WaveplateReading, WaveplateStatus } from './types'

// The Waveplate of the user of the token (D44, D105)

/** 404 if no reading was ever saved. */
export function getWaveplate(token: string): Promise<WaveplateStatus> {
  return apiRequest<WaveplateStatus>('/api/me/waveplate', { token })
}

export function saveWaveplate(token: string, reading: WaveplateReading): Promise<WaveplateStatus> {
  return apiRequest<WaveplateStatus>('/api/me/waveplate', { method: 'PUT', body: reading, token })
}
