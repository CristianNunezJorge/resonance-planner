package com.resonanceplanner.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Characters of the catalog, by readable id. */
public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {

    List<GameCharacter> findAllByOrderByNameAsc();

    // One query for the character, its costs and their materials, instead of one query per cost
    @EntityGraph(attributePaths = {"ascensionCosts", "ascensionCosts.material"})
    Optional<GameCharacter> findWithAscensionCostsById(String id);
}
