package com.resonanceplanner.event;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Game events; demo events are found by the code of the JSON file (D39). */
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    boolean existsByCode(String code);

    Optional<GameEvent> findByCode(String code);
}
