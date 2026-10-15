package com.resonanceplanner.event.dto;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.Edition;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventActivity;
import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * One edition of an event (D113): its whole dates, the days on which it is active, and what it offers.
 * {@code activeWeekdays} is null when the event runs every day (D96); {@code effect} is null without an effect.
 */
public record EventResponse(Long id, String name, EventType type, String description, LocalDate startDate,
        LocalDate endDate, List<DayOfWeek> activeWeekdays, List<LocalDate> activeDates, List<String> rewards,
        List<Activity> activities, Effect effect) {

    public static EventResponse from(GameEvent event, Edition edition) {
        List<DayOfWeek> weekdays = event.getActiveWeekdays().isEmpty()
                ? null
                : event.getActiveWeekdays().stream().sorted().toList();
        return new EventResponse(event.getId(), event.getName(), event.getType(), event.getDescription(),
                edition.start(), edition.end(), weekdays, edition.activeDates(), List.copyOf(event.getRewards()),
                event.getActivities().stream().map(Activity::from).toList(), Effect.from(event.getEffect()));
    }

    /** {@code unlockDay} is the day of the edition from which the activity is available; null = from the first. */
    public record Activity(String title, int minutes, ActivityFrequency frequency, Integer unlockDay) {

        static Activity from(EventActivity activity) {
            return new Activity(activity.getTitle(), activity.getMinutes(), activity.getFrequency(),
                    activity.getUnlockDay());
        }
    }

    /** {@code dailyLimit} is null when every run of the day gets the multiplier (D94). */
    public record Effect(EffectType type, FarmingSourceType sourceType, BigDecimal multiplier, Integer dailyLimit) {

        static Effect from(EventEffect effect) {
            // Hibernate gives an embedded value with all its columns empty as null
            if (effect == null || effect.getType() == null) {
                return null;
            }
            return new Effect(effect.getType(), effect.getSourceType(), effect.getMultiplier(), effect.getDailyLimit());
        }
    }
}
