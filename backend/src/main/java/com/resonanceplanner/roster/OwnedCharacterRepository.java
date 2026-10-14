package com.resonanceplanner.roster;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The roster of a user. Every query written here takes the user: nobody reads or changes the characters of another
 * one (D44). The methods that JpaRepository brings (findById, deleteById) take no user: do not use them for the roster.
 */
public interface OwnedCharacterRepository extends JpaRepository<OwnedCharacter, Long> {

    // In the order in which the characters were added
    List<OwnedCharacter> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<OwnedCharacter> findByUserIdAndCharacterId(Long userId, String characterId);

    boolean existsByUserIdAndCharacterId(Long userId, String characterId);

    /** Deletes with a single statement and returns how many rows it deleted (0 if the character was not in the roster). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OwnedCharacter o where o.userId = :userId and o.characterId = :characterId")
    int deleteOne(@Param("userId") Long userId, @Param("characterId") String characterId);
}
