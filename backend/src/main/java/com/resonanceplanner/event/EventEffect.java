package com.resonanceplanner.event;

import com.resonanceplanner.catalog.FarmingSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Effect of an event on farming, for example double drops on Simulation Training up to 3 runs a day (D94).
 * Stored in four columns of game_event; when they are all empty the event has no effect.
 */
@Embeddable
public class EventEffect {

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_type")
    private EffectType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_source_type")
    private FarmingSourceType sourceType;

    @Column(name = "effect_multiplier")
    private BigDecimal multiplier;

    // Empty = every run of the day gets the multiplier
    @Column(name = "effect_daily_limit")
    private Integer dailyLimit;

    protected EventEffect() {
        // for JPA
    }

    public EventEffect(EffectType type, FarmingSourceType sourceType, BigDecimal multiplier, Integer dailyLimit) {
        this.type = type;
        this.sourceType = sourceType;
        this.multiplier = multiplier;
        this.dailyLimit = dailyLimit;
    }

    public EffectType getType() {
        return type;
    }

    public FarmingSourceType getSourceType() {
        return sourceType;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }
}
