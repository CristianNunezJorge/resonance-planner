package com.resonanceplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Ascension costs; the startup loader replaces them all (D39). */
public interface AscensionCostRepository extends JpaRepository<AscensionCost, Long> {}
