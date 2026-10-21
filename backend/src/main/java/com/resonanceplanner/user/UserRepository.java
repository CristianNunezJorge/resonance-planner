package com.resonanceplanner.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

    // The email must be normalized with User.normalizeEmail before searching
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByRole(Role role);

    /** The role of the user, or nothing if the user no longer exists (D102). */
    @Query("select u.role from User u where u.id = :id")
    Optional<Role> findRoleById(@Param("id") Long id);

    /**
     * Deletes with a single statement the guests created before a moment, and with them everything they have (D42,
     * D134). It has its own transaction, because the scheduled cleanup has none.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from User u where u.role = com.resonanceplanner.user.Role.GUEST and u.createdAt < :before")
    int deleteGuestsCreatedBefore(@Param("before") Instant before);

    /**
     * Deletes the user with a single statement: the database deletes everything that depends on it (D42, ON DELETE
     * CASCADE). Returns 0 if the user did not exist. The persistence context is cleared, because the loaded
     * entities of the deleted rows would be out of date.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    int deleteAccount(@Param("id") Long id);

    /**
     * Marks the guided setup as completed, but only if it was not: one statement, so of two submissions at the same
     * moment only one gets 1 (the other waits for the row, then finds it already completed and gets 0). The
     * persistence context is cleared, because a User loaded before would still say it is not completed. It relies on
     * the default isolation of PostgreSQL, READ COMMITTED: with REPEATABLE READ the one that waits would get a
     * serialization error instead of 0.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.onboardingCompleted = true where u.id = :id and u.onboardingCompleted = false")
    int claimOnboarding(@Param("id") Long id);

    /**
     * Locks the row of the user until the end of the transaction and returns its id, or nothing if the user does not
     * exist (D118): the operations of one user that must not overlap, such as completing two tasks against the same
     * daily limit, wait for each other. FOR NO KEY UPDATE does not block the foreign key checks, so the other writes
     * of the user (inventory, roster) go on. It needs a read-write transaction.
     */
    @Query(value = "select id from users where id = :id for no key update", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);
}
