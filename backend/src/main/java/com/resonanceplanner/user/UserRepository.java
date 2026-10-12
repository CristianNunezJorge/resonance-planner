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
}
