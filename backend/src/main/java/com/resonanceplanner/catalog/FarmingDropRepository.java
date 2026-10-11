package com.resonanceplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Drops of the farming sources; the startup loader replaces them all (D39). */
public interface FarmingDropRepository extends JpaRepository<FarmingDrop, Long> {}
