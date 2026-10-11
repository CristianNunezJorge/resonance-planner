import { formatGameTerm } from '../catalog/format'
import type { CharacterSummary } from '../catalog/types'

interface CharacterCardProps {
  character: CharacterSummary
}

// Test card for T11; the presentation page P1 (T16) and CharacterAvatar (T24) will replace it
function CharacterCard({ character }: CharacterCardProps) {
  const stars = '★'.repeat(character.rarity)

  return (
    <article className="card h-100">
      {/* Empty alt: the name follows right after, so it is not read twice. h-auto keeps the
          proportions, while width and height reserve the space before the image arrives */}
      {character.icon && (
        <img
          src={`/${character.icon}`}
          alt=""
          className="card-img-top h-auto"
          width={256}
          height={256}
          loading="lazy"
        />
      )}
      <div className="card-body">
        <h3 className="h5 card-title mb-1">{character.name}</h3>
        <p className="mb-2">
          <span className={`badge element-${character.element.toLowerCase()}`}>
            {formatGameTerm(character.element)}
          </span>{' '}
          <span className="small text-body-secondary">{formatGameTerm(character.weaponType)}</span>
        </p>
        <p className="small mb-0">
          <span className="text-primary" role="img" aria-label={`${character.rarity} stelle`}>
            {stars}
          </span>{' '}
          <span className="text-body-secondary">{character.role}</span>
        </p>
      </div>
    </article>
  )
}

export default CharacterCard
