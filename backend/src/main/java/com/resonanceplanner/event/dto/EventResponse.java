package com.resonanceplanner.event.dto;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.Edition;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventActivity;
import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.GameEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * One edition of an event (D113): its whole dates, the days on which it is active, and what it offers.
 * {@code activeWeekdays} is null when the event runs every day (D96); {@code effect} is null without an effect.
 */
public record EventResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Bountiful Crescendo") String name,
        @Schema(example = "DOUBLE_DROP") EventType type,
        @Schema(example = "Simulation Training e Forgery Challenge danno ricompense doppie, fino a 3 volte al giorno.")
        String description,
        @Schema(description = "Primo giorno dell'edizione", example = "2026-10-18") LocalDate startDate,
        @Schema(description = "Ultimo giorno dell'edizione", example = "2026-10-24") LocalDate endDate,
        @Schema(description = "Giorni della settimana in cui è attivo; null = tutti", nullable = true, example = "null")
        List<DayOfWeek> activeWeekdays,
        @Schema(description = "I giorni in cui l'edizione è attiva",
                example = "[\"2026-10-18\", \"2026-10-19\", \"2026-10-20\", \"2026-10-21\", \"2026-10-22\", "
                        + "\"2026-10-23\", \"2026-10-24\"]")
        List<LocalDate> activeDates,
        @Schema(example = "[\"Ricompense doppie in Simulation Training e Forgery Challenge, massimo 3 al giorno\"]")
        List<String> rewards,
        List<Activity> activities,
        @Schema(description = "null se l'evento non ha un effetto", nullable = true, types = {"object", "null"})
        Effect effect) {

    public static EventResponse from(GameEvent event, Edition edition) {
        List<DayOfWeek> weekdays = event.getActiveWeekdays().isEmpty()
                ? null
                : event.getActiveWeekdays().stream().sorted().toList();
        return new EventResponse(event.getId(), event.getName(), event.getType(), event.getDescription(),
                edition.start(), edition.end(), weekdays, edition.activeDates(), List.copyOf(event.getRewards()),
                event.getActivities().stream().map(Activity::from).toList(), Effect.from(event.getEffect()));
    }

    /** {@code unlockDay} is the day of the edition from which the activity is available; null = from the first. */
    @Schema(name = "EventActivity")
    public record Activity(
            @Schema(example = "Obiettivi della festa") String title,
            @Schema(example = "15") int minutes,
            @Schema(example = "DAILY") ActivityFrequency frequency,
            @Schema(description = "Giorno dell'edizione da cui è disponibile; null = dal primo", nullable = true,
                    example = "null")
            Integer unlockDay) {

        static Activity from(EventActivity activity) {
            return new Activity(activity.getTitle(), activity.getMinutes(), activity.getFrequency(),
                    activity.getUnlockDay());
        }
    }

    /** {@code dailyLimit} is null when every run of the day gets the multiplier (D94). */
    @Schema(name = "EventEffect")
    public record Effect(
            @Schema(example = "DROP_MULTIPLIER") EffectType type,
            @Schema(example = "SIMULATION_TRAINING") FarmingSourceType sourceType,
            @Schema(example = "2") BigDecimal multiplier,
            @Schema(description = "Run al giorno con il moltiplicatore; null = tutte", nullable = true, example = "3")
            Integer dailyLimit) {

        static Effect from(EventEffect effect) {
            // Hibernate gives an embedded value with all its columns empty as null
            if (effect == null || effect.getType() == null) {
                return null;
            }
            return new Effect(effect.getType(), effect.getSourceType(), effect.getMultiplier(), effect.getDailyLimit());
        }
    }
}
