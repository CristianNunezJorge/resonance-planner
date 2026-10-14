package com.resonanceplanner.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ascension costs; the startup loader replaces them all (D39). */
public interface AscensionCostRepository extends JpaRepository<AscensionCost, Long> {

    // One query for the costs with their character and material, instead of one query per cost
    @EntityGraph(attributePaths = {"character", "material"})
    List<AscensionCost> findAllByOrderByIdAsc();
}
