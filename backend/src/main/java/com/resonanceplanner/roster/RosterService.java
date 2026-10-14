package com.resonanceplanner.roster;

import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.common.error.ConflictException;
import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.common.error.UniqueViolation;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.roster.dto.RosterEntryResponse;
import com.resonanceplanner.roster.dto.UpdateCharacterRequest;
import com.resonanceplanner.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The roster of the user of the token (D44). The token stays valid until it expires (D45), so every method answers
 * 401 if its user does not exist anymore (D102): otherwise the foreign key would answer with a database error.
 */
@Service
@Transactional(readOnly = true)
public class RosterService {

    private static final String ALREADY_IN_ROSTER = "Questo personaggio è già nel tuo roster.";
    private static final String NOT_IN_ROSTER = "Questo personaggio non è nel tuo roster.";

    private final OwnedCharacterRepository ownedCharacterRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final UserRepository userRepository;
    private final RosterRules rules;

    public RosterService(OwnedCharacterRepository ownedCharacterRepository,
            GameCharacterRepository gameCharacterRepository, UserRepository userRepository, RosterRules rules) {
        this.ownedCharacterRepository = ownedCharacterRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.userRepository = userRepository;
        this.rules = rules;
    }

    public List<RosterEntryResponse> list(Long userId) {
        requireUser(userId);
        return ownedCharacterRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(RosterEntryResponse::from)
                .toList();
    }

    /** Adds a character; if anything is wrong nothing is saved and every wrong field is named in the answer. */
    @Transactional
    public RosterEntryResponse add(Long userId, AddCharacterRequest request) {
        requireUser(userId);
        Map<String, String> problems = new LinkedHashMap<>();
        if (!gameCharacterRepository.existsById(request.characterId())) {
            problems.put("characterId", "Questo personaggio non esiste.");
        }
        problems.putAll(rules.problems(request.progress()));
        if (!problems.isEmpty()) {
            throw new FieldValidationException(problems);
        }
        if (ownedCharacterRepository.existsByUserIdAndCharacterId(userId, request.characterId())) {
            throw new ConflictException(ALREADY_IN_ROSTER, "characterId");
        }

        OwnedCharacter owned = new OwnedCharacter(userId, request.characterId(), request.progress());
        try {
            // Writes now, to answer here if the same character was added a moment after the check
            ownedCharacterRepository.saveAndFlush(owned);
        } catch (DataIntegrityViolationException e) {
            if (!UniqueViolation.isIn(e)) {
                throw e;
            }
            throw new ConflictException(ALREADY_IN_ROSTER, "characterId");
        }
        return RosterEntryResponse.from(owned);
    }

    @Transactional
    public RosterEntryResponse update(Long userId, String characterId, UpdateCharacterRequest request) {
        requireUser(userId);
        OwnedCharacter owned = requireOwned(userId, characterId);
        rules.requireValid(request.progress());
        owned.changeProgress(request.progress());
        return RosterEntryResponse.from(owned);
    }

    @Transactional
    public void delete(Long userId, String characterId) {
        requireUser(userId);
        // One statement: two requests that delete the same character at the same moment get one 204 and one 404
        if (ownedCharacterRepository.deleteOne(userId, characterId) == 0) {
            throw new NotFoundException(NOT_IN_ROSTER);
        }
    }

    private OwnedCharacter requireOwned(Long userId, String characterId) {
        return ownedCharacterRepository.findByUserIdAndCharacterId(userId, characterId)
                .orElseThrow(() -> new NotFoundException(NOT_IN_ROSTER));
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
    }
}
