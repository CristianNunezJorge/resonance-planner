package com.resonanceplanner.event;

import com.resonanceplanner.catalog.GameRules;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The editions of an event and the days on which it is active (D29, D96, D113). Pure calculation. Edition k of a
 * recurring event starts at anchor date + offset + k × repetition and lasts its duration, last day included; k comes
 * from a division, so the calculation does not walk through the years.
 */
@Component
public class EventCalendar {

    private final LocalDate anchorDate;

    public EventCalendar(GameRules gameRules) {
        this.anchorDate = gameRules.demoEventsAnchorDate();
    }

    /** The editions that have at least one active day between from and to, both included, in order. */
    public List<Edition> between(EventSchedule schedule, LocalDate from, LocalDate to) {
        List<Edition> editions = new ArrayList<>();
        for (Span span : spansTouching(schedule, from, to)) {
            LocalDate first = span.start().isAfter(from) ? span.start() : from;
            LocalDate last = span.end().isBefore(to) ? span.end() : to;
            List<LocalDate> activeDates = activeDays(schedule, first, last);
            if (!activeDates.isEmpty()) {
                editions.add(new Edition(span.start(), span.end(), activeDates));
            }
        }
        return editions;
    }

    /** The edition in progress on that day, otherwise the next one; a FIXED event has only its own. */
    public Edition currentOrNext(EventSchedule schedule, LocalDate today) {
        Span span = schedule.type() == ScheduleType.FIXED
                ? new Span(schedule.startDate(), schedule.endDate())
                : recurringSpan(schedule, firstEndingOnOrAfter(schedule, today));
        return new Edition(span.start(), span.end(), activeDays(schedule, span.start(), span.end()));
    }

    private List<Span> spansTouching(EventSchedule schedule, LocalDate from, LocalDate to) {
        if (schedule.type() == ScheduleType.FIXED) {
            boolean touches = !schedule.startDate().isAfter(to) && !schedule.endDate().isBefore(from);
            return touches ? List.of(new Span(schedule.startDate(), schedule.endDate())) : List.of();
        }
        List<Span> spans = new ArrayList<>();
        for (long index = firstEndingOnOrAfter(schedule, from); ; index++) {
            Span span = recurringSpan(schedule, index);
            if (span.start().isAfter(to)) {
                return spans;
            }
            spans.add(span);
        }
    }

    // The first edition whose last day (start + duration - 1) is on or after the date
    private long firstEndingOnOrAfter(EventSchedule schedule, LocalDate date) {
        long daysFromFirstStart = ChronoUnit.DAYS.between(firstStart(schedule), date);
        return Math.ceilDiv(daysFromFirstStart - schedule.durationDays() + 1, schedule.repeatEveryDays());
    }

    private Span recurringSpan(EventSchedule schedule, long index) {
        LocalDate start = firstStart(schedule).plusDays(Math.multiplyExact(index, schedule.repeatEveryDays()));
        return new Span(start, start.plusDays(schedule.durationDays() - 1L));
    }

    // Edition 0 starts at the anchor date plus the offset
    private LocalDate firstStart(EventSchedule schedule) {
        return anchorDate.plusDays(schedule.startOffsetDays());
    }

    private static List<LocalDate> activeDays(EventSchedule schedule, LocalDate first, LocalDate last) {
        return Stream.iterate(first, date -> !date.isAfter(last), date -> date.plusDays(1))
                .filter(date -> schedule.isActiveOn(date.getDayOfWeek()))
                .toList();
    }

    private record Span(LocalDate start, LocalDate end) {}
}
