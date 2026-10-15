package com.resonanceplanner.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Game events; demo events are found by the code of the JSON file (D39). */
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    boolean existsByCode(String code);

    Optional<GameEvent> findByCode(String code);

    // The collections of the events load in batches (default_batch_fetch_size), not one query per event
    List<GameEvent> findAllByOrderByIdAsc();

    /**
     * Deletes with a single statement and returns how many rows it deleted (0 if the event did not exist). The
     * database deletes days, rewards and activities with it and unlinks the planned tasks (ON DELETE).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GameEvent e where e.id = :id")
    int deleteOne(@Param("id") Long id);
}
