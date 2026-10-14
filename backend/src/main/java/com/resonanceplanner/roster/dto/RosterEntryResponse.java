package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.Progress;

/** A character of the roster. Name, element and icon are in the public catalog, which the frontend already has. */
public record RosterEntryResponse(String characterId, int currentLevel, int currentRank, int targetLevel,
        int targetRank) {

    public static RosterEntryResponse from(OwnedCharacter owned) {
        Progress progress = owned.getProgress();
        return new RosterEntryResponse(owned.getCharacterId(), progress.currentLevel(), progress.currentRank(),
                progress.targetLevel(), progress.targetRank());
    }
}
