package com.resonanceplanner.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.testsupport.TestGameRules;
import com.resonanceplanner.user.WaveplateSnapshot;
import com.resonanceplanner.waveplate.WaveplateRules;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// When the server warns a connected user (D135): Waveplate full within an hour, editions that end today or tomorrow
class NotificationRulesTest {

    // 14:00 in Italy: summer time lasts until 25/10/2026
    private static final Instant NOW = Instant.parse("2026-10-21T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-21");

    private final NotificationRules rules =
            new NotificationRules(new WaveplateRules(TestGameRules.standard()), ZoneId.of("Europe/Rome"));

    private static WaveplateSnapshot readAgo(int value, int crystals, Duration ago) {
        return new WaveplateSnapshot(value, crystals, NOW.minus(ago));
    }

    private static EventResponse edition(long id, String name, String start, String end) {
        return new EventResponse(id, name, EventType.DOUBLE_DROP, "", LocalDate.parse(start), LocalDate.parse(end),
                null, List.of(), List.of(), List.of(), null);
    }

    @Test
    void staysSilentWhileMoreThanAnHourIsMissing() {
        // 11 Waveplate are missing: full in 66 minutes
        assertThat(rules.waveplate(readAgo(229, 0, Duration.ZERO), NOW)).isEmpty();
    }

    @Test
    void warnsWhenTheWaveplateAreFullWithinAnHour() {
        WaveplateSnapshot reading = readAgo(230, 0, Duration.ZERO);

        Optional<Notice> notice = rules.waveplate(reading, NOW);

        assertThat(notice).contains(new Notice(NotificationType.WAVEPLATE_ALMOST_FULL,
                "Waveplate quasi piene: 230/240, piene alle 15:00.", "waveplate", reading.recordedAt().toString()));
    }

    @Test
    void countsTheWaveplateRegeneratedSinceTheReading() {
        // 220 an hour ago: 230 now, full in an hour
        Optional<Notice> notice = rules.waveplate(readAgo(220, 0, Duration.ofHours(1)), NOW);

        assertThat(notice).map(Notice::message).contains("Waveplate quasi piene: 230/240, piene alle 15:00.");
    }

    @Test
    void warnsUntilTheLastStep() {
        // 239 read 5 minutes ago: full in a minute
        Optional<Notice> notice = rules.waveplate(readAgo(239, 0, Duration.ofMinutes(5)), NOW);

        assertThat(notice).map(Notice::type).contains(NotificationType.WAVEPLATE_ALMOST_FULL);
        assertThat(notice).map(Notice::message).contains("Waveplate quasi piene: 239/240, piene alle 14:01.");
    }

    @Test
    void saysSinceWhenTheWaveplateAreFull() {
        // 200 five hours ago: full after 4 hours (13:00 in Italy), then 1 Crystal every 12 minutes for an hour
        Optional<Notice> notice = rules.waveplate(readAgo(200, 0, Duration.ofHours(5)), NOW);

        assertThat(notice).map(Notice::type).contains(NotificationType.WAVEPLATE_FULL);
        assertThat(notice).map(Notice::message)
                .contains("Le Waveplate sono piene dalle 13:00: ora si accumulano i Crystals (5/480).");
    }

    @Test
    void namesTheDayWhenTheWaveplateGotFullOnAnotherDay() {
        // 0 two days ago: full 24 hours later, yesterday at 14:00, and 120 Crystals since
        Optional<Notice> notice = rules.waveplate(readAgo(0, 0, Duration.ofDays(2)), NOW);

        assertThat(notice).map(Notice::message)
                .contains("Le Waveplate sono piene dal 20/10 alle 14:00: ora si accumulano i Crystals (120/480).");
    }

    @Test
    void saysWhenTheCrystalsAreFullToo() {
        Optional<Notice> notice = rules.waveplate(readAgo(0, 0, Duration.ofDays(10)), NOW);

        assertThat(notice).map(Notice::message).contains(
                "Le Waveplate sono piene dal 12/10 alle 14:00 e anche i Crystals sono al massimo (480/480).");
    }

    @Test
    void usesTheReadingAsTheOccasionFromAlmostFullToFull() {
        WaveplateSnapshot reading = readAgo(235, 0, Duration.ZERO);

        Optional<Notice> almostFull = rules.waveplate(reading, NOW);
        Optional<Notice> full = rules.waveplate(reading, NOW.plus(Duration.ofHours(1)));

        // The same subject and occasion: whoever got the first one does not get the second (one notice per reading)
        assertThat(almostFull).map(Notice::type).contains(NotificationType.WAVEPLATE_ALMOST_FULL);
        assertThat(full).map(Notice::type).contains(NotificationType.WAVEPLATE_FULL);
        assertThat(full.orElseThrow().subject()).isEqualTo(almostFull.orElseThrow().subject());
        assertThat(full.orElseThrow().occasion()).isEqualTo(almostFull.orElseThrow().occasion());
    }

    @Test
    void staysSilentForAReadingThatWasAlreadyFull() {
        // The user wrote 240: they already know
        assertThat(rules.waveplate(readAgo(240, 30, Duration.ofHours(2)), NOW)).isEmpty();
    }

    @Test
    void staysSilentWithoutAReading() {
        assertThat(rules.waveplate(null, NOW)).isEmpty();
    }

    @Test
    void announcesTheLastDayOfAnEdition() {
        List<Notice> notices = rules.endingEvents(List.of(edition(7, "Bountiful Crescendo", "2026-10-14", "2026-10-21")),
                TODAY);

        assertThat(notices).containsExactly(new Notice(NotificationType.EVENT_ENDING,
                "Ultimo giorno di «Bountiful Crescendo».", "event:7:2026-10-14", "2026-10-21"));
    }

    @Test
    void announcesAnEditionThatEndsTomorrow() {
        List<Notice> notices = rules.endingEvents(List.of(edition(3, "By Moon's Grace", "2026-10-08", "2026-10-22")),
                TODAY);

        assertThat(notices).containsExactly(new Notice(NotificationType.EVENT_ENDING,
                "«By Moon's Grace» finisce domani.", "event:3:2026-10-08", "2026-10-21"));
    }

    @Test
    void ignoresTheEditionsThatEndLater() {
        assertThat(rules.endingEvents(List.of(edition(3, "Later", "2026-10-20", "2026-10-23")), TODAY)).isEmpty();
    }

    @Test
    void keepsEveryEditionApart() {
        // The same event in two editions has two subjects: each edition gets its own notice
        List<Notice> notices = rules.endingEvents(List.of(
                edition(5, "Lament Recon", "2026-10-07", "2026-10-21"),
                edition(5, "Lament Recon", "2026-10-22", "2026-10-22")), TODAY);

        assertThat(notices).extracting(Notice::subject).containsExactly("event:5:2026-10-07", "event:5:2026-10-22");
    }
}
