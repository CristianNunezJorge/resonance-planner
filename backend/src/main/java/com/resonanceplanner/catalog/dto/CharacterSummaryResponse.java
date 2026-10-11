package com.resonanceplanner.catalog.dto;

import com.resonanceplanner.catalog.Element;
import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.WeaponType;

/** A character in the catalog list, without the ascension costs. */
public record CharacterSummaryResponse(String id, String name, Element element, WeaponType weaponType, Integer rarity,
        String role, String icon) {

    public static CharacterSummaryResponse from(GameCharacter character) {
        return new CharacterSummaryResponse(character.getId(), character.getName(), character.getElement(),
                character.getWeaponType(), character.getRarity(), character.getRole(), character.getIcon());
    }
}
