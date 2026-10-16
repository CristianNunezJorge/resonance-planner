package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.GameRules;
import java.time.LocalDate;
import java.util.List;

/**
 * The game rules the frontend needs: Waveplate limits, ascension levels, play time options and the day from which
 * the offset of a recurring event is counted (D29, D122).
 */
public record GameRulesResponse(
        Regeneration waveplate,
        Regeneration waveplateCrystals,
        List<Integer> ascensionLevels,
        int maxLevel,
        List<Integer> playTimeOptionsMinutes,
        LocalDate demoEventsAnchorDate) {

    public record Regeneration(int max, int regenMinutes) {}

    public static GameRulesResponse from(GameRules rules) {
        return new GameRulesResponse(
                new Regeneration(rules.waveplate().max(), rules.waveplate().regenMinutes()),
                new Regeneration(rules.waveplateCrystals().max(), rules.waveplateCrystals().regenMinutes()),
                rules.ascensionLevels(),
                rules.maxLevel(),
                rules.playTimeOptionsMinutes(),
                rules.demoEventsAnchorDate());
    }
}
