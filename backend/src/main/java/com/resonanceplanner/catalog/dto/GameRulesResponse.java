package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.GameRules;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * The game rules the frontend needs: Waveplate limits, ascension levels, play time options and the day from which
 * the offset of a recurring event is counted (D29, D122).
 */
public record GameRulesResponse(
        Regeneration waveplate,
        @Schema(example = "{\"max\": 480, \"regenMinutes\": 12}") Regeneration waveplateCrystals,
        @Schema(description = "I livelli in cui si fa un'ascensione", example = "[20, 40]") List<Integer> ascensionLevels,
        @Schema(example = "40") int maxLevel,
        @Schema(description = "Minuti di gioco al giorno che si possono scegliere", example = "[30, 60, 120]")
        List<Integer> playTimeOptionsMinutes,
        @Schema(description = "Il giorno da cui si contano le partenze degli eventi ricorrenti", example = "2026-10-01")
        LocalDate demoEventsAnchorDate) {

    public record Regeneration(
            @Schema(example = "240") int max,
            @Schema(description = "Minuti per ricaricarne una", example = "6") int regenMinutes) {}

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
