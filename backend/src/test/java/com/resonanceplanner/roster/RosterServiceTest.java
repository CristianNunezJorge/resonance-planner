package com.resonanceplanner.roster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.UserRepository;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

// Unit test with mocks: the cases that are hard to provoke with a real database (a race between two requests)
@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

    private static final long USER_ID = 7L;
    // What PostgreSQL answers for a unique index and for a foreign key
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Mock
    private OwnedCharacterRepository ownedCharacterRepository;

    @Mock
    private GameCharacterRepository gameCharacterRepository;

    @Mock
    private UserRepository userRepository;

    private RosterService rosterService;

    private final AddCharacterRequest jinhsi = new AddCharacterRequest("jinhsi", 1, 0, 40, 2);

    @BeforeEach
    void setUp() {
        rosterService = new RosterService(ownedCharacterRepository, gameCharacterRepository, userRepository,
                new RosterRules(TestGameRules.standard()));
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(gameCharacterRepository.existsById("jinhsi")).thenReturn(true);
        when(ownedCharacterRepository.existsByUserIdAndCharacterId(USER_ID, "jinhsi")).thenReturn(false);
    }

    @Test
    void turnsARaceOnTheUniqueIndexIntoTheSameConflictAnswer() {
        // The check passed, but another request added the same character a moment before this one saved
        doThrow(new DataIntegrityViolationException("duplicate", new SQLException("duplicate key", UNIQUE_VIOLATION)))
                .when(ownedCharacterRepository).saveAndFlush(any(OwnedCharacter.class));

        assertThatThrownBy(() -> rosterService.add(USER_ID, jinhsi))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Questo personaggio è già nel tuo roster.");
    }

    @Test
    void doesNotHideAnIntegrityErrorThatIsNotADuplicate() {
        // For example the user deleted a moment after the check: it must not look like a duplicate
        DataIntegrityViolationException failure = new DataIntegrityViolationException("foreign key",
                new SQLException("no such user", FOREIGN_KEY_VIOLATION));
        doThrow(failure).when(ownedCharacterRepository).saveAndFlush(any(OwnedCharacter.class));

        assertThatThrownBy(() -> rosterService.add(USER_ID, jinhsi)).isSameAs(failure);
    }
}
