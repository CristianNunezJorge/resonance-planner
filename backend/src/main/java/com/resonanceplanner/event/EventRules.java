package com.resonanceplanner.event;

import com.resonanceplanner.common.error.FieldValidationException;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.event.dto.EventRequest.Activity;
import com.resonanceplanner.event.dto.EventRequest.Schedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The rules of an event that join several fields (D114): the dates of its schedule, the unlock day of its activities
 * compared with the length of an edition, the effect compared with the type. Pure: no database. The shape of every
 * single value is already checked by the annotations of EventRequest.
 */
@Component
public class EventRules {

    static final int MAX_EDITION_DAYS = 366;
    static final int MAX_OFFSET_DAYS = 3650;
    static final int MAX_REPEAT_DAYS = 3650;
    /** The dates the app accepts, for events and for ranges (D113, D114): far from the limits of LocalDate. */
    public static final LocalDate FIRST_DATE = LocalDate.of(2000, 1, 1);
    public static final LocalDate LAST_DATE = LocalDate.of(2100, 12, 31);
    public static final String DATE_WINDOW = "Le date vanno dal 01/01/2000 al 31/12/2100.";

    private static final int DAYS_IN_A_WEEK = 7;

    /** True when the date is inside the window of the app. */
    public static boolean isInWindow(LocalDate date) {
        return !date.isBefore(FIRST_DATE) && !date.isAfter(LAST_DATE);
    }

    /** Every wrong field with its message, in the order of the form; empty if the event is possible. */
    public Map<String, String> problems(EventRequest request) {
        Map<String, String> problems = new LinkedHashMap<>();
        Integer editionDays = checkSchedule(problems, request.schedule());
        // Days of the week and unlock days are compared with an edition only when the dates are right
        if (editionDays != null) {
            checkWeekdays(problems, request.schedule(), request.activeWeekdaysOrEvery(), editionDays);
            checkUnlockDays(problems, request.activitiesOrNone(), editionDays);
        }
        checkEffect(problems, request);
        return problems;
    }

    /** Refuses an event that is not possible, naming every wrong field in one answer. */
    public void requireValid(EventRequest request) {
        Map<String, String> problems = problems(request);
        if (!problems.isEmpty()) {
            throw new FieldValidationException(problems);
        }
    }

    // The days of one edition, or null when the schedule is wrong
    private static Integer checkSchedule(Map<String, String> problems, Schedule schedule) {
        return schedule.type() == ScheduleType.FIXED ? checkFixed(problems, schedule) : checkRecurring(problems, schedule);
    }

    private static Integer checkFixed(Map<String, String> problems, Schedule schedule) {
        if (schedule.startDate() == null) {
            problems.put("schedule.startDate", "Inserisci la data di inizio.");
        }
        if (schedule.endDate() == null) {
            problems.put("schedule.endDate", "Inserisci la data di fine.");
        }
        if (schedule.startDate() == null || schedule.endDate() == null) {
            return null;
        }
        if (!isInWindow(schedule.startDate()) || !isInWindow(schedule.endDate())) {
            String field = isInWindow(schedule.startDate()) ? "schedule.endDate" : "schedule.startDate";
            problems.put(field, DATE_WINDOW);
            return null;
        }
        if (schedule.endDate().isBefore(schedule.startDate())) {
            problems.put("schedule.endDate", "La fine non può venire prima dell'inizio.");
            return null;
        }
        long days = ChronoUnit.DAYS.between(schedule.startDate(), schedule.endDate()) + 1;
        if (days > MAX_EDITION_DAYS) {
            problems.put("schedule.endDate", "Un evento può durare al massimo " + MAX_EDITION_DAYS + " giorni.");
            return null;
        }
        return (int) days;
    }

    private static Integer checkRecurring(Map<String, String> problems, Schedule schedule) {
        int before = problems.size();
        Integer offset = schedule.startOffsetDays();
        Integer duration = schedule.durationDays();
        Integer repeat = schedule.repeatEveryDays();
        if (offset == null) {
            problems.put("schedule.startOffsetDays", "Inserisci di quanti giorni l'evento parte dalla data di riferimento.");
        } else if (Math.abs((long) offset) > MAX_OFFSET_DAYS) {
            problems.put("schedule.startOffsetDays",
                    "Lo scostamento va da -" + MAX_OFFSET_DAYS + " a " + MAX_OFFSET_DAYS + " giorni.");
        }
        boolean durationOk = duration != null && duration >= 1 && duration <= MAX_EDITION_DAYS;
        if (duration == null) {
            problems.put("schedule.durationDays", "Inserisci la durata.");
        } else if (!durationOk) {
            problems.put("schedule.durationDays", "La durata va da 1 a " + MAX_EDITION_DAYS + " giorni.");
        }
        // The repetition cannot be shorter than an edition: two editions would overlap
        int shortestRepeat = durationOk ? duration : 1;
        if (repeat == null) {
            problems.put("schedule.repeatEveryDays", "Inserisci ogni quanti giorni si ripete.");
        } else if (repeat < shortestRepeat || repeat > MAX_REPEAT_DAYS) {
            problems.put("schedule.repeatEveryDays",
                    "La ripetizione va da " + shortestRepeat + " a " + MAX_REPEAT_DAYS + " giorni.");
        }
        return problems.size() == before ? duration : null;
    }

    /**
     * An event must be active on at least one day, otherwise it never appears. A FIXED event has its dates to look at.
     * A recurring edition of 7 days or more contains every day of the week; a shorter one does not always contain the
     * same days, because the editions move through the week: there the days must not be chosen.
     */
    private static void checkWeekdays(Map<String, String> problems, Schedule schedule, Set<DayOfWeek> weekdays,
            int editionDays) {
        if (weekdays.isEmpty() || weekdays.size() == DAYS_IN_A_WEEK) {
            return;
        }
        if (schedule.type() == ScheduleType.FIXED) {
            boolean someDayActive = schedule.startDate().datesUntil(schedule.endDate().plusDays(1))
                    .anyMatch(date -> weekdays.contains(date.getDayOfWeek()));
            if (!someDayActive) {
                problems.put("activeWeekdays", "Nessuno dei giorni scelti cade tra l'inizio e la fine dell'evento.");
            }
        } else if (editionDays < DAYS_IN_A_WEEK) {
            problems.put("activeWeekdays", "Un evento ricorrente di meno di 7 giorni è attivo in tutti i suoi giorni: "
                    + "in alcune edizioni i giorni scelti potrebbero mancare.");
        }
    }

    private static void checkUnlockDays(Map<String, String> problems, List<Activity> activities, int editionDays) {
        for (int i = 0; i < activities.size(); i++) {
            Integer unlockDay = activities.get(i).unlockDay();
            if (unlockDay != null && unlockDay > editionDays) {
                problems.put("activities[" + i + "].unlockDay",
                        "Il giorno di sblocco va da 1 a " + editionDays + ", la durata dell'evento.");
            }
        }
    }

    // Only a double-drop event changes the farming, and it must say what it doubles (D94)
    private static void checkEffect(Map<String, String> problems, EventRequest request) {
        boolean doubleDrop = request.type() == EventType.DOUBLE_DROP;
        if (doubleDrop && request.effect() == null) {
            problems.put("effect", "Un evento di ricompense doppie ha bisogno dell'effetto.");
        } else if (!doubleDrop && request.effect() != null) {
            problems.put("effect", "Solo gli eventi di ricompense doppie hanno un effetto.");
        }
    }
}
