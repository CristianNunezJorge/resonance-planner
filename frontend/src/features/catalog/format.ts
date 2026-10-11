// "BOSS_CHALLENGE" → "Boss Challenge": game names stay in English, as in the sources (D71)
export function formatGameTerm(code: string): string {
  return code
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}
