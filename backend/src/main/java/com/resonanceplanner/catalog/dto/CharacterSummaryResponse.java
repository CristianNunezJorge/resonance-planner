package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.Element;
import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.WeaponType;
import io.swagger.v3.oas.annotations.media.Schema;

/** A character in the catalog list, without the ascension costs. */
public record CharacterSummaryResponse(
        @Schema(example = "jinhsi") String id,
        @Schema(example = "Jinhsi") String name,
        @Schema(example = "SPECTRO") Element element,
        @Schema(example = "BROADBLADE") WeaponType weaponType,
        @Schema(description = "Stelle: 4 o 5", example = "5") Integer rarity,
        @Schema(example = "Main Damage Dealer") String role,
        @Schema(description = "Percorso dell'icona nel sito", example = "characters/jinhsi.webp") String icon) {

    public static CharacterSummaryResponse from(GameCharacter character) {
        return new CharacterSummaryResponse(character.getId(), character.getName(), character.getElement(),
                character.getWeaponType(), character.getRarity(), character.getRole(), character.getIcon());
    }
}
