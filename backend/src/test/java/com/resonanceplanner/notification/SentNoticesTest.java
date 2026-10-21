package com.resonanceplanner.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

// The server remembers what it sent (D135): one notice per user, subject and occasion
class SentNoticesTest {

    private final SentNotices sent = new SentNotices();

    private static Notice notice(String subject, String occasion) {
        return new Notice(NotificationType.EVENT_ENDING, "Testo.", subject, occasion);
    }

    @Test
    void sendsANoticeOnlyTheFirstTime() {
        assertThat(sent.firstTime(1L, notice("waveplate", "reading-1"))).isTrue();
        assertThat(sent.firstTime(1L, notice("waveplate", "reading-1"))).isFalse();
    }

    @Test
    void sendsAgainForANewOccasionOfTheSameSubject() {
        sent.firstTime(1L, notice("event:3:2026-10-08", "2026-10-21"));

        assertThat(sent.firstTime(1L, notice("event:3:2026-10-08", "2026-10-22"))).isTrue();
    }

    @Test
    void keepsUsersAndSubjectsApart() {
        sent.firstTime(1L, notice("waveplate", "reading-1"));

        assertThat(sent.firstTime(2L, notice("waveplate", "reading-1"))).isTrue();
        assertThat(sent.firstTime(1L, notice("event:3:2026-10-08", "reading-1"))).isTrue();
    }

    @Test
    void canSendAgainANoticeThatReachedNobody() {
        sent.firstTime(1L, notice("waveplate", "reading-1"));

        sent.undo(1L, notice("waveplate", "reading-1"));

        assertThat(sent.firstTime(1L, notice("waveplate", "reading-1"))).isTrue();
    }

    @Test
    void doesNotUndoANewerOccasion() {
        sent.firstTime(1L, notice("waveplate", "reading-2"));

        // The undo of an older reading arrives late: the newer one stays sent
        sent.undo(1L, notice("waveplate", "reading-1"));
        sent.undo(2L, notice("waveplate", "reading-2"));

        assertThat(sent.firstTime(1L, notice("waveplate", "reading-2"))).isFalse();
    }

    @Test
    void forgetsTheUsersThatNoLongerExist() {
        sent.firstTime(1L, notice("waveplate", "reading-1"));
        sent.firstTime(2L, notice("waveplate", "reading-1"));

        sent.forget(List.of(1L));

        assertThat(sent.firstTime(1L, notice("waveplate", "reading-1"))).isTrue();
        assertThat(sent.firstTime(2L, notice("waveplate", "reading-1"))).isFalse();
    }
}
