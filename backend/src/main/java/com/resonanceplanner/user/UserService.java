package com.resonanceplanner.user;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.dto.UpdateProfileRequest;
import com.resonanceplanner.user.dto.UserResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The profile of the user of the token (D42, D44). The user always comes from the token, never from the URL. The token
 * stays valid until it expires (D45), so every method answers 401 if its user does not exist anymore.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String USERNAME_TAKEN = "Questo username è già in uso.";
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final UserRepository userRepository;
    private final GameRules gameRules;

    public UserService(UserRepository userRepository, GameRules gameRules) {
        this.userRepository = userRepository;
        this.gameRules = gameRules;
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    /** Changes only what the request contains; if any value is not valid, nothing is changed. */
    @Transactional
    public UserResponse update(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        Integer playTime = request.playTimeMinutes();
        boolean renaming = request.username() != null && !request.username().equals(user.getUsername());

        // First all the checks, then the changes: if a value is not valid nothing is changed. The order matters also
        // for the query of the check, which would write the changes already made to the entity.
        if (playTime != null) {
            requirePlayTimeOption(playTime);
        }
        if (renaming) {
            requireUsernameAvailable(request.username());
        }

        if (renaming) {
            user.changeUsername(request.username());
        }
        if (playTime != null) {
            user.changePlayTimeMinutes(playTime);
        }
        try {
            // Writes now, to answer here if someone took the username a moment after the check
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            throw new ConflictException(USERNAME_TAKEN);
        }
        return UserResponse.from(user);
    }

    /** Deletes the account; the database deletes roster, inventory, activities, proposals and messages with it (D42). */
    @Transactional
    public void delete(Long userId) {
        if (userRepository.deleteAccount(userId) == 0) {
            throw new UnauthorizedException();
        }
    }

    /** Nobody can take the username of someone else, nor the one reserved for the admin (D100). */
    public void requireUsernameAvailable(String username) {
        if (User.ADMIN_USERNAME.equalsIgnoreCase(username) || userRepository.existsByUsername(username)) {
            throw new ConflictException(USERNAME_TAKEN);
        }
    }

    // PostgreSQL answers 23505 for a unique index; any other integrity error is a bug that must not look like a duplicate
    private static boolean isUniqueViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION_SQL_STATE.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
    }

    private void requirePlayTimeOption(int minutes) {
        List<Integer> options = gameRules.playTimeOptionsMinutes();
        if (!options.contains(minutes)) {
            throw new FieldValidationException("playTimeMinutes",
                    "Scegli un tempo di gioco tra " + describe(options) + " minuti.");
        }
    }

    // [30, 60, 120] becomes "30, 60 o 120"
    private static String describe(List<Integer> options) {
        String last = String.valueOf(options.getLast());
        if (options.size() == 1) {
            return last;
        }
        String others = options.subList(0, options.size() - 1).stream().map(String::valueOf).collect(Collectors.joining(", "));
        return others + " o " + last;
    }
}
