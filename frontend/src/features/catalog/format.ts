// "BOSS_CHALLENGE" → "Boss Challenge": game names stay in English, as in the sources (D71)
export function formatGameTerm(code: string): string {
  return code
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

/** "Jinhsi" → "JI", "Rover Havoc" → "RH": the two letters of the avatar without an icon (D30). */
export function initialsOf(name: string): string {
  const words = name.trim().split(/\s+/)
  const initials = words.length > 1 ? words[0].charAt(0) + words[1].charAt(0) : words[0].slice(0, 2)
  return initials.toUpperCase()
}
