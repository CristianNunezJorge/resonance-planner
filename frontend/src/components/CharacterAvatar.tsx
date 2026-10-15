import { useState } from 'react'
import { initialsOf } from '../features/catalog/format'
import { characterIconUrl } from '../features/catalog/icons'
import type { CharacterSummary } from '../features/catalog/types'

type AvatarCharacter = Pick<CharacterSummary, 'name' | 'element' | 'icon'>

interface CharacterAvatarProps {
  character: AvatarCharacter
}

// The official icon, or the initials in the colour of the element when the icon is missing or does not load (D30).
// Hidden from screen readers: the name of the character is always written next to it.
function CharacterAvatar({ character }: CharacterAvatarProps) {
  const url = characterIconUrl(character.icon)
  const [failedUrl, setFailedUrl] = useState<string | null>(null)

  if (url !== null && failedUrl !== url) {
    return (
      <img
        src={url}
        alt=""
        className="character-avatar"
        width={64}
        height={64}
        loading="lazy"
        onError={() => setFailedUrl(url)}
      />
    )
  }
  return (
    <span className={`character-avatar element-${character.element.toLowerCase()}`} aria-hidden="true">
      {initialsOf(character.name)}
    </span>
  )
}

export default CharacterAvatar
