package com.resonanceplanner.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Farming sources of the catalog, by readable id. */
public interface FarmingSourceRepository extends JpaRepository<FarmingSource, String> {

    // One query for sources, drops and materials, instead of one query per source
    @EntityGraph(attributePaths = {"drops", "drops.material"})
    List<FarmingSource> findAllByOrderByTypeAscNameAsc();
}
