package com.resonanceplanner.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Materials of the catalog, by readable id. */
public interface MaterialRepository extends JpaRepository<Material, String> {}
