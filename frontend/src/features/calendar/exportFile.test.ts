import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFileName, saveFile } from './exportFile'

describe('planFileName', () => {
  it('has only the dates of the range, like the name suggested by the server (D133)', () => {
    expect(planFileName({ from: '2026-10-19', to: '2026-10-25' })).toBe('resonance-planner_2026-10-19_2026-10-25.xlsx')
  })
})

describe('saveFile', () => {
  const createObjectURL = vi.fn(() => 'blob:https://resonance-planner.onrender.com/1234')
  const revokeObjectURL = vi.fn()

  beforeEach(() => {
    vi.useFakeTimers()
    // jsdom has no object URLs
    Object.assign(URL, { createObjectURL, revokeObjectURL })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    createObjectURL.mockClear()
    revokeObjectURL.mockClear()
  })

  it('downloads the file with its name through a temporary link, then frees its memory', () => {
    const clicked: HTMLAnchorElement[] = []
    // Some browsers (Firefox) download only from a link that is in the page at the moment of the click
    const inPageAtClick: boolean[] = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked.push(this)
      inPageAtClick.push(this.isConnected)
    })
    const file = new Blob(['xlsx'])

    saveFile(file, 'resonance-planner_2026-10-19_2026-10-25.xlsx')

    expect(createObjectURL).toHaveBeenCalledWith(file)
    expect(clicked).toHaveLength(1)
    expect(clicked[0].href).toBe('blob:https://resonance-planner.onrender.com/1234')
    expect(clicked[0].download).toBe('resonance-planner_2026-10-19_2026-10-25.xlsx')
    // The link was there only for the click
    expect(inPageAtClick).toEqual([true])
    expect(clicked[0].isConnected).toBe(false)
    expect(revokeObjectURL).not.toHaveBeenCalled()

    vi.runAllTimers()

    expect(revokeObjectURL).toHaveBeenCalledWith('blob:https://resonance-planner.onrender.com/1234')
  })
})
