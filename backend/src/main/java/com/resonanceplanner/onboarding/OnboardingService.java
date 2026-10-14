package com.resonanceplanner.onboarding;

import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.common.error.UniqueViolation;
import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.OwnedCharacterRepository;
import com.resonanceplanner.roster.RosterRules;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.user.PlayTimeRules;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import com.resonanceplanner.user.dto.UserResponse;
import com.resonanceplanner.waveplate.WaveplateRules;
import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The guided setup of the user of the token (D20, D50, D106): roster, Waveplate and play time are saved together, in
 * one transaction, so there is never a setup half done. Every check comes before the first write, and the setup
 * is "claimed" with one statement, so two submissions at the same moment (a double click) cannot both save.
 */
@Service
public class OnboardingService {

    private final UserRepository userRepository;
    private final OwnedCharacterRepository ownedCharacterRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final RosterRules rosterRules;
    private final WaveplateRules waveplateRules;
    private final PlayTimeRules playTimeRules;
    private final Clock clock;

    public OnboardingService(UserRepository userRepository, OwnedCharacterRepository ownedCharacterRepository,
            GameCharacterRepository gameCharacterRepository, RosterRules rosterRules, WaveplateRules waveplateRules,
            PlayTimeRules playTimeRules, Clock clock) {
        this.userRepository = userRepository;
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.rosterRules = rosterRules;
        this.waveplateRules = waveplateRules;
        this.playTimeRules = playTimeRules;
        this.clock = clock;
    }

    /**
     * Saves the whole setup and returns the updated user. The answers, in the order they are decided: a body of the
     * wrong shape (a missing value, a number that is not whole) is a 400 before this method runs; a user that does not
     * exist anymore gets 401 (the token stays valid until it expires, D45, D102); a value that breaks a rule of the
     * game is a 400 naming every wrong field, and nothing is saved; a setup that was already completed is a 409. So
     * someone who has already completed the setup and sends a body that is not valid gets the 400, not the 409.
     */
    @Transactional
    public UserResponse complete(Long userId, OnboardingRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
        Map<String, String> problems = problemsOf(request);
        if (!problems.isEmpty()) {
            throw new FieldValidationException(problems);
        }
        if (userRepository.claimOnboarding(userId) == 0) {
            // No row was claimed: either the setup was completed, or the account was deleted a moment after the check
            if (!userRepository.existsById(userId)) {
                throw new UnauthorizedException();
            }
            throw new ConflictException("La configurazione è già stata completata.");
        }
        replaceRoster(userId, request.characters());

        User user = userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
        user.changePlayTimeMinutes(request.playTimeMinutes());
        user.recordWaveplate(new WaveplateSnapshot(request.waveplate().value(), request.waveplate().crystalsOrZero(),
                clock.instant()));
        return UserResponse.from(user);
    }

    // The setup is the first complete picture of the roster: what was added before through the API is replaced (D109)
    private void replaceRoster(Long userId, List<AddCharacterRequest> characters) {
        try {
            ownedCharacterRepository.deleteAllOf(userId);
            for (AddCharacterRequest character : characters) {
                ownedCharacterRepository.saveAndFlush(new OwnedCharacter(userId, character.characterId(), character.progress()));
            }
        } catch (DataIntegrityViolationException e) {
            if (!UniqueViolation.isIn(e)) {
                throw e;
            }
            // The same user added a character through the API from another tab, a moment after the roster was
            // emptied: the unique index refuses it. Everything rolls back, and trying again works.
            throw new ConflictException("Il tuo roster è cambiato mentre salvavi la configurazione: riprova.");
        }
    }

    // Every wrong field, in the order of the three steps: characters, then Waveplate, then play time
    private Map<String, String> problemsOf(OnboardingRequest request) {
        Map<String, String> problems = new LinkedHashMap<>();
        List<AddCharacterRequest> characters = request.characters();
        Set<String> inCatalog = gameCharacterRepository
                .findAllById(characters.stream().map(AddCharacterRequest::characterId).distinct().toList()).stream()
                .map(GameCharacter::getId)
                .collect(Collectors.toSet());
        Set<String> alreadyChosen = new HashSet<>();

        for (int i = 0; i < characters.size(); i++) {
            AddCharacterRequest character = characters.get(i);
            String prefix = "characters[" + i + "].";
            if (!inCatalog.contains(character.characterId())) {
                problems.put(prefix + "characterId", "Questo personaggio non esiste.");
            } else if (!alreadyChosen.add(character.characterId())) {
                problems.put(prefix + "characterId", "Hai scelto due volte questo personaggio.");
            }
            rosterRules.problems(character.progress()).forEach((field, message) -> problems.put(prefix + field, message));
        }

        waveplateRules.problems(request.waveplate().value(), request.waveplate().crystalsOrZero())
                .forEach((field, message) -> problems.put("waveplate." + field, message));
        playTimeRules.problem(request.playTimeMinutes()).ifPresent(message -> problems.put("playTimeMinutes", message));
        return problems;
    }
}
