package com.resonanceplanner.event.dto;

import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.ScheduleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * An event that the admin creates or replaces (D14, D114). Here only the shape of every value; the rules that join
 * several fields (dates, effect and type, unlock day and duration) are in EventRules. No days of the week means every
 * day (D96); no rewards, activities or effect means none.
 */
public record EventRequest(
        @NotBlank(message = "Inserisci il nome.")
        @Size(max = 120, message = "Il nome può avere al massimo 120 caratteri.") String name,
        @NotNull(message = "Scegli il tipo di evento.") EventType type,
        @NotBlank(message = "Inserisci la descrizione.")
        @Size(max = 2000, message = "La descrizione può avere al massimo 2.000 caratteri.") String description,
        @NotNull(message = "Scegli le date dell'evento.") @Valid Schedule schedule,
        Set<@NotNull(message = "Giorno della settimana non valido.") DayOfWeek> activeWeekdays,
        @Size(max = 10, message = "Un evento può avere al massimo 10 ricompense.")
        List<@NotBlank(message = "La ricompensa non può essere vuota.")
                @Size(max = 120, message = "Una ricompensa può avere al massimo 120 caratteri.") String> rewards,
        @Size(max = 20, message = "Un evento può avere al massimo 20 attività.")
        List<@NotNull(message = "Attività non valida.") @Valid Activity> activities,
        @Valid Effect effect) {

    public Set<DayOfWeek> activeWeekdaysOrEvery() {
        return activeWeekdays == null ? Set.of() : activeWeekdays;
    }

    public List<String> rewardsOrNone() {
        return rewards == null ? List.of() : rewards;
    }

    public List<Activity> activitiesOrNone() {
        return activities == null ? List.of() : activities;
    }

    /** FIXED uses the two dates, RECURRING the three numbers (D29); the values of the other kind are ignored. */
    public record Schedule(
            @NotNull(message = "Scegli se l'evento ha date fisse o si ripete.") ScheduleType type,
            LocalDate startDate,
            LocalDate endDate,
            Integer startOffsetDays,
            Integer durationDays,
            Integer repeatEveryDays) {}

    public record Activity(
            @NotBlank(message = "Inserisci il titolo dell'attività.")
            @Size(max = 120, message = "Il titolo può avere al massimo 120 caratteri.") String title,
            @NotNull(message = "Inserisci i minuti.")
            @Min(value = 1, message = "I minuti vanno da 1 a 600.")
            @Max(value = 600, message = "I minuti vanno da 1 a 600.") Integer minutes,
            @NotNull(message = "Scegli la frequenza.") ActivityFrequency frequency,
            @Min(value = 1, message = "Il giorno di sblocco parte da 1.") Integer unlockDay) {}

    public record Effect(
            @NotNull(message = "Scegli il tipo di effetto.") EffectType type,
            @NotNull(message = "Scegli il tipo di fonte.") FarmingSourceType sourceType,
            @NotNull(message = "Inserisci il moltiplicatore.")
            @DecimalMin(value = "1", inclusive = false, message = "Il moltiplicatore deve essere maggiore di 1.")
            @DecimalMax(value = "10", message = "Il moltiplicatore può essere al massimo 10.")
            @Digits(integer = 2, fraction = 2, message = "Il moltiplicatore può avere al massimo due decimali.")
            BigDecimal multiplier,
            @Min(value = 1, message = "Il limite giornaliero va da 1 a 100.")
            @Max(value = 100, message = "Il limite giornaliero va da 1 a 100.") Integer dailyLimit) {}
}
