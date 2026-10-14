package com.resonanceplanner.onboarding;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.OwnedCharacterRepository;
import com.resonanceplanner.roster.RosterRules;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.PlayTimeRules;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.waveplate.WaveplateRules;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

// Unit test with mocks: the cases that are hard to provoke with a real database (a race with another request)
@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    private static final long USER_ID = 7L;
    // What PostgreSQL answers for a unique index and for a foreign key
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnedCharacterRepository ownedCharacterRepository;

    @Mock
    private GameCharacterRepository gameCharacterRepository;

    private OnboardingService onboardingService;

    private final OnboardingRequest request = new OnboardingRequest(
            List.of(new AddCharacterRequest("jinhsi", 1, 0, 40, 2)), new UpdateWaveplateRequest(180, null), 60);

    @BeforeEach
    void setUp() {
        var gameRules = TestGameRules.standard();
        onboardingService = new OnboardingService(userRepository, ownedCharacterRepository, gameCharacterRepository,
                new RosterRules(gameRules), new WaveplateRules(gameRules), new PlayTimeRules(gameRules),
                Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC));
    }

    private void theRequestIsValid() {
        when(gameCharacterRepository.findAllById(List.of("jinhsi"))).thenReturn(List.of(new GameCharacter("jinhsi")));
    }

    @Test
    void turnsARaceWithTheRosterOfTheSameUserIntoAConflictAnswer() {
        // The same person added "jinhsi" with POST /api/me/roster from another tab, a moment after the roster was
        // emptied: the unique index (user, character) refuses the insert, and the whole setup rolls back
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        theRequestIsValid();
        when(userRepository.claimOnboarding(USER_ID)).thenReturn(1);
        doThrow(new DataIntegrityViolationException("duplicate", new SQLException("duplicate key", UNIQUE_VIOLATION)))
                .when(ownedCharacterRepository).saveAndFlush(any(OwnedCharacter.class));

        assertThatThrownBy(() -> onboardingService.complete(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Il tuo roster è cambiato mentre salvavi la configurazione: riprova.");
    }

    @Test
    void doesNotHideAnIntegrityErrorThatIsNotADuplicate() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        theRequestIsValid();
        when(userRepository.claimOnboarding(USER_ID)).thenReturn(1);
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("foreign key", new SQLException("no such user", FOREIGN_KEY_VIOLATION));
        doThrow(failure).when(ownedCharacterRepository).saveAndFlush(any(OwnedCharacter.class));

        assertThatThrownBy(() -> onboardingService.complete(USER_ID, request)).isSameAs(failure);
    }

    @Test
    void answersConflictWhenTheSetupWasAlreadyCompleted() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        theRequestIsValid();
        when(userRepository.claimOnboarding(USER_ID)).thenReturn(0);

        assertThatThrownBy(() -> onboardingService.complete(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La configurazione è già stata completata.");
        // A setup that was not claimed writes nothing
        verify(ownedCharacterRepository, never()).deleteAllOf(any());
        verify(ownedCharacterRepository, never()).saveAndFlush(any());
    }

    @Test
    void answersUnauthorizedNotConflictWhenTheUserWasDeletedAfterTheFirstCheck() {
        // The account disappears between the first check and the claim: the claim finds no row, like an already
        // completed setup, but the answer must be the 401 of a deleted user (D102)
        when(userRepository.existsById(USER_ID)).thenReturn(true, false);
        theRequestIsValid();
        when(userRepository.claimOnboarding(USER_ID)).thenReturn(0);

        assertThatThrownBy(() -> onboardingService.complete(USER_ID, request)).isInstanceOf(UnauthorizedException.class);
    }
}
