package com.resonanceplanner.user;

import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.ForbiddenException;
import com.resonanceplanner.common.error.UniqueViolation;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.dto.UpdateProfileRequest;
import com.resonanceplanner.user.dto.UserResponse;
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

    private final UserRepository userRepository;
    private final PlayTimeRules playTimeRules;

    public UserService(UserRepository userRepository, PlayTimeRules playTimeRules) {
        this.userRepository = userRepository;
        this.playTimeRules = playTimeRules;
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
            playTimeRules.requireValid(playTime);
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
            if (!UniqueViolation.isIn(e)) {
                throw e;
            }
            throw new ConflictException(USERNAME_TAKEN, "username");
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
            throw new ConflictException(USERNAME_TAKEN, "username");
        }
    }

    /**
     * The user of a token on /api/admin must still exist (401) and still be ADMIN (403): Spring Security checks only the
     * role written in the token, which stays valid for 24 hours (D45, D102).
     */
    public void requireAdmin(Long userId) {
        if (requireUser(userId).getRole() != Role.ADMIN) {
            throw new ForbiddenException();
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
    }
}
