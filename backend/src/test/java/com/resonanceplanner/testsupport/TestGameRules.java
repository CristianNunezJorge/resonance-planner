package com.resonanceplanner.testsupport;

import com.resonanceplanner.catalog.GameRules;
import java.util.List;

/** Game rules for the unit tests that need only a few of the values, so a change of the record is fixed here once. */
public final class TestGameRules {

    private TestGameRules() {}

    public static GameRules withPlayTimeOptions(Integer... minutes) {
        return new GameRules(null, null, List.of(), 40, null, 4, null, List.of(minutes));
    }
}
