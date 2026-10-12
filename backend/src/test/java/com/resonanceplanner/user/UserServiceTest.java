package com.resonanceplanner.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.dto.UpdateProfileRequest;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

// Unit test with mocks: the cases that are hard to provoke with a real database (races, errors of the database)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final long USER_ID = 7L;
    // What PostgreSQL answers for a unique index and for a NOT NULL constraint
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String NOT_NULL_VIOLATION = "23502";

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, TestGameRules.withPlayTimeOptions(30, 60, 120));
    }

    @Test
    void turnsARaceOnTheUsernameIndexIntoTheSameConflictAnswer() {
        // The check passed, but another request took the username a moment before this one saved
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(rover()));
        doThrow(new DataIntegrityViolationException("duplicate", new SQLException("duplicate key", UNIQUE_VIOLATION)))
                .when(userRepository).flush();

        assertThatThrownBy(() -> userService.update(USER_ID, new UpdateProfileRequest("taken.just.now", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questo username è già in uso.");
    }

    @Test
    void doesNotHideAnIntegrityErrorThatIsNotADuplicate() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(rover()));
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("not null", new SQLException("null value", NOT_NULL_VIOLATION));
        doThrow(failure).when(userRepository).flush();

        assertThatThrownBy(() -> userService.update(USER_ID, new UpdateProfileRequest("new.name", null)))
                .isSameAs(failure);
    }

    @Test
    void changesNothingWhenThePlayTimeIsNotValid() {
        User user = rover();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.update(USER_ID, new UpdateProfileRequest("new.name", 45)))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        e -> assertThat(e.getFields()).isEqualTo(Map.of("playTimeMinutes",
                                "Scegli un tempo di gioco tra 30, 60 o 120 minuti.")));
        assertThat(user.getUsername()).isEqualTo("rover");
        verify(userRepository, never()).flush();
    }

    @Test
    void namesAPlayTimeOptionAloneWithoutAnOr() {
        UserService oneOption = new UserService(userRepository, TestGameRules.withPlayTimeOptions(60));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(rover()));

        assertThatThrownBy(() -> oneOption.update(USER_ID, new UpdateProfileRequest(null, 30)))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        e -> assertThat(e.getFields().get("playTimeMinutes"))
                                .isEqualTo("Scegli un tempo di gioco tra 60 minuti."));
    }

    @Test
    void treatsAUserWhoDoesNotExistAnymoreAsNotLoggedIn() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // deleteAccount answers 0 (no row deleted) without a stub
        assertThatThrownBy(() -> userService.getProfile(USER_ID)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> userService.update(USER_ID, new UpdateProfileRequest(null, null)))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> userService.delete(USER_ID)).isInstanceOf(UnauthorizedException.class);
    }

    private static User rover() {
        return User.create("rover", "rover@example.com", "hash", Role.USER);
    }
}
