package com.resonanceplanner.waveplate;

import com.resonanceplanner.common.error.NotFoundException;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import com.resonanceplanner.user.WaveplateSnapshot;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import com.resonanceplanner.waveplate.dto.WaveplateResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Waveplate of the user of the token (D36, D44). The token stays valid until it expires (D45), so every method
 * answers 401 if its user does not exist anymore (D102).
 */
@Service
@Transactional(readOnly = true)
public class WaveplateService {

    private final UserRepository userRepository;
    private final WaveplateRules rules;
    private final Clock clock;

    public WaveplateService(UserRepository userRepository, WaveplateRules rules, Clock clock) {
        this.userRepository = userRepository;
        this.rules = rules;
        this.clock = clock;
    }

    /** The values now, calculated from the last reading. */
    public WaveplateResponse get(Long userId) {
        WaveplateSnapshot snapshot = requireUser(userId).getWaveplate();
        if (snapshot == null) {
            throw new NotFoundException("Non hai ancora salvato le tue Waveplate.");
        }
        return respond(snapshot, clock.instant());
    }

    /** Saves what the user reads in the game; the moment of the reading is chosen here, never by the client. */
    @Transactional
    public WaveplateResponse update(Long userId, UpdateWaveplateRequest request) {
        User user = requireUser(userId);
        rules.requireValidReading(request.value(), request.crystalsOrZero());

        // The snapshot keeps microseconds, like the database: the answer of the PUT is the same as the next GET
        WaveplateSnapshot snapshot = new WaveplateSnapshot(request.value(), request.crystalsOrZero(), clock.instant());
        user.recordWaveplate(snapshot);
        return respond(snapshot, snapshot.recordedAt());
    }

    private WaveplateResponse respond(WaveplateSnapshot snapshot, Instant now) {
        WaveplateState state = rules.stateAt(snapshot, now);
        return new WaveplateResponse(state.value(), state.crystals(), rules.waveplateMax(), rules.crystalsMax(),
                state.fullAt(), state.crystalsFullAt(), snapshot.recordedAt());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
    }
}
