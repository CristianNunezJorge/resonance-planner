package com.resonanceplanner.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.event.dto.AdminEventResponse;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.event.dto.EventRequest.Activity;
import com.resonanceplanner.event.dto.EventRequest.Schedule;
import com.resonanceplanner.user.Role;
import com.resonanceplanner.user.User;
import com.resonanceplanner.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// No @Transactional on the test: every call of the service opens and commits its own transaction, like in production.
// In the other tests the service joins the transaction of the test, which never commits, so they could not notice a
// write that answers 200 and saves nothing.
@SpringBootTest
@ActiveProfiles("test")
@Timeout(60)
class AdminEventCommitTest {

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void writesCreateUpdateAndDeleteToTheDatabaseWhenEachCallEnds() {
        // A name and an email that no earlier run can have left behind in the test database
        String unique = "events.commit." + UUID.randomUUID().toString().substring(0, 8);
        User admin = userRepository.saveAndFlush(User.create(unique, unique + "@example.com", "hash", Role.ADMIN));
        Long eventId = null;
        try {
            AdminEventResponse created = adminEventService.create(admin.getId(), request(unique, List.of(
                    new Activity("Livello 1", 20, ActivityFrequency.ONCE, null),
                    new Activity("Livello 2", 20, ActivityFrequency.ONCE, 2))));
            eventId = created.id();
            assertThat(countIn("event_activity", eventId)).isEqualTo(2);
            assertThat(countIn("event_active_weekday", eventId)).isEqualTo(2);

            // Every day (no rows), two rewards and one activity instead of two
            EventRequest replaced = new EventRequest(unique, EventType.COMBAT_STAGES, "Evento cambiato.",
                    request(unique, List.of()).schedule(), Set.of(), List.of("Astrite", "Shell Credit"),
                    List.of(new Activity("Solo uno", 30, ActivityFrequency.DAILY, null)), null);
            adminEventService.update(admin.getId(), eventId, replaced);
            assertThat(countIn("event_active_weekday", eventId)).isZero();
            assertThat(jdbc.queryForList("select reward from event_reward where event_id = ? order by position",
                    String.class, eventId)).containsExactly("Astrite", "Shell Credit");
            assertThat(jdbc.queryForList("select position from event_activity where event_id = ?", Integer.class,
                    eventId)).containsExactly(0);
            assertThat(jdbc.queryForObject("select title from event_activity where event_id = ?", String.class, eventId))
                    .isEqualTo("Solo uno");

            adminEventService.delete(admin.getId(), eventId);
            assertThat(jdbc.queryForObject("select count(*) from game_event where id = ?", Integer.class, eventId)).isZero();
            assertThat(countIn("event_activity", eventId)).isZero();
        } finally {
            // This test commits, so it cleans up after itself (ON DELETE CASCADE removes days, rewards and activities)
            if (eventId != null) {
                jdbc.update("delete from game_event where id = ?", eventId);
            }
            jdbc.update("delete from users where id = ?", admin.getId());
        }
    }

    private static EventRequest request(String name, List<Activity> activities) {
        Schedule schedule = new Schedule(ScheduleType.FIXED, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18),
                null, null, null);
        return new EventRequest(name, EventType.COMBAT_STAGES, "Evento di prova del commit.", schedule,
                Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), List.of("Astrite"), activities, null);
    }

    // A query of its own, outside the persistence context: it sees only what was really committed
    private Integer countIn(String table, Long eventId) {
        return jdbc.queryForObject("select count(*) from " + table + " where event_id = ?", Integer.class, eventId);
    }
}
