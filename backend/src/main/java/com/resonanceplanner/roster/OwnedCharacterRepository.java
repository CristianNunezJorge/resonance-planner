package com.resonanceplanner.roster;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * The roster of a user. It extends the bare Repository on purpose: JpaRepository would bring findAll, findById and
 * deleteById, which take no user, and a later card could call them by mistake (D44). Every query here takes the user.
 */
public interface OwnedCharacterRepository extends Repository<OwnedCharacter, Long> {

    OwnedCharacter saveAndFlush(OwnedCharacter ownedCharacter);


    // In the order in which the characters were added
    List<OwnedCharacter> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<OwnedCharacter> findByUserIdAndCharacterId(Long userId, String characterId);

    boolean existsByUserIdAndCharacterId(Long userId, String characterId);

    /** Deletes the whole roster of a user with a single statement, for the guided setup (D109). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OwnedCharacter o where o.userId = :userId")
    int deleteAllOf(@Param("userId") Long userId);

    /** Deletes with a single statement and returns how many rows it deleted (0 if the character was not in the roster). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from OwnedCharacter o where o.userId = :userId and o.characterId = :characterId")
    int deleteOne(@Param("userId") Long userId, @Param("characterId") String characterId);
}
