package com.resonanceplanner.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // The email must be normalized with User.normalizeEmail before searching
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

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
}
