package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.GameRules;
import java.util.List;

/** The game rules the frontend needs: Waveplate limits, ascension levels and play time options. */
public record GameRulesResponse(
        Regeneration waveplate,
        Regeneration waveplateCrystals,
        List<Integer> ascensionLevels,
        int maxLevel,
        List<Integer> playTimeOptionsMinutes) {

    public record Regeneration(int max, int regenMinutes) {}

    public static GameRulesResponse from(GameRules rules) {
        return new GameRulesResponse(
                new Regeneration(rules.waveplate().max(), rules.waveplate().regenMinutes()),
                new Regeneration(rules.waveplateCrystals().max(), rules.waveplateCrystals().regenMinutes()),
                rules.ascensionLevels(),
                rules.maxLevel(),
                rules.playTimeOptionsMinutes());
    }
}
