package com.resonanceplanner.testsupport;

import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.catalog.GameRules.Regeneration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Game rules for the unit tests that need only a few of the values, so a change of the record is fixed here once. */
public final class TestGameRules {

    private TestGameRules() {}

    public static GameRules withPlayTimeOptions(Integer... minutes) {
        return new GameRules(null, null, List.of(), 40, null, 4, null, List.of(minutes));
    }

    /** The real values of game-rules.json: 240 Waveplate every 6 minutes, then 480 Crystals every 12 minutes. */
    public static GameRules standard() {
        return new GameRules(new Regeneration(240, 6), new Regeneration(480, 12), List.of(20, 40), 40,
                new BigDecimal("0.35"), 4, LocalDate.of(2026, 10, 1), List.of(30, 60, 120));
    }
}
