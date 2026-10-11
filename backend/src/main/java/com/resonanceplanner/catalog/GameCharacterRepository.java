package com.resonanceplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Characters of the catalog, by readable id. */
public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {}
