package com.resonanceplanner.roster.dto;

import com.resonanceplanner.roster.OwnedCharacter;
import com.resonanceplanner.roster.Progress;
import io.swagger.v3.oas.annotations.media.Schema;

/** A character of the roster. Name, element and icon are in the public catalog, which the frontend already has. */
public record RosterEntryResponse(
        @Schema(example = "jinhsi") String characterId,
        @Schema(example = "20") int currentLevel,
        @Schema(example = "1") int currentRank,
        @Schema(example = "40") int targetLevel,
        @Schema(example = "2") int targetRank) {

    public static RosterEntryResponse from(OwnedCharacter owned) {
        Progress progress = owned.getProgress();
        return new RosterEntryResponse(owned.getCharacterId(), progress.currentLevel(), progress.currentRank(),
                progress.targetLevel(), progress.targetRank());
    }
}
