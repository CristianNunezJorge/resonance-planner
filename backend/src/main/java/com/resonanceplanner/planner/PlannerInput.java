package com.resonanceplanner.planner;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.dto.EventResponse.Effect;
import com.resonanceplanner.roster.Progress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything the planner needs (D57, D123), already read from the database: the planner itself only calculates.
 *
 * @param characters the roster in order of priority, the most important first
 * @param inventory the quantity of each material, by id
 * @param days the days of the plan, in order
 * @param minutesPerDay the play time of a day, used to estimate the days that are still needed
 */
public record PlannerInput(List<CharacterGoal> characters, Map<String, Long> inventory, List<PlannerDay> days,
        int minutesPerDay) {

    public PlannerInput {
        characters = List.copyOf(characters);
        inventory = Map.copyOf(inventory);
        days = List.copyOf(days);
    }

    /** A character of the roster with its present and its goal. */
    public record CharacterGoal(String characterId, Progress progress) {}

    /**
     * A day of the plan with what is free on it.
     *
     * @param waveplates the Waveplate that can be spent on the day
     * @param minutes the minutes left after the tasks that stay in the calendar
     * @param effects the effects of the events active on the day
     * @param runsDone the runs of each type of source already done on the day, which use up the daily limit first
     * @param activities the event activities that can be done on the day and are not done yet, in order
     */
    public record PlannerDay(LocalDate date, boolean excluded, int waveplates, int minutes, List<Effect> effects,
            Map<FarmingSourceType, Long> runsDone, List<EventTodo> activities) {

        public PlannerDay {
            effects = List.copyOf(effects);
            runsDone = Map.copyOf(runsDone);
            activities = List.copyOf(activities);
        }
    }

    /** An activity of one edition of an event; a ONCE activity is planned once for the edition (D123, point 4). */
    public record EventTodo(Long eventId, String eventName, LocalDate editionStart, String title, int minutes,
            ActivityFrequency frequency) {}
}
