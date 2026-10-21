package com.resonanceplanner.notification;

import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.user.WaveplateSnapshot;
import com.resonanceplanner.waveplate.WaveplateRules;
import com.resonanceplanner.waveplate.WaveplateState;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * When the server warns a connected user (D135), with no database and no clock: the Waveplate are full within an
 * hour, or already full, and an edition of an event ends today or tomorrow. The times are written in Italian time
 * (D41).
 */
@Component
public class NotificationRules {

    static final Duration ALMOST_FULL_BEFORE = Duration.ofMinutes(60);
    private static final String WAVEPLATE_SUBJECT = "waveplate";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private final WaveplateRules waveplateRules;
    private final ZoneId zone;

    public NotificationRules(WaveplateRules waveplateRules, ZoneId appZone) {
        this.waveplateRules = waveplateRules;
        this.zone = appZone;
    }

    /**
     * The notice of a reading at `now`, or nothing. The occasion is the reading: one notice per reading, so whoever
     * was told "almost full" is not told "full" later. A reading that was already full says nothing: the user wrote it.
     */
    public Optional<Notice> waveplate(WaveplateSnapshot reading, Instant now) {
        if (reading == null || reading.value() >= waveplateRules.waveplateMax()) {
            return Optional.empty();
        }
        WaveplateState state = waveplateRules.stateAt(reading, now);
        String occasion = reading.recordedAt().toString();
        if (state.fullAt().isAfter(now)) {
            if (Duration.between(now, state.fullAt()).compareTo(ALMOST_FULL_BEFORE) > 0) {
                return Optional.empty();
            }
            String message = "Waveplate quasi piene: %d/%d, piene alle %s.".formatted(state.value(),
                    waveplateRules.waveplateMax(), TIME.format(state.fullAt().atZone(zone)));
            return Optional.of(new Notice(NotificationType.WAVEPLATE_ALMOST_FULL, message, WAVEPLATE_SUBJECT, occasion));
        }
        String since = since(state.fullAt(), now);
        String crystals = "%d/%d".formatted(state.crystals(), waveplateRules.crystalsMax());
        String message = state.crystals() >= waveplateRules.crystalsMax()
                ? "Le Waveplate sono piene %s e anche i Crystals sono al massimo (%s).".formatted(since, crystals)
                : "Le Waveplate sono piene %s: ora si accumulano i Crystals (%s).".formatted(since, crystals);
        return Optional.of(new Notice(NotificationType.WAVEPLATE_FULL, message, WAVEPLATE_SUBJECT, occasion));
    }

    /** One notice for every edition that ends today or tomorrow; the occasion is today, so one notice a day. */
    public List<Notice> endingEvents(List<EventResponse> editions, LocalDate today) {
        return editions.stream()
                .filter(edition -> edition.endDate().equals(today) || edition.endDate().equals(today.plusDays(1)))
                .map(edition -> new Notice(NotificationType.EVENT_ENDING,
                        edition.endDate().equals(today)
                                ? "Ultimo giorno di «%s».".formatted(edition.name())
                                : "«%s» finisce domani.".formatted(edition.name()),
                        "event:%d:%s".formatted(edition.id(), edition.startDate()), today.toString()))
                .toList();
    }

    // "dalle 13:00" today, "dal 20/10 alle 14:00" on another day
    private String since(Instant moment, Instant now) {
        ZonedDateTime at = moment.atZone(zone);
        if (at.toLocalDate().equals(now.atZone(zone).toLocalDate())) {
            return "dalle " + TIME.format(at);
        }
        return "dal " + DAY.format(at) + " alle " + TIME.format(at);
    }
}
