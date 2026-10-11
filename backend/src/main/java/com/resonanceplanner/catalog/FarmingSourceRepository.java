package com.resonanceplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Farming sources of the catalog, by readable id. */
public interface FarmingSourceRepository extends JpaRepository<FarmingSource, String> {}
