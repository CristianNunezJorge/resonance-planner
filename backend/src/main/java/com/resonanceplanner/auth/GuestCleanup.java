package com.resonanceplanner.auth;

import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes the guests of "Prova la demo" when their token has expired (D134): after that nobody can reach the account,
 * and the database deletes everything it has (D42). It runs every hour and one minute after the start, because the
 * free server sleeps when nobody uses it: a guest can stay longer than 24 hours, but nobody can use it.
 */
@Component
public class GuestCleanup {

    private static final Logger log = LoggerFactory.getLogger(GuestCleanup.class);

    private final UserRepository userRepository;
    private final Clock clock;

    public GuestCleanup(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    public void run() {
        int deleted = deleteExpiredGuests();
        if (deleted > 0) {
            log.info("Deleted {} guests whose token had expired", deleted);
        }
    }

    /** The guests created before the life of a token, in one statement; how many were deleted. */
    public int deleteExpiredGuests() {
        return userRepository.deleteGuestsCreatedBefore(clock.instant().minus(JwtService.TOKEN_VALIDITY));
    }
}
